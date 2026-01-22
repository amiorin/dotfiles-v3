(ns alpha
  (:require
   [babashka.fs :as fs]
   [big-config :as bc]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]
   [clojure.string :as str]))

(defn parse-ops
  [{:keys [::dsl-or-opts] :as opts}]
  (-> (cond
        (map? dsl-or-opts)
        dsl-or-opts

        :else
        (let [all-actions #{"create" "delete"               ; no extra-args
                            "tofu" "ansible"                ; former exec
                            "render"                        ; no exec
                            "init" "plan" "apply" "destroy" ; tofu
                            "playbook"                      ; ansible
                            }]
          (loop [xs dsl-or-opts
                 token nil
                 actions []
                 resource-name nil
                 extra-args []]
            (cond
              (string? xs)
              (let [xs (-> (str/trim xs)
                           (str/split #"\s+"))]
                (recur (rest xs) (first xs) actions resource-name extra-args))

              (all-actions token)
              (let [actions (into actions (case token
                                            "render" [:render-tofu :render-ansible]
                                            "create" [:create-tofu :create-ansible]
                                            [(keyword token)]))]
                (recur (rest xs) (first xs) actions name extra-args))

              (nil? (seq actions))
              (throw (ex-info (format "`%s` is not an action (%s)" token (str/join "|" all-actions)) {:opts opts}))

              (#{"--resource-name"} token)
              {::resource-name (first xs)
               ::actions actions
               ::extra-args (rest xs)}

              :else
              (throw (ex-info "--resource-name is missing" {:opts opts}))))))
      (merge opts {::bc/exit 0
                   ::bc/err nil})))

(comment
  (parse-ops {::dsl-or-opts "build destroy plan --resource-name cesar-ford --tags focus"})
  (parse-ops {::dsl-or-opts "--resource-name cesar-ford"})
  (parse-ops {::dsl-or-opts "playbook --resource-name cesar-ford --tags focus"})
  (parse-ops {::dsl-or-opts "delete render create --resource-name cesar-ford"}))

(defn tofu-init?
  [dir]
  (not (fs/exists? (fs/path dir ".terraform"))))

(defn ->action-opts
  [{:keys [::extra-args ::target-prefix]} action-opts action]
  (let [tofu-dir (str (fs/path target-prefix "tofu"))
        ansible-dir (str (fs/path target-prefix "ansible"))
        new-action-opts (case action
                          :render {::step/steps ["render"]}
                          :create {::step/steps ["render" "exec"]
                                   ::run/cmds (cond-> [[{:dir tofu-dir} "tofu apply -auto-approve"]
                                                       [{:dir ansible-dir} "ansible-playbook main.yml"]]
                                                (tofu-init? tofu-dir) (->> (into [[{:dir tofu-dir} "tofu init"]])))}
                          :delete {::step/steps ["render" "exec"]
                                   ::run/cmds [[{:dir tofu-dir} "tofu destroy -auto-approve"]]}
                          (:init :plan :apply :destroy) {::step/steps ["render" "exec"]
                                                         ::run/cmds [[{:dir tofu-dir} (format "tofu %s %s"
                                                                                              (name action)
                                                                                              (str/join " " (cond-> extra-args
                                                                                                              (#{:apply :destroy} action) (->> (into ["-auto-approve"])))))]]}
                          :playbook {::step/steps ["render" "exec"]
                                     ::run/cmds [[{:dir ansible-dir} (format "ansible-playbook main.yml %s" (str/join " " extra-args))]]}
                          :tofu {::step/steps ["render" "exec"]
                                 ::run/cmds [[{:dir tofu-dir} (str/join " " extra-args)]]}
                          :ansible {::step/steps ["render" "exec"]
                                    ::run/cmds [[{:dir ansible-dir} (str/join " " extra-args)]]})]
    (merge-with into action-opts new-action-opts)))

(defn actions->opts
  [{:keys [::actions] :as opts}]
  (let [action-opts (-> (reduce (partial ->action-opts opts) {} actions)
                        (update ::step/steps #(-> % distinct vec)))]
    (merge opts action-opts)))

(comment
  (into (sorted-map) (actions->opts (parse-ops {::dsl-or-opts "delete create apply --resource-name cesar-ford"
                                                ::target-prefix ".dist"}))))

(defn ops
  [dsl-or-opts & opts]
  (let [step-fns [step/print-step-fn
                  (step-fns/->exit-step-fn ::end)
                  (step-fns/->print-error-step-fn ::end)]
        opts (-> (merge {::dsl-or-opts dsl-or-opts
                         ::target-prefix ".dist"
                         ::step/module "alpha"
                         ::step/profile "default"
                         ::bc/env :shell}
                        (first opts)
                        {::render/templates [{:template "alpha"
                                              :dir "tofu"
                                              :data-fn 'alpha/data-fn
                                              :template-fn 'alpha/template-fn
                                              :overwrite true
                                              :transform [["tofu"
                                                           :raw]]}
                                             {:template "alpha"
                                              :dir "ansible"
                                              :data-fn 'ansible/data-fn
                                              :template-fn 'alpha/template-fn
                                              :overwrite true
                                              :transform [["ansible"
                                                           :raw]
                                                          ['ansible/render "roles/users/tasks"
                                                           {:packages "packages.yml"
                                                            :repos "repos.yml"
                                                            :ssh-config "ssh-config.yml"}
                                                           :raw]
                                                          ['ansible/render
                                                           {:inventory "inventory.json"
                                                            :config "default.config.yml"}
                                                           :raw]]}]})
                 parse-ops
                 actions->opts)
        run-steps (step/->run-steps)]
    (run-steps step-fns opts)))

(comment
  (into (sorted-map) (ops "render --resource-name cesar-ford" {::bc/env :repl})))

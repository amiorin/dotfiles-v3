(ns alpha
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]
   [big-config.utils :as utils]
   [cheshire.core :as json]
   [clojure.string :as str]))

(defn parse-ops
  [{:keys [::dsl-or-opts] :as opts}]
  (-> (cond
        (map? dsl-or-opts)
        dsl-or-opts

        :else
        (let [all-actions #{"create" "delete" ; no extra-args
                            "tofu" "ansible" "init" "plan" "apply" "playbook" "render" ; extra-args
                            }]
          (loop [xs dsl-or-opts
                 token nil
                 actions []
                 name nil
                 extra-args []]
            (cond
              (string? xs)
              (let [xs (-> (str/trim xs)
                           (str/split #"\s+"))]
                (recur (rest xs) (first xs) actions name extra-args))

              (all-actions token)
              (let [actions (into actions (case token
                                            "render" [:render-tofu :render-ansible]
                                            "create" [:create-tofu :create-ansible]
                                            [(keyword token)]))]
                (recur (rest xs) (first xs) actions name extra-args))

              (nil? (seq actions))
              (throw (ex-info (format "`%s` is not an action (%s)" token (str/join "|" all-actions)) {:opts opts}))

              (#{"--name"} token)
              {::name (first xs)
               ::actions actions
               ::extra-args (rest xs)}

              :else
              (throw (ex-info "--name is missing" {:opts opts}))))))
      (merge opts {::bc/exit 0
                   ::bc/err nil})))

(comment
  (parse-ops {::dsl-or-opts "build destroy plan --name cesar-ford --tags focus"})
  (parse-ops {::dsl-or-opts "--name cesar-ford"})
  (parse-ops {::dsl-or-opts "playbook --name cesar-ford --tags focus"})
  (parse-ops {::dsl-or-opts "delete render create --name cesar-ford"}))

(defn tofu-init?
  [dir]
  (not (fs/exists? (fs/path dir ".terraform"))))

(defn tofu
  [step-fns {:keys [::name ::bc/env ::action ::extra-args] :as opts}]
  (let [run-steps (step/->run-steps)
        dir (format ".dist/%s/tofu" name)
        action-opts (case action
                      :render-tofu {::step/steps ["render"]}
                      :create-tofu {::step/steps ["render" "exec"]
                                    ::run/cmds (cond-> ["tofu apply -auto-approve"]
                                                 (tofu-init? dir) (->> (into ["tofu init"])))}
                      :delete {::step/steps ["render" "exec"]
                               ::run/cmds ["tofu destroy -auto-approve"]}
                      :init :plan :apply :destroy {::step/steps ["render" "exec"]
                                                   ::run/cmds [(format "tofu %s %s"
                                                                       (name action)
                                                                       (str/join " " (cond-> extra-args
                                                                                       (#{:apply :destroy} action (->> (into ["-auto-approve"]))))))]}
                      :tofu {::step/steps ["render" "exec"]
                             ::run/cmds [(str/join " " extra-args)]})
        tofu-opts (merge action-opts
                         {::bc/env env
                          ::step/module "tofu"
                          ::step/profile name
                          ::run/shell-opts {:dir dir
                                            :extra-env {"AWS_PROFILE" "default"}}
                          ::render/templates [{:template "alpha"
                                               :target-dir dir
                                               :overwrite true
                                               :transform [["tofu"
                                                            :raw]]}]})
        tofu-opts (run-steps step-fns tofu-opts)]
    (->> (select-keys tofu-opts [::bc/exit ::bc/err])
         (merge opts {::tofu-opts tofu-opts}))))

(defn tofu-outputs
  [{:keys [::name] :as opts}]
  (->> (try
         (-> (p/shell {:dir (format ".dist/%s/tofu" name)
                       :out :string} "tofu output --json")
             :out
             {::output (json/parse-string keyword)})
         (catch Throwable _
           {::output {:ipv4_address {:sensitive false, :type "string", :value "77.42.91.213"}}}))
       (merge opts)))

(defn ansible
  [step-fns {:keys [::name ::bc/env ::action ::extra-args] :as opts}]
  (let [opts (tofu-outputs opts)
        run-steps (step/->run-steps)
        dir (format ".dist/%s/ansible" name)
        action-opts (case action
                      :render-ansible {::step/steps ["render"]}
                      :create-ansible {::step/steps ["render" "exec"]
                                       ::run/cmds ["ansible-playbook main.yml"]}
                      :playbook {::step/steps ["render" "exec"]
                                 ::run/cmds [(format "ansible-playbook main.yml %s" (str/join " " extra-args))]}
                      :ansible {::step/steps ["render" "exec"]
                                ::run/cmds [(str/join " " extra-args)]})
        ansible-opts (merge action-opts
                            {::output (::output opts)
                             ::bc/env env
                             ::step/module "ansible"
                             ::step/profile name
                             ::run/shell-opts {:dir dir
                                               :extra-env {"AWS_PROFILE" "default"}}
                             ::render/templates [{:template "alpha"
                                                  :name name
                                                  :target-dir dir
                                                  :overwrite true
                                                  :data-fn 'ansible/data-fn
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
        ansible-opts (run-steps step-fns ansible-opts)]
    (->> (select-keys ansible-opts [::bc/exit ::bc/err])
         (merge opts {::ansible-opts ansible-opts}))))

(comment
  (ansible nil {::name "cesar-ford"
                ::bc/env :repl}))

(defn ops
  [dsl-or-opts & opts]
  (let [step-fns [step/print-step-fn
                  (step-fns/->exit-step-fn ::end)
                  (step-fns/->print-error-step-fn ::end)]
        opts (merge {::dsl-or-opts dsl-or-opts
                     ::bc/env :repl} (first opts))
        rama-cluster (core/->workflow {:first-step ::parse-ops
                                       :wire-fn (fn [step step-fns]
                                                  (case step
                                                    ::parse-ops [parse-ops ::any]
                                                    ::tofu [(partial tofu step-fns) ::any]
                                                    ::ansible [(partial ansible step-fns) ::any]
                                                    ::end [identity]))
                                       :next-fn (fn [step _ {:keys [::bc/exit ::actions] :as opts}]
                                                  (let [next-action (fn [& subset-actions]
                                                                      (and ((set subset-actions)
                                                                            (first actions))
                                                                           (= exit 0)))
                                                        next-step (fn [next-step]
                                                                    [next-step (merge opts {::actions (rest actions)
                                                                                            ::action (first actions)})])]
                                                    (cond
                                                      (= step ::end)
                                                      [nil opts]

                                                      (next-action :create-tofu :delete :tofu :init :plan :destroy :render-tofu)
                                                      (next-step ::tofu)

                                                      (next-action :create-ansible :ansible :playbook :render-ansible)
                                                      (next-step ::ansible)

                                                      :else
                                                      [::end opts])))})]
    (rama-cluster step-fns opts)))

(comment
  (utils/sort-nested-map (ops "render --name cesar-ford" {::bc/env :repl})))

(comment
  (utils/sort-nested-map (ops "render --name cesar-ford" {::bc/env :repl})))

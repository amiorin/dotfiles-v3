(ns walter
  (:require
   [babashka.process :as p]
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.render :as render]
   [big-config.step-fns :as step-fns]
   [big-config.utils :refer [debug]]
   [big-config.workflow :as workflow]
   [cheshire.core :as json]
   [com.rpl.specter :as s]))

(def step-fns [workflow/print-step-fn
               #_step-fns/tap-step-fn
               (step-fns/->exit-step-fn ::workflow/end)
               (step-fns/->print-error-step-fn ::workflow/end)])

(defn tofu
  [step-fns opts]
  (let [opts (workflow/prepare {::workflow/name ::tofu
                                ::render/templates [{:template "alpha"
                                                     :overwrite true
                                                     :transform [["tofu"
                                                                  :raw]]}]}
                               opts)]
    (workflow/run-steps step-fns opts)))

(defn tofu*
  [args & [opts]]
  (let [opts (merge (workflow/parse-args args)
                    opts)]
    (tofu step-fns opts)))

(comment
  (debug tap-values
    (tofu* "render tofu:plan" {::bc/env :repl}))
  (-> tap-values))

(defn ansible
  [step-fns opts]
  (let [opts (workflow/prepare {::workflow/name ::ansible
                                ::render/templates [{:template "alpha"
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
                                                                  :raw]]}]}
                               opts)]
    (workflow/run-steps step-fns opts)))

(defn ansible*
  [args & [opts]]
  (let [opts (merge (workflow/parse-args args)
                    opts)]
    (ansible step-fns opts)))

(comment
  (debug tap-values
    (ansible* "render" {::bc/env :repl}))
  (-> tap-values))

(defn populate-params
  [{:keys [::workflow/dirs] :as opts}]
  (let [ip (-> (p/shell {:dir (::tofu dirs)
                         :out :string} "tofu show --json")
               :out
               (json/parse-string keyword)
               (->> (s/select-one [:values :root_module :resources s/FIRST :values :ipv4_address])))]
    (assoc opts ::workflow/params {:ip ip})))

(comment
  (populate-params {::tofu ".dist/clare/tofu"}))

(defn resource-create
  [step-fns {:keys [::tofu-opts ::ansible-opts] :as opts}]
  (let [tofu-opts (merge (workflow/parse-args "render tofu:init")
                         {::bc/env :repl}
                         tofu-opts)
        ansible-opts (merge (workflow/parse-args "render")
                            {::bc/env :repl}
                            ansible-opts)
        all-opts (atom {})
        wf (core/->workflow {:first-step ::start
                             :wire-fn (fn [step step-fns]
                                        (case step
                                          ::start [core/ok ::tofu]
                                          ::tofu [(partial tofu step-fns) ::ansible]
                                          ::ansible [(partial ansible step-fns) ::end]
                                          ::end [identity]))
                             :next-fn (fn [step next-step {:keys [::bc/exit ::workflow/dirs] :as opts}]
                                        (let [swap-opts! (fn [kw next-opts & opts-fns]
                                                           (swap! all-opts assoc kw opts)
                                                           (let [new-opts (merge (core/ok) {::workflow/dirs dirs} next-opts)]
                                                             (reduce (fn [a f]
                                                                       (f a)) new-opts opts-fns)))]
                                          (cond
                                            (= step ::end)
                                            [nil opts]

                                            (> exit 0)
                                            [::end opts]

                                            :else
                                            [next-step (case next-step
                                                         ::tofu (swap-opts! :create-opts tofu-opts)
                                                         ::ansible (-> (swap-opts! :tofu-opts ansible-opts populate-params))
                                                         ::end (let [{:keys [create-opts tofu-opts]} @all-opts]
                                                                 (-> (swap-opts! :ansible-opts create-opts)
                                                                     (assoc ::tofu-opts tofu-opts)
                                                                     (assoc ::ansible-opts opts))))])))})]
    (wf step-fns opts)))

(comment
  (debug tap-values
    (resource-create [step-fns/bling-step-fn] {}))
  (-> tap-values))

(defn resource-delete
  [step-fns {:keys [::tofu-opts] :as opts}]
  (let [tofu-opts (merge (workflow/parse-args "render tofu:destroy:-auto-approve")
                         {::bc/env :repl}
                         tofu-opts)
        all-opts (atom {})
        wf (core/->workflow {:first-step ::start
                             :wire-fn (fn [step step-fns]
                                        (case step
                                          ::start [core/ok ::tofu]
                                          ::tofu [(partial tofu step-fns) ::end]
                                          ::end [identity]))
                             :next-fn (fn [step next-step {:keys [::bc/exit ::workflow/dirs] :as opts}]
                                        (let [swap-opts! (fn [kw next-opts & opts-fns]
                                                           (swap! all-opts assoc kw opts)
                                                           (let [new-opts (merge (core/ok) {::workflow/dirs dirs} next-opts)]
                                                             (reduce (fn [a f]
                                                                       (f a)) new-opts opts-fns)))]
                                          (cond
                                            (= step ::end)
                                            [nil opts]

                                            (> exit 0)
                                            [::end opts]

                                            :else
                                            [next-step (case next-step
                                                         ::tofu (swap-opts! :create-opts tofu-opts)
                                                         ::end (let [{:keys [create-opts tofu-opts]} @all-opts]
                                                                 (-> (swap-opts! :ansible-opts create-opts)
                                                                     (assoc ::tofu-opts tofu-opts)
                                                                     (assoc ::ansible-opts opts))))])))})]
    (wf step-fns opts)))

(comment
  (debug tap-values
    (resource-delete [(fn [f step opts]
                        (tap> [step opts])
                        (f step opts))]  {}))
  (-> tap-values))

(defn resource
  [step-fns opts]
  (let [opts (merge {::workflow/create-fn resource-create
                     ::workflow/delete-fn resource-delete}
                    opts)
        wf (core/->workflow {:first-step ::start
                             :wire-fn (fn [step step-fns]
                                        (case step
                                          ::start [(partial workflow/run-steps step-fns) ::end-comp]
                                          ::end-comp [identity]))})]
    (wf step-fns opts)))

(comment
  (debug tap-values
    (resource [(fn [f step opts]
                 (tap> [step opts])
                 (f step opts))]
              (merge (workflow/parse-args "create delete")
                     {::bc/env :repl})))
  (-> tap-values))

(defn resource*
  [args & [opts]]
  (let [step-fns [workflow/print-step-fn
                  (step-fns/->exit-step-fn ::end-comp)
                  (step-fns/->print-error-step-fn ::end-comp)]
        opts (merge (workflow/parse-args args)
                    opts)]
    (resource step-fns opts)))

(comment
  (debug tap-values
    (resource* "create" {::bc/env :repl}))
  (-> tap-values))

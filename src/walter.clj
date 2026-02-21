(ns walter
  (:require
   [babashka.process :as p]
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step-fns :as step-fns]
   [big-config.utils :refer [debug assert-args-present]]
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
  [dirs]
  (let [ip (-> (p/shell {:dir (::tofu dirs)
                         :out :string} "tofu show --json")
               :out
               (json/parse-string keyword)
               (->> (s/select-one [:values :root_module :resources s/FIRST :values :ipv4_address])))]
    {::workflow/params {:ip ip}}))

(comment
  (populate-params {::tofu ".dist/clare/tofu"}))

(defn resource-create
  [step-fns {:keys [::tofu-opts ::ansible-opts] :as opts}]
  (assert-args-present tofu ansible)
  (let [all-opts (atom {})
        swap-opts (fn [kw prev-opts next-opts dirs]
                    (swap! all-opts assoc kw prev-opts)
                    (merge (core/ok) {::workflow/dirs dirs} next-opts))
        wf (core/->workflow {:first-step ::start
                             :wire-fn (fn [step step-fns]
                                        (case step
                                          ::start [core/ok ::tofu]
                                          ::tofu [(partial tofu step-fns) ::ansible]
                                          ::ansible [(partial ansible step-fns) ::end]
                                          ::end [identity]))
                             :next-fn (fn [step next-step {:keys [::bc/exit ::workflow/dirs] :as opts}]
                                        (cond
                                          (= step ::end)
                                          [nil opts]

                                          (> exit 0)
                                          [::end opts]

                                          :else
                                          [next-step (case next-step
                                                       ::tofu (swap-opts :create-opts opts tofu-opts dirs)
                                                       ::ansible (swap-opts :tofu-opts opts ansible-opts dirs)
                                                       ::end (let [{:keys [create-opts tofu-opts]} @all-opts]
                                                         (-> (swap-opts :ansible-opts opts create-opts dirs)
                                                             (assoc ::tofu-opts tofu-opts)
                                                             (assoc ::ansible-opts opts))))]))})]
    (wf step-fns opts)))

(comment
  (debug tap-values
    (resource-create [step-fns/bling-step-fn]
                     {::tofu-opts (merge (workflow/parse-args "render tofu:init tofu:plan")
                                         {::bc/env :repl})
                      ::ansible-opts (merge (workflow/parse-args "render")
                                            {::bc/env :repl})}))
  (-> tap-values))

(defn resource-delete
  [step-fns opts]
  (let [delete-opts (merge (workflow/parse-args "render tofu:destroy:-auto-approve")
                           (select-keys opts [::bc/env ::run/shell-opts]))
        wf (core/->workflow {:first-step ::start
                             :wire-fn (fn [step step-fns]
                                        (case step
                                          ::start [core/ok ::tofu]
                                          ::tofu [(partial tofu step-fns) ::end]
                                          ::end [identity]))})
        delete (wf step-fns delete-opts)]
    (merge opts (core/ok) {::delete delete})))

(comment
  (into (sorted-map) (resource-delete step-fns {::bc/env :repl})))

(defn resource
  [step-fns opts]
  (let [opts (merge {::workflow/create-fn 'clare/resource-create
                     ::workflow/delete-fn 'clare/resource-delete}
                    opts)
        wf (core/->workflow {:first-step ::start
                             :wire-fn (fn [step step-fns]
                                        (case step
                                          ::start [(partial workflow/run-steps step-fns) ::end-comp]
                                          ::end-comp [identity]))})
        opts (wf step-fns opts)]
    opts))

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

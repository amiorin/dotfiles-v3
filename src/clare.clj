(ns clare
  (:require
   [babashka.process :as p]
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step-fns :as step-fns]
   [big-config.workflow :as workflow]
   [cheshire.core :as json]))

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

(comment
  (workflow/parse-args "render -- ansible-playbook main.yml --start-at-task 'Install doomemacs'"))

(defn tofu*
  [args & [opts]]
  (let [opts (merge (workflow/parse-args args)
                    opts)]
    (tofu step-fns opts)))

(comment
  (into (sorted-map) (tofu* "render tofu:plan" {::bc/env :repl
                                                ::run/shell-opts {:err *err*
                                                                  :out *out*}})))

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
                    {::workflow/params {:ipv4-address "157.180.112.227"}}
                    opts)]
    (ansible step-fns opts)))

(comment
  (into (sorted-map) (ansible* "render" {::bc/env :repl
                                         ::run/shell-opts {:err *err*
                                                           :out *out*}})))

(defn populate-params
  [dirs]
  (let [ip (or (-> (p/shell {:dir (::tofu dirs)
                             :out :string} "tofu output --json")
                   :out
                   (json/parse-string keyword)
                   :ipv4_address
                   :value)
               "1.2.3.4")]
    {::workflow/params {:ipv4-address ip}}))

(comment
  (populate-params {::tofu ".dist/clare/tofu"}))

(defn resource-create
  [step-fns opts]
  (let [create-opts (select-keys opts [::bc/env ::run/shell-opts])
        wf (core/->workflow {:first-step ::start
                             :wire-fn (fn [step step-fns]
                                        (case step
                                          ::start [core/ok ::tofu]
                                          ::tofu [(partial tofu step-fns) ::ansible]
                                          ::ansible [(partial ansible step-fns) ::end]
                                          ::end [identity]))
                             :next-fn (fn [step next-step {:keys [::bc/exit ::workflow/dirs ::comp-opts] :as opts}]
                                        (cond
                                          (= step ::end)
                                          [nil opts]

                                          (> exit 0)
                                          [::end opts]

                                          (#{::tofu ::ansible} next-step)
                                          [next-step (case next-step
                                                       ::tofu (merge (workflow/parse-args "render")
                                                                     opts
                                                                     {::comp-opts opts})
                                                       ::ansible (merge (workflow/parse-args "render -- ansible-playbook main.yml")
                                                                        comp-opts
                                                                        {::workflow/dirs dirs}
                                                                        {step opts}
                                                                        (populate-params dirs)
                                                                        {::comp-opts comp-opts}))]

                                          :else
                                          [next-step opts]))})
        create (wf step-fns create-opts)]
    (merge opts (core/ok) {::create create})))

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
  (into (sorted-map) (resource-delete step-fns {::bc/env :repl
                                                ::run/shell-opts {:err *err*
                                                                  :out *out*}})))

(defn resource
  [step-fns opts]
  (let [opts (merge {::workflow/create-fn 'clare/resource-create
                     ::workflow/delete-fn 'clare/resource-delete}
                    opts)
        opts (workflow/run-steps step-fns opts)]
    opts))

(defn resource*
  [args & [opts]]
  (let [step-fns [workflow/print-step-fn
                  (step-fns/->exit-step-fn ::end)
                  (step-fns/->print-error-step-fn ::end)]
        opts (merge (workflow/parse-args args)
                    opts)]
    (resource step-fns opts)))

(comment
  (let [tap-values (atom [])
        done (promise)
        f (fn [v]
            (if (= v :done)
              (deliver done true)
              (swap! tap-values conj v)))]
    (add-tap f)
    (try
      (let [_ (tap> :start)
            res (into (sorted-map) (resource* "create" {::bc/env :repl
                                                        ::run/shell-opts {:err *err*
                                                                          :out *out*}}))]
        (tap> :done)
        @done
        (assoc res ::tap @tap-values))
      (finally
        (def tap-values @tap-values)
        (remove-tap f))))
  (-> tap-values))

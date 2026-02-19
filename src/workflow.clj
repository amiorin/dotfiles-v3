(ns workflow
  (:require
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step-fns :as step-fns]
   [big-config.workflow :as workflow]))

(def step-fns [workflow/print-step-fn
               (step-fns/->exit-step-fn ::workflow/end)
               (step-fns/->print-error-step-fn ::workflow/end)])

(defn tofu*
  [step-fns opts]
  (let [opts (workflow/prepare {::workflow/name ::tofu
                                ::render/templates [{:template "alpha"
                                                     :overwrite true
                                                     :transform [["tofu"
                                                                  :raw]]}]}
                               opts)]

    (workflow/run-steps step-fns opts)))

(defn tofu
  [args & [opts]]
  (let [opts (merge (workflow/parse-tool-args args)
                    opts)]
    (tofu* step-fns opts)))

(comment
  (into (sorted-map) (tofu "render tofu:plan" {::bc/env :repl
                                               ::run/shell-opts {:err *err*
                                                                 :out *out*}})))

(defn ansible*
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

(defn ansible
  [args & [opts]]
  (let [opts (merge (workflow/parse-tool-args args)
                    opts)]
    (ansible* step-fns opts)))

(comment
  (into (sorted-map) (ansible "render" {::bc/env :repl
                                        ::run/shell-opts {:err *err*
                                                          :out *out*}})))

(def resource*
  (core/->workflow {:first-step ::start
                    :wire-fn (fn [step step-fns]
                               (case step
                                 ::start [core/ok ::tofu]
                                 ::tofu [(partial tofu* step-fns) ::ansible]
                                 ::ansible [(partial ansible* step-fns) ::end]
                                 ::end [identity]))
                    :next-fn (fn [step next-step {:keys [::bc/exit ::workflow/dirs ::comp-opts] :as opts}]
                               (cond
                                 (= step ::end)
                                 [nil opts]

                                 (> exit 0)
                                 [::end opts]

                                 (#{::tofu ::ansible} next-step)
                                 [next-step (case next-step
                                              ::tofu (merge (workflow/parse-tool-args "render")
                                                            opts
                                                            {::comp-opts opts})
                                              (merge (workflow/parse-tool-args "render")
                                                     comp-opts
                                                     {::workflow/dirs dirs}
                                                     {step opts}
                                                     {::comp-opts comp-opts}))]

                                 :else
                                 [next-step opts]))}))

(comment
  (let [tap-values (atom [])
        done (promise)
        f (fn [v]
            (if (= v :done)
              (deliver done true)
              (swap! tap-values conj v)))]
    (add-tap f)
    (try
      (let [res (into (sorted-map) (resource* step-fns {::bc/env :repl
                                                        ::run/shell-opts {:err *err*
                                                                          :out *out*}}))]
        (tap> :done)
        @done
        (assoc res ::tap @tap-values))
      (finally
        (remove-tap f)))))

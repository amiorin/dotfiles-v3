(ns workflow
  (:require
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step-fns :as step-fns]
   [big-config.workflow :as workflow]))

(comment
  (into (sorted-map) (tofu* [] {::params {:parameter-a 1}})))

(def step-fns [workflow/print-step-fn
               #_(step-fns/->exit-step-fn ::workflow/end)
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

(comment
  (let [tap (atom [])
        _ (add-tap #(swap! tap conj %))]
    (def step-fns [workflow/print-step-fn
                   #_(step-fns/->exit-step-fn ::workflow/end)
                   #_step-fns/tap-step-fn
                   (step-fns/->print-error-step-fn ::workflow/end)])
    (defn save-comp-opts
      [args opts]
      (let [tool-opts (workflow/parse-tool-args args)]
        (merge tool-opts
               opts
               (core/ok)
               {::comp-opts opts})))
    (defn save-tool-opts
      [step args {:keys [::comp-opts] :as opts}]
      (let [tool-opts (workflow/parse-tool-args args)]
        (merge tool-opts
               comp-opts
               (core/ok)
               {step opts}
               {::comp-opts comp-opts})))
    (def resource*
      (core/->workflow {:first-step ::start
                        :wire-fn (fn [step step-fns]
                                   (case step
                                     ::start [(partial save-comp-opts "render") ::tofu]
                                     ::tofu [(partial tofu* step-fns) ::post-tofu]
                                     ::post-tofu [(partial save-tool-opts ::tofu "render") ::ansible]
                                     ::ansible [(partial ansible* step-fns) ::end]
                                     ::end [identity]))}))
    (-> (sorted-map)
        (into (resource* step-fns {::bc/env :repl
                                   ::run/shell-opts {:err *err*
                                                     :out *out*}}))
        (into {::tap @tap}))))

(comment
  (let [tap (atom [])
        _ (add-tap #(swap! tap conj %))]
    (def step-fns [workflow/print-step-fn
                   #_(step-fns/->exit-step-fn ::workflow/end)
                   #_step-fns/tap-step-fn
                   (step-fns/->print-error-step-fn ::workflow/end)])
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

    (-> (sorted-map)
        (into (resource* step-fns {::bc/env :repl
                                   ::run/shell-opts {:err *err*
                                                     :out *out*}}))
        (into {::tap @tap}))))

;; (defn resource
;;     [any & [opts step-fns]]
;;     (let [{:keys [::cmds ::resource-name]} (pstar-resource any)
;;           opts (merge {::bc/env :repl
;;                        ::working-dir-prefix (-> (fs/path  ".dist/" resource-name))}
;;                       opts)
;;           xs (mapcat #(case %
;;                         #_#_:create [(tofu "render tofu:init tofu:apply:-auto-approve -- big-iron cesar-ford" opts)
;;                                      (ansible "render ansible-playbook:main.yml -- big-iron cesar-ford" opts)]
;;                         :create [(tofu "render -- big-iron cesar-ford" opts)
;;                                  (ansible "render -- big-iron cesar-ford" opts)]
;;                         :delete [(tofu "render tofu:destroy:-auto-approve -- big-iron cesar-ford" opts)]) cmds)
;;           step-fns (if (= step-fns :default)
;;                      [(->working-dir-step-fn)
;;                       (->copy-data-step-fn)
;;                       step/print-step-fn
;;                       (step-fns/->exit-step-fn ::step/end xs)
;;                       (step-fns/->print-error-step-fn ::step/end)]
;;                      step-fns)]
;;       (if step-fns
;;         ((step/->run-steps) step-fns xs)
;;         xs)))

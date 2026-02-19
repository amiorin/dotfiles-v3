(ns workflow
  (:require
   [big-config :as bc]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step-fns :as step-fns]
   [big-config.workflow :as workflow]))

(comment
  (into (sorted-map) (tofu* [] {::params {:parameter-a 1}})))

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
    (tofu* [workflow/print-step-fn
            (step-fns/->exit-step-fn ::workflow/end)
            (step-fns/->print-error-step-fn ::workflow/end)] opts)))

(comment
  (tofu "render tofu:plan" {::bc/env :repl
                            ::run/shell-opts {:err *err*
                                              :out *out*}}))

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

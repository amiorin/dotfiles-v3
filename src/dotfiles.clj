(ns dotfiles
  (:require
   [big-config :as bc]
   [big-config.build :as build]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]))

(defn run-steps [s opts]
  (let [opts (merge opts {::build/templates [{:template "stage-1"
                                              :data-fn (fn [{:keys [::step/profile]} _]
                                                         {:profile profile})
                                              :template-fn (fn [{:keys [:profile]} edn]
                                                             (assoc edn :target-dir (format "resources/stage-2/%s" profile)))
                                              :overwrite :delete
                                              :transform [["build"
                                                           :raw]
                                                          ["{{ profile }}"
                                                           :raw]]}]})
        step-fns [step/print-step-fn
                  (step-fns/->exit-step-fn ::step/end)
                  (step-fns/->print-error-step-fn ::step/end)]]
    (step/run-steps s step-fns opts)))

(comment
  (build/create {::build/templates [{:template "stage-1"
                                     :target-dir "resources/stage-2/ubuntu"
                                     :post-process-fn (fn [edn data]
                                                        (println "foo"))
                                     :overwrite :delete
                                     :transform [["build"
                                                  :raw]
                                                 ["ubuntu"
                                                  :raw]]}]})
  (run-steps "create -- dotfiles macos" {::bc/env :repl}))

(ns dotfiles
  (:require
   [big-config.build :as build]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]))

(defn run-steps [s opts]
  (let [[_ _ module profile] (step/parse s)
        opts (merge opts {::build/recipes [{:template "amiorin/dotfiles_v3"
                                            :target-dir (format "dist/%s/%s" module profile)
                                            :overwrite :delete
                                            :transform [["build"
                                                         :raw]
                                                        [profile
                                                         :raw]]}]})
        step-fns [step/print-step-fn
                  (step-fns/->exit-step-fn ::step/end)
                  (step-fns/->print-error-step-fn ::step/end)]]
    (step/run-steps s step-fns opts)))

(comment
  (build/create {::build/recipes [{:template "amiorin/dotfiles_v3"
                                   :target-dir "dist/dotfiles/macos"
                                   :overwrite :delete
                                   :transform [["build"
                                                :raw]
                                               ["ubuntu"
                                                :raw]]}]}))

(ns dotfiles
  (:require
   [big-config :as bc]
   [big-config.render :as render]
   [big-config.lock :as lock]
   [big-config.run :as run]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]))

(alter-var-root #'render/*non-replaced-exts* (constantly #{"jpg" "jpeg" "png" "gif" "bmp" "bin"}))

(defn run-steps [s opts]
  (let [{:keys [module profile]} (step/parse-module-and-profile s)
        dir (format "dist/%s" profile)
        opts (merge opts
                    {::run/shell-opts {:dir dir}
                     ::render/templates [{:template "stage-1"
                                          :target-dir (format "resources/stage-2/%s" profile)
                                          :overwrite :delete
                                          :transform [["build"
                                                       :raw]
                                                      ["{{ profile }}"
                                                       :raw]]}
                                         {:template "stage-2"
                                          :target-dir dir
                                          :overwrite true
                                          :transform [["{{ profile }}"]]}]})]
    (step/run-steps s opts)))

(comment
  (run-steps "render -- dotfiles ubuntu" {::bc/env :repl}))

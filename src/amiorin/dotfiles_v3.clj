(ns amiorin.dotfiles-v3
  (:require
   [aero.core :as aero]
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.lock :as lock]
   [big-config.run :as run]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]
   [clojure.java.io :as io]
   [org.corfield.new :as new]))

(defn data-fn
  [data]
  (assoc data :target (System/getProperty "user.home")))

(defn template-fn
  [edn {:keys [root]}]
  (cond-> edn
    root
    (merge {:root root})))

(defn post-process-fn
  [_edn _opts])

(defn opts->dir
  [{:keys [::module ::profile ::bc/target-dir]}]
  (or target-dir (format "dist/%s/%s" module profile)))

(defn build-fn [{:keys [::module ::profile] :as opts}]
  (binding [*out* (java.io.StringWriter.)]
    (let [default-opts {:template "amiorin/dotfiles-v3"
                        :name "amiorin/dotfiles-v3"
                        :target-dir (opts->dir opts)
                        :module module
                        :profile profile}]
      (new/create (merge default-opts {:overwrite :delete}))
      (new/create (merge default-opts {:overwrite :true
                                       :root profile}))))
  (core/ok opts))

(defn opts-fn [opts]
  (merge opts {::lock/lock-keys [::module ::profile]
               ::run/shell-opts {:dir (opts->dir opts)
                                 :extra-env {"AWS_PROFILE" "default"}}}))

(defn run-steps
  ([s]
   (run-steps s nil))
  ([s opts]
   (run-steps s [step/print-step-fn
                 (step-fns/->exit-step-fn ::step/end)
                 (step-fns/->print-error-step-fn ::step/end)] opts))
  ([s step-fns opts]
   (apply run-steps step-fns opts (step/parse s)))
  ([step-fns opts steps cmds module profile]
   (let [opts (-> "amiorin/dotfiles_v3/config.edn"
                  io/resource
                  aero/read-config
                  (merge (or opts {::bc/env :repl})
                         {::step/steps steps
                          ::run/cmds cmds
                          ::module module
                          ::profile profile})
                  opts-fn)
         run-steps (step/->run-steps build-fn)]
     (run-steps step-fns opts))))

(comment
  (run-steps "build exec -- dotfiles macos bb tasks"
             {::bc/env :repl})
  (run-steps "build exec -- dotfiles ubuntu bb tasks"
             {::bc/env :repl}))

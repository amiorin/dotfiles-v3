(ns amiorin.dotfiles-v3
  (:require
   [aero.core :as aero]
   [amiorin.ansible :as ansible]
   [amiorin.repos :as repos]
   [babashka.fs :as fs]
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.lock :as lock]
   [big-config.run :as run]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.corfield.new :as new]))

(defn data-fn
  [data]
  data)

(defn template-fn
  [edn {:keys [module] :as _data}]
  (if (#{"minipc"} module)
    (merge edn {:root module})
    edn))

(defn post-process-fn
  [_edn {:keys [target-dir]}]
  (let [tpl-name "repos.yml"
        tpl-resource (format "amiorin/dotfiles_v3/selmer/%s" tpl-name)
        role (as-> (str/split tpl-name #"\.") $
               (clojure.string/join "." (butlast $)))
        dest (format "%s/roles/%s/tasks/main.yml" target-dir role)]
    (fs/create-dirs (fs/parent dest))
    (-> (repos/render tpl-resource repos/default-repos)
        (->> (spit dest))))
  (doseq [tpl-name ["default.config.yml" "inventory.ini"]]
    (let [tpl-file (format "amiorin/dotfiles_v3/selmer/%s" tpl-name)
          dest (format "%s/%s" target-dir tpl-name)]
      (fs/create-dirs (fs/parent dest))
      (-> (ansible/render tpl-file ansible/default-users)
          (->> (spit dest))))))

(defn opts->dir
  [{:keys [::module ::profile ::bc/target-dir]}]
  (or target-dir (format "dist/%s/%s" module profile)))

(defn build-fn [{:keys [::module ::profile] :as opts}]
  (binding [*out* (java.io.StringWriter.)]
    (new/create {:template "amiorin/dotfiles-v3"
                 :name "amiorin/dotfiles-v3"
                 :target-dir (opts->dir opts)
                 :module module
                 :profile profile
                 :overwrite :delete}))
  (core/ok opts))

(defn opts-fn [opts]
  (merge opts {::lock/lock-keys [::module ::profile]
               ::run/shell-opts {:dir (opts->dir opts)}}))

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
  (run-steps "build -- minipc ansible"
             {::bc/env :repl}))

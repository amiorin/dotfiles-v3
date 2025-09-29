(ns dotfiles
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [big-config :as bc]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step :as step]
   [clojure.string :as str]
   [selmer.filters :refer [add-filter!]]))

(add-filter! :lookup-env
             (fn [x]
               (System/getenv x)))

(alter-var-root #'render/*non-replaced-exts* (constantly #{"jpg" "jpeg" "png" "gif" "bmp" "bin"}))

(defn run-steps [s opts & step-fns]
  (let [{:keys [profile]} (step/parse-module-and-profile s)
        dir (format "dist/%s" profile)
        opts (merge opts
                    {::run/shell-opts {:dir dir}
                     ::render/templates [{:template "stage-1"
                                          :target-dir (format "resources/stage-2/%s" profile)
                                          :overwrite :delete
                                          :transform [["common"
                                                       :raw]
                                                      ["{{ profile }}"
                                                       :raw]]}
                                         {:template "stage-2"
                                          :target-dir dir
                                          :overwrite true
                                          :transform [["{{ profile }}"]]}]})]
    (if step-fns
      (apply step/run-steps s opts step-fns)
      (step/run-steps s opts))))

(comment
  (run-steps "render -- dotfiles ubuntu" {::bc/env :repl}))

(def home (System/getProperty "user.home"))

(defn diff [& {:keys [dir]}]
  (let [dir (str/replace dir #"/$" "")
        prefix (str dir "/")
        files (atom [])
        _ (fs/walk-file-tree dir {:visit-file (fn [path _attr]
                                                (let [rel-path (str/replace path prefix "")]
                                                  (swap! files conj (str path))
                                                  :continue))})
        copies (->> @files
                    (map (fn [x]
                           (let [src (str/replace x prefix "")]
                             [(format "%s/%s" home src) (str x)]))))]
    (doseq [[src dst] copies]
      (let [cmd (format "diff --color=always %s %s" src dst)]
        (println "$ " cmd)
        (process/shell {:continue true
                        :out *out*
                        :err *err*} cmd)))))

(defn install [& {:keys [dir]}]
  (let [dir (str/replace dir #"/$" "")]
    (fs/copy-tree dir home {:replace-existing true})))

(comment
  (diff :dir "dist/dotfiles/macos/dotfiles")
  (install :dir "dist/dotfiles/macos/dotfiles"))

(defn discover
  "discover all dirs inside a parent dir and return them as list of strings"
  [parent-dir]
  (let [profiles (atom [])]
    (fs/walk-file-tree parent-dir {:max-depth 2
                                   :pre-visit-dir (fn [dir _]
                                                    (let [dir (str (fs/relativize parent-dir dir))]
                                                      (when-not (str/blank? dir)
                                                        (swap! profiles conj dir)))
                                                    :continue)})
    @profiles))

(comment
  (discover "resources"))

(defn core [& [cmd profile opts]]
  (let [profile (case cmd
                  :render (or profile "all")
                  (or profile (System/getenv "DOTFILES_PROFILE")))
        opts (or opts {::bc/env :shell})]
    (case cmd
      :render (case profile
                "all" (let [profiles (discover "resources/stage-2")]
                        (doseq [profile profiles]
                          (process/shell (format "bb render %s" profile))))
                (run-steps (format "render -- dotiles %s" profile) opts))
      :diff (run-steps (format "render exec -- dotfiles %s bb diff" profile) opts)
      :install (run-steps (format "render exec -- dotfiles %s bb install" profile) opts))))

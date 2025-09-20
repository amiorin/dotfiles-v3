(ns amiorin.core
  (:require
   [babashka.fs :as fs]
   [big-config.run :as run]
   [clojure.string :as str]))

(defn git-config [env]
  (let [git-cmds [["user.name" "Alberto Miorin"]
                  ["user.email" "32617+amiorin@users.noreply.github.com"]
                  ["pull.ff" "only"]
                  ["pull.rebase" "true"]
                  ["init.defaultBranch" "main"]]
        cmds (for [[k v] git-cmds]
               (format "git config --global %s \"%s\"" k v))]

    (run/run-cmds
     {:big-config/env env
      :big-config.run/cmds cmds})))

(def blacklist #{".gitconfig"})

(def home (System/getProperty "user.home"))

(defn backup [& {:keys [dir]}]
  (let [dir (str/replace dir #"/$" "")
        prefix (str dir "/")
        files (atom [])
        _ (fs/walk-file-tree dir {:visit-file (fn [path _attr]
                                                (let [rel-path (str/replace path prefix "")]
                                                  (when-not (blacklist rel-path)
                                                    (swap! files conj (str path)))
                                                  :continue))})
        copies (->> @files
                    (map (fn [x]
                           (let [src (str/replace x prefix "")]
                             [(format "%s/%s" home src) (str x)]))))]
    (doseq [[src dst] copies]
      (fs/copy src dst {:replace-existing true}))))

(defn install [& {:keys [dir]}]
  (let [dir (str/replace dir #"/$" "")]
    (fs/copy-tree dir home {:replace-existing true})))

(comment
  (git-config :repl)
  (backup :dir "dist/dotfiles/macos/dotfiles")
  (install :dir "dist/dotfiles/macos/dotfiles"))

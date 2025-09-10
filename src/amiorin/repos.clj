(ns amiorin.repos
  (:require
   [clojure.java.io :as io]
   [selmer.parser :as p]))

(def default-repos [{:user "vscode"
                     :org "amiorin"
                     :repo "dotfiles-v3"
                     :branch "main"
                     :worktrees ["minipc"
                                 "ansible"]}
                    {:user "alberto"
                     :org "amiorin"
                     :repo "dotfiles-v3"
                     :branch "main"
                     :worktrees ["minipc"
                                 "ansible"]}])

(defn render
  [s repos]
  (p/render (slurp (io/resource s))
            {:repos repos}
            {:tag-open \<
             :tag-close \>}))

(comment
  (render "amiorin/dotfiles_v3/selmer/repos.yml" default-repos))

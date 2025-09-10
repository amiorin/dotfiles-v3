(ns amiorin.ansible
  (:require
   [clojure.java.io :as io]
   [selmer.parser :as p]))

(def default-users [{:name "vscode"
                     :uid "1001"}
                    {:name "alberto"
                     :uid "1002"}
                    {:name "amiorin"
                     :uid "1003"}])

(defn render
  [s users]
  (p/render (slurp (io/resource s))
            {:users users}
            {:tag-open \<
             :tag-close \>}))

(comment
  (render "amiorin/dotfiles_v3/selmer/default.config.yml" default-users)
  (render "amiorin/dotfiles_v3/selmer/inventory.ini" default-users)
  (render "amiorin/dotfiles_v3/selmer/inventory.ini" []))

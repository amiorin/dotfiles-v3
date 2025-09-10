(ns amiorin.users
  (:require
   [clojure.java.io :as io]
   [selmer.parser :as p]))

(def default-users [{:name "vscode"
                     :uid "1001"
                     :remove false}
                    {:name "alberto"
                     :uid "1002"
                     :remove false}
                    {:name "amiorin"
                     :uid "1003"
                     :remove false}])

(defn render
  [s users]
  (p/render (slurp (io/resource s))
            {:users (filter (complement :remove) users)
             :remove-users (filter :remove users)}
            {:tag-open \<
             :tag-close \>}))

(comment
  (render "amiorin/dotfiles_v3/selmer/default.config.yml" default-users)
  (render "amiorin/dotfiles_v3/selmer/inventory.ini" default-users)
  (render "amiorin/dotfiles_v3/selmer/inventory.ini" []))

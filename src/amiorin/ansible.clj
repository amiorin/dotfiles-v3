(ns amiorin.ansible
  (:require
   [clojure.java.io :as io]
   [selmer.parser :as p]))

(def users [{:name "vscode"
             :uid "1000"}
            {:name "alberto"
             :uid "1002"}
            {:name "amiorin"
             :uid "1003"}])

(defn render
  [s]
  (p/render (slurp (io/resource s))
            {:users users}))

(comment
  (render "amiorin/dotfiles_v3/selmer/default.config.yml")
  (render "amiorin/dotfiles_v3/selmer/inventory.ini"))

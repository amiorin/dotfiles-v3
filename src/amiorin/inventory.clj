(ns amiorin.inventory
  (:require
   [cheshire.core :refer [generate-string]]))

(defn default-opts
  []
  [{:name "ubuntu"
    :uid "1000"
    :doomemacs "68010af0906171e3c989fc19bcb3ba81f7305022"
    :remove true}
   {:name "vscode"
    :uid "1001"
    :doomemacs "68010af0906171e3c989fc19bcb3ba81f7305022"
    :remove true}
   {:name "alberto"
    :uid "1002"
    :doomemacs "68010af0906171e3c989fc19bcb3ba81f7305022"
    :remove false}
   {:name "amiorin"
    :uid "1003"
    :doomemacs "d6cdbb4d22a2db68e3d74b7f5abf5b1fd14f4de0"
    :remove false}])

(defn render
  [users]
  (let [users (filter (complement :remove) users)
        host "minipc"
        admin "alberto"
        hosts (reduce #(let [{:keys [name uid]} %2]
                         (assoc %1 name {:ansible_host host
                                         :ansible_user name
                                         :uid uid})) {} users)
        res {:all {:children {:admin {:hosts {:root {:ansible_host host
                                                     :ansible_user admin}}}
                              :users {:hosts hosts}}}}]
    (generate-string res {:pretty true})))

(comment
  (render (default-opts)))

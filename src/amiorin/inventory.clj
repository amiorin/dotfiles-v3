(ns amiorin.inventory
  (:require
   [cheshire.core :refer [generate-string]]))

(defn default-opts
  []
  [{:name "ubuntu"
    :uid "1000"
    :doomemacs "68010af0906171e3c989fc19bcb3ba81f7305022"
    :remove false}
   {:name "vscode"
    :uid "1001"
    :doomemacs "68010af0906171e3c989fc19bcb3ba81f7305022"
    :remove false}
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
    (let [sudoer "ubuntu"
          users (-> (filter (complement :remove) users)
                    (->> (map #(for [host ["minipc" "soyo"]]
                                 (assoc % :host host))))
                    flatten)
          admins (-> [{:ansible_user sudoer}]
                     (->> (map #(for [host ["minipc" "soyo"]]
                                  (-> %
                                      (merge {:host host
                                              :name "ubuntu"})))))
                     flatten)
          hosts-users (reduce #(let [{:keys [name uid host]} %2]
                                 (assoc %1 (format "%s@%s" name host) {:ansible_host host
                                                                       :ansible_user name
                                                                       :uid uid})) {} users)
          hosts-admins (reduce #(let [{:keys [name host]} %2]
                                  (assoc %1 (format "root@%s" host) {:ansible_host host
                                                                     :ansible_user name})) {} admins)
          res {:all {:children {:admin {:hosts hosts-admins}
                                :users {:hosts hosts-users}}}}]
      (generate-string res {:pretty true})))

(comment
  (render (default-opts)))

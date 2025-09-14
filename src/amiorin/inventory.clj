(ns amiorin.inventory
  (:require
   [cheshire.core :refer [generate-string]]))

(defn default-opts
  []
  {:sudoer "ubuntu"
   :hosts ["minipc" "soyo"]
   :users [{:name "ubuntu"
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
            :remove false}]})

(defn render
  [{:keys [sudoer hosts users]}]
  (let [users (-> (filter (complement :remove) users)
                  (->> (map #(for [host hosts]
                               (assoc % :host host))))
                  flatten)
        admins (-> [{:ansible_user sudoer}]
                   (->> (map #(for [host hosts]
                                (-> %
                                    (merge {:host host
                                            :name "ubuntu"})))))
                   flatten)
        users-hosts (reduce #(let [{:keys [name uid host]} %2]
                               (assoc %1 (format "%s@%s" name host) {:ansible_host host
                                                                     :ansible_user name
                                                                     :uid uid})) {} users)
        admins-hosts (reduce #(let [{:keys [name host]} %2]
                                (assoc %1 (format "root@%s" host) {:ansible_host host
                                                                   :ansible_user name})) {} admins)
        res {:all {:children {:admin {:hosts admins-hosts}
                              :users {:hosts users-hosts}}}}]
    (generate-string res {:pretty true})))

(comment
  (render (default-opts)))

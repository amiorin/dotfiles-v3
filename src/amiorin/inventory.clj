(ns amiorin.inventory
  (:require
   [cheshire.core :refer [generate-string]]))

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
  [users]
  (let [users (filter (complement :remove) users)
        host "minipc.afrino-bushi.ts.net"
        admin "vscode"
        hosts (reduce #(let [{:keys [name uid]} %2]
                         (assoc %1 name {:ansible_host host
                                         :ansible_user name
                                         :uid uid})) {} users)
        res {:all {:children {:admin {:hosts {:root {:ansible_host host
                                                     :ansible_user admin}}}
                              :users {:hosts hosts}}}}]
    (generate-string res {:pretty true})))

(comment
  (render default-users))

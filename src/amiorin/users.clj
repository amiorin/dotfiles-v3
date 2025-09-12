(ns amiorin.users
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

(defn render-inventory
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

(defn render-config
  [all-users]
  (let [atuin-login (System/getenv "ATUIN_LOGIN")
        users (filter (complement :remove) all-users)
        remove-users (filter :remove all-users)]
    (cond-> {:users users
             :remove_users remove-users
             :ssh_key "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHDKdUkY+SfRm6ttOz2EEZ2+i/zm+o1mpMOdMeGUr0t4 32617+amiorin@users.noreply.github.com"}
      atuin-login (assoc :atuin_login "\"{{ lookup('ansible.builtin.env', 'ATUIN_LOGIN') }}\"")
      true (generate-string {:pretty true}))))

(comment
  (render-inventory default-users)
  (render-config default-users))

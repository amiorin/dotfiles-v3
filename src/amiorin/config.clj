(ns amiorin.config
  (:require
   [amiorin.inventory :as inventory]
   [cheshire.core :refer [generate-string]]))

(defn default-opts
  ([]
   (default-opts (inventory/default-opts)))
  ([users]
   {:users (filter (complement :remove) users)
    :remove_users (filter :remove users)
    :ssh_key "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHDKdUkY+SfRm6ttOz2EEZ2+i/zm+o1mpMOdMeGUr0t4 32617+amiorin@users.noreply.github.com"}))

(defn render
  [config]
  (let [atuin-login (System/getenv "ATUIN_LOGIN")]
    (cond-> config
      atuin-login (assoc :atuin_login "{{ lookup('ansible.builtin.env', 'ATUIN_LOGIN') }}")
      true (generate-string {:pretty true}))))

(comment
  (render (default-opts)))

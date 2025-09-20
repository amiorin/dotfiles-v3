(ns amiorin.render
  (:require
   [cheshire.core :refer [generate-string]]))

(defn packages
  [{:keys [packages]}]
  (-> (for [[package cli] packages]
        [{:name (format "Add devbox package %s" package)
          :args {:creates (format ".local/share/devbox/global/default/.devbox/nix/profile/default/bin/%s" cli)}
          "ansible.builtin.shell" (format ". /etc/profile.d/nix.sh && devbox global add --disable-plugin %s" package)}])
      flatten
      (generate-string {:pretty true})))

(defn config
  [{:keys [config]}]
  (generate-string config {:pretty true}))

(defn ssh-config
  [{:keys [hosts]}]
  (-> (for [host hosts]
        [{:name (format "Add a new host entry using blockinfile for %s" host)
          "ansible.builtin.blockinfile" {:path "~/.ssh/config"
                                         :create true
                                         :block (format "Host %s
  Hostname %s.afrino-bushi.ts.net
  User ubuntu
  ForwardAgent yes " host host)
                                         :marker (format "# {mark} ANSIBLE MANAGED BLOCK FOR %s" host)
                                         :state "present"}}])
      flatten
      (generate-string {:pretty true})))

(defn inventory
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

(defn repos
  [{:keys [repos]}]
  (-> (for [{:keys [user org repo branch worktrees]} repos]
        (let [when-p (format "inventory_hostname.startswith(\"%s\")" user)]
          [{:name (format "Clone repo %s/%s" org repo)
            "ansible.builtin.shell" (format "ssh -o StrictHostKeyChecking=accept-new git@github.com || true && git clone git@github.com:%s/%s %s/%s" org repo repo branch)
            :args {:chdir (format "code/personal")
                   :creates (format "%s/%s" repo branch)}
            :when when-p}
           (for [worktree worktrees]
             {:name (format "Create the worktree %s for repo %s/%s" worktree org repo)
              "ansible.builtin.shell" (format "git fetch --all --tags && git worktree add ../%s %s" worktree worktree)
              :args {:chdir (format "code/personal/%s/%s" repo branch)
                     :creates (format "../%s" worktree)}
              :when when-p})]))
      flatten
      (generate-string {:pretty true})))

(defn ->opts []
  (let [sudoer "ubuntu"
        hosts ["firebat" "soyo"]
        users [{:name "ubuntu"
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
                :remove false}]
        config {:users (filter (complement :remove) users)
                :remove_users (filter :remove users)
                :atuin_login "{{ lookup('ansible.builtin.env', 'ATUIN_LOGIN') }}"
                :ssh_key "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHDKdUkY+SfRm6ttOz2EEZ2+i/zm+o1mpMOdMeGUr0t4 32617+amiorin@users.noreply.github.com"}
        repos (-> (into [] (for [[repo worktrees] [["dotfiles-v3" ["ansible" "babashka"]]
                                                   ["albertomiorin.com" ["albertomiorin" "big-config"]]
                                                   ["big-config" ["deps-new"]]
                                                   ["big-container" []]
                                                   ["rama-jdbc" []]]]
                             {:user "vscode"
                              :org "amiorin"
                              :repo repo
                              :branch "main"
                              :worktrees worktrees}))
                  (into [{:user "ubuntu"
                          :org "amiorin"
                          :repo "dotfiles-v3"
                          :branch "main"
                          :worktrees ["ansible"
                                      "babashka"]}
                         {:user "alberto"
                          :org "amiorin"
                          :repo "dotfiles-v3"
                          :branch "main"
                          :worktrees ["ansible"
                                      "babashka"]}
                         {:user "amiorin"
                          :org "amiorin"
                          :repo "dotfiles-v3"
                          :branch "main"
                          :worktrees ["ansible"
                                      "babashka"]}]))
        packages (->> ["fish"
                       "emacs"
                       "zellij"
                       "starship"
                       "direnv"
                       "gh"
                       "fd"
                       "fzf"
                       "atuin"
                       "just"
                       "git"
                       "cmake"
                       "libtool"
                       "socat"
                       "zoxide"
                       "pixi"
                       "eza"
                       "zip"
                       "unzip"
                       "d2"
                       "clojure-lsp"]
                      (mapv (fn [x] [x x]))
                      (into [["ripgrep" "rg"]]))]
    {:repos repos
     :sudoer sudoer
     :hosts hosts
     :users users
     :config config
     :packages packages}))

(comment
  (packages (->opts))
  (repos (->opts))
  (inventory (->opts))
  (config (->opts))
  (ssh-config (->opts)))

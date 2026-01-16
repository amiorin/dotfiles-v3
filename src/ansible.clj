(ns ansible
  (:require
   [babashka.fs :as fs]
   [babashka.process :as p]
   [cheshire.core :as json]
   [clj-yaml.core :as yaml]))

(defn generate-string
  [data]
  (yaml/generate-string data :dumper-options {:flow-style :block}))

(defn packages
  [{:keys [packages]}]
  (-> (for [[package cli] packages]
        [{:name (format "Add devbox package %s" package)
          :args {:creates (format ".local/share/devbox/global/default/.devbox/nix/profile/default/bin/%s" cli)}
          "ansible.builtin.shell" (format ". /etc/profile.d/nix.sh && devbox global add --disable-plugin %s" package)}])
      flatten
      generate-string))

(defn config
  [{:keys [config]}]
  (generate-string config))

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
      generate-string))

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
                                            :name sudoer})))))
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
    (json/generate-string res {:pretty true})))

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
      generate-string))

(defn render
  [target data]
  (case target
    :packages (packages data)
    :repos (repos data)
    :ssh-config (ssh-config data)
    :inventory (inventory data)
    :config (config data)))

(defn data-fn [data {:keys [:alpha/target-prefix]}]
  (let [sudoer "root"
        main-user "ubuntu"
        tofu-dir (str (fs/path target-prefix "tofu"))
        hosts [(try
                 (-> (p/shell {:dir tofu-dir
                               :out :string} "tofu output --json")
                     :out
                     (json/parse-string keyword)
                     :ipv4_address
                     :value)
                 (catch Throwable _
                   "77.42.91.213"))]
        users [{:name main-user
                :uid "1000"
                :doomemacs "6ea4332b854d311d7ec8ae6384fae8d9871f5730"
                :remove false}]
        config {:users (filter (complement :remove) users)
                :remove_users (filter :remove users)
                :atuin_login "{{ lookup('ansible.builtin.env', 'ATUIN_LOGIN') }}"
                :ssh_key "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHDKdUkY+SfRm6ttOz2EEZ2+i/zm+o1mpMOdMeGUr0t4 32617+amiorin@users.noreply.github.com"}
        repos (-> (into [] (for [[repo worktrees] [["dotfiles-v3" ["ansible" "babashka"]]
                                                   ["albertomiorin.com" ["albertomiorin" "big-config"]]
                                                   ["big-config" ["deps-new" "hyperlith" "hyperlith-counter"]]
                                                   ["big-container" []]
                                                   ["rama-jdbc" []]]]
                             {:user main-user
                              :org "amiorin"
                              :repo repo
                              :branch "main"
                              :worktrees worktrees}))
                  (into (for [[repo worktrees] [["hyperlith" []]]]
                          {:user main-user
                           :org "amiorin"
                           :repo repo
                           :branch "master"
                           :worktrees worktrees})))
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
                       "clojure-lsp"
                       "btop"
                       "clj-kondo"]
                      (mapv (fn [x] [x x]))
                      (into [["ripgrep" "rg"]]))]
    (merge data {:target-prefix target-prefix
                 :repos repos
                 :sudoer sudoer
                 :hosts hosts
                 :users users
                 :config config
                 :packages packages})))

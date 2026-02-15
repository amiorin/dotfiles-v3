(ns hetzner)

(defn data-fn [_ _]
  (let [sudoer "root"
        main-user "ubuntu"
        hosts ["95.217.164.175"]
        users [{:name main-user
                :uid "1000"
                :doomemacs "6ea4332b854d311d7ec8ae6384fae8d9871f5730"
                :remove false}
               {:name "vscode"
                :uid "1001"
                :doomemacs "6ea4332b854d311d7ec8ae6384fae8d9871f5730"
                :remove false}]
        config {:users (filter (complement :remove) users)
                :remove_users (filter :remove users)
                :atuin_login "{{ lookup('ansible.builtin.env', 'ATUIN_LOGIN') }}"
                :ssh_key "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIHDKdUkY+SfRm6ttOz2EEZ2+i/zm+o1mpMOdMeGUr0t4 32617+amiorin@users.noreply.github.com"}
        repos (-> (into [] (for [user [main-user "vscode"]]
                             (for [[repo worktrees] [["dotfiles-v3" ["ansible" "babashka"]]
                                                     ["albertomiorin.com" ["albertomiorin" "big-config"]]
                                                     ["big-config" ["deps-new" "hyperlith" "hyperlith-counter"]]
                                                     ["big-container" []]
                                                     ["rama-jdbc" []]]]
                               {:user user
                                :org "amiorin"
                                :repo repo
                                :branch "main"
                                :worktrees worktrees})))
                  (into (for [user [main-user "vscode"]]
                          (for [[repo worktrees] [["hyperlith" []]]]
                            {:user user
                             :org "amiorin"
                             :repo repo
                             :branch "master"
                             :worktrees worktrees})))
                  flatten)
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
    {:repos repos
     :sudoer sudoer
     :hosts hosts
     :users users
     :config config
     :packages packages}))

(comment
  (data-fn nil nil))

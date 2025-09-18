(ns config)

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
        repos (-> (into [] (for [[repo worktrees] [["dotfiles-v3" ["minipc" "ansible" "babashka"]]
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
                          :worktrees ["minipc"
                                      "ansible"]}
                         {:user "alberto"
                          :org "amiorin"
                          :repo "dotfiles-v3"
                          :branch "main"
                          :worktrees ["minipc"
                                      "ansible"]}
                         {:user "amiorin"
                          :org "amiorin"
                          :repo "dotfiles-v3"
                          :branch "main"
                          :worktrees ["minipc"
                                      "ansible"]}]))
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
  (->opts))

(ns amiorin.repos
  (:require
   [cheshire.core :refer [generate-string]]))

(def default-repos
  (-> (into [] (for [[repo worktrees] [["dotfiles-v3" ["minipc" "ansible"]]
                                       ["albertomiorin.com" ["albertomiorin" "big-config"]]
                                       ["big-config" ["deps-new"]]
                                       ["big-container" []]
                                       ["rama-jdbc" []]]]
                 {:user "vscode"
                  :org "amiorin"
                  :repo repo
                  :branch "main"
                  :worktrees worktrees}))
      (into [{:user "alberto"
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
                          "ansible"]}])))

(defn render
  [repos]
  (-> (for [{:keys [user org repo branch worktrees]} repos]
        [{:name (format "Clone repo %s/%s" org repo)
          "ansible.builtin.shell" (format "ssh -o StrictHostKeyChecking=accept-new  git@github.com || true && git clone git@github.com:%s/%s %s/%s" org repo repo branch)
          :args {:chdir (format "code/personal")
                 :creates (format "%s/%s" repo branch)}
          :when (format "inventory_hostname == \"%s\"" user)}
         (for [worktree worktrees]
           {:name (format "Create the worktree %s for repo %s/%s" worktree org repo)
            "ansible.builtin.shell" (format "git worktree add ../%s %s" worktree worktree)
            :args {:chdir (format "code/personal/%s/%s" repo branch)
                   :creates (format "../%s" worktree)}
            :when (format "inventory_hostname == \"%s\"" user)})])
      flatten
      (generate-string {:pretty true})))

(comment
  (render default-repos))

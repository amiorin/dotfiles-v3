(ns workflow
  "
  How to make workflows developed indipendently composable.

  * **shell-workflow**: Is a workflow that render template files and execute one
  or more commands like Terraform or Ansible. It takes `params`.
  * **comp-workflow**: Is a workflow that orchestrate multiple `shell-workflows`.

  ### Usage
  ```text
  bb <shell-workflow> <step|cmd>+ [-- last-command]
  bb <comp-workflow> <step>+
  ```

  ### Example
  ```sh
  bb shell-wf-a render tofu:init -- tofu apply -auto-approve
  bb shell-wf-b render ansible-playbook:main.yml
  bb comp-wf-c create
  ```

  `comp-wf-c` is the composition of `shell-wf-a` and `shell-wf-b` and it only
  supports create and delete. The development happens in `shell-wf-a` and
  `shell-wf-b`.

  ### Functions
  * `run-steps` is the dynamic workflow.
  * `parse-shell-args` is parsing a str or vec of args.
  * `parse-comp-args` is parsing a str or vec of args.
  * `prepare` is the common logic for render and exec workflows.

  ### Options
  * ::path-fn
  * ::params
  * ::name
  * ::dirs

  ### Naming convention for workflows
  * `tofu*` is the lib version that requires the arguments `step-fns` and
  `opts`.
  * `tofu` is the Babashka version that requires the arguments `args` and an
  optional `opts`.

  Note: The booking of resource names and resources is not part of BigConfig
  Workflow.

  ### Sharing data between workflows
  The problem with terraform is the coupling with a HCL able to read any
  property of any resource. For example, AWS and Hetzner computation have
  different way to output the IP address. If another tool like Ansible requires
  the IP address, the coupling will be high.  This will not happen in BigConfig
  Workflow. Replacing the first shell-workflow from Hetzner to AWS, will not
  require changes to the second shell-workflow using Ansible because the code is
  implemented a step of the comp-workflow.  BigConfig Workflow has `::params`
  and the `comp-workflow` is responsible to adapt the `outputs` of the previous
  `shell-workflow` to the `::params` of the next `shell-workflow` using `::dirs`
  to execute discover the outputs using commands like `tofu output --json`.
  "
  (:require
   [big-config :as bc]
   [big-config.core :as core]
   [big-config.git :as git]
   [big-config.lock :as lock]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.unlock :as unlock]
   [com.rpl.specter :as s]
   [clojure.string :as str]))

(defn pstar-workflow
  [s-or-seq]
  (loop [xs s-or-seq
         token nil
         steps []
         cmds []]
    (cond
      (string? xs)
      (let [xs (-> (str/trim xs)
                   (str/split #"\s+"))]
        (recur (rest xs) (first xs) steps cmds))

      (and (sequential? xs)
           (seq xs)
           (nil? token))
      (recur (rest xs) (first xs) steps cmds)

      (#{"lock" "git-check" "build" "render" "exec" "git-push" "unlock-any"} token)
      (let [steps (into steps [token])]
        (recur (rest xs) (first xs) steps cmds))

      (= "--" token)
      (if (seq xs)
        (recur '() (str/join ":" xs) steps cmds)
        (throw (ex-info "-- cannot be without a command" {})))

      token
      (let [steps (if (some #{"exec"} steps)
                    steps
                    (into steps ["exec"]))
            cmds (into cmds [(str/replace token ":" " ")])]
        (recur (rest xs) (first xs) steps cmds))

      :else
      {::steps steps
       ::run/cmds cmds})))

(comment
  (pstar-workflow "tofu:init -- tofu plan"))

(defn run-steps*
  ([step-fns {:keys [::steps] :as opts}]
   (loop [steps (map keyword steps)
          opts opts]
     (let [{:keys [::bc/exit] :as opts} (case (first steps)
                                          :lock (lock/lock step-fns opts)
                                          :git-check (git/check step-fns opts)
                                          :render (render/templates step-fns opts)
                                          :exec (run/run-cmds step-fns opts)
                                          :git-push (git/git-push opts)
                                          :unlock-any (unlock/unlock-any step-fns opts))]
       (cond
         (and (seq (rest steps))
              (or (= exit 0)
                  (nil? exit))) (recur (rest steps) opts)
         :else opts)))))

(def run-steps
  (core/->workflow {:first-step ::start
                    :wire-fn (fn [step step-fns]
                               (case step
                                 ::start [(partial run-steps* step-fns) ::end]
                                 ::end [identity]))}))

(do
  (defn keyword->path [kw]
    (let [full-str (if-let [ns (namespace kw)]
                     (str ns "/" (name kw))
                     (name kw))]
      (-> full-str
          (str/replace "." "/"))))

  (defn prepare
    [{:keys [::name] :as opts} {:keys [::path-fn ::params] :as overrides}]
    (let [path-fn (or path-fn #(format ".dist/%s" (-> % ::name keyword->path)))
          opts (merge opts overrides)
          dir (path-fn opts)
          opts (->> opts
                    (s/transform [::render/templates s/ALL] #(merge % params
                                                                    {:target-dir dir}))
                    (s/setval [::run/shell-opts :dir] dir)
                    (s/transform [::dirs] #(assoc % name dir)))]
      opts))
  (defn tofu*
    [step-fns opts]
    (let [opts (prepare {::name ::tofu
                         ::render/templates [{:template "alpha"
                                              :overwrite true
                                              :transform [["tofu"
                                                           :raw]]}]}
                        opts)]
      opts
      #_(run-steps step-fns opts)))
  (into (sorted-map) (tofu* [] {::params {:parameter-a 1}})))

(do
  (defn tofu
    [args & [opts]]
    (let [opts (merge (pstar-workflow args)
                      {::params {:foo 1
                                 :bar 2}}
                      opts)]
      (tofu* [] opts)))
  (into (sorted-map) (tofu "render -- tofu init" {::bc/env :repl})))

(defn pstar-resource
  [any]
  (loop [xs any
         token nil
         cmds []]
    (Thread/sleep 100)
    (println xs token cmds)
    (cond
      (string? xs)
      (let [xs (-> (str/trim xs)
                   (str/split #"\s+"))]
        (recur (rest xs) (first xs) cmds))

      (map? xs)
      (if-let [args (::args xs)]
        (recur args token cmds)
        xs)

      (and (seq xs)
           (nil? token))
      (recur (rest xs) (first xs) cmds)

      (#{"create" "delete"} token)
      (let [cmds (into cmds [(keyword token)])]
        (recur (rest xs) (first xs) cmds))

      :else
      (cond
        (nil? token)
        {::cmds cmds
         ::bc/exit 0
         ::bc/err nil}

        :else
        (throw (ex-info (format "Unknown cmd %s" token) {}))))))

#_(defn resource
    [any & [opts step-fns]]
    (let [{:keys [::cmds ::resource-name]} (pstar-resource any)
          opts (merge {::bc/env :repl
                       ::working-dir-prefix (-> (fs/path  ".dist/" resource-name))}
                      opts)
          xs (mapcat #(case %
                        #_#_:create [(tofu "render tofu:init tofu:apply:-auto-approve -- big-iron cesar-ford" opts)
                                     (ansible "render ansible-playbook:main.yml -- big-iron cesar-ford" opts)]
                        :create [(tofu "render -- big-iron cesar-ford" opts)
                                 (ansible "render -- big-iron cesar-ford" opts)]
                        :delete [(tofu "render tofu:destroy:-auto-approve -- big-iron cesar-ford" opts)]) cmds)
          step-fns (if (= step-fns :default)
                     [(->working-dir-step-fn)
                      (->copy-data-step-fn)
                      step/print-step-fn
                      (step-fns/->exit-step-fn ::step/end xs)
                      (step-fns/->print-error-step-fn ::step/end)]
                     step-fns)]
      (if step-fns
        ((step/->run-steps) step-fns xs)
        xs)))

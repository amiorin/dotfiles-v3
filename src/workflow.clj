(ns workflow
  "How to compose two workflow using a new interface.
  The workflow interface is
  [any & [opts step-fns]]
  If `step-fns` is vector or `:default` the workflow is executed.
  If `step-fns` is `nil`, `any` is converted to `opts`, merged with the provided `opts` and return.
  The new interface in Babashka is:
  ```
  bb workflow-a ...
  bb workflow-b ...
  bb workflow-c=a+b ...
  ```
  The step-fns uses `atoms` to transform and share options between workflows.

  New options
  * ::working-dir: it must be unique per workflow
  * ::working-dir-prefix: a prefix
  * ::working-dirs: a mapping between names and dirs, to be able to invoke `tofu output --json`

  New options revised
  * ::workflow-name: ::ansible
  * ::workflow-path-fn: a function that returns the working directory of the workflow
  * ::workflow-dirs: a mapping between the name and dirs, to be able to invoke
  `tofu output --json` inside a step function.
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

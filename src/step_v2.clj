(ns step-v2
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
   [babashka.fs :as fs]
   [babashka.process :as p]
   [big-config :as bc]
   [big-config.render :as render]
   [big-config.run :as run]
   [big-config.step :as step]
   [big-config.step-fns :as step-fns]
   [cheshire.core :as json]
   [clojure.string :as str]
   [com.rpl.specter :refer [ALL FIRST setval transform]]))

(do
  (defn keyword->source-path [kw]
    (let [full-str (if-let [ns (namespace kw)]
                     (str ns "/" (name kw))
                     (name kw))]
      (-> full-str
          (str/replace "-" "_")
          (str/replace "." "/"))))
  (-> ::ansible
      keyword->source-path))

(defn pstar-steps
  [any]
  (loop [xs any
         token nil
         steps []
         cmds []
         module nil
         profile nil
         global-args nil]
    (cond
      (string? xs)
      (let [xs (-> (str/trim xs)
                   (str/split #"\s+"))]
        (recur (rest xs) (first xs) steps cmds module profile global-args))

      (map? xs)
      (if-let [args (::args xs)]
        (recur args token steps cmds module profile global-args)
        xs)

      (and (sequential? xs)
           (seq xs)
           (nil? token)
           (nil? module))
      (recur (rest xs) (first xs) steps cmds module profile global-args)

      (= "--" token)
      (let [module (first xs)
            profile (second xs)
            xs (seq (drop 2 xs))
            global-args (if xs
                          (str/join ":" xs)
                          nil)
            cmds (if (seq cmds)
                   (mapv #(apply str % (if global-args
                                         [":" global-args]
                                         nil)) cmds)
                   (if global-args
                     [global-args]
                     []))
            cmds (mapv #(str/replace % ":" " ") cmds)]
        (assert module "Module is not defined")
        (assert profile "Profile is not defined")
        {::step/steps steps
         ::run/cmds cmds
         ::step/module module
         ::step/profile profile})

      (#{"lock" "git-check" "build" "render" "exec" "git-push" "unlock-any"} token)
      (let [steps (into steps [token])]
        (recur (rest xs) (first xs) steps cmds module profile global-args))

      token
      (let [steps (if (some #{"exec"} steps)
                    steps
                    (into steps ["exec"]))
            cmds (into cmds [token])]
        (recur (rest xs) (first xs) steps cmds module profile global-args))

      :else
      (throw (ex-info "-- not found" {})))))

(comment
  (pstar-steps "render -- module profile"))

(defn prepare
  "bb tofu render tofu:init lock tofu:apply:-auto-approve -- big-iron cesar-ford"
  [any & [opts]]
  (let [opts (merge (pstar-steps any)
                    {::bc/env :repl}
                    opts)]
    opts))

(defn tofu
  "bb tofu render tofu:init lock tofu:apply:-auto-approve -- big-iron cesar-ford"
  [any & [opts step-fns]]
  (let [opts (merge (prepare any opts)
                    {::working-dir "tofu"
                     ::render/templates [{:template "alpha"
                                          :target-dir ".dist/tofu"
                                          :overwrite true
                                          :transform [["tofu"
                                                       :raw]]}]})
        step-fns (if (= step-fns :default)
                   [step/print-step-fn
                    (step-fns/->exit-step-fn ::step/end)
                    (step-fns/->print-error-step-fn ::step/end)]
                   step-fns)]
    (if step-fns
      ((step/->run-steps) step-fns opts)
      opts)))

(comment
  (tofu "render pwd ls -- foo bar" {}))

(defn ansible
  "bb ansible render ansible-playbook:main.yml unlock-any -- big-iron cesar-ford"
  [any & [opts step-fns]]
  (let [opts (merge (prepare any opts)
                    {::working-dir "ansible"
                     ::render/templates [{:template "alpha"
                                          :target-dir ".dist/ansible"
                                          :data-fn 'ansible/data-fn
                                          :overwrite true
                                          :transform [["ansible"
                                                       :raw]
                                                      ['ansible/render "roles/users/tasks"
                                                       {:packages "packages.yml"
                                                        :repos "repos.yml"
                                                        :ssh-config "ssh-config.yml"}
                                                       :raw]
                                                      ['ansible/render
                                                       {:inventory "inventory.json"
                                                        :config "default.config.yml"}
                                                       :raw]]}]})
        step-fns (if (= step-fns :default)
                   [step/print-step-fn
                    (step-fns/->exit-step-fn ::step/end)
                    (step-fns/->print-error-step-fn ::step/end)]
                   step-fns)]
    (if step-fns
      ((step/->run-steps) step-fns opts)
      opts)))

(comment
  (into (sorted-map) (ansible "render -- foo bar" {} :default)))

(defn pstar-resource
  [any]
  (loop [xs any
         token nil
         cmds []
         resource-name nil]
    (cond
      (string? xs)
      (let [xs (-> (str/trim xs)
                   (str/split #"\s+"))]
        (recur (rest xs) (first xs) cmds resource-name))

      (map? xs)
      (if-let [args (::args xs)]
        (recur args token cmds resource-name)
        xs)

      (and (sequential? xs)
           (nil? token))
      (recur (rest xs) (first xs) cmds resource-name)

      (#{"create" "delete"} token)
      (let [cmds (into cmds [(keyword token)])]
        (recur (rest xs) (first xs) cmds resource-name))

      (and (seq cmds)
           (#{"--resource-name"} token))
      (let [resource-name (first xs)]
        (assert resource-name "Resource name is not defined")
        {::resource-name (first xs)
         ::cmds cmds
         ::bc/exit 0
         ::bc/err nil})

      :else
      (cond
        (nil? token)
        (throw (ex-info "--resource-name is not defined" {}))

        (#{"--resource-name"} token)
        (throw (ex-info "command is not defined (create|delete)" {}))

        :else
        (throw (ex-info (format "Unknown cmd %s" token) {}))))))

(comment
  (pstar-resource {})
  (pstar-resource {::args "create --resource-name foo"})
  (pstar-resource "create --resource-name foo")
  (pstar-resource (str/split "create --resource-name foo" #" ")))

(defn ->working-dir-step-fn []
  (let [dirs (atom {})]
    (fn [f step {:keys [::working-dir-prefix ::working-dir] :as opts}]
      (when (nil? working-dir-prefix)
        (throw (ex-info "working-dir-prefix must be defined" opts)))
      (when (nil? working-dir)
        (throw (ex-info "working-dir must be defined" opts)))
      (let [dir (-> (fs/path working-dir-prefix working-dir) str)
            _ (swap! dirs assoc (keyword working-dir) dir)
            opts (->> (assoc opts ::working-dirs @dirs)
                      (setval [::render/templates ALL :target-dir] dir)
                      (setval [::run/shell-opts :dir] dir)
                      (setval [::working-dirs] @dirs))]
        (f step opts)))))

(comment
  (let [working-dir-step-fn (->working-dir-step-fn)]
    (working-dir-step-fn (fn [_ opts] opts) nil {::render/templates [{} {}]
                                                 ::working-dir-prefix ".dist"
                                                 ::working-dir "tofu"})
    (working-dir-step-fn (fn [_ opts] opts) nil {::render/templates [{} {}]
                                                 ::working-dir-prefix ".dist"
                                                 ::working-dir "ansible"})))

(-> (p/shell {:dir ".dist/cesar-ford/tofu"
              :out :string} "tofu output --json")
    :out
    (json/parse-string keyword)
    :ipv4_address
    :value)

(defn ->copy-data-step-fn []
  (let [data (atom {})]
    (fn [f step {:keys [::working-dir ::working-dirs] :as opts}]
      (when (nil? working-dir)
        (throw (ex-info "working-dir must be defined" opts)))
      (when (and (= step :big-config.step/end)
                 (= working-dir "tofu"))
        (let [tofu-dir (working-dirs :tofu)
              ip (try
                   (-> (p/shell {:dir (working-dirs :tofu)
                                 :out :string} "tofu output --json")
                       :out
                       (json/parse-string keyword)
                       :ipv4_address
                       :value)
                   (catch Throwable _
                     "77.42.91.213"))]
          (swap! data merge {:ipv4-address ip})))
      (tap> @data)
      (let [opts (if (and (= step :big-config.step/start)
                          (= working-dir "ansible"))
                   (transform [::render/templates FIRST] #(merge % @data) opts)
                   opts)]
        (f step opts)))))

(comment
  (->> (resource "create --resource-name cesar-ford" {::bc/env :repl} :default)
       (map #(into (sorted-map) %))))

(comment
  (let [opts {::render/templates [{} {}]
              ::working-dir-prefix ".dist"
              ::working-dir "ansible"}]
    (transform [::render/templates FIRST] #(merge % {:foo :bar}) opts)))

(defn resource
  "bb resource create --resource-name cesar-ford"
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

(comment
  (->> (resource "create --resource-name cesar-ford" {::bc/env :repl} :default)
       (map #(into (sorted-map) %))))

(ns amiorin.dotfiles-v3-test
  (:require
   [amiorin.dotfiles-v3 :refer [run-steps]]
   [babashka.process :refer [shell]]
   [big-config :as bc]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [org.corfield.new]))

(deftest valid-template-test
  (testing "template.edn is valid."
    (let [template (edn/read-string (slurp (io/resource "amiorin/dotfiles_v3/template.edn")))]
      (is (s/valid? :org.corfield.new/template template)
          (s/explain-str :org.corfield.new/template template)))))

(defn discover
  []
  (-> (shell {:out :string} "find dist -mindepth 2 -maxdepth 2 -type d")
      :out
      str/trim
      (str/split #"\n")
      (->> (map #(str/split % #"/"))
           (map rest))))

(deftest stability
  (testing "working directory is clean after running all modules"
    (doseq [[module profile] (discover)]
      (run-steps (format "build -- %s %s" module profile) [] {::bc/env :repl}))
    (as-> (shell {:continue true} "git diff --quiet") $
      (:exit $)
      (is (= $ 0) (:out (shell {:continue true
                                :out :string} "git status"))))))

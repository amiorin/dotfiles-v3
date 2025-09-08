(ns amiorin.dotfiles-v3-test
  (:require
   [amiorin.dotfiles-v3 :refer [run-steps]]
   [babashka.fs :as fs]
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

(deftest stability
  (testing "working directory is clean after running all modules"
    (let [modules (->> (fs/list-dir "dist/prod")
                       (map str)
                       (map #(str/replace-first % "dist/prod/" "")))]
      (doseq [module modules]
        (run-steps (format "build -- %s prod" module) [] {::bc/env :repl}))
      (as-> (shell {:continue true} "git diff --quiet") $
        (:exit $)
        (is (= $ 0) "The working directory is not clean")))))

(ns amiorin.dotfiles-v3-test
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [org.corfield.new]))

(deftest valid-template-test
  (testing "template.edn is valid."
    (let [template (edn/read-string (slurp (io/resource "amiorin/dotfiles_v3/template.edn")))]
      (is (s/valid? :org.corfield.new/template template)
          (s/explain-str :org.corfield.new/template template)))))

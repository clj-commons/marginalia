(ns marginalia.test.multidoc
  (:require marginalia.core)
  (:use clojure.test)
  (:use [clojure.java.io :only (file)])
  (:use [clojure.contrib.java-utils :only (delete-file-recursively)]))

(def multi-page-project (file "test_projects" "multi_page"))
(def test-project-src (file multi-page-project "src"))
(def test-project-target (file multi-page-project "docs"))

(use-fixtures :each (fn [f]
                      (delete-file-recursively test-project-target true)
                      (.mkdirs test-project-target)
                      (f)))
                      ;;(delete-file-recursively test-project-target true)))

(def test-metadata {
  :dependencies [["some/dep" "0.0.1"]]
  :description "Test project"
  :name "test"
  :dev-dependencies []
  :version "0.0.1"
})

(defn run-marginalia [source-dir output-dir]
  (binding [marginalia.html/*resources* ""]
    (marginalia.core/multidoc! output-dir
                               (marginalia.core/find-clojure-file-paths source-dir)
                               test-metadata)))

(defn files-in [dir]
  (seq (.listFiles (file dir))))

(deftest test-multi-page-generation
  (do (run-marginalia test-project-src test-project-target)
      (is (= (count (files-in test-project-target))
             (+ (count (files-in test-project-src)) 1))))) ;; Additional index file

(ns marginalia.test.core
  (:require marginalia.core)
  (:use clojure.test))

(deftest parse-project-file-simple
  (is (= "project-name" (:name (marginalia.core/parse-project-file "test/marginalia/test/multi-def-project.clj.txt")))))

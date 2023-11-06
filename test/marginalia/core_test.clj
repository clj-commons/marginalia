(ns marginalia.core-test
  (:require
   [clojure.test :refer :all]
   [marginalia.core :as core]))

(deftest parse-project-file-simple
  (is (= "project-name"
         (:name (marginalia.core/parse-project-file "test/marginalia/resources/multi-def-project.clj.txt")))))

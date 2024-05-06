(ns marginalia.core-test
  (:require
   [clojure.test :refer :all]
   [marginalia.core :as marginalia]))

(deftest parse-project-file-simple
  (is (= "project-name"
         (:name (marginalia/parse-project-file "test/marginalia/resources/multi-def-project.clj.txt")))))

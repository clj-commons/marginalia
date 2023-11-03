(ns marginalia.uberdoc-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [marginalia.core :as core]
   [marginalia.test.helpers :refer :all]))

(deftest single-page-test
  (with-project "single_page"
    (fn [source-dir output-dir metadata]
      (marginalia.core/uberdoc! (io/file output-dir "index.html")
                                (find-clojure-file-paths source-dir)
                                metadata))

    (is (= 1 number-of-generated-pages))))

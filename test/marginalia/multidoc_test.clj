(ns marginalia.multidoc-test
  (:require
   [clojure.test :refer :all]
   [marginalia.core :as core]
   [marginalia.test.helpers :refer :all]))

(deftest multi-page-test
  (with-project "multi_page"
    (fn [source-dir output-dir metadata]
      (core/multidoc! output-dir
                      (find-clojure-file-paths source-dir)
                      metadata))

    (is (= number-of-generated-pages 3))))

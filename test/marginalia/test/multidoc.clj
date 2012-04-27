(ns marginalia.test.multidoc
  (:require marginalia.core)
  (:use clojure.test)
  (:use marginalia.test.helpers))

(with-project "multi_page"
  (fn [source-dir output-dir metadata]
    (marginalia.core/multidoc! output-dir
                               (find-clojure-file-paths source-dir)
                               metadata))

  (is (= number-of-generated-pages 3)))

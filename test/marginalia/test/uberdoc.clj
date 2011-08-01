(ns marginalia.test.uberdoc
  (:require marginalia.core)
  (:use marginalia.test.helpers)
  (:use clojure.test))

(with-project "single_page"
  (fn [source-dir output-dir metadata]
    (marginalia.core/uberdoc! (clojure.java.io/file output-dir "index.html")
                              (marginalia.core/find-clojure-file-paths source-dir)
                              metadata))

  (is (= number-of-generated-pages 1)))

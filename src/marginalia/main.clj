(ns marginalia.main
  (:use [marginalia.html :only [*resources*]]
        [marginalia.core :only [run-marginalia]])
  (:gen-class))

(defn -main
  "The main entry point into Marginalia."
  [& sources]
  (binding [*resources* ""]
    (run-marginalia sources)))

;; # Example Usage
(comment
  ;; Command line example
  (-main "./src/marginalia/core.clj" "./src/marginalia/html.clj")

  ;; This will find all marginalia source files, and then generate an uberdoc.
  (apply -main (find-clojure-file-paths "./src"))

;; Move these to tests
  (merge-line {:docstring-text "hello world" :line 3} {:docs ["stuff"]})
  (merge-line {:code-text "(defn asdf" :line 4} {:docs ["stuff"]})
  (merge-line {:docs-text "There's only one method in this module", :line 4} {}))
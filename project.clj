(defproject marginalia "0.9.2-SNAPSHOT"
  :description "lightweight literate programming for clojure -- inspired by [docco](http://jashkenas.github.com/docco/)"
;;  :main marginalia.main
  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [org.clojure/clojurescript "1.7.228"]
   [org.clojure/tools.namespace "0.2.10"]
   [org.clojure/tools.cli "0.3.3"]
   [org.markdownj/markdownj "0.3.0-1.0.2b4"]
   [de.ubercode.clostache/clostache "1.4.0"]]

  :resource-paths ["vendor"]

  ;;Needed for testing Latex equation formatting. You must download
  ;;and install MathJax in you doc directory.
  :marginalia {:javascript ["mathjax/MathJax.js"]}

  :aliases {"docs" ["run" "-m" "marginalia.main"
                    ;; leiningen/marg.clj ??
                    "src/marginalia/core.clj"
                    "src/marginalia/html.clj"
                    ;; "src/marginalia/tasks.clj"
                    "src/problem_cases/general.clj"]})

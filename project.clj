(defproject marginalia "0.8.0-SNAPSHOT"
  :description "lightweight literate programming for clojure -- inspired by [docco](http://jashkenas.github.com/docco/)"
;;  :main marginalia.main
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [org.clojure/tools.namespace "0.1.1"]
   [org.clojure/tools.cli "0.2.1"]
   [org.markdownj/markdownj "0.3.0-1.0.2b4"]]

  :resource-paths ["vendor"]

  ;;Needed for testing Latex equation formatting. You must download
  ;;and install MathJax in you doc directory.
  :marginalia {:javascript ["mathjax/MathJax.js"]})

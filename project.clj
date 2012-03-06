(defproject marginalia "0.7.0-SNAPSHOT"
  :description "lightweight literate programming for clojure -- inspired by [docco](http://jashkenas.github.com/docco/)"
;;  :main marginalia.main
  :dependencies
  [[org.clojure/clojure "1.3.0"]
   [org.clojure/tools.namespace "0.1.1"]
   [org.clojure/tools.cli "0.2.1"]
   [hiccup "0.3.7"]
   [org.markdownj/markdownj "0.3.0-1.0.2b4"]]
  :dev-dependencies
  [[lein-clojars "0.6.0"]
   [jline "0.9.94"]
   ;; lein vimclojure& #starts the nailgun server
   [org.clojars.autre/lein-vimclojure "1.0.0"]
   [lein-marginalia "0.7.0-SNAPSHOT"]]
  :resources-path "vendor"
  ;;Needed for testing Latex equation formatting. You must download
  ;;and install MathJax in you doc directory.
  :marginalia {:javascript ["mathjax/MathJax.js"]})

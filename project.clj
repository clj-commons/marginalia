(defproject marginalia "0.6.0"
  :description "lightweight literate programming for clojure -- inspired by [docco](http://jashkenas.github.com/docco/)"
  :main marginalia.core
  :dependencies
  [[org.clojure/clojure "1.2.0"]
   [org.clojure/clojure-contrib "1.2.0"]
   [hiccup "0.3.0"]
   [org.markdownj/markdownj "0.3.0-1.0.2b4"]]
  :dev-dependencies
  [[lein-clojars "0.6.0"]
   [jline "0.9.94"]
   ;; lein vimclojure& #starts the nailgun server
   [org.clojars.autre/lein-vimclojure "1.0.0"]
   [swank-clojure "1.2.1"]]
  ;;Needed for testing Latex equation formatting. You must download
  ;;and install MathJax in you doc directory.
  :marginalia {:javascript ["mathjax/MathJax.js"]})

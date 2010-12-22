(defproject marginalia "0.2.1"
  :description "lightweight literate programming for clojure -- inspired by [docco](http://jashkenas.github.com/docco/)"
  :dependencies
  [[org.clojure/clojure "1.2.0"]
   [org.clojars.nakkaya/markdownj "1.0.2b4"]
   [hiccup "0.3.0"]]
  :dev-dependencies
  [[lein-clojars "0.5.0-SNAPSHOT"]
   [jline "0.9.94"]
   [swank-clojure "1.2.1"]
   ;;Needed for testing lein plugin
   [hiccup "0.3.0"]
   [org.clojars.nakkaya/markdownj "1.0.2b4"]
   [marginalia "0.2.1"]]
  ;;Needed for testing cake plugin
  :tasks [marginalia.tasks]
  ;;Needed for testing Latex equation formatting. You must download
  ;;and install MathJax in you doc directory.
  :marginalia {:javascript ["mathjax/MathJax.js"]})

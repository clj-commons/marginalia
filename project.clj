(defproject marginalia "0.5.1-SNAPSHOT"
  :description "lightweight literate programming for clojure -- inspired by [docco](http://jashkenas.github.com/docco/)"
  :main marginalia.core
  :eval-in-leiningen true
  :dependencies
  [[org.clojure/clojure "1.2.0"]
   [hiccup "0.3.0"]
   [org.markdownj/markdownj "0.3.0-1.0.2b4"]]
  :dev-dependencies
  [[lein-clojars "0.5.0-SNAPSHOT"]
   [jline "0.9.94"]
   [swank-clojure "1.2.1"]
   ;;Needed for testing lein plugin
   [hiccup "0.3.4"]
   [org.markdownj/markdownj "0.3.0-1.0.2b4"]]
  ;;Needed for testing cake plugin
  :tasks [marginalia.tasks]
  ;;Needed for testing Latex equation formatting. You must download
  ;;and install MathJax in you doc directory.
  :marginalia {:javascript ["mathjax/MathJax.js"]})

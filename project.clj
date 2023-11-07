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

  ;; updated to match the latest mathjax website information:
  :marginalia {:javascript ["https://polyfill.io/v3/polyfill.min.js?features=es6"
                            "https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js"]}

  ;; lein docs assumes the lein-marginalia repo is a sibling of this
  ;; marginalia repo -- and that there is a marginalia-gh-pages sibling
  ;; which is marginalia checked out to the gh-pages branch:
  :aliases {"docs" ["run" "-m" "marginalia.main"
                    "-d" "../marginalia-gh-pages"
                    "-f" "index.html"
                    "../lein-marginalia/src/leiningen/marg.clj"
                    "src/marginalia/core.clj"
                    "src/marginalia/html.clj"
                    "src/problem_cases/general.clj"]})

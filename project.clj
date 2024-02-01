(defproject marginalia "0.9.2"
  :description "lightweight literate programming for clojure -- inspired by [docco](http://jashkenas.github.com/docco/)"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  ;;  :main marginalia.main
  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [org.clojure/clojurescript "1.11.132"]
   [org.clojure/tools.namespace "1.4.5"]
   [org.clojure/tools.cli "1.0.219"]
   [org.markdownj/markdownj-core "0.4"]
   [de.ubercode.clostache/clostache "1.4.0"]]

  :resource-paths ["vendor"]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :sign-releases false}]
                        ["snapshots" {:url "https://clojars.org/repo"
                                      :sign-releases false}]]

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

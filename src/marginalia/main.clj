(ns marginalia.main
  (:require
   [marginalia.core :as marginalia]
   [marginalia.html :as html])
  (:gen-class))

(defn -main
  "The main entry point into Marginalia."
  [& sources]
  (binding [html/*resources* ""]
    (marginalia/run-marginalia sources)
    (shutdown-agents)))

;; # Example Usage
(comment
  ;; Command line example
  (-main "./src/marginalia/core.clj" "./src/marginalia/html.clj"))

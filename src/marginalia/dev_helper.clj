(ns marginalia.dev-helper
  "Utilities for helping with marginalia development"
  (:use [clojure.pprint :only (pprint)]
        [clojure.java.browse :only (browse-url)]))

(defn browse-output [s]
  "Writes s to a temp file, then opens the file with your system's default browser.

   Typically used like so:
   (browse-output (output-html (group-lines (gen-doc! \"./src/marginalia/core.clj\""
  (do (spit "./example-output/marg.html" s)
      (let [path (.getAbsolutePath (java.io.File. ""))
            url (str "file://" path "/example-output/marg.html")]
        (browse-url url))))


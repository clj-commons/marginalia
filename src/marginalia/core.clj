(ns marginalia.core
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:use [cljojo.aux :only [*css* *html*]]))


(def *test* "./src/cljojo/core.clj")
(def *docs* "docs")
(def *comment* #"^\s*;;\s?")
(def *divider-text* "\n;;DIVIDER\n")
(def *divider-html* #"\n*<span class=\"c[1]?\">;;DIVIDER</span>\n*")

(defn ls
  [path]
  (let [file (java.io.File. path)]
    (if (.isDirectory file)
      (seq (.list file))
      (when (.exists file)
        [path]))))

(defn mkdir [path]
  (.mkdirs (io/file path)))

(defn ensure-directory! [path]
  (when-not (ls path)
    (mkdir path)))


(defn usage []
  (println "cljojo <src1> ... <src-n>"))

;; This line should be replaced
;; and this one too!
(defn parse [src]
  (for [line (line-seq src)]
    (if (re-find *comment* line)
      {:docs-text (str (str/replace line *comment* ""))}
      {:code-text (str line)})))

(defn parse [src]
  (loop [[line & more] (line-seq src) cnum 1 dnum 0 sections []]
    (if more
      (if (re-find *comment* line)
        (recur more
               cnum
               (inc dnum)
               (conj sections {:docs-text (str (str/replace line *comment* "")) :line (+ cnum dnum)}))
        (recur more
               (inc cnum)
               0
               (conj sections {:code-text (str line) :line cnum})))
      sections)))

;; How is this handled?
;; I wonder?
;; No idea ne
(defn gen-doc! [path]
  (println "Generating documentation for " path)
  (with-open [src (io/reader (io/file path))]
    (doseq [section (parse src)]
      ;; and this?
      (println section))))

(re-find *comment* "  ;; this is a comment")

(defn -main [sources]
  (if-not sources
    (do
      (println "Wrong number of arguments passed to cljojo.")
      (println "Please present paths to source files as follows:")
      (usage))
    (doseq [src sources]
      (ensure-directory!  "./docs")
      (spit (io/file (str "./docs/" "cljojo.css")) *css*)
      (gen-doc! src))))


(-main *command-line-args*)


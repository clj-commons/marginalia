(ns marginalia.core
  "**Core** provides all of the functionality around parsing clojure source files
   into an easily consumable format."
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:use [marginalia.html :only (uberdoc-html)]
        [clojure.contrib.find-namespaces :only (read-file-ns-decl)]))


(def *test* "./src/cljojo/core.clj")
(def *docs* "docs")
(def *comment* #"^\s*;;\s?")
(def *divider-text* "\n;;DIVIDER\n")
(def *divider-html* #"\n*<span class=\"c[1]?\">;;DIVIDER</span>\n*")

;; ## File System Utilities

(defn ls
  "Performs roughly the same task as the UNIX `ls`.  That is, returns a seq of the filenames
   at a given directory.  If a path to a file is supplied, then the seq contains only the
   original path given."
  [path]
  (let [file (java.io.File. path)]
    (if (.isDirectory file)
      (seq (.list file))
      (when (.exists file)
        [path]))))

(defn mkdir [path]
  (.mkdirs (io/file path)))

(defn ensure-directory!
  "Ensure that the directory specified by `path` exists.  If not then make it so.
   Here is a snowman ☃"
  [path]
  (when-not (ls path)
    (mkdir path)))

(defn dir? [path]
  (.isDirectory (java.io.File. path)))

(defn find-clojure-file-paths [dir]
  "Returns a seq of clojure file paths (strings) in alphabetical depth-first order (I think?)."
  (->> (java.io.File. dir)
       (file-seq)
       (filter #(re-find #"\.clj$" (.getAbsolutePath %)))
       (map #(.getAbsolutePath %))))


;; ## Project Info Parsing
;; Marginalia will parse info out of your project.clj to display in
;; the generated html file's header.
;;
;; TODO: add pom.xml support.




(defn parse-project-file
  "Parses a project.clj file and returns a map in the following form

       ex. {:name 
            :version
            :dependencies
            :dev-dependencies
            etc...}

   by reading the `defproject` form from your project.clj to obtain name and
   version, then merges in the rest of the defproject forms (`:dependencies`, etc)."
  ([] (parse-project-file "./project.clj"))
  ([path]
      (try
        (let [rdr (clojure.lang.LineNumberingPushbackReader.
                   (java.io.FileReader.
                    (java.io.File. path)))
              project-form (read rdr)]
          (merge {:name (str (second project-form))
                  :version (nth project-form 2)}
                 (apply hash-map (drop 3 project-form))))
        (catch Exception e
          (throw (Exception.
                  (str
                   "There was a problem reading the project definition from "
                   path)))))))


;; ## Source File Analysis

;; This line should be replaced
;; and this one too!
(defn parse [src]
  (for [line (line-seq src)]
    (if (re-find *comment* line)
      {:docs-text (str (str/replace line *comment* ""))}
      {:code-text (str line)})))


(defn end-of-block? [cur-group groups lines]
  (let [line (first lines)
        next-line (second lines)
        next-line-code (get next-line :code-text "")]
    (when (or (and (:code-text line)
                   (:docs-text next-line))
              (re-find #"^\(def" (str/trim next-line-code)))
      true)))

(defn merge-line [line m]
  (cond
   (:docstring-text line) (assoc m :docs (conj (get m :docs []) line))
   (:code-text line) (assoc m :codes (conj (get m :codes []) line))
   (:docs-text line) (assoc m :docs (conj (get m :docs []) line))))

(defn group-lines [doc-lines]
  (loop [cur-group {}
         groups []
         lines doc-lines]
    (cond
     (empty? lines) (conj groups cur-group)

     (end-of-block? cur-group groups lines)
     (recur (merge-line (first lines) {}) (conj groups cur-group) (rest lines))

     :else (recur (merge-line (first lines) cur-group) groups (rest lines)))))

;; Hacktastic, these ad-hoc checks should be replaced with something
;; more robust.
(defn docstring-line? [line sections]
  (let [l (last sections)
        last-code-text (get l :code-text "")]
    (try
      (or
       ;; Is the last line's code-text a defn, and does the
       ;; current line start with a quote?
       (and (re-find #"\(defn" last-code-text)
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the last line's code-text a deftask, and does the
       ;; current line start with a quote?
       (and (re-find #"\(deftask" last-code-text)
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the last line's code-text the start of a ns
       ;; decl, and does the current line start with a quote?
       (and (re-find #"\(ns" last-code-text)
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the prev line a docstring line, the current line _not_
       ;; start with a ( or [ or {, and the current line not an empty string?
       (and (:docstring-text l)
            (not (re-find #"^\(" (str/trim (str line))))
            (not (re-find #"^\[" (str/trim (str line))))
            (not (re-find #"^\{" (str/trim (str line))))
            (not= "" (str/trim (str line))))
       ;; Is the prev line a docstring, the prev line not end with a quote,
       ;; and the current line not an empty string?
       (and (:docstring-text l)
            (not (re-find #"\"$" (str/trim (:docstring-text l))))
            (= "" (str/trim (str line)))))
      (catch Exception e nil))))

(defn parse [src]
  (loop [[line & more] (line-seq src) cnum 1 dnum 0 sections []]
    (if line
      (if (re-find *comment* line)
        (recur more
               cnum
               (inc dnum)
               (conj sections {:docs-text (str (str/replace line *comment* "")) :line (+ cnum dnum)}))
        (recur more
               (inc cnum)
               0
               (if (docstring-line? (str line) sections)
                 (conj sections {:docstring-text (str line) :line cnum})
                 (conj sections {:code-text (str line) :line cnum}))))
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

(defn gen-doc! [path]
  (with-open [src (io/reader (io/file path))]
    (parse src)))

(re-find *comment* "  ;; this is a comment")

(defn path-to-doc [fn]
  (let [ns (-> (java.io.File. fn)
               (read-file-ns-decl)
               (second)
               (str))
        groups (->> fn
                    (gen-doc!)
                    (group-lines))]
    {:ns ns
     :groups groups}))


;; ## Ouput Generation

(defn uberdoc! [output-file-name files-to-analyze]
  "Generates an uberdoc html file from 3 pieces of information:

   1. Results from processing source files (`path-to-doc`)
   2. Project metadata obtained from `parse-project-file`.
   3. The path to spit the result (`output-file-name`)"
  (let [docs (map path-to-doc files-to-analyze)
        source (uberdoc-html
                output-file-name
                (parse-project-file)
                (map path-to-doc files-to-analyze))]
    (spit output-file-name source)))

;; ## External Interface (command-line, lein, cake, etc)

(defn format-sources [sources]
  (if (nil? sources)
    (find-clojure-file-paths "./src")
    (->> sources
         (map #(if (dir? %)
                 (find-clojure-file-paths %)
                 [%]))
         (flatten))))

(defn usage []
  (println "marginalia <src1> ... <src-n>"))

(defn run-marginalia [sources]
  (let [sources (format-sources sources)]
    (if-not sources
      (do
        (println "Wrong number of arguments passed to marginalia.")
        (println "Please present paths to source files as follows:")
        (usage))
      (do
        (println "Generating uberdoc for the following source files:")
        (doseq [s sources]
          (println "  " s))
        (println)
        (ensure-directory! "./docs")
        (uberdoc! "./docs/uberdoc.html" sources)
        (println "Done generating your docs, please see ./docs/uberdoc.html")
        (println)))))

(defn -main
  "main docstring
   Multi line"
  [sources]
  (run-marginalia sources))

;; # Example Usage
(comment

  ;; Command line example
  (-main ["./src/marginalia/core.clj" "./src/marginalia/html.clj"])
  
  ;; This will find all marginalia source files, and then generate an uberdoc.
  (-main (find-clojure-file-paths "./src"))

;; Move these to tests
  (merge-line {:docstring-text "hello world" :line 3} {:docs ["stuff"]})
  (merge-line {:code-text "(defn asdf" :line 4} {:docs ["stuff"]})
  (merge-line {:docs-text "There's only one method in this module", :line 4} {}))


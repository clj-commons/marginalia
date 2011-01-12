(ns marginalia.core
  "**Core** provides all of the functionality around parsing clojure source files
   into an easily consumable format."
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:use [marginalia.html :only (uberdoc-html)]
        [clojure.contrib.find-namespaces :only (read-file-ns-decl)])
  (:gen-class))


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

(defn dir?
  "Many Marginalia fns use dir? to recursively search a filepath."
  [path]
  (.isDirectory (java.io.File. path)))

(defn find-clojure-file-paths
  "Returns a seq of clojure file paths (strings) in alphabetical depth-first order."
  [dir]
  (->> (java.io.File. dir)
       (file-seq)
       (filter #(re-find #"\.clj$" (.getAbsolutePath %)))
       (map #(.getAbsolutePath %))))

;; ## Project Info Parsing
;; Marginalia will parse info out of your project.clj to display in
;; the generated html file's header.
;;
;; ![TODO](http://images.fogus.me/badges/todo.png "POM") add pom.xml support.


(defn parse-project-form
  "Pulls apart the seq of project information and assembles it into a map of
   the following form
       {:name 
        :version
        :dependencies
        :dev-dependencies
        etc...}
  by merging into the name and version information the rest of the defproject
  forms (`:dependencies`, etc)"
  [[_ project-name version-number & attributes]]
  (merge {:name (str project-name)
	  :version version-number}
	 (apply hash-map attributes)))

(defn parse-project-file
  "Parses a project file -- './project.clj' by default -- and returns a map
   assembled according to the logic in parse-project-form."
  ([] (parse-project-file "./project.clj"))
  ([path]
      (try
        (let [rdr (clojure.lang.LineNumberingPushbackReader.
                   (java.io.FileReader.
                    (java.io.File. path)))]
          (parse-project-form (read rdr)))
	(catch Exception e
          (throw (Exception.
                  (str
                   "There was a problem reading the project definition from "
                   path)))))))


;; ## Source File Analysis

;; Marginalia will parse your code to extract doc strings for display in the
;; generated html file.
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
       ;; Last line contain defn &&
       ;; last line not contain what looks like a param vector &&
       ;; current line start with a quote 
       (and (re-find #"\(defn" last-code-text)
            (not (re-find #"\[.*\]" last-code-text))
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the last line's code-text a deftask, and does the
       ;; current line start with a quote?
       (and (re-find #"^\(deftask" (str/trim last-code-text))
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the last line's code-text the start of a ns
       ;; decl, and does the current line start with a quote?
       (and (re-find #"^\(ns" last-code-text)
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the last line's code-text the start of a defprotocol,
       ;; and does the current line start with a quote?
       (and (re-find #"^\(defprotocol" last-code-text)
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the last line's code-text the start of a defmulti,
       ;; and does the current line start with a quote?
       (and (re-find #"^\(defmulti" last-code-text)
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the last line's code-text the start of a defmethod,
       ;; and does the current line start with a quote?
       (and (re-find #"^\(defmethod" last-code-text)
            (re-find #"^\"" (str/trim (str line))))       
       ;; Is the last line's code-text the start of a defmacro,
       ;; and does the current line start with a quote?
       (and (re-find #"^\(defmacro" last-code-text)
            (re-find #"^\"" (str/trim (str line))))
       ;; Is the prev line a docstring, prev line not end with a quote,
       ;; and the current line empty?
       (and (:docstring-text l)
            (not (re-find #"\"$" (str/trim (:docstring-text l)))))
       ;; Is the prev line a docstring, the prev line not end with a quote,
       ;; and the current line not an empty string?
       (and (:docstring-text l)
            (not (re-find #"[^\\]\"$" (str/trim (:docstring-text l))))
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

(defn uberdoc!
  "Generates an uberdoc html file from 3 pieces of information:

   2. The path to spit the result (`output-file-name`)
   1. Results from processing source files (`path-to-doc`)
   3. Project metadata as a map, containing at a minimum the following:
     - :name
     - :version
  "
  [output-file-name files-to-analyze props]
    (spit output-file-name
	  (uberdoc-html
                output-file-name
                (map path-to-doc files-to-analyze)
                props)))

;; ## External Interface (command-line, lein, cake, etc)

;; These functions support Marginalia's use by client software or command-line
;; users.

(defn format-sources
  "Given a collection of filepaths, returns a lazy sequence of filepaths to
   all .clj files on those paths: directory paths will be searched recursively
   for .clj files."
  [sources]
  (if (nil? sources)
    (find-clojure-file-paths "./src")
    (->> sources
         (map #(if (dir? %)
                 (find-clojure-file-paths %)
                 [%]))
         (flatten))))

(defn usage []
  (println "marginalia <src1> ... <src-n>"))

(defn run-marginalia
  "Default generation: given a collection of filepaths in a project, find the .clj
   files at these paths and, if Clojure source files are found:

   1. Print out a message to std out letting a user know which files are to be processed;
   1. Create the docs directory inside the project folder if it doesn't already exist;
   1. Call the uberdoc! function to generate the output file at its default location,
     using the found source files and a project file expected to be in its default location.

   If no source files are found, complain with a usage message."
  [sources]
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
        (uberdoc! "./docs/uberdoc.html" sources (parse-project-file))
        (println "Done generating your docs, please see ./docs/uberdoc.html")
        (println)))))

(defn -main
  "The main entry point into Marginalia."
  [& sources]
  (run-marginalia sources))


;; # Example Usage
(comment
  ;; Command line example
  (-main "./src/marginalia/core.clj" "./src/marginalia/html.clj")
  
  ;; This will find all marginalia source files, and then generate an uberdoc.
  (apply -main (find-clojure-file-paths "./src"))

;; Move these to tests
  (merge-line {:docstring-text "hello world" :line 3} {:docs ["stuff"]})
  (merge-line {:code-text "(defn asdf" :line 4} {:docs ["stuff"]})
  (merge-line {:docs-text "There's only one method in this module", :line 4} {})
)

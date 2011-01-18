(ns marginalia.core
  "**Core** provides all of the functionality around parsing clojure source files
   into an easily consumable format."
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:use [marginalia
         [html :only (uberdoc-html)]
         [parser :only (parse-file)]]
        [clojure.contrib
         [find-namespaces :only (read-file-ns-decl)]
         [command-line :only (print-help with-command-line)]])
  (:gen-class))


(def *test* "./src/cljojo/core.clj")
(def *docs* "./docs")
(def *comment* #"^\s*;;\s?")

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

(defn find-clojure-file-paths
  "Returns a seq of clojure file paths (strings) in alphabetical depth-first order (I think?)."
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




(defn parse-project-file
  "Parses a project.clj file and returns a map in the following form

       {:name 
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

(defn path-to-doc [fn]
  (let [ns (-> (java.io.File. fn)
               (read-file-ns-decl)
               (second)
               (str))
        groups (parse-file fn)]
    {:ns ns
     :groups groups}))


;; ## Ouput Generation

(defn uberdoc!
  "Generates an uberdoc html file from 3 pieces of information:

   1. Results from processing source files (`path-to-doc`)
   2. The path to spit the result (`output-file-name`)
   3. Project metadata as a map, containing at a minimum the following:
     - :name
     - :version
  "
  [output-file-name files-to-analyze props]
  (let [source (uberdoc-html
                output-file-name
                props
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

(defn run-marginalia [args]
  (with-command-line args
    (str "Leiningen plugin for running marginalia against your project.\n\n"
         "Usage: lein marg <options?> <src1> ... <src-n>\n")
    [[dir d "Directory into which the documentation will be written" "./docs"]
     [file f "File into which the documentation will be written" "uberdoc.html"]
     sources]
    (let [sources (format-sources (seq sources))]
      (if-not sources
        (do
          (println "Wrong number of arguments passed to marginalia.")
          (print-help))
        (binding [*docs* dir]
          (println "Generating uberdoc for the following source files:")
          (doseq [s sources]
            (println "  " s))
          (println)
          (ensure-directory! *docs*)
          (uberdoc! (str *docs* "/" file) sources (parse-project-file))
          (println "Done generating your documentation, please see"
                   (str *docs* "/" file))
          (println ""))))))

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

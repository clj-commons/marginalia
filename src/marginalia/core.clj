;; ## A new way to think about programs
;;
;; What if your code and its documentation were one and the same?
;;
;; Much of the philosophy guiding literate programming is the realization of the answer to this question.
;; However, if literate programming stands as a comprehensive programming methodology at one of end of the
;; spectrum and no documentation stands as its antithesis, then Marginalia falls somewhere between. That is,
;; you should always aim for comprehensive documentation, but the shortest path to a useful subset is the
;; commented source code itself.
;;
;; ## The art of Marginalia
;;
;; If you’re fervently writing code that is heavily documented, then using Marginalia for your Clojure projects
;; is as simple as running it on your codebase. However, if you’re unaccustomed to documenting your source, then
;; the guidelines herein will help you make the most out of Marginalia for true-power documentation.
;;
;; Following the guidelines will work to make your code not only easier to follow – it will make it better.
;; The very process of using Marginalia will help to crystalize your understanding of problem and its solution(s).
;;
;; The quality of the prose in your documentation will often reflect the quality of the code itself thus highlighting
;; problem areas. The elimination of problem areas will solidify your code and its accompanying prose. Marginalia
;; provides a virtuous circle spiraling inward toward maximal code quality.
;;
;; ## The one true way
;;
;; 1. Start by running Marginalia against your code
;; 2. Cringe at the sad state of your code commentary
;; 3. Add docstrings and code comments as appropriate
;; 4. Generate the documentation again
;; 5. Read the resulting documentation
;; 6. Make changes to code and documentation so that the “dialog” flows sensibly
;; 7. Repeat from step #4 until complete
;;
(ns marginalia.core
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:use [marginalia
         [html :only (uberdoc-html index-html single-page-html)]
         [parser :only (parse-file)]]
        [clojure.tools
         [namespace :only (read-file-ns-decl)]
         [cli :only (cli)]]))


(def ^{:dynamic true} *test* "src/marginalia/core.clj")
(def ^{:dynamic true} *docs* "./docs")
(def ^{:dynamic true} *comment* #"^\s*;;\s?")

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

(defn find-file-extension
  "Returns a string containing the files extension."
  [^java.io.File file]
  (second (re-find #"\.([^.]+)$" (.getName file))))

(defn processable-file?
  "Predicate. Returns true for \"normal\" files with a file extension which
  passes the provided predicate."
  [pred ^java.io.File file]
  (when (.isFile file)
    (-> file find-file-extension pred)))

(defn find-processable-file-paths
  "Returns a seq of processable file paths (strings) in alphabetical order by
  namespace."
  [dir pred]
  (->> (io/file dir)
       (file-seq)
       (filter (partial processable-file? pred))
       (sort-by #(->> % read-file-ns-decl second))
       (map #(.getCanonicalPath %))))

;; ## Project Info Parsing
;; Marginalia will parse info out of your project.clj to display in
;; the generated html file's header.


(defn parse-project-form
  "Parses a project.clj file and returns a map in the following form

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
   (:docstring-text line) (assoc m
])
        sources (distinct (format-sources (seq files)))
        sources (if leiningen (cons leiningen sources) sources)]
    (if-not sources
      (do
        (println "Wrong number of arguments passed to Marginalia.")
        (println help))
      (binding [*docs* dir]
        (let [project-clj (or project
                              (when (.exists (io/file "project.clj"))
                                (parse-project-file)))
              choose #(or %1 %2)
              marg-opts (merge-with choose
                                    {:css (when css (.split css ";"))
                                     :javascript (when js (.split js ";"))
                                     :leiningen leiningen}
                                    (:marginalia project-clj))
              opts (merge-with choose
                               {:name name
                                :version version
                                :description desc
                                :dependencies (split-deps deps)
                                :multi multi
                                :marginalia marg-opts}
                               project-clj)]
          (println "Generating Marginalia documentation for the following source files:")
          (doseq [s sources]
            (println "  " s))
          (println)
          (ensure-directory! *docs*)
          (if multi
            (multidoc! *docs* sources opts)
            (uberdoc!  (str *docs* "/" file) sources opts))
          (println "Done generating your documentation in" *docs*)
          (println ""))))))

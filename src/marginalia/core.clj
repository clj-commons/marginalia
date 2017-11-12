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
         [parser :only (parse-file
                        parse-ns
                        *lift-inline-comments*
                        *delete-lifted-comments*)]]
        [clojure.tools
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
       (sort-by #(->> % parse-ns second))
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
          (loop [line (read rdr)]
            (let [found-project? (= 'defproject (first line))]
              (if found-project?
                (parse-project-form line)
                (recur (read rdr))))))
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
                            :docs
                            (conj (get m :docs []) line))
   (:code-text line)      (assoc m
                            :codes
                            (conj (get m :codes []) line))
   (:docs-text line)      (assoc m
                            :docs
                            (conj (get m :docs []) line))))

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
  {:ns     (parse-ns (java.io.File. fn))
   :groups (parse-file fn)})


;; ## Output Generation

(defn filename-contents
  [props output-dir all-files parsed-file]
  {:name (io/file output-dir (str (:ns parsed-file) ".html"))
   :contents (single-page-html props parsed-file all-files)})

(defn multidoc!
  [output-dir files-to-analyze props]
  (let [parsed-files (map path-to-doc files-to-analyze)
        index (index-html props parsed-files)
        pages (map #(filename-contents props output-dir parsed-files %) parsed-files)]
    (doseq [f (conj pages {:name (io/file output-dir "toc.html")
                           :contents index})]
           (spit (:name f) (:contents f)))))

(defn uberdoc!
  "Generates an uberdoc html file from 3 pieces of information:

   2. The path to spit the result (`output-file-name`)
   1. Results from processing source files (`path-to-doc`)
   3. Project metadata as a map, containing at a minimum the following:
     - :name
     - :version
  "
  [output-file-name files-to-analyze props]
  (let [source (uberdoc-html
                props
                (map path-to-doc files-to-analyze))]
    (spit output-file-name source)))

;; ## External Interface (command-line, lein, cake, etc)

;; These functions support Marginalia's use by client software or command-line
;; users.

(def ^:private file-extensions #{"clj" "cljs" "cljx" "cljc"})

(defn format-sources
  "Given a collection of filepaths, returns a lazy sequence of filepaths to all
   .clj, .cljs, .cljx, and .cljc files on those paths: directory paths will be searched
   recursively for files."
  [sources]
  (if (nil? sources)
    (find-processable-file-paths "./src" file-extensions)
    (->> sources
         (mapcat #(if (dir? %)
                    (find-processable-file-paths % file-extensions)
                    [(.getCanonicalPath (io/file %))])))))

(defn split-deps [deps]
  (when deps
    (for [d (.split deps ";")
          :let [[group artifact version] (.split d ":")]]
      [(if (= group artifact) artifact (str group "/" artifact))
       version])))

(defn source-excluded?
  "Check if a source file is excluded from the generated documentation"
  [source opts]
  (if-not (empty?
           (filter #(if (re-find (re-pattern %) source)
                      true
                      false)
                   (-> opts :marginalia :exclude)))
    true
    false))

(defn run-marginalia
  "Default generation: given a collection of filepaths in a project, find the .clj
   files at these paths and, if Clojure source files are found:

   1. Print out a message to std out letting a user know which files are to be processed;
   1. Create the docs directory inside the project folder if it doesn't already exist;
   1. Call the uberdoc! function to generate the output file at its default location,
     using the found source files and a project file expected to be in its default location.

   If no source files are found, complain with a usage message."
  [args & [project]]
  (let [[{:keys [dir file name version desc deps css js multi
                 leiningen exclude
                 lift-inline-comments exclude-lifted-comments]} files help]
        (cli args
             ["-d" "--dir" "Directory into which the documentation will be written" :default "./docs"]
             ["-f" "--file" "File into which the documentation will be written" :default "uberdoc.html"]
             ["-n" "--name" "Project name - if not given will be taken from project.clj"]
             ["-v" "--version" "Project version - if not given will be taken from project.clj"]
             ["-D" "--desc" "Project description - if not given will be taken from project.clj"]
             ["-a" "--deps" "Project dependencies in the form <group1>:<artifact1>:<version1>;<group2>...
                 If not given will be taken from project.clj"]
             ["-c" "--css" "Additional css resources <resource1>;<resource2>;...
                 If not given will be taken from project.clj."]
             ["-j" "--js" "Additional javascript resources <resource1>;<resource2>;...
                 If not given will be taken from project.clj"]
             ["-m" "--multi" "Generate each namespace documentation as a separate file" :flag true]
             ["-l" "--leiningen" "Generate the documentation for a Leiningen project file."]
             ["-e" "--exclude" "Exclude source file(s) from the document generation process <file1>;<file2>;...
                 If not given will be taken from project.clj"]
             ["-L" "--lift-inline-comments" "Lift ;; inline comments to the top of the enclosing form.
                 They will be treated as if they preceded the enclosing form." :flag true]
             ["-X" "--exclude-lifted-comments" "If ;; inline comments are being lifted into documentation
                 then also exclude them from the source code display." :flag true])
        sources (distinct (format-sources (seq files)))
        sources (if leiningen (cons leiningen sources) sources)]
    (if-not sources
      (do
        (println "Wrong number of arguments passed to Marginalia.")
        (println help))
      (binding [*docs* dir
                *lift-inline-comments* lift-inline-comments
                *delete-lifted-comments* exclude-lifted-comments]
        (let [project-clj (or project
                              (when (.exists (io/file "project.clj"))
                                (parse-project-file)))
              choose #(or %1 %2)
              marg-opts (merge-with choose
                                    {:css (when css (.split css ";"))
                                     :javascript (when js (.split js ";"))
                                     :exclude (when exclude (.split exclude ";"))
                                     :leiningen leiningen}
                                    (:marginalia project-clj))
              opts (merge-with choose
                               {:name name
                                :version version
                                :description desc
                                :dependencies (split-deps deps)
                                :multi multi
                                :marginalia marg-opts}
                               project-clj)
              sources (->> sources
                           (filter #(not (source-excluded? % opts)))
                           (into []))]
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

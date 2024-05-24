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
;; Following the guidelines will work to make your code not only easier to follow: it will make it better.
;; The very process of using Marginalia will help to crystallize your understanding of problem and its solution(s).
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
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string  :as str]
   [clojure.tools.cli :as cli]
   [marginalia.html :as html]
   [marginalia.log :as log]
   [marginalia.parser :as parser])
  (:import
   (java.io File FileReader)))

(set! *warn-on-reflection* true)

(def ^:dynamic *working-directory*
  "What to use as the base directory. This is used in tests and is unlikely to be useful otherwise.
  Defaults to `nil`, which will result in the normal working directory being used."
  nil)

;; ## File System Utilities

(defn- ls
  "Performs roughly the same task as the UNIX `ls`.  That is, returns a seq of the filenames
   at a given directory.  If a path to a file is supplied, then the seq contains only the
   original path given."
  [path]
  (let [file (io/file path)]
    (if (.isDirectory file)
      (seq (.list file))
      (when (.exists file)
        [path]))))

(defn- mkdir [path]
  (.mkdirs (io/file path)))

(defn- ensure-directory!
  "Ensure that the directory specified by `path` exists.  If not then make it so.
   Here is a snowman ☃"
  [path]
  (when-not (ls path)
    (mkdir path)))

(defn dir?
  "Many Marginalia fns use dir? to recursively search a filepath."
  [path]
  (.isDirectory (io/file path)))

(defn- find-file-extension
  "Returns a string containing the files extension."
  [^File file]
  (second (re-find #"\.([^.]+)$" (.getName file))))

(defn- processable-file?
  "Predicate. Returns true for \"normal\" files with a file extension which
  passes the provided predicate."
  [pred ^File file]
  (when (.isFile file)
    (-> file find-file-extension pred)))

(defn find-processable-file-paths
  "Returns a seq of processable file paths (strings) in alphabetical order by
  namespace."
  [dir pred]
  (->> (io/file dir)
       (file-seq)
       (filter (partial processable-file? pred))
       (sort-by parser/parse-ns)
       (map #(.getCanonicalPath ^File %))))

;; ## Project Info Parsing
;; Marginalia will parse info out of your `project.clj` to display in
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
  (merge {:name    (str project-name)
	  :version version-number}
	 (apply hash-map attributes)))

(defn parse-project-file
  "Parses a project file -- './project.clj' by default -- and returns a map
   assembled according to the logic in parse-project-form."
  ([] (parse-project-file "./project.clj"))
  ([path]
   (try
     (let [rdr (clojure.lang.LineNumberingPushbackReader.
                (FileReader.
                 (io/file path)))]
       (loop [line (read rdr)]
         (let [found-project? (= 'defproject (first line))]
           (if found-project?
             (parse-project-form line)
             (recur (read rdr))))))
     (catch Exception _
       (throw (Exception.
               (str
                "There was a problem reading the project definition from "
                path)))))))

;; ## Config dir
;; Marginalia will also look in `.marginalia/config.edn` for config

(def cfg-dir "Where Marginalia keeps project-specific config" ".marginalia")

(defn- config-from-file
  "Returns any config that could be read from the config file (c.f. `cfg-dir`)."
  []
  (let [f (io/file *working-directory* cfg-dir "config.edn")]
    (if (.exists f)
      (try
        (edn/read-string (slurp f))
        (catch Exception e
          (log/error "Could not read config file: %s" (.getMessage e))
          {}))
      {})))

;; ## Output Generation

(defn- path-to-doc [filename]
  {:ns     (parser/parse-ns (io/file filename))
   :groups (parser/parse-file filename)})

(defn- filename-contents
  [props output-dir all-files parsed-file]
  {:name     (io/file output-dir (str (:ns parsed-file) ".html"))
   :contents (html/single-page-html props parsed-file all-files)})

(defn multidoc!
  "Generate documentation for the given `files-to-analyze`, write the doc files to disk in the `output-dir`"
  [output-dir files-to-analyze props]
  (let [parsed-files (map path-to-doc files-to-analyze)
        index        (html/index-html props parsed-files)
        pages        (map #(filename-contents props output-dir parsed-files %) parsed-files)]
    (doseq [f (conj pages {:name     (io/file output-dir "toc.html")
                           :contents index})]
           (spit (:name f) (:contents f)))))

(defn uberdoc!
  "Generates an uberdoc html file from 3 pieces of information:

   2. The path to spit the result (`output-file-name`)
   1. Results from processing source files (`path-to-doc`)
   3. Project metadata as a map, containing at a minimum the following:
     - :name
     - :version"
  [output-file-name files-to-analyze props]
  (let [source (html/uberdoc-html
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
    (find-processable-file-paths (.getAbsolutePath (io/file *working-directory* "src")) file-extensions)
    (->> sources
         (mapcat #(if (dir? %)
                    (find-processable-file-paths % file-extensions)
                    [(.getCanonicalPath (io/file %))])))))

(defn- split-deps [deps]
  (when deps
    (for [d (str/split deps #";")
          :let [[group artifact version] (str/split d #":")]]
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

(def ^:private cli-flags
  ;; If these are modified, update the README and the `select-keys` allowlist in `resolved-opts+sources` as well
  [["-d" "--dir"
    "Directory into which the documentation will be written" :default "./docs"]
   ["-f" "--file"
    "File into which the documentation will be written" :default "uberdoc.html"]
   ["-n" "--name"
    "Project name - if not given will be taken from project.clj"]
   ["-v" "--version"
    "Project version - if not given will be taken from project.clj"]
   ["-D" "--desc"
    "Project description - if not given will be taken from project.clj"]
   ["-a" "--deps"
    "Project dependencies in the form <group1>:<artifact1>:<version1>;<group2>...
                 If not given will be taken from project.clj"]
   ["-c" "--css"
    "Additional css resources <resource1>;<resource2>;...
                 If not given will be taken from project.clj."]
   ["-j" "--js"
    "Additional javascript resources <resource1>;<resource2>;...
                 If not given will be taken from project.clj"]
   ["-m" "--multi"
    "Generate each namespace documentation as a separate file" :flag true]
   ["-l" "--leiningen"
    "Generate the documentation for a Leiningen project file."]
   ["-e" "--exclude"
    "Exclude source file(s) from the document generation process <file1>;<file2>;...
                 If not given will be taken from project.clj"]
   ["-L" "--lift-inline-comments"
    "Lift ;; inline comments to the top of the enclosing form.
                 They will be treated as if they preceded the enclosing form." :flag true]
   ["-X" "--exclude-lifted-comments"
    "If ;; inline comments are being lifted into documentation
                 then also exclude them from the source code display." :flag true]])

(defn resolved-opts+sources
  "Parse CLI args and incorporate them with additional options specified in `project.clj` and `.marginalia/config.edn`.

   Displays a help message and returns `nil` if the CLI args are invalid, otherwise returns a tuple of `[opts sources]`.

   The precedence is CLI > config.edn > project.clj."
  [args project]
  (let [[cli-config files help]       (apply cli/cli args cli-flags)
        choose                        #(or %1 %2)
        {:keys        [css
                       deps
                       desc
                       exclude
                       js]
         lein         :leiningen
         ;; The precedence is CLI args > config.edn > project.clj
         ;; CLI args and config.edn are handled here; project.clj is dealt with below
         :as          cli+edn-config} (merge-with choose cli-config (config-from-file))
        sources                       (cond->> (distinct (format-sources (seq files)))
                                        lein (cons lein))]
    (if-not sources
      (do (println "Wrong number of arguments passed to Marginalia.")
          (println help)
          nil)                          ; be explicit about needing to return `nil` here
      (let [project-clj      (or project
                                 (let [proj (io/file *working-directory* "project.clj")]
                                   (when (.exists proj)
                                     (parse-project-file (.getAbsolutePath proj)))))
            marg-opts        (merge-with choose
                                         {:css        (when css (str/split css #";"))
                                          :javascript (when js (str/split js #";"))
                                          :exclude    (when exclude (str/split exclude #";"))
                                          :leiningen  lein}
                                         (:marginalia project-clj))
            opts             (merge-with choose
                                         ;; Config from the CLI/EDN file that we can pass on transparently
                                         (select-keys cli+edn-config [:dir :file :name :version :multi :exclude
                                                                      :lift-inline-comments :exclude-lifted-comments])
                                         ;; Config from the CLI/EDN file with renames or processing
                                         {:description  desc
                                          :dependencies (split-deps deps)
                                          :marginalia   marg-opts}
                                         ;; project.clj has the lowest priority
                                         project-clj)
            included-sources (->> sources
                                  (filter #(not (source-excluded? % opts)))
                                  (into []))]
        [opts included-sources]))))

(defn run-marginalia
  "Default generation: given a collection of filepaths in a project, find the .clj
   files at these paths and, if Clojure source files are found:

   1. Print out a message to std out letting a user know which files are to be processed;
   1. Create the docs directory inside the project folder if it doesn't already exist;
   1. Call the uberdoc! function to generate the output file at its default location,
     using the found source files and a project file expected to be in its default location.

   If no source files are found, complain with a usage message."
  [args & [project]]
  (let [[{:keys [dir file lift-inline-comments exclude-lifted-comments] :as opts}
         sources :as valid?] (resolved-opts+sources args project)]
    (when valid?
      (binding [parser/*lift-inline-comments*   lift-inline-comments
                parser/*delete-lifted-comments* exclude-lifted-comments]
        (println "Generating Marginalia documentation for the following source files:")
        (doseq [s sources]
          (println "  " s))
        (println)
        (ensure-directory! dir)
        (if (:multi opts)
          (multidoc! dir sources opts)
          (uberdoc! (str dir "/" file) sources opts))
        (println "Done generating your documentation in" dir)
        (println "")))))

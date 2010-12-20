(ns marginalia.core
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:use [marginalia.aux :only [*css* *html*]]
        [marginalia.html :only (output-html)]))


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
  "mkdir docstring"
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
     (end-of-block? cur-group groups lines) (recur (merge-line (first lines) {}) (conj groups cur-group) (rest lines))
     :else (recur (merge-line (first lines) cur-group) groups (rest lines))
     )))

(defn docstring-line? [line sections]
  (let [l (last sections)
        last-code-text (get l :code-text "")]
    (try
      (or (and (re-find #"\(defn" last-code-text)
               (re-find #"^\"" (str/trim (str line))))
          (and (re-find #"\(ns" last-code-text)
               (re-find #"^\"" (str/trim (str line))))
          (and (:docstring-text l)
               (not (re-find #"^\(" (str/trim (str line))))
               (not (re-find #"^\[" (str/trim (str line))))
               (not= "" (str/trim (str line))))
          (and (:docstring-text l)
               (not (re-find #"\"$" (str/trim (:docstring-text l))))
               (= "" (str/trim (str line)))))
      (catch Exception e nil))))

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

(defn -main [sources]
  "main docstring
   Multi line"
  (if-not sources
    (do
      (println "Wrong number of arguments passed to cljojo.")
      (println "Please present paths to source files as follows:")
      (usage))
    (doseq [src sources]
      (ensure-directory!  "./docs")
      (spit (io/file (str "./docs/" "marginalia.css")) *css*)
      (gen-doc! src))))

(use 'clojure.pprint)

(pprint (gen-doc! "./src/marginalia/html.clj"))

(use 'marginalia.dev-helper)

(browse-output (output-html "marginalia.core" (group-lines (gen-doc! "./src/marginalia/aux.clj"))))

(comment (merge-line {:docstring-text "hello world" :line 3} {:docs ["stuff"]})
         (merge-line {:code-text "(defn asdf" :line 4} {:docs ["stuff"]})
         (merge-line {:docs-text "There's only one method in this module", :line 4} {})


         (def g (group-lines (gen-doc! "./test/parse_test.clj")))

         (pprint g)


         (pprint  (gen-doc! "./test/parse_test.clj"))


         #_(-main *command-line-args*))


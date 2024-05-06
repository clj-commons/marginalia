(ns marginalia.latex
  "Utilities for converting parse results into LaTeX."
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clostache.parser :as mustache]
   [marginalia.html :as html]))

(set! *warn-on-reflection* true)

(def template "Mustache-format template to use for the document" (html/slurp-resource "latex.mustache"))

;; ## Namespace header calculation

(def ^:private ^clojure.lang.APersistentVector headers ["subsubsection" "subsection" "section"])

(def ^:private section-re #"\\((sub){0,2}section)\s*\{")

(defn get-section-type
  "It calculates the suitable header for a namespace `ns`. For example,
  if the generated output has subsections a section must be generated for each namespace in the table of contents. If
  it has subsubsections a subsection should be generated and so on."
  [groups]
  (let [matches        (->> groups
                            (map :doc)
                            (mapcat (partial re-seq section-re))
                            (map second))
        matches        (or (seq matches) ["none"])
        most-important (apply max (map #(.indexOf headers %) matches))
        position       (min (-> most-important inc)
                            (-> (count headers) dec))]
    (nth headers (max position 0))))

(defn- get-header
  [the-type ns']
  (str "\\" the-type "{" ns' "}"))

;; ## Markdown to LaTeX conversion

(def ^:private mark (str/join (repeat 3 "marginalialatex")))

(def ^:private separator-marker (str "\n\n" mark "\n\n"))

(def ^:private separator-marker-re (re-pattern (str "\n?\n?" mark "\n?\n?")))

(defn invoke-pandoc
  "We use pandoc to convert `markdown-input` to LaTeX."
  [markdown-input]
  (let [{:keys [exit out err]} (shell/sh "pandoc"
                                         "-f" "markdown" "-t" "latex"
                                         :in markdown-input)]
    (when (not= exit 0)
      (throw
       (RuntimeException. (str "Error code: " exit ", calling pandoc.\n" err))))
    out))

(defn md->latex
  "It applies pandoc in a string joined by `separator-marker`. The
  output is split using a regex created from `separator-marker`"
  [docs]
  (let [joined (str/join separator-marker docs)
        output (invoke-pandoc joined)
        result (str/split output separator-marker-re (count docs))]
    (assert (= (count docs) (count result))
            "The converted docs must have the same number")
    result))

(defn- to-latex
  [groups]
  (map #(assoc %1 :doc %2) groups (md->latex (map :doc groups))))

;; ## Code and doc extraction

(defn- docs-from-group [group]
  (or (:docstring group)
      (and (= (:type group) :comment) (:raw group))
      ""))

(defn- code-from-group [group]
  (if (= (:type group) :code)
    (:raw group)
    ""))

(defn extract-code-and-doc
  "From the original data in each group we extract the code and the
  documentation."
  [groups]
  (map (fn [group] {:code (code-from-group group) :doc (docs-from-group group)})
       groups))

;; ## Uberdoc generation

(def ^:private convert-groups (comp to-latex extract-code-and-doc))

(defn as-data-for-template
  "1. Converts each `groups` data to the format expected by the template.
   2. Calculates the correct header level for namespaces."
  [project-metadata docs]
  (let [namespaces   (map #(update % :groups convert-groups) docs)
        section-type (get-section-type (mapcat :groups namespaces))
        namespaces   (map #(assoc % :ns-header (get-header section-type (:ns %)))
                          namespaces)]
    {:namespaces namespaces :project project-metadata}))

(defn uberdoc-latex
  "It uses mustache to generate the LaTeX contents using a
  template. Before it needs to convert the data to the format expected
  for the template"
  [project-metadata docs]
    (mustache/render template (as-data-for-template project-metadata docs)))

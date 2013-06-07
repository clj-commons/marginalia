(ns marginalia.latex
  "Utilities for converting parse results into LaTeX."
  (:require [clojure.string :as str]
            [clostache.parser :as mustache]
            [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:use [marginalia.html :only [slurp-resource]]))


(def template (slurp-resource "latex.mustache"))

;; ## Namespace header calculation

(def headers ["subsubsection" "subsection" "section"])

(def section-re #"\\((sub){0,2}section)\s*\{")

(defn get-section-type
  "It calculates the suitable header for a namespace `ns`. For example,
  if the generated output has subsections a section must be generated
  for each namespace in the table of contents. If it has
  subsubsections a subsection should be generated and so on."
  [groups]
  (let [docs (map :doc groups)
        matches (->> docs (mapcat (partial re-seq section-re))
                     (map second))
        matches (or (seq matches) ["none"])
        most-important (apply max (map #(.indexOf headers %) matches))
        position (min (-> most-important inc)
                      (-> (count headers) dec))]
    (nth headers (max position 0))))

(defn get-header
  [type ns]
  (str "\\" type "{" ns "}"))

;; ## Markdown to LaTeX conversion

(def mark (string/join (repeat 3 "marginalialatex")))

(def separator-marker (str "\n\n" mark "\n\n"))

(def separator-marker-re (re-pattern (str "\n?\n?" mark "\n?\n?")))

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
  (let [joined (string/join separator-marker docs)
        output (invoke-pandoc joined)
        result (string/split output separator-marker-re (count docs))]
    (assert (= (count docs) (count result))
            "The converted docs must have the same number")
    result))

(defn to-latex
  [groups]
  (map #(assoc-in %1 [:doc] %2) groups (md->latex (map :doc groups))))

;; ## Code and doc extraction

(defn docs-from-group [group]
  (or (:docstring group)
      (and (= (:type group) :comment) (:raw group))
      ""))

(defn code-from-group [group]
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

(def convert-groups (comp to-latex extract-code-and-doc))

(defn as-data-for-template
  "
   1. Converts each `groups` data to the format expected by the template.
   1. Calculates the correct header level for namespaces."
  [project-metadata docs]
  (let [namespaces (map #(update-in % [:groups] convert-groups) docs)
        section-type (get-section-type (mapcat :groups namespaces))
        namespaces (map #(assoc % :ns-header (get-header section-type (:ns %)))
                        namespaces)]
    {:namespaces namespaces :project project-metadata}))

(defn uberdoc-latex
  "It uses mustache to generate the LaTeX contents using a
  template. Before it needs to convert the data to the format expected
  for the template"
  [project-metadata docs]
    (mustache/render template (as-data-for-template project-metadata docs)))
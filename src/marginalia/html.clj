(ns marginalia.html
  "# Utilities for converting parse results into html.
   ## Plus a few other goodies.

   Like I said:

   * utils for docs -> html
   * other goodies"
  (:use [hiccup.core :only (html escape-html)]
        [hiccup.page-helpers :only (doctype)])
  (:require [clojure.string :as str])
  (:import [com.petebevin.markdown MarkdownProcessor]))

;; The following functions handle preparation of doc text (both comment and docstring
;; based) for display through html & css.

;; Markdown processor.
(def mdp (com.petebevin.markdown.MarkdownProcessor.))

(defn md [s]
  "Markdown string to html converter. Translates strings like \"# header!\"
   -> \"<h1>header!</h1>"
  (.markdown mdp s))

(defn remove-leading-trailing-quote
  "Used in docstring pre-processing to remove the leading and trailing `\"` characters."
  [s]
  (-> s
      (str/trim)
      (str/replace #"\"$" "")
      (str/replace #"^\"" "")))

(defn replace-special-chars
  "Inserts those fancy ->'s into doc sections."
  [s]
  (str/replace s #"-&gt;" "&rarr;"))


(defn docs-to-html
  "Converts a docs section to html by threading each doc line through the forms
   outlined above.

   ex. `(docs-to-html [{:doc-text \"#hello world!\"} {:docstring-text \"I'm a docstring!}])
   -> \"<h1>hello world!</h1><br />\"`
   "
  [docs]
  (->> docs
       (map #(or (:docs-text %)
                 (str/replace (:docstring-text %) #"\\\"" "\"")))
       (map remove-leading-trailing-quote)
       (map escape-html)
       (map replace-special-chars)
       (interpose "\n")
       (apply str)
       (md)))

(defn codes-to-html [codes]
  (html [:pre {:class "brush: clojure"} (apply str (interpose "\n" (map escape-html (map :code-text codes))))]))

(defn group-to-html [group]
  (html
   [:tr
    [:td {:class "docs"} (docs-to-html (:docs group))]
    [:td {:class "codes"} (codes-to-html (:codes group))]]))

(defn css-rule [rule]
  (let [sel (first rule)
        props (second rule)]
    (str (name sel) "{" (apply str (map #(str (name (key %)) ":" (val %) ";") props)) "}")))

(defn css [& rules]
  (html [:style {:type "text/css"}
         (apply str (map css-rule rules))]))

(defn slurp-resource
  "Reads the resource named by f using the encoding enc into a string
  and returns it. See [slurp](http://clojuredocs.org/clojure_core/clojure.core/slurp)."
  ([f]
     (let [sb (StringBuilder.)]
       (with-open [#^java.io.Reader r (java.io.InputStreamReader.
                                       (.getResourceAsStream
                                        (.getClassLoader clojure.main)
                                        f))]
         (loop [c (.read r)]
           (if (neg? c)
             (str sb)
             (do
               (.append sb (char c))
               (recur (.read r)))))))))

(defn inline-js [resource]
  (let [src (slurp-resource resource)]
    (html [:script {:type "text/javascript"}
            src])))

(defn inline-css [resource]
  (let [src (slurp-resource resource)]
    (html [:style {:type "text/css"}
           (slurp-resource resource)])))

(defn output-html [project-name groups]
  (html
   (doctype :html5)
   [:html
    [:head
     (inline-js "xregexp-min.js")
     (inline-js "shCore.js")
     (inline-js "shBrushClojure.js")
     (inline-css "shCore.css")
     (inline-css "shThemeEclipse.css")
     (css
      [:body {:margin 0
              :padding 0
              :font-family "'Palatino Linotype', 'Book Antiqua', Palatino, FreeSerif, serif;"
              :font-size "16px"}]
      [:h1 {:font-size "20px"
            :margin-top 0}]
      [:h1.project-name {:font-size "34px"
                         :border-bottom "dotted #aaa 1px"
                         :padding-bottom "10px"}]
      [:h2 {:font-size "18px"
            :margin-top 0}]
      [:table {:border-spacing 0}]
      [:code {:display "inline"}]
      [:p {:margin-top "8px"}]
      [:tr {:margin "0px"
            :padding "0px"}]
      [:td.docs {:width "45%"
                 :vertical-align "top"
                 :margin "0px"
                 :padding-left "55px"
                 :padding-right "20px"
                 :border "none"}]
      [:td.codes {:width "55%"
                  :background-color "#F5F5FF"
                  :vertical-align "top"
                  :margin "0px"
                  :padding-left "20px"
                  :border "none"
                  :overflow "hidden"
                  :font-size "10pt"
                  :border-left "solid #ddd 1px"}])
     [:title "Marginalia Output"]]
    [:body
     [:table
      [:tr
       [:td {:class "docs"}
        [:h1 {:class "project-name"}
         project-name]]
       [:td {:class "codes"}]]
      (map group-to-html groups)]
     [:script {:type "text/javascript"}
      "SyntaxHighlighter.defaults['gutter'] = false;
       SyntaxHighlighter.all()"]]]))


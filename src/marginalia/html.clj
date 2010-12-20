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

;; As a result of docifying then grouping, you'll end up with a seq like this one:
;;
;;     [{:docs [{:docs-text "Some doc text"}]
;;      :codes [{code-text "(def something \"hi\")"}]}]
;;
;; `docs-to-html` and `codes-to-html` convert their respective entries into html,
;; and `group-to-html` calls them on each seq item to do so.


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

(defn toc [docs]
  (html
   [:a {:name "toc"} [:h1 "Table of Contents"]]
   [:ul
    (map #(vector :li [:a {:href (str "#" (:ns %))} (:ns %)])
         docs)]))

(defn groups-html [doc]
  (html 
   [:tr
    [:td {:class "docs"}
     [:div {:class "docs-header"}
      [:a {:class "anchor" :name (:ns doc) :href (str "#" (:ns doc))}
       [:h1 {:class "project-name"}
        (:ns doc)]
       [:a {:href "#toc" :class "toc-link"}
        "toc"]]]]
    [:td {:class "codes"}]]
   (map group-to-html (:groups doc))
   [:tr
    [:td {:class "spacer docs"} "&nbsp;"]
    [:td {:class "codes"}]]))



(defn page-template [toc content]
  (html
   (doctype :html5)
   [:html
    [:head
     (inline-js "xregexp-min.js")
     (inline-js "shCore.js")
     (inline-js "shBrushClojure.js")
     (inline-css "shCore.css")
     (inline-css "shThemeEclipse.css")

     ;; quick and dirty dsl for inline css rules, similar to hiccup.
     (css
      [:body {:margin 0
              :padding 0
              :font-family "'Palatino Linotype', 'Book Antiqua', Palatino, FreeSerif, serif;"
              :font-size "16px"
              :color "#252519"}]
      [:.toc-link {:font-size "12px"
                   :margin-left "10px"
                   :color "#252519"
                   :text-decoration "none"}]
      [:.toc-link:hover {:color "#5050A6"}]
      [:.docs-header {:border-bottom "dotted #aaa 1px"
                      :padding-bottom "10px"
                      :margin-bottom "25px"}]
      [:.anchor {:text-decoration "none"
                 :color "#252519"}]
      [:.anchor:hover {:color "#5050A6"}]
      [:h1 {:font-size "20px"
            :margin-top 0}]
      [:h1.project-name {:font-size "34px"
                         :display "inline"}]
      [:h2 {:font-size "18px"
            :margin-top 0}]
      [:table {:border-spacing 0
               :border-bottom "solid #ddd 1px;"
               :margin-bottom "10px"}]
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
                  :border-left "solid #ddd 1px"}]
      [:td.spacer {:padding-bottom "40px"}]
      [:.footer {:text-align "center"}])
     [:title "Marginalia Output"]]
    [:body
     toc
     [:table
      content]
     [:div {:class "footer"}
      "Generated by "
      [:a {:href "https://github.com/fogus/marginalia"} "marginalia"]
      ".&nbsp;&nbsp;"
      "Syntax highlighting provided by Alex Gorbatchev's "
      [:a {:href "http://alexgorbatchev.com/SyntaxHighlighter/"}
       "SyntaxHighlighter"]]
     [:script {:type "text/javascript"}
      "SyntaxHighlighter.defaults['gutter'] = false;
       SyntaxHighlighter.all()"]]]))




;; Syntax highlighting is done a bit differently than docco.  Instead of embedding
;; the higlighting metadata on the parse / html gen phase, we use [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/)
;; to do it in javascript.

(defn output-html
  "This function's the one that ties the whole html namespace together, and probably
   the only var you'll touch on.

   Notice that we're inlining the css & javascript for [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/) (`inline-js`
   & `inline-css`) to be able to package the output as a single file.  It goes without
   saying that all this is WIP and will prabably change in the future."
  [docs]
  (page-template
   (toc docs)
   (map groups-html docs)))


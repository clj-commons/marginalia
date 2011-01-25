(ns marginalia.html
  "Utilities for converting parse results into html."
  (:use [hiccup.core :only (html escape-html)]
        [hiccup.page-helpers :only (doctype)])
  (:require [clojure.string :as str])
  (:import [com.petebevin.markdown MarkdownProcessor]))

(def ^{:dynamic true} *resources* "./resources/")

(defn css-rule [rule]
  (let [sels (reverse (rest (reverse rule)))
        props (last rule)]
    (str (apply str (interpose " " (map name sels)))
         "{" (apply str (map #(str (name (key %)) ":" (val %) ";") props)) "}")))

(defn css
  "Quick and dirty dsl for inline css rules, similar to hiccup.

   ex. `(css [:h1 {:color \"blue\"}] [:div.content p {:text-indent \"1em\"}])`

   -> `h1 {color: blue;} div.content p {text-indent: 1em;}`"
  [& rules]
  (html [:style {:type "text/css"}
         (apply str (map css-rule rules))]))

(defn slurp-resource
  "Stolen from leiningen"
  [resource-name]
  (-> (.getContextClassLoader (Thread/currentThread))
      (.getResourceAsStream resource-name)
      (java.io.InputStreamReader.)
      (slurp)))

(defn inline-js [resource]
  (let [src (slurp-resource resource)]
    (html [:script {:type "text/javascript"}
            src])))

(defn inline-css [resource]
  (let [src (slurp-resource resource)]
    (html [:style {:type "text/css"}
           (slurp-resource resource)])))




;; The following functions handle preparation of doc text (both comment and docstring
;; based) for display through html & css.

;; Markdown processor.
(def mdp (com.petebevin.markdown.MarkdownProcessor.))

(defn md 
  "Markdown string to html converter. Translates strings like:

   \"# header!\" -> `\"<h1>header!</h1>\"`

   \"## header!\" -> `\"<h2>header!</h2>\"`

   ..."
  [s]
  (.markdown mdp s))

(defn replace-special-chars
  "Inserts super-fancy characters into the doc section."
  [s]
  (-> s
      (str/replace #"->"  "&rarr;")
      (str/replace #"&quot;" "\"")))

;; As a result of docifying then grouping, you'll end up with a seq like this one:
;; <pre><code>[...
;; {:docs [{:docs-text "Some doc text"}]
;;  :codes [{:code-text "(def something \"hi\")"}]}
;; ...]</code></pre>
;;
;; `docs-to-html` and `codes-to-html` convert their respective entries into html,
;; and `group-to-html` calls them on each seq item to do so.

(defn prep-docs-text [s] s)

(defn prep-docstring-text [s]
  (-> s
      (str/replace #"\\\"" "\"")
      (str/replace #"^\s\s\"" "")
      (str/replace #"^\s\s\s" "")
      (str/replace #"\"$" "")
      ;; Don't escape code blocks
      ((fn [t]
         (if (re-find #"^\s\s\s\s" t)
           t
           (escape-html t))))))


(defn docs-to-html
  "Converts a docs section to html by threading each doc line through the forms
   outlined above.

   ex. (docs-to-html [{:doc-text \"# hello world!\"} {:docstring-text \"I'm a docstring!}])
   
   ->  `\"<h1>hello world!</h1><br />\"`
   "
  [docs]
  (-> docs
      str
      prep-docs-text
      replace-special-chars
      (md)))

(defn codes-to-html [code-block]
  (html [:pre {:class "brush: clojure"} code-block]))

(defn section-to-html [section]
  (html [:tr
         [:td {:class "docs"} (docs-to-html
                               (if (= (:type section) :comment)
                                 (:raw section)
                                 (:docstring section)))]
         [:td {:class "codes"}] (if (= (:type section) :code)
                                  (codes-to-html (:raw section))
                                  "")]))

(defn dependencies-html [deps & header-name]
  (let [header-name (or header-name "dependencies")]
    (html [:div {:class "dependencies"}
           [:h3 header-name]
           [:table
            (map #(html [:tr
                         [:td {:class "dep-name"} (str (first %))]
                         [:td {:class "dotted"} [:hr]]
                         [:td {:class "dep-version"} (second %)]])
                 deps)]])))

(defn cake-plugins-html [tasks]
  (when tasks
    (html [:div {:class "plugins"}
           [:h3 "cake plugin namespaces"]
           [:ul
            (map #(vector :li %) tasks)]])))

;; # Load Optional Resources
;; Use external Javascript and CSS in your documentation. For example:
;; To format Latex math equations, download the
;; [MathJax](http://www.mathjax.org/) Javascript library to the docs
;; directory and then add
;;
;;     :marginalia {:javascript ["mathjax/MathJax.js"]}
;;
;; to project.clj. Below is a simple example of both inline and block
;; formatted equations.
;;
;; When \\(a \ne 0\\), there are two solutions to \\(ax^2 + bx + c = 0\\) and they are
;; $$x = {-b \pm \sqrt{b^2-4ac} \over 2a}.$$

(defn opt-resources-html
  "Generate script and link tags for optional external javascript and css."
  [project-info]
  (let [options (:marginalia project-info) 
        javascript (:javascript options)
        css (:css options)]
    (html (concat
           (when javascript
             (map #(vector :script {:type "text/javascript" :src %}) javascript))
           (when css
             (map #(vector :link {:tyle "text/css" :rel "stylesheet" :href %}) css))))))

;; Is &lt;h1/&gt; overloaded?  Maybe we should consider redistributing
;; header numbers instead of adding classes to all the h1 tags.
(defn header-html [project-info]
  (html
   [:tr
    [:td {:class "docs"}
     [:div {:class "header"}
      [:h1 {:class "project-name"} (:name project-info)]
      [:h2 {:class "project-version"} (:version project-info)]
      [:br]
      (md (:description project-info))]
     (dependencies-html (:dependencies project-info))
     (dependencies-html (:dev-dependencies project-info) "dev dependencies")
     (cake-plugins-html (:tasks project-info))]
    [:td {:class "codes"
          :style "text-align: center; vertical-align: middle;color: #666;padding-right:20px"}
     [:br]
     [:br]
     [:br]
     "(this space intentionally left blank)"]]))

(defn toc-html [docs]
  (html
   [:tr
    [:td {:class "docs"}
     [:div {:class "toc"}
      [:a {:name "toc"} [:h3 "namespaces"]]
      [:ul
       (map #(vector :li [:a {:href (str "#" (:ns %))} (:ns %)])
            docs)]]]
    [:td {:class "codes"} "&nbsp;"]]))

(defn floating-toc-html [docs]
  [:div {:id "floating-toc"}
   [:ul
    (map #(vector :li {:class "floating-toc-li"
                       :id (str "floating-toc_" (:ns %))}
                  (:ns %))
         docs)]])

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
   (map section-to-html (:groups doc))
   [:tr
    [:td {:class "spacer docs"} "&nbsp;"]
    [:td {:class "codes"}]]))

(def reset-css
  (css [:html {:margin 0 :padding 0}]
       [:h1 {:margin 0 :padding 0}]
       [:h2 {:margin 0 :padding 0}]
       [:h3 {:margin 0 :padding 0}]
       [:h4 {:margin 0 :padding 0}]
       [:a {:color "#261A3B"}]
       [:a:visited {:color "#261A3B"}]))

(def header-css
  (css [:.header {:margin-top "30px"}]
       [:h1.project-name {:font-size "34px"
                          :display "inline"}]
       [:h2.project-version {:font-size "18px"
                             :margin-top 0
                             :display "inline"
                             :margin-left "10px"}]
       [:.toc-link {:font-size "12px"
                    :margin-left "10px"
                    :color "#252519"
                    :text-decoration "none"}]
       [:.toc-link:hover {:color "#5050A6"}]
       [:.toc :h1 {:font-size "34px"
                   :margin 0}]
       [:.docs-header {:border-bottom "dotted #aaa 1px"
                       :padding-bottom "10px"
                       :margin-bottom "25px"}]
       [:.toc :h1 {:font-size "24px"}]
       [:.toc {:border-bottom "solid #bbb 1px"
               :margin-bottom "40px"}]
       [:.toc :ul {:margin-left "20px"
                   :padding-left "0px"
                   :padding-top 0
                   :margin-top 0}]
       [:.toc :li {:list-style-type "none"
                   :padding-left 0}]
       [:.dependencies {}]
       [:.dependencies :table {:font-size "16px"
                               :width "99.99%"
                               :border "none"
                               :margin-left "20px"}]
       [:.dependencies :td {:padding-right "20px;"
                            :white-space "nowrap"}]
       [:.dependencies :.dotted {:width "99%"}]
       [:.dependencies :.dotted :hr {:height 0
                                     :noshade "noshade"
                                     :color "transparent"
                                     :background-color "transparent"
                                     :border-bottom "dotted #bbb 1px"
                                     :border-top "none"
                                     :border-left "none"
                                     :border-right "none"
                                     :margin-bottom "-6px"}]
       [:.dependencies :.dep-version {:text-align "right"}]
       [:.plugins :ul {:margin-left "20px"
                       :padding-left "0px"
                       :padding-top 0
                       :margin-top 0}]
       [:.plugins :li {:list-style-type "none"
                       :padding-left 0}]
       [:.header :p {:margin-left "20px"}]))

(def floating-toc-css
  (css [:#floating-toc {:position "fixed"
                        :top "10px"
                        :right "20px"
                        :height "20px"
                        :overflow "hidden"
                        :text-align "right"}]
       [:#floating-toc :li {:list-style-type "none"
                            :margin 0
                            :padding 0}]))

(def general-css
  (css
   [:body {:margin 0
           :padding 0
           :font-family "'Palatino Linotype', 'Book Antiqua', Palatino, FreeSerif, serif;"
           :font-size "16px"
           :color "#252519"}]
   [:h1 {:font-size "20px"
         :margin-top 0}]
   [:a.anchor {:text-decoration "none"
              :color "#252519"}]
   [:a.anchor:hover {:color "#5050A6"}]
   [:table {:border-spacing 0
            :border-bottom "solid #ddd 1px;"
            :margin-bottom "10px"}]
   [:code {:display "inline"}]
   [:p {:margin-top "8px"}]
   [:tr {:margin "0px"
         :padding "0px"}]
   [:td.docs {:width "410px"
              :max-width "410px"
              :vertical-align "top"
              :margin "0px"
              :padding-left "55px"
              :padding-right "20px"
              :border "none"}]
   [:td.docs :pre {:font-size "12px"
                   :overflow "hidden"}]
   [:td.codes {:width "55%"
               :background-color "#F5F5FF"
               :vertical-align "top"
               :margin "0px"
               :padding-left "20px"
               :border "none"
               :overflow "hidden"
               :font-size "10pt"
               :border-left "solid #E5E5EE 1px"}]
   [:td.spacer {:padding-bottom "40px"}]
   [:pre :code {:display "block"
                :padding "4px"}]
   [:code {:background-color "ghostWhite"
           :border "solid #DEDEDE 1px"
           :padding-left "3px"
           :padding-right "3px"
           :font-size "14px"}]
   [:.syntaxhighlighter :code {:font-size "13px"}]
   [:.footer {:text-align "center"}]))

(defn page-template
  "Notice that we're inlining the css & javascript for [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/) (`inline-js`
   & `inline-css`) to be able to package the output as a single file (uberdoc if you will).  It goes without
   saying that all this is WIP and will prabably change in the future."
  [project-metadata opt-resources header toc floating-toc content]
  (html
   (doctype :html5)
   [:html
    [:head
     [:meta {:http-equiv "Content-Type" :content "text/html" :charset "utf-8"}]
     [:meta {:name "description" :content (:description project-metadata)}]
     (inline-js (str *resources* "jquery-1.4.4.min.js"))
     (inline-js (str *resources* "xregexp-min.js"))
     (inline-js (str *resources* "shCore.js"))
     (inline-js (str *resources* "shBrushClojure.js"))
     (inline-js (str *resources* "app.js"))
     #_[:script {:type "text/javascript" :src "./../resources/app.js"}]
     (inline-css (str *resources* "shCore.css"))
     (css
      [:.syntaxhighlighter {:overflow "hidden !important"}])
     (inline-css (str *resources* "shThemeEclipse.css"))
     reset-css
     header-css
     floating-toc-css
     general-css
     opt-resources
     [:title (:name project-metadata) " -- Marginalia"]]
    [:body
     [:table
      header
      toc
      content]
     [:div {:class "footer"}
      "Generated by "
      [:a {:href "https://github.com/fogus/marginalia"} "Marginalia"]
      ".&nbsp;&nbsp;"
      "Syntax highlighting provided by Alex Gorbatchev's "
      [:a {:href "http://alexgorbatchev.com/SyntaxHighlighter/"}
       "SyntaxHighlighter"]
      floating-toc]
     [:script {:type "text/javascript"}
      "SyntaxHighlighter.defaults['gutter'] = false;
       SyntaxHighlighter.all()"]]]))


;; Syntax highlighting is done a bit differently than docco.  Instead of embedding
;; the higlighting metadata on the parse / html gen phase, we use [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/)
;; to do it in javascript.

(defn uberdoc-html
  "This generates a stand alone html file (think `lein uberjar`).
   It's probably the only var consumers will use."
  [output-file-name project-metadata docs]
  (page-template
   project-metadata
   (opt-resources-html project-metadata)
   (header-html project-metadata)
   (toc-html docs)
   (floating-toc-html docs)
   (map groups-html docs)))


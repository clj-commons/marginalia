(ns marginalia.html
  "Utilities for converting parse results into html."
  (:use [marginalia.hiccup :only (html escape-html)])
  (:require [clojure.string :as str])
  (:import [com.petebevin.markdown MarkdownProcessor]))

(def ^{:dynamic true} *resources* "./vendor/")

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
  (try
    (-> (.getContextClassLoader (Thread/currentThread))
        (.getResourceAsStream resource-name)
        (java.io.InputStreamReader.)
        (slurp))
    (catch java.lang.NullPointerException npe
      (println (str "Could not locate resources at " resource-name))
      (println "    ... attempting to fix.")
      (let [resource-name (str *resources* resource-name)]
        (try
          (-> (.getContextClassLoader (Thread/currentThread))
              (.getResourceAsStream resource-name)
              (java.io.InputStreamReader.)
              (slurp))
          (catch java.lang.NullPointerException npe
            (println (str "    STILL could not locate resources at " resource-name ". Giving up!"))))))))

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

;; As a result of docifying then grouping, you'll end up with a seq like this one:
;; <pre><code>[...
;; {:docs [{:docs-text "Some doc text"}]
;;  :codes [{:code-text "(def something \"hi\")"}]}
;; ...]</code></pre>
;;
;; `docs-to-html` and `codes-to-html` convert their respective entries into html,
;; and `group-to-html` calls them on each seq item to do so.

(defn docs-to-html
  "Converts a docs section to html by threading each doc line through the forms
   outlined above.

   ex. (docs-to-html [{:doc-text \"# hello world!\"} {:docstring-text \"I'm a docstring!}])

   ->  `\"<h1>hello world!</h1><br />\"`
   "
  [docs]
  (-> docs
      str
      (md)))

(defn codes-to-html [code-block]
  (html [:pre {:class "brush: clojure"}
         (escape-html code-block)]))

(defn section-to-html [section]
  (html [:tr
         [:td {:class "docs"} (docs-to-html
                               (if (= (:type section) :comment)
                                 (:raw section)
                                 (:docstring section)))]
         [:td {:class "codes"} (if (= (:type section) :code)
                                  (codes-to-html (:raw section))
                                  "")]]))

(defn dependencies-html [deps & header-name]
  (when-let [deps (seq deps)]
    (let [header-name (or header-name "dependencies")]
      (html [:div {:class "dependencies"}
             [:h3 header-name]
             [:table
              (map #(html [:tr
                           [:td {:class "dep-name"} (str (first %))]
                           [:td {:class "dotted"} [:hr]]
                           [:td {:class "dep-version"} (second %)]])
                   deps)]]))))


;; # Generate Optional Metadata
;; Add metadata to your documentation.
;;
;; To add <meta name="robots" content="noindex"> to the head of the
;; docs, specify a hash map for the :meta key :marginalia in project.clj:
;;
;;     :marginalia {:meta {:robots "noindex"}}

(defn metadata-html
  "Generate meta tags from project info."
  [project-info]
  (let [options (:marginalia project-info)
        meta (:meta options)]
    (html (when meta
            (map #(vector :meta {:name (name (key %)) :contents (val %)}) meta)))))

;; # Load Optional Resources
;; Use external Javascript and CSS in your documentation. For example:
;;
;; To format Latex math equations, download the
;; [MathJax](http://www.mathjax.org/) Javascript library to the docs
;; directory and then add
;;
;;     :marginalia {:javascript ["mathjax/MathJax.js"]}
;;
;; to project.clj. :javascript and :css accept a vector of paths or URLs
;;
;; Below is a simple example of both inline and block formatted equations.
;;
;; Optionally, you can put the MathJax CDN URL directly as a value of `:javascript`
;; like this:
;;
;;     :marginalia {
;;       :javascript
;;         ["http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML"]}
;;
;; That way you won't have to download and carry around the MathJax library.
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
      [:h1 {:class "project-name"} (if (seq (:url project-info))
                                     [:a {:href (:url project-info)} (:name project-info)]
                                     (:name project-info))]
      [:h2 {:class "project-version"} (:version project-info)]
      [:br]
      (md (:description project-info))]
     (dependencies-html (:dependencies project-info))
     (dependencies-html (:dev-dependencies project-info) "dev dependencies")]
    [:td {:class "codes"
          :style "text-align: center; vertical-align: middle;color: #666;padding-right:20px"}
     [:br]
     [:br]
     [:br]
     "(this space intentionally left almost blank)"]]))

(defn link-to-namespace
  "Creates an 'a' tag pointing to the `namespace-name`, either as an anchor (if
  `anchor?` is true) or as a link to a separate `$namespace-name.html` file.
  If `attrs` aren't empty, they are added to the resulting tag."
  [namespace-name anchor? & attrs]
  [:a (into {:href (if anchor?
                   (str "#" namespace-name)
                   (str namespace-name ".html"))}
            attrs)
   namespace-name])

(defn link-to-toc
  "This is a hack, as in the case when `anchor?` is false, the link will contain
  a reference to `toc.html` which might not even exist."
  [anchor?]
  (link-to-namespace "toc" anchor? {:class "toc-link"}))

(defn toc-html [props docs]
  (html
   [:tr
    [:td {:class "docs"}
     [:div {:class "toc"}
      [:a {:name "toc"} [:h3 "namespaces"]]
      [:ul
       (map #(vector :li (link-to-namespace (:ns %) (:uberdoc? props)))
            docs)]]]
    [:td {:class "codes"} "&nbsp;"]]))

(defn floating-toc-html [docs]
  [:div {:id "floating-toc"}
   [:ul
    (map #(vector :li {:class "floating-toc-li"
                       :id (str "floating-toc_" (:ns %))}
                  (:ns %))
         docs)]])

(defn groups-html [props doc]
  (html
   [:tr
    [:td {:class "docs"}
     [:div {:class "docs-header"}
      [:a {:class "anchor" :name (:ns doc) :href (str "#" (:ns doc))}
       [:h1 {:class "project-name"}
        (:ns doc)]
       (link-to-toc (:uberdoc? props))]]]
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
           :color "#252519"
           :background-color "#F5F5FF"}]
   [:h1 {:font-size "20px"
         :margin-top 0}]
   [:h2 {:font-size "18px"}]
   [:h3 {:font-size "16px"}]
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
              :border "none"
              :background-color "#FFF"}]
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
   saying that all this is WIP and will probably change in the future."
  [project-metadata opt-resources header toc content floating-toc]
  (html
   "<!DOCTYPE html>\n"
   [:html
    [:head
     [:meta {:http-equiv "Content-Type" :content "text/html" :charset "utf-8"}]
     [:meta {:name "description" :content (:description project-metadata)}]
     (metadata-html project-metadata)
     (inline-css (str *resources* "shCore.css"))
     (css
      [:.syntaxhighlighter {:overflow "hidden !important"}])
     (inline-css (str *resources* "shThemeMarginalia.css"))
     reset-css
     header-css
     floating-toc-css
     general-css
     (inline-js (str *resources* "jquery-1.7.1.min.js"))
     (inline-js (str *resources* "xregexp-min.js"))
     (inline-js (str *resources* "shCore.js"))
     (inline-js (str *resources* "shBrushClojure.js"))
     opt-resources
     [:title (:name project-metadata) " -- Marginalia"]]
    [:body
     [:table
      header
      toc
      content]
     [:div {:class "footer"}
      "Generated by "
      [:a {:href "https://github.com/clj-commons/marginalia"} "Marginalia"]
      ".&nbsp;&nbsp;"
      "Syntax highlighting provided by Alex Gorbatchev's "
      [:a {:href "http://alexgorbatchev.com/SyntaxHighlighter/"}
       "SyntaxHighlighter"]
      floating-toc]
     (inline-js (str *resources* "app.js"))]]))


;; Syntax highlighting is done a bit differently than docco.  Instead of embedding
;; the highlighting metadata on the parse / html gen phase, we use [SyntaxHighlighter](http://alexgorbatchev.com/SyntaxHighlighter/)
;; to do it in javascript.

(defn uberdoc-html
  "This generates a stand alone html file (think `lein uberjar`).
   It's probably the only var consumers will use."
  [project-metadata docs]
  (page-template
   project-metadata
   (opt-resources-html project-metadata)
   (header-html project-metadata)
   (toc-html {:uberdoc? true} docs)
   (map #(groups-html {:uberdoc? true} %) docs)
   (floating-toc-html docs)))

(defn index-html
  [project-metadata docs]
  (page-template
   project-metadata
   (opt-resources-html project-metadata)
   (header-html project-metadata)
   (toc-html {:uberdoc? false} docs)
   ""   ;; no contents
   "")) ;; no floating toc

(defn single-page-html
  [project-metadata doc all-docs]
  (page-template
   project-metadata
   (opt-resources-html project-metadata)
   "" ;; no header
   "" ;; no toc
   (groups-html {:uberdoc? false} doc)
   "" ;; no floating toc
   ))

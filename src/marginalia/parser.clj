;; This file contains the complete Marginalia parser.
;; It leverages the Clojure reader instead of implementing a complete
;; Clojure parsing solution.
(ns marginalia.parser
  "Provides the parsing facilities for Marginalia."
  (:require
   [cljs.tagged-literals :as cljs]
   [clojure.string :as str]
   [clojure.tools.namespace :as tools.ns])
  (:import
   [clojure.lang LineNumberingPushbackReader LispReader]
   [java.io File Reader Writer]))

(set! *warn-on-reflection* true)

;; Extracted from clojure.contrib.reflect
(defn get-field
  "Access to private or protected field.  field-name is a symbol or
  keyword."
  [^Class klass field-name obj]
  (->  klass
      (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

;; Extracted from clojure.contrib.reflect
(defn call-method
  "Calls a private or protected method.

   params is a vector of classes which correspond to the arguments to
   the method e

   obj is nil for static methods, the instance object otherwise.

   The method-name is given a symbol or a keyword (something Named)."
  [^Class klass method-name params obj & args]
  (-> klass
      (.getDeclaredMethod (name method-name)
                          (into-array Class params))
      (doto (.setAccessible true))
      (.invoke obj (into-array Object args))))

(defrecord Comment [content])

(defmethod print-method Comment [^Comment the-comment ^Writer out]
  (.write out (str \" (.content the-comment) \")))

(def ^:private top-level-comments (atom []))
(def ^:private sub-level-comments (atom []))

(def ^:dynamic *comments* "TODO: document" nil)
(def ^:dynamic *comments-enabled* "TODO: document" nil)
(def ^:dynamic *lift-inline-comments* "TODO: document" nil)
(def ^:dynamic *delete-lifted-comments*"TODO: document" nil)

(defn- comments-enabled?
  []
  @*comments-enabled*)

(def directives
  "Marginalia can be given directives in comments.  A directive is a comment
   line containing a directive name, in the form `;; @DirectiveName`.
   Directives change the behavior of the parser within the files that contain
   them.

   The following directives are defined:

   * `@MargDisable` suppresses subsequent comments from the docs
   * `@MargEnable` includes subsequent comments in the docs"
  {"MargDisable" (fn [] (swap! *comments-enabled* (constantly false)))
   "MargEnable"  (fn [] (swap! *comments-enabled* (constantly true)))})

(defn process-directive!
  "If the given line is a directive, applies it.  Returns a value
   indicating whether the line should be included in the comments
   list."
  [line]
  (let [directive (->> (re-find #"^;+\s*@(\w+)" line)
                       (last)
                       (get directives))]
    (when directive
      (directive))
    (not directive)))

(defn- read-comment
  ([^LineNumberingPushbackReader reader semicolon]
   (let [sb (StringBuilder.)]
     (.append sb semicolon)
     (loop [c (.read reader)]
       (let [ch (char c)]
         (if (or (= ch \newline)
                 (= ch \return))
           (let [line (dec (.getLineNumber reader))
                 text (.toString sb)
                 include? (process-directive! text)]
             (when (and include? (comments-enabled?))
               (swap! *comments* conj {:form (Comment. text)
                                       :text [text]
                                       :start line
                                       :end line}))
             reader)
           (do
             (.append sb (Character/toString ch))
             (recur (.read reader))))))))
  ([^Reader reader semicolon _opts _pending] ;; TODO: who uses this?
   (read-comment reader semicolon)))

(defn- set-comment-reader [^Reader reader]
  (aset ^"[Lclojure.lang.IFn;" (get-field LispReader :macros nil)
        (int \;)
        reader))

(defrecord DoubleColonKeyword [content])

(defmethod print-method DoubleColonKeyword [^DoubleColonKeyword dck ^java.io.Writer out]
  (.write out (str \: (.content dck))))

(defmethod print-dup DoubleColonKeyword [dck ^java.io.Writer out]
  (print-method dck out))

(defn ^:private read-token [reader c]
  (call-method clojure.lang.LispReader :readToken
               [java.io.PushbackReader Character/TYPE]
               nil reader c))

;;; Clojure 1.9 changed the signature of LispReader/matchSymbol, taking a new
;;; parameter of type LispReader$Resolver.  Conveniently, we can test for the
;;; existence of the *reader-resolver* var to detect running under 1.9.
;;;
;;; We must take care to use the correct overload for the project's runtime,
;;; else we will crash and fail people's builds.
(if-let [resolver-var (resolve '*reader-resolver*)]
  (defn ^:private match-symbol [s]
    (call-method LispReader :matchSymbol
                 [String, (Class/forName "clojure.lang.LispReader$Resolver")]
                 nil s (deref resolver-var)))
  (defn ^:private match-symbol [s]
    (call-method LispReader :matchSymbol
                 [String]
                 nil s)))

(defn- read-keyword
  ([^LineNumberingPushbackReader reader colon]
   (let [c (.read reader)]
     (if (= (int \:) c)
       (-> (read-token reader (char c))
           match-symbol
           DoubleColonKeyword.)
       (do (.unread reader c)
           (-> (read-token reader colon)
               match-symbol)))))
  ([^LineNumberingPushbackReader reader colon _opts _pending] ;; TODO: who uses this?
   (read-keyword reader colon)))

(defn- set-keyword-reader [reader]
  (aset ^"[Lclojure.lang.IFn;" (get-field LispReader :macros nil)
        (int \:)
        reader))

(defn- skip-spaces-and-comments [^LineNumberingPushbackReader rdr]
  (loop [c (.read rdr)]
    (cond
      (= c -1)
      nil

      (= (char c) \;)
      (do (read-comment rdr \;)
          (recur (.read rdr)))

      (#{\space \tab \return \newline \,} (char c))
      (recur (.read rdr))

      :else
      (.unread rdr c))))

(declare adjacent?)
(declare merge-comments)

(defn- parse* [^LineNumberingPushbackReader reader]
  (take-while
   #(not= :_eof (:form %))
   (flatten
    (repeatedly
     (fn []
       (binding [*comments* top-level-comments]
         (skip-spaces-and-comments reader))
       (let [start                 (.getLineNumber reader)
             form                  (binding [*comments* sub-level-comments]
                                     (try (. LispReader
                                             (read reader {:read-cond :allow
                                                           :eof       :_eof}))
                                          (catch Exception ex
                                            (let [msg (str "Problem parsing near line " start
                                                           " <" (.readLine reader) ">"
                                                           " original reported cause is "
                                                           (.getCause ex) " -- "
                                                           (.getMessage ex))
                                                  e (RuntimeException. msg)]
                                              (.setStackTrace e (.getStackTrace ex))
                                              (throw e)))))
             end                   (.getLineNumber reader)
             code                  {:form form :start start :end end}
             ;; We optionally lift inline comments to the top of the form.
             ;; This monstrosity ensures that each consecutive group of inline
             ;; comments is treated as a mergable block, but with a fake
             ;; blank comment between non-adjacent inline comments. When merged
             ;; and converted to markdown, this will produce a paragraph for
             ;; each separate block of inline comments.
             paragraph-comment     {:form (Comment. ";;")
                                    :text [";;"]}
             merge-inline-comments (fn [cs c]
                                     (if (re-find #"^;(\s|$)"
                                                  (.content ^Comment (:form c)))
                                       cs
                                       (if-let [t (peek cs)]
                                         (if (adjacent? t c)
                                           (conj cs c)
                                           (conj cs paragraph-comment c))
                                         (conj cs c))))
             inline-comments       (when (and *lift-inline-comments*
                                              (seq @sub-level-comments))
                                     (cond->> (reduce merge-inline-comments
                                                      []
                                                      @sub-level-comments)
                                       (seq @top-level-comments)
                                       (into [paragraph-comment])
                                       true
                                       (mapv #(assoc % :start start :end (dec start)))))
             comments              (concat @top-level-comments inline-comments)]
         (swap! top-level-comments (constantly []))
         (swap! sub-level-comments (constantly []))
         (if (empty? comments)
           [code]
           (vec (concat comments [code])))))))))

(defn- strip-docstring [docstring raw]
  (-> raw
      (str/replace (str \" (-> docstring
                               str
                               (str/replace "\"" "\\\""))
                        \")
                   "")
      (str/replace #"#?\^\{\s*:doc\s*\}" "")
      (str/replace #"\n\s*\n" "\n")
      (str/replace #"\n\s*\)" ")")))

(defn- get-var-docstring [nspace-sym sym]
  (let [s (if nspace-sym
            (symbol (str nspace-sym) (str sym))
            (symbol (str sym)))]
    (try
      (-> `(var ~s) eval meta :doc)
      ;; HACK: to handle types
      (catch Exception _))))

(defmulti dispatch-form "TODO: document"
  (fn [form _ _]
    (if (seq? form)
      (first form)
      form)))

(defn- extract-common-docstring
  [form raw nspace-sym]
  (let [sym (second form)]
    (if (symbol? sym)
      (let [maybe-metadocstring  (:doc (meta sym))
            nspace               (find-ns sym)
            [maybe-ds remainder] (let [[_ _ ? & more?] form] [? more?])
            docstring            (if (and (string? maybe-ds) remainder)
                                   maybe-ds
                                   (if (= (first form) 'ns)
                                     (if (not maybe-metadocstring)
                                       (when (string? maybe-ds) maybe-ds)
                                       maybe-metadocstring)
                                     (if-let [ds maybe-metadocstring]
                                       ds
                                       (when nspace
                                         (-> nspace meta :doc)
                                         (get-var-docstring nspace-sym sym)))))]
        [#_form
         (when docstring
           ;; Exclude flush left docstrings from adjustment:
           (if (re-find #"\n[^\s]" docstring)
             docstring
             (str/replace docstring #"\n  " "\n")))
         #_raw
         (strip-docstring docstring raw)
         #_nspace-sym
         (if (or (= 'ns (first form)) nspace) sym nspace-sym)])
      [nil raw nspace-sym])))

(defn- extract-impl-docstring
  [fn-body]
  (filter string? (rest fn-body)))

(defn- extract-internal-docstrings
  [body]
  (mapcat extract-impl-docstring
          body))

(defmethod dispatch-form 'defprotocol
  [form raw nspace-sym]
  (let [[ds r s]      (extract-common-docstring form raw nspace-sym)
        ;; Clojure 1.10 added `:extend-via-metadata` to the `defprotocol` macro.
        ;; If the flag is present, `extract-internal-docstrings` needs to start
        ;; 2 forms later, to account for the presence of a keyword,
        ;; `:extend-via-metadata` and a boolean `true` in the macro body.
        evm           (contains? (set form) :extend-via-metadata)
        internal-dses (cond
                        (and evm ds) (extract-internal-docstrings (nthnext form 5))
                        evm          (extract-internal-docstrings (nthnext form 4))
                        ds           (extract-internal-docstrings (nthnext form 3))
                        :else        (extract-internal-docstrings (nthnext form 2)))]
    (with-meta
      [ds r s]
      {:internal-docstrings internal-dses})))

(defmethod dispatch-form 'ns
  [form raw nspace-sym]
  (extract-common-docstring form raw nspace-sym))

(defmethod dispatch-form 'def
  [form raw nspace-sym]
  (extract-common-docstring form raw nspace-sym))

(defmethod dispatch-form 'defn
  [form raw nspace-sym]
  (extract-common-docstring form raw nspace-sym))

(defmethod dispatch-form 'defn-
  [form raw nspace-sym]
  (extract-common-docstring form raw nspace-sym))

(defmethod dispatch-form 'defmulti
  [form raw nspace-sym]
  (extract-common-docstring form raw nspace-sym))

(defmethod dispatch-form 'defmethod
  [_form raw nspace-sym]
  [nil raw nspace-sym])

(defn- dispatch-inner-form
  [form raw nspace-sym]
  (conj
   (reduce (fn [[adoc araw] inner-form]
             (if (seq? inner-form)
               (let [[d r] (dispatch-form inner-form
                                          araw
                                          nspace-sym)]
                 [(str adoc d) r])
               [adoc araw]))
           [nil raw]
           form)
   nspace-sym))

(defn- dispatch-literal
  [_form raw _nspace-sym]
  [nil raw])

(defn- literal-form? [form]
  (or (string? form) (number? form) (keyword? form) (symbol? form)
      (char? form) (true? form) (false? form) (instance? java.util.regex.Pattern form)))

(defmethod dispatch-form :default
  [form raw nspace-sym]
  (cond (literal-form? form)
        (dispatch-literal form raw nspace-sym)
        (and (first form)
             (.isInstance clojure.lang.Named (first form))
             (re-find #"^def" (-> form first name)))
          (extract-common-docstring form raw nspace-sym)
        :else
          (dispatch-inner-form form raw nspace-sym)))

(defn- extract-docstring [{:keys [start end form]} raw-lines nspace-sym]
  (let [new-lines (str/join "\n" (subvec raw-lines (dec start) (min end (count raw-lines))))]
    (dispatch-form form new-lines nspace-sym)))

(defn- ->str [{:keys [form]}]
  (-> (.content ^Comment form)
      (str/replace #"^;+\s(\s*)" "$1")
      (str/replace #"^;+" "")))

(defn- merge-comments [f s]
  {:form (Comment. (str (->str f) "\n" (->str s)))
   :text (into (:text f) (:text s))
   :start (:start f)
   :end (:end s)})

(defn- comment? [{:keys [form]}]
  (instance? Comment form))

(defn- code? [{:keys [form] :as o}]
  (and (not (nil? form))
       (not (comment? o))))

(defn adjacent?
  "Two parsed objects are adjacent if the end of the first is followed by the start of the second."
  [{:keys [end] :as _first} {:keys [start] :as _second}]
  (= end (dec start)))

(defn- arrange-in-sections [parsed-code raw-code]
  (loop [sections []
         f        (first parsed-code)
         s        (second parsed-code)
         nn       (nnext parsed-code)
         nspace   nil]
    (if f
      (cond
        ;; ignore comments with only one semicolon
        (and (comment? f) (re-find #"^;(\s|$)" (.content ^Comment (:form f))))
        (recur sections s (first nn) (next nn) nspace)
        ;; merging comments block
        (and (comment? f) (comment? s) (adjacent? f s))
        (recur sections (merge-comments f s)
               (first nn) (next nn)
               nspace)
        ;; merging adjacent code blocks
        (and (code? f) (code? s) (adjacent? f s))
        (let [[fdoc fcode nspace] (extract-docstring f raw-code nspace)
              [sdoc scode _]      (extract-docstring s raw-code nspace)]
          (recur sections (assoc s
                                 :type      :code
                                 :raw       (str (or (:raw f) fcode) "\n" scode)
                                 :docstring (str (or (:docstring f) fdoc) "\n\n" sdoc))
                 (first nn) (next nn) nspace))
        ;; adjacent comments are added as extra documentation to code block
        (and (comment? f) (code? s) (adjacent? f s))
        (let [[doc code nspace] (extract-docstring s raw-code nspace)]
          (recur sections (assoc s
                                 :type :code
                                 :raw (if *delete-lifted-comments*
                                        ;; this is far from perfect but should work
                                        ;; for most cases: erase matching comments
                                        ;; and then remove lines that are blank
                                        (-> (reduce (fn [raw the-comment]
                                                      (str/replace raw
                                                                   (str the-comment "\n")
                                                                   "\n"))
                                                    code
                                                    (:text f))
                                            (str/replace #"\n\s+\n" "\n"))
                                        code)
                                 :docstring (str doc "\n\n" (->str f)))
                 (first nn) (next nn) nspace))
        ;; adding comment section
        (comment? f)
        (recur (conj sections (assoc f :type :comment :raw (->str f)))
               s
               (first nn) (next nn)
               nspace)
        ;; adding code section
        :else
        (let [[doc code nspace] (extract-docstring f raw-code nspace)]
          (recur (conj sections (if (= (:type f) :code)
                                  f
                                  {:type :code
                                   :raw code
                                   :docstring doc}))
                 s (first nn) (next nn) nspace)))
      sections)))

(defn parse
  "Return a parse tree for the given code (provided as a string)"
  [source-string]
  (let [make-reader #(java.io.BufferedReader.
                      (java.io.StringReader. (str source-string "\n")))
        lines       (vec (line-seq (make-reader)))
        reader      (clojure.lang.LineNumberingPushbackReader. (make-reader))
        old-cmt-rdr (aget ^"[Lclojure.lang.IFn;" (get-field LispReader :macros nil) (int \;))]
    (try
      (set-comment-reader read-comment)
      (set-keyword-reader read-keyword)
      (let [parsed-code (-> reader parse* doall)]
        (set-comment-reader old-cmt-rdr)
        (set-keyword-reader nil)
        (arrange-in-sections parsed-code lines))
      (catch Exception e
        (set-comment-reader old-cmt-rdr)
        (set-keyword-reader nil)
        (throw e)))))

(defn- cljs-file? [filepath]
  (str/ends-with? (str/lower-case filepath) "cljs"))

(defn- cljx-file? [filepath]
  (str/ends-with? (str/lower-case filepath) "cljx"))

(def ^:private cljx-data-readers {'+clj identity
                        '+cljs identity})

(defmacro ^:private with-readers-for [file & body]
  `(let [readers# (merge {}
                        (when (cljs-file? ~file) cljs/*cljs-data-readers*)
                        (when (cljx-file? ~file) cljx-data-readers)
                        default-data-readers)]
     (binding [*data-readers* readers#]
       ~@body)))

(defn parse-file
  "Return a parse tree for the given `filename`"
  [filename]
  (with-readers-for filename
    (binding [*comments-enabled* (atom true)]
      (parse (slurp filename)))))

(defn parse-ns
  "Return a parse tree for all the files in the namespace"
  [^File file]
  (let [filename (.getName file)]
    (with-readers-for filename
      (or (not-empty (-> file
                         (tools.ns/read-file-ns-decl)
                         (second)
                         (str)))
          filename))))

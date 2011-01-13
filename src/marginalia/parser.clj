(ns marginalia.parser
  "Provides the parsing facilities for Marginalia."
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:use clojure.contrib.reflect))

(defrecord CommentLine [line comment-str])

(defn- read-comment
  [reader semicolon]
  (let [sb (StringBuilder.)]
    (.append sb semicolon)
    (loop [ch (char (.read reader))]
      (if (or (= ch \newline)
              (= ch \return)
              (= ch -1))
        (CommentLine. (.getLineNumber reader) (.toString sb))
        (do
          (.append sb (Character/toString ch))
          (recur (char (.read reader))))))))

(defn make-parse-fn
  [src]
  (let [rdr (clojure.lang.LineNumberingPushbackReader. (java.io.StringReader. src))]
    (fn []
      (let [old-cmt-rdr (aget (get-field clojure.lang.LispReader :macros nil) (int \;))]
        (aset (get-field clojure.lang.LispReader :macros nil) (int \;) read-comment)
        (let [result (read rdr)]
          (aset (get-field clojure.lang.LispReader :macros nil) (int \;) old-cmt-rdr)
          result)))))

(comment
  (def R (make-parse-fn ";; Some Comment
        ;; with two lines
        ;  maybe 3?
        ;

        (defn hello-world
          \"This is a docstring\"
          [name]
          (pritnln \"hello\" name \"!\"))"))

  (R))
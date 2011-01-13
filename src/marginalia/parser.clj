;; This file contains the complete Marginalia parser.
;; It leverages the Clojure reader instead of implementing a complete
;; Clojure parsing solution.
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

(defn parse-file
  [filepath]
  (let [parser (make-parse-fn (slurp filepath))]
    (loop [lines []]
      (if-let [result (try (parser) (catch Exception _ nil))]
        (recur (conj lines result))
        lines))))

(comment
  (parse-file "/tmp/parser.clj")
)

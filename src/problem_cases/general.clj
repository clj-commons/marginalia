(ns problem-cases.general
  "A place to examine poor parser behavior.  These should go in tests when they get written."
  )


;; Should have only this comment in the left margin.
;; See [https://github.com/fogus/marginalia/issues/#issue/4](https://github.com/fogus/marginalia/issues/#issue/4)

(defn parse-bool [v] (condp = (.trim (text v))
                         "0" false
                         "1" true
                         "throw exception here"))

(defn a-function "Here is a docstring. It should be to the left."
  [x]
  (* x x))

(defn b-function
  "Here is a docstring. It should be to the left."
  [x]
  "Here is just a string.  It should be to the right."
  (* x x))

(defprotocol Relation
  (select     [this predicate]
    "Confines the query to rows for which the predicate is true

     Ex. (select (table :users) (where (= :id 5)))")
  (join       [this table2 join_on]
    "Joins two tables on join_on

     Ex. (join (table :one) (table :two) :id)
         (join (table :one) (table :two)
               (where (= :one.col :two.col)))"))

(defmulti bazfoo
  "This is a defmulti docstring, it should also be on the left"
  class)

(defmethod bazfoo String [s]
  "This is a defmethod docstring.  It should be on the left."
  (vec (seq s)))

(bazfoo "abc")

(defprotocol Foo
  "This is a protocol docstring.  It should be on the left."
  (lookup  [cache e])
  (has?    [cache e] )
  (hit     [cache e])
  (miss    [cache e ret]))

(def ^{:doc "This is also a docstring via metadata. It should be on the left."}
  a 42)

(def ^{:doc
       "This is also a docstring via metadata. It should be on the left."}
  b 42)

(def ^{:doc
       "This is also a docstring via metadata. It should be on the left."}
  c
  "This is just a value.  It should be on the right.")

;; From [fnparse](https://github.com/joshua-choi/fnparse)

; Define single-character indicator rules.
; I use `clojure.template/do-template` to reduce repetition.
(do-template [rule-name token]
  (h/defrule rule-name
    "Padded on the front with optional whitespace."
    (h/lit token))
  <escape-char-start> \\
  <str-delimiter>   \"
  <value-separator> \,
  <name-separator>  \:
  <array-start>     \[
  <array-end>       \]
  <object-start>    \{
  <object-end>      \})

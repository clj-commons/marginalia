(ns problem-cases.general
  "A place to examine poor parser behavior.  These should go in tests when they get written."
  )


;; Should have only this comment in the left margin.
;; See [https://github.com/fogus/marginalia/issues/#issue/4](https://github.com/fogus/marginalia/issues/#issue/4)

(defn parse-bool [v] (condp = (.trim (str v))
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
  "Defines a relation... duh!"
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
(comment
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
               <object-end>      \}))

(defmulti kompile identity)

(defmethod kompile [::standard AutoIncClause]
  "This is a docstring.  On the left."
  [_]
  "GENERATED ALWAYS AS IDENTITY")

(defn strict-eval-op-fn
  "`strict-eval-op-fn` is used to define functions of the above pattern for fuctions such as `+`, `*`,   etc.  Cljs special forms defined this way are applyable, such as `(apply + [1 2 3])`.

   Resulting expressions are wrapped in an anonymous function and, down the line, `call`ed, like so:

       (+ 1 2 3) -> (function(){...}.call(this, 1 2 3)"
  [op inc-ind-str ind-str op nl]
  (ind-str
   "(function() {" nl
   (inc-ind-str
    "var _out = arguments[0];" nl
    "for(var _i=1; _i<arguments.length; _i++) {" nl
    (inc-ind-str
     "_out = _out " op " arguments[_i];")
    nl
    "}" nl
    "return _out;")
   nl
   "})"))


'(defn special-forms []
  {'def     handle-def
   'fn      handle-fn
   'fn*     handle-fn
   'set!    handle-set
   'let     handle-let
   'defn    handle-defn
   'aget    handle-aget
   'aset    handle-aset
   'if      handle-if
   'while   handle-while
   'when    handle-when
   'doto    handle-doto
   '->      handle-->
   '->>     handle-->>
   'not     handle-not
   'do      handle-do
   'cond    handle-cond
   '=       (make-lazy-op '==)
   '>       (make-lazy-op '>)
   '<       (make-lazy-op '<)
   '>=      (make-lazy-op '>=)
   '<=      (make-lazy-op '<=)
   'or      (make-lazy-op '||)
   'and     (make-lazy-op '&&)
   'doseq   handle-doseq
   'instanceof handle-instanceof
   'gensym handle-gensym
   'gensym-str handle-gensym-str})

'(defn greater [a b]
  (>= a b))

'(fact
  (greater 2 1) => truthy)

'(file->tickets commits)


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
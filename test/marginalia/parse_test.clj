(ns marginalia.parse-test
  "This module does stuff"
  (:require
   [clojure.test :refer :all]
   [marginalia.parser :as p]))

(deftest test-inline-literals
  (is (= 1 (count (p/parse "(ns test)"))))
  (is (= 1 (count (p/parse "(ns test)\n123"))))
  (is (= 1 (count (p/parse "(ns test)\n123\n"))))
  (is (= 1 (count (p/parse "(ns test)\n\"string\""))))
  (is (= 1 (count (p/parse "(ns test)\n\"some string\""))))
  (is (= 1 (count (p/parse "(ns test (:require [marginalia.parser :as parser]))\n(defn foo [] ::parser/foo)")))))

(def def-docstring
  "(def PI \"a docstring\" 3.1415926)")

(def defn-docstring
  "(defn some-fn
  \"the docstring\"
  [x]
  (* x x))")

(def markdown-docstring-fn
  "(defn some-fn
  \"the docstring

  ```clojure
  (some-fn 1 :abc #{['foo \\\"bar\\\"]})
  ```\"
  [& args]
  (vec args))")

(def defmulti-docstring
  "(defmulti bazfoo
  \"This is a defmulti docstring\"
  class)")

(def defmethod-docstring
  "(defmethod bazfoo
  \"This is a defmethod docstring\"
  String [s]
  (vec (seq s)))")

(def defprotocol-docstring
  "(defprotocol Foo \"Does a Foo\"
    :extend-via-metadata true
    (do-foo! [_ opts] \"Foo!\"))")

(def schema-return-type-fn
  "(defn some-fn :- :string
  \"the docstring\"
  [x]
  (str x))")

(def schema-return-type-qualified-fn
  "(defn some-fn :- ::output/schema
  \"the docstring\"
  [x]
  (str x))")

(def schema-return-type-vector-fn
  "(defn some-fn :- [:maybe :int]
  \"the docstring\"
  [x]
  (str x))")

(def reader-conditional-fn
  "(defn error
  \"Returns a language-appropriate error\"
  [^String msg]
  #?(:clj  (Exception. msg)
     :cljs (js/Error. msg)))")

(deftest test-parse-fn-docstring
  (are [input expected] (let [{docstring :docstring the-type :type} (first (p/parse input))]
                          (is (= :code the-type))
                          (is (= expected docstring))
                          true)
    def-docstring "a docstring"
    defn-docstring "the docstring"
    markdown-docstring-fn "the docstring\n\n```clojure\n(some-fn 1 :abc #{['foo \"bar\"]})\n```"
    defmulti-docstring "This is a defmulti docstring"
    defmethod-docstring "This is a defmethod docstring"
    defprotocol-docstring "Does a Foo"
    schema-return-type-fn "the docstring"
    schema-return-type-qualified-fn "the docstring"
    schema-return-type-vector-fn "the docstring"
    reader-conditional-fn "Returns a language-appropriate error"))

(deftest inline-comments
  (testing "inline comments ignored by default"
    (binding [p/*comments-enabled* (atom true)]
      (let [result (p/parse
                    "(ns test)

                     (defn foo
                       \"docstring\"
                       []
                       (let [x 1]
                         ;; A
                         x))")]
        (is (= 2 (count result)))
        (is (re-find #";; A" (:raw (second result))))
        (is (= "docstring" (:docstring (second result))))))

    (binding [p/*comments-enabled* (atom true)]
      ;; tests that prelude is appended to docstring
      (let [result (p/parse
                    "(ns test)

                     ;; A
                     (defn foo
                       \"docstring\"
                       []
                       (let [x 1]
                         ;; B
                         x))")]
        (is (= 2 (count result)))
        (is (re-find #";; B" (:raw (second result))))
        (is (= "docstring\n\nA" (:docstring (second result)))))))

  (testing "inline single ; comments still ignored"
    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true]
      (let [result (p/parse
                    "(ns test)

                     (defn foo
                       \"docstring\"
                       []
                       (let [x 1]
                         ; A
                         x))")]
        (is (= 2 (count result)))
        (is (re-find #"; A" (:raw (second result))))
        (is (= "docstring" (:docstring (second result)))))))

  (testing "inline comments added to docstring as paragraphs"
    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true]
      (let [result (p/parse
                    "(ns test)

                     (defn foo
                       \"docstring\"
                       []
                       (let [x 1]
                         ;; A
                         x))")]
        (is (= 2 (count result)))
        (is (re-find #";; A" (:raw (second result))))
        (is (= "docstring\n\nA" (:docstring (second result))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true]
      ;; A and B should be separate paragraphs
      (let [result (p/parse
                    "(ns test)

                     (defn foo
                       \"docstring\"
                       []
                       ;; A
                       (let [x 1]
                         ;; B
                         x))")]
        (is (= 2 (count result)))
        (is (re-find #";; A" (:raw (second result))))
        (is (re-find #";; B" (:raw (second result))))
        (is (= "docstring\n\nA\n\nB" (:docstring (second result)))))))

  (testing "inline comments added to prelude after docstring"
    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true]
      ;; prelude A follows docstring, then B and C as separate paragraphs
      (let [result (p/parse
                    "(ns test)

                     ;; A
                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       (let [x 1]
                         ;; C
                         x))")]
        (is (= 2 (count result)))
        (is (not (re-find #";; A" (:raw (second result)))))
        (is (re-find #";; B" (:raw (second result))))
        (is (re-find #";; C" (:raw (second result))))
        (is (= "docstring\n\nA\n\nB\n\nC" (:docstring (second result))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true]
      ;; this checks that consecutive comment lines stay in the same paragraph
      (let [result (p/parse
                    "(ns test)

                     ;; A
                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       ;; C
                       (let [x 1]
                         ;; D
                         x))")]
        (is (= 2 (count result)))
        (is (re-find #";; B" (:raw (second result))))
        (is (re-find #";; C" (:raw (second result))))
        (is (re-find #";; D" (:raw (second result))))
        (is (= "docstring\n\nA\n\nB\nC\n\nD" (:docstring (second result))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true]
      ;; this checks that a comment above the function doesn't merge in
      ;; when separated by a blank line
      (let [result (p/parse
                    "(ns test)

                     ;; A

                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       ;; C
                       (let [x 1]
                         ;; D
                         x))")]
        (is (= 3 (count result)))
        (is (= "A" (:raw (second result))))
        (is (re-find #";; B" (:raw (nth result 2))))
        (is (re-find #";; C" (:raw (nth result 2))))
        (is (re-find #";; D" (:raw (nth result 2))))
        (is (= "docstring\n\n\nB\nC\n\nD" (:docstring (nth result 2))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true]
      ;; this checks that a comment above the function does merge in
      ;; when a blank comment joins it to the function
      (let [result (p/parse
                    "(ns test)

                     ;; A
                     ;;
                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       ;; C
                       (let [x 1]
                         ;; D
                         x))")]
        (is (= 2 (count result)))
        (is (not (re-find #";; A" (:raw (second result)))))
        (is (re-find #";; B" (:raw (second result))))
        (is (re-find #";; C" (:raw (second result))))
        (is (re-find #";; D" (:raw (second result))))
        (is (= "docstring\n\nA\n\n\nB\nC\n\nD" (:docstring (second result))))))))

(deftest inline-comments-deleted
  (testing "inline comments added to docstring as paragraphs and deleted"
    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true
              p/*delete-lifted-comments* true]
      (let [result (p/parse
                     "(ns test)

                     (defn foo
                       \"docstring\"
                       []
                       (let [x 1]
                         ;; A
                         x))")]
        (is (= 2 (count result)))
        (is (not (re-find #";; A" (:raw (second result)))))
        (is (= "docstring\n\nA" (:docstring (second result))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true
              p/*delete-lifted-comments* true]
      ;; A and B should be separate paragraphs
      (let [result (p/parse
                     "(ns test)

                     (defn foo
                       \"docstring\"
                       []
                       ;; A
                       (let [x 1]
                         ;; B
                         x))")]
        (is (= 2 (count result)))
        (is (not (re-find #";; A" (:raw (second result)))))
        (is (not (re-find #";; B" (:raw (second result)))))
        (is (= "docstring\n\nA\n\nB" (:docstring (second result)))))))

  (testing "inline comments added to prelude after docstring"
    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true
              p/*delete-lifted-comments* true]
      ;; prelude A follows docstring, then B and C as separate paragraphs
      (let [result (p/parse
                     "(ns test)

                     ;; A
                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       (let [x 1]
                         ;; C
                         x))")]
        (is (= 2 (count result)))
        (is (not (re-find #";; A" (:raw (second result)))))
        (is (not (re-find #";; B" (:raw (second result)))))
        (is (not (re-find #";; C" (:raw (second result)))))
        (is (= "docstring\n\nA\n\nB\n\nC" (:docstring (second result))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true
              p/*delete-lifted-comments* true]
      ;; this checks that consecutive comment lines stay in the same paragraph
      (let [result (p/parse
                     "(ns test)

                     ;; A
                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       ;; C
                       (let [x 1]
                         ;; D
                         x))")]
        (is (= 2 (count result)))
        (is (not (re-find #";; B" (:raw (second result)))))
        (is (not (re-find #";; C" (:raw (second result)))))
        (is (not (re-find #";; D" (:raw (second result)))))
        (is (= "docstring\n\nA\n\nB\nC\n\nD" (:docstring (second result))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true
              p/*delete-lifted-comments* true]
      ;; this checks that a comment above the function doesn't merge in
      ;; when separated by a blank line
      (let [result (p/parse
                     "(ns test)

                     ;; A

                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       ;; C
                       (let [x 1]
                         ;; D
                         x))")]
        (is (= 3 (count result)))
        (is (= "A" (:raw (second result))))
        (is (not (re-find #";; B" (:raw (nth result 2)))))
        (is (not (re-find #";; C" (:raw (nth result 2)))))
        (is (not (re-find #";; D" (:raw (nth result 2)))))
        (is (= "docstring\n\n\nB\nC\n\nD" (:docstring (nth result 2))))))

    (binding [p/*comments-enabled* (atom true)
              p/*lift-inline-comments* true
              p/*delete-lifted-comments* true]
      ;; this checks that a comment above the function does merge in
      ;; when a blank comment joins it to the function
      (let [result (p/parse
                     "(ns test)

                     ;; A
                     ;;
                     (defn foo
                       \"docstring\"
                       []
                       ;; B
                       ;; C
                       (let [x 1]
                         ;; D
                         x))")]
        (is (= 2 (count result)))
        (is (not (re-find #";; A" (:raw (second result)))))
        (is (not (re-find #";; B" (:raw (second result)))))
        (is (not (re-find #";; C" (:raw (second result)))))
        (is (not (re-find #";; D" (:raw (second result)))))
        (is (= "docstring\n\nA\n\n\nB\nC\n\nD" (:docstring (second result))))))))

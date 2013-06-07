(ns marginalia.test.parse
  "This module does stuff"
  (:use clojure.test)
  (:require marginalia.parser))

(deftest test-inline-literals
  (is (= (count (marginalia.parser/parse "(ns test)")) 1))
  ;; (is (= (count (marginalia.parser/parse "(ns test)\n123")) 1)) ;; still failing
  (is (= (count (marginalia.parser/parse "(ns test)\n123\n")) 1))
  (is (= (count (marginalia.parser/parse "(ns test)\n\"string\"")) 1))
  (is (= (count (marginalia.parser/parse "(ns test)\n\"some string\"")) 1))
  (is (= (count (marginalia.parser/parse "(ns test (:require [marginalia.parser :as parser]))\n(defn foo [] ::parser/foo)")) 1)))


(def simple-fn
  "(defn some-fn
  \"the docstring\"
  [x]
  (* x x))")

(deftest test-parse-fn-docstring
  (let [{:keys [type docstring]} (first (marginalia.parser/parse simple-fn))]
    (is (= :code type))
    (is (= "the docstring" docstring))))
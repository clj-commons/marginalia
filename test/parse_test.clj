(ns parse-test
  "This module does stuff"
  (:require [clojure.string :as str])
  (:use clojure.test)
  (:require marginalia.parser))


;; It seems that, in Clojure, block
;; comments are used to inform about
;; the next section of code, rather than
;; the next function definition only.

(defn hello-world [name]
  "Greets a person by name.
   Does some other cool stuff too."
  (println (str "Hello World, " name "!")))

(deftest test-inline-literals
  (is (= (count (marginalia.parser/parse "(ns test)")) 1))
  ;(is (= (count (marginalia.parser/parse "(ns test)\n123")) 1)) still failing
  (is (= (count (marginalia.parser/parse "(ns test)\n123\n")) 1))
  (is (= (count (marginalia.parser/parse "(ns test)\n::foo\n")) 1))
  (is (= (count (marginalia.parser/parse "(ns test)\n\"string\"")) 1))
  (is (= (count (marginalia.parser/parse "(ns test)\n\"some string\"")) 1)))

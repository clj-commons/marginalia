(ns parse-test
  "This module does stuff"
  (:require [clojure.string :as str]))


;; It seems that, in Clojure, block
;; comments are used to inform about
;; the next section of code, rather than
;; the next function definition only.

(defn hello-world [name]
  "Greets a person by name.
   Does some other cool stuff too."
  (println (str "Hello World, " name "!")))


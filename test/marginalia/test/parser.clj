(ns marginalia.test.parser
  (:use marginalia.parser :reload)
  (:use clojure.test))


(def simple-fn
  "(defn some-fn
  \"the docstring\"
  [x]
  (* x x))")

(deftest test-parse-fn-docstring
  (let [{:keys [type raw docstring]} (first (parse simple-fn))]
    (is (= :code type))
    (is (= simple-fn raw))
    (is (= "the docstring" docstring))))



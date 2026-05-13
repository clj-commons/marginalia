(ns marginalia.html-test
  (:require
   [clojure.test :refer :all]
   [marginalia.html :as html]
   [marginalia.parser :as p]))

(def markdown-docstring-fn
  "(defn some-fn
  \"the docstring

  ```clojure
  (some-fn 1 :abc #{['foo \\\"bar\\\"]})
  ```\"
  [& args]
  (vec args))")

(deftest markdown-docstring-test
  (is (=
       "<p>the docstring</p>\n<pre><code class=\"language-clojure\">(some-fn 1 :abc #{['foo &quot;bar&quot;]})\n</code></pre>\n"
       (html/docs-to-html (:docstring (first (p/parse markdown-docstring-fn)))))))

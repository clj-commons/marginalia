(ns leiningen.marg
  "Run Marginalia against your project source files."
  (:use [leiningen.compile :only [eval-in-project]]
        marginalia.core))

(defn marg [project & args]
  (eval-in-project project
    `(binding [marginalia.html/*resources* ""]
       (marginalia.core/run-marginalia (list ~@args)))
    nil
    nil
    '(require 'marginalia.core)))

(.setMeta #'marg
          (assoc (.meta #'marg)
            :doc (with-out-str (run-marginalia (list "-h")))))

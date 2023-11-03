(ns marginalia.test.helpers
  (:require
   [clojure.java.io :refer [file delete-file]]
   [clojure.test :refer :all]
   [marginalia.core :as core]
   [marginalia.html :as html]))


;; copied from http://clojuredocs.org/clojure_contrib/clojure.contrib.io/delete-file-recursively
;; N.B. that Raynes's filesystem library could possibly replace this,
;; but that's a separate task.
(defn delete-file-recursively
  "Delete file f. If it's a directory, recursively delete all its contents. Raise an exception if any deletion fails unless silently is true."
  [f & [silently]]
  (let [f (file f)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (delete-file f silently)))

(defn find-clojure-file-paths [source-dir]
  (core/find-processable-file-paths source-dir #(re-find #"clj$" %)))

(defn files-in [dir]
  (seq (.listFiles (file dir))))

(defmacro with-project
  "Runs assertions in the context of a project set up for testing in the `test_projects` directory.
   Provides the following variables to the assertion context:

   * `number-of-generated-pages` - result of running the `doc-generator`
      function (which should ultimately call one of the Marginalia's own
      functions.

   * `project-name` - the name of the project
   * `doc-generator` - function which invokes marginalia (actually produces
                       output). Function accepts three arguments: path to source files, path to
                       output files and test project metadata
   * `tests` - assertions to be run after the output has been produced"
  [project-name doc-generator & tests]
  (let [project (file "test_projects" project-name)
        test-project-src (str (file project "src"))
        test-project-target (str (file project "docs"))
        test-metadata {
           :dependencies [["some/dep" "0.0.1"]]
           :description "Test project"
           :name "test"
           :dev-dependencies []
           :version "0.0.1"
         }]

    `(do
       (delete-file-recursively ~test-project-target true)
       (.mkdirs (file ~test-project-target))
       (binding [html/*resources* ""]
         (~doc-generator ~test-project-src ~test-project-target ~test-metadata))
       (let [~'number-of-generated-pages (count (files-in ~test-project-target))]
         ;; We need to `deftest` in order for test runners (e.g. `lein test`) to pick up failures
         (deftest ~(gensym (str project-name "-"))
           ~@tests))
       (delete-file-recursively ~test-project-target true))))

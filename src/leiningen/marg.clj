(ns leiningen.marg
  "# Leiningen plugin for running marginalia against your project.

   ## Usage

   1. Add `[marginalia \"0.2.3\"]` to your project.clj's `:dev-dependencies` section.
   2. run `lein marg` from your project's root directory. "
  (:use [marginalia.core]))

(defn marg [project & args]
  (run-marginalia args))

;; You can pass a file, directory, multiple files and/or directories to marginalia like so:
;;
;;     $ lein marg  # runs marginalia on all the cljs found in your ./src dir.
;;     $ lein marg ./path/to/files  # runs marginalia on all cljs found in ./path/to/files
;;     $ lein marg ./path/to/file.clj  # runs marginalia on ./path/to/file.clj only.
;;     $ lein marg ./path/to/one.clj ./path/to/another.clj
;;     $ lein marg ./path/to/dir ./path/to/some/random.clj
;;
;; This allows you to control the order in which sections appear in the generated
;; documentation.  For example, in marginalia's docs, the leiningen.marg namespace
;; forced to the bottom of the namespace ordering by using this command:
;;
;;     $ lein marg ./src/marginalia ./src/leiningen



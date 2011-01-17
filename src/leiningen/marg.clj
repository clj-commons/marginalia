(ns leiningen.marg
  "# Leiningen plugin for running marginalia against your project.

   ## Usage

   1. Add `[marginalia \"<current version number>\"]` to your project.clj's `:dev-dependencies` section.
   2. run `lein marg` from your project's root directory. "
  (:use [leiningen.compile :only [eval-in-project]]
        marginalia.core))

(defn marg [project & args]
  (eval-in-project project
    `(do
       (require 'marginalia.core)
       (marginalia.core/run-marginalia (list ~@args)))))

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



(ns marginalia.tasks
  "# Cake plugin for running marginalia against your project.

   ## Usage

   1. In your project.clj, add `[marginalia \"<current version number>\"] to your `:dev-dependencies` and `marginalia.tasks` to `:tasks`
   2. Run `cake marg` from within your project directory."
  (:use marginalia.core
        [cake.core :only [deftask]]))

(deftask marg
  "Run marginalia against your project code.
   Optionally, you can pass files or directories to control what documentation is generated and in what order."
  {files :marg}
  (run-marginalia files))

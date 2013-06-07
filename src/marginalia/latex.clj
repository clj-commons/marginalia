(ns marginalia.latex
  "Utilities for converting parse results into LaTeX."
  (:require [clojure.string :as str]
            [clostache.parser :as mustache]
            [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:use [marginalia.html :only [slurp-resource]]))

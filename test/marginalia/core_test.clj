(ns marginalia.core-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [marginalia.core :as marginalia]
   [marginalia.test.helpers :refer [in-project]]))

(set! *warn-on-reflection* true)

(deftest parse-project-file-simple
  (is (= "project-name"
         (:name (marginalia/parse-project-file "test/marginalia/resources/multi-def-project.clj")))))

(deftest config-test
  (let [expected-config {:description             "Overridden description" ; config.edn
                         :marginalia              {:css        nil
                                                   :javascript ["http://example.com/magic.js"] ; project.clj
                                                   :exclude    nil
                                                   :leiningen  nil}
                         :dir                     "./docs"
                         :name                    "configurationpalooza" ; project.clj
                         :file                    "uberdoc.html"
                         :lift-inline-comments    false ; config.edn
                         :multi                   true ; CLI > config.edn
                         :version                 "0.2.1-SNAPSHOT" ; project.clj
                         :dependencies            [['org.clojure/clojure "1.11.1"] ; project.clj
                                                   ['org.markdownj/markdownj-core "0.4"]]
                         :exclude-lifted-comments false}  ; default
        expected-sources [(.getAbsolutePath (io/file "test_projects" "highly_configured" "src" "core.clj"))]]
    (is (= [expected-config expected-sources]
           (in-project "highly_configured"
             (marginalia/resolved-opts+sources ["--multi"] nil))))))

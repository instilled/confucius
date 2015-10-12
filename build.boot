(def artifact
  {:project
   'confucius/confucius

   :version
   "0.0.1-SNAPSHOT"

   :description
   "A library for declarative configuration."

   :url
   "https://github.com/instilled/confucius"})

(set-env!
  :source-paths
  #{"src/main/clojure"}

  :resource-paths
  #{"src/main/clojure"}

  :dependencies
  '[[org.yaml/snakeyaml                   "1.16"]
    [org.clojure/data.json                "0.2.6"]
    [org.clojure/tools.logging            "0.3.1"
     :scope "provided"]
    [org.clojure/clojure                  "1.7.0"
     :scope "provided"]

    ;; test dependencies
    [org.apache.logging.log4j/log4j-core  "2.3"]
    [adzerk/boot-test                     "1.0.4"
     :scope "test"]])

(task-options!
  pom artifact)

(require
  '[adzerk.boot-test :refer :all])

(deftask remove-ignored
  []
  (sift
    :invert true
    :include #{#".*\.swp" #".gitkeep"}))

(deftask dev
  "Profile setup for running tests."
  []
  (merge-env!
    :source-paths
    #{"src/test/clojure"}

    :resource-paths
    #{"src/test/resources"})
  identity)

(deftask test-repeatedly
  []
  (comp
    (dev)
    (watch)
    (speak)
    (test)))

(deftask test-single
  []
  (comp
    (dev)
    (speak)
    (test)))

(deftask build
  []
  ;; Only :resource-paths will be added to the final
  ;; aritfact. Thus we need to merge :source-paths
  ;; into :resources-paths.
  (merge-env!
    :resource-paths
    #{"src/main/cloujure"})
  (comp
    (remove-ignored)
    (pom)
    (jar)
    (install)))

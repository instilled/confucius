(def artifact
  {:project 'confucius/confucius
   :version "0.0.1-SNAPSHOT"
   :description "A library for declarative configuration."
   :url "https://github.com/instilled/confucius"})

(set-env!
  :source-paths
  #{"src/main/clojure"
    "src/main/java"}

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

;; For s3 repo
;; https://github.com/boot-clj/boot/wiki/S3-Repositories


;; Tasks to be moved into a library
(deftask remove-ignored
  []
  (sift
    :invert true
    :include #{#".*\.swp" #".gitkeep"}))

(deftask dev
  "Profile setup for running tests."
  []
  ;; To set an initial namespace on startup
  #_(task-options!
      repl
      {:init-ns 'my.awesome.dev-ns
       :eval '(set! *print-length* 20)})

  (merge-env!
    :source-paths
    #{"src/test/clojure"
      "src/test/java"}

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
    (watch)
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
    (javac)
    (pom)
    (jar)
    (install)))

;; Ubejar example, see:
;; https://github.com/adzerk-oss/boot-uberjar-example
#_(deftask uberbuild
    "Builds an uberjar of this project that can
    be run with java -jar"
    []
    (comp
      (remove-ignored)
      (pom)
      (uber)
      (jar)
      (install)))

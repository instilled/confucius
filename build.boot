(task-options!
  pom {:project
       'confucius/confucius

       :version
       "0.0.3-SNAPSHOT"

       :description
       "A library for declarative configuration."

       :scm
       {:url "https://github.com/instilled/confucius"}

       :url
       "https://github.com/instilled/confucius"}
  jar {:file "confucius.jar"})

(set-env!
  :source-paths
  #{"src/main/clojure"}

  :resource-paths
  #{"src/main/clojure"}

  :target-path
  "target"

  :dependencies
  '[[org.clojure/clojure                  "1.7.0"
     :scope "provided"]
    [org.yaml/snakeyaml                   "1.16"
     :scope "provided"]
    [org.clojure/data.json                "0.2.6"
     :scope "provided"]

    ;; test dependencies
    [org.clojure/tools.cli                "0.3.3"
     :scope "test"]
    [adzerk/boot-test                     "1.1.0"
     :scope "test"]])

(require
  '[adzerk.boot-test :refer :all])

(deftask remove-ignored
  []
  (sift
    :invert true
    :include #{#".*\.swp" #".gitkeep"}))

(deftask dev
  "Pull in test dependencies."
  []
  (merge-env!
    :source-paths
    #{"src/test/clojure"}

    :resource-paths
    #{"src/test/resources"})
  identity)

(replace-task!
  [t test] (fn [& xs] (comp (dev) (apply t xs))))

(deftask test-repeatedly
  "Repeatedly execute tests."
  []
  (comp
    (watch)
    (speak)
    (test)))

(deftask build
  "Build the shizzle."
  []
  ;; Only :resource-paths will be added to the final
  ;; aritfact. Thus we need to merge :source-paths
  ;; into :resources-paths. see https://github.com/boot-clj/boot/wiki/Boot-Environment#env-keys
  (merge-env!
    :resource-paths
    #{"src/main/cloujure"})
  (comp
    (remove-ignored)
    (pom)
    (jar)
    (target)
    (install)))

(deftask deploy
  []
  (push
    :gpg-sign false
    :repo "clojars"))

(task-options!
 pom {:project
      'confucius/confucius

      :version
      "0.0.1-SNAPSHOT"

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
  '[adzerk.boot-test :refer :all]
  '[boot.gpg             :as gpg]
  '[boot.util            :as util])

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

(deftask debugme
  []
  (with-pass-thru [fs]
    (clojure.pprint/pprint (get-env))
    (clojure.pprint/pprint (output-files fs))

    ;;(println "fs: " fs)
    fs))

(deftask sign2
  [k gpg-user-id    KEY  str  "The name or key-id used to select the signing key."
   p gpg-passphrase PASS str  "The passphrase to unlock GPG signing key."]
  (let [tgt (tmp-dir!)]
    (with-pass-thru [fs]
      (empty-dir! tgt)
      (let [[pom & p] (->> (output-files fs)
                           (by-name ["pom.xml"])
                           (map tmp-file))
            jarfiles (->> (output-files fs)
                          (by-ext [".jar"])
                          (map tmp-file))]
        (when-not (and pom (seq jarfiles))
          (throw (Exception. "missing pom or jar file")))
        (let [signed (reduce
                       (fn [acc f]
                         (util/info "Signing %s...\n" (.getName f))
                         (assoc acc (.getName f)
                                (gpg/sign-jar tgt f pom {:gpg-key gpg-user-id
                                                         :gpg-passphrase gpg-passphrase})))
                       {} jarfiles)]
          ;;(set-env! :signed-artifacts signed)
          (-> fs (add-resource tgt) commit!))))))


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
    (sign2)
    (target)
    (install)))

(deftask deploy
  []
  (push
   ;;:gpg-sign true
   ;;:gpg-user-id "BFE605B5"
   :repo "clojars"
   ;;:ensure-branch "master"
   ;;:ensure-clean true
   ;;:ensure-release true
   ;;:ensure-snapshot true
   ;;:ensure-tag ""
   ;;:ensure-version ""
   ))

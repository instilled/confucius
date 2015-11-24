(ns confucius.ext
  (:require
    [confucius.proto   :refer [ToUrl from-url]]
    [confucius.utils   :refer [keywordize-keys]]
    [clojure.data.json :as    json]
    [clojure.java.io   :as    io] )
  (:import
    [org.yaml.snakeyaml
     Yaml]))

(defmethod from-url :default
  [url]
  (throw
    (IllegalStateException.
      (format "Don't know how to load configuration from %s"
              url))))

(letfn [(load-yaml
          [url]
          (let [y (Yaml.)]
            (with-open [s (.openStream url)]
              ;; convert to clojure persistent
              ;; datatructs
              (keywordize-keys (.load y s)))))]
  (defmethod from-url ".yaml"
    [url] (load-yaml url))

  (defmethod from-url ".yml"
    [url] (load-yaml url)))

;; JSON
(defmethod from-url ".json"
  [url]
  (with-open [s (.openStream url)
              r (io/reader s)]
    (json/read r :key-fn keyword)))

;; ToUrl extend types
(extend-type
  java.io.File

  ToUrl
  (toUrl
    [this]
    (.toURL this)))

(extend-type
  java.lang.String

  ToUrl
  (toUrl
    [this]
    (if (.matches this ".+:\\/\\/.+")
      (java.net.URL. this)
      (.toUrl (io/file this)))))

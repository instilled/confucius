(ns ^{:author "Fabio Bernasconi"
      :doc "Tools for working with configuration maps."}
  confucius.impl
  (:refer-clojure   :exclude [load])
  (:require
    [confucius.proto :refer [ConfigSource from-url] :as p]
    [confucius.utils :refer [keywordize-keys]]
    [clojure.edn     :as    edn]
    [clojure.java.io :as    io]))

(extend-type clojure.lang.IPersistentMap
  ConfigSource
  (load [this] this))

(extend-type java.net.URL
  ConfigSource
  (load [this]
    (-> this
        (from-url))))

(extend-type java.io.File
  ConfigSource
  (load [this]
    (-> this
        (.toURI)
        (.toURL)
        (from-url))))

(extend-type java.lang.String
  ConfigSource
  (load [this]
    (from-url
      (if (.matches this ".+:\\/\\/.+")
        (java.net.URL. this)
        (-> this
            (io/file)
            (.toURI)
            (.toURL)
            #_(p/load))))))

;; -------------
;; from-url default impls

(defmethod from-url :default
  [url]
  (throw
    (IllegalStateException.
      (format "Don't know how to load configuration from %s"
        url))))

(defmethod from-url ".edn"
  [url]
  (with-open [s (.openStream url)
              r (io/reader s)]
    ;; TODO: ensure keys are keywords? -> keywordize-keys
    (edn/read r)))

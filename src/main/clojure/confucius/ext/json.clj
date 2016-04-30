(ns ^{:author "Fabio Bernasconi"
      :doc "Tools for working with configuration maps."}
  confucius.source.json
  (:require
    [confucius.proto   :refer [from-url]]
    [clojure.data.json :as    json]
    [clojure.java.io   :as    io]))

(defmethod from-url ".json"
  [url]
  (with-open [s (.openStream url)
              r (io/reader s)]
    (json/read r :key-fn keyword)))

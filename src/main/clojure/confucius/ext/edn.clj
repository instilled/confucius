(ns ^{:author "Fabio Bernasconi"
      :doc "Tools for working with configuration maps."}
  confucius.source.edn
  (:require
    [confucius.proto   :refer [from-url]]
    [clojure.java.io   :as    io]))

(defmethod from-url ".edn"
  [url]
  (with-open [s (.openStream url)
              r (io/reader s)]
    (read-string (slurp r))))

(ns ^{:author "Fabio Bernasconi"
      :doc "Tools for working with configuration maps."}
  confucius.proto
  (:refer-clojure :exclude [load]))

(defprotocol ValueReader
  (process [this opts ctx v] "Return the processed value or nil if valuereader does not apply."))

(defprotocol ConfigSource
  (load [this]))

(defmulti from-url
  "Load data from url. Dispatch on the extension
   of url."
  (fn [^java.net.URL url]
    (if-let [^String f (str url)]
      (let [i (.lastIndexOf f ".")]
        (if (< 0 i)
          (.substring f i)))
      :default)))

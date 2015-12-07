(ns confucius.proto)

(defprotocol ValueReader
  (process [this opts ctx v] "Return the processed value or nil if valuereader does not apply."))

(defprotocol ToUrl
  (toUrl [this]))

(defmulti from-url
  "Load data from url. Dispatch on the extension
  of url."
  (fn [url]
    (if-let [^String f (str url)]
      (let [i (.lastIndexOf f ".")]
        (if (< 0 i)
          (.substring f i)))
      :default)))

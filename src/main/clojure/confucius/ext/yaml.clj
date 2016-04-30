(ns ^{:author "Fabio Bernasconi"
      :doc "Tools for working with configuration maps."}
  confucius.ext.yaml
  (:require
    [confucius.proto :refer [from-url]]
    [confucius.utils :refer [keywordize-keys]])
  (:import
    [org.yaml.snakeyaml
     Yaml]))

(defn ^:private load-yaml
  [url]
  (let [y (Yaml.)]
    (with-open [s (.openStream url)]
      ;; convert to clojure persistent
      ;; datatructs
      (keywordize-keys (.load y s)))))

(defmethod from-url ".yaml"
  [url] (load-yaml url))

(defmethod from-url ".yml"
  [url] (load-yaml url))

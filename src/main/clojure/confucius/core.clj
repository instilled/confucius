(ns confucius.core
  (:require
    [confucius.env     :refer [envify]]
    [confucius.utils   :refer [keywordize-keys deep-merge]]
    [clojure.walk      :refer [walk postwalk]]
    [clojure.data.json :as    json]
    [clojure.java.io   :as    io])
  (:import
    [org.yaml.snakeyaml
     Yaml]))

(defn ^:private ext
  "Get file extension of `f`, i.e. text after the last `.` in `f`, or nil."
  [f]
  (if-let [f (str f)]
    (let [i (.lastIndexOf f ".")]
      (if (< 0 i)
        (.substring f i)))))

(defmulti from-url
  "Load data from url. Dispatches on the extension
  of url."
  (fn [url] (or (ext url) :default)))

(defmethod from-url :default
  [url]
  (throw
    (IllegalStateException.
      (format "Don't know how to load configuration from %s"
              url))))

(defmethod from-url ".yml"
  [url] "abc")

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

(defn ^:private url-or-throw
  [ctx url]
  (if (instance? java.net.URL url)
    url
    (throw
      (IllegalStateException.
        (format "Not a valid url. Context:" ctx)))))

(defn unprefix
  "Unprefix `prefix` from `value`. Return `nil` if
  `value` did not start with `prefix`."
  [^String value prefix]
  (when (and (string? value)
             (.startsWith value prefix))
    (.substring value (count prefix))))

(defn ^:private cp-ref
  [value]
  (when-let [value (unprefix value "cp://")]
    (if-let [url (io/resource value)]
      url
      (throw
        (IllegalStateException.
          (str "Resource not found: " value))))))

(defn ^:private file-ref
  "Convert file ref to url. Knows how to handle
  relative file:// urls, i.e. file://abc.yml will
  be taken relative to the current working dir."
  [value]
  (when-let [path (io/file (unprefix value "file://"))]
    (if-let [url (and (.isFile path) (-> path (.toURI) (.toURL)))]
      url
      (throw
        (IllegalStateException.
          (str "File not found: " value))))))

(declare load-config)

(defn ^:private include
  "Extract path from ref value."
  [value]
  (when-let [value (unprefix value "@:")]
    (-> value
        (url-or-throw
          (or (cp-ref value)
              (file-ref value)))
        (load-config))))

(defn ^:private expand-env
  ([v]
   (expand-env v v))
  ([ctx v]
   (cond
     (map? v)
     (reduce (fn [acc [k v]] (assoc acc k (expand-env ctx v))) v v)

     (coll? v)
     (into (empty v) (map (partial expand-env ctx) v))

     (string? v)
     (envify ctx v)

     :else
     v)))

(defn ^:private process-map
  [m]
  (letfn [(dive
            [v]
            (when (map? v)
              (process-map v)))]
    (reduce
      (fn [acc [k v]]
        (assoc
          acc k
          (or (include v)
              (cp-ref v)
              (file-ref v)
              (dive v)
              v)))
      {}
      m)))

(defn load-config
  "Recursively load a configuration from url. Merge
  from left to right."
  [& urls]
  (->> urls
       (transduce
         (comp
           (map from-url)
           (map expand-env)
           (map process-map))
         conj
         [])
       (apply deep-merge)))

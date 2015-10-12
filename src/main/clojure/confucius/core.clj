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

(defn ^:private unprefix
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
  "Load configuration data from `urls-and-opts`. Does deep-merging
  of the data from left to right to form the final configuration
  map. Reads`*.yml|yaml` or `*.json` encoded content.
  The last value in `urls-and-opts` may be a map with further
  options:

  :transform-fn    One arity fn taking the configuration to
  be transformed. Must return the modified
  configuration.

  `load-config` has support for:
  * variable expansion and deault values `${my-var:default}`
  * referencing other configuration with `@:`
  * referencing files on the classpath with `cp://...`
  * relative file urls for `file://...`


  A note on syntax and behaviour

  Variable will be expanded to values either from other config
  values, java properties, or the native environment. If it could
  not be expanded its default value is taken. In case no default
  value was given an IllegalStateException is thrown.
  To reference a path in the confguration the variable
  is split at `.` and each segment keywordized, e.g. `${a.b.c}`
  will result in `(get-in cfg [:a :b :c])`. For native env
  lookups `.` is replaced with underscore and the final string
  uppercased.

  Referencing configuration includes the target at point, i.e.
  the file content will be inserted under the given key.
  Referencing is done by prefixing a value with `@:`,
  e.g. \"@:cp://abc\".

  Relative file urls will be made absolute by creating a file
  and replacing its absolute path in the url, e.g.
  `file://rel/path` -> `file:///tmp/rel/path`.


  Example

  Given three configuration files:

  ```
  # file on classpath: happy-service.yml
  http-port: 8080

  # file on fs (where java is invoked): crary-service.yml
  http-port: 8081

  # file on fs (where java is invoked): config.yml
  base-path: \"file://${expanded-from-env:target}\"

  happy-service: \"@:cp://happy-service.yml\"
  crazy-service: \"@:file://crazy-service.yml\"
  ```

  Loading `config.yml` will result in the following map:

  ```
  {:base-path \"file:///projects/confucius/target\"
   :happy-service {:http-port 8080}
   :crazy-service {:http-port 8081}}
  ```

  where `${expanded-from-env}` will be expanded
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

(defprotocol ToUrl
  (toUrl
    [this]))

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
      (toUrl (io/file this)))))

(defn ->url
  "Convenience function to build urls. Currently supports string
  and file coercion.

  If `v` is a string and is an url pattern, e.g `file:///abc`,
  uses `(java.net.URL. v)` otherwise `(.toURL (java.io.File. v))`."
  [v]
  (toUrl v))


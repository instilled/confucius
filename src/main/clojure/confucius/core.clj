(ns confucius.core
  (:require
    [confucius.proto   :as    p]
    [confucius.ext]
    [confucius.env     :refer [envify]]
    [confucius.utils   :refer [deep-merge unprefix]]
    [clojure.walk      :refer [walk postwalk]]
    [clojure.java.io   :as    io]))

(declare load-config)

(def classpath-value-reader
  "Classpath value reader."
  (reify p/ValueReader
    (process
      [this ctx value]
      (when-let [value (unprefix value "cp://")]
        (if-let [url (io/resource value)]
          url
          (throw
            (IllegalStateException.
              (str "Resource not found: " value))))))))

(def fileref-value-reader
  "File ref value reader."
  (reify p/ValueReader
    (process
      [this ctx value]
      (when-let [path (io/file (unprefix value "file://"))]
        (if-let [url (and (.isFile path) (-> path (.toURI) (.toURL)))]
          url
          (throw
            (IllegalStateException.
              (str "File not found: " value))))))))

(def include-value-reader
  "Includes either file or classpath refs."
  (reify p/ValueReader
    (process
      [this ctx value]
      (when-let [value (unprefix value "@:")]
        (let [url (or (p/process classpath-value-reader ctx value)
                      (p/process fileref-value-reader ctx value))]
          (assert (instance? java.net.URL url)
                  (str "Not a valid url: " url))
          (load-config ctx url))))))

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
  [{:keys [value-readers] :as opts} cfg]
  (letfn [(first-wins
            [value-readers opts v]
            (loop [value-readers value-readers]
              (when-let [rdr (first value-readers)]
                (or (p/process rdr opts v)
                    (recur (next value-readers))))))]
    (reduce
      (fn [cfg [k v]]
        (assoc
          cfg
          k
          (or (first-wins value-readers opts v)
              (and (map? v) (process-map opts v))
              v)))
      cfg
      cfg)))

(def ^:dynamic *default-value-readers*
  "Default value readers."
  [include-value-reader
   classpath-value-reader
   fileref-value-reader])

(defn ->url
  "Convenience function to build urls. By default supports string
  and file coercion. Protocol `confucius.proto/ToUrl` may be
  extended to add more support."
  [v]
  (p/toUrl v))

(defn load-config
  "Load configuration data from `opts-and-urls`. Does deep-merging
  of the data from left to right to form the final configuration
  map. Reads`*.yml|yaml` or `*.json` encoded content.
  The first value in `opts-and-urls` may be a map with further
  options:

  :transform-fn   One arity fn taking the configuration to
                  be transformed. Must return the modified
                  configuration.
  :value-readers  By default uses `include-value-reader`,
                  `classpath-value-reader` and
                  `fileref-value-reader`. See
                  `*default-value-readers*`.

  `load-config` has support for:
  * variable expansion with deault values `${my-var:default}`
  * extendable value-reader support, by default:
    * including other configuration with `@:`
    * referencing files on the classpath with `cp://...`
    * or referincing files `file://...`


  A note on syntax and behaviour

  Variables will be expanded to values either from other config
  values, java properties, or the native environment. If it can
  not be expanded its default value is taken. In case no default
  value was given an IllegalStateException is thrown.
  To reference a path in the confguration the variable
  is split at `.` and each segment keywordized, e.g. `${a.b.c}`
  will result in `(get-in cfg [:a :b :c])`. For native env
  lookups `.` is replaced with underscore and the final string
  uppercased.

  Referencing configuration with `@:` includes the target at point,
  i.e. the file contents will be inserted at the given key.

  Relative file urls will be made absolute by replacing its absolute
  path in the url, e.g. `file://rel/path` -> `file:///tmp/rel/path`.


  Example

  Given three configuration files:

  ```
  # file on classpath: happy-service.yml
  http-port: 8080

  # file on fs (where java is invoked): crazy-service.yml
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
  from the environment. Use default value if it
  not found or throws when none was provided."
  [& opts-and-urls]
  (let [[opts urls] (if (map? (first opts-and-urls))
                      [(first opts-and-urls)
                       (rest opts-and-urls)]
                      [nil opts-and-urls])
        opts (merge
               {:value-readers *default-value-readers*
                :transform-fn identity}
               opts)]
    (->> urls
         (reduce
           (fn [cfg url]
             (deep-merge
               cfg
               (->> url
                    (p/from-url)
                    (expand-env)
                    (process-map opts))))
           {})
    ((:transform-fn opts)))))

(ns ^{:author "Fabio Bernasconi"
      :doc "Tools for working with configuration maps."}
  confucius.core
  (:require
    [confucius.proto   :as    p]
    [confucius.impl]
    [confucius.utils   :refer [deep-merge unprefix postwalkx]]
    [clojure.java.io   :as    io]))

(defn ^:private parse-var
  "Extract var from `s`."
  [s]
  (into
    []
    (.split
      (-> s
          (.substring 0 (- (.length s) 1))
          (.substring 2))
      ":"
      2)))

(defn ^:private extract-vars
  [s]
  (re-seq #"\$\{.*\}" s))

(declare envify)

(defn ^:private from-env
  "Recursively expand `env`."
  [[m :as ctx] env & [default]]
  (letfn [(->ctx [^String s]
            (when-let [p (seq (map keyword (.split s "\\.")))]
              (get-in m p)))

          (->jvmn [^String s]
            (System/getProperty s))

          (->envn [^String s]
            (-> s
                (.replace "." "_")
                (.toUpperCase)
                (System/getenv)))]
    (let [v (or
              (->ctx env)
              (->jvmn env)
              (->envn env)
              default
              (throw
                (IllegalStateException.
                  (str "Reference not found in environment and no default provided! Var: " env))))]
      (if (extract-vars v) (envify ctx v) v))))

(defn envify
  "Replace any occurrence of `${var.name:default}` with the value
   referenced by `var.name`. First try to find the value
   in `ctx`, then java properties and finally in the native
   environment. For `ctx` matching interprets `var.name` as
   a path in a map, i.e. `{:var {:name xyz}}`.

   For native environment replace `.` with `_` and uppercase
   the string, e.g. `var.name` -> `VAR_NAME`."
  [ctx s]
  (reduce
    (fn [s r]
      (let [v (apply from-env ctx (parse-var r))]
        (if v
          (.replace s r v)
          s)))
    s
    (extract-vars s)))

(defn ^:private process
  ([opts [m path :as ctx] v]
   (letfn [(first-wins
             [{:keys [value-readers] :as opts} ctx v]
             (loop [value-readers value-readers]
               (when-let [rdr (first value-readers)]
                 (or (p/process rdr opts ctx v)
                     (recur (next value-readers))))))]
     (cond
       (map? v)
       (into {} (map (fn [[k v*]] [k (process opts [m (conj path k)] v*)]) v))

       (coll? v)
       (do (into (empty v) (map #(process opts ctx %) v)))

       (string? v)
       (let [v (envify ctx v)] (or (first-wins opts ctx v) v))

       :else
       v))))

(def classpath-value-reader
  "Classpath value reader."
  (reify p/ValueReader
    (process [this opts [m path :as ctx] value]
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
      [this opts [m path :as ctx] value]
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
      [this opts [m path :as ctx] value]
      (when-let [value (unprefix value "@:")]
        (let [url (or (p/process classpath-value-reader opts ctx value)
                      (p/process fileref-value-reader opts ctx value))]
          (assert (instance? java.net.URL url)
            (str "Not a valid url: " url))
          (let [m* (p/load url)]
            (process
              opts
              [(update-in m path deep-merge (or m* {})) path]
              m*)))))))

(def ^:dynamic *default-value-readers*
  "Default value readers."
  [include-value-reader
   classpath-value-reader
   fileref-value-reader])

(defn load-config
  "Load configuration data from `sources`. Does deep-merging
   of the data from left to right to form the final configuration
   map. Supports different data sources such as clojure datastructures
   (maps, seqs, vecs), urls or files (currently supports `edn`, `yaml`
   or `json` encoded content), or attempts to coerce strings to urls.

   `opts` may be a map with the keys:

   :postprocess-fn One arity fn taking the configuration to
                  be post processed. Must return the modified
                  configuration. This may be used e.g. to
                  schema validate the final configuration
                  map.
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
   values, java properties, or the native environment. If expansion
   fails the default value is taken. In case no default
   value was given an IllegalStateException is thrown.
   To reference a path in the confguration the variable
   is split at `.` and each segment keywordized, e.g. `${a.b.c}`
   will result in `(get-in cfg [:a :b :c])`. For native env
   lookups `.` is replaced with underscore and the final string
   uppercased. Takes the value as is for lookups in the Java System
   Property.

   Referencing configuration with `@:` includes the target at point,
   i.e. the file contents will be inserted at the given key.

   Relative file urls will be made absolute by replacing the relative
   part with an absolute path, e.g.
   `file://rel/path` or `file://./rel/path` -> `file:///tmp/rel/path`.


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
  ([sources]
   (load-config nil sources))
  ([opts sources]
   (let [{:keys [postprocess-fn] :as opts}
         (merge
           {:value-readers *default-value-readers*
            :postprocess-fn identity}
           opts)

         m
         (reduce
           (fn [m source] (deep-merge m (p/load source)))
           {}
           sources)]
     (-> (process opts [m []] m)
         (postprocess-fn)))))

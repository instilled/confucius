(ns confucius.utils
  (:import
    [java.util.LinkedHashMap]))

(defn walkx
  "Custom `clojure.walk/walk`. We also turn java.util.Collection
  classes, e.g. LinkedHashMap, List, into clojure data structures."
  [inner outer form]
  (cond
    (list? form) (outer (apply list (map inner form)))
    (instance? clojure.lang.IMapEntry form) (outer (vec (map inner form)))
    (seq? form) (outer (doall (map inner form)))
    (instance? clojure.lang.IRecord form) (outer (reduce (fn [r x] (conj r (inner x))) form form))
    (instance? java.util.LinkedHashMap form) (outer (into {} (map inner (into {} form))))
    (instance? java.util.List form) (outer (into '() (map inner (into '() form))))
    (coll? form) (outer (into (empty form) (map inner form)))
    :else (outer form)))

(defn postwalkx
  "Copied from `clojure.walk/postwalk`."
  [f form]
  (walkx (partial postwalkx f) f form))

(defn keywordize-keys
  "Recursively transforms all map keys from strings to keywords."
  [m]
  (let [f (fn [[k v]] (if (string? k) [(keyword k) v] [k v]))]
    (postwalkx (fn [x] (if (instance? java.util.Map x) (into {} (map f x)) x)) m)))

(defn deep-merge
  "Recursively merges maps. If keys are not maps,
  the last value wins.

  Shamelessly copied from: https://groups.google.com/forum/#!topic/clojure/UdFLYjLvNRs"
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

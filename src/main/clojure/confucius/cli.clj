(ns confucius.cli)

(defn parse-override
  [^String v]
  (let [parts (.split v ":")]
    (when (< (count parts) 2)
      (throw (IllegalStateException. "Expecting parts as K:V but was: " v)))
    (let [val (second parts)
          parts (->> (-> parts first (.split "\\."))
                     (into [])
                     (map keyword))]
      (assoc-in nil parts val))))

(defn ^:private collector
  [m k v]
  (update-in m [k] #(conj (if (vector? %1) %1 []) %2) v))

(def cli-opts
  [["-c" "--config FILE" "A list of config files"
    :id ::config
    :parse-fn identity
    :assoc-fn collector]
   ["-o" "--opt K:V" "A comma separated list of overrides."
    :id ::override
    :parse-fn parse-override
    :assoc-fn collector]])

(defn from-parse-opts
  [{:keys [options]}]
  (let [fs (::config options)
        ov (::override options)]
    (vec (concat [] fs ov))))

(comment
  ;; use as follows
  (require '[confucius.core    :as c])
  (require '[confucius.cli     :as cli])
  (require '[clojure.tools.cli :refer [parse-opts]])

  (c/load-config
   ["/tmp/host.edn"])

  ;; or with parse opts
  (defn -main [& args]
    (let [opts (parse-opts (conj my-opts cli/cli-opts))]
      (c/load-config
       (concat
        ["/tmp/host.edn"]
        (cli/from-parse-opts opts))))))

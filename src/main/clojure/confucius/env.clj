(ns confucius.env)

(defn ^:private parse-var
  [s]
  (into
    []
    (.split
      (-> s
          (.substring 0 (- (.length s) 1))
          (.substring 2))
      ":"
      2)))

(defn ^:private get-vars
  [s]
  (re-seq #"\$\{.*\}" s))

(declare envify)

(defn ^:private from-env
  [ctx env & [default]]
  (letfn [(->ctx  [^String s]
            (when-let [p (seq (map keyword (.split s "\\.")))]
              (get-in ctx p)))

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
              (->envn env)
              default
              (throw (IllegalStateException. (str "Failed to find in environment and no default value provided: " env))))]
      (if (get-vars v) (envify ctx v) v))))

(defn envify
  "Replaces any occurrences of `${var.name:default}` with the value
  referenced by `var.name`. First tries to find the value
  in `ctx`, then java properties and finally in the native
  environment.

  For navtive environment var matching replaces `.` with
  `_` and uppercases the string, e.g. `var.name` -> `VAR_NAME`."
  [ctx s]
  (reduce
    (fn [s r]
      (let [v (apply from-env ctx (parse-var r))]
        (if v
          (.replace s r v)
          ;; TODO: better to throw if not found?
          s)))
    s
    (get-vars s)))

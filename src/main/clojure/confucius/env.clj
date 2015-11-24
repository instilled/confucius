(ns confucius.env)

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

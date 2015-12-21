(ns confucius.cli-test
  (:require
   [confucius.cli     :refer :all]
   [confucius.core    :as    c]
   [confucius.proto   :as    p]
   [confucius.ext.all]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.java.io   :as    io]
   [clojure.test      :refer :all]))

(deftest test-cli-parsing
  (testing "config option"
    (let [{:keys [options] :as res} (parse-opts
                                     ["-c" "src/test/resources/test1.yml"
                                      "--config" "src/test/resources/test2.yml"
                                      "-o" "a.b.c1:v1"
                                      "--opt" "a.b.c2:v2"]
                                     cli-opts)]
      (is (= ["src/test/resources/test1.yml"
              "src/test/resources/test2.yml"
              {:a {:b {:c1 "v1"}}}
              {:a {:b {:c2 "v2"}}}]
             (from-parse-opts res))))))

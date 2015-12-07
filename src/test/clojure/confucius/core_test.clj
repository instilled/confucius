(ns confucius.core-test
  (:require
    [confucius.core  :as    c
                     :refer :all]
    [confucius.proto :as    p]
    [clojure.java.io :as    io]
    [clojure.test    :refer :all]))

(deftest test-from-url
  (testing "yaml"
    (let [r (p/from-url (io/resource "test1.yml"))]
      (is (instance?
            clojure.lang.PersistentArrayMap
            (get r :abc)))
      (is (= {:abc {:on-classpath "@:cp://test2.yml"
                    :on-fs-rel "@:file://src/test/resources/test2.yml"}}
             r))))
  (testing "json"
    (is (= {:abc {:on-classpath "@:cp://test2.yml"
                  :on-fs-rel "@:file://src/test/resources/test2.yml"}}
           (p/from-url (io/resource "test1.json"))))))

(deftest test-load-config
  (testing "include"
    (let [expected {:abc
                    {:on-fs-rel {:abc 1}
                     :on-classpath {:abc 1}}}]
      ;; classpath
      (is (= expected
             (p/process
               include-value-reader
               {:value-readers *default-value-readers*}
               [{}]
               "@:cp://test1.yml")))
      ;; file
      (is (= expected
             (p/process
               include-value-reader
               {:value-readers *default-value-readers*}
               [{}]
               "@:file://src/test/resources/test1.yml")))))

  (testing "classpath handling"
    (let [cfg (load-config (io/resource "test1.yml"))]
      (is (= {:abc
              {:on-classpath
               {:abc 1}

               :on-fs-rel
               {:abc 1}}}
             cfg)))))

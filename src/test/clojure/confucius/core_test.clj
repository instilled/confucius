(ns confucius.core-test
  (:require
    [confucius.core  :as    c
                     :refer :all]
    [clojure.java.io :as    io]
    [clojure.test    :refer :all]))

(deftest test-from-url
  (testing "ext"
    (is (= ".yml"
           (#'c/ext (io/resource "test1.yml")))))
  (testing "yaml"
    (let [r (from-url (io/resource "test1.yml"))]
      (is (instance?
            clojure.lang.PersistentArrayMap
            (get r :abc)))
      (is (= {:abc {:on-classpath "@:cp://test2.yml"
                    :on-fs-rel "@:file://src/test/resources/test2.yml"}}
             r))))
  (testing "json"
    (is (= {:abc {:on-classpath "@:cp://test2.yml"
                  :on-fs-rel "@:file://src/test/resources/test2.yml"}}
           (from-url (io/resource "test1.json"))))))

(deftest test-load-config
  (testing "include"
    (let [expected {:abc
                    {:on-fs-rel {:abc 1}
                     :on-classpath {:abc 1}}}]
      ;; classpath
      (is (= expected
             (#'c/include "@:cp://test1.yml")))
      ;; file
      (is (= expected
             (#'c/include "@:file://src/test/resources/test1.yml")))))

  (testing "classpath handling"
    (let [cfg (load-config (io/resource "test1.yml"))]
      (is (= {:abc
              {:on-classpath
               {:abc 1}

               :on-fs-rel
               {:abc 1}}}
             cfg))))

  (testing "defaults"
    #_(is (= {}
             (load-config
               (io/resource "test1.yml"))))))

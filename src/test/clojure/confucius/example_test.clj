(ns confucius.example-test
  (:require
    [confucius.core  :as    c]
    [clojure.java.io :as    io]
    [clojure.test    :refer :all]))

(deftest test-example
  (testing "only base"
    (is (= {:global
            {:install-dir "/usr/local/services"}
            :service1
            {:http
             {:bind-address "0.0.0.0"
              :port "80"}
             :log-dir "/usr/local/services/log"}
            :service2
            {:http
             {:bind-address "0.0.0.0"
              :port "80"}
             :log-dir "/usr/local/services/log"}}
           (c/load-config
             (io/resource "example/base.yml")))))

  (testing "base with host"
    (is (= {:global
            {:install-dir "/usr/local/services"}
            :service1
            {:http
             {:bind-address "0.0.0.0"
              :port "8080"}
             :log-dir "/usr/local/services/log"}
            :service2
            {:http
             {:bind-address "0.0.0.0"
              :port "8081"}
             :log-dir "/usr/local/services/log"}}
           (c/load-config
             (io/resource "example/base.yml")
             (io/resource "example/host.yml"))))))

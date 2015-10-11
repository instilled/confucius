(ns confucius.env-test
  (:require
    [confucius.env :as    e
                   :refer :all]
    [clojure.test  :refer :all]))

(deftest test-envify
  (testing "precedence"
    (System/setProperty "path" "from properties")
    (is (= "from context"
           (envify
             {:path "from context"}
             "${path}")))
    (is (= "from properties"
           (envify
             {}
             "${path}")))
    (System/clearProperty "path")
    (let [n (envify {} "${path}")]
      (is (< 1 (count n)))
      (is (not (or (= "from properties" n)
                   (= "from context" n))))))

  (testing "recursive lookups"
    (is (= "root -> node -> leaf"
           (envify
             {:root "root"
              :node "${root} -> node"}
             "${node} -> leaf"))))

  (testing "not found cases"
    (is (= "defaulted"
           (envify
             {}
             "${abc:defaulted}")))
    (is (thrown-with-msg?
          IllegalStateException
          #"Reference not found.*"
          (envify
            {}
            "${abc}")))))

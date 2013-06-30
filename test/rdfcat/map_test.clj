(ns rdfcat.map-test
  (:require [clojure.test :refer :all]
            [rdfcat.util.map :refer :all]))

(deftest test-map-vals
  (is (= (map-vals {:a "yo" :b "zapp" :c "bang"} str "!")
         {:a "yo!" :b "zapp!" :c "bang!"})))

(deftest test-merge-reduce
  (let [maps '({:a 1 :b #{} :c [2] :d #{"a"} :e {:z \z}}
               {:a 1 :b [1 2 3] :c 99 :d "aa" :e 3}
               {:a 11})]
    (is (= (merge-reduce maps)
           {:a #{1 11}
            :b #{[1 2 3]}
            :c #{[2] 99}
            :d #{"a" "aa"}
            :e #{{:z \z} 3}}))))
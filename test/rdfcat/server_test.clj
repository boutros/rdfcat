(ns rdfcat.server-test
  (:require [clojure.test :refer :all]
            [rdfcat.server :refer :all]
            [ring.mock.request :refer :all]))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (get (:headers response ) "Content-Type") "text/html; charset=utf-8"))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
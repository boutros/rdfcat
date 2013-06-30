(ns rdfcat.sparql.response
  "Functions to fetch, parse and transform the SPARQL query responses adhering
  to the JSON format spec at http://www.w3.org/TR/rdf-sparql-json-res/"
  (:require [clojure.walk :refer [keywordize-keys]]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]))


;; Config

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))


;; Fetching the results

(defn fetch
  "Performs SPARQL request by sending the query to the SPARQL endpoint via HTTP.
  Expects query as a string or query-fn defined by defquery. 
  Returns the parsed JSON with all keys transfomed to symbols."
  [q]
  (->> (client/get
         (config :endpoint)
         (merge (config :http-options)
                {:query-params {"query" (if (string? q) q (q))
                                "format" "application/sparql-results+json"}}))
       :body
       parse-string
       keywordize-keys))


;; Functions for extracting and transforming selected parts of the response:

(defn bindings
  "Returns the bindings of a sparql/json response in a map with the binding
  parameters as keys and results as sets:

  {:binding1 #{value1, value2} :binding2 #{v1, v2}}"
  [response]
  (let [{{vars :vars} :head {solutions :bindings} :results}
        response vars (map keyword vars)]
    (into {}
          (for [v vars]
            [v (set (keep #(->> % v :value) solutions))]))))

(defn solutions
  "Returns the solution maps from a sparql/json response."
  [response]
  (for [solution (->> response :results :bindings)]
    (into {}
          (for [[k v] solution]
            [k (:value v)]))))
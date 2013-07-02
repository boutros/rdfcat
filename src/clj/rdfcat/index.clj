(ns rdfcat.index
  (:require [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer [parse-string]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [rdfcat.sparql.query :as query]
            [rdfcat.sparql.response :refer :all]
            [taoensso.timbre :as timbre :refer [info error]])
  (:import java.net.URI))

;; Setup

(def mapping
  (->> (slurp (clojure.java.io/resource "mapping.json"))
       parse-string
       keywordize-keys))


(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] (:index-log config))

; Indexing

(defn get-works [offset limit]
  (->> (fetch (query/all-works offset limit))
       bindings
       :work))

(defn index! [work]
  (esd/put "rdfcat" "work" (:_id work) work))

(defn index-all! []
  (info "Creating index & mapping")
  (esr/connect! "http://127.0.0.1:9200")
  (esi/delete "rdfcat")
  (esi/create "rdfcat"
              :settings (:settings mapping)
              :mappings (:mappings mapping))
  (info "Done creating index rdfcat/work")
  (info "Start indexing batch of 9000 works")
  (doseq [work (get-works 0 9000)]
    (let [res (->> work URI. query/work fetch)]
      (if (->> res :results :bindings empty?)
        (info (str "Insuficient information for work: " work))
        (index! (populate-work res)))))
  (info "Done indexing batch of 9000"))
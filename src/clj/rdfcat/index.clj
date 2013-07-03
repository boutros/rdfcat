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
(timbre/set-config! [:timestamp-pattern] "yyyy-MM-dd'T'HH:mm:ssZ")

; Indexing

(defn get-works [offset limit]
  (->> (fetch (query/all-works offset limit))
       bindings
       :work))

(defn index! [work]
  (esd/put "rdfcat" "work" (:id work) work))

(defn setup-index []
  (info "Creating index & mapping")
  (esr/connect! "http://127.0.0.1:9200")
  (esi/delete "rdfcat")
  (esi/create "rdfcat"
              :settings (:settings mapping)
              :mappings (:mappings mapping)))

(defn index-all! [offset limit]
  (esr/connect! "http://127.0.0.1:9200")
  (info "Start indexing batch" offset "-" (+ offset limit ))
  (doseq [work (get-works offset limit)]
    (try
      (let [res (->> work URI. query/work fetch)]
        (if (->> res :results :bindings empty?)
          (info "Insuficient information for work:" work)
          (index! (populate-work res))))
      (catch Exception e (error "Error indexing work:" work "because" (.getMessage e)))))
  (info "Done indexing batch" offset "-" (+ offset limit)))
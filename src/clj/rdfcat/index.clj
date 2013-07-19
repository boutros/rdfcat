(ns rdfcat.index
  (:require [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer [parse-string]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.bulk :as bulk]
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

(defn update-index! [work]
  (esd/update-with-script "rdfcat" "work" (:id work) "ctx._source.creator += creator;" work))

(defn setup-index []
  (info "Creating index & mapping")
  (esr/connect! "http://127.0.0.1:9200")
  (esi/delete "rdfcat")
  (esi/create "rdfcat"
              :settings (:settings mapping)
              :mappings (:mappings mapping)))

(defn index-works [offset limit]
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

(defn update-works [offset limit]
  (esr/connect! "http://127.0.0.1:9200")
  (info "Start updating works batch" offset "-" (+ offset limit ))
  (doseq [work (get-works offset limit)]
    (try
      (let [res (->> work URI. query/work-update fetch)]
        (if (->> res :results :bindings empty?)
          (info "Nothing to update for work:" work)
          (update-index! (updates res))))
      (catch Exception e (error "Error indexing work:" work "because" (.getMessage e)))))
  (info "Done updating batch" offset "-" (+ offset limit)))

(defn index-all! []
  (doseq [i (range 0 320000 10000)]
    (index-works i 10000)))

(defn retry-failed []
  (esr/connect! "http://127.0.0.1:9200")
  ;TODO increase read timeout to 3000 for retries
  (info "Retrying indexing works from file 'failed.txt'")
  (let [works (clojure.string/split (slurp "failed.txt") #"\n")
        n (count works)
        i (atom 0)]
    (doseq [work works]
      (try
        (let [res (->> work URI. query/work fetch)]
          (if (->> res :results :bindings empty?)
            (info "Insuficient information for work:" work)
            (do
              (index! (populate-work res))
              (swap! i inc))))
        (catch Exception e (error "Error indexing work:" work "because" (.getMessage e)))))
    (info "Done retrying. Sucessfully indexed" @i "of" n )))

(defn index-subjects! []
  (let [subjects (->> query/subjects fetch bindings :subject)
        litgenres (->> query/litgenres fetch bindings :litgenre)
        musgenres (->> query/musgenres fetch bindings :musgenre)
        subjects-merged (merge-subjects subjects litgenres musgenres)
        subjects-bulk (bulk/bulk-index (map #(assoc {} :label %) subjects-merged))]
    (bulk/bulk-with-index-and-type "rdfcat" "subject" subjects-bulk)))
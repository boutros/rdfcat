(ns rdfcat.index
  (require [clojure.walk :refer [keywordize-keys]]
           [cheshire.core :refer [parse-string]]
           [clojurewerkz.elastisch.rest :as esr]
           [clojurewerkz.elastisch.rest.index :as esi]
           [clojurewerkz.elastisch.rest.document :as esd]))

;; Setup

(def mapping
  (->> (slurp (clojure.java.io/resource "mapping.json"))
       parse-string
       keywordize-keys))

;; Indexing

(defn index! [work]
  (esd/put "rdfcat" "work" (:_id work) work))

(defn index-all! []
  (esr/connect! "http://127.0.0.1:9200")
  (esi/delete "rdfcat")
  (esi/create "rdfcat"
              :settings (:settings mapping)
              :mappings (:mappings mapping))
  )
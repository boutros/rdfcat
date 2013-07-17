(ns rdfcat.p1
  (:require [net.cgrand.enlive-html :as html :refer [deftemplate defsnippet]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [rdfcat.util.string :refer [length-num]]))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

(deftemplate anne-lena "p1.html" []
  [:h2] (html/content "Utforsk Deichmanske biblioteks katalog"))

(defn pre-queries
  [s n]
  [{} {:query {:multi_match
               {:query s
                :fields ["work.title" "edition.title" "edition.subtitle"]}}
       :size n}
   {} {:query {:multi_match
               {:query s
                :fields ["work.creator.name" "edition.creator.name"]}}
       :size n}
   {} {:query {:match {:subject s}} :size n}])


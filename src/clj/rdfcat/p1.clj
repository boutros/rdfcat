(ns rdfcat.p1
  (:require [net.cgrand.enlive-html :as html :refer [deftemplate defsnippet]]
            [clojurewerkz.elastisch.rest.multi :as multi]
            [rdfcat.util.string :refer [length-num]]
            [rdfcat.p2 :refer [pp-creators]]))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

(deftemplate anne-lena "p1.html" []
  [:h2] (html/content "Utforsk Deichmanske biblioteks katalog"))

(defsnippet results "p1-pre-results.html" [:div#search-results]
  [name-res title-res subject-res]

  [:td.p1-show-all.p1-author]
  (let [name-total (->> name-res :hits :total int)]
    (when (> name-total 0)
      (html/content {:tag :a :attrs {:class "p1-show-all p1-author"} :content (str "Vis alle " name-total " treff ▸")})))

  [:tbody.p1-result.p1-author]
  (let [name-hits (->> name-res :hits :hits)]
    (html/clone-for [{res :_source} name-hits]
                    [:td.p1-author] (html/content (pp-creators (res :creator)))
                    [:td.p1-title] (html/content (res :title))
                    [:td.p1-subtitle] (html/content "-"))))

(defn pre-queries
  [s n]
  [{} {:query {:multi_match
               {:query s
                :fields ["work.creator.name" "edition.creator.name"]}}
       :size n}
   {} {:query {:multi_match
               {:query s
                :fields ["work.title" "edition.title" "edition.subtitle"]}}
       :size n}
   {} {:query {:match {:subject s}} :size n}])

(defn search [term]
  (multi/search-with-index-and-type "rdfcat" "work" (pre-queries term (config :p1-pre-results-per-concept))))
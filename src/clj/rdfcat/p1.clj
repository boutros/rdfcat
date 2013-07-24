(ns rdfcat.p1
  (:require [net.cgrand.enlive-html :as html :refer [deftemplate defsnippet]]
            [clojurewerkz.elastisch.rest.multi :as multi]
            [rdfcat.util.string :refer [length-num]]
            [rdfcat.p2 :refer [pp-creators]]))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

(defn if-match
  "Returns those in coll which maches term"
  [coll term]
  (filter
    #(re-seq (re-pattern (str "(?i)" (clojure.string/trim term))) %)
    (filter string? coll)))

(deftemplate anne-lena "p1.html" []
  [:h2] (html/content "Utforsk Deichmanske biblioteks katalog"))

(defsnippet results "p1-pre-results.html" [:div#search-results]
  [name-res title-res subject-res term]

  [:td.p1-show-all.p1-author]
  (let [name-total (->> name-res :hits :total int)]
    (if (> name-total 0)
      (html/content {:tag :a :attrs {:class "p1-show-all p1-author"} :content (str "Vis alle " name-total " treff ▸")})
      (html/content "ingen treff")))

  [:tbody.p1-result.p1-author]
  (let [name-hits (->> name-res :hits :hits)]
    (html/clone-for [{res :_source} name-hits]
                    [:td.p1-author] (html/content (res :name))))

  [:td.p1-show-all.p1-title]
  (let [title-total (->> title-res :hits :total int)]
    (if (> title-total 0)
      (html/content {:tag :a :attrs {:class "p1-show-all p1-title"} :content (str "Vis alle " title-total " treff ▸")})
      (html/content "ingen treff")))

  [:tbody.p1-result.p1-title]
  (let [title-hits (->> title-res :hits :hits)]
    (html/clone-for [{res :_source} title-hits]
                    [:td.p1-author] (html/content (pp-creators (res :creator)))
                    [:td.p1-title] (html/content (res :title))
                    [:td.p1-subtitle] (html/content (str (first (if-match (map :subtitle (res :edition)) term))))))

  [:td.p1-show-all.p1-subject]
  (let [subject-total (->> subject-res :hits :total int)]
    (if (> subject-total 0)
      (html/content {:tag :a :attrs {:class "p1-show-all p1-title"} :content (str "Vis alle " subject-total " treff ▸")})
      (html/content "ingen treff")))

  [:tbody.p1-result.p1-subject]
  (let [subject-hits (->> subject-res :hits :hits)]
    (html/clone-for [{res :_source} subject-hits]
                    [:td.p1-subject] (html/content (res :label))))
  )

(defn pre-queries
  [s n]
  [{:type "creator"} {:query {:match {"name" {:query s :operator "and"}}} :size n}
   {:type "work"} {:query {:multi_match
               {:query s
                :fields ["work.title" "edition.title" "edition.subtitle"]}}
       :size n}
   {:type "subject"} {:query {:match {"label" {:query s :operator "and"}}} :size n}])

(defn search [term]
  (multi/search-with-index "rdfcat" (pre-queries term (config :p1-pre-results-per-concept))))
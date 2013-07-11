(ns rdfcat.server
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [net.cgrand.enlive-html :as html :refer [deftemplate defsnippet]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [rdfcat.util.string :refer [length-num]])
  (:gen-class))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

(defonce formats
  (read-string (slurp (clojure.java.io/resource "formats.edn"))))

(defn edn-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn html-response [string & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (apply str string)})

(deftemplate anne-lena "p1.html" []
  [:h2] (html/content "Utforsk Deichmanske biblioteks katalog"))

(deftemplate petter "p2.html" [] )


(defsnippet p2-results "p2-results.html" [:div#search-results]
  [results page limit filters]
  [:caption] (html/content (str (->> results :hits :total) " treff (" (->> results :took) "ms)"))
  [:td.p2-pagination] (if (> page 1)
                        (html/prepend {:tag :a :attrs {:class "p2-prev"} :content "forrige"})
                        identity)
  [:td.p2-pagination] (if (and (> (->> results :hits :total) limit)
                               (< page (/ (->> results :hits :total) limit)))
                        (html/append {:tag :a :attrs {:class "p2-next"} :content "neste"})
                        identity)
  [:a.p2-pagenum] (let [max-pages (->> (/ (->> results :hits :total float) limit) Math/ceil int)
                        upto (if (< max-pages 12) max-pages 7)
                        pages (->> (inc max-pages) (range 1) (take upto) (into []))]
                    (html/clone-for [p (if (> max-pages upto)
                                         (if (and (> page upto) (not= max-pages page))
                                           (conj pages "..." page "..." max-pages)
                                           (conj pages "..." max-pages))
                                         pages)]
                                    (if (= p page)
                                      (html/substitute {:tag :span :attrs {:id "p2-curpage"} :content (str p)})
                                      (if (= "..." p)
                                        (html/substitute {:tag :span :content "..."})
                                        (html/content (str p))))))
  [:tbody.p2-work]
  (html/clone-for [work (->> results :hits :hits)]
                  [:td.p2-author :strong] (html/content (or (->> work :_source :creator first :name) "(div)"))
                  [:td.p2-title :em] (html/content (->> work :_source :title))
                  [:tr.p2-subjects] (if (config :p2-show-subjects)
                                      (html/remove-class "hidden")
                                      identity)
                  [:td.p2-subjects :span] (let [all-subjects (->> work :_source :subject)
                                                n (length-num all-subjects 50)
                                                visible-subjects (take n all-subjects)]
                                            (html/clone-for [subject visible-subjects]
                                                            (html/content subject)))
                  [:a.p2-show-sub] (let [all-subjects (->> work :_source :subject)
                                         n (length-num all-subjects 50)
                                         rest-subjects (drop n all-subjects)]
                                     (if (empty? rest-subjects)
                                       (html/add-class "hidden")
                                       (html/after {:tag :div :attrs {:class "p2-rest-subjects hidden"}
                                                    :content (map #(assoc {:tag :span :attrs {:class "p2-subject"}} :content %) rest-subjects)})))
                  [:tr.p2-edition]
                  (html/clone-for
                    [edition (#(if (config :p2-editions-reverse-sort-order)
                                 (reverse %1)
                                 (identity %1))
                                (->> work :_source :edition (sort-by :year)))]
                    [:td.format :img] (html/clone-for [f (edition :format)]
                                                      (html/set-attr :src (get-in formats [f :image] "?")))
                    [:td.title] (html/content (edition :title))
                    [:td.year] (html/content (str (edition :year)))
                    [:td.lang] (html/content (clojure.string/join ", " (remove #(= "Norsk" %) (edition :language)))))
                  [:td.p2-show-all :a] (html/content (str "Vis alle " (->> work :_source :edition count) " utgavene"))
                  [:tr.p2-show-editions] (when (> (->> work :_source :edition count)
                                                  (config :p2-show-num-editions)) (html/add-class "visible")))
  [[:tr.p2-edition (html/nth-child -1 (+ 2 (config :p2-show-num-editions)))]] (html/add-class "visible")
  [:div.p2-format] (html/clone-for [f (->> results :facets :formats :terms)]
                                   [:label]
                                   (html/content
                                     (str (get-in formats [(f :term) :label] "XXX") " (" (f :count) ")"))
                                   [:input] (html/do->
                                              (html/set-attr :data-original (f :term))
                                              (if (or (false? filters) (some #{(f :term)} (filters :format)))
                                                identity
                                                (html/remove-attr :checked))))
  [:div.p2-lang] (html/clone-for [l (->> results :facets :languages :terms)]
                                 [:label] (html/content
                                            (str (l :term) " (" (l :count) ")"))
                                 [:input] (html/do->
                                              (html/set-attr :data-original (l :term))
                                              (if (or (false? filters) (some #{(l :term)} (filters :lang)))
                                                identity
                                                (html/remove-attr :checked))))
  [:#p2-filter-year-from] (html/set-attr :value (let [n (->> results :facets :years :min)
                                                      year-from (get filters :year-from false)]
                                                  (if year-from
                                                    year-from
                                                    (if (number? n)
                                                      (->> n int str)
                                                      "~"))))
  [:#p2-filter-year-to] (html/set-attr :value (let [n (->> results :facets :years :max)
                                                    year-to (get filters :year-to false)]
                                                (if year-to
                                                  year-to
                                                  (if (number? n)
                                                    (->> n int str)
                                                    "~")))))

;; queries

(defn only-who [who]
  {:must {:match {"name" who}}})

(defn only-what [what]
  {:must {:multi_match {:query what
                        :fields ["work.title" "edition.title" "work.subject"]}}})

(defn who-and-what [who what]
  {:must [{:match {"name" who}}
          {:multi_match
           {:query what :fields ["work.title" "edition.title" "work.subject"]}}]})

(defn p2-search [who what offset limit]
  (let [hvem (if (empty? who) nil who)
        hva (if (empty? what) nil what)
        query (cond
                (every? string? [hvem hva]) (who-and-what hvem hva)
                (string? hvem) (only-who hvem)
                (string? hva) (only-what hva))]
    (when (or hvem hva)
      (esd/search "rdfcat" "work" :from offset :size limit :query {:bool query}
                  :facets {:formats {:terms {:field "edition.format" :size 30}}
                           :languages {:terms {:field "edition.language" :size 30}}
                           :years {:statistical {:field "edition.year"}}}))))

(defn p2-search-filtered [who what offset limit filters]
  (let [hvem (if (empty? who) nil who)
        hva (if (empty? what) nil what)
        query (cond
                (every? string? [hvem hva]) (who-and-what hvem hva)
                (string? hvem) (only-who hvem)
                (string? hva) (only-what hva))]
    (when (or hvem hva)
      (esd/search "rdfcat" "work" :from offset :size limit :query {:bool query}
                  :facets {:formats {:terms {:field "edition.format" :size 30}}
                           :languages {:terms {:field "edition.language" :size 30}}
                           :years {:statistical {:field "edition.year"}}}
                  :filter {:and {:filters
                                 [{:terms {:format (remove nil? (filters :format)) :execution "bool"}}
                                  {:or {:filters [{:terms {:language (remove nil? (filters :lang)) :execution "bool"}}
                                   {:missing {:field "language" :existence true :null_value true}}]}}
                                  {:or {:filters [{:range {:year {:from (filters :year-from) :to (filters :year-to)
                                                  :include_lower true :include_upper true}}}
                                                  {:missing {:field "year" :existence true :null_value true}}]}}
                                  ]}}))))

(defroutes app-routes
  (GET "/" [] (redirect "p2"))
  (GET "/p1" [] (anne-lena))
  (GET "/p2" [] (petter))
  (GET "/search/p1" [term] "OK") ;use multi search  /_msearh
  (POST "/search/p2" [who what page]
        (let [limit (config :p2-results-per-page)
              offset (* (dec page) limit)]
          (html-response
            (html/emit*
              (p2-results
                (p2-search who what offset limit)
                page
                limit
                false)))))
  (POST "/search/p2filter" [who what page filters]
        (let [limit (config :p2-results-per-page)
              offset (* (dec page) limit)]
          (html-response
            (html/emit*
              (p2-results
                (p2-search-filtered who what offset limit filters)
                page
                limit
                filters)))))
  (route/resources "/")
  (route/not-found "Not Found."))

(esr/connect! "http://127.0.0.1:9200")

(def app
  (handler/site (wrap-edn-params app-routes)))

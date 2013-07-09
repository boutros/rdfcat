(ns rdfcat.server
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [net.cgrand.enlive-html :as html :refer [deftemplate defsnippet]]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd])
  (:gen-class))

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))

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

(def icon-mapping
  {"http://data.deichman.no/format/Book" "img/book.png"
   "http://data.deichman.no/format/Music" "img/music.png"
   "http://data.deichman.no/format/Compact_Disc" "img/CD.png"
   "http://data.deichman.no/format/Compact_Cassette" "img/cassette.png"
   "http://data.deichman.no/format/DVD" "img/video.png"
   "http://data.deichman.no/format/Videotape" "img/video.png"
   "http://data.deichman.no/format/Blu-ray_Disk" "img/video.png"
   "http://data.deichman.no/format/Sheet_music" "img/clef.png"
   "http://data.deichman.no/format/Periodical_literature" "img/book.png"
   "http://data.deichman.no/format/Audiobook" "img/audiobook.png"})

(defsnippet p2-results "p2-results.html" [:table.p2-results]
  [results page limit]
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
                  [:span.p2-subjects :span] (html/clone-for [subject (->> work :_source :subject)]
                                                            (html/content subject))
                  [:tr.p2-edition]
                  (html/clone-for
                    [edition (#(if (config :p2-editions-reverse-sort-order)
                                 (reverse %1)
                                 (identity %1))
                                (->> work :_source :edition (sort-by :year)))]
                    [:td.format :img] (html/clone-for [f (edition :format)]
                                                      (html/set-attr :src (get icon-mapping f "?")))
                    [:td.title] (html/content (edition :title))
                    [:td.year] (html/content (str (edition :year)))
                    [:td.lang] (html/content (clojure.string/join ", " (remove #(= "Norsk" %) (edition :language)))))
                  [:td.p2-show-all :a] (html/content (str "Vis alle " (->> work :_source :edition count) " utgavene"))
                  [:tr.p2-show-editions] (when (> (->> work :_source :edition count)
                                                  (config :p2-show-num-editions)) (html/add-class "visible")))
  [[:tr.p2-edition (html/nth-child -1 (inc (config :p2-show-num-editions)))]]
  (html/add-class "visible"))

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
    (esd/search "rdfcat" "work" :from offset :size limit :query {:bool query})))

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
                limit)))))
  (route/resources "/")
  (route/not-found "Not Found."))

(esr/connect! "http://127.0.0.1:9200")

(def app
  (handler/site (wrap-edn-params app-routes)))

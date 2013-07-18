(ns rdfcat.server
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [net.cgrand.enlive-html :as html]
            [clojurewerkz.elastisch.rest :as esr]
            [rdfcat.p2 :as p2]
            [rdfcat.p1 :as p1])
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


(defroutes app-routes
  (GET "/" [] (redirect "p1"))
  (GET "/p1" [] (p1/anne-lena))
  (GET "/p2" [] (p2/petter))
  (POST "/search/p1" [term] (let [res (p1/search term)
                                  name-res (first res)
                                  title-res (second res)
                                  subjects-res (last res)]
                              (html-response
                                (html/emit*
                                  (p1/results name-res title-res subjects-res term)))))
  (POST "/search/p2" [who what page]
        (let [limit (config :p2-results-per-page)
              offset (* (dec page) limit)]
          (html-response
            (html/emit*
              (p2/results
                (p2/search who what offset limit)
                page
                limit
                false)))))
  (POST "/search/p2filter" [who what page filters]
        (let [limit (config :p2-results-per-page)
              offset (* (dec page) limit)
              res (p2/search-filtered who what offset limit filters)]
          (html-response
            (html/emit*
              (p2/results
                res
                page
                limit
                filters)))))
  (route/resources "/")
  (route/not-found "Not Found."))

(esr/connect! "http://127.0.0.1:9200")

(def app
  (handler/site (wrap-edn-params app-routes)))

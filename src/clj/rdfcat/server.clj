(ns rdfcat.server
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [net.cgrand.enlive-html :as html :refer [deftemplate]])
  (:gen-class))

(deftemplate anne-lena "p1.html" []
  [:h2] (html/content "Utforsk Deichmanske biblioteks katalog"))

(deftemplate petter "p2.html" [] )

(defroutes app-routes
  (GET "/" [] (redirect "p1"))
  (GET "/p1" [] (anne-lena))
  (GET "/p2" [] (petter))
  (route/resources "/")
  (route/not-found "Not Found."))

(def app
  (handler/site app-routes))

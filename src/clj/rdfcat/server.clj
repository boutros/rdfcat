(ns rdfcat.server
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [net.cgrand.enlive-html :as html :refer [deftemplate]])
  (:gen-class))

(deftemplate anne-lena "p1.html" [overskrift]
  [:h2] (html/content overskrift))

(deftemplate petter "p2.html" [overskrift]
  [:h2] (html/content overskrift))

(defroutes app-routes
  (GET "/" [] (redirect "p1"))
  (GET "/p1" [] (anne-lena "Prototype \"Anne-Lena\""))
  (GET "/p2" [] (petter "Prototype \"Petter\""))
  (route/resources "/")
  (route/not-found "Not Found."))

(def app
  (handler/site app-routes))

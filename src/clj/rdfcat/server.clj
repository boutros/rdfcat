(ns rdfcat.server
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html :refer [deftemplate]])
  (:gen-class))

(deftemplate home "index.html" [overskrift]
  [:h1] (html/content overskrift))

(defroutes app-routes
  (GET "/" [] (home "Vælkomn."))
  (route/resources "/")
  (route/not-found "Not Found."))

(def app
  (handler/site app-routes))

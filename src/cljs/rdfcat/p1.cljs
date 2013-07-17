(ns rdfcat.p1
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [domina :as dom :refer [by-id by-class log]]
            [domina.css :as css]
            [domina.events :as event]
            [goog.net.XhrIo :as xhr])
  (:import goog.async.Throttle))

(defn ajax-call
  [path callback method data]
  (xhr/send path
            callback
            method
            (pr-str data)
            (clj->js {"Content-Type" "application/edn"})))

(defn search [evt]
  (do
    (log "searching..")
    "OK"))

(defn ^:export init []
  (log "Hallo der, mister Åsen!")
  (let [throttled-search (Throttle. (fn [evt] (search evt)) 300)]
    (event/listen! (by-class "big-search") :input #(.fire throttled-search %) )))
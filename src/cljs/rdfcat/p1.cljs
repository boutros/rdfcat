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

(defn search-handler [evt]
  (let [response (.-target evt)
        result (.getResponseText response)]
    (dom/swap-content! (by-id "search-results") result)))

(defn search [evt]
  (let [term (dom/value (by-id "pre-search"))]
    (if (or (empty? term) (< (count term) 2))
      (dom/destroy-children! (by-id "search-results"))
      (ajax-call "/search/p1" search-handler "POST" {:term term}))))

(defn ^:export init []
  (log "Hallo der, mister Åsen!")
  (let [throttled-search (Throttle. (fn [evt] (search evt)) 250)]
    (event/listen! (by-class "big-search") :input #(.fire throttled-search %) )))
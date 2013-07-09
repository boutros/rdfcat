(ns rdfcat.p2
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [domina :as dom :refer [by-id by-class log]]
            [domina.events :as event]
            [goog.net.XhrIo :as xhr]))

(defn ajax-call
  [path callback method data]
  (xhr/send path
            callback
            method
            (pr-str data)
            (clj->js {"Content-Type" "application/edn"})))

(defn search-handler [event]
  (let [response (.-target event)
        result (.getResponseText response)]
    (set! (.-innerHTML (by-id "search-results")) result)))

(defn search-error-handler [{:keys [status status-text]}]
  (log (str "something bad happened: " status " " status-text)))

(defn search [evt]
  (let [who (dom/value (by-id "search-who"))
        what (dom/value (by-id "search-what"))]
    (when-not (every? empty? [who what])
      (ajax-call "/search/p2" search-handler "POST" {:who who :what what}))))

(defn ^:export init []
  (log "Hallo der, mister Åsen.")
  (event/listen! (by-class "medium-search") :keyup search))
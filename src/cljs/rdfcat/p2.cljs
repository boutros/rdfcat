(ns rdfcat.p2
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [domina :as dom :refer [by-id by-class log]]
            [domina.css :as css]
            [domina.events :as event]
            [goog.net.XhrIo :as xhr]))

(defn ajax-call
  [path callback method data]
  (xhr/send path
            callback
            method
            (pr-str data)
            (clj->js {"Content-Type" "application/edn"})))

(defn show-editions [evt]
  (let [t (event/target evt)
        p (->> t .-parentElement .-parentElement .-parentElement)]
    (dom/add-class! (css/sel p ".p2-edition") "visible")
    (dom/destroy! (->> t .-parentElement .-parentElement))))

(declare search)

(defn search-handler [event]
  (let [response (.-target event)
        result (.getResponseText response)]
    (dom/swap-content! (by-id "search-results") result)
    (event/listen! (by-class "p2-show-ed") :click show-editions)
    (event/listen! (by-class "p2-pagenum")
                   :click
                   (fn [evt]
                     (let [p (->> (event/target evt) (dom/text) int)]
                       (search evt p))))
    (event/listen! (by-class "p2-next")
                   :click
                   (fn [evt]
                     (let [p (->> (by-id "p2-curpage") (dom/text) int inc)]
                       (search evt p))))
    (event/listen! (by-class "p2-prev")
                   :click
                   (fn [evt]
                     (let [p (->> (by-id "p2-curpage") (dom/text) int dec)]
                       (search evt p))))))

(defn search-error-handler [{:keys [status status-text]}]
  (log (str "something bad happened: " status " " status-text)))

(defn search [evt page]
  (let [who (dom/value (by-id "search-who"))
        what (dom/value (by-id "search-what"))]
    (when-not (every? empty? [who what])
      (ajax-call "/search/p2" search-handler "POST" {:who who :what what :page page}))))


(defn ^:export init []
  (log "Hallo der, mister Åsen.")
  (event/listen! (by-class "medium-search") :keyup #(search % 1)))
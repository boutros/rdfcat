(ns rdfcat.p2
  (:require [clojure.browser.repl :as repl]
            [cljs.reader :as reader]
            [domina :as dom :refer [by-id by-class log]]
            [domina.css :as css]
            [domina.events :as event]
            [goog.net.XhrIo :as xhr]))

;(repl/connect "http://localhost:9000/repl")

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

(defn show-subjects [evt]
  (let [t (event/target evt)
        p (->> t .-parentElement)]
    (log "viser emner")
    (dom/remove-class! (css/sel p ".p2-rest-subjects") "hidden")
    (dom/destroy! t)))

(declare search filter-search)

(defn search-handler [event]
  (let [response (.-target event)
        result (.getResponseText response)]
    (dom/swap-content! (by-id "search-results") result)
    (event/listen! (by-class "p2-show-ed") :click show-editions)
    (event/listen! (by-class "p2-pagenum")
                   :click
                   (fn [evt]
                     (let [p (->> (event/target evt) (dom/text) int)]
                       (filter-search evt p))))
    (event/listen! (by-class "p2-next")
                   :click
                   (fn [evt]
                     (let [p (->> (by-id "p2-curpage") (dom/text) int inc)]
                       (filter-search evt p))))
    (event/listen! (by-class "p2-prev")
                   :click
                   (fn [evt]
                     (let [p (->> (by-id "p2-curpage") (dom/text) int dec)]
                       (filter-search evt p))))
    (event/listen! (by-class "p2-show-sub") :click show-subjects)
    (event/listen! (by-class "p2-facet") :click filter-search)
    (event/listen! (by-class "input-number") :keyup
                   (fn [evt] (let [year-from (dom/value (by-id "p2-filter-year-from"))
                                   year-to (dom/value (by-id "p2-filter-year-to"))]
                               (when (every? #(= 4 (count %)) [year-from year-to]) (filter-search evt)))))
    (event/listen! (by-class "p2-select-lang")
                   :click
                   (fn [evt]
                     (do
                       (dom/remove-attr! (css/sel ".p2-facet.lang") :checked)
                       (dom/set-attr! (css/sel (->> (event/target evt) .-parentElement) ".p2-facet.lang") :checked "checked")
                       (filter-search evt))))
    (event/listen! (by-class "p2-select-format")
                   :click
                   (fn [evt]
                     (do
                       (dom/remove-attr! (css/sel ".p2-facet.format") :checked)
                       (dom/set-attr! (css/sel (->> (event/target evt) .-parentElement) ".p2-facet.format") :checked "checked")
                       (filter-search evt))))
    ))

(defn search-error-handler [{:keys [status status-text]}]
  (log (str "something bad happened: " status " " status-text)))

(defn search [evt page]
  (let [who (dom/value (by-id "search-who"))
        what (dom/value (by-id "search-what"))]
    (if (or (every? empty? [who what]) (every? #(< (count %) 2) [who what]))
      (dom/destroy-children! (by-id "search-results"))
      (ajax-call "/search/p2" search-handler "POST" {:who who :what what :page page}))))

(defn filter-search
  ([evt] (filter-search evt 1))
  ([evt page]
   (let [filter-lang (map #(dom/attr % :data-original) (dom/nodes (css/sel ".p2-facet.lang:checked")))
         filter-format (map #(dom/attr % :data-original) (dom/nodes (css/sel ".p2-facet.format:checked")))
         year-from (dom/value (by-id "p2-filter-year-from"))
         year-to (dom/value (by-id "p2-filter-year-to"))
         who (dom/value (by-id "search-who"))
         what (dom/value (by-id "search-what"))
         ;lang-missing (if ())
         ]
     (if (or (every? empty? [who what]) (every? #(< (count %) 2) [who what]))
       (dom/destroy-children! (by-id "search-results"))
       (ajax-call "/search/p2filter" search-handler "POST"
                  {:who who :what what :page page
                   :filters {:lang (vec filter-lang) :format (vec filter-format)
                             :year-from year-from :year-to year-to}})))))

(defn ^:export init []
  (log "Hallo der, mister Åsen.")
  (event/listen! (by-class "medium-search") :input #(search % 1)))
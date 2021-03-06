(ns rdfcat.sparql.response
  "Functions to fetch, parse and transform the SPARQL query responses adhering
  to the JSON format spec at http://www.w3.org/TR/rdf-sparql-json-res/"
  (:require [clojure.walk :refer [keywordize-keys]]
            [clojure.string :as str]
            [clojure.set :refer [rename-keys]]
            [clj-http.client :as client]
            [cheshire.core :refer [parse-string]]
            [rdfcat.util.map :refer [merge-reduce]]
            [rdfcat.util.string :refer [date-clean]]))


;; Config

(defonce config
  (read-string (slurp (clojure.java.io/resource "config.edn"))))


;; Fetching the results

(defn fetch
  "Performs SPARQL request by sending the query to the SPARQL endpoint via HTTP.
  Expects query as a string or query-fn defined by defquery.
  Returns the parsed JSON with all keys transfomed to symbols."
  [q]
  (->> (client/get
         (config :endpoint)
         (merge (config :http-options)
                {:query-params {"query" (if (string? q) q (q))
                                "format" "application/sparql-results+json"}}))
       :body
       parse-string
       keywordize-keys))


;; Functions for extracting and transforming selected parts of the response:

(defn bindings
  "Returns the bindings of a sparql/json response in a map with the binding
  parameters as keys and results as sets:

  {:binding1 #{value1, value2} :binding2 #{v1, v2}}"
  [response]
  (let [{{vars :vars} :head {solutions :bindings} :results}
        response vars (map keyword vars)]
    (into {}
          (for [v vars]
            [v (set (keep #(->> % v :value) solutions))]))))

(defn solutions
  "Returns the solution maps from a sparql/json response."
  [response]
  (for [solution (->> response :results :bindings)]
    (into {}
          (for [[k v] solution]
            [k (:value v)]))))

(defn extract
  "Extract selected variables from solutions. Return a set of maps."
  [vars solutions]
  (set (remove empty? (map #(select-keys % vars) solutions))))

(defn extract-ensure-all
  "Extract selected variables from solutions. Discard solutions which do not
  contain all variables. Returns a seq of maps."
  [vars solutions]
  (->> solutions
       (extract vars)
       (remove #(< (count %) (count vars)))))

(defn extract-where
  "Extract selected variables from solutions where k equals v. Return a map with
  values reduced into sets."
  [k v vars solutions]
  (merge-reduce
    (set
      (remove empty?
              (map #(select-keys % vars)
                   (filter #(= (k %) v) solutions))))))


;; Generating the giant work-map

(def translation-map
  {:creator :id :creatorname :name :contributor :id :contributorname :name
   :edition :id :editiontitle :title :editionyear :year
   :editionlang :language :editionformat :format
   :editionsubjectlabel :label
   :editionsubtitle :subtitle :director :id :directorname :name
   :editor :id :editorname :name})

(defn- select-edition-creator [ed creators]
  (for [c creators :when (= (:edition c) ed)]
    (select-keys c [:id :name :role])))

(defn populate-work [res]
  "Extract the work properties to be indexed. Expects a response from fetch.
  Returns a work map ready to be sent to elasticsearch."
  (let [bindings (bindings res)
        solutions (solutions res)
        editions (:edition bindings)
        directors (->> (extract-ensure-all
                   [:edition :editiondirector :editiondirectorname] solutions)
                      (map #(assoc % :role "director"))
                      (map #(rename-keys % {:editiondirectorname :name
                                            :editiondirector :id})))
        actors (->> (extract-ensure-all
                   [:edition :actor :actorname] solutions)
                      (map #(assoc % :role "actor"))
                      (map #(rename-keys % {:actorname :name
                                            :actor :id})))
        creators (->> (extract-ensure-all
                   [:edition :editioncreator :editioncreatorname] solutions)
                      (map #(assoc % :role "creator"))
                      (map #(rename-keys % {:editioncreatorname :name
                                            :editioncreator :id})))
        editors (->> (extract-ensure-all
                       [:edition :editioneditor :editioneditorname] solutions)
                     (map #(assoc % :role "editor"))
                     (map #(rename-keys % {:editioneditorname :name
                                           :editioneditor :id})))
        illustrators (->> (extract-ensure-all
                       [:edition :editionillustrator :editionillustratorname] solutions)
                          (map #(assoc % :role "illustrator"))
                          (map #(rename-keys % {:editionillustratorname :name
                                                :editionillustrator :id})))
        translators (->> (extract-ensure-all
                           [:edition :editiontranslator :editiontranslatorname] solutions)
                         (map #(assoc % :role "translator"))
                         (map #(rename-keys % {:editiontranslatorname :name
                                               :editiontranslator :id})))
        contributors (->> (extract-ensure-all
                           [:edition :editioncontributor :editioncontributorname] solutions)
                         (remove #(= (:editioncontributorname %) "zzz"))
                         (map #(assoc % :role "contributor"))
                         (map #(rename-keys % {:editioncontributorname :name
                                               :editioncontributor :id})))]
    {:title (->> bindings :title first)
     :id (->> bindings :id first)
     :creator (vec (clojure.set/union (->> (extract [:creator :creatorname] solutions)
                                           (map #(assoc % :role "creator"))
                                           (map #(rename-keys % translation-map)))
                                      (->> (extract [:contributor :contributorname] solutions)
                                           (map #(assoc % :role "contributor"))
                                           (map #(rename-keys % translation-map)))
                                      (->> (extract [:editor :editorname] solutions)
                                           (map #(assoc % :role "editor"))
                                           (map #(rename-keys % translation-map)))
                                      (->> (extract [:director :directorname] solutions)
                                           (map #(assoc % :role "director"))
                                           (map #(rename-keys % translation-map)))))
     :subject (-> (->> bindings
                       :editionsubjectlabel
                       (into (bindings :editionmusicgenrelabel))
                       (into (bindings :editiongenrelabel))
                       (map #(str/split % #"\s-\s"))
                       flatten
                       set)
                  (disj "Norvegica" "genre" "GRUPPE-SKJØNNSAL" "GRUPPE-1"
                        "GRUPPE-10" "GRUPPE-11" "GRUPPE-12" "GRUPPE-13"
                        "GRUPPE-14" "GRUPPE-15" "GRUPPE-16" "GRUPPE-17"
                        "GRUPPE-18" "GRUPPE-19" "GRUPPE-20" "GRUPPE-21"
                        "GRUPPE-2a" "GRUPPE-2b" "GRUPPE-3" "GRUPPE-4"
                        "GRUPPE-5" "GRUPPE-6" "GRUPPE-7" "GRUPPE-8" "GRUPPE-9"
                        "GRUPPE-DATA" "GRUPPE-MILJØ" "GRUPPE-OSLO-HYLLA"
                        "GRUPPE-SKJØNNSAL" "GRUPPE-SLEKT" "GRUPPE-SPRÅK")
                  vec)
     :edition (->> (for [e editions]
                     (extract-where :edition e
                                    [:edition :editionlang :editionformat :editionyear
                                     :editiontitle :editionsubtitle]
                                    solutions))
                   (map #(rename-keys % translation-map))
                   (map #(update-in % [:id] first))
                   (map #(update-in % [:title] first))
                   (map #(update-in % [:subtitle] first))
                   (map #(update-in % [:year] date-clean))
                   (map #(assoc % :creator []))
                   (map #(update-in %1 [:creator] into
                                    (select-edition-creator (:id %1) translators)))
                   (map #(update-in %1 [:creator] into
                                    (select-edition-creator (:id %1) creators)))
                   (map #(update-in %1 [:creator] into
                                    (select-edition-creator (:id %1) illustrators)))
                   (map #(update-in %1 [:creator] into
                                    (select-edition-creator (:id %1) directors)))
                   (map #(update-in %1 [:creator] into
                                    (select-edition-creator (:id %1) contributors)))
                   (map #(update-in %1 [:creator] into
                                    (select-edition-creator (:id %1) actors)))
                   (map #(update-in %1 [:creator] into
                                    (select-edition-creator (:id %1) editors)))
                   vec)}))

(defn updates
  "Extract work.editor + work.director to update es index if they exists"
  [res]
  {:id (->> res bindings :id first)
   :creator (vec (clojure.set/union
                   (->> (extract [:director :directorname] (solutions res))
                        (map #(assoc % :role "director"))
                        (map #(rename-keys % translation-map)))
                   (->> (extract [:editor :editorname] (solutions res))
                        (map #(assoc % :role "editor"))
                        (map #(rename-keys % translation-map)))))})

(defn merge-subjects
  "Merges the subjects, literary genres and music genres"
  [subjects litgenres musgenres]
  (disj (->> (clojure.set/union subjects litgenres musgenres)
             (map #(str/split % #"\s-\s"))
             flatten
             (remove #(re-matches #"^[0-9\.]+$" %))
             set)
        "Norvegica" "genre" "GRUPPE-SKJØNNSAL" "GRUPPE-1"
        "GRUPPE-10" "GRUPPE-11" "GRUPPE-12" "GRUPPE-13"
        "GRUPPE-14" "GRUPPE-15" "GRUPPE-16" "GRUPPE-17"
        "GRUPPE-18" "GRUPPE-19" "GRUPPE-20" "GRUPPE-21"
        "GRUPPE-2a" "GRUPPE-2b" "GRUPPE-3" "GRUPPE-4"
        "GRUPPE-5" "GRUPPE-6" "GRUPPE-7" "GRUPPE-8" "GRUPPE-9"
        "GRUPPE-DATA" "GRUPPE-MILJØ" "GRUPPE-OSLO-HYLLA"
        "GRUPPE-SKJØNNSAL" "GRUPPE-SLEKT" "GRUPPE-SPRÅK"))

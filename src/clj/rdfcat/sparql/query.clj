(ns rdfcat.sparql.query
  "SPARQL queries to get work and edition information from the RDF-catalogue."
  (:refer-clojure :exclude [filter concat group-by max min count])
  (:require [boutros.matsu.sparql :refer :all]
            [boutros.matsu.core :refer [register-namespaces]])
  (:import java.net.URI))

(register-namespaces {:bibo "<http://purl.org/ontology/bibo/>"
                      :rdfs "<http://www.w3.org/2000/01/rdf-schema#>"
                      :foaf "<http://xmlns.com/foaf/0.1/>"
                      :xfoaf "<http://www.foafrealm.org/xfoaf/0.1/>"
                      :deichman "<http://data.deichman.no/>"
                      :skos "<http://www.w3.org/2004/02/skos/core#>"
                      :dc "<http://purl.org/dc/terms/>"
                      :fabio "<http://purl.org/spar/fabio/>"
                      :muso "<http://purl.org/ontology/mo/>"
                      :dbo "<http://dbpedia.org/ontology/>"})

(defquery all-works [start n]
  (select-distinct :work)
  (from (URI. "http://data.deichman.no/books"))
  (where :work [:fabio :hasManifestation] :edition )
  (offset start)
  (limit n))

(defquery work
  [work]
  (select :id :title :creator :creatorname :contributor :contributorname
          :edition :editionlang :editionyear :editiontitle :editionsubtitle :editionformat
          :editioncreator :editioncreatorname :editionsubjectlabel
          :editiontranslator :editiontranslatorname :editioneditor :editioneditorname
          :editioncontributor :editioncontributorname :editionillustrator :editionillustratorname
          :director :directorname :actor :actorname :editionmusicgenrelabel :editiongenrelabel)
  (from (URI. "http://data.deichman.no/books"))
  (where work [:fabio :hasManifestation] :edition \;
              [:dc :title] :title \.
         :id [:fabio :hasManifestation] :edition \.
         (filter :id = work)
         (optional work [:dc :creator] :creator \.
                   :creator [:foaf :name] :creatorname \.)
         (optional work [:dc :contributor] :contributor \.
                   :contributor [:foaf :name] :contributorname \.)
         :edition [:dc :title] :editiontitle \;
                  [:dc :format] :editionformat \.
          (optional :edition [:dc :issued] :editionyear \.)
          (optional :edition [:dc :language] :editionl \.
                    :editionl [:rdfs :label] :editionlang)
          (optional :edition [:fabio :hasSubtitle] :editionsubtitle)
          (optional :edition [:dc :creator] :editioncreator \.
                    :editioncreator [:foaf :name] :editioncreatorname)
          (optional :edition [:bibo :director] :director \.
                    :director [:foaf :name] :directorname)
          (optional :edition [:deichman :actor] :actor \.
                    :actor [:foaf :name] :actorname)
          (optional :edition [:bibo :translator] :editiontranslator \.
                    :editiontranslator [:foaf :name] :editiontranslatorname)
          (optional :edition [:bibo :editor] :editioneditor \.
                    :editioneditor [:foaf :name] :editioneditorname)
          (optional :edition [:dc :contributor] :editioncontributor \.
                    :editioncontributor [:foaf :name] :editioncontributorname \.)
          (optional :edition [:bibo :illustrator] :editionillustrator \.
                    :editionillustrator [:foaf :name] :editionillustratorname \.)
          (optional :edition [:dc :subject] :editionsubject \.
                    (union (group :editionsubject [:skos :prefLabel] :editionsubjectlabel \.)
                           (group :editionsubject [:foaf :name] :editionsubjectlabel \.)))
          (optional :edition [:muso :genre] :editionmusicgenre \.
                    :editionmusicgenre [:rdfs :label] :editionmusicgenrelabel)
          (optional :edition [:dbo :literaryGenre] :genre \.
                    :genre [:rdfs :label] :editiongenrelabel)))
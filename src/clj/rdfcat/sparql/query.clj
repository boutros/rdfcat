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

(defquery subjects [start n]
  (select-distinct :subject)
  (from (URI. "http://data.deichman.no/books"))
  (where :doc a [:bibo :Document] \.
         :doc [:dc :subject] :editionsubject \.
         (union (group :editionsubject [:skos :prefLabel] :subject \.)
                (group :editionsubject [:foaf :name] :subject \.)))
  (offset start)
  (limit n))

(defquery litgenres []
  (select-distinct :litgenre)
  (from (URI. "http://data.deichman.no/books"))
  (where :doc a [:bibo :Document] \.
         :doc [:dbo :literaryGenre] :genre \.
         :genre [:rdfs :label] :litgenre))

(defquery musgenres []
  (select-distinct :musgenre)
  (from (URI. "http://data.deichman.no/books"))
  (where :doc a [:bibo :Document] \.
         :doc [:muso :genre] :editionmusicgenre \.
         :editionmusicgenre [:rdfs :label] :musgenre))

(defquery creators [start size]
  (select-distinct :_id :name :type :lifespan :note)
  (from (URI. "http://data.deichman.no/books"))
  (where :document [:dc :creator] :_id \.
         :_id a :type \;
             [:foaf :name] :name \.
         (optional :_id [:deichman :lifespan] :lifespan)
         (optional :_id [:skos :note] :note))
  (offset start)
  (limit size))

(defquery work
  [work]
  (select *)
  (from (URI. "http://data.deichman.no/books"))
  (where work [:fabio :hasManifestation] :edition \;
              [:dc :title] :title \.
         :id [:fabio :hasManifestation] :edition \.
         (filter :id = work)
         (optional work [:dc :creator] :creator \.
                   :creator [:foaf :name] :creatorname \.)
         (optional work [:dc :contributor] :contributor \.
                   :contributor [:foaf :name] :contributorname \.)
         (optional work [:bibo :director] :director \.
                   :director [:foaf :name] :directorname)
         (optional work [:bibo :editor] :editor \.
                  :editor [:foaf :name] :editorname)
         :edition [:dc :title] :editiontitle \;
                  [:dc :format] :editionformat \.
          (optional :edition [:dc :issued] :editionyear \.)
          (optional :edition [:dc :language] :editionl \.
                    :editionl [:rdfs :label] :editionlang)
          (optional :edition [:fabio :hasSubtitle] :editionsubtitle)
          (optional :edition [:dc :creator] :editioncreator \.
                    :editioncreator [:foaf :name] :editioncreatorname)
          (optional :edition [:bibo :director] :editiondirector \.
                    :editiondirector [:foaf :name] :editiondirectorname)
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

;fetch work.editor & work.director as well
(defquery work-update
  [work]
  (select :id :director :directorname :editor :editorname)
  (from (URI. "http://data.deichman.no/books"))
  (where work [:fabio :hasManifestation] :edition \.
         :id [:fabio :hasManifestation] :edition \.
         (filter :id = work)
         (optional work [:bibo :director] :director \.
                    :director [:foaf :name] :directorname)
         (optional work [:bibo :editor] :editor \.
                  :editor [:foaf :name] :editorname)))

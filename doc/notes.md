TODO
======
* muliggjør deselect uspesifisert år
* refakotrer og rydd opp
   - del p2-snippet opp i flere snippets
   - lag helperfns for å lage lister opp til n + link til se alle
   - helper/snippet for paginering
   - p2 query fn refaktorer til en fn
* emner clickable? søkefelt ->> emne: stumfilm
* error-handling when search fails
* vis alle utgaver hvis <=6, opp til 3 hvis flere
* http://purl.org/spar/fabio/hasURL, eks:
  SELECT * WHERE {<http://data.deichman.no/resource/tnr_431462> ?p ?o}
* dc:abstract
* hasPart, både uri og tekst?
  SELECT *
  FROM <http://data.deichman.no/books>
  WHERE { <http://data.deichman.no/resource/tnr_914497> ?p ?o}


REPL!
=====
(load-file "doc/repl.clj")
(in-ns 'rdfcat.repl)
(use 'clojure.repl)

Nivåer
1. Verk
2. Manifestasjon
3. Eksemplar



Mapping
=========

http get localhost:9200/rdfcat/work/_mapping

work
work.id
work.title
work.creators [ { id, name }, { ... } ]
work.editions [ { id, title, format, language, pubyear, creators }, { ... } ]
work.editions.creators [ { id, name, role } { ... } ]
work.subjects [ { id, label } ]

kanskje:
work.edition.description (dc:description 520-note)
work.edition.contents (dc:hasPart, brukes b.la. om sanger på CDer)

slå sammen til creators:
# dc.creator
# deichman.director (film)
# deichman.actor (film)
# dc.contribitor
# bibo.editor
# bibo.translator
# bibo.illustrator

kanskje:
work.creators [ { id, name/label, role } ]
hvor role = author|editor|director|editor|actor|translator|etc..

hva med litteær form?
SELECT DISTINCT ?format_label
WHERE { ?s <http://data.deichman.no/literaryFormat> ?format . ?format rdfs:label ?format_label }

sjanger?
SELECT DISTINCT ?genreLabel
FROM <http://data.deichman.no/books>
WHERE { ?s <http://dbpedia.org/ontology/literaryGenre> ?genre . ?genre rdfs:label ?genreLabel }

isåfall remove de med label #"GRUPPE-\n|SKJØNNSAL|" etc

Hva er egentlig forskjellen på sjanger, litterær for og emne?
For bibliotekaren - og for vanlige folk?



INDEKSERING
===========

dc.language
-----------
Fjerne "Norsk", når "Norsk Bokmål" finnes?

dc.creator (musikk)
Strippe "Popgruppe"/"Rockegruppe"  fra creator? (ex "Boyzone. Popgruppe")

evt overføre til rolle: "musical group"

NB noen har to foaf:name, eks
SELECT * WHERE { <http://data.deichman.no/organization/x29676200> ?p ?o}

http://xmlns.com/foaf/0.1/name  "Astroburger. Popgruppe"
http://xmlns.com/foaf/0.1/name  "Astroburger"

eller

SELECT * WHERE { <http://data.deichman.no/organization/x20806200> ?p ?o}

http://xmlns.com/foaf/0.1/name  "Palace of Pleasure"
http://xmlns.com/foaf/0.1/name  "Palace of Pleasure. Electronicagruppe"

SPARQLS
# get all works
# get work info



SELECT DISTINCT ?genre_label
FROM <http://data.deichman.no/books>
WHERE { ?bokok <http://purl.org/ontology/mo/genre> ?genre .
        ?genre rdfs:label ?genre_label
}

SELECT COUNT (distinct ?work)
FROM <http://data.deichman.no/books>
WHERE {
   ?work <http://purl.org/spar/fabio/hasManifestation> ?book
}

SELECT COUNT (distinct ?book)
WHERE {
   GRAPH <http://data.deichman.no/books> { ?book <http://purl.org/spar/fabio/isManifestationOf> ?work }
}

Formater
PREFIX dc: <http://purl.org/dc/terms/>

SELECT DISTINCT ?format
FROM <http://data.deichman.no/books>
WHERE {
  ?something dc:format ?format
}

feil dc:issued
-------------
Disse slutter på s|s., antageligvis sidetall i feil felt:

PREFIX bibo:<http://purl.org/ontology/bibo/>
PREFIX dc: <http://purl.org/dc/terms/>

SELECT ?book ?issued
FROM <http://data.deichman.no/books>
WHERE {
  ?book dc:issued ?issued
  FILTER (regex(?issued, 's\\.?$'))
} LIMIT 1000

Issued som begynner på bokstaven 'L' istedenfor tallet 1

PREFIX bibo:<http://purl.org/ontology/bibo/>
PREFIX dc: <http://purl.org/dc/terms/>

SELECT ?book ?issued
FROM <http://data.deichman.no/books>
WHERE {
  ?book dc:issued ?issued
  FILTER (regex(?issued, '^l'))
} LIMIT 1000

GUI
=================

rdfcat versjon 27.06.2013 | prototyp "Anne-Lena" | prototyp "Petter" | Hva er dette?
routes
/p1
/p2
/om

ikoner for format:
# bok
# lydbok
# cd
# dvdfilm

visninger
---------

#Work
work.title
work.creators

#Edition
ed.title (+ work.originaltitle?)
ed.subtitle
ed.creators w roles
ed.yearpublished

#Ex
Status | avdeling



PROTOTYPER
===========
1. 2 søkefelt:

* HVEM: forfatter/person/artist/band
* HVA: tittel/emne/sang/album/film

2. 1 søkefelt

* Dropdownliste med valg for å spesifisere



1+2 Felles
Filtrer resultat (alle disse gjelder utgave)
* år
* språk
* format


SPRÅK
=====

creator.role     på norsk
----------------------------------
contributor(bok) bidrag: Jon Bing, Carl
contributor(cd)  medvirkende: x, y, orchestar ditt
editor           redaktør: Hr Hansen
                 redaktører: A, B
dc.creator(bok)  forfatter: Knnut hamsn
dc.creator(cd)   artist: The Beatles
actor            skuespillere: Jhonny depp osv
director         regissør: F. F. Coppola
illustrator      illustrert av X
translator       oversatt av Y



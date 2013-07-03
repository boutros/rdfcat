(ns rdfcat.repl
  (:require [rdfcat.sparql.query :as query]
            [rdfcat.sparql.response :refer :all]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd])
  (:import java.net.URI))

(comment

(def w1 (URI. "http://data.deichman.no/work/x18264400_alice_in_wonderland"))
(def w2 (URI. "http://data.deichman.no/work/x14668800_peer_gynt"))
(def w3 (URI. "http://data.deichman.no/work/x16485900_ruffen_paa_nye_eventyr"))
(def w4 (URI. "http://data.deichman.no/work/x34275600_fars_hus"))
(def w5 (URI. "http://data.deichman.no/work/x12276900_sult"))
(def w6 (URI. "http://data.deichman.no/work/x30496100_dmitrij_sjostakovitsj"))

(def r1 (fetch (query/work w1)))
(def r2 (fetch (query/work w2)))
(def r3 (fetch (query/work w3)))
(def r4 (fetch (query/work w4)))
(def r5 (fetch (query/work w5)))

)

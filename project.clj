(defproject rdfcat "0.1.0-SNAPSHOT"
  :description "Prototype på katalogsøk basert på deichmans rdf-store"
  :url "http://github.com/boutros/rdfcat"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [clj-http "0.7.3"]
                 [clojurewerkz/elastisch "1.2.0-beta1"]
                 [matsu "0.1.3-SNAPSHOT"]
                 [com.taoensso/timbre "2.1.2"]]
  :plugins [[lein-cljsbuild "0.3.0"]
            [lein-ring "0.8.3"]]
  ;:hooks [leiningen.cljsbuild]
  :source-paths ["src/clj"]
  :cljsbuild {
              :builds {
                       :main {
                              :source-paths ["src/cljs"]
                              :compiler {:output-to "resources/public/js/cljs.js"
                                         :optimizations :simple
                                         :pretty-print true}
                              :jar true}}}
  :main rdfcat.index
  :ring {:handler rdfcat.server/app}
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
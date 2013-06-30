(ns rdfcat.util.map
  "Map+set utility functions"
  (:require [clojure.set :refer [union]]))

(defn map-vals
  "Apply f on each values in m."
  [m f & args]
  (into {}
        (for [[k v] m]
           [k (apply f v args)])))

(defn- ensure-set [x]
  (if (set? x) x (hash-set x)))

(defn merge-reduce
  "Merges maps and reduces the values into sets."
  [sq]
  (->> (map #(map-vals % ensure-set) sq)
       (reduce #(merge-with union %1 %2))))
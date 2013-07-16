(ns rdfcat.util.string
  "String transformation and utility functions"
  (:require [clojure.string :as string]))

(defn date-clean [s]
  "Takes a string and return a 4-digit year as integer,
  or nil if it's not possible to generate a valid date."
  (let [cleaned (Integer.
                 (subs
                   (.replaceAll (str s "0000") "[^0-9]" "") 0 4))]
   (if (zero? cleaned)
     nil
     cleaned)))

(defn length-num
  "Given a collection of strings and a maximum character length,
  returns how many of the elements in coll needed to reach length. "
  [coll length]
  (loop [c (map count coll) s 0 i 0]
    (cond
      (> s length) (dec i)
      (empty? c) i
      :else (recur (rest c) (+ (first c) s) (inc i)))))

(defn list-names
  "Takes a coll of names, and list them with conjuctions according to the
  length of the coll."
  [coll]
  (cond
    (= 0 (count coll)) ""
    (= 1 (count coll)) (first coll)
    (= 2 (count coll)) (string/join " og " coll)
    (= 3 (count coll)) (str (first coll) ", " (second coll) " og " (last coll))
    :else (str (first coll) ", " (second coll) " m.fl.")))
(ns rdfcat.util.string)

(defn date-clean [s]
  "Takes a string and return a 4-digit year as integer,
  or nil if it's not possible to generate a valid date."
  (let [cleaned (Integer.
                 (subs
                   (.replaceAll (str s "0000") "[^0-9]" "") 0 4))]
   (if (zero? cleaned)
     nil
     cleaned)))
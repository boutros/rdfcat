(ns rdfcat.string-test
  (:require [clojure.test :refer :all]
            [rdfcat.util.string :refer :all]))

(deftest test-date-clean
  (are [a b] (= (date-clean a) b)
       "1995-"     1995
       "cop. 1929" 1929
       "(198-)"    1980
       "u.å"       nil
       "19-"       1900
       "1895-1979" 1895
       "p1994"     1994))
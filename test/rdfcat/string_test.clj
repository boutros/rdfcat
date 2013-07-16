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

(deftest test-length-num
  (is (= 2 (length-num ["kake" "mann" "fisk"] 10)))
  (is (= 3 (length-num ["kake" "mann" "fisk"] 100)))
  (is (= 6 (length-num ["a" "bb" "ccc" "ddd" "eee" "xxx" "yy"] 15))))

(deftest test-list-names
  (is (= (list-names []) ""))
  (is (= (list-names ["Per"]) "Per"))
  (is (= (list-names ["Per" "Pål"]) "Per og Pål"))
  (is (= (list-names ["Per" "Pål" "Espen"]) "Per, Pål og Espen"))
  (is (= (list-names ["Per" "Pål" "Frank" "Franz"]) "Per, Pål m.fl.")))
(ns spell-checker.query
  (:require [clojure.string :as string]
            [spell-checker.dictionary :refer :all]))


;; do reductions on the dictionary with different filter functions, theneby allowing
;; the user some more advanced searching ability.

;; for example , function for getting words that start with co , end in ing and is 9 letters long.

(defn match-start
  "takes an arbitrary amount of words and return a function that
  checks all the supplied letters are at the starts, in the order they are given."
  [& letters]
  (fn [word] (string/starts-with? (str letters) word)))

(defn match-end
  "does the same as match-start, but checks the letters given match the end of the word"
  [& letters]
  (fn [word] (string/ends-with? (str letters) word)))

(defn match-length
  "given a number , will return a function that takes a word and will check it's length
  using that count"
  [length]
  (fn [word] (= (count word) length)))

;; could take a map of k/v pairs
;; k would be the kw for the right fn
;; value could be a vector of arguments.
(defn all-tests
  "takes a map of args, and will call the appropriate args
  using the keywords in the map, so each match-fn gets the right args"
  [args]
  (let [db {
            :start (match-start (:start args))
            :end (match-end (:end args))
            :length (match-length (:length args))
            }]))

(defn filter-on-match
  [dictionary fns]
  (filter (comp fns) dictionary))


(filter-on-match
 (load-dictionary)
 (match-start "cou")
 (match-end "ing")
 (match-length 8))

;; rewriting our dictionary for much faster query searches.
;; dictionary will be a tree , as a map, with letters as the keys
;; like this

(comment
  ;; which will continue until every word starting with a specific letter is covered.
  {"a" {"ab" {"aba" ...}}
   "b" {"ba" {""}}}

  ;; the function in charge of adding appropriately given a word. We will probably do
  ;; three layers of keys

  (defn map-to-dictionary
    [word]
    (let [index ()]))
  )

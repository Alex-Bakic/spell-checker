(ns spell-checker.grammar
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [ends-with? join capitalize]]))

;; this file checks that sentences are properly constructed e.g don't miss full stops,
;; start with a capital letter etc.

;; define a function that checks that a sentence ends with no more than set of allowed terminators.
;; Only full stops, question and exclamation marks are allowed.

;; check that a string has the allowed punctuation at the end
(defn punctuated?
  [s]
  (let [punctuation #{"!" "." "?"}]
    (some (partial ends-with? s) punctuation)))

;; check that a string has no more than one piece of punctuation at the end.
(defn excessive-punctuation?
  [s]
  (-> s
      (reverse)
      (second)
      (punctuated?)))

;; trim the sentence so that only the first encountered piece of punctuation is kept and anything after removed..
(defn trim-excessive-punctuation
  [s]
  (let [words-and-punctuation (vals (group-by punctuated? s))
        words (first words-and-punctuation)
        only-needed-punctuation (first (second words-and-punctuation))
        correct-ending (conj words only-needed-punctuation)]
    (join correct-ending)))

;; define a function which checks that a given word has a capital letter
(defn starts-with-capital?
  [s]
  (Character/isUpperCase (first s)))

;; given a lower case word convert the first letter into uppercase
(defn capitalise-word
  [s]
  (capitalize s))


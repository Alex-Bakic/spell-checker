(ns spell-checker.dictionary
  (:require [clojure.string :refer [split-lines lower-case includes?]]
            [clojure.spec.alpha :as s]))

;; this file checks if a word has been misspelled or not and uses
;; the concept of differences between words to find the closest word
;; to it (the one with the least differences).

;; put all the words from our text file into a set for manipulation.
(defn load-dictionary
  []
  (-> (slurp "/home/alex/spell-checker/src/words.txt")
      (split-lines)
      (set)))

(def dictionary (load-dictionary))

(defn all-lowercase?
  [word]
  (every? #(Character/isLowerCase %) word))

;; check that a word is in the dictionary
(defn in-dictionary?
  [word]
  (dictionary (if (all-lowercase? word) word (lower-case word))))






(ns spell-checker.dictionary-spec
  (:require [spell-checker.dictionary :refer [dictionary]]))

;; specs for dictionary , with an entry assuming the form : ["*the-word*" popularity [definitions] trademark?]

(s/def ::trademark (s/? boolean?))

(s/def ::an-entry (s/def ::an-entry (s/cat :the-word ::h/word 
                                           :popularity-per-million ::h/perMillion 
                                           :definitions ::h/definitions
                                           :trademark ::trademark)))

(s/def ::all-entries (s/coll-of ::an-entry))

;; since for now we don't actually know what the word actually is, we are just doing completely arbitrary,
;; inaccurate generations, but when we design functions to do this properly, we will be making 2 API calls
;; per word, which means , being on the free tier I can only do 1250 words per day.

;; new dictionary snippet
;; (gen/generate (s/gen (s/cat :word ::word :popularity ::perMillion :definition ::definitions)))


;; use with-gen to show how we would try and brute-force almost, for more words in the dictionary
(defn just-letters?
  [word]
  (every? #(Character/isLetter %) word))

(defn lower-case?
  [word]
  (every? #(Character/isLowerCase %) word))

(s/def ::new-word (s/with-gen string?
                    #(s/gen (gen/such-that (complement empty?)
                                           (s/and string? just-letters? lower-case?)))))


(comment

  ;; this will help us generate a new, more detailed dictionary

  ;; helper functions to help fmap generate accurate results
  (defn grab-popularity
    [word]
    (-> (frequency word)
        (:frequency)
        (:perMillion)))
  
  (defn grab-definitions
    [word]
    (-> (definitions word)
        (:definitions)))

  ;; so this could be how we would generate accurate words, using the API functions themselves,
  ;; but you might want to swap them out for generated results while testing.
  ;; we can keep the (s/gen ::word) set as the set is the same as our dictionary, and we need
  ;; a generator for it to work.
  (def new-word (gen/fmap (fn [word] [word (grab-popularity word) (grab-definitions word)])
                          (s/gen ::word)))

)

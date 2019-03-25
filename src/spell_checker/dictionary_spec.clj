(ns spell-checker.dictionary-spec
  (:require [spell-checker.dictionary :refer [dictionary]]))

(s/def ::trademark (s/? boolean?))

(s/def ::an-entry (s/def ::an-entry (s/cat :the-word ::h/word 
                                           :popularity-per-million ::h/perMillion 
                                           :definitions ::h/definitions
                                           :trademark ::trademark)))

(s/def ::all-entries (s/coll-of ::an-entry))

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

(ns spell-checker.spelling)

;; for every word in the dictionary, we are going to tag it with the number of changes our misspelled word
;; would need to make before it was the same as that word. Then we call all the words that have the key "1"
;; associated and list to the user as suggestions.


;; by finding the length of the word, if it is a short word. Then it is likely to be a small mistake.
;; Moreover, if the word is less than or equal to 5 characters long, then we will only return maps of
;; movements of 1.

;; if the word is between 5 or 6 , we will return 2 movements max.
;; if the word is between 6 and 9 we will return 3 movements max.
;; more than 9 characters we will return 4 movements max.
(defn dictate-suggestions
  [bad-word]
  (let [word-count (count bad-word)]
    (cond
      (< word-count 5) 1
      (<= 5 word-count 6) 2
      (<= 7 word-count 9) 4
      :else 4)))

(defn update-distances
  [moves word suggestion-limit]
  (let [moves-of-all-words {}]
    (if (> moves suggestion-limit) nil  (conj moves-of-all-words [moves word]))))

;; find the differences in the strings. We return 0 for no difference, 1 for a difference in the
;; letters. So we can sum all the differences in the word and see if it is at all similar.
(defn movements [bad-letter given-letter]
  (if (= bad-letter given-letter) 0 1))

(defn movements-of-every-letter
  [bad-letters given-letters]
  (reduce + (map movements bad-letters given-letters)))

(defn difference-in-length [bad-word given-word]
  (Math/abs (- (count bad-word) (count given-word))))

(defn distance
  [bad-word given-word]
  (let [bad-letters (seq bad-word)
        given-letters (seq given-word)
        every-difference (+ (movements-of-every-letter bad-letters given-letters)
                            (difference-in-length bad-word given-word))]
    (update-distances every-difference given-word (dictate-suggestions bad-word))))

;; ranks the suggestions by accuracy, will typically be called, with all-suggestions
;; being called as the parameter.

;; for each map we come across
;; have we seen the key? , if so add the value to the vector with that key
;; if we haven't, conj a new key value vector pair.
;; but we know that the maximum number of movements will allow is 4.
;; So we can make the organising process simpler.

(defn add-to-suggestions
  [organised-map suggested-word-map]
  (let [k (keys suggested-word-map)
        v (first (vals suggested-word-map))]
    (update-in organised-map k conj v)))

(defn organise-suggestions
  [all-the-suggestions]
  (reduce add-to-suggestions {1 [] 2 [] 3 [] 4 []} all-the-suggestions))

;; map over the map, check if vals are empty, otherwise print.
(defn show-suggestions [] ,,,)

(defn show-ranked-suggestions
  [all-the-suggestions]
  (let [organised-suggestions (organise-suggestions all-the-suggestions)]
    ()))


;; will return a sequence of maps, for every word
(defn all-suggestions
  [bad-word]
  (->> (map (partial distance bad-word) dictionary)
       (filter (complement nil?))
       ;; call ranked-suggestions for full printing instead...
       (organise-suggestions)))

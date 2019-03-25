(ns spell-checker.handler-spec
  (:require [spell-checker.dictionary :refer [dictionary]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

;;
;;
;;    SPECS FOR ALL THE HANDLER FUNCTIONS , IN HANDLER.CLJ
;;
;;

;; SPECS FOR ALL THESE FUNCTIONS SO THEY ONLY RETURN WHAT WE WANT

;; spec for checking the first key/value pair of the map, which is always the same
;; regardless of which function is called, the other specs , excluding the ones
;; that say ::check-"" , are speccing the second k/v pair.

;; made slight change to word so we use the set itself instead
;; if the word isn't in the dictionary, then we know it's pointless
;; checking if it is a string and lower-case. 
(s/def ::dictionary dictionary)
(s/def ::word ::dictionary)

(s/def ::synonyms (s/coll-of string? :kind vector? :distinct true))
(s/def ::antonyms (s/coll-of string? :kind vector? :distinct true))

;; different to the one used for pronunciation.
(s/def :rhymes/all (s/coll-of string? :kind vector? :distinct true))
(s/def :rhymes/empty (s/and map? empty?))
(s/def ::rhymes (s/or :results (s/keys :req-un [:rhymes/all])
                      :empty :rhymes/empty))

(s/def ::frequency
  (s/keys :req-un [::zipf ::perMillion ::diversity]))

(s/def ::zipf (s/and number? pos? #(<= % 10)))
(s/def ::perMillion (s/and number? pos? #(<= % 1000000)))

(s/def ::diversity (s/and float? pos? #(<= 0 % 1)))

(s/def ::syllables
  (s/keys :req-un [::count ::list]))

(s/def ::count (s/and int? #(> % 0)))
(s/def ::list (s/coll-of string? :kind vector?))

(s/def ::definitions (s/coll-of ::each-definition))

(s/def ::each-definition
  (s/keys :req-un [::definition ::partOfSpeech]))


(s/def ::definition string?)

(s/def ::partOfSpeech
  #{"noun" "pronoun" "adjective" "adverb" "proverb" "verb"
    "preposition" "conjunction" "interjection" "exclamation"})

(s/def ::check-synonyms
  (s/keys :req-un [::word ::synonyms]))

(s/def ::check-antonyms
  (s/keys :req-un [::word ::antonyms]))

(s/def ::check-rhymes
  (s/keys :req-un [::word ::rhymes]))

;; and to check the entire map
(s/def ::check-syllables
  (s/keys :req-un [::word ::syllables]))

(s/def ::check-frequency
  (s/keys :req-un [::word ::frequency]))

(s/def ::check-definitions
  (s/keys :req-un [::word ::definitions]))

;; checking the word function, which can produce pretty much any combination of results.
;; need to put them all in the :opt-un for the most part.

;; top level keys, which are required:
;;               :word  :results :syllables :pronunciation :frequency 

(s/def ::check-word
  (s/keys :req-un [::word ::results ::syllables ::pronunciation :word/frequency]))

(s/def :word/frequency (s/and number? #(<= 0 % 10)))

(s/def ::pronunciation
  (s/keys :req-un [::all]))

(s/def ::all string?)

;; map keys , in the vector , which are required:
;;               :definition, :partOfSpeech

;; map keys , in the vector, which are optional:

;;               :examples, :derivation, :typeOf, :similarTo , :entails

(s/def ::examples (s/coll-of string? :kind vector? :distinct true))
(s/def ::derivation (s/coll-of string? :kind vector? :distinct true))
(s/def ::typeOf (s/coll-of string? :kind vector? :distinct true))
(s/def ::similarTo (s/coll-of string? :kind vector? :distinct true))
(s/def ::entails (s/coll-of string? :kind vector? :distinct true))
(s/def ::inCategory (s/coll-of string? :kind vector? :distinct true))

(s/def ::results (s/coll-of
                   (s/merge
                     ::each-definition
                     (s/keys :req-un [] :opt-un [::examples ::derivation ::typeOf
                                                 ::similarTo ::entails ::inCategory]))))

(s/def ::check-examples (s/keys :req-un [::word ::examples]))


;; need to write a macro that returns the correct s/fdef check given
;; a particular parameter. So the macro would use the :param keyword as
;; to return either a :ret ::check-words or to return ::check-syllables

(s/def ::check-param #{"synonyms" "antonyms" "definitions" "syllables"
                       "rhymes" "frequency"})

(defn param->spec
  "given a param, returns the appropriate spec check for that request."
  [param]
  (let [specs {"synonyms" ::check-synonyms
               "antonyms" ::check-antonyms
               "definitions" ::check-definitions
               "frequency" ::check-frequency
               "syllables" ::check-syllables
               "rhymes" ::check-rhymes
               nil ::check-word}]
    (specs param)))

(defn fn-check
  "Is passed the value that :fn would from fdef,
  for all our api functions that would be:

    {:args {:word \"the-word\"} , :ret {:word \"daphnis\"}}

  Given this I would take the value of the word in :args and the
  value of the word in :ret and check for equality.
  "
  [conformed-map]
  (let [args-word (:word (:args conformed-map))
        ret-word (:word (:ret conformed-map))]
    (= args-word ret-word)))

(defmacro param->fdef
  [param]
  (list 's/fdef (symbol param)
          :args (s/cat :word ::word)
          :ret (param->spec param)
          :fn 'fn-check))


(s/def ::parameter #{:synonyms :antonyms :definitions :syllables
                     :rhymes :frequency :examples :word})

;; (s/def ::check-synonyms ...)
;; (s/def ::check-antonyms ...)
;; (s/def ::check-rhymes ...)
;; (s/def ::check-definitions ...)
;; (s/def ::check-syllables ...)
;; (s/def ::check-word ...)
;; (s/def ::check-frequency ...)
;; (s/def ::check-examples ...)

(defn dispatch-param
  [response]
  (->> response
       (second)

       (first)
       (s/conform ::parameter)))

(s/def ::type keyword?)

(defmulti spell-checker :type)

(defmethod spell-checker :synonyms [_] (s/keys :req-un [::type ::word ::synonyms]))
(defmethod spell-checker :antonyms [_] (s/keys :req-un [::type ::word ::antonyms]))
(defmethod spell-checker :rhymes [_] (s/keys :req-un [::type ::word ::rhymes]))
(defmethod spell-checker :syllables [_] (s/keys :req-un [::type ::word ::syllables]))
(defmethod spell-checker :frequency [_] (s/keys :req-un [::type ::word ::frequency]))
(defmethod spell-checker :definitions [_] (s/keys :req-un [::type ::word ::definitions]))
(defmethod spell-checker :word [_] (s/merge (s/keys :req-un [::type]) ::check-word))


;; (s/def ::response (s/multi-spec spell-checker ::type))
(s/def ::response (s/multi-spec spell-checker :type))

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

(s/def ::new-word (s/with-gen string? #(s/gen (gen/such-that (complement empty?) (s/and string? just-letters? lower-case?)))))


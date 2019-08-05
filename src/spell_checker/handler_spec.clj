(ns spell-checker.handler-spec
  (:require [spell-checker.dictionary :refer [dictionary]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

;;
;;
;;    SPECS FOR ALL THE HANDLER FUNCTIONS , IN HANDLER.CLJ
;;
;;

;; dictionary is a set of words, which will be what all the input is checked against.
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

(s/def ::definition string?)

(s/def ::each-definition
  (s/keys :req-un [::definition ::partOfSpeech]))


(s/def ::definitions (s/coll-of ::each-definition))

;; using camelCasing as the api returns keys in this format...
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

(comment

  (hs/param->fdef "synonyms")
  spell-checker.core/synonyms
  spell-checker.core> (s/exercise-fn `synonyms 4)
  Execution error at spell-checker.core/eval17718 (form-init6650090911814085149.clj:143).
  No :args spec found, can't generate
  spell-checker.core> (doc synonyms)
  -------------------------
  spell-checker.handler/synonyms
  ([word])
  nil
  spell-checker.core>

  )

(s/def ::parameter #{:synonyms :antonyms :definitions :syllables
                     :rhymes :frequency :examples :word})

(defn dispatch-param
  [response]
  (->> response
       (second)
       (first)
       (s/conform ::parameter)))

;; the way defmulti works is that it needs you to have a "universal key", a key that will appear
;; in every map that the api could return. So for example let's say that key is :type. Now you need
;; to be careful when choosing a namespaced key or not, in our case the api will just have :type

;; the check that this universal key should have , it is just a check
;; that is done like the other keys, the value itself is used as the differentiator for the methods.
;; for example, if the map was {:type :synonyms :word "some-word" :synonyms ["some" "synonyms"]}
;; then the method called would be the one that expects :


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

(s/def ::new-word (s/with-gen string?
                    #(s/gen (gen/such-that (complement empty?)
                                           (s/and string? just-letters? lower-case?)))))



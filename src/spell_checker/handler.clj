(ns spell-checker.handler
  (:require [clj-http.client :as client]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.data.json :refer [read-str]]
            [clojure.spec.alpha :as s]
            [clojure.string :only [trim]]
            [spell-checker.handler-spec :as hs]))

;; so those specs define all the  possible requests someone can make, with those 8 checks in place
;; now need to define the wrapper for the Words API on Rapid API

;; for our specs to work, the cleanest solution to avoid having to do converts/ messing around symbols
;; and such is to just convert the string keys that Words API returns into keywords so that we can
;; write normal specs. Done by using the `keywordize-keys` function in the clojure.walk lib.

;; need to specify that we accept json otherwise request won't be executed.
;; request url --> "https://wordsapiv1.p.rapidapi.com/words/your-word-here/your-parameter-here"

;; api key -> ** set using below fn at repl. 
;; included header "X-RapidAPI-Key" "the-key"

;; takes the response map, pulls what's in the body out and converts it.

;; atom that stores the api key

(def ^:dynamic ^:private *api-key* (atom nil))

(defn set-api-key!
  [key]
  (swap! *api-key* (fn [_ k] k) key))

(defn json->clj
  [request]
  (let [body (:body request)]
    (read-str body)))

;; take the first key of the map and conj it to the :type key
(defn- show-type
  [m]
  (assoc m :type (second (keys m))))

;; so let's try and make a get request
(defn- request
  [[header-name header-value] param word]
  (let [url "https://wordsapiv1.p.rapidapi.com/words/"
        req (str url word "/" param)
        options {:headers {header-name header-value}
                 :accept :json}]
      (client/get req options)))

(defn- format-response
  [[header-name header-value] param word]
  (-> (request [header-name header-value] param word)
      (json->clj)
      (keywordize-keys)
      (show-type)))
 
;; partially apply to avoid having to constantly recall defaults.
(defn- defaults
  [param word]
  (format-response ["X-RapidAPI-Key" @*api-key*] param word))

(defn word
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]}
  (defaults nil word))

(defn synonyms
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]}
  (defaults "synonyms" word))

(defn antonyms
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]}
  (defaults "antonyms" word))

(defn definitions
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]} 
  (defaults "definitions" word))

(defn examples
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]}
  (defaults "examples" word))

(defn rhymes
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]} 
  (defaults "rhymes" word))

(defn syllables
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]} 
  (defaults "syllables" word))

(defn frequency
  [word]
  {:pre [(s/valid? ::hs/word word)]
   :post [(s/valid? ::hs/response %)]}
  (defaults "frequency" word))


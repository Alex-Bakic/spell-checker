(ns spell-checker.core
  (:gen-class)
  (:require [clojure.spec.alpha :as s]
            [spell-checker.handler-spec :as hs]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args])

;; can use (s/def :event/type #{:event/search :event/error}) too

(comment

(s/def :event/type #{:event/search :event/error})
;; :event/type
(s/def :event/timestamp int?)
(s/def :search/url string?)
(s/def :error/message string?)
(s/def :error/code int?)

(defmulti event-type :event/type)

(defmethod event-type :event/search [v]
  (do (prn v)
      (s/keys :req [:event/type :event/timestamp :search/url])))

(defmethod event-type :event/error [v]
  (do (prn v)
      (s/keys :req [:event/type :event/timestamp :error/message :error/code])))

(s/def :event/event (s/multi-spec event-type :event/type))

(s/def :event/event (s/multi-spec event-type
                                  (fn [genv tag]
                                        (assoc genv :event/type tag)))))

(s/valid? :event/event
  {:event/type :event/search
   :event/timestamp 1463970123000
   :search/url "https://clojure.org"})


;; true

(s/valid? :event/event
  {:event/type :event/error
   :event/timestamp 1463970123000
   :error/message "Invalid host"
   :error/code 500})

;; true

(s/explain :event/event
  {:event/type :event/restart})
;; #:event{:type :event/restart} - failed: no method at: [:event/restart] spec: :event/event

;; was :spell-checker/type
(s/def ::type keyword?)
(s/def ::word string?)
(s/def ::synonyms (s/coll-of string? :kind vector? :distinct true))
(s/def ::antonyms (s/coll-of string? :kind vector? :distinct true))

(defmulti spell-checker :type)

(defmethod spell-checker :synonyms [_] (s/keys :req-un [::type ::word ::synonyms]))

(defmethod spell-checker :antonyms [_] (s/keys :req-un [::type ::word ::antonyms]))

(s/def :spell-checker/response (s/multi-spec spell-checker :type))

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

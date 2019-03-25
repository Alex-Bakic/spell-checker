(ns spell-checker.writing
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [includes?]]))

;; could contradict previous advice with this example showing why it may be very tedious
;; to always have one predicate check separated, as it can lead to so much boilerplate.
;; show that in simple, pure checks it really isn't too bad if you use s/or and tags etc.
(defn good-addition?
  [sentence]
  (some (partial includes? sentence) #{"Furthermore" "Therefore" "In addition" "Moreover"}))

(defn bad-addition?
  [sentence]
  (some (partial includes? sentence) #{"And" "The" "Because"}))

(defn good-example?
  [sentence]
  (some (partial includes? sentence) #{"To Illustrate" "Comparably" "Equivalently"}))

(defn bad-example?
  [sentence]
  (some (partial includes? sentence) #{"It means" "Then that" "Ok then"}))

(defn good-summary?
  [sentence]
  (some (partial includes? sentence #{"Clearly" "In conclusion" "To summarise"})))

(defn bad-summary?
  [sentence]
  (some (partial includes? sentence #{"That's all" "Yeh I'm done"})))

;; specs

;; for the "bad" functions we would have to check that they return nil
;; as in the s/and that would mean both would return true, for being in the good
;; set and not in the bad one.

(s/def ::good-addition? good-addition?)
(s/def ::bad-addition? bad-addition?)
(s/def ::good-example? good-example?)
(s/def ::bad-example? bad-example?)
(s/def ::good-summary? good-summary?)
(s/def ::bad-summary? bad-summary?)

(s/def ::check-addition (s/or :good ::good-addition? :bad ::bad-addition?))
(s/def ::check-example (s/or :good ::good-example? :bad ::bad-example?))
(s/def ::check-summary (s/or :good ::good-summary? :bad ::bad-summary?))


(comment

  "previous thoughts on solving this problem, might still use"

   (ns hello-world.core
      (:require [clojure.spec.alpha :as s]
                [clojure.string :refer [ends-with? split]]))

    ;; let's say we are checking a sentence for good connectives and openers.
    ;; For example, when talking about the addition to an idea or concept we could use:
   ;;    - furthermore, therefore, in addition, moreover

    (defn good-addition? [sentence]
       ;; the regex splits on the following criteria : either whitespace or comma.
       (let [opening-word (first (split sentence #"\s|,"))
             good-openers #{"Furthermore" "Therefore" "In addition" "Moreover"}
             bad-openers #{"And" "The" "Because"}]
         (and (good-openers opening-word) (nil? (bad-openers opening-word)))))

    ;; Good words for signalling an example could be:
    ;;   - to illustrate, for instance, just as significant , comparably, equivalently.
    ;; (defn good-example? [s] ;; pretty much identical to fn above)

    ;; For bringing your report to an end:
    ;;   - in summary , in conclusion , clearly , lastly
    ;; (defn good-summary? [s] ;; pretty much identical to fn above)

)

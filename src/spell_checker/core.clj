(ns spell-checker.core
  (:gen-class)
  (:require [clojure.spec.alpha :as s]
            [spell-checker.handler :as h]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args])

;; handler should define some functions which setup instrumentation
;; then they should be called here. Then from handler we should be able to call
;; the api functions with the validation included.

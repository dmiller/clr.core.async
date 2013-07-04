(ns clojure.core.async.mutex-test
  (:use clojure.test)
  (:require [clojure.core.async.impl.mutex :as mutex])   ;;; DM: Added
  )                                            ;;; (:import (clojure.core.async Mutex))

(deftest mutex-test
  (let [lock (mutex/mutex)]                         ;;; Mutex.
    (.lock lock)
    (try
      ;; do stuff
      (finally
       (.unlock lock)))))
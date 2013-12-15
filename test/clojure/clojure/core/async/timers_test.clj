(ns clojure.core.async.timers-test
  (:require [clojure.test :refer :all]
            [clojure.core.async.impl.timers :refer :all]
            [clojure.core.async :as async])
  (:import [System.Threading Thread ThreadStart]))                   ;;; DM:Added

(deftest timeout-interval-test
  (let [start-stamp (current-time-millis)                ;;; System/currentTimeMillis
        test-timeout (timeout 500)]
    (is (<= (+ start-stamp 500)
            (do (async/<!! test-timeout)
                (current-time-millis)))                  ;;; System/currentTimeMillis
        "Reading from a timeout channel does not complete until the specified milliseconds have elapsed.")))

(defn pause [ms] 
  (System.Threading.Thread/Sleep ms))		
		
(deftest timeout-ordering-test
  (let [test-atom (atom [])
        timeout-channels [(timeout 8000)
                          (timeout 6000)
                          (timeout 7000)
                          (timeout 5000)]
        threads (doall (for [i (range 4)]
                         (doto (Thread. (gen-delegate ThreadStart [] 
						                   (do (async/<!! (timeout-channels i))        ;;; Add gen-delegate, remove #
                                               (swap! test-atom conj i))))
                           (.Start))))]                                                ;;; .Start
    (doseq [thread threads]
      (.Join ^Thread thread))                              ;;; .join
    (is (= @test-atom [3 1 2 0])
        "Timeouts close in order determined by their delays, not in order determined by their creation.")))
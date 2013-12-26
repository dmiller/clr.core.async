;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(assembly-load-from "Utilities.dll")

(ns ^{:skip-wiki true}
  clojure.core.async.impl.timers
  (:require [clojure.core.async.impl.protocols :as impl]
            [clojure.core.async.impl.channels :as channels])
  ;;;(:import [java.util.concurrent DelayQueue Delayed TimeUnit ConcurrentSkipListMap])
  (:import [clojure.core.async IDelayed DelayQueue]
           [clojure.lang  Seqable]))

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Skip list implementation taken from the CLJS implementation
;;; Find it in cljs.core.async.impl.timers

(def MAX_LEVEL 15) ;; 16 levels
(def P (/ 1 2))

;;;  Added
(def ^:private ^Random rndm (Random.))


(defn random-level
  ([] (random-level 0))
  ([level]
    (if (and (< (.NextDouble rndm) P)                      ;;; (.random js/Math)
             (< level MAX_LEVEL))
      (recur (inc level))
      level)))

(deftype SkipListNode [key ^:mutable val forward]
  Seqable                                                 ;;; ISeqable
  (seq [coll]                                             ;;; -seq
    (list key val))

  ;;;IPrintWithWriter
  ;;;(-pr-writer [coll writer opts]
  ;;;  (pr-sequential-writer writer pr-writer "[" " " "]" opts coll))
	)

(defn skip-list-node
  ([level] (skip-list-node nil nil level))
  ([k v level]
    (let [^|System.Object[]| arr (make-array Object (inc level))]            ;;; DM: Added Object, added type hint
      (loop [i 0]
        (when (< i (alength arr))
          (aset arr i nil)
          (recur (inc i))))
      (SkipListNode. k v arr))))

(defn safe-aget	[^|System.Object[]| a ^long i]
  (when (and (< i (alength a))
             (>= i 0))
    (aget a i)))
	   
(defmacro aget-forward [x i]                                                  ;;; DM: Added
  `(safe-aget (.-forward ~(with-meta x {:tag 'SkipListNode})) ~i))            ;;; DM: Added
	  
(defn least-greater-node
  ([x k level] (least-greater-node x k level nil))
  ([x k level update]
    (if-not (neg? level)
      (let [x (loop [x x]
                (if-let [^SkipListNode x' (aget-forward x level)]
                  (if (< (.-key x') k)
                    (recur x')
                    x)
                  x))]
        (when-not (nil? update)
          (aset ^|System.Object[]| update level x))
        (recur x k (dec level) update))
      x)))

;; DM: Added interface to provide methods for the SkipList type
(definterface ISkipList
  (put [k v])
  (remove [k])
  (ceilingEntry [k])
  (floorEntry [k]))
	  
(deftype SkipList [header ^:volatile-mutable level]                      ;;; ^:mutable
  ISkipList                                                              ;;; Object
  (put [coll k v]
    (let [^|System.Object[]| update (make-array Object MAX_LEVEL)                           ;;; DM: Added Object and type hint
          x (least-greater-node header k level update)
          ^SkipListNode x (aget-forward x 0)]
      (if (and (not (nil? x)) (== (.-key x) k))
        (set! (.-val x) v)
        (let [new-level (random-level)]
          (when (> new-level level)
            (loop [i (inc level)]
              (when (<= i (inc new-level))
                (aset update i header)
                (recur (inc i))))
            (set! level new-level))
          (let [^SkipListNode x (skip-list-node k v new-level)]    ;;; (make-array new-level is wrong?
            (loop [i 0]
              (when (<= i level)
                (let [^|System.Object[]| links (.-forward ^SkipListNode (aget update i))]
                  (aset ^|System.Object[]| (.-forward x) i (safe-aget links i))
                  (aset links i x)))))))))

  (remove [coll k]
    (let [^|System.Object[]| update (make-array Object MAX_LEVEL)                           ;;; DM: Added Object
          x (least-greater-node header k level update)
          ^SkipListNode x (aget-forward x 0)]
      (when (and (not (nil? x)) (== (.-key x) k))
        (loop [i 0]
          (when (<= i level)
            (let [^|System.Object[]| links (.-forward ^SkipListNode (aget update i))]
              (if (identical? (safe-aget links i) x)
                (do
                  (aset links i (aget-forward x i))
                  (recur (inc i)))
                (recur (inc i))))))
        (while (and (> level 0)
                    (nil? (aget-forward header level)))
          (set! level (dec level))))))

  (ceilingEntry [coll k]
    (loop [x header level level]
      (if-not (neg? level)
        (let [nx (loop [x x]
                   (let [^SkipListNode x' (aget-forward x level)]
                     (when-not (nil? x')
                       (if (>= (.-key x') k)
                         x'
                         (recur x')))))]
          (if-not (nil? nx)
            (recur nx (dec level))
            (recur x (dec level))))
        (when-not (identical? x header)
          x))))
  
  (floorEntry [coll k]
    (loop [x header level level]
      (if-not (neg? level)
        (let [nx (loop [x x]
                   (let [^SkipListNode x' (aget-forward x level)]
                     (if-not (nil? x')
                       (if (> (.-key x') k)
                         x
                         (recur x'))
                       (when (zero? level)
                         x))))]
          (if nx
            (recur nx (dec level))
            (recur x (dec level))))
        (when-not (identical? x header)
          x))))

  Seqable                                                       ;;; ISeqable
  (seq [coll]                                                   ;;; -seq
    (letfn [(iter [node]
              (lazy-seq
                (when-not (nil? node)
                  (cons [(.-key ^SkipListNode node) (.-val ^SkipListNode node)]
                    (iter (aget-forward node 0))))))]
        (iter (aget-forward header 0))))

  ;;;IPrintWithWriter
  ;;;(-pr-writer [coll writer opts]
  ;;;  (let [pr-pair (fn [keyval]
  ;;;                  (pr-sequential-writer writer pr-writer "" " " "" opts keyval))]
  ;;;    (pr-sequential-writer writer pr-pair "{" ", " "}" opts coll)))
	  )

(defn skip-list []
  (SkipList. (skip-list-node 0) 0))

;;; End Skip-list impl
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  
  
(defonce ^:private ^DelayQueue timeouts-queue
  (DelayQueue.))

;;;(defonce ^:private ^ConcurrentSkipListMap timeouts-map
;;;  (ConcurrentSkipListMap.))

(defonce ^:private ^SkipList timeouts-map (skip-list))
  
(def ^:const TIMEOUT_RESOLUTION_MS 10)

;;; ADDED
(defn current-time-millis []
  (quot (.Ticks (DateTime/Now)) 10000))

(deftype TimeoutQueueEntry [channel ^long timestamp]
  IDelayed                                                 ;;; Delayed
  (GetRemainingDelay [this]                                ;;; getDelay [this time-unit]   
      (TimeSpan. (- timestamp (current-time-millis))))     ;;; (.convert time-unit
                                                           ;;;    (- timestamp (System/currentTimeMillis))
                                                           ;;;    TimeUnit/MILLISECONDS))
  (CompareTo							                   ;;; compareTo
   [this other]
   (let [ostamp (.-timestamp other)]                       ;;; if we hint to TimeoutQueueEntry to avoid reflection, .-timestamp gives zero. ???
     (if (< timestamp ostamp)
       -1
       (if (= timestamp ostamp)
         0
         1))))
  Object
  (ToString [this]
    (str "TQE: " timestamp))
  impl/Channel
  (close! [this]
    (impl/close! channel)))

(defn timeout
  "returns a channel that will close after msecs"
  [msecs]
  (let [timeout (+ (current-time-millis) msecs)                                  ;;;  (System/currentTimeMillis)
        ^SkipListNode me (.ceilingEntry timeouts-map timeout)]                   ;;;  Added type hint
    (or (when (and me (< (.-key me) (+ timeout TIMEOUT_RESOLUTION_MS)))          ;;;  .getKey
          (.channel ^TimeoutQueueEntry (.-val me)))                              ;;; .getValue
        (let [timeout-channel (channels/chan nil)
              timeout-entry (TimeoutQueueEntry. timeout-channel timeout)]
          (.put timeouts-map timeout timeout-entry)
          (.put timeouts-queue timeout-entry)
          timeout-channel))))

(defn- timeout-worker
  []
  (let [q timeouts-queue]
    (loop []
      (let [^TimeoutQueueEntry tqe (.take q)]
        (.remove ^ISkipList timeouts-map (.timestamp tqe))
        (impl/close! tqe))
      (recur))))

(defonce timeout-daemon
  (let [^System.Threading.ThreadStart start                                    ;;; DM:Added
        (gen-delegate System.Threading.ThreadStart [] (timeout-worker))]       ;;; DM:Added
    (doto (System.Threading.Thread. start)                                     ;;;(Thread. ^Runnable timeout-worker "clojure.core.async.timers/timeout-daemon")
      (.set_Name "clojure.core.async.timers/timeout-daemon")                   ;;; DM:Added
      (.set_IsBackground true)                                                                         ;;;(.setDaemon true)
      (.Start))))                                                                                      ;;;
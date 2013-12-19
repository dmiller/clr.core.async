;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;;  Ported to ClojureCLR by David Miller
(ns ^{:skip-wiki true}
  clojure.core.async.impl.channels
  (:require [clojure.core.async.impl.protocols :as impl]
            [clojure.core.async.impl.dispatch :as dispatch]
            [clojure.core.async.impl.mutex :as mutex])
  (:import  [System.Collections.Generic |LinkedList`1[System.Object]|]          ;;; [java.util LinkedList Queue Iterator]
            [clojure.core.async.impl.mutex ILock]))                             ;;; [java.util.concurrent.locks Lock]

(set! *warn-on-reflection* true)

(defmacro assert-unlock [lock test msg]
  `(when-not ~test
     (.unlock ~lock)
     (throw (new Exception (str "Assert failed: " ~msg "\n" (pr-str '~test))))))       ;;;  AssertionError

(defn box [val]
  (reify clojure.lang.IDeref
         (deref [_] val)))

(defprotocol MMC
  (cleanup [_]))

;;;  JVM version uses java.util.Queue.  j.u.Queue supports deletions.  System.Collections.Generic.Queue<T> does not.
;;;  So I'm using System.Collections.Generic.LinkedList<Object> instead.

   
(deftype ManyToManyChannel [^|LinkedList`1| takes ^|LinkedList`1| puts ^|LinkedList`1| buf closed ^ILock mutex]   ;;; ^Queue ^Lock
  MMC
  (cleanup
   [_]
;;;   (when-not (.isEmpty takes)
;;;     (let [iter (.iterator takes)]
;;;       (loop [taker (.next iter)]
;;;         (when-not (impl/active? taker)
;;;           (.remove iter))
;;;         (when (.hasNext iter)
;;;           (recur (.next iter))))))
;;;   (when-not (.isEmpty puts)
;;;     (let [iter (.iterator puts)]
;;;       (loop [[putter] (.next iter)]
;;;         (when-not (impl/active? putter)
;;;           (.remove iter))
;;;         (when (.hasNext iter)
;;;           (recur (.next iter))))))
  (when-not (zero? (.Count takes))
    (loop [node (.First takes)] 
	  (let [next (.Next node)]
	    (when-not (impl/active? (.Value node))
		   (.Remove takes node))
		(when next
		   (recur next)))))
  (when-not (zero? (.Count puts))
    (loop [node (.First puts)] 
	  (let [next (.Next node)]
	    (when-not (impl/active? (first (.Value node)))
		   (.Remove puts node))
		(when next
		   (recur next))))))
  impl/WritePort
  (put!
   [this val handler]
   (when (nil? val)
     (throw (ArgumentNullException. "Can't put nil on channel")))          ;;; IllegalArgumentException
   (.lock mutex)
   (cleanup this)
   (if @closed
     (do (.unlock mutex)
         (box nil))
     (let [^ILock handler handler                                          ;;; ^Lock
;;;           iter (.iterator takes)
;;;           [put-cb take-cb] (when (.hasNext iter)
;;;                              (loop [^Lock taker (.next iter)]
;;;                               (if (< (impl/lock-id handler) (impl/lock-id taker))
;;;                                  (do (.lock handler) (.lock taker))
;;;                                  (do (.lock taker) (.lock handler)))
;;;                                (let [ret (when (and (impl/active? handler) (impl/active? taker))
;;;                                            [(impl/commit handler) (impl/commit taker)])]
;;;                                  (.unlock handler)
;;;                                  (.unlock taker)
;;;                                  (if ret
;;;                                    (do
;;;                                      (.remove iter)
;;;                                      ret)
;;;                                    (when (.hasNext iter)
;;;                                      (recur (.next iter)))))))
          [put-cb take-cb] (when-not (zero? (.Count takes))
		                     (loop [node (.First takes)]
							   (let [^ILock taker (.Value node)]
							     (if (< (impl/lock-id handler) (impl/lock-id taker))
								   (do (.lock handler) (.lock taker))
								   (do (.lock taker) (.lock handler)))
								 (let [ret (when (and (impl/active? handler) (impl/active? taker))
								              [(impl/commit handler) (impl/commit taker)])]
							       (.unlock handler)
								   (.unlock taker)
								   (if ret
								      (do (.Remove takes node) ret)
									  (when-let [next (.Next node)]
									      (recur next)))))))]
       (if (and put-cb take-cb)
         (do
           (.unlock mutex)
           (dispatch/run (fn [] (take-cb val)))
           (box nil))
         (if (and buf (not (impl/full? buf)))
           (do
             (.lock handler)
             (let [put-cb (and (impl/active? handler) (impl/commit handler))]
               (.unlock handler)
               (if put-cb
                 (do (impl/add! buf val)
                     (.unlock mutex)
                     (box nil))
                 (do (.unlock mutex)
                     nil))))
           (do
             (when (impl/active? handler)
               (assert-unlock mutex
                               (< (.Count puts) impl/MAX-QUEUE-SIZE)                         ;;; .size
                               (str "No more than " impl/MAX-QUEUE-SIZE
                                    " pending puts are allowed on a single channel."
                                    " Consider using a windowed buffer."))			 
               (.AddLast puts [handler val]))                                                ;;; .add
             (.unlock mutex)
             nil))))))
  
  impl/ReadPort
  (take!
   [this handler]
   (.lock mutex)
   (cleanup this)
   (let [^ILock handler handler                                                 ;;; ^Lock
         commit-handler (fn []
                          (.lock handler)
                          (let [take-cb (and (impl/active? handler) (impl/commit handler))]
                            (.unlock handler)
                            take-cb))]
     (if (and buf (pos? (count buf)))
       (do
         (if-let [take-cb (commit-handler)]
           (let [val (impl/remove! buf)
;;;                 iter (.iterator puts)
;;;                 cb (when (.hasNext iter)
;;;                      (loop [[^Lock putter val] (.next iter)]
;;;                        (.lock putter)
;;;                        (let [cb (and (impl/active? putter) (impl/commit putter))]
;;;                          (.unlock putter)
;;;                          (.remove iter)
;;;                          (if cb
;;;                            (do (impl/add! buf val)
;;;                                cb)
;;;                            (when (.hasNext iter)
;;;                              (recur (.next iter)))))))
                 cb (when-not (zero? (.Count puts))
                       (loop [node (.First puts)]
                          (let [[^ILock putter val] (.Value node)
						        next (.Next node)]
                            (.lock putter)
                            (let [cb (and (impl/active? putter) (impl/commit putter))]
							  (.unlock putter)
							  (.Remove puts node)
							  (if cb
							    (do (impl/add! buf val) cb)
								(when next
								    (recur next)))))))]
             (.unlock mutex)
             (when cb
               (dispatch/run cb))
             (box val))
           (do (.unlock mutex)
               nil)))
;;;       (let [iter (.iterator puts)
;;;             [take-cb put-cb val]
;;;             (when (.hasNext iter)
;;;               (loop [[^Lock putter val] (.next iter)]
;;;                 (if (< (impl/lock-id handler) (impl/lock-id putter))
;;;                   (do (.lock handler) (.lock putter))
;;;                   (do (.lock putter) (.lock handler)))
;;;                 (let [ret (when (and (impl/active? handler) (impl/active? putter))
;;;                             [(impl/commit handler) (impl/commit putter) val])]
;;;                   (.unlock handler)
;;;                   (.unlock putter)
;;;                   (if ret
;;;                     (do
;;;                       (.remove iter)
;;;                       ret)
;;;                     (when-not (impl/active? putter)
;;;                       (.remove iter)
;;;                       (when (.hasNext iter)
;;;                         (recur (.next iter))))))))]
         (let [[take-cb put-cb val]
		       (when-not (zero? (.Count puts))
			     (loop [node (.First puts)]
				   (let [[^ILock putter val] (.Value node)
				         next (.Next node)]
				     (if (< (impl/lock-id handler) (impl/lock-id putter))
					   (do (.lock handler) (.lock putter))
					   (do (.lock putter) (.lock handler)))
					 (let [ret (when (and (impl/active? handler) (impl/active? putter))
					              [(impl/commit handler) (impl/commit putter) val])]
					   (.unlock handler)
					   (.unlock putter)
					   (if ret
					     (do (.Remove puts node) ret)
						 (when-not (impl/active? putter)
						    (.Remove puts node)
							(when next
							   (recur next))))))))]
         (if (and put-cb take-cb)
           (do
             (.unlock mutex)
             (dispatch/run put-cb)
             (box val))
           (if @closed
             (do
               (.unlock mutex)
               (if-let [take-cb (commit-handler)]
                 (box nil)
                 nil))
             (do
                (assert-unlock mutex
                               (< (.Count takes) impl/MAX-QUEUE-SIZE)                        ;;; .size
                               (str "No more than " impl/MAX-QUEUE-SIZE
                                    " pending takes are allowed on a single channel."))
			   (.AddLast takes handler)                                                      ;;; .add
               (.unlock mutex)
               nil)))))))

  impl/Channel
  (close!
   [this]
   (.lock mutex)
   (cleanup this)
   (if @closed
     (do
       (.unlock mutex)
       nil)
     (do
       (reset! closed true)
;;;       (let [iter (.iterator takes)]
;;;         (when (.hasNext iter)
;;;           (loop [^Lock taker (.next iter)]
;;;             (.lock taker)
;;;             (let [take-cb (and (impl/active? taker) (impl/commit taker))]
;;;               (.unlock taker)
;;;               (when take-cb
;;;                 (dispatch/run (fn [] (take-cb nil))))
;;;               (when (.hasNext iter)
;;;                 (recur (.next iter)))))))
       (when-not (zero? (.Count takes))
	     (loop [node (.First takes)]
		   (let [^ILock taker (.Value node)
		         next (.Next node)]
		     (.lock taker)
			 (let [take-cb (and (impl/active? taker) (impl/commit taker))]
			   (.unlock taker)
			   (when take-cb
			      (dispatch/run (fn [] (take-cb nil))))
			   (when next
			     (recur next))))))
       (.unlock mutex)
       nil))))

(defn chan [buf]
 (ManyToManyChannel. (|LinkedList`1|.) (|LinkedList`1|.) buf (atom nil) (mutex/mutex)))       ;;; LinkedList.

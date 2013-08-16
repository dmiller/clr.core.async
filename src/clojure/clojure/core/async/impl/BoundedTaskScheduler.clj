;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;;   Author: David Miller

;;   A Clojure implementation (no-thought translation) of LimitedConcurrencyTaskScheduler
;;      from http://code.msdn.microsoft.com/Samples-for-Parallel-b4b76364/sourcecode?fileId=44488&pathId=1111204181

;;   I had hoped to do this with a proxy, but the need to access 
;;   the protected method TryExecuteTask in the base class forced this 
;;   to be a gen-class.

(ns clojure.core.async.impl.BoundedTaskScheduler
    (:gen-class
	  :main false
	  :state state
	  :extends TaskScheduler
	  :constructors []
	  :init init
	  :exposes-methods {TryExecuteTask try-execute-task})	  
    (:import [System.Threading.Tasks TaskScheduler Task]
	         [System.Threading Monitor ThreadPool WaitCallback]))

(set! *warn-on-reflection* true)
	
	
(def ^:private ^|System.Threading.ThreadLocal`1[System.Boolean]| current-thread-processing-flag 
  (|System.Threading.ThreadLocal`1[System.Boolean]|.))
	
(defn- current-thread-processing-items? []
  (.get_Value current-thread-processing-flag))	
  
(defn- set-current-thread-processing-items! [value]
  (.set_Value current-thread-processing-flag value))

(deftype LctsState [^long max-degree 
                    ^|System.Collections.Generic.LinkedList`1[System.Threading.Tasks.Task]| tasks 
					^long delegates-queued-or-running])

 
(defn init [max-degree]
  (when (< max-degree 1)
    (throw (ArgumentOutOfRangeException. "maxDegreeOfParallelism")))
  [[]
   (LctsState. max-degree
               (|System.Collections.Generic.LinkedList`1[System.Threading.Tasks.Task]|. max-degree)
			   (atom 0))])


(defn update-delegates-queued-or-running [this f]
  (swap! (-> this .state .delegates-queued-or-running) f))
  
  
(defn notify-thread-pool [this]
  (set-current-thread-processing-items! true)
  (try 
    ;; process all items in queue
	(loop []
	  (let [tasks (.tasks this)
  	        ^Task item
          	  (locking tasks
				(if (zero? (.Count tasks))
				  (do (update-delegates-queued-or-running this dec)
				      nil)
				(let [t1 (-> tasks (.First) (.Value))]
				  (.RemoveFirst tasks)
				  t1)))]
	    (when item
	      (.TryExecuteTask this item)
		  (recur))))
	(finally 
	  (set-current-thread-processing-items! false))))
 
  
(defn -GetScheduledTasks [this]  
  (let [^Boolean taken? false
        tasks (.tasks this)]
    (try 
	  (Monitor/TryEnter tasks (by-ref taken?))
	  (if taken?
		(System.Linq.Enumerable/ToArray (type-args Task) tasks)
		(throw (NotSupportedException.)))
	  (finally
        (when taken?
		  (Monitor/Exit tasks))))))

(defn- num-delegates-queued-or-running [instance]
   (-> instance (.state) (.delegates-queued-or-running)))
   
;; Add the task to the list of tasks to be processed.  If there aren't enough
;; delegates currently queued or running to process tasks, schedule another.
(defn -QueueTask [this ^Task task]
  (let [tasks (.tasks this)]
    (locking tasks
	  (.AddLast tasks task)
	  (when (< (long (num-delegates-queued-or-running this)) (.max-degree (.state this)))
  	    (update-delegates-queued-or-running this inc)
        (ThreadPool/UnsafeQueueUserWorkItem
          (gen-delegate WaitCallback [^Object o]
	  	   (notify-thread-pool this))
	  	  nil)))))
	    
							
(defn -TryExecuteTaskInline [this ^Task task previously-queued?] 
  ;;If this thread isn't already processing a task, we don't support inlining
  (when (current-thread-processing-items?)
    ;; If the task was previously queued, remove it from the queue
	(when previously-queued?
	   (.TryDequeue this task))
	;; Try to run task
	(.TryExecuteTask this task)))
    
  
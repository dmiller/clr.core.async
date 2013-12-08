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

(ns clojure.core.async.util.BoundedTaskScheduler
  (:import [System.Threading.Tasks TaskScheduler Task]
	         [System.Threading Monitor ThreadPool WaitCallback])
  (:gen-class
	  :main false
	  :state state
	  :extends System.Threading.Tasks.TaskScheduler
	  :constructors {[Int64][]}
	  :init init
	  :exposes-methods {TryExecuteTask try-execute-task})	  
 )

(set! *warn-on-reflection* true)
	
	
;; Thread-local flag indicating processing
	
(def 
  ^{:private true 
    :doc "Whether the current thread is processing work items.  Type is ThreadLocal<Boolean>"}
  ^|System.Threading.ThreadLocal`1[System.Boolean]| current-thread-processing-flag 
  (|System.Threading.ThreadLocal`1[System.Boolean]|.))
	
(defn- current-thread-processing-items? 
  "Determine if the current thread is processing items"
  []
  (.get_Value current-thread-processing-flag))	
  
(defn- set-current-thread-processing-items!
  "Set to true to indicate that the current thread is processing items."
  [value]
  (.set_Value current-thread-processing-flag value))

  
;; The state of a BoundedTaskScheduler  
  
(deftype BtsState [^long max-degree 
                   ^|System.Collections.Generic.LinkedList`1[System.Threading.Tasks.Task]| tasks 
				   ^clojure.lang.Atom delegates-queued-or-running])

 
(defn -init [max-degree]
  (when (< max-degree 1)
    (throw (ArgumentOutOfRangeException. "maxDegreeOfParallelism")))
  [[]
   (BtsState. max-degree
               (|System.Collections.Generic.LinkedList`1[System.Threading.Tasks.Task]|.)
			   (atom 0))])

(defn- max-degree 
  "The maximum degree of concurrency"
  [this]
  (-> this .state .max-degree))
  
(defn- tasks
  "The tasks scheduled in this scheduler.  Should be manipulated when locked only."
  [this]
  (-> this .state .tasks))
  
(defn- num-delegates-queued-or-running
  "The number of delegates queued or running"
  [this]
  @(-> this .state .delegates-queued-or-running))
  
(defn- update-delegate-count! 
  "Atomic updated of delegate count"
  [this f]
  (swap! (-> this .state .delegates-queued-or-running) f))
  
(defn- inc-delegate-count! 
   "Increment delegate count"
   [this]
   (update-delegate-count! this inc))

(defn- dec-delegate-count! 
  "Decrement delegate count"
  [this]
  (update-delegate-count! this dec))
  
  
;; Low-level thread pool management  
  
(defn- notify-thread-pool-worker [this]
  ;; Set flag that the current thread is now processing work items.
  ;; This is necessary to enable inlining of tasks into this thread.
  (set-current-thread-processing-items! true)

  (try 
    ;; process all available items in queue
	(loop []
	  (let [ts (tasks this)
  	        ^Task item
          	  (locking ts
			    ;; When there are no more items to be processed,
				;; note that we are done processing and yield no item
				;; else get (and remove) first item.
				(if (zero? (.Count ts))
				  (do (dec-delegate-count! this)
				      nil)
				(let [t1 (-> ts (.First) (.Value))]
				  (.RemoveFirst ts)
				  t1)))]
	    (when item
	      (.TryExecuteTask this item)
		  (recur))))
	(finally 
	  (set-current-thread-processing-items! false))))
 
(defn- notify-thread-pool-of-pending-work
  "Inform the thread pool that there's work to be executed for this scheduler."
  [this]
  (ThreadPool/UnsafeQueueUserWorkItem
    (gen-delegate WaitCallback [^Object o]
	  (notify-thread-pool-worker this))
  nil))
 
 
;; public method overrides  

   

(defn -QueueTask 
  "Queue a task to the scheduler.
  
  If there aren't enough delegates currently queued or running to process tasks,
  schedule another."
  [this ^Task task]
  (let [ts (tasks this)]
    (locking ts
	  (.AddLast ts task)
	  (when (< (long (num-delegates-queued-or-running this)) (max-degree this))
  	    (inc-delegate-count! this)
        (notify-thread-pool-of-pending-work this)))))

						
(defn -TryExecuteTaskInline 
  "Attempt to execute the specified task on the current thread.
  
  Returns true if the task can be executed on the current thread"
  [this ^Task task previously-queued?] 
  ;;If this thread isn't already processing a task, we don't support inlining
  (when (current-thread-processing-items?)
    ;; If the task was previously queued, remove it from the queue
	(when previously-queued?
	  (.TryDequeue this task))
	 ;; Try to run task
	(.TryExecuteTask this task)))


(defn -TryDequeue 
  "Attempt to remove a previously scheduled task from the scheduler"
  [this ^Task task]
  (let [ts (tasks this)]
    (locking ts
        (.Remove ts task))))
	

(defn -get_MaximumConcurrencyLevel [this]
  (int (max-degree this)))

(defn -GetScheduledTasks [this]  
  (let [^Boolean taken? false
        ts (tasks this)]
    (try 
	  (Monitor/TryEnter ts (by-ref taken?))
	  (if taken?
		(System.Linq.Enumerable/ToArray (type-args Task) ts)
		(throw (NotSupportedException.)))
	  (finally
        (when taken?
		  (Monitor/Exit ts))))))

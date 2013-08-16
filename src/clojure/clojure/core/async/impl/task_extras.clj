;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;;   Author: David Miller


(ns clojure.core.async.impl.task-extras
    (:import [System.Threading.Tasks TaskScheduler Task]
	         [System.Threading Monitor ThreadPool WaitCallback]))

(set! *warn-on-reflection* true)	
	
;;   A Clojure implementation (no-thought translation) of LimitedConcurrencyTaskScheduler
;;      from http://code.msdn.microsoft.com/Samples-for-Parallel-b4b76364/sourcecode?fileId=44488&pathId=1111204181
	
(def ^:private ^|System.Threading.ThreadLocal`1[System.Boolean]| current-thread-processing-flag 
  (|System.Threading.ThreadLocal`1[System.Boolean]|.))
	
(defn- current-thread-processing-items? []
  (.get_Value current-thread-processing-flag))	
  
(defn- set-current-thread-processing-items! [value]
  (.set_Value current-thread-processing-flag value))

  
  
(defn limited-concurrency-task-scheduler [^long max-degree]
  (when (< max-degree 1)
    (throw (ArgumentOutOfRangeException. "maxDegreeOfParallelism")))
  (let [tasks (|System.Collections.Generic.LinkedList`1[System.Threading.Tasks.Task]|.)
        delegates-queued-or-running (atom 0)]
   (proxy [TaskScheduler] []
      (GetScheduledTasks []  
	    (let [^Boolean taken? false]
		  (try 
			(Monitor/TryEnter tasks (by-ref taken?))
		    (if taken?
		      (System.Linq.Enumerable/ToArray (type-args Task) tasks)
			  (throw (NotSupportedException.)))
			(finally
			  (when taken?
			     (Monitor/Exit tasks))))))
      (TryExecuteTask [^Task task]
	    (proxy-super TryExecuteTask task))
			
      (QueueTask [^Task task]
	     ;; Add the task to the list of tasks to be processed.  If there aren't enough
         ;; delegates currently queued or running to process tasks, schedule another.
		 (locking tasks
		   (.AddLast tasks task)
		   (when (< (long @delegates-queued-or-running) max-degree)
		     (swap! delegates-queued-or-running inc)
			 (let [try-execute-task (fn [^Task task] (.TryExecuteTask this task))]
               (ThreadPool/UnsafeQueueUserWorkItem
                 (gen-delegate WaitCallback [^Object o]
	               (set-current-thread-processing-items! true)
	               (try 
				      ;; process all items in queue
				      (loop []
				  	    (let [^Task item
          		  			  (locking tasks
					            (if (zero? (.Count tasks))
						          (do (swap! delegates-queued-or-running dec)
								      nil)
								  (let [t1 (-> tasks (.First) (.Value))]
								    (.RemoveFirst tasks)
								    t1)))]
						  (when item
						    (try-execute-task item)
							(recur))))
					(finally 
					  (set-current-thread-processing-items! false))))
				 nil)))))
							
						 
      (TryExecuteTaskInline [^Task task previously-queued?] ))))
    
  
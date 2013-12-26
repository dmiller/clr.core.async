(ns clojure.core.async.util.bts
  (:import [System.Threading.Tasks TaskScheduler Task]
	       [System.Threading Monitor ThreadPool WaitCallback])
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

(defn ^System.Threading.Tasks.TaskScheduler bounded-task-scheduler [max-degree]
  (when (< max-degree 1)
    (throw (ArgumentOutOfRangeException. "maxDegreeOfParallelism")))
  (let [tasks (|System.Collections.Generic.LinkedList`1[System.Threading.Tasks.Task]|.)
        num-delegates (atom 0)
		update-delegate-count! (fn [f] (swap! num-delegates f))
		inc-delegate-count! #(update-delegate-count! inc)
		dec-delegate-count! #(update-delegate-count! dec)]
	(proxy [System.Threading.Tasks.TaskScheduler] []
	  (TryExecuteTaskInline 
         ;; Attempt to execute the specified task on the current thread.
         ;; Returns true if the task can be executed on the current thread
        [^Task task previously-queued?] 
  	    ;;If this thread isn't already processing a task, we don't support inlining
        (when (current-thread-processing-items?)
          ;; If the task was previously queued, remove it from the queue
	     (when previously-queued?
	       (.TryDequeue this task))
	     ;; Try to run task
	     (.TryExecuteTask this task)))
		 
      (TryDequeue 
        ;; Attempt to remove a previously scheduled task from the scheduler
        [^Task task]
        (locking tasks
          (.Remove tasks task)))
		  
      (get_MaximumConcurrencyLevel []
  	    (int max-degree))
		
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
			  
      (QueueTask 
        ;; Queue a task to the scheduler.
        ;;  If there aren't enough delegates currently queued or running to process tasks,
        ;;  schedule another.
        [^Task task]
		(let [notify-thread-pool-worker (fn []
		       ;; Set flag that the current thread is now processing work items.
		       ;; This is necessary to enable inlining of tasks into this thread.
		      (set-current-thread-processing-items! true)
		      (try
                ;; process all available items in queue
			    (loop []
			      (let [^Task item
			            (locking tasks
			              ;; When there are no more items to be processed,
				          ;; note that we are done processing and yield no item
				          ;; else get (and remove) first item.
				          (if (zero? (.Count tasks))
				            (do (dec-delegate-count!)
				                nil)
		    		        (let [t1 (-> tasks (.First) (.Value))]
				              (.RemoveFirst tasks)
				              t1)))]
                    (when item
                      (.TryExecuteTask this item)
		              (recur))))
              (finally 
                (set-current-thread-processing-items! false))))]
        (locking tasks
	      (.AddLast tasks task)
	      (when (< (long @num-delegates) (long max-degree))
  		    (inc-delegate-count!)
			(ThreadPool/UnsafeQueueUserWorkItem
              (gen-delegate WaitCallback [^Object o]
	              (notify-thread-pool-worker))
  		      nil))))))))

						

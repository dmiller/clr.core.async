;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;;  Ported to ClojureCLR by David Miller
(assembly-load-from "clojure.core.async.util.BoundedTaskScheduler.dll")
(ns ^{:skip-wiki true}
  clojure.core.async.impl.dispatch
  (:require [clojure.core.async.impl.protocols :as impl]
			[clojure.core.async.impl.concurrent :as conc])                  ;;; DM:Added  
  (:import [clojure.core.async.util BoundedTaskScheduler]                   ;;; DM:Added
           [System.Threading.Tasks             TaskFactory])                ;;; DM:Added
            )                                                ;;; [clojure.core.async.impl.exec.threadpool :as tp]

(set! *warn-on-reflection* true)

;;; We don't have fun thread-pool goodies found in the JVM.
;;; I have not ported impl/exec/threadpool -- I'll just do the work of creating the custom scheduler here.
;;; I have not ported impl/concurrent, except for the processor count 
;;;     -- I don't see a hook in the CLR to modify the thread names in the pool.


;;;(def executor (delay (tp/thread-pool-executor)))

;;;(defn run
;;;  "Runs Runnable r in a thread pool thread"
;;;  [^Runnable r]
;;;  (impl/exec @executor r))

(def task-factory (delay (TaskFactory. (BoundedTaskScheduler.  (+ 2 conc/processors)))))

(defn run
  "Runs fn in thread pool thread"
  [^clojure.lang.IFn f]
  (impl/exec @task-factory f))

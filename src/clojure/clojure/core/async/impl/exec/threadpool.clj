;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;;  Ported to ClojureCLR by David Miller

(assembly-load-from "clojure.core.async.util.BoundedTaskScheduler.dll")

(ns clojure.core.async.impl.exec.threadpool
  (:require [clojure.core.async.impl.protocols :as impl]
            [clojure.core.async.impl.concurrent :as conc])
  (:import  [clojure.core.async.util BoundedTaskScheduler]      ;;; DM:Added
            [System.Threading.Tasks TaskFactory]))              ;;; [java.util.concurrent Executors Executor]

(set! *warn-on-reflection* true)

;;;(defonce the-executor
;;;  (Executors/newFixedThreadPool
;;;   (-> (Runtime/getRuntime)
;;;       (.availableProcessors)
;;;       (* 2)
;;;       (+ 42))
;;;   (conc/counted-thread-factory "async-dispatch-%d" true)))

;;;(defn thread-pool-executor
;;;  ([] (thread-pool-executor the-executor))
;;;  ([^Executor executor-svc]
;;;     (reify impl/Executor
;;;       (impl/exec [this r]
;;;         (.execute executor-svc ^Runnable r)))))

(defonce the-scheduler  (TaskFactory. (BoundedTaskScheduler.  (+ 2 conc/processors))))

(defn bounded-task-executor
  ([] (bounded-task-executor the-scheduler))
  ([^TaskFactory tf]
     (reify impl/Executor
	    (impl/exec [this r]
		   (.StartNew tf ^System.Action (gen-delegate System.Action [] r))))))

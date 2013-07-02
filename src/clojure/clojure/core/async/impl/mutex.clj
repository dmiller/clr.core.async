;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;;  Ported to ClojureCLR by David Miller.
(ns ^{:skip-wiki true}
  clojure.core.async.impl.mutex
                     ;;;(:require [clojure.core.async.impl.protocols :as impl])
                     ;;; (:import [clojure.core.async Mutex]
  )                  ;;;          [java.util.concurrent.locks Lock])

;;;#_(defn mutex []
;;;  (let [m (Mutex.)]
;;;    (reify
;;;    Lock
;;;     (lock [_] (.lock m))
;;;     (unlock [_] (.unlock m)))))

;;; DM: Added
(definterface ILock
  (lock [])
  (unlock []))

(defn mutex []
  (let [cas (clojure.lang.AtomicInteger.)]            ;;; java.util.concurrent.atomic.AtomicInteger.
    (reify
     ILock                                            ;;; Lock
     (lock [_] (loop [got (.compareAndSet cas 0 1)]
                 (if got
                   nil
                   (recur (.compareAndSet cas 0 1)))))
     (unlock [_] (.set cas 0)))))
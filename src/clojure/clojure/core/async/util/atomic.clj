 ;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;;   Author: David Miller

;; Trivial implementation of java.util.concurrent.atomic.AtomicReferenceArray, really only need get/set
;; See https://code.google.com/p/netconcurrent/source/browse/trunk/src/Spring/Spring.Threading/Threading/AtomicTypes/AtomicReferenceArray.cs 
;; for a more idiomatic implementation.
;; Or see https://github.com/henon/GitSharp/blob/master/GitSharp.Core/Util/AtomicReferenceArray.cs

(ns clojure.core.async.util.atomic)


(set! *warn-on-reflection* true)

(definterface IAtomicArray
   (count [] "The size of the array")
   (get [i] "Get the i-th element")
   (set [i v] "Set the i-th element")
   (cas [i expected v] "Compare against expected and set to v if equal"))
   
(deftype AtomicRefArray [^|System.Object[]| a]
  IAtomicArray
  (count [this] (.Length a))
  (get [this i] (locking this (.Get a i)))
  (set [this i v] (locking this (.Set a i v)))
  (cas [this i expected v] 
    (locking this
	  (let [current (.Get a i)]
	    (if (or (and (nil? current) (nil? expected))
		        (and (not (nil? current)) (= current expected)))
		  (do (.Set a i v)
		      true)
		  false)))))
		  
 (defn atomic-ref-array [length]
   (AtomicRefArray. (object-array length)))
   

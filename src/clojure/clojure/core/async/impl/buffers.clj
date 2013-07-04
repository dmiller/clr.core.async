;;   Copyright (c) Rich Hickey and contributors. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.
;;;  Ported to ClojureCLR by David Miller.
(ns ^{:skip-wiki true} 
  clojure.core.async.impl.buffers
  (:require [clojure.core.async.impl.protocols :as impl])
  )                                                       ;;;(:import [java.util LinkedList Queue])

(set! *warn-on-reflection* true)

(deftype FixedBuffer [^|System.Collections.Generic.LinkedList`1[System.Object]| buf ^long n]       ;;; LinkedList
  impl/ABuffer
  (full? [this]
    (= (.Count buf) n))                                            ;;; .size
  (remove! [this]
    (let [x (.Value (.Last buf))] (.RemoveLast buf) x))                     ;;; .removeLast -- added let
  (add! [this itm]
    (assert (not (impl/full? this)) "Can't add to a full buffer")
    (.AddFirst buf itm))                                           ;;;  addFirst
  clojure.lang.Counted
  (count [this]
    (.Count buf)))                                                 ;;; .size

(defn fixed-buffer [^long n]
  (FixedBuffer. (|System.Collections.Generic.LinkedList`1[System.Object]|.) n))                    ;;; LinkedList.


(deftype DroppingBuffer [^|System.Collections.Generic.LinkedList`1[System.Object]| buf ^long n]    ;;; LinkedList
  impl/ABuffer
  (full? [this]
    false)
  (remove! [this]
    (let [x (.Value (.Last buf))] (.RemoveLast buf) x))                   ;;; .removeLast -- added let
  (add! [this itm]
    (when-not (= (.Count buf) n)                                 ;;; .size
      (.AddFirst buf itm)))                                      ;;; .addFirst
  clojure.lang.Counted
  (count [this]
    (.Count buf)))                                               ;;; .size

(defn dropping-buffer [n]
  (DroppingBuffer. (|System.Collections.Generic.LinkedList`1[System.Object]|.) n))                  ;;; LinkedList.

(deftype SlidingBuffer [^|System.Collections.Generic.LinkedList`1[System.Object]| buf ^long n]      ;;; LinkedList
  impl/ABuffer
  (full? [this]
    false)
  (remove! [this]
    (let [x (.Value (.Last buf))] (.RemoveLast buf) x))                   ;;; .removeLast -- added let
  (add! [this itm]
    (when (= (.Count buf) n)                                     ;;; .size
      (impl/remove! this))
    (.AddFirst buf itm))                                         ;;; .addFirst
  clojure.lang.Counted
  (count [this]
    (.Count buf)))                                               ;;; .size

(defn sliding-buffer [n]
  (SlidingBuffer. (|System.Collections.Generic.LinkedList`1[System.Object]|.) n))                 ;;; LinkedList.
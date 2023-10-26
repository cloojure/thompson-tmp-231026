(ns demo.core
  (:use tupelo.core)
  (:require
    [clojure.math.combinatorics :as combo]
    [schema.core :as s]
    [tupelo.schema :as tsk]
    [tupelo.set :as set]
    ))

(def CoinSeq
  "A seq of available coin values; eg normal US coins are: [1 5 10 25] "
  [s/Int])

(def CoinPurse
  "A map from coin value to a count of that coin ; eg 44 cents is:
      {25 1
       10 1
       5  1
       1  4}"
  {s/Int s/Int})

(s/defn purse->total :- s/Int
  [purse :- CoinPurse]
  (reduce (fn [cum [coin N]]
            (+ cum (* coin N)))
          0
          purse))

(s/defn add-coin-to-purse
  [purse :- CoinPurse
   coin :- s/Int]
  (let [num (get purse coin 0)]
    (assoc purse coin (inc num))))

(def RecursiveCtx
  "The arguments for a recursive call.  Allows for testing just one recursive invocation"
  {:purse      CoinPurse ; current coin allocation
   :goal       s/Int ; the amount we wish the coins to sum up to
   :coin       (s/maybe s/Int) ; current coin to use (possibly nil)
   :coins-left CoinSeq ; coins not yet used
   })

(s/defn make-change-impl->ctx :- RecursiveCtx
  [ctx :- RecursiveCtx]
  (with-map-vals ctx [purse goal coin coins-left]
    ; decide if rescurse with current coin type of the next one
    (let [total-candidate (+ coin (purse->total purse))]
      (if (<= total-candidate goal)
        ; recurse with current coin type
        {:purse      (add-coin-to-purse purse coin)
         :goal       goal
         :coin       coin
         :coins-left coins-left}

        ; recurse with next coin type
        {:purse      purse
         :goal       goal
         :coin       (first coins-left) ; nil if no coins left => end of recursion
         :coins-left (rest coins-left)}))))

(s/defn make-change-impl :- (s/maybe CoinPurse)
  [ctx :- RecursiveCtx]
  (with-map-vals ctx [purse goal coin coins-left]
    (assert (= #{} (set/intersection (set (keys purse)) (set coins-left)))) ; sanity check

    (if (nil? coin)
      ; at end of recursion
      (do
        (assert (empty? coins-left)) ; sanity check
        (when (= goal (purse->total purse)) ; return nil if couldn't make correct change
          purse
          ))

      ; calculate next recursion ctx and invoke
      (let [ctx-next (make-change-impl->ctx ctx)]
        (make-change-impl ctx-next)))))

(s/defn make-change
  "Attempts to make change for integer `goal` given coin denominatinos in `coin-set`. If successful,
   returns a map from coin denomination to coin quantity like

        {25 1
         10 1
         5  1
         1  4}

  Returns `nil` on failure. "
  [goal :- s/Int
   coin-set :- #{s/Int}]
  ; (first (filter not-nil?))
  (let [
        ; We prepend the supplied set to force that usage in case the caller desires a specific order
        ; of coin priority (eg using an ordered-set). If that doesn't work, we try all other coin permutations
        ; in random order.
        coin-perms   (cons (vec coin-set)
                           (combo/permutations (vec coin-set)))
        purses-seq   (for [coin-perm coin-perms]
                       (let [first-coin (first coin-perm)
                             coins-left (rest coin-perm)
                             purse      (make-change-impl
                                          {:purse      {}
                                           :goal       goal
                                           :coin       first-coin
                                           :coins-left coins-left})]
                         purse))
        purse-result (first
                       (filter not-nil? purses-seq))]
    purse-result))


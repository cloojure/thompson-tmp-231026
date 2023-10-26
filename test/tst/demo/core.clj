(ns tst.demo.core
  (:use demo.core
        tupelo.core
        tupelo.test)
  (:require
    [flatland.ordered.set :refer [ordered-set]]
    [schema.core :as s]
    [tupelo.set :as set]
    ))

;  email to    dan@metabase.com


;;;; What we’re looking for:

;;; Working code. You should be able to run this from the command line or repl and have it work.
;;;
;;; We don’t really care which language you use, but we recommend something high level.
;;;
;;; We’re interested in the general style of code, how you use your editor, naming conventions etc.
;;;
;;; There are many ways to solve this, but we suggest using a simple approach, as it is not primarily an algorithmic
;;; exercise (though bonus points for knowing and being able to implement the best approaches).
;;;
;;; Also, just so you know we’re serious — the code _must_ work. Pseudo code or code with syntactic errors is an
;;; automatic 0.

;;;; The problem

;;; Given a number `x` and a sorted array of coins `coinset`, write a function that finds a combination of these coins
;;; that add up to X There are an infinite number of each coin. This is hopefully familiar to making change for a
;;; given amount of money in a currency, but it gets more interesting if we remove the 1 coin and have “wrong” coins
;;; in the coinset.
;;;
;;; Return a map (or dictionary or whatever it is called in your preferred programming language) such that each key is
;;; the coin, and each value is the number of times you need that coin. You need to only return a single solution, but
;;; for bonus points, return the one with the fewest number of coins. Don’t worry about performance or scalability for
;;; this problem.

;;;; A Specific example

;;; If x=7 and the coinset= [1,5,10,25], then the following are both solutions:
;;; `{1: 7}` since  7*1 = 7
;;; `{1: 2, 5: 1}` since 1*2 + 5*1=7

;;; # Some test cases for you to verify your approach works
;;; A. x = 6 coinset = [1,5,10,25]
;;; B. x = 6, coinset = [3,4]
;;; C. x = 6, coinset = [1,3,4]
;;; D. x = 6, coinset = [5,7]
;;; E. x = 16, coinset = [5,7,9]

(verify
  (is= 0 (purse->total {}))
  (is= 1 (purse->total {1 1}))
  (is= 5 (purse->total {5 1}))
  (is= 13 (purse->total {1 3
                         5 2}))
  (is= 34 (purse->total {1 3
                         5 2
                         7 3})))

(verify
  (is= {5 1} (add-coin-to-purse {} 5))
  (is= {5 2} (add-coin-to-purse {5 1} 5))
  (is= (add-coin-to-purse {5 1} 1)
       {1 1
        5 1})
  (is= (add-coin-to-purse {5 3} 25)
       {25 1
        5  3}))

(verify
  (is= #{} (set/intersection #{1 3 5} #{2 4})) ; verify syntax of set/intersection

  ; verify proper form of test
  (isnt= #{} (rest #{1}))
  (is= () (rest #{1}))
  (is (empty? #{}))
  (is= nil (first []))
  (is= nil (first (list))))

(verify
  ; terminate & return purse
  (is= (make-change-impl->ctx {:purse      {1 2 3 4}
                               :goal       99
                               :coin       1
                               :coins-left []})
       {:purse      {1 3 3 4}
        :goal       99
        :coin       1
        :coins-left []})

  ; switch to next coin type
  (is= (make-change-impl->ctx {:purse      {5 1}
                               :goal       9
                               :coin       5
                               :coins-left [1]})
       {:purse      {5 1}
        :goal       9
        :coin       1
        :coins-left []})

  ; use next coin type
  (is= (make-change-impl->ctx {:purse      {5 1}
                               :goal       9
                               :coin       1
                               :coins-left []})
       {:purse      {5 1
                     1 1}
        :goal       9
        :coin       1
        :coins-left []})
  (is= (make-change-impl->ctx {:purse      {5 1
                                            1 1}
                               :goal       9
                               :coin       1
                               :coins-left []})
       {:purse      {5 1
                     1 2}
        :goal       9
        :coin       1
        :coins-left []})

  ; last call
  (is= (make-change-impl->ctx {:purse      {5 1
                                            1 4}
                               :goal       9
                               :coin       1
                               :coins-left []})
       {:purse      {5 1
                     1 4}
        :goal       9
        :coin       nil
        :coins-left []}))

(verify
  (throws? (make-change-impl {1 2 3 4} 99 nil [3 4])) ; illegal overlap of purse & coins-left
  (throws? (make-change-impl->ctx {1 2 3 4} 99 nil [4 5])) ; illegal to terminate if coins-left

  ; terminate & return purse
  (is= (make-change-impl {:purse      {1 2 5 2}
                          :goal       12
                          :coin       nil
                          :coins-left []})
       {1 2 5 2})

  ; could not make change
  (is= (make-change-impl {:purse      {5 1}
                          :goal       6
                          :coin       nil
                          :coins-left []})
       nil))

;---------------------------------------------------------------------------------------------------
; Depending on the order of coin denominations retreived form the initial set of coins,
; we can get very different ways to make change.  Use an `ordered-set` to verify different
; valid ways of making change.
(verify

  (is=
    (make-change 44 (ordered-set 1 5 10 25))
    (make-change 44 (ordered-set 1 10 5 25))
    (make-change 44 (ordered-set 1 25 5 10))
    {1 44})
  (is= (make-change 44 (ordered-set 25 10 5 1))
       {25 1
        10 1
        5  1
        1  4})
  (is=
    (make-change 44 (ordered-set 10 5 1 25))
    (make-change 44 (ordered-set 10 1 5 25))
    (make-change 44 (ordered-set 10 25 5 1))
    {10 4
     1  4})
  (is=
    (make-change 44 (ordered-set 5 10 1 25))
    (make-change 44 (ordered-set 5 10 25 1))
    (make-change 44 (ordered-set 5 25 1 10))
    {5 8
     1 4}))

(verify
  (is= {5 1
        1 1} (make-change 6 (ordered-set 25 10 5 1)))
  (is= {3 2} (make-change 6 (ordered-set 3 4)))

  (is= {1 6} (make-change 6 (ordered-set 1 3 4)))
  (is= {4 1
        1 2} (make-change 6 (ordered-set 4 3 1)))
  (is= {4 1
        1 2} (make-change 6 (ordered-set 4 1 3)))

  (is= nil (make-change 6 (ordered-set 5 7)))

  (is= {7 1
        9 1} (make-change 16 (ordered-set 5 7 9))))

;---------------------------------------------------------------------------------------------------
; OK, for this one we had to pull in `clojure.math.combinatorics` after all!
(verify
  (is= {7 1 9 1}
    (make-change 16 (ordered-set 5 7 9))
    (make-change 16 (ordered-set 7 5 9))
    (make-change 16 (ordered-set 7 9 5))
    (make-change 16 (ordered-set 9 5 7))))

;---------------------------------------------------------------------------------------------------
; Really should add in some generative testing here using clojure.test.check
;


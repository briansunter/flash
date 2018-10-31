(ns lambda.core
  (:require [cljs-lambda.macros :refer-macros [deflambda]]))

(def test-suggested-reviews [{:type "info" :id "foo" :cardReviewsId "foo" :card {:id "bas" :name "fee" :description "bar"}}])

(deflambda suggested-reviews [event ctx]
  test-suggested-reviews)

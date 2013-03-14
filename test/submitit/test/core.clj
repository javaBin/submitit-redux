(ns submitit.test.core
  (:use [submitit.core])
  (:use [clojure.test]))

(deftest check-do-to-map
	(is (= {:a "ax" :b "bx"} (do-to-map {:a "a" :b "b"} (fn [x] (str x "x")))) "Something wrong")
	)

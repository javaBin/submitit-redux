(ns submitit.test.core
  (:use [submitit.core])
  (:use [clojure.test]))

(deftest check-do-to-map
	(is (= {:a "ax" :b "bx"} (do-to-map {:a "a" :b "b"} (fn [x] (str x "x")))) "Basic")
	(is (= {:a "ax" :b ["bx" "cx"]} (do-to-map {:a "a" :b ["b" "c"]} (fn [x] (str x "x"))))  "With array")
	)

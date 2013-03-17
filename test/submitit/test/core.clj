(ns submitit.test.core
  (:use [submitit.core])
  (:use [clojure.test]))

(deftest check-do-to-map
	(is (= {:a "ax" :b "bx"} (do-to-map {:a "a" :b "b"} (fn [x] (str x "x")))) "Basic")
	(is (= {:a "ax" :b ["bx" "cx"]} (do-to-map {:a "a" :b ["b" "c"]} (fn [x] (str x "x"))))  "With array")
	(is (= {:a "ax" :b {:ba "bax" :ca "cax"}} (do-to-map {:a "a" :b {:ba "ba" :ca "ca"}} (fn [x] (str x "x"))))  "With map")
	(is (= {:a "ax" :b [{:ba "bax" :ca "cax"} {:ba "2bx" :ca "2cx"}]} (do-to-map {:a "a" :b [{:ba "ba" :ca "ca"} {:ba "2b" :ca "2c"}]} (fn [x] (str x "x"))))  "With vector map")
	)

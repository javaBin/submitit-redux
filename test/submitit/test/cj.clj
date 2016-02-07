(ns submitit.test.cj
  (:use [submitit.cj])
  (:use [submitit.core])
  (:use [submitit.base])
  (:use [clojure.test])

  )


(deftest length-as-tag
  (is (= ["len45"] ((add-length-to-tags nil "45") "tags")) "Adds single tag to new talk")
  (is (= (set ["xx" "len45"]) (set ((add-length-to-tags {"tags" ["xx"]} "45") "tags"))) "Adds a talk to existing without length")
  (is (= (set ["xx" "len45"]) (set ((add-length-to-tags {"tags" ["len60" "xx"]} "45") "tags"))) "Replaces existing length")
	)
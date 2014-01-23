(ns submitit.test.email
  (:use [submitit.email])
  (:use [clojure.test])

  )

 (deftest generate-mail-text-tests
 	(is (= (generate-mail-text "Here is text" {}) "Here is text") "No template stuff - do nothing")
 )


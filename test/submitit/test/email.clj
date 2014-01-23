(ns submitit.test.email
  (:use [submitit.email])
  (:use [clojure.test])

  )

 (deftest generate-mail-text-tests
 	(is (= (generate-mail-text "Here is text" {}) "Here is text") "No template stuff - do nothing")
   (is (= (generate-mail-text "Title %title% some" {"title" "cool title"}) "Title cool title some") "Replace basic field")
   (is (= (generate-mail-text "Numbers %anum%" {"num" ["one" "two" "three"]}) "Numbers one, two, three") "Handle arrays")
 )


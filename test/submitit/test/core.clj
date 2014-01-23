(ns submitit.test.core
  (:use [submitit.core])
  (:use [submitit.base])
  (:use [clojure.test])

  )


(deftest closing-submit-test
	(is (submit-open? {"addKey" "34534"}) "Always allowed to update talks")
	(with-redefs [read-setup (fn [keyval] nil)]
		(is (submit-open? {}) "Open if closing date not set"))

	(with-redefs [read-setup (fn [keyval] (cond (= keyval :closing-time) "20130315200000" (= keyval :close-password) "secret" :else nil))
				  time-now (fn [] "20130316101523")]
		(is (not (submit-open? {})) "We are closed")
				)
	(with-redefs [read-setup (fn [keyval] (if (= keyval :closing-time) "20130315200000" nil))
				  time-now (fn [] "20130216101523")]
		(is (submit-open? {}) "We are open")
				)

	(with-redefs [read-setup (fn [keyval] (cond (= keyval :closing-time) "20130315200000" (= keyval :close-password) "secret" :else nil))
				  time-now (fn [] "20130316101523")]
		(is (not (submit-open? {"password" "dummy"})) "We are closed wrong password"))

	(with-redefs [read-setup (fn [keyval] (cond (= keyval :closing-time) "20130315200000" (= keyval :close-password) "secret" :else nil))
				  time-now (fn [] "20130316101523")]
		(is (submit-open? {"password" "secret"}) "We are closed correct password"))

	(with-redefs [read-setup (fn [keyval] (cond (= keyval :closing-time) "20130315200000" (= keyval :close-password) "secret" :else nil))
				  time-now (fn [] "20130316101523")]
		(is (need-submit-password?) "Password needed"))

	(with-redefs [read-setup (fn [keyval] nil)
				  time-now (fn [] "20130316101523")]
		(is (not (need-submit-password?)) "Password not needed no config"))

	(with-redefs [read-setup (fn [keyval] (cond (= keyval :closing-time) "20130315200000" (= keyval :close-password) "secret" :else nil))
				  time-now (fn [] "20130116101523")]
		(is (not (need-submit-password?)) "Password not needed not closed"))
)

(deftest validate-speaker-test
	(is (= "Speaker name is required" (validate-speaker-input [{"speakerName" "a" "email" "a@a.com" "bio" "bioa"} {"speakerName" "" "email" "b@a.com" "bio" "biob"}])))
    (is (= "Speakers must have different email" (validate-speaker-input [{"speakerName" "a" "email" "a@a.com" "bio" "bioa"} {"speakerName" "b" "email" "a@a.com" "bio" "biob"}])))
	(is (nil? (validate-speaker-input [{"speakerName" "a" "email" "a@a.com" "bio" "bioa"} {"speakerName" "a" "email" "b@a.com" "bio" "biob"}])))	
)
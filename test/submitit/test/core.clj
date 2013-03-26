(ns submitit.test.core
  (:use [submitit.core])
  (:use [clojure.test]))

(deftest check-do-to-map
	(is (= {:a "ax" :b "bx"} (do-to-map {:a "a" :b "b"} (fn [x] (str x "x")))) "Basic")
	(is (= {:a "ax" :b ["bx" "cx"]} (do-to-map {:a "a" :b ["b" "c"]} (fn [x] (str x "x"))))  "With array")
	(is (= {:a "ax" :b {:ba "bax" :ca "cax"}} (do-to-map {:a "a" :b {:ba "ba" :ca "ca"}} (fn [x] (str x "x"))))  "With map")
	(is (= {:a "ax" :b [{:ba "bax" :ca "cax"} {:ba "2bx" :ca "2cx"}]} (do-to-map {:a "a" :b [{:ba "ba" :ca "ca"} {:ba "2b" :ca "2c"}]} (fn [x] (str x "x"))))  "With vector map")
	)


(deftest input-cleaning 
	(is (= "Noe" (clean-html "Noe")) "Plain text")
	(is (= "Noe æøå noe" (clean-html "Noe æøå noe")) "Norwegian letters")
	(is (= "This remains Heading" (clean-html "<script src='fhf'>The script</script>This remains <h1>Heading</h1>")) "Removes html-tags")
	)

(deftest clean-all-input

 (is (= {:abstract "Abstract"} (clean-input-map {:abstract "<h1>Abstract</h1>"})))
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
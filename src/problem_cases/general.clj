(ns problem-cases.general
  "A place to examine poor parser behavior.  These should go in tests when they get written."
  )


;; Should have only this comment in the left margin.
;; See [https://github.com/fogus/marginalia/issues/#issue/4](https://github.com/fogus/marginalia/issues/#issue/4)

(defn parse-bool [v] (condp = (.trim (text v))
                         "0" false
                         "1" true
                         "throw exception here"))

(ns marginalia.log
  "Using a third-party logging library would be too much, but this namespace provides some lightweight functions to at
  least centralize where logging happens.")

(defn error
  "Print the message to stderr, substituting any provided `format-args` in (as in `format`)."
  [msg & format-args]
  (binding [*out* *err*]
    (println (apply format msg format-args))))

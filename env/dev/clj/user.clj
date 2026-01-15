(ns user)

(defonce debug-atom (atom []))
(defn add-to-debug [x]
  (swap! debug-atom conj x))
(add-tap add-to-debug)

(comment
  (reset! debug-atom [])
  (-> @debug-atom))

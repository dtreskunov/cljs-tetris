(ns cljs-tetris.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [cljs-tetris.canvas :as canvas]
    [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(enable-console-print!)

(def the-canvas (canvas/init! "tetris" 800 600))

(-> the-canvas
    (canvas/add-background! "#333")
    (canvas/add-fps-counter!))

(defn add-random-bubble []
  (defn rand-range [a b] (+ a (rand (- b a))))
  (defn rand-color [] {:r (rand-int 255) :g (rand-int 255) :b (rand-int 255)})
  (let [box (canvas/make-bounding-box the-canvas)
        r (rand-range 20 300)]
    (canvas/add-bubble!
      the-canvas
      :x (rand-int (:w box))
      :y (rand-int (:h box))
      :x' (rand-range -100 100)
      :y' (rand-range -100 100)
      :r r
      :color (rand-color))))

(defn schedule [ms callback & args]
  (go-loop
    []
    (callback args)
    (<! (timeout ms))
    (recur)))

(schedule 500 add-random-bubble)

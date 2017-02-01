(ns cljs-tetris.core
  (:require [monet.canvas :as canvas]))

(enable-console-print!)

(def canvas-dom (.getElementById js/document "tetris"))
(assert (not (nil? canvas-dom)) "HTML should contain a <canvas id='tetris'> element")

(def canvas-w 400)
(def canvas-h 500)

(set! (.-width canvas-dom) canvas-w) 
(set! (.-height canvas-dom) canvas-h) 

(def monet-canvas (canvas/init canvas-dom "2d"))

(canvas/add-entity monet-canvas :background
                   (canvas/entity {:x 0 :y 0 :w canvas-w :h canvas-h} ; val
                                  nil                       ; update function
                                  (fn [ctx val]             ; draw function
                                    (-> ctx
                                        (canvas/fill-style "#191d21")
                                        (canvas/fill-rect val)))))

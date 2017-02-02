(ns cljs-tetris.canvas
  (:require [monet.canvas :as canvas]
            [monet.geometry :as geometry]))

(defn init! [element-id w h]
  (let [element (.getElementById js/document element-id)]
    (assert (not (nil? element)) (str "HTML should contain an element with id" element-id))
    (assert (= "CANVAS" (.-tagName element)) (str "Element with id " element-id " should be a <canvas>"))
    (set! (.-width element) w)
    (set! (.-height element) h)
    (canvas/init element)))

(defn make-bounding-box [canvas]
  (let [element (:canvas canvas)]
    {:x 0 :w (.-width element)
     :y 0 :h (.-height element)}))

(defn add-background! [canvas color]
  (canvas/add-entity
    canvas :background
    (let
      [val (make-bounding-box canvas)
       update nil
       draw (fn [ctx val]
              (-> ctx
                  (canvas/fill-style color)
                  (canvas/fill-rect val)))]
      (canvas/entity val update draw)))
  canvas)

(defn add-fps-counter! [canvas]
  (canvas/add-entity
    canvas :fps
    (let
      [val {:x 0 :y 10 :text ""}
       update (let [frames (atom 0)
                    millis (atom 0)]
                (fn [val dt]
                  (swap! frames inc)
                  (swap! millis #(+ dt %))
                  (when (> @millis 1000)
                    (let [fps (str (/ @frames (/ @millis 1000)))
                          n (count (js-keys (:entities canvas)))]
                      (reset! frames 0)
                      (reset! millis 0)
                      (assoc val :text (str n " objs, " (int fps) " FPS")))
                    )
                  ))
       draw (fn [ctx val]
              (-> ctx
                  (canvas/fill-style "#FFFFFF")
                  (canvas/text val)))]
      (canvas/entity val update draw)))
  canvas)

(defn outside? [container object]
  (let [cbr (geometry/bottom-right container)
        ctl (geometry/top-left container)
        br (geometry/bottom-right object)
        tl (geometry/top-left object)]
    (or
      (or (> (:x ctl) (:x br))
          (> (:y ctl) (:y br)))
      (or (< (:x cbr) (:x tl))
          (< (:y cbr) (:y tl))))))

(defn add-bubble! [canvas & {:keys [x x' y y' r color scale-x scale-y] :as val}]
  (let [k (gensym "bouncy-ball")
        box (make-bounding-box canvas)
        update (let [t (atom 0)]
                 (fn [val dt]
                   (reset! t (+ @t dt))
                   (let [x (:x val)
                         y (:y val)
                         dx (* x' (/ dt 1000))
                         dy (* y' (/ dt 1000))
                         newx (+ x dx)
                         newy (+ y dy)]
                     (if (outside? box val)
                       (canvas/remove-entity canvas k)
                       (assoc val
                         :x newx
                         :y newy
                         :scale-x (+ 0.9 (* 0.1 (.sin js/Math (/ @t 200))))
                         :scale-y (+ 0.9 (* 0.1 (.sin js/Math (/ @t 200)) -1)))))))
        draw (fn [ctx {:keys [x y r scale-x scale-y] :as val}]
               (-> ctx
                   (canvas/save)
                   (canvas/translate x y)
                   (canvas/scale scale-x scale-y)
                   (canvas/circle {:x 0 :y 0 :r r})
                   (canvas/stroke-style "white")
                   (canvas/stroke-width 2)
                   (canvas/stroke)
                   (canvas/fill-style color)
                   (canvas/fill)
                   (canvas/translate 0 (- (/ r 2)))
                   (canvas/scale 1 0.8)
                   (canvas/circle {:x 0 :y 0 :r (/ r 2)})
                   (canvas/fill-style "white")
                   (canvas/alpha 0.5)
                   (canvas/fill)
                   (canvas/restore)))]
    (canvas/add-entity canvas k (canvas/entity val update draw)))
  canvas)

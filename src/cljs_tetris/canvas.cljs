(ns cljs-tetris.canvas
  (:require [monet.canvas :as canvas]
            [monet.geometry :as geometry]
            [goog.events :as gevents]))

(defn init! [element-id]
  (let [element (.getElementById js/document element-id)]
    (assert (not (nil? element)) (str "HTML should contain an element with id" element-id))
    (assert (= "CANVAS" (.-tagName element)) (str "Element with id " element-id " should be a <canvas>"))
    (gevents/listen js/window goog.events.EventType.RESIZE
                    (fn [evt]
                      (set! (.-width element) (.-clientWidth element))
                      (set! (.-height element) (.-clientHeight element))))
    
    (canvas/init element)))

(defn make-bounding-box [canvas]
  (let [element (:canvas canvas)]
    {:x 0 :w (.-width element)
     :y 0 :h (.-height element)}))

(defn add-background! [canvas color]
  (canvas/add-entity
    canvas :background
    (let
      [val nil
       update nil
       draw (fn [ctx val]
              (-> ctx
                  (canvas/fill-style color)
                  (canvas/fill-rect (make-bounding-box canvas))))]
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

(defn- fill-style [ctx style]
  (set! (.-fillStyle ctx) style)
  ctx)

(defn add-bubble! [canvas & {:keys [x x' y y' r color] :as init-val}]
  (defn bubble-fill-style! [ctx color]
    (let [gradient (.createRadialGradient ctx 40 40 0 0 0 100)]
      (.addColorStop gradient 0    (str "rgba(" (color :r) "," (color :g) "," (color :b) "," 0   ")"))
      (.addColorStop gradient 0.7  (str "rgba(" (color :r) "," (color :g) "," (color :b) "," 0.1 ")"))
      (.addColorStop gradient 0.95 (str "rgba(" (color :r) "," (color :g) "," (color :b) "," 1   ")"))
      (.addColorStop gradient 1    (str "rgba(" (color :r) "," (color :g) "," (color :b) "," 0   ")"))
      (set! (.-fillStyle ctx) gradient)
      ctx))
    
  (let [k (gensym "bouncy-ball")
        box (make-bounding-box canvas)
        t (atom 0)
        val (assoc init-val :r 0)
        update (fn [{:keys [x y r] :as val} dt]
         (reset! t (+ @t dt))
         (let [dx (* x' (/ dt 1000))
               dy (* y' (/ dt 1000))
               dr (* 5 (.sqrt js/Math (+ (* dx dx) (* dy dy )))) 
               newval (assoc val
                             :x (+ x dx)
                             :y (+ y dy)
                             :r (min (+ r dr) (:r init-val)))]
           (if (outside? box newval)
             (canvas/remove-entity canvas k)
             newval)))
        draw (fn [ctx {:keys [x y r scale-x scale-y] :as val}]
               (-> ctx
                    (canvas/save)
                    (canvas/translate x y)
                    (canvas/scale (/ r 100) (/ r 100))
                    (bubble-fill-style! color)
                    (canvas/fill-rect {:x (- 100) :y (- 100) :w 200 :h 200})
                    (canvas/restore)))]
    (canvas/add-entity canvas k (canvas/entity val update draw)))
  canvas)

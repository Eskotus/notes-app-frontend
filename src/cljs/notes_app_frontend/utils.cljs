(ns notes-app-frontend.utils
  (:require
    [cljs.core.async :as a]
    [reagent.session :as session]))

(defn set-hash! [loc]
  (prn loc)
  (set! (.-hash js/window.location) loc))

(defn <<< [f & args]
  (let [c (a/chan)]
    (apply f (concat args [(fn [x]
                             (if (or (nil? x)
                                     (undefined? x))
                               (a/close! c)
                               (a/put! c x)))]))
    c))

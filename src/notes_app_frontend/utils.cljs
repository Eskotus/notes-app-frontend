(ns notes-app-frontend.utils
  (:require
    [clojure.string :as s]
    [cljs.core.async :as a]))

(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))

(defn validate-login-form
  "Validate login form submit"
  [email-atom password-atom]
  (not (or (s/blank? @email-atom) (s/blank? @password-atom))))

(defn <<< [f & args]
  (let [c (a/chan)]
    (apply f (concat args [(fn [x]
                             (if (or (nil? x)
                                     (undefined? x))
                               (a/close! c)
                               (a/put! c x)))]))
    c))

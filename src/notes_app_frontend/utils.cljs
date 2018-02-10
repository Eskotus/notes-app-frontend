(ns notes-app-frontend.utils
  (:require
    [clojure.string :as s]))

(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))

(defn validate-form
  "Validate login form submit"
  [email-atom password-atom]
  (not (and (not (s/blank? @email-atom)) (not (s/blank? @password-atom)))))

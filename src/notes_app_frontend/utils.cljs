(ns notes-app-frontend.utils
  (:require
    [clojure.string :as s]))

(defn set-hash! [loc]
  (set! (.-hash js/window.location) loc))

(defn validate-login-form
  "Validate login form submit"
  [email-atom password-atom]
  (not (or (s/blank? @email-atom) (s/blank? @password-atom))))

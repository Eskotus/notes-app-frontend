(ns notes_app_frontend.views.login-page
  (:require
    [notes_app_frontend.utils :as u]
    [notes-app-frontend.components :as c]
    [notes-app-frontend.aws-lib2 :as aws]
    [reagent.core :as r]
    [clojure.string :as s]
    [cljs.core.async :as a :refer-macros [go]]))

(defn handle-submit
  [event email password loading-atom]
  (.preventDefault event)
  (reset! loading-atom true)
  (go
    (let [result (a/<! (u/<<< aws/login email password))]
      (if (instance? js/Error result)
        (js/alert result)
        (u/set-hash! "")))
    (reset! loading-atom false)))

(defn validate-login-form
  "Validate login form submit"
  [email-atom password-atom]
  (or (s/blank? @email-atom) (s/blank? @password-atom)))

(defn render []
  (let [email (r/atom nil)
        password (r/atom nil)
        loading? (r/atom false)]
    (fn []
      [:div.Login
       [:form {:on-submit #(handle-submit % @email @password loading?)}
        [c/email-form email]
        [c/password-form password]
        [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                          :loading?     @loading?
                          :loading-text "Logging in..."
                          :text         "Login"
                          :disabled     (validate-login-form email password)
                          :type         "submit"}]]])))

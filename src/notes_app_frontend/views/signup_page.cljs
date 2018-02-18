(ns notes_app_frontend.views.signup-page
  (:require
    [notes_app_frontend.utils :as u]
    [reagent.core :as r]
    [clojure.string :as s]
    [cljs.core.async :as a :refer-macros [go]]
    [notes-app-frontend.components :as c]
    [notes-app-frontend.aws-lib :as aws]
    [wilson.react-bootstrap :refer [help-block]]))

(defn handle-submit [event loading? email password confirm-password new-user]
  (.preventDefault event)
  (reset! loading? true)
  (go
    (let [result (a/<! (u/<<< aws/signup @email @password))]
      (if (instance? js/Error result)
        (js/alert result)
        (reset! new-user (:user result)))
      (reset! loading? false))))


(defn handle-confirmation-submit [event loading? confirmation-code]
  (.preventDefault event)
  (reset! loading? true)
  (.log js/console "Verify button clicked")
  (reset! loading? false))

(defn validate-confirmation-form [confirmation-code]
  (empty? @confirmation-code))

(defn validate-form
  [email-address password confirm-password]
  (or (not (= @password @confirm-password))
      (or (s/blank? @email-address) (s/blank? @password) (s/blank? @confirm-password))))

(defn confirmation-form
  "Render form for entering confirmation code"
  [loading?]
  (let [confirmation-code (r/atom nil)]
    (fn [loading?]
      [:form {:on-submit #(handle-confirmation-submit % loading? confirmation-code)}
       [c/confirmation-form confirmation-code]
       [help-block "Please check your email for the code."]
       [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                         :loading?     @loading?
                         :loading-text "Verifying..."
                         :text         "Verify"
                         :disabled     (validate-confirmation-form confirmation-code)
                         :type         "submit"}]])))

(defn signup-form [loading? new-user]
  (let [email-address (r/atom nil)
        password (r/atom nil)
        confirm-password (r/atom nil)]
    (fn [loading? new-user]
      [:form {:on-submit #(handle-submit %
                                         loading?
                                         email-address
                                         password
                                         confirm-password
                                         new-user)}
       [c/email-form email-address]
       [c/password-form password]
       [c/confirm-password-form confirm-password]
       [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                         :loading?     @loading?
                         :loading-text "Signing up..."
                         :text         "Signup"
                         :type         "submit"
                         :disabled     (validate-form email-address
                                                      password
                                                      confirm-password)}]])))



(defn render []
  (let [loading? (r/atom false)
        new-user (r/atom nil)]
    (fn []
      [:div {:class "Signup"}
       (if (nil? @new-user)
         [signup-form loading? new-user]
         [confirmation-form loading?])])))

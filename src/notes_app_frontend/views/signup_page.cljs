(ns notes_app_frontend.views.signup-page
  (:require
    [reagent.core :as r]
    [clojure.string :as s]
    [notes-app-frontend.components :as c]))

(defn handle-submit [event loading? email password confirm-password new-user]
  (.preventDefault event)
  (reset! loading? true)
  (reset! new-user "test")
  (reset! loading? false))

(defn handle-confirmation-submit [event loading? confirmation-code]
  (.preventDefault event))

(defn validate-confirmation-form [confirmation-code]
  (not (empty? @confirmation-code)))

(defn validate-form
  [email-address password confirm-password]
  (or (s/blank? @email-address) (s/blank? @password) (s/blank? @confirm-password)))

(defn confirmation-form
  "Render form for entering confirmation code"
  [loading?]
  (let [confirmation-code (r/atom nil)]
    [:form {:on-submit #(handle-confirmation-submit % loading? confirmation-code)}
     [c/confirmation-form confirmation-code]
     [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                       :loading?     @loading?
                       :loading-text "Verifying..."
                       :text         "Verify"
                       :disabled     (validate-confirmation-form confirmation-code)
                       :type         "submit"}]]))

(defn signup-form [loading? new-user]
  (let [email-address (r/atom nil)
        password (r/atom nil)
        confirm-password (r/atom nil)]
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
                                                    confirm-password)}]]))



(defn render []
  (let [loading? (r/atom false)
        new-user (r/atom nil)]
    [:div {:class "Signup"}
     (if (nil? @new-user)
       (signup-form loading? new-user)
       (confirmation-form loading?))]))

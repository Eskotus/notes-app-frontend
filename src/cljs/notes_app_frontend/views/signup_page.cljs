(ns notes_app_frontend.views.signup-page
  (:require
    [notes-app-frontend.utils :as u]
    [reagent.core :as r]
    [clojure.string :as s]
    [reagent.session :as session]
    [cljs.core.async :as a :refer-macros [go]]
    [notes-app-frontend.components :as c]
    [notes-app-frontend.aws-lib2 :as aws]
    [wilson.react-bootstrap :refer [help-block]]))

(defn handle-submit
  [event loading? email password confirm-password new-user]
  (.preventDefault event)
  (reset! loading? true)
  (go
    (let [result (a/<! (u/<<< aws/signup @email @password))]
      (if (instance? js/Error result)
        (js/alert result)
        (reset! new-user (:user result)))
      (reset! loading? false))))

(defn handle-confirmation-submit
  [event loading? confirmation-code new-user email password]
  (.preventDefault event)
  (reset! loading? true)
  (go
    (let [result (a/<! (u/<<< aws/confirm @email @confirmation-code))]
      (if (instance? js/Error result)
        (js/alert result)
        (let [result (a/<! (u/<<< aws/login @email @password))]
          (if (instance? js/Error result)
            (js/alert result)
            (do
              (session/put! :authenticated? true)
              (u/set-hash! ""))))))
    (reset! loading? false)))

(defn validate-confirmation-form [confirmation-code]
  (empty? @confirmation-code))

(defn validate-form
  [email-address password confirm-password]
  (or (not (= @password @confirm-password))
      (or (s/blank? @email-address) (s/blank? @password) (s/blank? @confirm-password))))

(defn confirmation-form
  "Render form for entering confirmation code"
  [loading? new-user email password]
  (let [confirmation-code (r/atom nil)]
    (fn [loading? new-user email password]
      [:form {:on-submit #(handle-confirmation-submit % loading? confirmation-code new-user email password)}
       [c/confirmation-form confirmation-code]
       [help-block "Please check your email for the code."]
       [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                         :loading?     @loading?
                         :loading-text "Verifying..."
                         :text         "Verify"
                         :disabled     (validate-confirmation-form confirmation-code)
                         :type         "submit"}]])))

(defn signup-form [loading? new-user email password]
  (let [confirm-password (r/atom nil)]
    (fn [loading? new-user email password]
      [:form {:on-submit #(handle-submit %
                                         loading?
                                         email
                                         password
                                         confirm-password
                                         new-user)}
       [c/email-form email]
       [c/password-form password]
       [c/confirm-password-form confirm-password]
       [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                         :loading?     @loading?
                         :loading-text "Signing up..."
                         :text         "Signup"
                         :type         "submit"
                         :disabled     (validate-form email
                                                      password
                                                      confirm-password)}]])))



(defn render []
  (if (= (session/get :authenticated?) false)
    (let [loading? (r/atom false)
          new-user (r/atom nil)
          email (r/atom nil)
          password (r/atom nil)]
      (fn []
        [:div {:class "Signup"}
         (if (nil? @new-user)
           [signup-form loading? new-user email password]
           [confirmation-form loading? new-user email password])]))
    (fn []
      (u/set-hash! "/")
      [:div])))
(ns notes-app-frontend.core
  (:import goog.History)
  (:require
    [notes-app-frontend.aws-lib :as aws]
    [notes-app-frontend.components :as c]
    [notes_app_frontend.utils :as u]
    [notes_app_frontend.views.signup-page :as signup]
    [secretary.core :as secretary :refer-macros [defroute]]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [reagent.core :as r]
    [reagent.session :as session]))

;; -------------------------
;; Views

(defn home-page []
  [:div.Home
   [:div.lander
    [:h1 "Scratch"]
    [:p "A simple note taking app"]]])

(defn login-page []
  (let [email-address (r/atom nil)
        password (r/atom nil)
        loading? (r/atom false)]
    (fn []
      [:div.Login
       [:form {:on-submit (fn [event]
                            (.preventDefault event)
                            (aws/login @email-address @password loading?))}
        [c/email-form email-address]
        [c/password-form password]
        [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                          :loading?     @loading?
                          :loading-text "Logging in..."
                          :text         "Login"
                          :disabled     (u/validate-login-form email-address password)
                          :type         "submit"}]]])))

(defn signup-page []
  [:div.Signup
   [:h1 "Signup"]])

(defn not-found-page []
  [:div.NotFound
   [:h3 "Sorry, page not found!"]])

(defn current-page
  "Wraps all other page content in container that has navigation in the header"
  []
  (aws/authenticate-user)
  (when (not (session/get :authenticating?))
    [:div.App.container
     (c/navigation)
     [(session/get :current-page)]]))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(defroute "/" [] (session/put! :current-page home-page))

(defroute "/signup" [] (session/put! :current-page signup/render))

(defroute "/login" [] (session/put! :current-page login-page))

(defroute "*" [] (session/put! :current-page not-found-page))

;; -------------------------
;; History

(defn hook-browser-navigation! []
  (let [history (doto (History.)
                  (events/listen
                    EventType/NAVIGATE
                    (fn [event]
                      (secretary/dispatch! (.-token event))))
                  (.setEnabled true))]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (hook-browser-navigation!)
  (r/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (session/put! :authenticated? false)
  (session/put! :authenticating? true)
  (mount-root))

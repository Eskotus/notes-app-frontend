(ns notes-app-frontend.core
  (:import goog.History)
  (:require
    [notes-app-frontend.aws-lib2 :as aws]
    [notes-app-frontend.components :as c]
    [notes_app_frontend.views.home-page :as home]
    [notes_app_frontend.views.not-found-page :as not-found]
    [notes_app_frontend.views.login-page :as login]
    [notes_app_frontend.views.signup-page :as signup]
    [notes_app_frontend.views.new-note-page :as new-note]
    [secretary.core :as secretary :refer-macros [defroute]]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [reagent.core :as r]
    [reagent.session :as session]))

;; -------------------------
;; Views

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

(defroute "/" [] (session/put! :current-page home/render))

(defroute "/signup" [] (session/put! :current-page signup/render))

(defroute "/login" [] (session/put! :current-page login/render))

(defroute "/notes/new" [] (session/put! :current-page new-note/render))

(defroute "/notes/:id" [id] (session/put! :current-page nil))

(defroute "*" [] (session/put! :current-page not-found/render))

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

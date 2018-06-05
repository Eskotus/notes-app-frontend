(ns notes-app-frontend.core
  (:import goog.History)
  (:require
    [notes-app-frontend.aws-lib2 :as aws]
    [notes-app-frontend.components :as c]
    [notes_app_frontend.utils :as u]
    [notes_app_frontend.views.home-page :as home]
    [notes_app_frontend.views.not-found-page :as not-found]
    [notes_app_frontend.views.login-page :as login]
    [notes_app_frontend.views.signup-page :as signup]
    [notes_app_frontend.views.new-note-page :as new-note]
    [notes_app_frontend.views.note-page :as note]
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

(defn route-helper
  [requires-authentication render-fn]
  (if requires-authentication
    (if (session/get :authenticated?)
      (session/put! :current-page render-fn)
      (session/put! :current-page
                    (fn []
                      (u/set-hash!
                        (str "/login?redirect="
                          (.-hash js/location)))
                      [:div])))
    (if (session/get :authenticated?)
      (session/put! :current-page
                    (fn []
                      (u/set-hash! "/")
                      [:div]))
      (session/put! :current-page render-fn))))

(secretary/set-config! :prefix "#")

(defroute "/" [] (session/put! :current-page home/render))

(defroute "/signup" [] (route-helper false signup/render))

(defroute "/login" [] (route-helper false login/render))

(defroute "/notes/new" [] (route-helper true new-note/render))

(defroute "/notes/:id" [id] (route-helper true #(note/render id)))

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

(ns notes-app-frontend.routes
  (:require
    [notes_app_frontend.views.home-page :as home]
    [notes_app_frontend.views.not-found-page :as not-found]
    [notes_app_frontend.views.login-page :as login]
    [notes_app_frontend.views.signup-page :as signup]
    [notes_app_frontend.views.new-note-page :as new-note]
    [secretary.core :as sec :refer-macros [defroute]]
    [reagent.session :as session]
    [goog.events :as events]
    [goog.history.EventType :as EventType])
  (:import goog.history.Html5History))

(defonce history (Html5History.))

;; We can switch routes through this function
(defn trans
  [path]
  (.setToken history path))

(defroute "/" [] (session/put! :current-page home/render))

(defroute "/signup" [] (session/put! :current-page signup/render))

(defroute "/login" [] (session/put! :current-page login/render))

(defroute "/notes/new" [] (session/put! :current-page new-note/render))

(defroute "*" [] (session/put! :current-page not-found/render))

(defn- on-popstate
  [e]
  (-> e .-token sec/dispatch!))

(events/listen history EventType/NAVIGATE on-popstate)

;; We can remove # prefix this way
(doto history (.setEnabled true)
              (.setPathPrefix "")
              (.setUseFragment false))

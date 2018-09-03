(ns notes-app-frontend.core
  (:import goog.History)
  (:require
    [notes-app-frontend.aws-lib2 :as aws]
    [notes-app-frontend.components :as c]
    [notes-app-frontend.utils :as u]
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
    [reagent.session :as session]
    [cljs.core.async :as a :refer-macros [go]]))

(defn route-auth-helper
  [render-fn]
  (go
    (session/put! :authenticating? true)
    (let [result (a/<! (u/<<< aws/authenticate-user2))]
      (if (instance? js/Error result)
        (session/put! :authenticated? false)
        (do
          (session/put! :authenticated? true)
          (println "User authenticated"))))
    (session/put! :authenticating? false)
    (session/put! :current-page render-fn)))
;(if requires-authentication
;  (if (session/get :authenticated?)
;    (session/put! :current-page render-fn)
;    (session/put! :current-page
;                  (fn []
;                    (u/set-hash!
;                      (str "/login?redirect="
;                        (.-hash js/location)))
;                    [:div])))
;  (if (session/get :authenticated?)
;    (session/put! :current-page
;                  (fn []
;                    (println "Authenticated, but does not require it")
;                    (u/set-hash! "/")
;                    [:div]))
;    (session/put! :current-page render-fn)))))

;; -------------------------
;; Views

(defn current-page
  "Wraps all other page content in container that has navigation in the header"
  []
  (println "current-page"
           (re-seq #"^.*\(" (str (session/get :current-page)))
           "authenticating?" (session/get :authenticating?))
  (when (not (session/get :authenticating?))
    [:div.App.container
     (c/navigation)
     [(session/get :current-page)]]))

;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(defroute "/" [] (route-auth-helper home/render))

(defroute "/signup" [] (route-auth-helper signup/render))

(defroute "/login" [] (route-auth-helper login/render))

(defroute "/notes/new" [] (route-auth-helper new-note/render))

(defroute "/notes/:id" [id] (route-auth-helper #(note/render id)))

(defroute "*" [] (route-auth-helper not-found/render))

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

(defn init []
  (session/put! :authenticated? false)
  (session/put! :authenticating? true)
  (mount-root))


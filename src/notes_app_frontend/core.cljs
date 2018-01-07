(ns notes-app-frontend.core
  (:import goog.History)
  (:require
    [clojure.string :as s]
    [secretary.core :as secretary :refer-macros [defroute]]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [reagent.core :as r]
    [reagent.session :as session]
    [wilson.react-bootstrap :refer
     [navbar
      navbar-header
      navbar-brand
      navbar-toggle
      navbar-collapse
      nav
      nav-item]]))

;; -------------------------
;; Atoms

;; -------------------------
;; Components

(defn route-nav-item
  [props & content]
  (fn [props]
    (let [new-props (if (s/ends-with?
                          js/window.location.href
                          (get props :href))
                      (conj props {:class "active"})
                      props)
          view (session/get :current-page)]
      ;(.log js/console @current-url)
      ;(.log js/console (get props :href))
      ;(.log js/console new-props)
      [nav-item new-props content])))

;; -------------------------
;; Views

(defn home-page []
  [:div.Home
   [:div.lander
    [:h1 "Scratch"]
    [:p "A simple note taking app"]]])

(defn login-page []
  [:div.Login
   [:h1 "Login"]])

(defn signup-page []
  [:div.Signup
   [:h1 "Signup"]])

(defn current-page
  "Wraps all other page content in container that has navigation in the header"
  []
  [:div.App.container
   [navbar {:fluid true
            :collapseOnSelect true}
    [navbar-header
     [navbar-brand
      [:a {:href "/"} "Scratch"]]
     [navbar-toggle]]
    [navbar-collapse
     [nav {:pullRight true}
      [route-nav-item {:href "#/signup"} "Signup"]
      [route-nav-item {:href "#/login"} "Login"]]]]
   [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(defroute "/" [] (session/put! :current-page home-page))

(defroute "/signup" [] (session/put! :current-page signup-page))

(defroute "/login" [] (session/put! :current-page login-page))

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
  (mount-root))

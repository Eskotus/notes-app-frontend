(ns notes-app-frontend.core
  (:import goog.history.Html5History
           goog.Uri)
  (:require
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
;; Components

(defn route-nav-item
  [props & content]
  (fn [props]
    (let [props (if (= (get props :href)
                       js/window.location.pathname)
                       (conj props {:class "active"})
                       props)]
      [nav-item props content])))

;; -------------------------
;; Views

(defn home-page []
  [:div.Home
   [:div.lander
    [:h1 "Scratch"]
    [:p "A simple note taking app"]]])

(defn current-page
  "Wraps all other page content in container that has navigation in the header"
  []
  [:div.App.container
   [navbar {:class "navbar-fluid navbar-collapse-on-select"}
    [navbar-header
     [navbar-brand
      [:a {:href "/"} "Scratch"]]
     [navbar-toggle]
     [navbar-collapse
      [nav {:pullRight true}
       [route-nav-item {:href "/signup"} "Signup"]
       [route-nav-item {:href "/login"} "Login"]]]]]
   [(session/get :current-page)]])

;; -------------------------
;; Routes

(defroute "/" [] (session/put! :current-page home-page))

;; -------------------------
;; History

(defn hook-browser-navigation! []
  (let [history (doto (Html5History.)
                  (events/listen
                    EventType/NAVIGATE
                    (fn [event]
                      (secretary/dispatch! (.-token event))))
                  (.setUseFragment false)
                  (.setPathPrefix "")
                  (.setEnabled true))]

    (events/listen js/document "click"
                   (fn [e]
                     (. e preventDefault)
                     (let [path (.getPath (.parse Uri (.-href (.-target e))))
                           title (.-title (.-target e))]
                       (when path
                         (. history (setToken path title))))))))

;; -------------------------
;; Initialize app

(defn mount-root []
  (hook-browser-navigation!)
  (r/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

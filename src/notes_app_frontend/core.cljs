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

(defn validate-form
  "Validate login form submit"
  [email-atom password-atom]
  (not (and (not (s/blank? @email-atom)) (not (s/blank? @password-atom)))))

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
      [nav-item new-props content])))

(defn input-element
  "An input element which updates its value and on focus parameters on change, blur, and focus"
  [id name type value]
  [:input {:id        id
           :name      name
           :class     "form-control"
           :type      type
           :required  ""
           :value     @value
           :on-change #(reset! value (-> % .-target .-value))}])

(defn email-form
  [email-address-atom]
  (input-element "email"
                    "email"
                    "email"
                    email-address-atom))

(defn password-form [password-atom]
  (input-element "password"
                    "password"
                    "password"
                    password-atom))

(defn wrap-as-element-in-form
  [element]
  [:div {:class "form-group form-group-lg"}
   element])

;; -------------------------
;; Views

(defn home-page []
  [:div.Home
   [:div.lander
    [:h1 "Scratch"]
    [:p "A simple note taking app"]]])

(defn login-page []
  (let [email-address (r/atom nil)
        password (r/atom nil)]
    (fn []
      [:div.Login
       [:form {:on-submit (fn [event] (.preventDefault event))}
        (wrap-as-element-in-form [email-form email-address])
        (wrap-as-element-in-form [password-form password])
        [:button.btn.btn-default.btn-lg {:disabled (validate-form email-address password) :type "submit"} "Login"]]])))

(defn signup-page []
  [:div.Signup
   [:h1 "Signup"]])

(defn not-found-page []
  [:div.NotFound
   [:h3 "Sorry, page not found!"]])

(defn current-page
  "Wraps all other page content in container that has navigation in the header"
  []
  [:div.App.container
   [navbar {:fluid            true
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
  (mount-root))

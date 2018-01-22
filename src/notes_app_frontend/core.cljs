(ns notes-app-frontend.core
  (:import goog.History)
  (:require
    [clojure.string :as s]
    [cljs.core.async :as a :refer-macros [go]]
    [secretary.core :as secretary :refer-macros [defroute]]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [reagent.core :as r]
    [reagent.session :as session]
    [cljsjs.amazon-cognito-identity-js]
    [wilson.react-bootstrap :refer
     [navbar
      navbar-header
      navbar-brand
      navbar-toggle
      navbar-collapse
      nav
      nav-item]]))

(def config
  {:cognito
   {:userPoolId       "us-east-1_G25s7dUDi"
    :userPoolClientID "1im6a36mes510oabud7cd7jte6"}})

(def CognitoUserPool (-> js/AmazonCognitoIdentity .-CognitoUserPool))
(def CognitoUserAttribute (-> js/AmazonCognitoIdentity .-CognitoUserAttribute))
(def CognitoUser (-> js/AmazonCognitoIdentity .-CognitoUser))
(def AuthenticationDetails (-> js/AmazonCognitoIdentity .-AuthenticationDetails))
(def AWSCognito js/AWSCognito)

(defn get-user-pool []
  (new CognitoUserPool
       (clj->js
         {:UserPoolId (get-in config [:cognito :userPoolId])
          :ClientId   (get-in config [:cognito :userPoolClientID])})))

(defonce user-pool (get-user-pool))

(defn- create-cognito-user [email]
  (new CognitoUser
       (clj->js
         {:Username email
          :Pool     user-pool})))

(defn validate-form
  "Validate login form submit"
  [email-atom password-atom]
  (not (and (not (s/blank? @email-atom)) (not (s/blank? @password-atom)))))

(defn login
  "Authenticate user against AWS cognito"
  [email password]
  (let [user (create-cognito-user email)
        auth-details (new AuthenticationDetails
                          (clj->js
                            {:Username email
                             :Password password}))]
    (go
      (let [success-chan (a/chan)
            err-chan (a/chan)]
        (.authenticateUser user
                           auth-details
                           (clj->js
                             {:onSuccess (fn [result]
                                           (go (a/>! success-chan result)))
                              :onFailure (fn [err]
                                           (go (a/>! err-chan err)))}))
        (let [[v ch] (a/alts! [success-chan err-chan])]
          (if (= ch success-chan)
            (session/put! :authenticated? true)
            (js/alert v)))))))

(defn authenticate-user
  "Check user's authentication status"
  []
  (let [u (.getCurrentUser user-pool)]
    (when (some? u)
      (go
        (let [success-chan (a/chan)
              err-chan (a/chan)]
          (.getSession u
                       (fn [err session]
                         (go
                           (cond
                             (some? err) (a/>! err-chan err)
                             :default (a/>! success-chan (-> session .getIdToken .getJwtToken))))))
          (let [[v ch] (a/alts! [success-chan err-chan])]
            (if (= ch success-chan)
              (do
                (session/put! :authenticated? true)
                (session/put! :authenticating? false))
              (js/alert v))))))))
;; -------------------------
;; Atoms

;; -------------------------
;; Components

(defn route-nav-item
  [props & content]
  (fn []
    (let [new-props (if (s/ends-with?
                          js/window.location.href
                          (get props :href))
                      (conj props {:class "active"})
                      props)
          view (session/get :current-page)]
      [nav-item new-props content])))

(defn navigation
  []
  [navbar {:fluid            true
           :collapseOnSelect true}
   [navbar-header
    [navbar-brand
     [:a {:href "/"} "Scratch"]]
    [navbar-toggle]]
   [navbar-collapse
    (if (session/get :authenticated?)
      [nav {:pullRight true}
       [nav-item {:on-click (fn [] (session/put! :authenticated? false))} "Logout"]]
      [nav {:pullRight true}
       [route-nav-item {:href "#/signup"} "Signup"]
       [route-nav-item {:href "#/login"} "Login"]])]])

(defn input-element
  "An input element which updates its value and on focus parameters on change, blur, and focus"
  ([id name type value]
   (input-element id name type value false))
  ([id name type value auto-focus]
   [:input {:id         id
            :name       name
            :class      "form-control"
            :auto-focus auto-focus
            :type       type
            :required   ""
            :value      @value
            :on-change  #(reset! value (-> % .-target .-value))}]))

(defn email-form
  [email-address-atom]
  (input-element "email"
                 "email"
                 "email"
                 email-address-atom
                 true))

(defn password-form
  [password-atom]
  (input-element "password"
                 "password"
                 "password"
                 password-atom
                 false))

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
       [:form {:on-submit (fn [event]
                            (.preventDefault event)
                            (login @email-address @password))}
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
  (authenticate-user)
  (when (not (session/get :authenticating?))
    [:div.App.container
     (navigation)
     [(session/get :current-page)]]))

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
  (session/put! :authenticated? false)
  (session/put! :authenticating? true)
  (mount-root))

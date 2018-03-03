(ns notes-app-frontend.components
  (:require
    [notes-app-frontend.aws-lib :as aws]
    [reagent.core :as r]
    [reagent.session :as session]
    [clojure.string :as s]
    [wilson.react-bootstrap :refer
     [navbar
      navbar-header
      navbar-brand
      navbar-toggle
      navbar-collapse
      nav
      nav-item]]))

(defn- handle-input-change
  "Resets atom associated with input based on input type"
  [event value type]
  (if (= type "file")
    (reset! value (get (-> event .target .files) 0))
    (reset! value (-> event .-target .-value))))

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
       [nav-item {:on-click #(aws/logout)} "Logout"]]
      [nav {:pullRight true}
       [route-nav-item {:href "#/signup"} "Signup"]
       [route-nav-item {:href "#/login"} "Login"]])]])

(defn input-element
  "An input element which updates its value and on focus parameters on change, blur, and focus"
  ([id name type value]
   (input-element id name type value false))
  ([id name type value auto-focus]
   [:div {:class "form-group form-group-lg"}
      [:label {:for id :class "control-label"} name]
      [:input {:id         id
               :class      "form-control"
               :auto-focus auto-focus
               :type       type
               :value      @value
               :on-change  #(handle-input-change % value type)}]]))

(defn email-form
  [email-address-atom]
  (input-element "email"
                 "Email"
                 "email"
                 email-address-atom
                 true))

(defn password-form
  [password-atom]
  (input-element "password"
                 "Password"
                 "password"
                 password-atom
                 false))

(defn confirm-password-form
  [confirm-password-atom]
  (input-element "confirmPassword"
                 "Confirm password"
                 "password"
                 confirm-password-atom
                 false))

(defn confirmation-form
  [confirmation-atom]
  (input-element "confirmationCode"
                 "Confirmation code"
                 "tel"
                 confirmation-atom
                 true))

(defn loader-button
  "Button with loader animation"
  []
  (let [this (r/current-component)
        props (r/props this)
        {:keys [loading? text loading-text disabled]} props
        updated-props (assoc-in props [:disabled] (or loading? disabled))
        new-props (dissoc updated-props :loading? :text :loading-text)]
    [:button.LoaderButton new-props
     (if loading? [:span.glyphicon.glyphicon-refresh.spinning])
     (if loading? loading-text text)]))

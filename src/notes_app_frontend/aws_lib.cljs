(ns notes-app-frontend.aws-lib
  (:require
    [notes-app-frontend.utils :as u]
    [cljs.core.async :as a :refer-macros [go]]
    [reagent.session :as session]
    [cljsjs.amazon-cognito-identity-js]))

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

(defn login
  "Authenticate user against AWS cognito"
  [email password loading-atom]
  (let [user (create-cognito-user email)
        auth-details (new AuthenticationDetails
                          (clj->js
                            {:Username email
                             :Password password}))]
    (go
      (let [success-chan (a/chan)
            err-chan (a/chan)]
        (reset! loading-atom true)
        (.authenticateUser user
                           auth-details
                           (clj->js
                             {:onSuccess (fn [result]
                                           (go (a/>! success-chan result)))
                              :onFailure (fn [err]
                                           (go (a/>! err-chan err)))}))
        (let [[v ch] (a/alts! [success-chan err-chan])]
          (if (= ch success-chan)
            (do
              (session/put! :authenticated? true)
              (reset! loading-atom false)
              (u/set-hash! ""))
            (js/alert v)))
        (a/close! success-chan)
        (a/close! err-chan)))))

(defn authenticate-user
  "Check user's authentication status"
  []
  (let [u (.getCurrentUser user-pool)]
    (when (some? u)
      (go
        (let [success-chan (a/chan)
              err-chan (a/chan)]
          (session/put! :authenticating? true)
          (.getSession u
                       (fn [err session]
                         (go
                           (cond
                             (some? err) (a/>! err-chan err)
                             :default (a/>! success-chan (-> session .getIdToken .getJwtToken))))))
          (let [[v ch] (a/alts! [success-chan err-chan])]
            (if (= ch success-chan)
              (session/put! :authenticated? true)
              (js/alert v)))
          (a/close! success-chan)
          (a/close! err-chan))))
    (session/put! :authenticating? false)))

(defn logout
  "Logout incognito session"
  []
  (let [u (.getCurrentUser user-pool)]
    (when (some? u)
      (.signOut u)
      (session/put! :authenticated? false)
      (u/set-hash! "/login"))))

(defn signup
  "Register a new cognito user"
  [email password cb]
  (let [attr-email (new CognitoUserAttribute
                      (clj->js {:Name "email" :Value email}))]
    (.signUp user-pool
             email
             password
             (to-array [attr-email]) nil
             (fn [err result]
               (if (some? err)
                 (cb (js->clj err :keywordize-keys true))
                 (cb (js->clj result :keywordize-keys true)))))))

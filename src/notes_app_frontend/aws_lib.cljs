(ns notes-app-frontend.aws-lib
  (:require
    [notes-app-frontend.utils :as u]
    [cljs.core.async :as a :refer-macros [go]]
    [reagent.session :as session]
    cljsjs.amazon-cognito-identity-js
    cljsjs.aws-sdk-js))

(def config
  {:cognito
   {:region           "us-east-1"
    :userPoolId       "us-east-1_G25s7dUDi"
    :userPoolClientID "1im6a36mes510oabud7cd7jte6"
    :identityPoolId   "us-east-1:599fa785-1b2d-4682-ad95-91fd32eba5d8"}})

(def authenticator (str "cognito-idp."
                        (get-in config [:cognito :region])
                        ".amazonaws.com/"
                        (get-in config [:cognito :userPoolId])))

(def CognitoUserPool (-> js/AmazonCognitoIdentity .-CognitoUserPool))
(def CognitoUserAttribute (-> js/AmazonCognitoIdentity .-CognitoUserAttribute))
(def CognitoUser (-> js/AmazonCognitoIdentity .-CognitoUser))
(def AuthenticationDetails (-> js/AmazonCognitoIdentity .-AuthenticationDetails))
(def AWSCognito js/AWSCognito)
(def AWSConfig (-> js/AWS .-config))
(def CognitoIdentityCredentials (-> js/AWS .-CognitoIdentityCredentials))

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

(defn callback-wrapper
  "Converts json result to map with keywords"
  [err result cb]
  (if (some? err)
    (cb (js->clj err :keywordize-keys true))
    (cb (js->clj result :keywordize-keys true))))

(defn get-user-token
  [current-user cb]
  (.getSession current-user
               (fn [err session]
                 (let [token (when (some? session)
                               (-> session .getIdToken .getJwtToken))]
                   (callback-wrapper err token cb)))))

(defn login
  "Authenticate user against AWS cognito"
  [email password cb]
  (let [user (create-cognito-user email)
        auth-details (new AuthenticationDetails
                          (clj->js
                            {:Username email
                             :Password password}))]
    (.authenticateUser user
                       auth-details
                       (clj->js
                         {:onSuccess #(callback-wrapper nil % cb)
                          :onFailure #(callback-wrapper % nil cb)}))))

(defn get-aws-credentials
  [user-token cb]
  (.update AWSConfig
           (clj->js
             {:region (get-in config [:cognito :region])}))
  (set! (.-credentials AWSConfig)
        (new CognitoIdentityCredentials
          (clj->js
            {:IdentityPoolId  (get-in config [:cognito :identityPoolId])
             :Logins          {}})))
  ;; Because clj->js doesn't support keywords with '/'
  ;; we need to set this value separately
  (aset (-> AWSConfig .-credentials .-params .-Logins) authenticator user-token)
  (-> AWSConfig .-credentials .getPromise
      (.then #(cb nil) #(cb %))))

(defn authenticate-user
  []
  (if (and (some? (.-credentials AWSConfig))
           (< (.now js/Date) (-> AWSConfig .-credentials .-expireTime (- 60000))))
    (session/put! :authenticated? true)
    (let [current-user (.getCurrentUser user-pool)]
      (if (some? current-user)
        (go
          (let [result (a/<! (u/<<< get-user-token current-user))]
            (if (instance? js/Error result)
              (js/alert result)
              (let [result (a/<! (u/<<< get-aws-credentials result))]
                (if (some? result)
                  (js/alert result)
                  (do
                    (session/put! :authenticated? true)
                    (session/put! :authenticating? false)))))))
        (session/put! :authenticating? false)))))

(defn logout
  "Logout incognito session"
  []
  (let [u (.getCurrentUser user-pool)]
    (when (some? u)
      (.signOut u))
    (session/put! :authenticated? false)
    (u/set-hash! "/login")))

(defn signup
  "Register a new cognito user"
  [email password cb]
  (let [attr-email (new CognitoUserAttribute
                      (clj->js {:Name "email" :Value email}))]
    (.signUp user-pool
             email
             password
             (to-array [attr-email]) nil
             #(callback-wrapper %1 %2 cb))))

(defn confirm
  "Verify confirmation code"
  [user confirmation-code cb]
  (.confirmRegistration user
                        confirmation-code
                        true
                        #(callback-wrapper %1 %2 cb)))

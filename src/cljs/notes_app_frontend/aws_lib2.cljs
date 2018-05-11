(ns notes-app-frontend.aws-lib2
  (:require
    [notes-app-frontend.utils :as u]
    [cljs.core.async :as a :refer-macros [go]]
    [reagent.session :as session]
    [goog.object :as gobj]))

(def config
  {:cognito
   {:region               "us-east-1"
    :user-pool-id         "us-east-1_G25s7dUDi"
    :app-client-id        "1im6a36mes510oabud7cd7jte6"
    :identity-pool-id     "us-east-1:599fa785-1b2d-4682-ad95-91fd32eba5d8"}
   :api-gateway
   {:region               "us-east-1"
    :url                  ""}
   :s3
   {:region               "us-east-1"
    :bucket               ""}})

(def amplify-config
  {:Auth
   {:mandatorySignIn true
    :region (get-in config [:cognito :region])
    :userPoolId (get-in config [:cognito :user-pool-id])
    :identityPoolId (get-in config [:cognito :identity-pool-id])
    :userPoolWebClientId (get-in config [:cognito :app-client-id])}
   :Storage
   {:region (get-in config [:s3 :region])
    :bucket (get-in config [:s3 :bucket])
    :identityPoolId (get-in config [:cognito :identity-pool-id])}
   :API
   {:endpoints
    [{:name "notes"
      :endpoint (get-in config [:api-gateway :url])
      :region (get-in config [:api-gateway :region])}]}})

(defn get-amplify
  []
  (let [amplify (gobj/get js/window "aws-amplify")]
    (-> amplify .-Auth (.configure (clj->js (amplify-config :Auth))))
    amplify))

(defonce amplify
  (get-amplify))

(defn callback-wrapper
  "Converts json result to map with keywords"
  [err result cb]
  (if (some? err)
    (cb err)
    (cb (js->clj result :keywordize-keys true))))

(defn login
  [email password cb]
  (-> amplify
      .-Auth
      (.signIn email password)
      (.then #(callback-wrapper nil % cb))
      (.catch #(cb (js/Error. (.stringify js/JSON %))))))

(defn authenticate-user
  []
  (let [c (a/chan)]
    (go
      (session/put! :authenticating? true)
      (-> amplify
          .-Auth
          .currentSession
          (.then (fn [result]
                   (session/put! :authenticated? true)
                   (a/put! c result)))
          (.catch (fn [err]
                    (a/put! c err))))
      (.log js/console "Session:" (a/<! c))
      (session/put! :authenticating? false)
      (a/close! c))))

(defn logout
  []
  (let [c (a/chan)]
    (go
      (-> amplify
          .-Auth
          .signOut
          (.then (fn []
                   (session/put! :authenticated? false)
                   (a/put! c "Done")))
          (.catch (fn [err]
                    (a/put! c err))))
      (.log js/console "Logout:" (a/<! c))
      (a/close! c)
      (u/set-hash! "/login"))))

(defn signup
  [email password cb]
  (-> amplify
      .-Auth
      (.signUp email password)
      (.then #(callback-wrapper nil % cb))
      (.catch #(cb (js/Error. (.stringify js/JSON %))))))

(defn confirm
  [user confirmation-code cb]
  (-> amplify
      .-Auth
      (.confirmSignUp user confirmation-code)
      (.then #(callback-wrapper nil % cb))
      (.catch #(cb (js/Error. (.stringify js/JSON %))))))

(ns notes-app-frontend.aws-lib2
  (:require
    [notes-app-frontend.utils :as u]
    [cljs.core.async :as a :refer-macros [go]]
    [clojure.string :as s]
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
    :url                  "https://uhawxc57sa.execute-api.us-east-1.amazonaws.com/prod"}
   :s3
   {:region               "us-east-1"
    :bucket               "notes-app-uploads-20171006"}})

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
    (-> amplify .-API (.configure (clj->js (amplify-config :API))))
    (-> amplify .-Storage (.configure (clj->js (amplify-config :Storage))))
    amplify))

(defonce amplify
  (get-amplify))

(def auth (.-Auth amplify))
(def api (.-API amplify))
(def storage (.-Storage amplify))

(defn callback-wrapper
  "Converts json result to map with keywords"
  [err result cb]
  (if (some? err)
    (cb err)
    (cb (js->clj result :keywordize-keys true))))

(defn login
  [email password cb]
  (-> auth
      (.signIn email password)
      (.then #(callback-wrapper nil % cb))
      (.catch #(cb (js/Error. (.stringify js/JSON %))))))

(defn authenticate-user
  []
  (let [c (a/chan)]
    (go
      (session/put! :authenticating? true)
      (-> auth
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
      (-> auth
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
  (-> auth
      (.signUp email password)
      (.then #(callback-wrapper nil % cb))
      (.catch #(cb (js/Error. (.stringify js/JSON %))))))

(defn confirm
  [user confirmation-code cb]
  (-> auth
      (.confirmSignUp user confirmation-code)
      (.then #(callback-wrapper nil % cb))
      (.catch #(cb (js/Error. (.stringify js/JSON %))))))

(defn create-note
  [note cb]
  (.log js/console (clj->js {:body note}))
  (-> api
      (.post "notes" "/notes" (clj->js {:body note}))
      (.then #(callback-wrapper nil % cb))
      (.catch #(cb (js/Error. (.stringify js/JSON %))))))

(defn s3-upload
  [file cb]
  (let [file-name (str (.now js/Date) "-" (.-name file))
        opts (clj->js {:contentType (.-type file)})]
    (-> storage
        .-vault
        (.put file-name file opts)
        (.then #(cb (-> % .-key)))
        (.catch #(cb (js/Error. (.stringify js/JSON %)))))))

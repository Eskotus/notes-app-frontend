(ns notes_app_frontend.views.new-note-page
  (:require
    [notes-app-frontend.utils :as u]
    [notes-app-frontend.components :as c]
    [notes-app-frontend.aws-lib2 :as aws]
    [reagent.core :as r]
    [reagent.session :as session]
    [clojure.string :as s]
    [cljs.core.async :as a :refer-macros [go]]))

(def max-attachment-size 5000000)

(defn handle-submit
  [event loading? content file]
  (.preventDefault event)
  (if (and (some? @file)
           (> (.-size @file) max-attachment-size))
    (js/alert "Please pick a file smaller than 5MB")
    (do
      (reset! loading? true)
      (go
        (let [attachment-result (a/<! (u/<<< aws/s3-upload @file))]
          (if (instance? js/Error attachment-result)
            (js/alert attachment-result)
            (let [note (clj->js {:content @content
                                 :attachment attachment-result})
                  result (a/<! (u/<<< aws/create-note note))]
              (if (instance? js/Error result)
                (js/alert result)
                (u/set-hash! "/"))))
          (reset! loading? false))))))

(defn validate-form
  [content]
  (s/blank? @content))

(defn render
  []
  (if (= (session/get :authenticated?) true)
    (let [content   (r/atom nil)
          file      (r/atom nil)
          loading?  (r/atom false)]
      (fn []
        [:div.NewNote
         [:form {:on-submit #(handle-submit % loading? content file)}
          [:div.form-group
           [:textarea {:id         "content"
                       :class      "form-control"
                       :value      @content
                       :on-change  #(reset! content (-> % .-target .-value))}]]
          [:div.form-group
           [:label {:for "file" :class "control-label"} "Attachment"]
           [:input {:id         "file"
                    :type       "file"
                    :on-change  #(reset! file (-> % .-target .-files (aget 0)))}]]
          [c/loader-button {:class        "btn btn-primary btn-lg btn-block"
                            :loading?     @loading?
                            :loading-text "Creating..."
                            :text         "Create"
                            :type         "submit"
                            :disabled     (validate-form content)}]]]))
    (fn []
      (u/set-hash!
        (str "/login?redirect="
             (.-hash js/location)))
      [:div])))
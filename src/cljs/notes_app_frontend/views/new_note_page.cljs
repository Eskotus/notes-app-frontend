(ns notes_app_frontend.views.new-note-page
  (:require
    [notes_app_frontend.utils :as u]
    [notes-app-frontend.components :as c]
    [reagent.core :as r]
    [clojure.string :as s]
    [reagent.session :as session]
    [cljs.core.async :as a :refer-macros [go]]))

(def max-attachment-size 5000000)

(defn handle-submit
  [event loading? content file]
  (.preventDefault event)
  (if (> (.-size @file) max-attachment-size)
    (js/alert "Please pick a file smaller than 5MB")
    (reset! loading? true)))

(defn validate-form
  [content]
  (s/blank? @content))

(defn render
  []
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
        [c/loader-button {:class        "btn btn-default btn-lg btn-block"
                          :loading?     @loading?
                          :loading-text "Creating..."
                          :text         "Create"
                          :type         "submit"
                          :disabled     (validate-form content)}]]])))

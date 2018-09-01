(ns notes_app_frontend.views.note-page
  (:require
    [notes-app-frontend.aws-lib2 :as aws]
    [notes-app-frontend.utils :as u]
    [notes-app-frontend.components :as c]
    [reagent.core :as r]
    [clojure.string :as s]
    [cljs.core.async :as a :refer-macros [go]]))

(def max-attachment-size 5000000)

(defn handle-save-submit
  [event loading? note-atom content-atom file-atom id]
  (prn id)
  (.preventDefault event)
  (if (and (some? @file-atom)
           (> (.-size @file-atom) max-attachment-size))
    (js/alert "Please pick a file smaller than 5MB")
    (do
      (reset! loading? true)
      (go
        (let [attachment-result (a/<! (u/<<< aws/s3-upload @file-atom))]
          (if (instance? js/Error attachment-result)
            (js/alert attachment-result)
            (let [note (clj->js {:content @content-atom
                                 :attachment (if (some? attachment-result)
                                               attachment-result
                                               (:attachment @note-atom))})
                  result (a/<! (u/<<< aws/save-note id note))]
              (if (instance? js/Error result)
                (js/alert result)
                (u/set-hash! "/"))))
          (reset! loading? false))))))

(defn handle-delete
  [event id deleting?]
  (let [confirmed (.confirm js/window "Are you sure you want to delete this note?")]
    (when confirmed
      (reset! deleting? true)
      (go
        (let [result (a/<! (u/<<< aws/delete-note id))]
          (if (instance? js/Error result)
            (js/alert result)
            (u/set-hash! "/")))
        (reset! deleting? false)))))

(defn validate-form
  [content-atom]
  (s/blank? @content-atom))

(defn format-file-name
  [file-name]
  (s/replace file-name #"^\w+-" ""))

(defn render
  [id]
  (let [note-atom           (r/atom nil)
        content-atom        (r/atom "")
        attachment-url-atom (r/atom nil)
        file-atom           (r/atom nil)
        loading?             (r/atom false)
        deleting?           (r/atom false)]
    (go
      (let [note (a/<! (u/<<< aws/get-note id))]
        (if (instance? js/Error note)
          (js/alert note)
          (do
            (when some? (:attachment note)
              (let [attachment-url (a/<! (u/<<< aws/s3-get (:attachment note)))]
                (if (instance? js/Error attachment-url)
                  (js/alert attachment-url)
                  (reset! attachment-url-atom attachment-url))))
            (reset! note-atom note)
            (reset! content-atom (:content note))))))
    (fn []
      [:div.Notes
       (when (some? @note-atom)
         [:form {:on-submit #(handle-save-submit % loading? note-atom content-atom file-atom id)}
          [:div.form-group
           [:textarea {:id         "content"
                       :class      "form-control"
                       :value      @content-atom
                       :on-change  #(reset! content-atom (-> % .-target .-value))}]]
          (when (some? (:attachment @note-atom))
            [:div.form-group
             [:label {:for "file" :class "control-label"} "Attachment"]
             [:p.form-control-static
              [:a {:target  "_blank"
                   :rel     "noopener noreferrer"
                   :href    @attachment-url-atom}
               (format-file-name (:attachment @note-atom))]]])
          [:div.form-group
           (when (not (some? (:attachment @note-atom)))
             [:label {:for "file" :class "control-label"} "Attachment"])
           [:input {:id         "file"
                    :type       "file"
                    :on-change  #(reset! file-atom (-> % .-target .-files (aget 0)))}]]
          [c/loader-button {:class        "btn btn-primary btn-lg btn-block"
                            :loading?     @loading?
                            :loading-text "Saving..."
                            :text         "Save"
                            :type         "submit"
                            :disabled     (validate-form content-atom)}]
          [c/loader-button {:class        "btn btn-danger btn-lg btn-block"
                            :loading?     @deleting?
                            :loading-text "Deleting..."
                            :text         "Delete"
                            :on-click     #(handle-delete % id deleting?)
                            :type         "button"}]])])))

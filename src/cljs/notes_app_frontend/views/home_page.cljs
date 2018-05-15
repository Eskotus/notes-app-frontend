(ns notes_app_frontend.views.home-page
  (:require
    [notes_app_frontend.utils :as u]
    [notes-app-frontend.aws-lib2 :as aws]
    [clojure.string :as s]
    [reagent.session :as session]
    [reagent.core :as r]
    [cljs.core.async :as a :refer-macros [go]]))

(defn handle-click
  [event]
  nil)

(defn render-notes-list
  [notes]
  (let [cont-notes (into [{:noteId "new" :content "\uFF0B Create a new note" :createdAt (.now js/Date)}] notes)]
    [:div.list-group
      (for [note cont-notes]
        [:a.list-group-item {:key (:noteId note)
                             :href (str "/#/notes/" (:noteId note))}
         [:h4.list-group-item-heading (first (s/split-lines (s/trim (:content note))))]
         (when (not (= "new" (:noteId note)))
           [:p.list-group-item-text (str "Created: " (.toLocaleString (js/Date. (:createdAt note))))])])]))

(defn render-lander
  []
  [:div.lander
   [:h1 "Scratch"]
   [:p "A simple note taking app"]])

(defn render-notes
  []
  (let [loading? (r/atom true)
        notes-atom (r/atom nil)]
    (go
      (let [result (a/<! (u/<<< aws/get-notes))]
        (if (instance? js/Error result)
          (js/alert result)
          (reset! notes-atom result)))
      (reset! loading? false))
    (fn []
      [:div.notes
       [:div.page-header
        [:h1 "Your Notes"]]
       (when (not @loading?)
         [render-notes-list @notes-atom])])))

(defn render []
  [:div.Home
   (if (= (session/get :authenticated?) true)
     [render-notes]
     [render-lander])])

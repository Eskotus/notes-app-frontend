(ns notes-app-frontend.prod
  (:require
    [notes-app-frontend.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)

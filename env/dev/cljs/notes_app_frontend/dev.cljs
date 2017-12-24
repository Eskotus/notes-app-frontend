(ns ^:figwheel-no-load notes-app-frontend.dev
  (:require
    [notes-app-frontend.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)

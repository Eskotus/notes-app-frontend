(defproject notes-app-frontend "0.0.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [reagent-forms "0.5.35"]
                 [secretary "1.2.3"]
                 [wilson "0.33.1"]
                 [org.clojure/core.async "0.4.474"]
                 [thheller/shadow-cljs "2.6.6"]]

  :min-lein-version "2.5.0"

  :source-paths ["src/clj" "src/cljs"])

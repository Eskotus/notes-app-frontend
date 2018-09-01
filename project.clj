(defproject notes-app-frontend "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [reagent-forms "0.5.35"]
                 [secretary "1.2.3"]
                 [wilson "0.33.1"]
                 [cljsjs/amazon-cognito-identity-js "1.21.0-0"]
                 [cljsjs/aws-sdk-js "2.94.0-0"]
                 [org.clojure/core.async "0.4.474"]]
                 

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.14"]]

  :min-lein-version "2.5.0"

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :resource-paths ["public"]

  :figwheel {:http-server-root "."
             :nrepl-port 7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs ["public/css"]}

  :cljsbuild {:builds {:app
                       {:source-paths ["src/clj" "src/cljs" "env/dev/cljs"]
                        :compiler
                        {:main "notes-app-frontend.dev"
                         :install-deps true
                         :output-to "public/js/app.js"
                         :output-dir "public/js/out"
                         :asset-path   "js/out"
                         :source-map true
                         :optimizations :none
                         :pretty-print  true
                         :verbose       true}
                        :figwheel
                        {:on-jsload "notes-app-frontend.core/mount-root"
                         :open-urls ["http://localhost:3449/"]}}
                       :release
                       {:source-paths ["src/clj" "src/cljs" "env/prod/cljs"]
                        :compiler
                        {:output-to "public/js/app.js"
                         :output-dir "public/js/release"
                         :asset-path   "js/out"
                         :optimizations :advanced
                         :pretty-print false}}}}

  :aliases {"package" ["do" "clean" ["cljsbuild" "once" "release"]]}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.7"]
                                  [figwheel-sidecar "0.5.14"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [com.cemerick/piggieback "0.2.2"]]}}

  :source-paths [])

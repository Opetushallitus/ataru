(defproject lomake-editori "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ; clojurescript
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]
                 [re-frame "0.7.0"]
                 [re-com "0.8.0"]
                 [secretary "1.2.3"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]

                 ;clojure/clojurescript
                 [prismatic/schema "1.0.5"]
                 [com.taoensso/timbre "4.3.1"]
                 [org.clojure/core.match "0.3.0-alpha4"]

                 ;clojure
                 [compojure "1.5.0"]
                 [metosin/compojure-api "1.0.1"]
                 [ring "1.4.0"]
                 [yesql "0.5.2"]
                 [camel-snake-kebab "0.3.2"]
                 [environ "1.0.2"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [postgresql/postgresql "9.1-901-1.jdbc4"]
                 [clj-time "0.11.0"]
                 [cider/cider-nrepl "0.12.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                 [cheshire/cheshire "5.5.0"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["src/sql" "resources"]
  :uberjar-name "lomake-editori.jar"

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.0-6"]
            [lein-doo "0.1.6"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.8"]
            [lein-environ "1.0.2"]]

  :eastwood {:namespaces [:source-paths]
             :add-linters [:unused-locals
                           :unused-namespaces
                           :unused-private-vars]}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler lomake-editori.handler/handler
             :server-port 3449}

  :doo {:paths {:phantom "./node_modules/phantomjs-prebuilt/bin/phantomjs"}}

  :less {:source-paths ["resources/less"]
         :target-path  "resources/public/css/compiled"}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "lomake-editori.core/mount-root"}
                        :compiler {:main lomake-editori.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/test.js"
                                   :main lomake-editori.runner
                                   :optimizations :none}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main lomake-editori.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :init (set! *print-length* 50)}
  :profiles {:repl {:plugins [[cider/cider-nrepl "0.12.0-SNAPSHOT" :exclusions [org.clojure/clojure]]]}
             :dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-2"]
                                  [refactor-nrepl "2.2.0"]
                                  [org.clojure/tools.nrepl "0.2.12"]]
                   :plugins [[jonase/eastwood "0.2.3" :exclusions [org.clojure/clojure]]]
                   :source-paths ["env/dev/clj"]
                   :env {:dev? true}}})




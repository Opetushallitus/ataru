(defproject ataru "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.9.293"]
                 [reagent "0.6.0"]   ; react in clojure
                 [re-frame "0.8.0"]  ; flux for re-agent
                 [secretary "1.2.3"] ; routing
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [oph/soresu "0.1.7-SNAPSHOT"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cljs-ajax "0.5.8"]
                 [binaryage/devtools "0.8.3"]
                 [re-frisk "0.2.2"] ; will only be used in development side
                 [venantius/accountant "0.1.7"]
                 [com.cemerick/url "0.1.1"]

                 ;clojure/clojurescript
                 [prismatic/schema "1.1.3"]
                 [com.taoensso/timbre "4.7.4"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [metosin/schema-tools "0.9.0"]
                 [jayq "2.5.4"]

                 ;clojure
                 [compojure "1.5.0"]
                 [crypto-random "1.2.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [metosin/compojure-api "1.1.2"]
                 [com.stuartsierra/component "0.3.1"]
                 [aleph "0.4.1"]
                 [http-kit "2.1.18"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring/ring-json "0.4.0"]
                 [ring-ratelimit "0.2.2"]
                 [bk/ring-gzip "0.1.1"]
                 [buddy/buddy-auth "0.13.0"]
                 [yesql "0.5.2"]
                 [org.flywaydb/flyway-core "3.2.1"]
                 [camel-snake-kebab "0.3.2"]
                 [environ "1.0.2"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [clj-time "0.11.0"]
                 [cider/cider-nrepl "0.15.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                 [cheshire/cheshire "5.5.0"]
                 [selmer "1.0.4"]
                 [metosin/ring-http-response "0.6.5"]
                 [oph/clj-util "0.1.0"]
                 [ring.middleware.logger "0.5.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.apache.poi/poi-ooxml "3.15-beta1"]
                 [org.clojars.pntblnk/clj-ldap "0.0.12"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/tools.nrepl "0.2.12"]]

  :min-lein-version "2.5.3"

  :repositories [["releases" {:url "https://artifactory.oph.ware.fi/artifactory/oph-sade-release-local"
                              :sign-releases false
                              :snapshots false
;                             :creds :gpg
}]
                 ["snapshots"      {:url "https://artifactory.oph.ware.fi/artifactory/oph-sade-snapshot-local"
;                                   :creds :gpg
                                     }]
                 ["ext-snapshots"  {:url "https://artifactory.oph.ware.fi/artifactory/ext-snapshot-local"}]
                 ["Laughing Panda" {:url "http://maven.laughingpanda.org/maven2"
                                    :snapshots false}]]

  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["spec"]
  :resource-paths ["src/sql" "resources"]
  :uberjar-name "ataru.jar"
  :jvm-opts ^:replace ["-Xmx2g"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]
            [lein-figwheel "0.5.8"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.8"]
            [lein-environ "1.0.2"]
            [lein-resource "15.10.2"]
            [speclj "3.3.0"]]

  :doo {:paths {:phantom "./node_modules/phantomjs-prebuilt/bin/phantomjs"}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "resources/public/css"
                                    "target"
                                    "test/js"
                                    "resources/public/js/test"
                                    "out"]

  :auto-clean false

  :figwheel {:css-dirs ["resources/public/css"]}

  :less {:source-paths ["resources/less"]
         :target-path  "resources/public/css/compiled"}

  :main ataru.core

  :aot [com.stuartsierra.dependency ataru.db.migrations]

  :cljsbuild {:builds [{:id           "virkailija-dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     {:on-jsload "ataru.virkailija.core/mount-root"}
                        :compiler     {:main                 "ataru.virkailija.core"
                                       :output-to            "resources/public/js/compiled/virkailija-app.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :asset-path           "/lomake-editori/js/compiled/out"
                                       :optimizations        :none
                                       :source-map-timestamp true}}

                       {:id           "hakija-dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel      {:on-jsload "ataru.hakija.core/mount-root"}
                        :compiler {:main                 "ataru.hakija.core"
                                   :output-to            "resources/public/js/compiled/hakija-app.js"
                                   :output-dir           "resources/public/js/compiled/hakija-out"
                                   :asset-path           "/hakemus/js/compiled/hakija-out"
                                   :optimizations        :none
                                   :source-map-timestamp true}}

                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs/unit" "src/cljc" "test/cljc/unit"]
                        :compiler {:output-to "resources/public/js/test/test.js"
                                   :main "ataru.unit-runner"
                                   :optimizations :none}}

                       {:id "virkailija-min"
                        :jar true
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler {:main "ataru.virkailija.core"
                                   :output-to "resources/public/js/compiled/virkailija-app.js"
                                   :output-dir "resources/public/js/compiled/virkailija-app-out"
                                   :externs ["resources/virkailija-externs.js"]
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :source-map "resources/public/js/compiled/virkailija-app.js.map"
                                   :source-map-timestamp true
                                   :pretty-print false}}

                       {:id "hakija-min"
                        :jar true
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler {:main "ataru.hakija.core"
                                   :output-to "resources/public/js/compiled/hakija-app.js"
                                   :output-dir "resources/public/js/compiled/hakija-app-out"
                                   :externs ["resources/hakija-externs.js"]
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :source-map "resources/public/js/compiled/hakija-app.js.map"
                                   :source-map-timestamp true
                                   :pretty-print false}}]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :init (set! *print-length* 50)
                 :init-ns user}

  :resource {:resource-paths ["templates"]
             :target-path "resources/public"
             :update false ;; if true only process files with src newer than dest
             :extra-values {:version "0.1.0-SNAPSHOT"
                            :buildTime ~(.format
                                          (java.text.SimpleDateFormat. "yyyyMMdd-HHmm")
                                          (java.util.Date.) )}
             :silent false}

  :profiles {:repl {:plugins [[cider/cider-nrepl "0.15.0-SNAPSHOT" :exclusions [org.clojure/clojure]]]}
             :dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.8"]
                                  [refactor-nrepl "2.2.0"]
                                  [snipsnap "0.1.0" :exclusions [org.clojure/clojure]]
                                  [reloaded.repl "0.2.1"]
                                  [ring/ring-mock "0.3.0"]
                                  [speclj "3.3.2"]
                                  [speclj-junit "0.0.10"]]
                   :plugins [[refactor-nrepl "2.2.0"]
                             [cider/cider-nrepl "0.15.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                             [lein-cljfmt "0.5.1"]]
                   :source-paths ["dev/clj" "test/cljc/unit"]
                   :resource-paths ["dev-resources"]
                   :env {:dev? true}}

             :virkailija-dev [:dev {:figwheel {:nrepl-port  3334
                                               :server-port 3449}}]
             :hakija-dev [:dev {:figwheel {:nrepl-port  3336
                                           :server-port 3450}}]
             :uberjar {:aot :all
                       :resource-paths ["resources"]}}
  :aliases {"virkailija-dev" ["with-profile" "virkailija-dev" "run" "virkailija"]
            "hakija-dev" ["with-profile" "hakija-dev" "run" "hakija"]
            "figwheel-virkailija" ["with-profile" "virkailija-dev" "figwheel" "virkailija-dev"]
            "figwheel-hakija" ["with-profile" "hakija-dev" "figwheel" "hakija-dev"]})




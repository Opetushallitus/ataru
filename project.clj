(defproject ataru "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.9.946"]
                 [reagent "0.8.0-alpha1"]
                 [re-frame "0.10.2"]
                 [secretary "1.2.3"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cljs-ajax "0.7.3"]
                 [binaryage/devtools "0.9.8"]
                 [re-frisk "0.5.3"]
                 [venantius/accountant "0.2.3"]
                 [com.cemerick/url "0.1.1"]

                 ;clojure/clojurescript
                 [prismatic/schema "1.1.7"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [metosin/schema-tools "0.9.1"]
                 [medley "1.0.0"]
                 [markdown-clj "1.0.1"]

                 ;clojure
                 [compojure "1.6.0"]
                 [com.github.fge/json-patch "1.9"]
                 [com.stuartsierra/component "0.3.2"]
                 [metosin/compojure-api "1.1.11"]
                 [aleph "0.4.4"]
                 [fi.vm.sade/auditlogger "5.0.0-SNAPSHOT"]
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [http-kit "2.2.0"]
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-ratelimit "0.2.2"]
                 [bk/ring-gzip "0.2.1"]
                 [buddy/buddy-auth "2.1.0"]
                 [yesql "0.5.3"]
                 ; Flyway 4 breaks our migrations
                 [org.flywaydb/flyway-core "3.2.1" :upgrade false]
                 [camel-snake-kebab "0.4.0"]
                 [environ "1.1.0"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [org.postgresql/postgresql "42.1.4"]
                 [clj-time "0.14.2"]
                 [cheshire/cheshire "5.8.0"]
                 [selmer "1.11.3"]
                 [metosin/ring-http-response "0.9.0"]
                 ;; These two explicit dependencies are required to force
                 ;; newer, fixed versions of those which come with Scala Cas Client
                 ;; Used by clj-util below. Without these, we would not be able to
                 ;; authenticate to /oppijanumerorekisteri-service, we would just get:
                 ;; BadResponse Response lacks status Reason  [trace missing]
                 ;; We can't upgrade these either. Looks like Cas requires a specific
                 ;; version, and it's this one.
                 [org.http4s/blaze-http_2.11 "0.10.1" :upgrade false]
                 [org.http4s/http4s-json4s-native_2.11 "0.10.1" :upgrade false]
                 ;; And naturally this exclusion is important as well
                 [oph/clj-util "0.1.0" :exclusions [org.http4s/blaze-http_2.11]]
                 [ring.middleware.logger "0.5.0" :exclusions [onelog]] ; Remove :exclusions and onelog dependency below when updating if included onelog works with clojure 1.9.0
                 [onelog "0.5.0"]
                 [ring/ring-session-timeout "0.2.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.apache.poi/poi-ooxml "3.17"]
                 [org.clojars.pntblnk/clj-ldap "0.0.15"]
                 [org.clojure/core.cache "0.6.5"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [com.hazelcast/hazelcast "3.9.1"]
                 [com.taoensso/carmine "2.16.0"]
                 [pandect "0.6.1"]
                 [hikari-cp "2.0.0" :exclusions [prismatic/schema]]
                 [ring/ring-mock "0.3.2"]
                 [speclj "3.3.2"]]

  :min-lein-version "2.5.3"

  :repositories [["releases" {:url           "https://artifactory.oph.ware.fi/artifactory/oph-sade-release-local"
                              :sign-releases false
                              :snapshots     false
                              ;                             :creds :gpg
                              }]
                 ["snapshots" {:url "https://artifactory.oph.ware.fi/artifactory/oph-sade-snapshot-local"
                               ;                                   :creds :gpg
                               }]
                 ["ext-snapshots" {:url "https://artifactory.oph.ware.fi/artifactory/ext-snapshot-local"}]
                 ["Laughing Panda" {:url       "http://maven.laughingpanda.org/maven2"
                                    :snapshots false}]]

  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["spec"]
  :resource-paths ["src/sql" "resources"]
  :uberjar-name "ataru.jar"
  :jvm-opts ^:replace ["-Xmx2g"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]
            [lein-figwheel "0.5.14"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.14"]
            [lein-environ "1.1.0"]
            [lein-resource "17.06.1"]
            [speclj "3.3.2"]]

  :doo {:paths {:phantom "./node_modules/phantomjs-prebuilt/bin/phantomjs"}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "resources/public/css"
                                    "target"
                                    "target-virkailija"
                                    "target-hakija"
                                    "test/js"
                                    "resources/public/js/test"
                                    "out"]

  :auto-clean false

  :figwheel {:css-dirs ["resources/public/css"]}

  :less {:source-paths ["resources/less"]
         :target-path  "resources/public/css/compiled"}

  :main ataru.core

  :aot [com.stuartsierra.dependency com.stuartsierra.component ataru.db.migrations]

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
                        :figwheel     {:on-jsload "ataru.hakija.core/mount-root"}
                        :compiler     {:main                 "ataru.hakija.core"
                                       :output-to            "resources/public/js/compiled/hakija-app.js"
                                       :output-dir           "resources/public/js/compiled/hakija-out"
                                       :asset-path           "/hakemus/js/compiled/hakija-out"
                                       :optimizations        :none
                                       :source-map-timestamp true}}

                       {:id           "test"
                        :source-paths ["src/cljs" "test/cljs/unit" "src/cljc" "test/cljc/unit"]
                        :compiler     {:output-to     "resources/public/js/test/test.js"
                                       :main          "ataru.unit-runner"
                                       :process-shim  false
                                       :optimizations :none}}

                       {:id           "virkailija-min"
                        :jar          true
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main                 "ataru.virkailija.core"
                                       :output-to            "resources/public/js/compiled/virkailija-app.js"
                                       :output-dir           "resources/public/js/compiled/virkailija-app-out"
                                       :externs              ["resources/virkailija-externs.js"]
                                       :optimizations        :advanced
                                       :closure-defines      {goog.DEBUG false}
                                       :source-map           "resources/public/js/compiled/virkailija-app.js.map"
                                       :source-map-timestamp true
                                       :pretty-print         false}}

                       {:id           "hakija-min"
                        :jar          true
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main                 "ataru.hakija.core"
                                       :output-to            "resources/public/js/compiled/hakija-app.js"
                                       :output-dir           "resources/public/js/compiled/hakija-app-out"
                                       :externs              ["resources/hakija-externs.js"]
                                       :optimizations        :advanced
                                       :closure-defines      {goog.DEBUG false}
                                       :source-map           "resources/public/js/compiled/hakija-app.js.map"
                                       :source-map-timestamp true
                                       :pretty-print         false}}]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :init             (set! *print-length* 50)
                 :init-ns          user}

  :resource {:resource-paths ["templates"]
             :target-path    "resources/public"
             :update         false                          ;; if true only process files with src newer than dest
             :extra-values   {:version   "0.1.0-SNAPSHOT"
                              :buildTime ~(.format
                                            (java.text.SimpleDateFormat. "yyyyMMdd-HHmm")
                                            (java.util.Date.))
                              :githash   ~(System/getenv "githash")}
             :silent         false}

  :profiles {:dev            {:dependencies   [[com.cemerick/piggieback "0.2.2"]
                                               [figwheel-sidecar "0.5.14"]
                                               [refactor-nrepl "2.3.1"]
                                               [snipsnap "0.2.0" :exclusions [org.clojure/clojure]]
                                               [reloaded.repl "0.2.4"]
                                               [speclj-junit "0.0.11-20151116.130002-1"]
                                               [criterium "0.4.4"]]
                              :plugins        [[refactor-nrepl "2.3.1"]
                                               [lein-cljfmt "0.5.7"]
                                               [lein-kibit "0.1.6"]]
                              :source-paths   ["dev/clj" "test/cljc/unit" "spec"]
                              :resource-paths ["dev-resources"]
                              :env            {:dev? "true"}}

             :virkailija-dev [:dev {:figwheel {:nrepl-port  3334
                                               :server-port 3449}
                                    :target-path "target-virkailija"
                                    :env      {:app "virkailija"}
                                    :jvm-opts ^:replace ["-Dapp=virkailija"
                                                         "-Duser.home=."]}]
             :hakija-dev     [:dev {:figwheel {:nrepl-port  3336
                                               :server-port 3450}
                                    :target-path "target-hakija"
                                    :env      {:app "hakija"}
                                    :jvm-opts ^:replace ["-Dapp=hakija"
                                                         "-Duser.home=."]}]
             :uberjar        {:aot            :all
                              :resource-paths ["resources"]}}

  :aliases {"virkailija-dev"      ["with-profile" "virkailija-dev" "run" "virkailija"]
            "hakija-dev"          ["with-profile" "hakija-dev" "run" "hakija"]
            "figwheel-virkailija" ["with-profile" "virkailija-dev" "figwheel" "virkailija-dev"]
            "figwheel-hakija"     ["with-profile" "hakija-dev" "figwheel" "hakija-dev"]
            "anonymize-data"      ["with-profile" "dev" "run" "-m" "ataru.anonymizer.core/anonymize-data"]
            "db-schema"           ["with-profile" "dev" "run" "-m" "ataru.scripts.generate-schema-diagram"]})




(defproject ataru "0.1.0-SNAPSHOT"
  :managed-dependencies [[com.fasterxml.jackson.core/jackson-core "2.9.8"]
                         [com.fasterxml.jackson.core/jackson-databind "2.9.8"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.9.8"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.9.8"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.9.8"]
                         [com.github.fge/jackson-coreutils "1.8"]
                         [ring-middleware-format "0.7.4"]
                         [org.clojure/clojurescript "1.10.520"]
                         [org.clojure/tools.reader "1.3.2"]
                         [com.cognitect/transit-clj "0.8.313"]
                         [org.apache.httpcomponents/httpcore "4.4.11"]
                         [com.taoensso/encore "2.99.0"]
                         [ring/ring-core "1.7.1"]
                         [ring/ring-codec "1.1.1"]
                         [com.google.code.gson/gson "2.7"]
                         [com.google.code.findbugs/jsr305 "3.0.2"]
                         [potemkin "0.4.5"]
                         [org.slf4j/slf4j-api "1.7.26"]
                         [commons-codec "1.12"]
                         [riddley "0.1.14"]
                         [instaparse "1.4.10"]]
  :dependencies [[org.clojure/clojure "1.10.0"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [clj-commons/secretary "1.2.4"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cljs-ajax "0.8.0"]
                 [binaryage/devtools "0.9.10"]
                 [re-frisk "0.5.4.1"]
                 [venantius/accountant "0.2.4"]
                 [com.cemerick/url "0.1.1"]

                 ;clojure/clojurescript
                 [prismatic/schema "1.1.10"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/core.match "0.3.0"]
                 [metosin/schema-tools "0.11.0"]
                 [medley "1.1.0"]
                 [markdown-clj "1.0.7"]

                 ;clojure
                 [compojure "1.6.1"]
                 [com.github.fge/json-patch "1.9"]
                 [com.stuartsierra/component "0.4.0"]
                 [metosin/compojure-api "1.1.12"]
                 [aleph "0.4.6"]
                 [fi.vm.sade/auditlogger "8.3.0-SNAPSHOT"]
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [http-kit "2.4.0-alpha4"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.4.0"]
                 [ring-ratelimit "0.2.2"]
                 [bk/ring-gzip "0.3.0"]
                 [buddy/buddy-auth "2.1.0"]
                 [yesql "0.5.3"]
                 ; Flyway 4 breaks our migrations
                 [org.flywaydb/flyway-core "3.2.1" :upgrade false]
                 [camel-snake-kebab "0.4.0"]
                 [environ "1.1.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.postgresql/postgresql "42.2.5"]
                 [clj-time "0.15.1"]
                 [cheshire/cheshire "5.8.1"]
                 [selmer "1.12.12"]
                 [metosin/ring-http-response "0.9.1"]
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
                 [org.apache.poi/poi-ooxml "3.17"]
                 [org.clojure/core.cache "0.7.1"]
                 [nrepl "0.4.5"]
                 [com.taoensso/carmine "2.16.0"]
                 [org.apache.poi/poi-ooxml "4.1.0"]
                 [org.clojure/core.cache "0.7.2"]
                 [nrepl "0.6.0"]
                 [com.taoensso/carmine "2.19.1"]
                 [pandect "0.6.1"]
                 [hikari-cp "2.7.1"]
                 [ring/ring-mock "0.3.2"]
                 [speclj "3.3.2"]
                 [org.clojure/test.check "0.9.0"]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "20190325.1" :exclusions [com.google.guava/guava]]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.536"]
                 [com.amazonaws/aws-java-sdk-sns "1.11.536"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.536"]
                 [com.github.ben-manes.caffeine/caffeine "2.7.0"]
                 [clj-http "3.9.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [com.jcraft/jsch "0.1.55"]]

  :min-lein-version "2.5.3"

  :repositories [["releases" {:url           "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"
                              :sign-releases false
                              :snapshots     false
                              ;                             :creds :gpg
                              }]
                 ["snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"
                               ;                                   :creds :gpg
                               }]
                 ["ext-snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/ext-snapshot-local"}]]

  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["spec"]
  :resource-paths ["src/sql" "resources"]
  :uberjar-name "ataru.jar"
  :jvm-opts ^:replace ["-Xmx2g"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.10"]
            [lein-figwheel "0.5.14"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.14"]
            [lein-environ "1.1.0"]
            [lein-resource "17.06.1"]
            [speclj "3.3.2"]]

  :doo {:debug true
        :paths {:karma "./node_modules/karma/bin/karma"}}

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
                                       :preloads             [devtools.preload]
                                       :output-to            "resources/public/js/compiled/virkailija-app.js"
                                       :output-dir           "resources/public/js/compiled/out"
                                       :asset-path           "/lomake-editori/js/compiled/out"
                                       :parallel-build       true
                                       :optimizations        :none
                                       :source-map-timestamp true}}

                       {:id           "hakija-dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     {:on-jsload "ataru.hakija.core/mount-root"}
                        :compiler     {:main                 "ataru.hakija.core"
                                       :preloads             [devtools.preload]
                                       :output-to            "resources/public/js/compiled/hakija-app.js"
                                       :output-dir           "resources/public/js/compiled/hakija-out"
                                       :asset-path           "/hakemus/js/compiled/hakija-out"
                                       :parallel-build       true
                                       :optimizations        :none
                                       :source-map-timestamp true}}

                       {:id           "test"
                        :source-paths ["src/cljs" "test/cljs/unit" "src/cljc" "test/cljc/unit"]
                        :compiler     {:output-to     "resources/public/js/test/test.js"
                                       :main          "ataru.unit-runner"
                                       :parallel-build       true
                                       :process-shim  false
                                       :optimizations :none}}

                       {:id           "virkailija-min"
                        :jar          true
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main                 "ataru.virkailija.core"
                                       :output-to            "resources/public/js/compiled/virkailija-app.js"
                                       :output-dir           "resources/public/js/compiled/virkailija-app-out"
                                       :externs              ["resources/virkailija-externs.js"]
                                       :parallel-build       true
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
                                       :parallel-build       true
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
                                            (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZZZ")
                                            (java.util.Date.))
                              :branch    ~(System/getenv "TRAVIS_BRANCH")
                              :commit    ~(System/getenv "TRAVIS_COMMIT")}
             :silent         false}

  :profiles {:dev            {:dependencies   [[com.cemerick/piggieback "0.2.2"]
                                               [figwheel-sidecar "0.5.18"]
                                               [refactor-nrepl "2.4.0"]
                                               [snipsnap "0.2.0" :exclusions [org.clojure/clojure]]
                                               [reloaded.repl "0.2.4"]
                                               [speclj-junit "0.0.11-20151116.130002-1"]
                                               [criterium "0.4.4"]]
                              :plugins        [[refactor-nrepl "2.4.0"]
                                               [lein-cljfmt "0.5.7"]
                                               [lein-kibit "0.1.6"]]
                              :source-paths   ["dev/clj" "test/cljc/unit" "spec"]
                              :resource-paths ["dev-resources"]
                              :env            {:dev? "true"}}

             :virkailija-dev [:dev {:figwheel    {:nrepl-port  3334
                                                  :server-port 3449}
                                    :target-path "target-virkailija"
                                    :env         {:app "virkailija"}
                                    :jvm-opts    ^:replace ["-Dapp=virkailija"
                                                            "-Duser.home=."
                                                            "-XX:MaxJavaStackTraceDepth=10"]}]
             :hakija-dev     [:dev {:figwheel    {:nrepl-port  3336
                                                  :server-port 3450}
                                    :target-path "target-hakija"
                                    :env         {:app "hakija"}
                                    :jvm-opts    ^:replace ["-Dapp=hakija"
                                                            "-Duser.home=."
                                                            "-XX:MaxJavaStackTraceDepth=10"]}]
             :uberjar        {:aot            :all
                              :resource-paths ["resources"]}}

  :aliases {"virkailija-dev"      ["with-profile" "virkailija-dev" "run" "virkailija"]
            "hakija-dev"          ["with-profile" "hakija-dev" "run" "hakija"]
            "figwheel-virkailija" ["with-profile" "virkailija-dev" "figwheel" "virkailija-dev"]
            "figwheel-hakija"     ["with-profile" "hakija-dev" "figwheel" "hakija-dev"]
            "export-locales"      ["with-profile" "dev" "run" "-m" "ataru.scripts.export-locales"]
            "anonymize-data"      ["with-profile" "dev" "run" "-m" "ataru.anonymizer.core/anonymize-data"]
            "db-schema"           ["with-profile" "dev" "run" "-m" "ataru.scripts.generate-schema-diagram"]})




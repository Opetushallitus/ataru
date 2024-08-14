(defproject ataru "0.1.0-SNAPSHOT"
  :managed-dependencies [[com.fasterxml.jackson.core/jackson-core "2.15.2"]
                         [com.fasterxml.jackson.core/jackson-databind "2.15.2"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.15.2"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.15.2"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.15.2"]
                         [com.github.fge/jackson-coreutils "1.8"]
                         [ring-middleware-format "0.7.5"]
                         [org.clojure/core.memoize "1.0.257"]
                         [org.clojure/clojurescript "1.11.121"]
                         [org.clojure/tools.reader "1.3.6"]
                         [com.cognitect/transit-clj "1.0.333"]
                         [org.apache.httpcomponents/httpcore "4.4.16"]
                         [org.apache.httpcomponents/httpasyncclient "4.1.5"]
                         [com.taoensso/encore "3.68.0"]
                         [ring/ring-core "1.10.0"]
                         [ring/ring-codec "1.2.0"]
                         [com.google.code.gson/gson "2.10.1"]
                         [com.google.code.findbugs/jsr305 "3.0.2"]
                         [potemkin "0.4.6"]
                         [org.slf4j/slf4j-api "2.0.9"]
                         [commons-codec "1.16.0"]
                         [riddley "0.2.0"]
                         [instaparse "1.4.12"]
                         [org.mozilla/rhino "1.7.14"]
                         [org.scala-lang/scala-library "2.12.18"]
                         [org.scala-lang.modules/scala-xml_2.12 "2.2.0"]]
  :dependencies [[org.clojure/clojure "1.11.2"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.11.121" :exclusions [com.cognitect/transit-java]]
                 [reagent "1.2.0"]
                 [re-frame "1.3.0" :exclusions [org.clojure/tools.logging]]
                 [clj-commons/secretary "1.2.4"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cljs-ajax "0.8.4"]
                 [binaryage/devtools "1.0.7"]
                 [day8.re-frame/tracing "0.6.2"]
                 [day8.re-frame/re-frame-10x "1.9.8"]
                 [venantius/accountant "0.2.5"]
                 [com.cemerick/url "0.1.1"]
                 [cljsjs/react "18.2.0-1"]
                 [cljsjs/react-dom "18.2.0-1"]

                 ;clojure/clojurescript
                 [prismatic/schema "1.4.1"]
                 [com.taoensso/timbre "6.2.2"]
                 [timbre-ns-pattern-level "0.1.2"]
                 [org.clojure/core.match "1.0.1"]
                 [metosin/schema-tools "0.13.1"]
                 [medley "1.4.0"]
                 [markdown-clj "1.11.7"]

                 ;clojure
                 [com.rpl/specter "1.1.4"]
                 [compojure "1.7.0"]
                 [com.github.fge/json-patch "1.9"]
                 [com.stuartsierra/component "1.1.0"]
                 [metosin/compojure-api "1.1.13"]
                 [aleph "0.6.3"]
                 [oph/clj-access-logging "1.0.0-SNAPSHOT" :exclusions [javax.xml.bind/jaxb-api]]
                 [oph/clj-stdout-access-logging "1.0.0-SNAPSHOT" :exclusions [com.google.guava/guava]]
                 [oph/clj-timbre-access-logging "1.1.0-SNAPSHOT" :exclusions [com.google.guava/guava]]
                 [oph/clj-timbre-auditlog "0.2.0-SNAPSHOT" :exclusions [com.google.guava/guava]]
                 [fi.vm.sade/auditlogger "9.2.0-SNAPSHOT"]
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [clj-http "3.12.3" :exclusions [commons-io]]
                 [ring "1.10.0"]
                 [oph/clj-ring-db-cas-session "0.3.0-SNAPSHOT"]
                 [ring/ring-defaults "0.4.0"]
                 [ring/ring-json "0.5.1"]
                 [ring-ratelimit "0.2.3"]
                 [bk/ring-gzip "0.3.0"]
                 [buddy/buddy-auth "3.0.323"]
                 [yesql "0.5.3"]
                 [com.layerware/hugsql "0.5.3"]
                 ; Flyway 4 breaks our migrations
                 [org.flywaydb/flyway-core "3.2.1" :upgrade false]
                 [camel-snake-kebab "0.4.3"]
                 [environ "1.2.0"]
                 [org.clojure/core.async "1.6.681"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.postgresql/postgresql "42.7.2" :exclusions [org.checkerframework/checker-qual]]
                 [clj-time "0.15.2"]
                 [cheshire/cheshire "5.12.0"]
                 [selmer "1.12.59"]
                 [metosin/ring-http-response "0.9.3"]
                 [fi.vm.sade/scala-cas_2.12 "2.2.2.1-SNAPSHOT"]
                 [ring/ring-session-timeout "0.3.0"]
                 [org.apache.poi/poi-ooxml "5.2.5"]
                 [org.clojure/core.cache "1.0.225"]
                 [nrepl "1.0.0"]
                 [com.taoensso/carmine "3.2.0" :exclusions [io.aviso/pretty]]
                 [hikari-cp "3.0.1"]
                 [ring/ring-mock "0.4.0"]
                 [speclj "3.4.3"]
                 [org.clojure/test.check "1.1.1"]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "20220608.1" :exclusions [com.google.guava/guava]]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.558"]
                 [com.amazonaws/aws-java-sdk-sns "1.12.558"]
                 [com.amazonaws/aws-java-sdk-sqs "1.12.558"]
                 [com.amazonaws/aws-java-sdk-cloudwatch "1.12.558"]
                 [com.github.ben-manes.caffeine/caffeine "3.1.8"]
                 [org.clojure/data.xml "0.0.8"]
                 [com.jcraft/jsch "0.1.55"]
                 [fi.vm.sade.dokumenttipalvelu/dokumenttipalvelu "6.12-SNAPSHOT"]
                 ; these two deps are for routing all other logging frameworks' output to timbre by first piping them to SLF4J and then timbre
                 [com.fzakaria/slf4j-timbre "0.4.0" :exclusions [io.aviso/pretty]]
                 [org.slf4j/log4j-over-slf4j "2.0.9"]
                 [oph/clj-string-normalizer "0.1.0-SNAPSHOT" :exclusions [org.jboss.logging/jboss-logging com.google.guava/guava]]
                 [com.google.guava/guava "31.1-jre"]
                 [msolli/proletarian "1.0.68-alpha"]
                 [jarohen/chime "0.3.3"]
                 [cronstar "1.0.2"]]

  :min-lein-version "2.5.3"

  :repositories [["releases" {:url           "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"
                              :sign-releases false
                              :snapshots     false}]
                 ["snapshots" {:url      "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"
                               :releases {:update :never}}]
                 ["ext-snapshots" {:url      "https://artifactory.opintopolku.fi/artifactory/ext-snapshot-local"
                                   :releases {:update :never}}]]

  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["spec"]
  :resource-paths ["src/sql" "resources"]
  :uberjar-name "ataru.jar"
  :jvm-opts ^:replace ["-Xmx2g"]

  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-doo "0.1.11"]
            [lein-figwheel "0.5.20"]
            [lein-ancient "0.7.0"]
            [lein-environ "1.2.0"]
            [lein-resource "17.06.1"]
            [speclj "3.4.3"]]

  :doo {:debug true
        :paths {:karma "./node_modules/karma/bin/karma"}
        :karma {:config
                {"customLaunchers"
                 {"Chrome"
                  {"base" "ChromeHeadless"
                   "flags" ["--disable-gpu" "--disable-software-rasterizer" "--no-sandbox"]}}}}}

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "resources/public/css"
                                    "target"
                                    "target-virkailija"
                                    "target-hakija"
                                    "test/js"
                                    "resources/public/js/test"
                                    "out"]

  :auto-clean false

  :figwheel {:css-dirs ["resources/public/css"]
             :repl     false
             :readline false
             :hawk-options {:watcher :polling}}

  :main ataru.core

  :aot [ataru.db.migrations]

  :cljsbuild {:builds [{:id           "virkailija-dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     {:on-jsload "ataru.virkailija.core/mount-root"}
                        :compiler     {:main                 "ataru.virkailija.core"
                                       :preloads             [devtools.preload day8.re-frame-10x.preload.react-18]
                                       :output-to            "resources/public/js/compiled/virkailija-app.js"
                                       :output-dir           "resources/public/js/compiled/virkailija-out"
                                       :asset-path           "/lomake-editori/js/compiled/virkailija-out"
                                       :parallel-build       true
                                       :optimizations        :none
                                       :source-map-timestamp true
                                       :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}}}

                       {:id           "hakija-dev"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     {:on-jsload "ataru.hakija.core/mount-root"}
                        :compiler     {:main                 "ataru.hakija.core"
                                       :preloads             [devtools.preload day8.re-frame-10x.preload.react-18]
                                       :output-to            "resources/public/js/compiled/hakija-app.js"
                                       :output-dir           "resources/public/js/compiled/hakija-out"
                                       :asset-path           "/hakemus/js/compiled/hakija-out"
                                       :parallel-build       true
                                       :optimizations        :none
                                       :source-map-timestamp true
                                       :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}}}

                       {:id           "virkailija-cypress"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     {:on-jsload "ataru.virkailija.core/mount-root"}
                        :compiler     {:main                 "ataru.virkailija.core"
                                       :preloads             [devtools.preload]
                                       :output-to            "resources/public/js/compiled/virkailija-cypress-app.js"
                                       :output-dir           "resources/public/js/compiled/virkailija-cypress-out"
                                       :asset-path           "/lomake-editori/js/compiled/virkailija-cypress-out"
                                       :parallel-build       true
                                       :optimizations        :none}}

                       {:id           "hakija-cypress"
                        :source-paths ["src/cljs" "src/cljc"]
                        :figwheel     {:on-jsload "ataru.hakija.core/mount-root"}
                        :compiler     {:main                 "ataru.hakija.core"
                                       :preloads             [devtools.preload]
                                       :output-to            "resources/public/js/compiled/hakija-cypress-app.js"
                                       :output-dir           "resources/public/js/compiled/hakija-cypress-out"
                                       :asset-path           "/hakemus/js/compiled/hakija-cypress-out"
                                       :parallel-build       true
                                       :optimizations        :none}}

                       {:id           "virkailija-cypress-ci"
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main                 "ataru.virkailija.core"
                                       :output-to            "resources/public/js/compiled/virkailija-cypress-ci-app.js"
                                       :output-dir           "resources/public/js/compiled/virkailija-cypress-ci-out"
                                       :externs              ["resources/virkailija-externs.js"]
                                       :parallel-build       true
                                       :optimizations        :advanced}}

                       {:id           "hakija-cypress-ci"
                        :source-paths ["src/cljs" "src/cljc"]
                        :compiler     {:main                 "ataru.hakija.core"
                                       :output-to            "resources/public/js/compiled/hakija-cypress-ci-app.js"
                                       :output-dir           "resources/public/js/compiled/hakija-cypress-ci-out"
                                       :externs              ["resources/hakija-externs.js"]
                                       :parallel-build       true
                                       :optimizations        :advanced}}

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

  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                 :init             (set! *print-length* 50)
                 :init-ns          user}

  :resource {:resource-paths ["templates"]
             :target-path    "resources/public"
             :update         false                          ;; if true only process files with src newer than dest
             :extra-values   {:version   "0.1.0-SNAPSHOT"
                              :buildTime ~(.format
                                            (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssZZZ")
                                            (java.util.Date.))
                              :branch    ~(System/getenv "CI_BRANCH")
                              :commit    ~(System/getenv "CI_COMMIT")}
             :silent         false}

  :profiles {:dev            {:dependencies   [[cider/piggieback "0.5.3"]
                                               [figwheel-sidecar "0.5.20"]
                                               [snipsnap "0.2.0" :exclusions [org.clojure/clojure]]
                                               [reloaded.repl "0.2.4" :exclusions [org.clojure/tools.namespace]]
                                               [org.clojure/tools.namespace "1.5.0"]
                                               [speclj-junit "0.0.11-20151116.130002-1"]
                                               [criterium "0.4.6"]
                                               [com.gfredericks/debug-repl "0.0.12"]
                                               [clj-test-containers "0.7.4"]]
                              :plugins        [[lein-cljfmt "0.6.7"]
                                               [lein-kibit "0.1.8"]]
                              :source-paths   ["dev/clj" "test/cljc/unit" "spec"]
                              :resource-paths ["dev-resources"]
                              :env            {:dev? "true"
                                               :aws-access-key "localhost"
                                               :aws-secret-key "localhost"}}

             :test           {:dependencies   [[cider/piggieback "0.5.3"]
                                               [figwheel-sidecar "0.5.20"]
                                               [snipsnap "0.2.0" :exclusions [org.clojure/clojure]]
                                               [reloaded.repl "0.2.4" :exclusions [org.clojure/tools.namespace]]
                                               [org.clojure/tools.namespace "1.5.0"]
                                               [speclj-junit "0.0.11-20151116.130002-1"]
                                               [criterium "0.4.6"]
                                               [com.gfredericks/debug-repl "0.0.12"]
                                               [clj-test-containers "0.7.4"]]
                              :source-paths   ["dev/clj" "test/cljc/unit" "spec"]
                              :resource-paths ["dev-resources"]
                              :env            {:dev? "true"
                                               :config "config/test.edn"}
                              :jvm-opts       ^:replace ["-Durl.valinta-tulos-service.baseUrl=http://localhost:8097"]}
             :figwheel {:nrepl-port  3334
                        :server-port 3449
                        :hawk-options {:watcher :polling}}

             :virkailija-cypress        {:env {:dev? "true"}
                                         :target-path "target/target-cypess-virkailija"}

             :hakija-cypress        {:env {:dev? "true"}
                                     :target-path "target/target-cypess-hakija"}

             :virkailija-dev [:dev {:target-path "target-virkailija"
                                    :jvm-opts    ^:replace ["-Duser.home=."
                                                            "-XX:MaxJavaStackTraceDepth=10"
                                                            "-Dclojure.main.report=stderr"]}]

             :hakija-dev     [:dev {:target-path "target-hakija"
                                    :jvm-opts    ^:replace ["-Duser.home=."
                                                            "-XX:MaxJavaStackTraceDepth=10"]}]
             :uberjar        {:aot            :all
                              :resource-paths ["resources"]}

             :opintopolku-local {:local-repo "/m2-home/.m2/repository"}
             :opintopolku-local-virkailija {:figwheel {:server-ip "ataru-figwheel-virkailija.kehittajan-oma-kone.testiopintopolku.fi"
                                                       :server-port 3449
                                                       :repl false}}
             :opintopolku-local-hakija {:figwheel {:server-ip "ataru-figwheel-hakija.kehittajan-oma-kone.testiopintopolku.fi"
                                                   :server-port 3450
                                                   :repl false}}}

  :aliases {"virkailija-dev"      ["with-profile" "virkailija-dev" "run" "virkailija"]
            "hakija-dev"          ["with-profile" "hakija-dev" "run" "hakija"]
            "start-figwheel"      ["with-profile" "figwheel" "figwheel" "virkailija-dev" "hakija-dev" "virkailija-cypress" "hakija-cypress"]
            "export-locales"      ["with-profile" "dev" "run" "-m" "ataru.scripts.export-locales"]
            "anonymize-data"      ["with-profile" "dev" "run" "-m" "ataru.anonymizer.core/anonymize-data"]
            "db-schema"           ["with-profile" "dev" "run" "-m" "ataru.scripts.generate-schema-diagram"]
            "generate-secrets"    ["with-profile" "dev" "run" "-m" "ataru.util.secrets-generator"]})




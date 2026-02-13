(defproject ataru "0.1.0-SNAPSHOT"
  :managed-dependencies [[com.fasterxml.jackson.core/jackson-core "2.18.3"]
                         [com.fasterxml.jackson.core/jackson-databind "2.18.3"]
                         [com.fasterxml.jackson.core/jackson-annotations "2.18.3"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.18.3"]
                         [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.18.3"]
                         [com.github.fge/jackson-coreutils "1.8"]
                         [ring-middleware-format "0.7.5"]
                         [org.apache.commons/commons-io "2.19.0"]
                         [org.clojure/clojure "1.11.2"]
                         [org.clojure/data.json "1.0.0"]
                         [org.clojure/core.memoize "1.0.257"]
                         [org.clojure/clojurescript "1.11.121"]
                         [org.clojure/tools.reader "1.3.6"]
                         [com.cognitect/transit-clj "1.0.333"]
                         [org.apache.httpcomponents/httpcore "4.4.16"]
                         [org.apache.httpcomponents/httpasyncclient "4.1.5"]
                         [com.taoensso/encore "3.113.0"]
                         [ring/ring-core "1.10.0"]
                         [ring/ring-codec "1.2.0"]
                         [com.google.code.gson/gson "2.10.1"]
                         [com.google.code.findbugs/jsr305 "3.0.2"]
                         [potemkin "0.4.7"]
                         [org.slf4j/slf4j-api "2.0.9"]
                         [commons-codec "1.16.0"]
                         [commons-logging "1.3.5"]
                         ; transitive from compojure
                         [commons-fileupload "1.6.0"]
                         [riddley "0.2.0"]
                         [instaparse "1.4.12"]
                         [org.mozilla/rhino "1.7.14"]
                         [org.scala-lang/scala-library "2.12.18"]
                         [org.scala-lang.modules/scala-xml_2.12 "2.2.0"]
                         [joda-time "2.12.7"]
                         [net.java.dev.jna/jna "5.8.0"]
                         [opiskelijavalinnat-utils/java-cas "2.0.0-SNAPSHOT"]
                         ;transitive from clj-util
                         [io.undertow/undertow-core "2.3.20.Final"]
                         [org.apache.commons/commons-lang3 "3.14.0"]
                         [org.jboss.threads/jboss-threads "3.5.0.Final"]
                         [org.jboss.xnio/xnio-api "3.8.14.Final"]
                         [org.jboss.xnio/xnio-nio "3.8.14.Final"]
                         [org.testcontainers/testcontainers "2.0.2"]]
  :dependencies [[org.clojure/clojure "1.11.2"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.11.121" :exclusions [com.cognitect/transit-java]]
                 [reagent "1.2.0"]
                 [re-frame "1.4.3" :exclusions [org.clojure/tools.logging]]
                 [clj-commons/secretary "1.2.4"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [cljs-ajax "0.8.4"
                  :exclusions [commons-logging]]
                 [binaryage/devtools "1.0.7"]
                 [day8.re-frame/tracing "0.6.2"]
                 [day8.re-frame/re-frame-10x "1.9.9"]
                 [venantius/accountant "0.2.5"]
                 [com.cemerick/url "0.1.1"]
                 [cljsjs/react "18.2.0-1"]
                 [cljsjs/react-dom "18.2.0-1"]
                 [lein-doo "0.1.11"]

                 ;clojure/clojurescript
                 [prismatic/schema "1.4.1"]
                 [com.taoensso/timbre "6.5.0"]
                 [timbre-ns-pattern-level "0.1.2"]
                 [org.clojure/core.match "1.0.1"]
                 [metosin/schema-tools "0.13.1"]
                 [medley "1.4.0"]
                 [markdown-clj "1.12.4"]

                 ;clojure
                 [com.rpl/specter "1.1.4"]
                 [compojure "1.7.0"
                  :exclusions [commons-io]]
                 [com.stuartsierra/component "1.1.0"]
                 [metosin/compojure-api "1.1.13"
                  :exclusions [commons-io]]
                 [aleph "0.9.3"
                  :exclusions [io.netty/netty-buffer
                               io.netty/netty-codec
                               io.netty/netty-codec-dns
                               io.netty/netty-codec-http
                               io.netty/netty-codec-http2
                               io.netty/netty-codec-socks
                               io.netty/netty-common
                               io.netty/netty-handler
                               io.netty/netty-handler-proxy
                               io.netty/netty-resolver
                               io.netty/netty-resolver-dns
                               io.netty/netty-resolver-dns-native-macos
                               io.netty/netty-transport
                               io.netty/netty-transport-classes-epoll
                               io.netty/netty-transport-classes-kqueue
                               io.netty/netty-transport-native-epoll
                               io.netty/netty-transport-native-kqueue
                               io.netty/netty-transport-native-unix-common
                               org.clojure/tools.logging]]
                 ; pinning netty deps to same version because of conflicting transitive deps
                 [io.netty/netty-buffer "4.1.124.Final"]
                 [io.netty/netty-codec "4.1.124.Final"]
                 [io.netty/netty-codec-dns "4.1.124.Final"]
                 [io.netty/netty-codec-http "4.1.124.Final"]
                 [io.netty/netty-codec-http2 "4.1.124.Final"]
                 [io.netty/netty-codec-socks "4.1.124.Final"]
                 [io.netty/netty-common "4.1.124.Final"]
                 [io.netty/netty-handler "4.1.124.Final"]
                 [io.netty/netty-handler-proxy "4.1.124.Final"]
                 [io.netty/netty-resolver "4.1.124.Final"]
                 [io.netty/netty-resolver-dns "4.1.124.Final"]
                 [io.netty/netty-resolver-dns-native-macos "4.1.124.Final"]
                 [io.netty/netty-transport "4.1.124.Final"]
                 [io.netty/netty-transport-classes-epoll "4.1.124.Final"]
                 [io.netty/netty-transport-classes-kqueue "4.1.124.Final"]
                 [io.netty/netty-transport-native-epoll "4.1.124.Final"]
                 [io.netty/netty-transport-native-kqueue "4.1.124.Final"]
                 [io.netty/netty-transport-native-unix-common "4.1.124.Final"]
                 [oph/clj-access-logging "1.0.0-SNAPSHOT" :exclusions [javax.xml.bind/jaxb-api io.findify/s3mock_2.12]]
                 [oph/clj-stdout-access-logging "1.0.0-SNAPSHOT" :exclusions [com.google.guava/guava io.findify/s3mock_2.12]]
                 [oph/clj-timbre-access-logging "1.1.0-SNAPSHOT" :exclusions [com.google.guava/guava io.findify/s3mock_2.12]]
                 [oph/clj-timbre-auditlog "0.2.0-SNAPSHOT" :exclusions [com.google.guava/guava io.findify/s3mock_2.12]]
                 [fi.vm.sade/auditlogger "9.2.0-SNAPSHOT"]
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [clj-http "3.12.3" :exclusions [commons-io]]
                 [ring "1.11.0"
                  :exclusions [commons-io]]
                 [oph/clj-ring-db-cas-session "0.3.0-SNAPSHOT" :exclusions [io.findify/s3mock_2.12 commons-io]]
                 [ring/ring-defaults "0.4.0"
                  :exclusions [commons-io]]
                 [ring/ring-json "0.5.1"
                  :exclusions [commons-io]]
                 [ring-ratelimit "0.2.3"]
                 [bk/ring-gzip "0.3.0"]
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
                 [cheshire/cheshire "6.0.0"]
                 [selmer "1.12.59"]
                 [metosin/ring-http-response "0.9.3"
                  :exclusions [commons-io]]
                 [opiskelijavalinnat-utils/java-cas "2.0.0-SNAPSHOT"]
                 [org.asynchttpclient/async-http-client "3.0.1"]
                 [ring/ring-session-timeout "0.3.0"]
                 [org.apache.poi/poi-ooxml "5.3.0"]
                 [org.clojure/core.cache "1.0.225"]
                 [nrepl "1.0.0"]
                 [com.taoensso/carmine "3.4.1" :exclusions [io.aviso/pretty]]
                 [hikari-cp "3.0.1"]
                 [ring/ring-mock "0.4.0"]
                 [speclj "3.4.3"]
                 [org.clojure/test.check "1.1.1"]
                 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "20220608.1" :exclusions [com.google.guava/guava]]
                 [software.amazon.awssdk/s3 "2.36.3"]
                 [software.amazon.awssdk/sqs "2.36.3"]
                 [software.amazon.awssdk/cloudwatch "2.36.3"]
                 [com.github.ben-manes.caffeine/caffeine "3.1.8"]
                 [org.clojure/data.xml "0.0.8"]
                 [fi.vm.sade.dokumenttipalvelu/dokumenttipalvelu "6.15-SNAPSHOT"]
                 [opiskelijavalinnat-utils.viestinvalitys/kirjasto "1.2.2-SNAPSHOT"]
                 [com.thoughtworks.paranamer/paranamer "2.8.3"]
                 ; these two deps are for routing all other logging frameworks' output to timbre by first piping them to SLF4J and then timbre
                 [com.fzakaria/slf4j-timbre "0.4.0" :exclusions [io.aviso/pretty]]
                 [org.slf4j/log4j-over-slf4j "2.0.9"]   ;; Log4j 1.x
                 [org.apache.logging.log4j/log4j-to-slf4j "2.25.2"]  ;; Log4j 2.x
                 [oph/clj-string-normalizer "0.1.0-SNAPSHOT" :exclusions [org.jboss.logging/jboss-logging com.google.guava/guava commons-io]]
                 [com.google.guava/guava "31.1-jre"]
                 [msolli/proletarian "1.0.68-alpha"]
                 [jarohen/chime "0.3.3"]
                 [cronstar "1.0.2"]]

  :min-lein-version "2.5.3"

  :repositories [["github" {:url "https://maven.pkg.github.com/Opetushallitus/packages"
                            :username "private-token"
                            :password :env/GITHUB_TOKEN}]
                 ["releases" {:url           "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"
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
  :jvm-opts ^:replace ["-Xmx8g"]

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
                                               [org.clojure/data.csv "1.1.0"]
                                               [figwheel-sidecar "0.5.20"
                                                :exclusions [commons-io]]
                                               [snipsnap "0.2.0" :exclusions [org.clojure/clojure]]
                                               [reloaded.repl "0.2.4" :exclusions [org.clojure/tools.namespace]]
                                               [org.clojure/tools.namespace "1.5.0"]
                                               [speclj-junit "0.0.11-20151116.130002-1"]
                                               [criterium "0.4.6"]
                                               [com.gfredericks/debug-repl "0.0.12"]
                                               [org.testcontainers/testcontainers "2.0.2"]
                                               [clj-test-containers "0.7.4"]]
                              :plugins        [[lein-cljfmt "0.6.7"]
                                               [lein-kibit "0.1.8"]]
                              :source-paths   ["dev/clj" "test/cljc/unit" "spec"]
                              :resource-paths ["dev-resources"]
                              :env            {:dev? "true"
                                               :aws-access-key "localhost"
                                               :aws-secret-key "localhost"}}

             :test           {:dependencies   [[cider/piggieback "0.5.3"]
                                               [org.clojure/data.csv "1.1.0"]
                                               [figwheel-sidecar "0.5.20"]
                                               [snipsnap "0.2.0" :exclusions [org.clojure/clojure]]
                                               [reloaded.repl "0.2.4" :exclusions [org.clojure/tools.namespace]]
                                               [org.clojure/tools.namespace "1.5.0"]
                                               [speclj-junit "0.0.11-20151116.130002-1"]
                                               [criterium "0.4.6"]
                                               [com.gfredericks/debug-repl "0.0.12"]
                                               [org.testcontainers/testcontainers "2.0.2"]
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

             :ataru-main {:main ataru.core
                          :target-path "ataru"
                          :source-paths ["src/clj" "src/cljc"]
                          :test-paths ["spec"]
                          :uberjar-name "ataru.jar"}

             :ovara {:main ataru.siirtotiedosto-app
                     :target-path "ataru"
                     :source-paths ["src/clj" "src/cljc"]
                     :test-paths ["spec"]
                     :env            {:config "config/siirtotiedostoapp-dev.edn"}
                     :uberjar-name "ovara-ataru.jar"}}

             :opintopolku-local {:local-repo "/m2-home/.m2/repository"}
             :opintopolku-local-virkailija {:figwheel {:server-ip "ataru-figwheel-virkailija.kehittajan-oma-kone.testiopintopolku.fi"
                                                       :server-port 3449
                                                       :repl false}}
             :opintopolku-local-hakija {:figwheel {:server-ip "ataru-figwheel-hakija.kehittajan-oma-kone.testiopintopolku.fi"
                                                   :server-port 3450
                                                   :repl false}}

  :aliases {"virkailija-dev"      ["with-profile" "virkailija-dev" "run" "virkailija"]
            "hakija-dev"          ["with-profile" "hakija-dev" "run" "hakija"]
            "start-figwheel"      ["with-profile" "figwheel" "figwheel" "virkailija-dev" "hakija-dev" "virkailija-cypress" "hakija-cypress"]
            "export-locales"      ["with-profile" "dev" "run" "-m" "ataru.scripts.export-locales"]
            "anonymize-data"      ["with-profile" "dev" "run" "-m" "ataru.anonymizer.core/anonymize-data"]
            "db-schema"           ["with-profile" "dev" "run" "-m" "ataru.scripts.generate-schema-diagram"]
            "generate-secrets"    ["with-profile" "dev" "run" "-m" "ataru.util.secrets-generator"]})

(load-file "soresu.clj")
(defproject ataru "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ; clojurescript
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]   ; react in clojure
                 [re-frame "0.7.0"]  ; flux for re-agent
                 [re-com "0.8.0"]    ; reusable re-frame components
                 [secretary "1.2.3"] ; routing
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [oph/soresu "0.1.0-SNAPSHOT"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]

                 ;clojure/clojurescript
                 [prismatic/schema "1.0.5"]
                 [com.taoensso/timbre "4.3.1"]
                 [org.clojure/core.match "0.3.0-alpha4"]

                 ;clojure
                 [compojure "1.5.0"]
                 [metosin/compojure-api "1.0.1"]
                 [aleph "0.4.1"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [ring/ring-json "0.4.0"]
                 [ring-ratelimit "0.2.2"]
                 [bk/ring-gzip "0.1.1"]
                 [yesql "0.5.2"]
                 [camel-snake-kebab "0.3.2"]
                 [environ "1.0.2"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [postgresql/postgresql "9.1-901-1.jdbc4"]
                 [clj-time "0.11.0"]
                 [cider/cider-nrepl "0.12.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                 [cheshire/cheshire "5.5.0"]]

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
  :resource-paths ["src/sql" "resources"]
  :uberjar-name "ataru.jar"

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.0-6"]
            [lein-doo "0.1.6"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.8"]
            [lein-environ "1.0.2"]]

  :eastwood {:namespaces [:source-paths]
             :add-linters [:unused-locals
                           :unused-private-vars]
             :exclude-linters [:local-shadows-var]}

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :nrepl-port 3334}

  :doo {:paths {:phantom "./node_modules/phantomjs-prebuilt/bin/phantomjs"}}

  :less {:source-paths ["resources/less"]
         :target-path  "resources/public/css/compiled"}

  :main lomake-editori.core

  :cljsbuild { :builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "lomake-editori.core/mount-root"}
                        :compiler {:main lomake-editori.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :foreign-libs [{:file ~soresu
                                                   :provides ["oph.lib.soresu"]}]
                                   :source-map-timestamp true}}

                       {:id "test"
                        :source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/test.js"
                                   :main lomake-editori.runner
                                   :foreign-libs [{:file ~soresu
                                                   :provides ["oph.lib.soresu"]}]
                                   :optimizations :none}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main lomake-editori.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false
                                   :foreign-libs [{:file ~soresu
                                                   :provides ["oph.lib.soresu"]}]}}]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :init (set! *print-length* 50)}
  :profiles {:repl {:plugins [[cider/cider-nrepl "0.12.0-SNAPSHOT" :exclusions [org.clojure/clojure]]]}
             :dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [figwheel-sidecar "0.5.0-2"]
                                  [refactor-nrepl "2.2.0"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [snipsnap "0.1.0" :exclusions [org.clojure/clojure]]]
                   :plugins [[jonase/eastwood "0.2.3" :exclusions [org.clojure/clojure]]
                             [refactor-nrepl "2.2.0"]
                             [cider/cider-nrepl "0.12.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                             [lein-cljfmt "0.5.1"]]
                   :source-paths ["env/dev/clj" "env/dev/cljc"]
                   :resource-paths ["dev-resources"]
                   :env {:dev? true}}
             :uberjar {:aot :all
                       :prep-tasks [["less" "once"] "compile" ["cljsbuild" "once" "min"]]
                       :resource-paths ["resources"]}
             :figwheel-standalone {:figwheel {:ring-handler lomake-editori.handler/handler
                                              :server-port 3449}}})




#!/bin/bash
set -e

OLD_CWD=$(pwd)

echo $OLD_CWD

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

exec-lein-cmd() {
    ./bin/lein version
    $1
}

compile-less() {
    exec-lein-cmd "./bin/lein less once"
}

npm-dependencies() {
    npm install
}

process-resources() {
    exec-lein-cmd "./bin/lein resource"
}

build-clojurescript-virkailija() {
    exec-lein-cmd "./bin/lein cljsbuild once virkailija-min"
}

build-clojurescript-hakija() {
    exec-lein-cmd "./bin/lein cljsbuild once hakija-min"
}

create-uberjar() {
    exec-lein-cmd "./bin/lein uberjar"
}

test-clojure() {
    exec-lein-cmd "./bin/lein spec -t ~ui"
}

test-clojurescript() {
    exec-lein-cmd "./bin/lein doo phantom test once"
}

test-integration() {
    exec-lein-cmd "./bin/lein spec -t ui"
}

run-migrations() {
    exec-lein-cmd "./bin/lein with-profile dev run -m manual-migrations/migrate"
}

clean() {
    ./bin/lein clean
    rm -rf target
    rm -rf node_modules
}

command="$1"

case "$command" in
    "compile-less" )
        compile-less
        ;;
    "npm-dependencies" )
        npm-dependencies
        ;;
    "process-resources" )
        process-resources
        ;;
    "build-clojurescript-virkailija" )
        build-clojurescript-virkailija
        ;;
    "build-clojurescript-hakija" )
        build-clojurescript-hakija
        ;;
    "create-uberjar" )
        create-uberjar
        ;;
    "test-clojure" )
        test-clojure
        ;;
    "test-clojurescript" )
        test-clojurescript
        ;;
    "test-integration" )
        test-integration
        ;;
    "run-migrations" )
        run-migrations
        ;;
    "clean" )
        clean
        ;;
    *)
        echo "Unknown command $command"
        ;;
esac

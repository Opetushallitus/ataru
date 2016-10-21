#!/bin/bash
set -e

OLD_CWD=$(pwd)

echo $OLD_CWD

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

./bin/lein version

compile-less() {
    ./bin/lein less once
}

npm-dependencies() {
    npm install
}

process-resources() {
    ./bin/lein resource
}

build-clojurescript-virkailija() {
    ./bin/lein cljsbuild once virkailija-min
}

build-clojurescript-hakija() {
    ./bin/lein cljsbuild once hakija-min
}

create-uberjar() {
    ./bin/lein uberjar
}

test-clojure() {
    ./bin/lein clean
    ./bin/lein spec -t ~ui
}

test-clojurescript() {
    ./bin/lein clean
    ./bin/lein doo phantom test once
}

test-integration() {
    ./bin/lein spec -t ui
}

run-migrations() {
    ./bin/lein with-profile dev run -m manual-migrations/migrate
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

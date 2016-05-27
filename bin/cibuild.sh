#!/bin/bash
set -e

OLD_CWD=$(pwd)

echo $OLD_CWD

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

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
    ./bin/lein spec
}

test-clojurescript() {
    ./bin/lein doo phantom test once
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

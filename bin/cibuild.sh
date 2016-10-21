#!/bin/bash
set -e

OLD_CWD=$(pwd)

echo $OLD_CWD

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

echo "Lein version:"
./bin/lein version

compile-less() {
    echo "Compiling less"
    ./bin/lein less once
}

npm-dependencies() {
    echo "Installing npm dependencies"
    npm install
}

process-resources() {
    echo "Processing resources"
    ./bin/lein resource
}

build-clojurescript-virkailija() {
    echo "Building virkailija clojurescript"
    ./bin/lein cljsbuild once virkailija-min
}

build-clojurescript-hakija() {
    echo "Building hakija clojurescript"
    ./bin/lein cljsbuild once hakija-min
}

create-uberjar() {
    echo "Creating uberjar"
    ./bin/lein uberjar
}

test-clojure() {
    echo "Running clojure tests"
    ./bin/lein spec -t ~ui
}

test-clojurescript() {
    echo "Testing clojurescript"
    ./bin/lein doo phantom test once
}

test-browser() {
    ./bin/lein spec -t ui
}

run-migrations() {
    echo "Running migrations"
    ./bin/lein with-profile dev run -m manual-migrations/migrate
}

nuke-test-db() {
    echo "Nuking test database"
    ./bin/lein with-profile dev run -m ataru.fixtures.db.unit-test-db/clear-database
}

run-tests() {
    echo "Starting test run"
    ./bin/lein clean
    rm -rf out # phantom tests fail randomly without this
    npm-dependencies
    test-clojurescript
    nuke-test-db
    run-migrations
    test-clojure
    compile-less
    build-clojurescript-virkailija
    build-clojurescript-hakija
    test-browser
}

clean() {
    echo "Cleaning everything"
    ./bin/lein clean
    rm -rf target
    rm -rf out
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
    "run-tests" )
        run-tests
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

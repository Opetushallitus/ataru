#!/bin/bash
set -e

OLD_CWD=$(pwd)

echo $OLD_CWD

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

echo "Lein version:"
./bin/lein version

clean() {
    echo "Cleaning everything"
    ./bin/lein clean
}

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
    ./bin/lein with-profile dev run -m ataru.db.migrations/migrate
}

nuke-test-db() {
    echo "Nuking test database"
    ./bin/lein with-profile dev run -m ataru.fixtures.db.unit-test-db/clear-database
}

create-db-schema() {
    echo "Creating DB schema diagrams"
    ./bin/lein db-schema
}

reset-test-database-with-fixture() {
    nuke-test-db
    run-migrations
    ./bin/lein with-profile dev run -m ataru.fixtures.db.browser-test-db/init-db-fixture
}

ui-compile() {
    clean
    build-clojurescript-hakija
    build-clojurescript-virkailija
    compile-less
}

prepare-ui-tests() {
    ui-compile
    reset-test-database-with-fixture
}


create-uberjar() {
    clean
    build-clojurescript-hakija
    build-clojurescript-virkailija
    compile-less
    process-resources
    echo "Creating uberjar"
    ./bin/lein uberjar
}

run-tests() {
    echo "Starting test run"
    ./bin/lein clean
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

run-browser-tests() {
    echo "Starting browser test run"
    clean
    npm-dependencies
    nuke-test-db
    run-migrations
    compile-less
    build-clojurescript-virkailija
    build-clojurescript-hakija
    test-browser
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
    "prepare-ui-tests" )
        prepare-ui-tests
        ;;
    "reset-test-database-with-fixture" )
        reset-test-database-with-fixture
        ;;
    "ui-compile" )
        ui-compile
        ;;
    "create-uberjar" )
        create-uberjar
        ;;
    "create-db-schema" )
        create-db-schema
        ;;
    "test-clojure" )
        test-clojure
        ;;
    "test-browser" )
        run-browser-tests
        ;;
    "test-clojurescript" )
        test-clojurescript
        ;;
    "run-tests" )
        run-tests
        ;;
    "nuke-test-db" )
        nuke-test-db
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
        echo "Unknown command $command. Available commands:
* compile-less
* npm-dependencies
* process-resources
* build-clojurescript-virkailija
* build-clojurescript-hakija
* prepare-ui-tests
* reset-test-database-with-fixture
* ui-compile
* create-uberjar
* create-db-schema
* test-clojure
* test-browser
* test-clojurescript
* run-tests
* nuke-test-db
* test-integration
* run-migrations
* clean"
        ;;
esac

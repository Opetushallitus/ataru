#!/bin/bash
set -e

CONFIG=${CONFIG:-config/dev.edn}

echo "java version:"
java -version

echo "Lein version:"
./bin/lein version

clean() {
    echo "Cleaning everything"
    time ./bin/lein clean
}

compile-less() {
    echo "Compiling less"
    time ./bin/lein less once
}

npm-dependencies() {
    echo "Installing npm dependencies"
    time npm install
    export CHROME_BIN=$(node -e "console.log(require('puppeteer').executablePath());")
}

type_check() {
  echo "Running TypeScript type check"
  time npm run tsc:type-check
}

eslint() {
    echo "Running ESLint"
    npm run lint:js
}

clj_kondo() {
    echo "Running clj-kondo"
    #npm run lint:clj
}

lint() {
    type_check
    eslint
    clj_kondo
}

start_fake_deps_server() {
  ./bin/fake-deps-server.sh start
}

stop_fake_deps_server() {
  ./bin/fake-deps-server.sh stop
}

process-resources() {
    echo "Processing resources"
    time ./bin/lein resource
}

build-clojurescript() {
    echo "Building clojurescript"
    time ./bin/lein cljsbuild once virkailija-min hakija-min
}

test-clojure() {
    echo "Running clojure tests"
    start_fake_deps_server
    time ./bin/lein spec -t ~ui
    stop_fake_deps_server
}

test-clojurescript() {
    echo "Testing clojurescript"
    time ./bin/lein doo chrome-headless test once
}

test-browser() {
  start_fake_deps_server
  time ./bin/lein spec -t ui
  time ./bin/run-cypress-tests-in-travis.sh
  stop_fake_deps_server
}

run-migrations() {
    echo "Running migrations"
    start_fake_deps_server
    time ./bin/lein with-profile dev run -m ataru.db.migrations/migrate "use dummy-audit-logger!"
    stop_fake_deps_server
}

nuke-test-db() {
    echo "Nuking test database"
    time ./bin/lein with-profile dev run -m ataru.fixtures.db.unit-test-db/clear-database
}

create-db-schema() {
    echo "Creating DB schema diagrams"
    time ./bin/lein db-schema
}

reset-test-database-with-fixture() {
    nuke-test-db
    run-migrations
    time ./bin/lein with-profile dev run -m ataru.fixtures.db.browser-test-db/init-db-fixture
}

ui-compile() {
    clean
    build-clojurescript
    compile-less
}

prepare-ui-tests() {
    ui-compile
    reset-test-database-with-fixture
}


create-uberjar() {
    clean
    build-clojurescript
    compile-less
    process-resources
    echo "Creating uberjar"
    time ./bin/lein uberjar
}

run-tests() {
    echo "Starting test run"
    clean
    npm-dependencies
    lint
    test-clojurescript
    nuke-test-db
    run-migrations
    test-clojure
    compile-less
    build-clojurescript
    test-browser
}

run-clojure-tests() {
    echo "Starting clojure test run"
    clean
    npm-dependencies
    lint
    test-clojurescript
    nuke-test-db
    run-migrations
    test-clojure
}

run-tests-and-create-uberjar() {
    run-tests
    process-resources
    time ./bin/lein uberjar
}

run-browser-tests() {
    echo "Starting browser test run"
    clean
    npm-dependencies
    nuke-test-db
    run-migrations
    compile-less
    build-clojurescript
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
    "eslint" )
        eslint
        ;;
    "lint" )
        lint
        ;;
    "process-resources" )
        process-resources
        ;;
    "build-clojurescript" )
        build-clojurescript
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
    "run-tests-and-create-uberjar" )
        run-tests-and-create-uberjar
        ;;
    "run-browser-tests" )
        run-browser-tests
        ;;
    "run-clojure-tests" )
        run-clojure-tests
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
* clean
* compile-less
* npm-dependencies
* eslint
* lint
* process-resources
* build-clojurescript
* test-clojure
* test-clojurescript
* test-browser
* run-migrations
* nuke-test-db
* create-db-schema
* reset-test-database-with-fixture
* ui-compile
* prepare-ui-tests
* create-uberjar
* run-tests
* run-tests-and-create-uberjar
* run-clojure-tests
* run-browser-tests"
esac

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
    time ./bin/compile-less.sh
}

pnpm-dependencies() {
    echo "Installing pnpm dependencies"
    time pnpm install --frozen-lockfile
    export CHROME_BIN=$(node -e "console.log(require('puppeteer').executablePath());")
}

type_check() {
  echo "Running TypeScript type check"
  time pnpm run tsc:type-check
}

eslint() {
    echo "Running ESLint"
    pnpm run lint:js
}

clj_kondo() {
    echo "Running clj-kondo"
    pnpm run lint:clj
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
    time ./bin/lein doo chrome test once
}

test-browser() {
  start_fake_deps_server
  time ./bin/lein spec -t ui
  time ./bin/run-integration-tests-in-ci.sh
  stop_fake_deps_server
}

test-browser-mocha() {
  start_fake_deps_server
  time ./bin/lein spec -t ui
  stop_fake_deps_server
}

run-migrations() {
    echo "Running migrations"
    start_fake_deps_server
    time ./bin/lein with-profile dev run -m ataru.db.flyway-migration/migrate "use dummy-audit-logger!"
    stop_fake_deps_server
}

nuke-test-db() {
    echo "Nuking test database"
    time ./bin/lein with-profile dev run -m ataru.fixtures.db.unit-test-db/clear-database
}

create-both-uberjars() {
    clean
    build-clojurescript
    pnpm-dependencies
    compile-less
    echo "Creating uberjar"
    time ./bin/lein with-profile ataru-main:ovara uberjar
}

run-spec-and-mocha-tests() {
    echo "Starting spec and mocha test run"
    clean
    pnpm-dependencies
    lint
    test-clojurescript
    nuke-test-db
    run-migrations
    test-clojure
    compile-less
    build-clojurescript
    test-browser-mocha
}

run-browser-tests-integration() {
    echo "Starting browser integration test run"
    clean
    nuke-test-db
    run-migrations
    start_fake_deps_server
    time ./bin/run-integration-tests-in-ci.sh
    stop_fake_deps_server
}

command="$1"

case "$command" in
    "create-both-uberjars" )
        create-both-uberjars
        ;;
    "run-browser-tests-integration" )
        run-browser-tests-integration
        ;;
    "run-spec-and-mocha-tests" )
        run-spec-and-mocha-tests
        ;;
    *)
        echo "Unknown command $command. Available commands:
* create-both-uberjars
* run-browser-tests-integration
* run-spec-and-mocha-tests"
esac

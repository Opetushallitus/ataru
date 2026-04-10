#!/bin/bash
set -e


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

create-both-uberjars() {
    clean
    build-clojurescript
    pnpm-dependencies
    compile-less
    echo "Creating uberjar"
    time ./bin/lein with-profile ataru-main:ovara uberjar
}

run-browser-tests-integration() {
    export CONFIG=${CONFIG:-config/cypress.ci.edn}
    echo "Starting browser integration test run"
    clean
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
    *)
        echo "Unknown command $command. Available commands:
* create-both-uberjars
* run-browser-tests-integration
* run-spec-and-mocha-tests"
esac

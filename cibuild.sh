#!/bin/bash
set -e

export JAVA_HOME=/data00/oph/java/jdk1.8.0_60
export PATH=/data00/oph/java/jdk1.8.0_60/bin:$PATH

build-clojurescript() {
    ./bin/lein cljsbuild once min
}

package() {
    ./bin/lein do clean, uberjar
}

clean() {
    ./bin/lein clean
    rm -rf target
    rm -rf node_modules
}

test-clojure() {
    ./bin/lein spec
}

test-browser() {
    ./bin/lein doo phantom test once
}

command="$1"

case "$command" in
    "build-clojurescript" )
        build-clojurescript
        ;;
    "test-clojure" )
        test-clojure
        ;;
    "test-browser" )
        test-browser
        ;;
    "package" )
        package
        ;;
    "clean" )
        clean
        ;;
    *)
        echo "Unknown command $command"
        ;;
esac

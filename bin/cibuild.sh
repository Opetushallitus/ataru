#!/bin/bash
set -e

OLD_CWD=$(pwd)

echo $OLD_CWD

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

lint-clojure() {
    ./bin/lein eastwood
}

command="$1"

case "$command" in
    "build-clojurescript" )
        build-clojurescript
        ;;
    "test-clojure" )
        test-clojure
        ;;
    "lint-clojure" )
        lint-clojure
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

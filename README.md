# Ataru

[![Build Status](https://github.com/Opetushallitus/ataru/actions/workflows/build.yml/badge.svg)](https://github.com/Opetushallitus/ataru/actions/workflows/build.yml)

A system for creating custom forms, applying to education and handling applications.

## How to start

Start all Ataru processes and docker containers using command

    make start

Stop all Ataru processes and docker containers using command

    make stop

See `make help` for details

## Running custom configurations

If you need to run your custom configuration, you may configure the
configuration files the makefile system uses to start the services.

First, you must kill existing pm2 instance, since it caches the environment
variables.

    make kill

Then, you can start the system using your own configuration files.

    make start VIRKAILIJA_CONFIG=../ataru-secrets/virkailija-my-config.edn HAKIJA_CONFIG=../ataru-secrets/hakija-my-config.edn

Now your local instances are running using your custom configuration.

### AWS service integration

Currently S3 integration is used in non-dev environments for storage of
temporary files accrued when uploading attachments in parts (for upload resume
support). By default the local file system (/tmp) is used for temporary file
storage.

In order to use S3 in development environments, add the following to your
configuration file:

```
:aws {:region "eu-west-1"
      :temp-files {:bucket "opintopolku-<env>-ataru-temp-files"}}
```

and provide credentials when running the hakija application, e.g.

```
AWS_ACCESS_KEY_ID=abc AWS_SECRET_ACCESS_KEY=xyz CONFIG=../ataru-secrets/hakija-<env>.edn lein hakija-dev
```

## Running tests

### Running Playwright test

When Playwright is updated or installed for the first time, it needs some dependencies installed. Since we only use Chromium in tests, needed dependencies can be installed with:

    npx playwright install --with-deps chromium

Start the service locally with make start command

    make start VIRKAILIJA_CONFIG=$PWD/config/cypress.edn HAKIJA_CONFIG=$PWD/config/cypress.edn

Then run all Playwright tests 

   npx playwright test

See more Playwright CLI-tips at https://playwright.dev/docs/test-cli

You can also use Playwright [VSCode-extension](https://playwright.dev/docs/getting-started-vscode) for running and debugging tests.

**If you want to write new tests, please use Playwright. Hopefully at some point all integration tests will use Playwright.**

### Running Cypress tests

Start the service locally with make start command

    make start VIRKAILIJA_CONFIG=$PWD/config/cypress.edn HAKIJA_CONFIG=$PWD/config/cypress.edn

Then either open Cypress with command

    npm run cypress:open

or run it headless using command

    npm run cypress:run

### Cypress & Playwright tests in Github Actions

Github Actions runs Cypress & Playwright tests with separate configuration (ClojureScript is compiled with `:advanced` optimizations for improved page load performance). All server logs are automatically uploaded to S3.
Both Playwright and Cypress tests are run together in the same CI-job and use shared config and browser in CI. 

### Running legacy browser tests

Tests require a database, a Redis and a FTPS server. Here is an example of
running those with Docker:

```
docker run -d --name ataru-test-db -p 5433:5432 -e POSTGRES_DB=ataru-test -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph ataru-test-db
docker run --name ataru-test-redis -p 6380:6379 -d redis
docker run -d --name ataru-test-ftpd -p 2221:21 -p 30000-30009:30000-30009 ataru-test-ftpd
```

The tests also require the `lftp` command to be available.

Tests assume some fixtures in the db. To clear the test db, run migrations and
insert the required fixtures by running:

```
./bin/cibuild.sh reset-test-database-with-fixture
```

For Github Actions CI the ataru-test-db and ataru-test-ftpd images have to be
available as
`190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-postgres`
and
`190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-ftpd`.
To make that happen, first login by executing the output of:

```
aws ecr get-login --region eu-west-1 --profile oph-utility --no-include-email
```

You should then be able to push images to the repository:

```
docker tag ataru-test-db 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-postgres
docker push 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-postgres
docker tag ataru-test-ftpd 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-ftpd
docker push 190073735177.dkr.ecr.eu-west-1.amazonaws.com/utility/hiekkalaatikko:ataru-test-ftpd
```

To build and run all the tests in the system:

```
./bin/cibuild.sh run-tests
```

### All tests

```
make test
```

### Backend unit tests

```
make start-docker test-clojure
```

#### Single backend unit test

```
lein with-profile test spec <PATH_TO_TEST_FILE>
```

e.g.

```
lein with-profile test spec spec/ataru/applications/answer_util_spec.clj
```

Hint: you can also run only individual tests in a file by 
temporarily naming them with `focus-it` instead of `it`, see: 
[http://micahmartin.com/speclj/speclj.core.html#var-focus-it](http://micahmartin.com/speclj/speclj.core.html#var-focus-it)

### ClojureScript unit tests

```
make test-clojurescript
```

### Browser tests

To run only browser tests (headless, using puppeteer):

```
make start-docker test-browser
```

To run browser tests using a real browser start both virkailija and hakija
applications with `lein hakija-dev` and `lein virkailija-dev`.

Then navigate to a test suite of your choosing, e.g. [http://localhost:8350/lomake-editori/virkailija-test.html](http://localhost:8350/lomake-editori/virkailija-test.html)

### Running browser tests locally

This process is highly fragile and the test running should be rewritten.

First, stop all running systems:

```
make stop
```

Start the servers using following configuration overrides:

```
make start VIRKAILIJA_CONFIG=$PWD/config/test.edn HAKIJA_CONFIG=$PWD/config/test.edn
```

Then, load the test data fixture to database:

```
make load-test-fixture
```

After the service starts and fixture data is loaded, navigate to local URL to run the tests (in incognito mode): http://localhost:8350/lomake-editori/virkailija-test.html

Test suites can be found in karma-runner.js

**NOTE**: after each run you need to manually compile test code and load the fixture data to test database using following command, otherwise the test will fail.

```
make compile-test-code load-test-fixture
```

## API documentation

Swagger specs for the APIs can be found at

- <http://localhost:8351/hakemus/swagger.json>
- <http://localhost:8350/lomake-editori/swagger.json>

Swagger UI can be found at

- <http://localhost:8351/hakemus/api-docs/index.html>
- <http://localhost:8350/lomake-editori/api-docs/index.html>

## Anonymize data

Before transfering data between environments one can anonymize the data by running

```
CONFIG=path-to-application-config.edn lein anonymize-data fake-person-file.txt
```

## Updating dependencies

Because vulnerability scanning tools don't work well with clojure, pom.xml is used for scanning. If you update dependencies to project.clj, run `lein pom` to update pom.xml accordingly

## Troubleshooting

### `make start` hangs in container creation

If your build gets stuck in the phase where all containers are listed by `docker-compose` like so:

```bash
Step 6/7 : RUN chmod a=,u=rw /etc/ssl/private/pure-ftpd.pem
 ---> Using cache
 ---> c6033ca419e9
Step 7/7 : CMD /run.sh -l puredb:/etc/pure-ftpd/pureftpd.pdb -E -j -R -P $PUBLICHOST -s -A -j -Z -H -4 -E -R -X -x -d -d --tls 3
 ---> Using cache
 ---> 1818a90a9990
Successfully built 1818a90a9990
Successfully tagged ataru_ataru-test-ftpd:latest
COMPOSE_PARALLEL_LIMIT=8 docker-compose up -d
Creating network "ataru_ataru-test-network" with the default driver
Creating network "ataru_cypress-http-proxy-network" with driver "bridge"
Creating ataru_ataru-dev-db_1 ...
Creating ataru_ataru-cypress-test-db_1 ...
Creating ataru_ataru-test-redis_1      ...
Creating ataru-cypress-http-proxy      ...
Creating ataru_ataru-test-db_1         ...
Creating ataru_ataru-dev-redis_1       ...
Creating ataru_ataru-test-ftpd_1       ...
Creating ataru-cypress-test-redis      ...
```

and there's no containers running as shown by `docker ps`:

```bash
➜  ~ docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```

try running Docker Compose manually with

```bash
docker-compose up -d
```

If everything starts, run `make stop` and now `make start` should work as expected. Why? Who knows...

## Running locally (Mac OS X)

In order to run locally against qa, hahtuva or untuva, another cloned repo is needed: https://github.com/Opetushallitus/ataru-secrets, cloned e.g. in parallel folder with _ataru_

Before running ataru locally, you need to setup ssh tunneling connection to the corresponding bastion server.
You can use configurations on the ataru-secrets repo to set up ssh port forwardings, see readme in dev-local-config folder: https://github.com/Opetushallitus/ataru-secrets/tree/master/dev_local_config

then local run whould be launched e.q. as follows:

```
make start VIRKAILIJA_CONFIG=../ataru-secrets/virkailija-qa.edn HAKIJA_CONFIG=../ataru-secrets/hakija-qa.edn
```

### Leiningen installation and possible issues

[leiningen](https://formulae.brew.sh/formula/leiningen) - `brew install leiningen`

Preferred version of Java for leiningen is Java17. Before project run check that Leiningen tool points to correct Java Open JDK version: `lein -v`

```
Leiningen 2.10.0 on Java 17 OpenJDK 64-Bit Server VM
```

and this is the same version that system uses as default e.g.: `java --version` and `java -version`

```
openjdk 17 2021-09-14
OpenJDK Runtime Environment Temurin-17+35 (build 17+35)
OpenJDK 64-Bit Server VM Temurin-17+35 (build 17+35, mixed mode)
```

Test `lein` tool by running the following command to print dependency tree

```
lein deps :tree
```

Output should be like this:

```
[aleph "0.6.3"]
   [io.netty.incubator/netty-incubator-transport-native-io_uring "0.0.18.Final" :classifier "linux-aarch_64"]
   [io.netty.incubator/netty-incubator-transport-native-io_uring "0.0.18.Final" :classifier "linux-x86_64"]
     [io.netty.incubator/netty-incubator-transport-classes-io_uring "0.0.18.Final"]
   [io.netty/netty-codec-http "4.1.94.Final"]
   [io.netty/netty-codec "4.1.94.Final"]
   [io.netty/netty-handler-proxy "4.1.94.Final"]
     [io.netty/netty-codec-socks "4.1.94.Final"]
   [io.netty/netty-handler "4.1.94.Final"]
   [io.netty/netty-resolver-dns "4.1.94.Final"]
     [io.netty/netty-codec-dns "4.1.94.Final"]
   [io.netty/netty-resolver "4.1.94.Final"]
   [io.netty/netty-transport-native-epoll "4.1.94.Final" :classifier "linux-aarch_64"]
   [io.netty/netty-transport-native-epoll "4.1.94.Final" :classifier "linux-x86_64"]
     [io.netty/netty-transport-classes-epoll "4.1.94.Final"]
     [io.netty/netty-transport-native-unix-common "4.1.94.Final"]
   [io.netty/netty-transport-native-kqueue "4.1.94.Final" :classifier "osx-x86_64"]
     [io.netty/netty-transport-classes-kqueue "4.1.94.Final"]
   [io.netty/netty-transport "4.1.94.Final"]
     [io.netty/netty-buffer "4.1.94.Final"]
     [io.netty/netty-common "4.1.94.Final"]
   [manifold "0.3.0" :exclusions [[org.clojure/tools.logging]]]
   [metosin/malli "0.10.4" :exclusions [[org.clojure/clojure]]]
     [borkdude/dynaload "0.3.5"]
     [borkdude/edamame "1.3.20"]
     [fipp "0.6.26"]
       [org.clojure/core.rrb-vector "0.1.2"]
     [mvxcvi/arrangement "2.1.0"]
   [org.clj-commons/byte-streams "0.3.1"]
   [org.clj-commons/dirigiste "1.0.3"]
   [org.clj-commons/primitive-math "1.0.0"]
   [org.clojure/tools.logging "1.2.4" :exclusions [[org.clojure/clojure]]]
 [binaryage/devtools "1.0.7"]
 [bk/ring-gzip "0.3.0"]
 [buddy/buddy-auth "3.0.323"]
   [buddy/buddy-sign "3.4.333"]
     [buddy/buddy-core "1.10.413"]
       [org.bouncycastle/bcpkix-jdk15on "1.70"]
         [org.bouncycastle/bcutil-jdk15on "1.70"]
       [org.bouncycastle/bcprov-jdk15on "1.70"]
 [camel-snake-kebab "0.4.3"]
 [cheshire "5.12.0"]
   [com.fasterxml.jackson.core/jackson-core "2.15.2"]
   [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.15.2" :exclusions [[com.fasterxml.jackson.core/jackson-databind]]]
   [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.15.2" :exclusions [[com.fasterxml.jackson.core/jackson-databind]]]
   [tigris "0.1.2"]
 [cider/piggieback "0.5.3" :scope "test"]
 [clj-commons/secretary "1.2.4"]
   [com.cemerick/clojurescript.test "0.2.3-20140317.141743-3"]
 [clj-http "3.12.3" :exclusions [[commons-io]]]
   [commons-codec "1.16.0" :exclusions [[org.clojure/clojure]]]
   [org.apache.httpcomponents/httpclient-cache "4.5.13" :exclusions [[org.clojure/clojure]]]
   [org.apache.httpcomponents/httpclient "4.5.13" :exclusions [[org.clojure/clojure]]]
   [org.apache.httpcomponents/httpmime "4.5.13" :exclusions [[org.clojure/clojure]]]
   [slingshot "0.12.2" :exclusions [[org.clojure/clojure]]]
 [clj-time "0.15.2"]
   [joda-time "2.10"]
 [cljs-ajax "0.8.4"]
   [com.cognitect/transit-clj "1.0.333"]
     [com.cognitect/transit-java "1.0.371"]
       [javax.xml.bind/jaxb-api "2.4.0-b180830.0359"]
         [javax.activation/javax.activation-api "1.2.0"]
       [org.msgpack/msgpack "0.6.12"]
         [com.googlecode.json-simple/json-simple "1.1.1" :exclusions [[junit]]]
         [org.javassist/javassist "3.18.1-GA"]
   [com.cognitect/transit-cljs "0.8.264"]
     [com.cognitect/transit-js "0.8.861"]
   [org.apache.httpcomponents/httpasyncclient "4.1.5"]
     [commons-logging "1.2"]
     [org.apache.httpcomponents/httpcore-nio "4.4.15"]
   [org.apache.httpcomponents/httpcore "4.4.16"]
 [cljsjs/react-dom "18.2.0-1"]
 [cljsjs/react "18.2.0-1"]
 [com.amazonaws/aws-java-sdk-s3 "1.12.558"]
   [com.amazonaws/aws-java-sdk-core "1.12.558"]
     [software.amazon.ion/ion-java "1.0.2"]
   [com.amazonaws/aws-java-sdk-kms "1.12.558"]
   [com.amazonaws/jmespath-java "1.12.558"]
 [com.amazonaws/aws-java-sdk-sns "1.12.558"]
 [com.amazonaws/aws-java-sdk-sqs "1.12.558"]
 [com.andrewmcveigh/cljs-time "0.5.2"]
 [com.cemerick/url "0.1.1"]
   [pathetic "0.5.0"]
 [com.fzakaria/slf4j-timbre "0.4.0" :exclusions [[io.aviso/pretty]]]
 [com.github.ben-manes.caffeine/caffeine "3.1.8"]
   [com.google.errorprone/error_prone_annotations "2.21.1"]
   [org.checkerframework/checker-qual "3.37.0"]
 [com.github.fge/json-patch "1.9"]
   [com.github.fge/jackson-coreutils "1.8"]
     [com.fasterxml.jackson.core/jackson-databind "2.15.2"]
       [com.fasterxml.jackson.core/jackson-annotations "2.15.2"]
     [com.github.fge/msg-simple "1.1"]
       [com.github.fge/btf "1.2"]
 [com.google.guava/guava "31.1-jre"]
   [com.google.code.findbugs/jsr305 "3.0.2"]
   [com.google.guava/failureaccess "1.0.1"]
   [com.google.guava/listenablefuture "9999.0-empty-to-avoid-conflict-with-guava"]
   [com.google.j2objc/j2objc-annotations "1.3"]
 [com.googlecode.owasp-java-html-sanitizer/owasp-java-html-sanitizer "20220608.1" :exclusions [[com.google.guava/guava]]]
 [com.jcraft/jsch "0.1.55"]
 [com.layerware/hugsql "0.5.3"]
   [com.layerware/hugsql-adapter-clojure-java-jdbc "0.5.3"]
   [com.layerware/hugsql-core "0.5.3"]
     [com.layerware/hugsql-adapter "0.5.3"]
 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
 [com.rpl/specter "1.1.4"]
   [riddley "0.2.0"]
 [com.stuartsierra/component "1.1.0"]
   [com.stuartsierra/dependency "1.0.0"]
 [com.taoensso/carmine "3.2.0" :exclusions [[io.aviso/pretty]]]
   [com.taoensso/nippy "3.2.0"]
     [org.iq80.snappy/snappy "0.4"]
     [org.lz4/lz4-java "1.8.0"]
     [org.tukaani/xz "1.9"]
   [org.apache.commons/commons-pool2 "2.11.1"]
 [com.taoensso/timbre "6.2.2"]
   [com.taoensso/encore "3.68.0"]
     [com.taoensso/truss "1.11.0"]
   [io.aviso/pretty "1.1.1"]
 [compojure "1.7.0"]
   [clout "2.2.1"]
   [org.clojure/tools.macro "0.1.5"]
   [ring/ring-codec "1.2.0"]
   [ring/ring-core "1.10.0"]
     [commons-fileupload "1.5"]
     [crypto-equality "1.0.1"]
     [crypto-random "1.2.1"]
 [criterium "0.4.6" :scope "test"]
 [environ "1.2.0"]
 [fi.vm.sade.java-utils/java-properties "0.1.0-20220622.111904-268"]
 [fi.vm.sade/auditlogger "9.2.0-20210312.091036-1"]
   [com.google.code.gson/gson "2.10.1"]
   [com.tananaev/json-patch "1.2"]
   [org.apache.commons/commons-lang3 "3.12.0"]
 [fi.vm.sade/scala-cas_2.12 "2.2.2.1-20230320.182907-1"]
   [org.http4s/http4s-blaze-client_2.12 "0.16.6a"]
     [org.http4s/http4s-blaze-core_2.12 "0.16.6a"]
       [org.http4s/blaze-http_2.12 "0.12.11"]
         [com.twitter/hpack "1.0.2"]
         [org.eclipse.jetty.alpn/alpn-api "1.1.3.v20160715"]
         [org.http4s/blaze-core_2.12 "0.12.11"]
   [org.http4s/http4s-client_2.12 "0.16.6a" :exclusions [[org.scala-lang/scala-library] [org.slf4j/slf4j-api]]]
     [org.http4s/http4s-core_2.12 "0.16.6a"]
       [org.http4s/http4s-parboiled2_2.12 "0.16.6a"]
       [org.http4s/http4s-websocket_2.12 "0.2.0"]
       [org.log4s/log4s_2.12 "1.4.0"]
       [org.scalaz.stream/scalaz-stream_2.12 "0.8.6a"]
         [org.scalaz/scalaz-concurrent_2.12 "7.2.7"]
           [org.scalaz/scalaz-effect_2.12 "7.2.7"]
         [org.scodec/scodec-bits_2.12 "1.1.2"]
       [org.scalaz/scalaz-core_2.12 "7.2.17"]
       [org.typelevel/macro-compat_2.12 "1.1.1"]
   [org.http4s/http4s-dsl_2.12 "0.16.6a"]
   [org.scala-lang.modules/scala-xml_2.12 "2.2.0"]
   [org.scala-lang/scala-library "2.12.18"]
   [org.slf4j/slf4j-api "2.0.9"]
 [figwheel-sidecar "0.5.20" :scope "test"]
   [clj-stacktrace "0.2.8"]
   [co.deps/ring-etag-middleware "0.2.1" :scope "test"]
   [figwheel "0.5.20" :scope "test" :exclusions [[org.clojure/tools.reader]]]
   [hawk "0.2.11" :exclusions [[org.clojure/clojure]]]
     [net.incongru.watchservice/barbary-watchservice "1.0"]
       [net.java.dev.jna/jna "3.2.2"]
   [http-kit "2.3.0" :scope "test"]
   [ring-cors "0.1.13" :scope "test" :exclusions [[ring/ring-core] [org.clojure/clojure]]]
   [simple-lein-profile-merge "0.1.4" :scope "test"]
   [strictly-specking-standalone "0.1.1" :scope "test"]
     [net.cgrand/parsley "0.9.3" :scope "test" :exclusions [[org.clojure/clojure]]]
       [net.cgrand/regex "1.1.0" :scope "test"]
     [net.cgrand/sjacket "0.1.1" :scope "test" :exclusions [[org.clojure/clojure] [net.cgrand/parsley]]]
   [suspendable "0.1.1" :scope "test" :exclusions [[org.clojure/clojure] [com.stuartsierra/component]]]
 [funcool/cuerdas "2.2.0"]
 [hikari-cp "3.0.1"]
   [com.zaxxer/HikariCP "5.0.1"]
 [markdown-clj "1.11.7"]
   [clj-commons/clj-yaml "1.0.26"]
     [org.flatland/ordered "1.5.9"]
     [org.yaml/snakeyaml "1.33"]
 [medley "1.4.0"]
 [metosin/compojure-api "1.1.13"]
   [frankiesardo/linked "1.3.0"]
   [metosin/ring-swagger-ui "2.2.10"]
   [metosin/ring-swagger "0.26.2"]
     [metosin/scjsv "0.5.0"]
       [com.github.java-json-tools/json-schema-validator "2.2.10"]
         [com.github.java-json-tools/json-schema-core "1.2.10"]
           [com.github.fge/uri-template "0.9"]
           [com.github.java-json-tools/jackson-coreutils "1.9"]
           [org.mozilla/rhino "1.7.14"]
         [com.googlecode.libphonenumber/libphonenumber "8.0.0"]
         [javax.mail/mailapi "1.4.3"]
           [javax.activation/activation "1.1"]
         [net.sf.jopt-simple/jopt-simple "5.0.3"]
   [org.tobereplaced/lettercase "1.0.0"]
   [potemkin "0.4.6"]
     [clj-tuple "0.2.2"]
   [prismatic/plumbing "0.5.5"]
     [de.kotka/lazymap "3.1.0" :exclusions [[org.clojure/clojure]]]
   [ring-middleware-format "0.7.5"]
     [clojure-msgpack "1.2.1"]
     [org.clojure/core.memoize "1.0.257"]
 [metosin/ring-http-response "0.9.3"]
 [metosin/schema-tools "0.13.1"]
 [nrepl "1.0.0"]
 [oph/clj-access-logging "1.0.0-20230921.142041-67" :exclusions [[javax.xml.bind/jaxb-api]]]
   [io.findify/s3mock_2.12 "0.2.6"]
     [com.github.pathikrit/better-files_2.12 "3.9.1"]
     [com.typesafe.akka/akka-http_2.12 "10.1.12"]
       [com.typesafe.akka/akka-http-core_2.12 "10.1.12"]
         [com.typesafe.akka/akka-parsing_2.12 "10.1.12"]
     [com.typesafe.akka/akka-stream_2.12 "2.5.31"]
       [com.typesafe.akka/akka-actor_2.12 "2.5.31"]
         [com.typesafe/config "1.3.3"]
         [org.scala-lang.modules/scala-java8-compat_2.12 "0.8.0"]
       [com.typesafe.akka/akka-protobuf_2.12 "2.5.31"]
       [com.typesafe/ssl-config-core_2.12 "0.3.8"]
         [org.scala-lang.modules/scala-parser-combinators_2.12 "1.1.2"]
       [org.reactivestreams/reactive-streams "1.0.2"]
     [com.typesafe.scala-logging/scala-logging_2.12 "3.9.2"]
       [org.scala-lang/scala-reflect "2.12.7"]
     [org.iq80.leveldb/leveldb "0.12"]
       [org.iq80.leveldb/leveldb-api "0.12"]
     [org.scala-lang.modules/scala-collection-compat_2.12 "2.1.6"]
   [pl.allegro.tech/embedded-elasticsearch "2.10.0"]
     [com.fasterxml.jackson.dataformat/jackson-dataformat-yaml "2.6.2"]
     [org.rauschig/jarchivelib "1.0.0"]
 [oph/clj-ring-db-cas-session "0.3.0-20210312.103812-20"]
 [oph/clj-stdout-access-logging "1.0.0-20230921.142010-65" :exclusions [[com.google.guava/guava]]]
 [oph/clj-string-normalizer "0.1.0-20210312.103940-62" :exclusions [[org.jboss.logging/jboss-logging] [com.google.guava/guava]]]
   [thheller/shadow-cljs "2.8.95"]
     [com.bhauman/cljs-test-display "0.1.1"]
     [com.wsscode/pathom "2.2.31" :exclusions [[org.clojure/data.json] [fulcrologic/fulcro] [camel-snake-kebab]]]
       [com.wsscode/spec-inspec "1.0.0-alpha2"]
       [edn-query-language/eql "0.0.9"]
       [spec-coerce "1.0.0-alpha6"]
     [expound "0.8.4"]
     [io.undertow/undertow-core "2.0.30.Final" :exclusions [[org.jboss.xnio/xnio-api] [org.jboss.xnio/xnio-nio]]]
     [org.clojure/data.json "1.0.0"]
     [org.clojure/tools.cli "1.0.194"]
     [org.jboss.threads/jboss-threads "3.1.0.Final"]
     [org.jboss.xnio/xnio-api "3.8.0.Final"]
       [org.wildfly.client/wildfly-client-config "1.0.1.Final"]
       [org.wildfly.common/wildfly-common "1.5.2.Final"]
     [org.jboss.xnio/xnio-nio "3.8.0.Final"]
     [thheller/shadow-client "1.3.2"]
     [thheller/shadow-cljsjs "0.0.21"]
     [thheller/shadow-util "0.7.0"]
 [oph/clj-timbre-access-logging "1.1.0-20230921.142050-3" :exclusions [[com.google.guava/guava]]]
 [oph/clj-timbre-auditlog "0.2.0-20230921.142126-2" :exclusions [[com.google.guava/guava]]]
 [org.apache.poi/poi-ooxml "5.2.3"]
   [com.github.virtuald/curvesapi "1.07"]
   [commons-io "2.11.0"]
   [org.apache.commons/commons-collections4 "4.4"]
   [org.apache.commons/commons-compress "1.21"]
   [org.apache.logging.log4j/log4j-api "2.18.0"]
   [org.apache.poi/poi-ooxml-lite "5.2.3"]
   [org.apache.poi/poi "5.2.3"]
     [com.zaxxer/SparseBitSet "1.2"]
     [org.apache.commons/commons-math3 "3.6.1"]
   [org.apache.xmlbeans/xmlbeans "5.1.1"]
 [org.clojure/clojure "1.11.1"]
   [org.clojure/core.specs.alpha "0.2.62"]
   [org.clojure/spec.alpha "0.3.218"]
 [org.clojure/clojurescript "1.11.121" :exclusions [[com.cognitect/transit-java]]]
   [com.google.javascript/closure-compiler-unshaded "v20220502"]
   [org.clojure/google-closure-library "0.0-20230227-c7c0a541"]
     [org.clojure/google-closure-library-third-party "0.0-20230227-c7c0a541"]
   [org.clojure/tools.reader "1.3.6"]
 [org.clojure/core.async "1.6.681"]
   [org.clojure/tools.analyzer.jvm "1.2.3"]
     [org.clojure/tools.analyzer "1.1.1"]
     [org.ow2.asm/asm "9.2"]
 [org.clojure/core.cache "1.0.225"]
   [org.clojure/data.priority-map "1.1.0"]
 [org.clojure/core.match "1.0.1"]
 [org.clojure/data.xml "0.0.8"]
 [org.clojure/java.jdbc "0.7.12"]
 [org.clojure/test.check "1.1.1"]
 [org.flywaydb/flyway-core "3.2.1"]
 [org.nrepl/incomplete "0.1.0" :exclusions [[org.clojure/clojure]]]
 [org.postgresql/postgresql "42.6.0" :exclusions [[org.checkerframework/checker-qual]]]
 [org.slf4j/log4j-over-slf4j "2.0.9"]
 [pandect "1.0.2"]
 [prismatic/schema "1.4.1"]
 [re-frame "1.3.0" :exclusions [[org.clojure/tools.logging]]]
   [net.cgrand/macrovich "0.2.1"]
 [reagent "1.2.0"]
 [reloaded.repl "0.2.4" :scope "test"]
   [org.clojure/tools.namespace "0.2.11"]
 [ring-ratelimit "0.2.3"]
 [ring/ring-defaults "0.4.0"]
   [javax.servlet/javax.servlet-api "3.1.0"]
   [ring/ring-anti-forgery "1.3.0"]
   [ring/ring-headers "0.3.0"]
   [ring/ring-ssl "0.3.0"]
 [ring/ring-json "0.5.1"]
 [ring/ring-mock "0.4.0"]
 [ring/ring-session-timeout "0.3.0"]
 [ring "1.10.0"]
   [ring/ring-devel "1.10.0"]
     [hiccup "1.0.5"]
     [ns-tracker "0.4.0"]
       [org.clojure/java.classpath "0.3.0"]
   [ring/ring-jetty-adapter "1.10.0"]
     [org.eclipse.jetty/jetty-server "9.4.51.v20230217"]
       [org.eclipse.jetty/jetty-http "9.4.51.v20230217"]
         [org.eclipse.jetty/jetty-util "9.4.51.v20230217"]
       [org.eclipse.jetty/jetty-io "9.4.51.v20230217"]
   [ring/ring-servlet "1.10.0"]
 [selmer "1.12.59"]
 [snipsnap "0.2.0" :scope "test" :exclusions [[org.clojure/clojure]]]
 [speclj-junit "0.0.11-20151116.130002-1" :scope "test"]
 [speclj "3.4.3"]
   [fresh "1.1.2"]
   [mmargs "1.2.0"]
   [trptcolin/versioneer "0.1.1"]
 [timbre-ns-pattern-level "0.1.2"]
 [venantius/accountant "0.2.5"]
 [yesql "0.5.3"]
   [instaparse "1.4.12" :exclusions [[org.clojure/clojure]]]
```

More info regarding Java setup on MacOs can be found [here](https://mkyong.com/java/how-to-set-java_home-environment-variable-on-mac-os-x/).

## Logs

Application logs are in /tmp folder.

Build/compilation logs are in logs/pm2 folder.

## Reloaded repl

Ataru backends use Component to wire up the system. User.clj also has reloaded.repl imported which means you can use
the reloaded pattern to restart backends in about one second (first start takes longer) using the following workflow:

1. Run the application with and/or backend specific environment variables (HAKIJARELOADED, VIRKAILIJARELOADED) set to true,
e.g. to run hakija with the reloaded functionality run:

```
make start VIRKAILIJA_CONFIG=../ataru-secrets/virkailija-local-dev.edn HAKIJA_CONFIG=../ataru-secrets/hakija-local-dev.edn HAKIJARELOADED=true
```

2. Run (in IntelliJ) a "Clojure REPL -> Remote" run configuration (port 3333 for lomake-editori, port 3335 for hakija).
3. To start and subsequently restart the backend, run in REPL:

```
(reset)
```

## Breakpoints

Backend breakpoints (using debug-repl library) can be used with the following steps:

1. Run the nREPL run configuration (see above)
2. In the REPL command line, go to the namespace to which you want to put breakpoints, e.g.

```
(in-ns 'ataru.valinta-tulos-service.valintatulosservice-client)
```

3. Import the required tooling in the REPL command line:

```
(require '[com.gfredericks.debug-repl.async :refer [break! wait-for-breaks]])
(require '[com.gfredericks.debug-repl :refer [unbreak!]])
```

4. Insert (break!) macro invocation in the code to places where you want execution to break
5. Wait for breaks in the REPL command line (120 is timeout in seconds)

```
(wait-for-breaks 120)
```

6. Use browser to invoke the code
7. REPL window should display something like

```
Hijacking repl for breakpoint: ...
```
8. Examine the context, run code, etc.
9. Continue execution by invoking the (unbreak!) macro in the REPL command line
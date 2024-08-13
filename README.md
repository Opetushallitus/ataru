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

    make start-cypress VIRKAILIJA_CONFIG=$PWD/config/cypress.edn HAKIJA_CONFIG=$PWD/config/cypress.edn

Then run all Playwright tests 

   npx playwright test

See more Playwright CLI-tips at https://playwright.dev/docs/test-cli

You can also use Playwright [VSCode-extension](https://playwright.dev/docs/getting-started-vscode) for running and debugging tests.

**If you want to write new tests, please use Playwright. Hopefully at some point all integration tests will use Playwright.**

### Running Cypress tests

Start the service locally with make start command

    make start-cypress VIRKAILIJA_CONFIG=$PWD/config/cypress.edn HAKIJA_CONFIG=$PWD/config/cypress.edn

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

If your build gets stuck in the phase where all containers are listed by `docker compose` like so:

```bash
Step 6/7 : RUN chmod a=,u=rw /etc/ssl/private/pure-ftpd.pem
 ---> Using cache
 ---> c6033ca419e9
Step 7/7 : CMD /run.sh -l puredb:/etc/pure-ftpd/pureftpd.pdb -E -j -R -P $PUBLICHOST -s -A -j -Z -H -4 -E -R -X -x -d -d --tls 3
 ---> Using cache
 ---> 1818a90a9990
Successfully built 1818a90a9990
Successfully tagged ataru_ataru-test-ftpd:latest
COMPOSE_PARALLEL_LIMIT=8 docker compose up -d
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
docker compose up -d
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

Test `lein` tool by running the following command to print the available profiles

```
lein show-profiles
```

Output should be like this:
```
base
debug
default
dev
figwheel
hakija-cypress
hakija-dev
leiningen/default
leiningen/test
offline
opintopolku-local
opintopolku-local-hakija
opintopolku-local-virkailija
test
uberjar
update
virkailija-cypress
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
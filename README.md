# Ataru

[![Build Status](https://github.com/Opetushallitus/ataru/actions/workflows/build.yml/badge.svg)](https://github.com/Opetushallitus/ataru/actions/workflows/build.yml)

A system for creating custom forms, applying to education and handling applications.

## How to start

Start all Ataru processes and docker containers using command

    make start

Stop all Ataru processes and docker containers using command

    make stop

See `make help` for details

## Running locally

When running locally, it is **highly** recommended to utilize the databases in any of the test environments. In order to run locally against the test environments (qa, hahtuva, or untuva), you need to clone the [ataru-secrets](https://github.com/Opetushallitus/ataru-secrets) repository. It is recommended to clone it to a folder parallel to Ataru.

Before running Ataru locally, you need to setup ssh tunneling connection to the corresponding bastion server, and be connected with SSO to the bastion server. For setting this up, please refer to the README in the `dev-local-config`-directory in the [ataru-secrets](https://github.com/Opetushallitus/ataru-secrets/tree/master/dev_local_config) repo. It will guide you on how to set up the SSO tunneling using the `cloud-base` repository, and adding the necessary changes to work with Ataru.

After this is set up, in one terminal, connect to the bastion server (QA in this example) like so:

```
ssh -F ~/.opintopolku/pallero.ssh.config bastion.pallero
```

Finally, run Ataru locally from the root of this project like so (assuming `ataru-secrets` is parallel to the `ataru` directory):

```
make start VIRKAILIJA_CONFIG=../ataru-secrets/virkailija-qa.edn HAKIJA_CONFIG=../ataru-secrets/hakija-qa.edn
```

### Leiningen installation and possible issues (Mac OS X)

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

### Running Playwright and Cypress tests

**If you write new tests, please use Playwright. Also consider migrating some legacy Cypress tests to Playwright.**

#### Running as in CI

Github Actions builds ClojureScript with `:advanced` optimizations, runs both Playwright and Cypress tests together. To reproduce this locally:

```
make ci-test-playwright-and-cypress
```

This command:

1. Builds ClojureScript with `:advanced` optimizations (same as CI)
2. Starts all required services and Docker containers
3. Runs Playwright tests inside a Docker container (using the official `mcr.microsoft.com/playwright` image matching the project's Playwright version)
4. Runs Cypress tests in headless CI mode
5. Stops all services and containers when done

**Prerequisites:** Docker must be running. The command handles everything else (dependency installation, service startup, and teardown) automatically.

#### Running Playwright/Cypress tests locally with manual startup

You can also start the app separately with cypress-configuration and then run the tests.

First make sure all services are stopped:

    make stop

Then start the services with the cypress config:

    make start-cypress VIRKAILIJA_CONFIG=$PWD/config/cypress.edn HAKIJA_CONFIG=$PWD/config/cypress.edn

Note that this command doesn't compile the code with advanced optimizations and also includes devtools, so the tests run much slower than using the CI-specific command.

Alternatively you can start the CI-app:

    make start-cypress-ci

The app startup takes a moment. If all your tests fail with timeouts, then it might be that it's not started yet.

When app is started, run all Playwright tests:

    pnpm exec playwright test

See more Playwright CLI-tips at <https://playwright.dev/docs/test-cli>

You can also use the Playwright [VSCode-extension](https://playwright.dev/docs/getting-started-vscode) for running and debugging tests.

Cypress tests can either be run interactively

    pnpm run cypress:open

or headless:

    pnpm run cypress:run

### All tests (except Cypress & Playwright)

```
make test
```

### Backend tests

Includes integration and unit tests

```
make start-docker test-clojure
```

#### Single backend unit test

```
lein with-profile test spec <PATH_TO_TEST_FILE>
```

e.g.

```
lein with-profile test spec spec/ataru/applications/applications.application_access_control_spec.clj
```

Hint: you can also run only individual tests in a file by
temporarily naming them with `focus-it` instead of `it`, see:
[http://micahmartin.com/speclj/speclj.core.html#var-focus-it](http://micahmartin.com/speclj/speclj.core.html#var-focus-it)

### ClojureScript unit tests

```
make test-clojurescript
```

## Static checks (Linting and type checking)

To run all static checks for code in repo:

```
make lint
```

To update clj-kondo configurations from dependencies to `.clj-kondo`-directory:

```
make clj-kondo-update-configs
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

Application logs are in logs folder.

Build/compilation logs are in logs/pm2 folder.

## Reloaded repl

Ataru backends use Component to wire up the system. User.clj also has reloaded.repl imported which means you can use
the reloaded pattern to restart backends in about one second (first start takes longer) using the following workflow:

1. Run the application with and/or backend specific environment variables (HAKIJARELOADED, VIRKAILIJARELOADED) set to true,
   e.g. to run hakija with the reloaded functionality run:

```
make start VIRKAILIJA_CONFIG=../ataru-secrets/virkailija-local-dev.edn HAKIJA_CONFIG=../ataru-secrets/hakija-local-dev.edn HAKIJARELOADED=true
```

1. Run (in IntelliJ) a "Clojure REPL -> Remote" run configuration (port 3333 for lomake-editori, port 3335 for hakija).
2. To start and subsequently restart the backend, run in REPL:

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

1. Import the required tooling in the REPL command line:

```
(require '[com.gfredericks.debug-repl.async :refer [break! wait-for-breaks]])
(require '[com.gfredericks.debug-repl :refer [unbreak!]])
```

1. Insert (break!) macro invocation in the code to places where you want execution to break
2. Wait for breaks in the REPL command line (120 is timeout in seconds)

```
(wait-for-breaks 120)
```

1. Use browser to invoke the code
2. REPL window should display something like

```
Hijacking repl for breakpoint: ...
```

```
Hijacking repl for breakpoint: ...
```

1. Examine the context, run code, etc.
2. Continue execution by invoking the (unbreak!) macro in the REPL command line

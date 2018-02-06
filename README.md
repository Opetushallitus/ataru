# Ataru

[![Dependencies Status](https://jarkeeper.com/Opetushallitus/ataru/status.svg)](https://jarkeeper.com/Opetushallitus/ataru)

A system for creating custom forms, applying to education and handling applications.

## Development Mode

### Compile css:

Compile css file once.

```
lein less once
```

Automatically recompiles css file on change.

```
lein less auto
```

### Create database

Just use the postgres Docker (9.5) image:

```
docker run -d --name ataru-dev-db -p 5432:5432 -e POSTGRES_DB=ataru-dev -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph postgres:9.5
```

### Redis caches

If you want to use Redis caches locally:

* Change cache type to redis in config
* Set Redis url in config to `redis://localhost:6379` in config
* Run Redis using docker: `docker run --name ataru-dev-redis -p 6379:6379 -d redis`

### Run application:

This will also allow you to connect to the nREPL servers of the jvm processes individually and change running code without restarting the JVM.

### Virkailija app

Virkailija has a certain amount of configurations containing private
secrets like passwords etc. To run it in full development mode, first
check out `https://github.com/Opetushallitus/ataru-secrets` (you'll
need privileges). You also need to forward untuva ldap to a local port:

```
ssh -L31337:ldap.ldap.untuvaopintopolku.fi:389 ubuntu@bastion.untuvaopintopolku.fi
```

Or you can edit your `~/.ssh/config` file to contain a host definition like:
 
```
Host bastion.untuvaopintopolku.fi
    LocalForward 31337 ldap.ldap.untuvaopintopolku.fi:389
    user <your username here>
    IdentityFile <your private key file path here>
    HostName bastion.untuvaopintopolku.fi
```

Possibly with multiple LocalForward definitions for whatever you need. Then you can simply `ssh ubuntu@bastion.untuvaopintopolku.fi`

After setting up the tunnel you can run:

`./bin/start-dev-build.sh`

Which starts all build processes in a new tmux instance. You can also manually run:

```
CONFIG=../ataru-secrets/virkailija-dev.edn lein virkailija-dev
(in another terminal)
lein figwheel virkailija-dev
```
The above assumes that your ataru-secrets repo is checked out beside
ataru repo. Figwheel will automatically push cljs changes to the browser.

It is recommended to use figwheel with rlwrap to enable a better ux for the repl!

Navigate to untuva virkailija.untuvaopintopolku.fi and login with ataru-user to get cas session.
Browse to [http://localhost:8350](http://localhost:8350).

You can also run a "minimal" version of the virkailija system with
just fake integrations (no organizations etc, hard-coded stuff):

```
lein virkailija-dev
```

(Above uses `config/dev.edn` by default, including the
unit/browser-testing database which will be wiped out when you run tests)

### Hakija app
```
CONFIG=../ataru-secrets/hakija-dev.edn lein hakija-dev
(in another terminal)
lein figwheel hakija-dev
```
Browse to [http://localhost:8351/hakemus/<uuid>](http://localhost:8351/hakemus/<uuid>).

### Backend & browser tests

Tests require a special database. Here is an example of running it
with Docker:

```
docker run -d --name ataru-test-db -p 5433:5432 -e POSTGRES_DB=ataru-test -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph postgres:9.5
```

To build and run all the tests in the system:

```
./bin/cibuild.sh run-tests
```

This takes a while, but is great for reproducing any issus which occur
on the CI build machine.

To run all clojure tests once:

```
lein spec
```

To run them automatically whenever code changes, use `-a`.

### Backend unit tests

```
lein spec -t unit
```

### ClojureScript unit tests

```
lein doo phantom test once
```

### Browser tests

To run only browser tests (headless, using phantomJS):

```
lein spec -t ui
```

Browser tests rely on having dummy implementations of certain
backend-services, e.g. organization service. Instantiating fake
versions of required services is configured in the normal
edn-config-files like this:

```
:dev {:fake-dependencies true}
```

To run browser tests using a real browser, e.g. Chrome, start both virkailija and hakija applications with:

```
lein hakija-dev
```

and

```
lein virkailija-dev
```

Remeber to compile all changed UI code before running UI-tests. To clean and recompile all UI code:

```
./bin/cibuild ui-compile
```

Also remember to restart applications after re-compiling code!

To run virkailija tests:
* Navigate to untuva virkailija.untuvaopintopolku.fi and login with ataru-user to get cas session.
* Navigate to [http://localhost:8350/lomake-editori/virkailija-test.html](http://localhost:8350/lomake-editori/virkailija-test.html)

That will run the tests for virkailija.

You can run only some of the tests with Mocha's grep feature, for example:
```http://localhost:8350/lomake-editori/virkailija-test.html?grep=Application```

Hakija has the following tests (no login required): 
* [http://localhost:8351/hakemus/hakija-form-test.html](http://localhost:8351/hakemus/hakija-form-test.html)
* [http://localhost:8351/hakemus/hakija-haku-test.html](http://localhost:8351/hakemus/hakija-haku-test.html)
* [http://localhost:8351/hakemus/hakija-hakukohde-test.html](http://localhost:8351/hakemus/hakija-hakukohde-test.html)
* [http://localhost:8351/hakemus/hakija-ssn-test.html](http://localhost:8351/hakemus/hakija-ssn-test.html)
* [http://localhost:8351/hakemus/hakija-edit-test.html](http://localhost:8351/hakemus/hakija-edit-test.html)

Tests assume some fixtures in db. To clear test db, run migrations and insert the required fixtures run:
`./bin/ci-build reset-test-database-with-fixture`

### ClojureScript unit tests

```
lein doo phantom test [once|auto]
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. 
However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS 
environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build

```
lein clean
lein cljsbuild once <app id>-min
```

## API documentation

Swagger specs for the APIs can be found at the following locations:

* Applicant API: <http://localhost:8351/hakemus/swagger.json>
* Officer API: <http://localhost:8350/lomake-editori/swagger.json>

## Anonymize data

Application data can be anonymized as follows:

```
CONFIG=path-to-application-config.edn lein anonymize-data
```

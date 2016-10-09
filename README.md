# ataru

Ataru is a name for a service used to create forms.

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

Just use the postgres Docker (9.4) image:

```
docker run -d --name ataru-dev-db -p 5432:5432 -e POSTGRES_DB=ataru-dev -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph postgres:9.5
```

### Run application:

This will also allow you to connect to the nREPL servers of the jvm processes individually and change running code without restarting the JVM.

### Virkailija app

Virkailija has a certain amount of configurations containing private
secrets like passwords etc. To run it in full development mode, first
check out `https://github.com/Opetushallitus/ataru-secrets` (you'll
need privileges). Then you can run:

```
CONFIG=../ataru-secrets/virkailija-dev.edn lein virkailija-dev
(in another terminal)
lein figwheel virkailija-dev
```
The above assumes that your ataru-secrets repo is checked out beside
ataru repo. Figwheel will automatically push cljs changes to the browser.

Browse to [http://localhost:8350](http://localhost:8350).

You can also run a "minimal" version of the virkailija system with
just fake integrations (no organizations etc, hard-coded stuff):

```
lein virkailija-dev
```

(Above uses config/dev.edn by default)

### Hakija app
```
lein hakija-dev
(in another terminal)
lein figwheel hakija-dev
```
Browse to [http://localhost:8351/hakemus/<uuid>](http://localhost:8351/hakemus/<uuid>).

_Note: figwheel nrepl ports now conflict (they are the same and it's not easy to configure
separate ports in project.clj), so you can run only either hakija/virkailija 
figwheel process at once. You can still run both applications just fine, but the other
 one will have to be either with lein cljsbuild once or auto <id>_

### Backend & browser tests

Tests require a special database. Here is an example of running it
with Docker:

```
docker run -d --name ataru-test-db -p 5433:5432 -e POSTGRES_DB=ataru-test -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph postgres:9.5
```

To run all tests once:

```
lein spec
```

To run them automatically whenever code changes, use `-a`.

### Backend unit tests

```
lein spec -t unit
```

### Browser integration tests

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

### Running integration tests on your browser

* Start the development server
* Navigate to [http://localhost:8350/lomake-editori/test.html](http://localhost:8350/lomake-editori/test.html)

Note that this assumes a blank database on which to run and create new data to! In other words, you might want to:

* Create a test database (see `config/test.edn` for details)
* Run your development server with the test profile: `lein virkailija-dev` (uses `config/dev.edn` settings)
* Wipe the test db between each test run (`lein spec -t ui` will do this automatically).

Alternatively, you can e.g. use Mocha's grep utility to run only the desired tests.

### ClojureScript unit tests

```
lein doo phantom test [once|auto]
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build

```
lein clean
lein cljsbuild once <app id>-min
```

## API documentation

Swagger specs for the APIs can be found at the following locations:

* Applicant API: <http://localhost:8351/hakemus/swagger.json>
* Officer API: <http://localhost:8350/lomake-editori/swagger.json>

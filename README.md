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
docker run -d --name oph -p 5432:5432 -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph postgres:9.4
```

### Run application:

This will also allow you to connect to the nREPL servers of the jvm processes individually and change running code without restarting the JVM.

### Virkailija app
```
lein virkailija-dev
(in another terminal)
lein figwheel virkailija-dev
```
Figwheel will automatically push cljs changes to the browser.

Browse to [http://localhost:8350](http://localhost:8350).

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

To run all tests once:

```
CONFIG=config/test.edn lein spec
```

To run them automatically whenever code changes, use `-a`.

### Backend unit tests

```
CONFIG=config/test.edn lein spec -t unit
```

### Browser integration tests

To run only browser tests (headless, using phantomJS):

```
CONFIG=config/test.edn lein spec -t ui
```

### Running integration tests on your browser

* Start the development server
* Navigate to [http://localhost:8350/lomake-editori/test.html](http://localhost:8350/lomake-editori/test.html)

Note that this assumes a blank database on which to run and create new data to! In other words, you might want to:

* Create a test database (see `config/test.edn` for details)
* Run your development server with the test profile: `CONFIG=config/test.edn lein virkailija-dev`
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

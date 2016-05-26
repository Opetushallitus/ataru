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

Install PostgreSQL

Init Postgres database files to a desired location (this will create a subdir here)

```
initdb -d atarudb
```

Run the server

```
postgres -D atarudb
```

In another shell, create the DB:

```
createdb ataru
```

Create user for the db:

```
createuser -s oph -P
```

Give it a password `oph`

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
Browse to [http://localhost:8351/hakemus/<id>](http://localhost:8351/hakemus/<id>).

_Note: figwheel nrepl ports now conflict (they are the same and it's not easy to configure
separate ports in project.clj), so you can run only either hakija/virkailija 
figwheel process at once. You can still run both applications just fine, but the other
 one will have to be either with lein cljsbuild once or auto <id>_

### Browser tests:

Browser tests can be run by invoking

```
npm install

# In one terminal window, run the actual application, for example:
lein virkailija-dev

# In second window, you'll want to run figwheel in virkailija-dev profile as normally:
lein figwheel virkailija-dev

# And in the third window, run cljsbuild for tests to automatically recompile test.js
lein cljsbuild auto browser-test
```

After this you can run tests by navigating to http://localhost:8350/lomake-editori/test.html .

### Backend tests

To run tests once:

```
lein spec
```

To run them automatically whenever code changes, use `-a`.

### ClojureScript unit tests

```
lein doo phantom test [once|auto]
```

### Clojure linter

```
lein eastwood
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn). 

## Production Build

```
lein clean
lein cljsbuild once <app id>-min
```

# lomake-editori

## Development Mode

### Compile css:

Compile css file once.

```
lein less once
```

Automatically recompile css file on change.

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

```
lein clean
lein with-profile figwheel-standalone figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

### Preferred way to run application

This way is preferred because you can shutdown the server and frontend separately from each other.

This will also allow you to connect to the nREPL servers of the jvm processes individually and change running code without restarting the JVM.

```
lein with-profile dev run
(in another terminal)
lein with-profile dev figwheel dev
```

Browse to [http://localhost:3450](http://localhost:3450).

### Run tests:

[doo](https://github.com/bensu/doo) assumes that [PhantomJS](http://phantomjs.org/) is installed locally to `./node_modules/phantomjs-prebuilt/bin/phantomjs`. The `package.json` in this repository does just that:

```
npm install
```

To actually run tests:

```
lein clean
lein doo phantom test once
```

### Clojure linter

```
lein eastwood
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn). 

## Production Build

```
lein clean
lein cljsbuild once min
```

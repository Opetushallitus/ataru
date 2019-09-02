# Ataru

A system for creating custom forms, applying to education and handling applications.

## Development setups

### Create database

./test-postgres contains a Dockerfile for a Postgres 10 based image with fi_FI
locale. To build the image:

```
docker build -t ataru-test-db -t ataru-dev-db ./test-postgres
```

Then to run a local development db:

```
docker run -d --name ataru-dev-db -p 5432:5432 -e POSTGRES_DB=ataru-dev -e POSTGRES_PASSWORD=oph -e POSTGRES_USER=oph ataru-dev-db
```

### Create Redis for caches

```
docker run --name ataru-dev-redis -p 6379:6379 -d redis
```

### Create a FTPS server for mocked ASHA integration

Ataru has an integration to ASHA for tutkintojen tunnustaminen process.
Certain actions (submiting an application, modifying it and changing the state
to inactivated) start background jobs that create an XML message and transfer
it to a FTP server for processing by ASHA.

For testing and local development a docker container running a FTP server is
used. Directory test-ftpd contains a Dockerfile and certificates for building
this container.

```
docker build -t ataru-test-ftpd -t ataru-dev-ftpd ./test-ftpd
```

Stop the dev-container (if running) to avoid port collision.

```
docker stop ataru-dev-ftpd
```

Then to run a local test FTP server:

```
docker run -d --name ataru-dev-ftpd -p 2221:21 -p 30000-30009:30000-30009 ataru-dev-ftpd
```

### Compile css:

```
lein less once
```

or

```
lein less auto
```

### Compile frontend code:

```
lein cljsbuild once virkailija-dev
```
```
lein cljsbuild once hakija-dev
```

### Run application:

This will also allow you to connect to the nREPL servers of the jvm processes
individually and change running code without restarting the JVM.

#### Local setup with mocked remote resources and using the local db

```
lein virkailija-dev
```
```
lein hakija-dev
```
```
lein figwheel virkailija-dev
```
```
lein figwheel hakija-dev
```

Note: currently it is not possible to run both figwheel commands above simultaneously.

#### Using remote resources

Secrets requiret for accessing remote resources are stored in a separate
private repository `https://github.com/Opetushallitus/ataru-secrets`. After
checking out the repository you can start the application using one of the
existing configurations.

```
CONFIG=../ataru-secrets/virkailija-<env>.edn lein virkailija-dev
```
```
CONFIG=../ataru-secrets/hakija-<env>.edn lein hakija-dev
```

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

For Travis CI the ataru-test-db and ataru-test-ftpd images have to be
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

### Backend unit tests

```
APP=virkailija lein spec -t unit
APP=hakija lein spec -t unit
```

### ClojureScript unit tests

```
lein doo chrome-headless test once
```

### Browser tests

To run only browser tests (headless, using puppeteer):

```
lein spec -t ui
```

To run browser tests using a real browser start both virkailija and hakija
applications with `lein hakija-dev` and `lein virkailija-dev`.

Then navigate to a test suite of your choosing, e.g. [http://localhost:8350/lomake-editori/virkailija-test.html](http://localhost:8350/lomake-editori/virkailija-test.html)

## API documentation

Swagger specs for the APIs can be found at

* <http://localhost:8351/hakemus/swagger.json>
* <http://localhost:8350/lomake-editori/swagger.json>

## Anonymize data

Before transfering data between environments one can anonymize the data by running

```
CONFIG=path-to-application-config.edn lein anonymize-data fake-person-file.txt
```

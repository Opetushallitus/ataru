
Basic usage:
------------

make start
	Starts all applications and docker containers

make start-dev:
        Starts the local dev applications and docker containers

make start-cypress: 
        Starts applications and docker containers needed for running Cypress/Playwright-tests

make stop
	Stop all applications and docker containers

make restart
	Restart all applications and docker containers

make test
	Run all tests

make lint
	Run linters

make check-ports
	With this target, you can check if some other application is already
        listening to a port that is used. If everything is ok, you get no
        output, otherwise you get the list of processes that use conflicting
        ports.

Advanced targets:
-----------------

make kill
	Kills pm2 and stops docker containers

make check-tools
	Verifies that all necessary tools are in path. Is called by most targets

make build-docker-images
	Build docker images using docker compose. Is called by many other targets

make status
	Shows status of applications and docker containers

make log, make logs
	Follow logs via PM2

make clean
	Clean project. Removes unused docker containers and cleans compiled classes

make clear-test-db, clear-cypress-db
        Clear the test database. Doesn't run migrations. Cypress/Playwright use separate database from other tests, and they are cleared automatically when running start-cypress.

make test-clojurescript, test-browser, test-clojure
        Test targets. These targets DO NOT clear the database between runs in order
        to speed up execution, so use with care.

make load-test-fixture
        This target resets test database and loads test fixture. It is useful for
        repeatedly running browser tests.

Makefile targets are provided for restarting certain applications. Examples:

make stop-hakija	Stops hakija application
make start-hakija	Starts hakija application
make restart-docker	Restarts docker containers
...

Inspect makefile for details

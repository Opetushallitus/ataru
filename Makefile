# Project-specific configuration
EXECUTABLES = lein docker docker-compose npm lftp
PORTS=15432 16379 15433 1221 16380 16381 8350 8351 8352 8353
TOOL_VERSIONS := node:8.11 npm:6 docker-compose:1.21 lein:2.9

VIRKAILIJA_CONFIG ?= ../ataru-secrets/virkailija-local-dev.edn
HAKIJA_CONFIG ?= ../ataru-secrets/hakija-local-dev.edn

VIRKAILIJA_RELOADED ?= false
HAKIJA_RELOADED ?= false

FIGWHEEL=ataru-figwheel
CSS_COMPILER=ataru-css-compilation
HAKIJA_BACKEND=ataru-hakija-backend-8351
VIRKAILIJA_BACKEND=ataru-virkailija-backend-8350
HAKIJA_CYPRESS_BACKEND=ataru-hakija-cypress-backend-8353
VIRKAILIJA_CYPRESS_BACKEND=ataru-virkailija-cypress-backend-8352

DEV_SERVICES = $(FIGWHEEL) $(CSS_COMPILER) $(HAKIJA_BACKEND) $(VIRKAILIJA_BACKEND)
CYPRESS_SERVICES = $(FIGWHEEL) $(CSS_COMPILER) $(HAKIJA_CYPRESS_BACKEND) $(VIRKAILIJA_CYPRESS_BACKEND)

DOCKER_CONTAINERS_DEV = ataru-dev-db ataru-dev-redis ataru-test-db ataru-test-ftpd ataru-test-redis
DOCKER_CONTAINERS_CYPRESS = ataru-cypress-test-db ataru-test-ftpd ataru-test-redis ataru-cypress-test-redis ataru-cypress-http-proxy

# General options
PM2=npx pm2 --no-autorestart
START_ONLY=start pm2.config.js --only
STOP_ONLY=stop pm2.config.js --only

DOCKER_SUDO ?=
DOCKER=$(if $(DOCKER_SUDO),sudo )docker
DOCKER_COMPOSE=COMPOSE_PARALLEL_LIMIT=8 $(if $(DOCKER_SUDO),sudo )docker-compose

NODE_MODULES=node_modules

# ----------------
# Check that all necessary tools are in path and have new enough versions
# ----------------
check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

	$(info Checking tool versions ...)
	@$(foreach TOOL_VERSION, $(TOOL_VERSIONS), \
		$(eval TOOL = $(word 1,$(subst :, ,$(TOOL_VERSION)))) \
		$(eval VERSION = $(word 2,$(subst :, ,$(TOOL_VERSION)))) \
		\
		./bin/check-tool-version.sh $(TOOL) $(VERSION) || exit 1; )

# ----------------
# Docker build
# ----------------
build-docker-images: check-tools generate-nginx-config
	$(DOCKER_COMPOSE) build

generate-nginx-config:
	@./bin/generate-nginx-conf.sh

# ----------------
# Npm installation
# ----------------
$(NODE_MODULES): package.json package-lock.json
	npm ci
	touch $(NODE_MODULES)

# ----------------
# Start apps
# ----------------
start-docker-all: build-docker-images
	$(DOCKER_COMPOSE) up -d

start-docker: build-docker-images
	$(DOCKER_COMPOSE) up -d $(DOCKER_CONTAINERS_DEV)

start-docker-cypress: build-docker-images
	$(DOCKER_COMPOSE) up -d $(DOCKER_CONTAINERS_CYPRESS)

start-pm2-all: $(NODE_MODULES) start-docker-all run-fake-deps-server
	$(PM2) start pm2.config.js

start-pm2: $(NODE_MODULES) start-docker
	$(foreach service, $(DEV_SERVICES), \
		$(PM2) $(START_ONLY) $(service) || exit 1;)

start-pm2-cypress: $(NODE_MODULES) start-docker-cypress run-fake-deps-server
	$(foreach service, $(CYPRESS_SERVICES), \
		$(PM2) $(START_ONLY) $(service) || exit 1;)

start-watch: $(NODE_MODULES)
	$(PM2) $(START_ONLY) $(FIGWHEEL)
	$(PM2) $(START_ONLY) $(CSS_COMPILER)

start-hakija: start-watch start-docker
	$(PM2) $(START_ONLY) $(HAKIJA_BACKEND)

start-virkailija: start-watch start-docker
	$(PM2) $(START_ONLY) $(VIRKAILIJA_BACKEND)

# ----------------
# Stop apps
# ----------------
stop-pm2: $(NODE_MODULES)
	$(PM2) stop pm2.config.js

stop-watch:
	$(PM2) $(STOP_ONLY) $(FIGWHEEL)
	$(PM2) $(STOP_ONLY) $(CSS_COMPILER)

stop-docker:
	$(DOCKER_COMPOSE) kill

stop-hakija:
	$(PM2) $(STOP_ONLY) $(HAKIJA_BACKEND)

stop-virkailija:
	$(PM2) $(STOP_ONLY) $(VIRKAILIJA_BACKEND)

# ----------------
# Restart apps
# ----------------
restart-hakija: start-hakija

restart-virkailija: start-virkailija

restart-docker: stop-docker start-docker

restart-watch: start-watch

# ----------------
# Clean commands
# ----------------
clean-docker:
	$(DOCKER_COMPOSE) stop
	$(DOCKER_COMPOSE) rm
	$(DOCKER) system prune -f

clean-lein:
	lein clean

# Fake dependencies for tests
run-fake-deps-server:
	./bin/fake-deps-server.sh start

stop-fake-deps-server:
	./bin/fake-deps-server.sh stop

# ----------------
# Database initialization
# ----------------
init-test-db: run-fake-deps-server
	lein with-profile test run -m ataru.db.flyway-migration/migrate "use dummy-audit-logger!"

nuke-test-db:
	lein with-profile test run -m ataru.fixtures.db.unit-test-db/clear-database

load-test-fixture: nuke-test-db init-test-db
	lein with-profile test run -m ataru.fixtures.db.browser-test-db/init-db-fixture

# ----------------
# Top-level commands (all apps)
# ----------------
start: start-pm2-all

start-dev: start-pm2

start-cypress: start-pm2-cypress

stop: stop-pm2 stop-docker stop-fake-deps-server

restart: stop-pm2 start-pm2

clean: nuke-test-db stop clean-lein clean-docker
	rm -rf node_modules
	rm *.log

status: $(NODE_MODULES)
	docker ps
	$(PM2) status

log: $(NODE_MODULES)
	$(PM2) logs --timestamp

# Alias for log
logs: log

lint: $(NODE_MODULES)
	npx eslint .

check-ports:
	@for PORT in $(PORTS); do sudo lsof -i :$$PORT -sTCP:LISTEN; done || exit 0

help:
	@cat Makefile.md

# ----------------
# Test db management
# ----------------

compile-test-code:
	./bin/compile-less.sh
	lein with-profile test cljsbuild once virkailija-min hakija-min

test-clojurescript: $(NODE_MODULES)
	lein with-profile test doo chrome test once

test-browser: $(NODE_MODULES) compile-test-code run-fake-deps-server
	lein with-profile test spec -t ui

test-clojure: nuke-test-db init-test-db
	lein with-profile test spec -t ~ui

test: start-docker test-clojurescript test-clojure test-browser

# ----------------
# Kill PM2 and all apps managed by it (= everything)
# ----------------
kill: stop-pm2 stop-docker
	$(PM2) kill


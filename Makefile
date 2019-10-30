# Project-specific configuration
EXECUTABLES = lein docker docker-compose npm lftp
PORTS=15432 16379 15433 1221 16380 8350 8351
TOOL_VERSIONS := node:8.11 npm:6 docker-compose:1.21 lein:2.9

VIRKAILIJA_CONFIG ?= ../ataru-secrets/virkailija-local-dev.edn
HAKIJA_CONFIG ?= ../ataru-secrets/hakija-local-dev.edn

FIGWHEEL=ataru-figwheel
CSS_COMPILER=ataru-css-compilation
HAKIJA_BACKEND=ataru-hakija-backend-8351
VIRKAILIJA_BACKEND=ataru-virkailija-backend-8350

# General options
PM2=npx pm2 --no-autorestart
START_ONLY=start pm2.config.js --only
STOP_ONLY=stop pm2.config.js --only

DOCKER_SUDO ?=
DOCKER=$(if $(DOCKER_SUDO),sudo )docker
DOCKER_COMPOSE=COMPOSE_PARALLEL_LIMIT=8 $(if $(DOCKER_SUDO),sudo )docker-compose

NODE_MODULES=node_modules/pm2/bin/pm2

# ----------------
# Check ataru-secrets existence and config files
# ----------------
ifeq ("$(wildcard $(VIRKAILIJA_CONFIG))","")
    $(error $(VIRKAILIJA_CONFIG) not found, clone/update ataru-secrets alongside ataru since configs are stored there)
endif

ifeq ("$(wildcard $(HAKIJA_CONFIG))","")
    $(error $(HAKIJA_CONFIG) not found, clone/update ataru-secrets alongside ataru since configs are stored there)
endif

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
build-docker-images: check-tools
	$(DOCKER_COMPOSE) build

# ----------------
# Npm installation
# ----------------
$(NODE_MODULES):
	npm install

# ----------------
# Start apps
# ----------------
start-docker: build-docker-images
	$(DOCKER_COMPOSE) up -d

start-pm2: $(NODE_MODULES) start-docker
	$(PM2) start pm2.config.js

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
	$(DOCKER_COMPOSE) down

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

# ----------------
# Top-level commands (all apps)
# ----------------
start: start-pm2

stop: stop-pm2 stop-docker

restart: stop-pm2 start-pm2

clean: stop clean-lein clean-docker
	rm -rf node_modules
	rm *.log

status: $(NODE_MODULES)
	docker ps
	$(PM2) status

log: $(NODE_MODULES)
	$(PM2) logs

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

test: start-docker
	CONFIG=config/test.edn ./bin/cibuild.sh run-tests

# ----------------
# Kill PM2 and all apps managed by it (= everything)
# ----------------
kill: stop-pm2 stop-docker
	$(PM2) kill


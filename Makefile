EXECUTABLES = lein docker docker-compose npm lftp

VIRKAILIJA_CONFIG ?= ../ataru-secrets/virkailija-local-dev.edn
HAKIJA_CONFIG ?= ../ataru-secrets/hakija-local-dev.edn

FIGWHEEL=ataru-figwheel
CSS_COMPILER=ataru-css-compilation
HAKIJA_BACKEND=ataru-hakija-backend-8351
VIRKAILIJA_BACKEND=ataru-virkailija-backend-8350

PM2=npx pm2 --no-autorestart
START_ONLY=start pm2.config.js --only
STOP_ONLY=stop pm2.config.js --only

DOCKER_COMPOSE=COMPOSE_PARALLEL_LIMIT=8 docker-compose

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
# Check that all necessary tools are in path
# ----------------
check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

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
	docker-compose down

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
	docker-compose stop
	docker-compose rm
	docker system prune -f

clean-lein:
	lein clean

# ----------------
# Top-level commands (all apps)
# ----------------
start: start-pm2

stop: stop-pm2

restart: stop-pm2 start-pm2

clean: stop clean-lein clean-docker
	rm -rf node_modules
	rm *.log

status: $(NODE_MODULES)
	docker ps
	$(PM2) status

log: $(NODE_MODULES)
	$(PM2) logs

lint: $(NODE_MODULES)
	npx eslint .

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


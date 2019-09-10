EXECUTABLES = lein docker docker-compose npm lftp

VIRKAILIJA_DEV_CONFIG=../ataru-secrets/virkailija-local-dev.edn
HAKIJA_DEV_CONFIG=../ataru-secrets/hakija-local-dev.edn

ifeq ("$(wildcard $(VIRKAILIJA_DEV_CONFIG))","")
    $(error $(VIRKAILIJA_DEV_CONFIG) not found, clone/update ataru-secrets alongside ataru since configs are stored there)
endif

ifeq ("$(wildcard $(HAKIJA_DEV_CONFIG))","")
    $(error $(HAKIJA_DEV_CONFIG) not found, clone/update ataru-secrets alongside ataru since configs are stored there)
endif

check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

docker-clean:
	docker system prune -f

lein-clean:
	lein clean

build-docker-images: check-tools
	docker-compose build

install-node-modules: check-tools
	npm install

start-pm2: build-docker-images
	npx pm2 start pm2.config.js

stop-pm2: install-node-modules
	npx pm2 stop pm2.config.js

start: start-pm2

stop: stop-pm2

clean: lein-clean docker-clean

kill: stop-pm2
	npx pm2 kill


EXECUTABLES = lein docker docker-compose npm lftp

check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

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

kill: stop-pm2
	npx pm2 kill


EXECUTABLES = lein docker docker-compose npm

check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

build-docker-images:
	docker-compose build

install-npm:
	npm install

start-pm2:
	npx pm2 start pm2.config.js

stop-pm2:
	npx pm2 stop pm2.config.js

start: install-npm build-docker-images start-pm2

stop: stop-pm2

kill: stop-pm2
	npx pm2 kill


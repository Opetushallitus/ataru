EXECUTABLES = lein docker docker-compose npm

check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

install-npm:
	npm install

start-pm2:
	npx pm2 start pm2.config.js

stop-pm2:
	npx pm2 stop pm2.config.js

docker-images:
	docker build -t ataru-test-db -t ataru-dev-db ./test-postgres
	docker build -t ataru-test-ftpd -t ataru-dev-ftpd ./test-ftpd

start: install-npm docker-images

stop: stop-pm2

kill:
	npx pm2 kill

source-to-image:
	LEIN_ROOT=true ./bin/cibuild.sh create-uberjar

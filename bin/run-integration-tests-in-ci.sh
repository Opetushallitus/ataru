#!/usr/bin/env sh

echo "Creating directory for logs"
mkdir -p logs/pm2

echo "Generating nginx configuration"
./bin/generate-nginx-conf.sh || exit 1

echo "Starting Docker containers for integration tests"
docker compose up -d ataru-cypress-test-db ataru-cypress-test-redis ataru-cypress-http-proxy
./bin/wait-for.sh localhost:8354 -t 20 || exit 1

echo "Running ClojureScript build for integration tests"
time lein cljsbuild once virkailija-cypress-ci hakija-cypress-ci

echo "Running less compilation for integration tests"
time ./bin/compile-less.sh

echo "Ensuring Cypress binary is available"
pnpm exec cypress install || exit 1

echo "Starting services for integration tests"

pnpm exec pm2 start pm2.ci.config.js

echo "Waiting for local services to become available"
./bin/wait-for.sh localhost:8352 -t 500 || exit 1
./bin/wait-for.sh localhost:8353 -t 500 || exit 1
./bin/wait-for.sh localhost:8354 -t 500 || exit 1

echo "Running integration tests"
time pnpm exec playwright test && pnpm run cypress:run:ci
RESULT=$?

echo "Stopping processes used by integration tests"
pnpm exec pm2 kill
docker compose kill ataru-cypress-test-db ataru-cypress-http-proxy
docker compose rm -f ataru-cypress-test-db ataru-cypress-http-proxy

if [ $RESULT != 0 ]; then
  echo "Integration tests failed! Please see logs for more info"
fi

exit $RESULT

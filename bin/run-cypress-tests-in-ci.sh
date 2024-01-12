#!/usr/bin/env sh

echo "Creating directory for logs"
mkdir -p logs/pm2

echo "Generating nginx configuration"
./bin/generate-nginx-conf.sh || exit 1

echo "Starting Docker containers for Cypress tests"
docker-compose up -d ataru-cypress-test-db ataru-cypress-test-redis ataru-cypress-http-proxy
./bin/wait-for.sh localhost:8354 -t 10 || exit 1

echo "Running ClojureScript build for Cypress tests"
time lein cljsbuild once virkailija-cypress-ci hakija-cypress-ci

echo "Running less compilation for Cypress tests"
time ./bin/compile-less.sh

echo "Starting services for Cypress tests"

npx pm2 start pm2.ci.config.js

echo "Waiting for local services to become available"
./bin/wait-for.sh localhost:8352 -t 500 || exit 1
./bin/wait-for.sh localhost:8353 -t 500 || exit 1
./bin/wait-for.sh localhost:8354 -t 500 || exit 1

echo "Running Cypress tests"
time npm run cypress:run:ci
RESULT=$?

echo "Stopping processes used by Cypress tests"
npx pm2 kill
docker-compose kill ataru-cypress-test-db ataru-cypress-http-proxy
docker-compose rm -f ataru-cypress-test-db ataru-cypress-http-proxy

if [ $RESULT != 0 ]; then
  echo "Cypress tests failed! Please see logs and screenshots from AWS S3"
fi

exit $RESULT

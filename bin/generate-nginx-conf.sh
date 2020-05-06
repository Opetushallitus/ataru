#!/usr/bin/env bash

REPOSITORY_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )"/.. >/dev/null 2>&1 && pwd )"

CONF_FILE=$REPOSITORY_ROOT/docker/ataru-cypress-http-proxy/etc/nginx/nginx.conf
CONF_TEMPLATE=$REPOSITORY_ROOT/docker/ataru-cypress-http-proxy/etc/nginx/nginx.conf.template

rm -f $CONF_FILE

if [ -x "$(command -v ip)" ]; then
  export DOCKER_HOST_ADDRESS=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')
else
  export DOCKER_HOST_ADDRESS="host.docker.internal"
fi

envsubst '$DOCKER_HOST_ADDRESS' < $CONF_TEMPLATE > $CONF_FILE

#!/usr/bin/env bash

set -eo pipefail

PORTTI=55443
DATAHAKEMISTO="`dirname $0`/../spec/test-fake-dependencies"
PIDFILE=/tmp/ataru-fake-deps-server.pid

if [[ -f ${PIDFILE} ]]; then
  AJOSSA_OLEVA_PID=`cat ${PIDFILE}`
fi


function start() {
  echo "Käynnistetään koodistopalvelua, organisaatiopalvelua ym feikkaavava palvelin porttiin ${PORTTI}"
  echo "palvelemaan sisältöä hakemistosta ${DATAHAKEMISTO}"
  if [[ ! -z "$AJOSSA_OLEVA_PID" ]]; then
    echo "Varoitus: Löytyi jo käynnistetty palvelu pidillä $AJOSSA_OLEVA_PID . Stopataan se ensin."
    kill ${AJOSSA_OLEVA_PID} || true
    wait ${AJOSSA_OLEVA_PID} || true
  fi

  pnpm exec http-server ${DATAHAKEMISTO} -p ${PORTTI} &
  FAKE_SERVER_PID=$!
  echo ${FAKE_SERVER_PID} > ${PIDFILE}
  echo "Käynnistettiin prosessi $FAKE_SERVER_PID ja tallennettiin pid tiedostoon $PIDFILE ."
}

function stop() {
  if [[ ! -z "$AJOSSA_OLEVA_PID" ]]; then
    echo "Pysäytetään prosessi $AJOSSA_OLEVA_PID ..."
    kill ${AJOSSA_OLEVA_PID} || true
    wait ${AJOSSA_OLEVA_PID} || true
  fi
  rm -f ${PIDFILE}
}

command="$1"

case "$command" in
    "start" )
        start
        ;;
    "stop" )
        stop
        ;;
    *)
  echo "Käyttö: $0 <start|stop>"
  exit 2
esac

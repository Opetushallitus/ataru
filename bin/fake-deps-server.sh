#!/usr/bin/env bash

set -eo pipefail

PORTTI=55443
JUURIHAKEMISTO="$(cd "$(dirname "$0")/.." && pwd)"
DATAHAKEMISTO="${JUURIHAKEMISTO}/spec/test-fake-dependencies"
HTTP_SERVER_BIN="${JUURIHAKEMISTO}/node_modules/http-server/bin/http-server"
PIDFILE=/tmp/ataru-fake-deps-server.pid

if [[ -f ${PIDFILE} ]]; then
  AJOSSA_OLEVA_PID=`cat ${PIDFILE}`
fi

function hae_portin_kuuntelijat() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:${PORTTI} -sTCP:LISTEN || true
  fi
}

function pysayta_portin_kuuntelijat() {
  PORTIN_PIDS=$(hae_portin_kuuntelijat)
  if [[ ! -z "$PORTIN_PIDS" ]]; then
    echo "Pysäytetään porttia ${PORTTI} kuuntelevat prosessit: $PORTIN_PIDS"
    kill ${PORTIN_PIDS} || true
  fi
}

function start() {
  echo "Käynnistetään koodistopalvelua, organisaatiopalvelua ym feikkaavava palvelin porttiin ${PORTTI}"
  echo "palvelemaan sisältöä hakemistosta ${DATAHAKEMISTO}"
  if [[ ! -z "$AJOSSA_OLEVA_PID" ]]; then
    echo "Varoitus: Löytyi jo käynnistetty palvelu pidillä $AJOSSA_OLEVA_PID . Stopataan se ensin."
    kill ${AJOSSA_OLEVA_PID} || true
    wait ${AJOSSA_OLEVA_PID} 2>/dev/null || true
  fi
  pysayta_portin_kuuntelijat
  PORTIN_PIDS=$(hae_portin_kuuntelijat)
  if [[ ! -z "$PORTIN_PIDS" ]]; then
    echo "Virhe: portti ${PORTTI} on edelleen varattu prosesseilla: $PORTIN_PIDS"
    exit 1
  fi

  if [[ ! -f ${HTTP_SERVER_BIN} ]]; then
    echo "Virhe: http-server ei löytynyt polusta ${HTTP_SERVER_BIN}"
    exit 1
  fi

  node ${HTTP_SERVER_BIN} ${DATAHAKEMISTO} -p ${PORTTI} &
  FAKE_SERVER_PID=$!
  echo ${FAKE_SERVER_PID} > ${PIDFILE}
  echo "Käynnistettiin prosessi $FAKE_SERVER_PID ja tallennettiin pid tiedostoon $PIDFILE ."
}

function stop() {
  if [[ ! -z "$AJOSSA_OLEVA_PID" ]]; then
    echo "Pysäytetään prosessi $AJOSSA_OLEVA_PID ..."
    kill ${AJOSSA_OLEVA_PID} || true
    wait ${AJOSSA_OLEVA_PID} 2>/dev/null || true
  fi
  pysayta_portin_kuuntelijat
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

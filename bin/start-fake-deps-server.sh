#!/usr/bin/env bash

set -euo pipefail

PORTTI=55443
DATAHAKEMISTO="`dirname $0`/../spec/test-fake-dependencies"
echo "Käynnistetään koodistopalvelua, organisaatiopalvelua ym feikkaavava palvelin porttiin ${PORTTI}"
echo "palvelemaan sisältöä hakemistosta ${DATAHAKEMISTO}"
npx http-server -p ${PORTTI} ${DATAHAKEMISTO} &


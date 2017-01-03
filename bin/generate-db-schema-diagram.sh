#!/bin/bash

set -e

if [ $# -ne 5 ]
then
    printf "Usage: $0 <db host> <db port> <db name> <output dir> <version>\n"
    exit 1
fi

HOST=$1
PORT=$2
DB=$3
OUT=$4
VERSION=$5
PREFIX="$4/ataru-${VERSION}"

mkdir -p "${OUT}"
postgresql_autodoc -s public -d "${DB}" -h "${HOST}" -p "${PORT}" -f "${PREFIX}" && dot -Tpng "${PREFIX}.dot" -o"${PREFIX}.png"

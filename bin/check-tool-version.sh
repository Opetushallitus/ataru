#!/bin/bash

TOOL=$1
EXPECTED_VERSION=$2

# Source: https://stackoverflow.com/questions/4023830/how-to-compare-two-strings-in-dot-separated-version-format-in-bash
vercomp () {
    if [[ $1 == $2 ]]
    then
        return 0
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            return 1
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            return 2
        fi
    done
    return 0
}

VERSION=$($TOOL --version | sed -nEe 's/^[^0-9]*(([0-9]+\.)*[0-9]+).*/\1/p')

vercomp $VERSION $EXPECTED_VERSION
RESULT=$?

if [[ "$RESULT" -gt 1 ]]; then
    echo "$TOOL ... installed version $VERSION is too old! update to version $EXPECTED_VERSION"
    exit 1
else
    echo "$TOOL ... version ok ($VERSION)"
    exit 0
fi

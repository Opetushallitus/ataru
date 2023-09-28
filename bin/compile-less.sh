#!/bin/bash

for FILE in resources/less/*.less; do
  NAME=${FILE##*/}
  BASE=${NAME%.less}
  node_modules/less/bin/lessc $FILE resources/public/css/compiled/${BASE}.css;
done
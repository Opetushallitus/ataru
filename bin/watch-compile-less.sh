#!/bin/bash

node_modules/.bin/less-watch-compiler resources/less resources/public/css/compiled virkailija-site.less &
node_modules/.bin/less-watch-compiler resources/less resources/public/css/compiled hakija-site.less &
wait
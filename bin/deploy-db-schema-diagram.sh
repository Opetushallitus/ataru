#!/bin/bash

scp ./target/db-schema/*.png bamboo@pulpetti:/var/www/html/db
scp ./target/db-schema/*.html bamboo@pulpetti:/var/www/html/db


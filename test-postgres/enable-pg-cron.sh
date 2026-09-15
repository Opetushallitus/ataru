#!/bin/bash
# pg_cron toimii vain shared_preload_libraries -kirjastona, ja sen extension
# voidaan luoda ainoastaan cron.database_name -asetuksen mukaiseen kantaan.
set -e

cat >> "$PGDATA/postgresql.conf" <<EOF

shared_preload_libraries = 'pg_cron'
cron.database_name = '$POSTGRES_DB'
EOF

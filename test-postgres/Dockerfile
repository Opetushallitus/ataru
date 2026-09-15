ARG POSTGRESQL_VERSION
FROM docker.io/library/postgres:$POSTGRESQL_VERSION
RUN localedef -i fi_FI -c -f UTF-8 -A /usr/share/locale/locale.alias fi_FI.UTF-8
ENV LANG fi_FI.utf8
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-${PG_MAJOR}-cron \
    && rm -rf /var/lib/apt/lists/*
COPY enable-pg-cron.sh /docker-entrypoint-initdb.d/

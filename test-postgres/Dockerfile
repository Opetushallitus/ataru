ARG POSTGRESQL_VERSION
FROM docker.io/library/postgres:$POSTGRESQL_VERSION
RUN localedef -i fi_FI -c -f UTF-8 -A /usr/share/locale/locale.alias fi_FI.UTF-8
ENV LANG fi_FI.utf8

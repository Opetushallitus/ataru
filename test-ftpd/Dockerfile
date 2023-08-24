FROM docker.io/stilliard/pure-ftpd:hardened
ENV FTP_USER_NAME=oph
ENV FTP_USER_PASS=oph
ENV FTP_USER_HOME=/home/ftpusers/oph
ADD --chown=root:root pure-ftpd.pem /etc/ssl/private/
RUN chmod a=,u=rw /etc/ssl/private/pure-ftpd.pem
CMD /run.sh -l puredb:/etc/pure-ftpd/pureftpd.pdb -E -j -R -P $PUBLICHOST -s -A -j -Z -H -4 -E -R -X -x -d -d --tls 3

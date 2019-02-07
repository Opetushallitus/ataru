FROM sickp/alpine-sshd:latest
ADD --chown=root:root ssh_host_rsa_key /etc/ssh/
ADD --chown=root:root ssh_host_rsa_key.pub /etc/ssh/
RUN chmod a=,u=rw /etc/ssh/ssh_host_rsa_key

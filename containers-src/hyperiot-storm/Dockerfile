FROM fabric8/java-alpine-openjdk11-jre:latest

# Install required packages
RUN apk add --no-cache \
    bash \
    python \
    su-exec \
    gcompat libstdc++

ENV STORM_USER=storm \
    STORM_CONF_DIR=/conf \
    STORM_DATA_DIR=/data \
    STORM_LOG_DIR=/logs

# Add a user and make dirs
RUN set -ex; \
    adduser -D "$STORM_USER"; \
    mkdir -p "$STORM_CONF_DIR" "$STORM_DATA_DIR" "$STORM_LOG_DIR"; \
    chown -R "$STORM_USER:$STORM_USER" "$STORM_CONF_DIR" "$STORM_DATA_DIR" "$STORM_LOG_DIR"``

ARG GPG_KEY=ACEFE18DD2322E1E84587A148DE03962E80B8FFD
ARG DISTRO_NAME=apache-storm-2.4.0

# Download Apache Storm, verify its PGP signature, untar and clean up
RUN set -ex; \
    apk add --no-cache --virtual .build-deps \
      gnupg; \
    wget -q "https://archive.apache.org/dist/storm/$DISTRO_NAME/$DISTRO_NAME.tar.gz"; \
    wget -q "https://archive.apache.org/dist/storm/$DISTRO_NAME/$DISTRO_NAME.tar.gz.asc"; \
    export GNUPGHOME="$(mktemp -d)"; \
#    gpg --keyserver ha.pool.sks-keyservers.net --recv-key "$GPG_KEY" || \
#    gpg --keyserver pgp.mit.edu --recv-keys "$GPG_KEY" || \
#    gpg --keyserver keyserver.pgp.com --recv-keys "$GPG_KEY"; \
#    gpg --batch --verify "$DISTRO_NAME.tar.gz.asc" "$DISTRO_NAME.tar.gz"; \
    tar -xzf "$DISTRO_NAME.tar.gz"; \
    chown -R "$STORM_USER:$STORM_USER" "$DISTRO_NAME"; \
    rm -rf "$GNUPGHOME" "$DISTRO_NAME.tar.gz" "$DISTRO_NAME.tar.gz.asc"; \
    apk del .build-deps

WORKDIR $DISTRO_NAME

ENV PATH $PATH:/$DISTRO_NAME/bin

# Storm ACS-Topology
COPY acsoftware /$DISTRO_NAME/acsoftware

# Storm dependencies for running HyperIoT topologies
COPY ./storm_extlib/* /$DISTRO_NAME/extlib/
RUN echo /$DISTRO_NAME/extlib/


COPY docker-entrypoint.sh /
ENTRYPOINT ["/docker-entrypoint.sh"]

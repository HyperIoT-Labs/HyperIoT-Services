FROM alpine:3.20.0

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH=/usr/lib/jvm/java-17-openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

# Install required packages
RUN apk update && apk add --no-cache bash python3 openjdk17 java-cacerts jq curl su-exec gcompat libstdc++

ENV STORM_USER=storm \
    STORM_CONF_DIR=/conf \
    STORM_DATA_DIR=/data \
    STORM_LOG_DIR=/logs

# Add a user and make dirs
RUN set -ex; \
    adduser -D "$STORM_USER"; \
    mkdir -p "$STORM_CONF_DIR" "$STORM_DATA_DIR" "$STORM_LOG_DIR"; \
    chown -R "$STORM_USER:$STORM_USER" "$STORM_CONF_DIR" "$STORM_DATA_DIR" "$STORM_LOG_DIR"``

ARG DISTRO_NAME=apache-storm-2.7.1

# Download Apache Storm, verify its PGP signature, untar and clean up
RUN set -ex; \
    apk add --no-cache --virtual .build-deps \
      gnupg; \
    wget -q "https://archive.apache.org/dist/storm/$DISTRO_NAME/$DISTRO_NAME.tar.gz"; \
    wget -q "https://archive.apache.org/dist/storm/$DISTRO_NAME/$DISTRO_NAME.tar.gz.asc"; \
    export GNUPGHOME="$(mktemp -d)"; \
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

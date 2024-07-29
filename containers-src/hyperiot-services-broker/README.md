# Karaf-HyperIoT-Microservice with Storm dist

Custom docker image with Karaf-HyperIoT microservices and Storm binaries required for managing Storm topologies.

## Building
```
mvn clean package -Dkaraf.version=4.4.3 -Dhyperiot.version=2.2.4 -Dhyperiot.platform.version=2.2.4
docker build . -f Dockerfile-activemq-mqtt -t nexus.acsoftware.it:18079/hyperiot/karaf-activemq-mqtt:5.17.3-2.2.4-1  --build-arg KARAF_MICROSERVICES_VERSION=2.2.4 --load
```

### Multi platform build

```
mvn clean package
docker buildx  build . -f Dockerfile-activemq-mqtt -t nexus.acsoftware.it:18079/hyperiot/karaf-activemq-mqtt:5.16.3-1.3.1-3  --build-arg KARAF_MICROSERVICES_VERSION=1.3.1 --platform linux/arm/v7,linux/arm64/v8,linux/amd64 --push
```

Be careful: KARAF_MICROSERVICES_VERSION has the same value of the last part of the tag (i.e. 1.0.0).

## Publishing
```
docker push nexus.acsoftware.it:18079/hyperiot/karaf-activemq-mqtt:5.18.8-1.1.0
```


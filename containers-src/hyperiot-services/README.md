# Karaf-HyperIoT-Microservice with Storm dist

Custom docker image with Karaf-HyperIoT microservices and Storm binaries required for managing Storm topologies.

## Building
```
mvn clean package -Dhyperiot.version=x.x.x -Dhyperiot.platform.version=x.x.x -Dkaraf.version=x.x.x
docker build . -f Dockerfile-microservices -t nexus.acsoftware.it:18079/hyperiot/karaf-microservices:1.3.1-1 --build-arg KARAF_MICROSERVICES_VERSION=1.3.1
```

## Publishing
```
docker push nexus.acsoftware.it:18079/hyperiot/karaf-microservices:1.0.149
```


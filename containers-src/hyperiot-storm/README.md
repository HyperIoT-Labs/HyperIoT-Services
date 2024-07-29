# Storm

Custom Storm docker image with required dependencies for running HyperIoT topologies.
Dependency jars are copied in the `extlib` storm folder.

## Building
```
rm -rf ./storm_extlib/* && mvn dependency:copy-dependencies -DoutputDirectory=./storm_extlib -Dhyperiot.version=x.x.x -Dhyperiot.platform.version=x.x.x
docker build . -f Dockerfile -t nexus.acsoftware.it:18079/hyperiot/storm:1.0.10


FOR MULTI PLATFORM WITH DOCKER BUILDX

docker buildx build . -f Dockerfile -t nexus.acsoftware.it:18079/hyperiot/storm:1.1.5 --platform linux/arm/v7,linux/arm64/v8,linux/amd64 --push

```

## Publishing
```
docker push nexus.acsoftware.it:18079/hyperiot/storm:1.0.10
```

[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/big-data-europe/Lobby)

# docker-hbase


docker buildx build --no-cache -t nexus.acsoftware.it:18079/hyperiot/hbase-base:1.0.3 -f Dockerfile .  --platform linux/arm64/v8,linux/amd64 

docker buildx build --no-cache -t nexus.acsoftware.it:18079/hyperiot/hbase-master:1.0.3 -f Dockerfile .  --platform linux/arm64/v8,linux/amd64 

docker buildx build --no-cache -t nexus.acsoftware.it:18079/hyperiot/hbase-regionserver:1.0.3 -f Dockerfile .  --platform linux/arm64/v8,linux/amd64 

# Standalone
To run standalone hbase:
```
docker-compose -f docker-compose-standalone.yml up -d
```
The deployment is the same as in [quickstart HBase documentation](https://hbase.apache.org/book.html#quickstart).
Can be used for testing/development, connected to Hadoop cluster.

# Local distributed
To run local distributed hbase:
```
docker-compose -f docker-compose-distributed-local.yml up -d
```

This deployment will start Zookeeper, HMaster and HRegionserver in separate containers.

# Distributed
To run distributed hbase on docker swarm see this [doc](distributed/README.md):

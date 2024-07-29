[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/big-data-europe/Lobby)

# Changes

Version 1.1.0 introduces healthchecks for the containers.

# Hadoop Docker

## Supported Hadoop Versions
* 3.3.5 with OpenJDK 11

## Build

1) Build base image

docker build --no-cache -t nexus.acsoftware.it:18079/hyperiot/hadoop-base:1.0.${VERSIONE} -f Dockerfile .

For Multi-platform build run: 

docker buildx build --no-cache -t nexus.acsoftware.it:18079/hyperiot/hadoop-base:1.0.${VERSIONE} -f Dockerfile .  --platform linux/arm/v7,linux/arm64/v8,linux/amd64 

2) publish it 

docker push nexus.acsoftware.it:18079/hyperiot/hadoop-base:1.0.${VERSIONE}

3) build datanode and namenode images

4) Check always base image version from Dockerfiles container in namenode and datanode folders


## Redirect host machine traffic to Hadoop container

Create alias for container ip:

sudo ifconfig lo0 alias <ip_container> 

To delete alias 

sudo ifconfig lo0 alias <ip_container> unplumb

## Quick Start

To deploy an example HDFS cluster, run:
```
  docker-compose up
```

`docker-compose` creates a docker network that can be found by running `docker network list`, e.g. `dockerhadoop_default`.

Run `docker network inspect` on the network (e.g. `dockerhadoop_default`) to find the IP the hadoop interfaces are published on. Access these interfaces with the following URLs:

* Namenode: http://<dockerhadoop_IP_address>:50070/dfshealth.html#tab-overview
* History server: http://<dockerhadoop_IP_address>:8188/applicationhistory
* Datanode: http://<dockerhadoop_IP_address>:50075/
* Nodemanager: http://<dockerhadoop_IP_address>:8042/node
* Resource manager: http://<dockerhadoop_IP_address>:8088/

## Configure Environment Variables

The configuration parameters can be specified in the hadoop.env file or as environmental variables for specific services (e.g. namenode, datanode etc.):
```
  CORE_CONF_fs_defaultFS=hdfs://namenode:8020
```

CORE_CONF corresponds to core-site.xml. fs_defaultFS=hdfs://namenode:8020 will be transformed into:
```
  <property><name>fs.defaultFS</name><value>hdfs://namenode:8020</value></property>
```
To define dash inside a configuration parameter, use triple underscore, such as YARN_CONF_yarn_log___aggregation___enable=true (yarn-site.xml):
```
  <property><name>yarn.log-aggregation-enable</name><value>true</value></property>
```

The available configurations are:
* /etc/hadoop/core-site.xml CORE_CONF
* /etc/hadoop/hdfs-site.xml HDFS_CONF
* /etc/hadoop/yarn-site.xml YARN_CONF
* /etc/hadoop/httpfs-site.xml HTTPFS_CONF
* /etc/hadoop/kms-site.xml KMS_CONF

If you need to extend some other configuration file, refer to base/entrypoint.sh bash script.

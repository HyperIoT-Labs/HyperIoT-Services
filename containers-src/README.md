# LOGIN

**Docker login**: `nexus.acsoftware.it:18079/nexus/repository/acs-docker`

# KAFKA MANAGER

##### Build
```
docker build . -f Dockerfile-kafka-manager -t nexus.acsoftware.it:18079/nexus/repository/acs-docker/hyperiot/kafka-manager:1.3.3.21
```
##### Publish docker image
```
docker tag hyperiot/kafka-manager nexus.acsoftware.it:18079/nexus/repository/hyperiot/kafka-manager:1.3.3.21
docker push nexus.acsoftware.it:18079/nexus/repository/hyperiot/kafka-manager:1.3.3.21
```
**KafkaManager Console**: `localhost:9000`

# KARAF ACTIVEMQ MQTT

##### Create Karaf project with specific version
```
mvn archetype:generate \
  -DarchetypeGroupId=org.apache.karaf.archetypes \
  -DarchetypeArtifactId=karaf-assembly-archetype \
  -DarchetypeVersion=4.2.1 \
  -DgroupId=it.acsoftware.hyperiot.container \
  -DartifactId=karaf-activemq-mqtt \
  -Dversion=1.0.0 \
  -Dpackage=it.acsoftware.hyperiot.container
  
  sudo keytool -importcert -keystore /Library/Java/JavaVirtualMachines/jdk1.8.0_192.jdk/Contents/Home/jre/lib/security/cacerts -storepass changeit -file ./target/assembly/etc/karaf-pem --alias "karaf-cert"
```
##### Create project and build 
```
mvn clean package
```  
##### Notes
In `src/main/filtered-resources/etc` c'è il file con le feature da installare e far partire
```
docker build . -f Dockerfile-activemq-mqtt -t nexus.acsoftware.it:18079/hyperiot/karaf-activemq-mqtt:5.15.8-1.0.47
```
**ActiveMQ Web Console**: `http://localhost:8181/activemqweb`

Connect to karaf via ssh : ssh hadmin@127.0.0.1 -p 8102

# KARAF MICROSERVICES

##### Create Karaf project with specific version
```
mvn archetype:generate \
  -DarchetypeGroupId=org.apache.karaf.archetypes \
  -DarchetypeArtifactId=karaf-assembly-archetype \
  -DarchetypeVersion=4.2.1 \
  -DgroupId=it.acsoftware.hyperiot.container \
  -DartifactId=karaf-activemq-mqtt \
  -Dversion=1.0.0 \
  -Dpackage=it.acsoftware.hyperiot.container
```
##### Create project and build 
```
  mvn clean package
```
##### Notes
In src/main/filtered-resources/etc c'è il file con le feature da installare e far partire
```
docker build . -f Dockerfile-microservices -t nexus.acsoftware.it:18079/hyperiot/karaf-microservices:1.0.54
```

Connect to karaf via ssh : ssh hadmin@127.0.0.1 -p 8101

# KARAF HYPERIOT RUNTIME 

##### Create Karaf project with specific version
```
mvn archetype:generate \
  -DarchetypeGroupId=org.apache.karaf.archetypes \
  -DarchetypeArtifactId=karaf-assembly-archetype \
  -DarchetypeVersion=4.2.1 \
  -DgroupId=it.acsoftware.hyperiot.container \
  -DartifactId=karaf-activemq-mqtt \
  -Dversion=1.0.0 \
  -Dpackage=it.acsoftware.hyperiot.container
```
##### Create project and build 
```
  mvn clean package
```
##### Notes
In src/main/filtered-resources/etc c'è il file con le feature da installare e far partire
```
docker build . -f Dockerfile-hyperiot-runtime -t nexus.acsoftware.it:18079/hyperiot/karaf-runtime:1.0.0
```

Connect to karaf via ssh : ssh hadmin@127.0.0.1 -p 8101

# LOGSTASH | ELASTIC | KIBANA

**Kibana config**:
`./data/kibana/config`

**Kibana Console**:
`http://localhost:5601/app/kibana#/home?_g=()`

**Elastic config**:
`./data/elasticsearch/config`

**Elastic Base Endpoint**:
`http://localhost:9200`

**logStash config**:
`./data/logstash/config`

**Logstash Console**: ?

# KAFKA

Kafka container with HyperIoT Kafka-MQTT Connector

##### Building Docker image
```
cd ./container-src/docker-kafka
docker build . -f Dockerfile -t nexus.acsoftware.it:18079/hyperiot/hyperiot/kafka:1.0.0
```
##### Publish docker image:
```
docker push nexus.acsoftware.it:18079/hyperiot/hyperiot/kafka:1.0.0
```
##### Running the container
```
docker-compose -f docker-compose-zk-kafka.yml up
```

# POSTGRESQL

Custom postgres Docker image with data persistence and pre-configured user (hyperiot) and database (hyperiot).

##### Building Docker image
```
cd ./container-src/docker-postgresql
docker build . -f Dockerfile -t nexus.acsoftware.it:18079/hyperiot/acs-postgresql:1.0.0
```
##### Publish docker image:
```
docker push nexus.acsoftware.it:18079/hyperiot/acs-postgresql:1.0.0
```
##### Running the container 
```
docker-compose -f docker-compose-postgresql.yml up
```

# CREATE SELF CERTS

To create ONLY server certs keystore and truststore:
https://unix.stackexchange.com/questions/347116/how-to-create-keystore-and-truststore-using-self-signed-certificate

To create CLIENT certs:

openssl genrsa -out alice.key 2028
openssl req -new -key alice.key -out alice.csr
openssl x509 -sha256 -req -in alice.csr -out alice.crt -CA CA.pem -CAkey CA.key -CAcreateserial -days 1095

# Spark base

The Spark base image serves as a base image for the Spark master, Spark worker and Spark submit images. The user should not run this image directly. See [big-data-europe/docker-spark README](https://github.com/big-data-europe/docker-spark) for more information.

docker buildx build --no-cache -t nexus.acsoftware.it:18079/hyperiot/spark-base-temp:1.0.7 -f Dockerfile .  --platform linux/arm64/v8,linux/amd64 --push

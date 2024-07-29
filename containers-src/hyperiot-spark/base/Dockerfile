FROM python:3.8-slim

ENV ENABLE_INIT_DAEMON true
ENV INIT_DAEMON_BASE_URI http://identifier/init-daemon
ENV INIT_DAEMON_STEP spark_master_init

ENV SPARK_VERSION=3.4.0
ENV HADOOP_VERSION=3

COPY wait-for-step.sh /
COPY execute-step.sh /
COPY finish-step.sh /

RUN apt-get update && apt-get -y install software-properties-common libgl1 libx11-6 libxext6 libxi6 libxrender1 libxtst6 libatk-wrapper-java-jni
RUN apt-get update && apt-get -y install wget vim
RUN apt-get -y install zsh default-jre libnss3 libnss3-tools procps coreutils rsync \
      && ln -s /lib64/ld-linux-x86-64.so.2 /lib/ld-linux-x86-64.so.2 \
      && chmod +x *.sh \
      && wget https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz \
      && tar -xvzf spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz \
      && mv spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} spark \
      && rm spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz \
      && cd /


RUN apt-get clean

#Give permission to execute scripts
RUN chmod +x /wait-for-step.sh && chmod +x /execute-step.sh && chmod +x /finish-step.sh

# Fix the value of PYTHONHASHSEED
# Note: this is needed when you use Python 3.3 or greater
ENV PYTHONHASHSEED 1

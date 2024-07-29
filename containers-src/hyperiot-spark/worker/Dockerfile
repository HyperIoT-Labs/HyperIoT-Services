FROM nexus.acsoftware.it:18079/hyperiot/spark-base:2.0.0

RUN apt-get update
RUN apt-get -y install gcc git

RUN pip3 install happybase

COPY worker.sh /

ENV SPARK_WORKER_WEBUI_PORT 8081
ENV SPARK_WORKER_LOG /spark/logs

EXPOSE 8081

CMD ["/bin/bash", "/worker.sh"]

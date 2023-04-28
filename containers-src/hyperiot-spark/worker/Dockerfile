FROM nexus.acsoftware.it:18079/hyperiot/spark-base:1.0.8

COPY worker.sh /

ENV SPARK_WORKER_WEBUI_PORT 8081
ENV SPARK_WORKER_LOG /spark/logs

EXPOSE 8081

CMD ["/bin/bash", "/worker.sh"]

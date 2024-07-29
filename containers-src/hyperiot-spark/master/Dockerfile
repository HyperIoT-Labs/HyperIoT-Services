FROM nexus.acsoftware.it:18079/hyperiot/spark-base:2.0.0

COPY master.sh /

ENV SPARK_MASTER_PORT 7077
ENV SPARK_MASTER_WEBUI_PORT 8080
ENV SPARK_MASTER_LOG /spark/logs

EXPOSE 22 8080 7077 6066

CMD ["/bin/bash", "/master.sh"]

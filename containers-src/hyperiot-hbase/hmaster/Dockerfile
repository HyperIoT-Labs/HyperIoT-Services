#FROM hyperiot/hbase-base
FROM nexus.acsoftware.it:18079/hyperiot/hbase-base:1.0.8

#HBase log dir
ENV HBASE_LOG_DIR=$HBASE_HOME/logs

#root logger
ENV HBASE_ROOT_LOGGER=INFO,console,DRFA

# A string representing this instance of hbase. $USER by default.
ENV HBASE_IDENT_STRING=$USER

#log file name
ENV HBASE_LOGFILE=hbase-$HBASE_IDENT_STRING-master.log

COPY run.sh /run.sh

RUN chmod +x /run.sh

EXPOSE 16000 16010 9090

CMD ["/run.sh"]

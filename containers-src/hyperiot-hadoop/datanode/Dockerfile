FROM nexus.acsoftware.it:18079/hyperiot/hadoop-base:1.0.6
MAINTAINER Ivan Ermilov <ivan.s.ermilov@gmail.com>

HEALTHCHECK CMD curl -f http://localhost:50075/ || exit 1

ENV HDFS_CONF_dfs_datanode_data_dir=file:///hadoop/dfs/data
RUN mkdir -p /hadoop/dfs/data
VOLUME /hadoop/dfs/data

#Hadoop log dir
ENV HADOOP_LOG_DIR=$HADOOP_HOME/logs

#root logger
ENV HADOOP_ROOT_LOGGER=INFO,console,DRFA

# A string representing this instance of hadoop. $USER by default.
ENV HADOOP_IDENT_STRING=$USER

#log file name
ENV HADOOP_LOGFILE=hadoop-$HADOOP_IDENT_STRING-datanode.log

ADD run.sh /run.sh
RUN chmod a+x /run.sh

EXPOSE 9866 9865 9864 9867

CMD ["/run.sh"]

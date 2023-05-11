FROM nexus.acsoftware.it:18079/hyperiot/hadoop-base:1.0.6

MAINTAINER Ivan Ermilov <ivan.s.ermilov@gmail.com>

HEALTHCHECK CMD curl -f http://localhost:9870/ || exit 1

ENV HDFS_CONF_dfs_namenode_name_dir=file:///hadoop/dfs/name
RUN mkdir -p /hadoop/dfs/name
VOLUME /hadoop/dfs/name

#Hadoop log dir
ENV HADOOP_LOG_DIR=$HADOOP_HOME/logs

#root logger
ENV HADOOP_ROOT_LOGGER=INFO,console,DRFA

# A string representing this instance of hadoop. $USER by default.
ENV HADOOP_IDENT_STRING=$USER

#log file name
ENV HADOOP_LOGFILE=hadoop-$HADOOP_IDENT_STRING-namenode.log

ADD run.sh /run.sh
# prove per leggere file avro
ADD avro-tools-1.7.7.jar /avro-tools-1.7.7.jar
RUN chmod a+x /run.sh

EXPOSE 50100 50105 9870 9871 8020

CMD ["/run.sh"]

FROM nexus.acsoftware.it:18079/hyperiot/hadoop-base:1.0.5
MAINTAINER Ivan Ermilov <ivan.s.ermilov@gmail.com>

HEALTHCHECK CMD curl -f http://localhost:8188/ || exit 1

ENV YARN_CONF_yarn_timeline___service_leveldb___timeline___store_path=/hadoop/yarn/timeline
RUN mkdir -p /hadoop/yarn/timeline
VOLUME /hadoop/yarn/timeline

#Yarn log dir
ENV YARN_LOG_DIR=$HADOOP_HOME/logs

#root logger
ENV YARN_ROOT_LOGGER=INFO,console,DRFA

# A string representing this instance of yarn. $USER by default.
ENV YARN_IDENT_STRING=$USER

#log file name
ENV YARN_LOGFILE=yarn-$YARN_IDENT_STRING-nodemanager.log

ADD run.sh /run.sh
RUN chmod a+x /run.sh

EXPOSE 8188

CMD ["/run.sh"]

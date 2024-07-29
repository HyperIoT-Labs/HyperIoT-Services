FROM nexus.acsoftware.it:18079/hyperiot/hadoop-base:1.0.5
MAINTAINER Ivan Ermilov <ivan.s.ermilov@gmail.com>

HEALTHCHECK CMD curl -f http://localhost:8088/ || exit 1

#Yarn log dir
ENV YARN_LOG_DIR=$HADOOP_HOME/logs

#root logger
ENV YARN_ROOT_LOGGER=INFO,console,DRFA

# A string representing this instance of yarn. $USER by default.
ENV YARN_IDENT_STRING=$USER

#log file name
ENV YARN_LOGFILE=yarn-$YARN_IDENT_STRING-resourcemanager.log

ADD run.sh /run.sh
RUN chmod a+x /run.sh

EXPOSE 8088

CMD ["/run.sh"]

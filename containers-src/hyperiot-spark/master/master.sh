#!/bin/bash

#
# Copyright 2019-2023 ACSoftware
#
# Licensed under the Apache License, Version 2.0 (the "License")
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

#export SPARK_MASTER_HOST=`hostname`
export SPARK_MASTER_HOST    # on Kubernetes, hostname variable contains a non-public ip

. "/spark/sbin/spark-config.sh"

. "/spark/bin/load-spark-env.sh"

mkdir -p $SPARK_MASTER_LOG

export SPARK_HOME=/spark

rm -f $SPARK_HOME/conf/spark-defaults.conf  # remove file and recreate with new values
touch $SPARK_HOME/conf/spark-defaults.conf
echo "spark.eventLog.enabled true" >> $SPARK_HOME/conf/spark-defaults.conf
echo "spark.master $SPARK_MASTER" >> $SPARK_HOME/conf/spark-defaults.conf
echo "spark.eventLog.dir $SPARK_EVENT_LOG_DIR" >> $SPARK_HOME/conf/spark-defaults.conf
echo "spark.master.rest.enabled true" >> $SPARK_HOME/conf/spark-defaults.conf

ln -sf /dev/stdout $SPARK_MASTER_LOG/spark-master.out

cd /spark/bin && /spark/bin/spark-class org.apache.spark.deploy.master.Master -h $SPARK_MASTER_HOST -p $SPARK_MASTER_PORT --webui-port $SPARK_MASTER_WEBUI_PORT >> $SPARK_MASTER_LOG/spark-master.out

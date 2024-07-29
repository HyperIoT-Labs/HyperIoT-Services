/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.acsoftware.hyperiot.stormmanager.model;

public enum MessageConversionStrategy {
     HDFS_DLQ_HOUR_MESSAGE_STRATEGY,
     HDFS_DLQ_DAY_MESSAGE_STRATEGY,
     HDFS_DLQ_MONTH_MESSAGE_STRATEGY,
     HDFS_DLQ_YEAR_MESSAGE_STRATEGY,
     HDFS_DLQ_SEMESTER_MESSAGE_STRATEGY,
     HDFS_DLQ_QUARTER_MESSAGE_STRATEGY,
     HBASE_DLQ_EVENT_TABLE_MESSAGE_STRATEGY ,
     HBASE_DLQ_AVRO_TABLE_MESSAGE_STRATEGY ,
     HBASE_DLQ_TIMELINE_TABLE_MESSAGE_STRATEGY,
     HBASE_DLQ_ALARM_TABLE_MESSAGE_STRATEGY,
     HBASE_DLQ_EVENT_RULE_STATE_MESSAGE_STRATEGY,
     HBASE_DLQ_ERROR_TABLE_MESSAGE_STRATEGY
}

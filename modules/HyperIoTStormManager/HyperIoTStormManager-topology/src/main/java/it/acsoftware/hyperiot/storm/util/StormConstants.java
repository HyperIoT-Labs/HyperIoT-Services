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

package it.acsoftware.hyperiot.storm.util;

public final class StormConstants {

    private StormConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String AVRO_HPACKET_FIELD = "avroHPacket";

    public static final String AVRO_HPACKET_ATTACHMENTS = "avroHPacketAttachments";
    public static final String AVRO_HPROJECT_STREAM_ID_PREFIX = "hproject_%s";
    public static final String DAY_STREAM_ID = "day";
    public static final String DESERIALIZATION_BOLT_ID = "bolt-deserialization";
    public static final String DEVICEID_PACKETID_FIELD = "deviceId.packetId";
    public static final String ENRICHMENT_BOLT_ID = "bolt-enrichment";
    public static final String EVENT_BOLT_ID = "bolt-event";
    public static final String EVENT_COLUMN_FIELD = "eventColumn";
    public static final String EVENT_HPROJECT_STREAM_ID_PREFIX = "event_hproject_%s";
    public static final String EVENT_ID_FIELD = "eventId";
    public static final String EVENT_RULE_ID = "eventRuleId";
    public static final String EVENT_RULE_STATE_INFO_FIELD = "eventRuleStateField";
    public static final String ALARM_EVENT_DEVICE_ID = "alarmEventDeviceId";
    public static final String ALARM_STATE_FIELD = "alarmStateField";
    public static final String ALARM_EVENT_HPROJECT_STREAM_ID_PREFIX = "alarmevent_hproject_%s";
    public static final String EVENT_JSON_FIELD = "event_json";
    public static final String EVENT_LAYER_KAFKA_BOLT = "bolt-kafka-eventlayer";
    public static final String HBASE_ALARM_BOLT_PREFIX = "hbase-alarm-bolt-";
    public static final String HBASE_EVENT_RULE_STATE_BOLT_PREFIX = "hbase-event-rule-state-bolt-";
    public static final String HBASE_AVRO_BOLT_PREFIX = "hbase-avro-bolt-";
    public static final String HBASE_BOLT_CONF = "hbase.conf";

    public static final String HBASE_COLUMN_FAMILY_HPACKET = "hpacket";

    public static final String HBASE_COLUMN_FAMILY_HPACKET_ATTACHMENTS = "attachments";

    public static final String HBASE_ALARM_TABLE_COLUMN_FAMILY_UP = "UP";

    public static final String HBASE_ALARM_TABLE_COLUMN_FAMILY_DOWN = "DOWN";

    public static final String HBASE_COLUMN_FAMILY_RULE = "rule";

    public static final String HBASE_ERROR_BOLT_PREFIX = "hbase-error-bolt-";
    public static final String HBASE_EVENT_BOLT_PREFIX = "hbase-event-bolt-";
    public static final String HBASE_ROWKEY = "rowkey";
    public static final String HBASE_TIMELINE_BOLT_PREFIX = "hbase-timeline-bolt-";
    public static final String HDFS_ERROR_STREAM_ID_PREFIX = "hdfs_error_%s";
    public static final String HDFS_PERSISTENCE_DAY = "bolt-hyperiot-day_%s";
    public static final String HDFS_PERSISTENCE_HOUR = "bolt-hyperiot-hour_%s";
    public static final String HDFS_PERSISTENCE_MONTH = "bolt-hyperiot-month_%s";
    public static final String HDFS_PERSISTENCE_QUARTER = "bolt-hyperiot-quarter_%s";
    public static final String HDFS_PERSISTENCE_SEMESTER = "bolt-hyperiot-semester_%s";
    public static final String HDFS_PERSISTENCE_YEAR = "bolt-hyperiot-year_%s";
    public static final String HOUR_STREAM_ID = "hour";
    public static final String HPACKET_FIELD = "hpacket";
    public static final String HPACKET_ID_FIELD = "hPacketId";
    public static final String HDEVICE_FIELD = "hdevice";
    public static final String HDEVICE_ID_FIELD = "hDeviceId";
    public static final String EVENT_RULE_STATE_TABLE_NAME_PREFIX = "hproject_event_rule_state_%s";
    public static final String HPROJECT_ERROR_STREAM_ID = "hproject_error_%s";
    public static final String KAFKA_BOOTSTRAP_SERVERS = "spout.kafka_servers";
    public static final String KAFKA_EVENT_KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    public static final String KAFKA_EVENT_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    public static final String KAFKA_REALTIME_KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    public static final String KAFKA_REALTIME_VALUE_SERIALIZER = "it.acsoftware.hyperiot.hproject.serialization.service.KafkaAvroHPacketSerializer";
    public static final String KAFKA_DLQ_VALUE_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    public static final String KAFKA_SPOUT_ID = "kafka-spout-raw-";
    public static final String MESSAGE_FIELD = "message";
    public static final String MESSAGE_TYPE_FIELD = "message_type";
    public static final String MONTH_STREAM_ID = "month";
    public static final String PACKET_FIELD = "packet";
    public static final String QUARTER_STREAM_ID = "quarter";
    public static final String REALTIME_LAYER_KAFKA_BOLT = "bolt-kafka-realtime-";
    public static final String RECEIVED_PACKET_FIELD = "received_packet";
    public static final String ROWKEY_FIELD = "rowkey";
    public static final String SELECTION_BOLT_ID = "bolt-selection";
    public static final String SEMESTER_STREAM_ID = "semester";
    public static final String STEP_FIELD = "step";
    public static final String TIMELINE_HPROJECT_STREAM_ID_PREFIX = "timeline_hproject_%s";
    public static final String EVENT_RULE_STATE_HBASE_STREAM_ID = "eventRuleStateToHBase_%s";

    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String YEAR_STREAM_ID = "year";
    public static final String KAFKA_DLQ_SPOUT_TOPIC_NAME="kafka.spout.dql.topic.";
    public static final String KAFKA_DLQ_SPOUT_ID="kafka-spout-dql-id_%s";
    public static final String KAFKA_DLQ_BOLT_ID="kafka-bolt-dql_%s";
    public static final String KAKFA_DLQ_BOLT_DESERIALIZATION_ID="kafka-dlq-bolt-deserialization-id_%s";
    public static final String KAKFA_DLQ_STREAM_HDFS_HOUR="dlq-bolt-stream-hdfs-hour";
    public static final String KAKFA_DLQ_STREAM_HDFS_DAY="dlq-bolt-stream-hdfs-day";
    public static final String KAKFA_DLQ_STREAM_HDFS_MONTH="dlq-bolt-stream-hdfs-month";
    public static final String KAKFA_DLQ_STREAM_HDFS_YEAR="dlq-bolt-stream-hdfs-year";
    public static final String KAKFA_DLQ_STREAM_HDFS_SEMESTER="dlq-bolt-stream-hdfs-semester";
    public static final String KAKFA_DLQ_STREAM_HDFS_QUARTER="dlq-bolt-stream-hdfs-quarter";
    public static final String KAKFA_DLQ_STREAM_HBASE_AVRO="dlq-bolt-stream-hbase-avro";
    public static final String KAKFA_DLQ_STREAM_HBASE_EVENT="dlq-bolt-stream-hbase-event";
    public static final String KAKFA_DLQ_STREAM_HBASE_TIMELINE="dlq-bolt-stream-hbase-timeline";
    public static final String KAKFA_DLQ_STREAM_HBASE_ALARM="dlq-bolt-stream-hbase-alarm";
    public static final String KAKFA_DLQ_STREAM_HBASE_EVENT_RULE_STATE="dlq-bolt-stream-hbase-event-rule-state";
    public static final String KAKFA_DLQ_STREAM_HBASE_ERROR="dlq-bolt-stream-hbase-error";



    //DLQ DESERIALIZATION STREAM.
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_HOUR="dlq-bolt-stream-hdfs-hour-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_DAY="dlq-bolt-stream-hdfs-day-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_MONTH="dlq-bolt-stream-hdfs-month-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_YEAR="dlq-bolt-stream-hdfs-year-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_SEMESTER="dlq-bolt-stream-hdfs-semester-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_QUARTER="dlq-bolt-stream-hdfs-quarter-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_AVRO="dlq-bolt-stream-hbase-avro-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT="dlq-bolt-stream-hbase-event-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_TIMELINE="dlq-bolt-stream-hbase-timeline-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ALARM="dlq-bolt-stream-hbase-alarm-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT_RULE_STATE="dlq-bolt-stream-hbase-event-rule-state-deserialization";
    public static final String KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ERROR="dlq-bolt-stream-hbase-error-deserialization";

    //Nella mappa che invierò come messaggio questo valore mi permette di capire a chi dovrò inviare la tupla.
    public static final String KAFKA_DLQ_PACKET_SENDER_LABEL="KAFKA_DLQ_PACKET_SENDER_LABEL";

    public static final String BOLT_HPACKET_DESERIALIZATION_FIELD_LABEL="BOLT_HPACKET_DESERIALIZATION_FIELD_LABEL";

    public static final String ERROR_PACKET_FIELD_RETRANSMIT="packet-to-retransmit";


}

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

package it.acsoftware.hyperiot.storm.builder;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.storm.api.StormTopologyBuilder;
import it.acsoftware.hyperiot.storm.hbase.mapper.AlarmHBaseMapper;
import it.acsoftware.hyperiot.storm.hbase.mapper.AvroHBaseMapper;
import it.acsoftware.hyperiot.storm.hbase.mapper.EventRuleStateHBaseMapper;
import it.acsoftware.hyperiot.storm.hbase.mapper.TimelineHBaseMapper;
import it.acsoftware.hyperiot.storm.hdfs.partitioner.HyperiotPartitioner;
import it.acsoftware.hyperiot.storm.runtime.bolt.*;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import it.acsoftware.hyperiot.stormmanager.model.MessageConversionStrategy;
import org.apache.storm.Config;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.hbase.bolt.HyperIoTHBaseBolt;
import org.apache.storm.hbase.bolt.mapper.SimpleHBaseMapper;
import org.apache.storm.hdfs.bolt.AvroGenericRecordBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.common.Partitioner;
import org.apache.storm.kafka.bolt.KafkaBolt;
import org.apache.storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import org.apache.storm.kafka.bolt.selector.DefaultTopicSelector;
import org.apache.storm.kafka.spout.KafkaSpout;
import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.kafka.spout.KafkaSpoutRetryExponentialBackoff;
import org.apache.storm.kafka.spout.KafkaSpoutRetryService;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.osgi.service.component.annotations.Component;

import java.util.Map;
import java.util.Properties;

import static it.acsoftware.hyperiot.storm.util.StormConstants.*;

/**
 * @Author Aristide Cittadino
 */

@Component(service = StormTopologyBuilder.class, immediate = true)
public class HyperIoTTopologyBuilder implements StormTopologyBuilder {


    @Override
    public StormTopology configureTopology(Config config) {
        return buildTopology(config);
    }

    /**
     * @param props
     * @return
     */
    public static StormTopology buildTopology(Map<String, Object> props) {
        TopologyBuilder builder = new TopologyBuilder();
        String projectIdStr = (String) props.get("project.id");
        long projectId = Long.parseLong(projectIdStr);
        HProjectSystemApi hProjectSystemApi = (HProjectSystemApi) HyperIoTUtil.getService(HProjectSystemApi.class);
        HProject project = hProjectSystemApi.find(projectId, null);
        String kafkaSpoutId = StormConstants.KAFKA_SPOUT_ID + project.getId();
        //Adding spout layer with Kafka
        buildSpoutLayer(props, project, builder, kafkaSpoutId);
        //Realtime speed layer with deserialization,enrichment,event and selection bolts
        buildRealtimeBoltsLayer(props, project, builder, projectIdStr, kafkaSpoutId);
        //Speed layer with HBase
        buildSpeedAndServingBoltsLayer(props, project, builder, projectIdStr);
        //HDFS persistence
        buildBatchBoltsLayer(props, project, builder);
        //Bolt layer that write on dlq when there is an error
        buildDLQBoltsLayer(props, project, builder, projectIdStr, kafkaSpoutId);

        buildErrorBoltsLayer(props, builder, projectIdStr);
        StormTopology topology = builder.createTopology();
        return topology;
    }

    /**
     * @param props
     * @param project
     * @param builder
     */
    private static void buildSpoutLayer(Map<String, Object> props, HProject project, TopologyBuilder builder, String kafkaSpoutId) {
        int kafkaSpoutParallelism = getKafkaSpoutParallelism(project);
        KafkaSpout<byte[], byte[]> kafkaSpout = createKafkaSpout(props, project);
        builder.setSpout(kafkaSpoutId, kafkaSpout, kafkaSpoutParallelism);

        String kafkaDlqSpoutId = String.format(StormConstants.KAFKA_DLQ_SPOUT_ID, project.getId());
        KafkaSpout<byte[], byte[]> kafkaDLQSpout = createKafkaDLQSpout(props, project, KAFKA_DLQ_SPOUT_TOPIC_NAME);
        builder.setSpout(kafkaDlqSpoutId, kafkaDLQSpout, kafkaSpoutParallelism);

    }

    /**
     * @param props
     * @param project
     * @param builder
     * @param projectIdStr
     * @param kafkaSpoutId
     */
    private static void buildRealtimeBoltsLayer(Map<String, Object> props, HProject project, TopologyBuilder builder, String projectIdStr, String kafkaSpoutId) {
        //Deserialization Bolt
        DeserializationBolt deserializationBolt = new DeserializationBolt().withHProject(String.valueOf(project.getId()));
        int deserilizationBoltParallelism = getDeserializationBoltParallelism(project);
        //Enrichment Bolt
        EnrichmentBolt enrichmentBolt = new EnrichmentBolt().withHProject(String.valueOf(project.getId()));
        int enrichmentBoltParallelism = getEnrichmentBoltParallelism(project);
        //Kafka Bolt
        KafkaBolt kafkaRealtimeBolt = createKafkaBolt(props, project, StormConstants.KAFKA_REALTIME_KEY_SERIALIZER, StormConstants.KAFKA_REALTIME_VALUE_SERIALIZER, "streaming.realtime." + projectIdStr, "deviceId.packetId", "packet");
        int kafkaBoltParallelism = getKafkaRealtimeBoltParallelism(project);
        //Event Bolt
        EventBolt eventBolt = new EventBolt().withHProject(String.valueOf(project.getId()));
        //Kafka EventBolt
        KafkaBolt kafkaEventBolt = createKafkaBolt(props, project, StormConstants.KAFKA_EVENT_KEY_SERIALIZER, StormConstants.KAFKA_EVENT_VALUE_SERIALIZER, (String) props.get("bolt.event.kafka_topic"), "message_type", "event_json");
        //Persistence Bolt
        SelectionBolt selectionBolt = new SelectionBolt().withHProject(String.valueOf(project.getId()));
        builder.setBolt(StormConstants.DESERIALIZATION_BOLT_ID, deserializationBolt, deserilizationBoltParallelism).shuffleGrouping(kafkaSpoutId);
        builder.setBolt(StormConstants.ENRICHMENT_BOLT_ID, enrichmentBolt, enrichmentBoltParallelism).shuffleGrouping(StormConstants.DESERIALIZATION_BOLT_ID, "deserializationOk");
        builder.setBolt(StormConstants.EVENT_BOLT_ID, eventBolt, getEventBoltParallelism(project)).shuffleGrouping(StormConstants.DESERIALIZATION_BOLT_ID, "deserializationOk");
        builder.setBolt(StormConstants.EVENT_LAYER_KAFKA_BOLT, kafkaEventBolt, getEventToKafkaBoltParallelism(project)).shuffleGrouping(StormConstants.EVENT_BOLT_ID);
        builder.setBolt(StormConstants.SELECTION_BOLT_ID, selectionBolt, getSelectionBoltParallelism(project))
                .shuffleGrouping(StormConstants.EVENT_BOLT_ID, String.format(EVENT_HPROJECT_STREAM_ID_PREFIX, project.getId()))
                .shuffleGrouping(StormConstants.EVENT_BOLT_ID, String.format(StormConstants.ALARM_EVENT_HPROJECT_STREAM_ID_PREFIX, project.getId()))
                .shuffleGrouping(StormConstants.ENRICHMENT_BOLT_ID);
        builder.setBolt(StormConstants.REALTIME_LAYER_KAFKA_BOLT + projectIdStr, kafkaRealtimeBolt, kafkaBoltParallelism)
                .shuffleGrouping(StormConstants.ENRICHMENT_BOLT_ID)
                .shuffleGrouping(StormConstants.DESERIALIZATION_BOLT_ID, "systemTick")
                .shuffleGrouping(StormConstants.EVENT_BOLT_ID, "kafkaRealtimeTopicEvent");
    }

    private static void buildDLQBoltsLayer(Map<String, Object> props, HProject project, TopologyBuilder builder, String projectIdStr, String kafkaSpoutId) {
        int kafkaDlqBoltParallelism = getKafkaDqlBoltParallelism(project);
        KafkaBolt kafkaDlqBolt = createKafkaBolt(props,
                project,
                StormConstants.KAFKA_EVENT_KEY_SERIALIZER,
                KAFKA_DLQ_VALUE_SERIALIZER,
                KAFKA_DLQ_SPOUT_TOPIC_NAME + projectIdStr,
                StormConstants.TIMESTAMP_FIELD,
                StormConstants.ERROR_PACKET_FIELD_RETRANSMIT
        );

        builder.setBolt(String.format(KAFKA_DLQ_BOLT_ID, projectIdStr), kafkaDlqBolt, kafkaDlqBoltParallelism).
                shuffleGrouping(String.format(HDFS_PERSISTENCE_HOUR, projectIdStr), KAKFA_DLQ_STREAM_HDFS_HOUR).
                shuffleGrouping(String.format(HDFS_PERSISTENCE_DAY, projectIdStr), KAKFA_DLQ_STREAM_HDFS_DAY).
                shuffleGrouping(String.format(HDFS_PERSISTENCE_MONTH, projectIdStr), KAKFA_DLQ_STREAM_HDFS_MONTH).
                shuffleGrouping(String.format(HDFS_PERSISTENCE_YEAR, projectIdStr), KAKFA_DLQ_STREAM_HDFS_YEAR).
                shuffleGrouping(String.format(HDFS_PERSISTENCE_SEMESTER, projectIdStr), KAKFA_DLQ_STREAM_HDFS_SEMESTER).
                shuffleGrouping(String.format(HDFS_PERSISTENCE_QUARTER, projectIdStr), KAKFA_DLQ_STREAM_HDFS_QUARTER).
                shuffleGrouping(HBASE_AVRO_BOLT_PREFIX + project.getId(), KAKFA_DLQ_STREAM_HBASE_AVRO).
                shuffleGrouping(HBASE_EVENT_BOLT_PREFIX + project.getId(), KAKFA_DLQ_STREAM_HBASE_EVENT).
                shuffleGrouping(HBASE_TIMELINE_BOLT_PREFIX + project.getId(), KAKFA_DLQ_STREAM_HBASE_TIMELINE).
                shuffleGrouping(HBASE_ALARM_BOLT_PREFIX + project.getId(), KAKFA_DLQ_STREAM_HBASE_ALARM).
                shuffleGrouping(HBASE_EVENT_RULE_STATE_BOLT_PREFIX + project.getId(), KAKFA_DLQ_STREAM_HBASE_EVENT_RULE_STATE).
                shuffleGrouping(HBASE_ERROR_BOLT_PREFIX + project.getId(), KAKFA_DLQ_STREAM_HBASE_ERROR);


        //DlqBolt use to read and deserialize data that arrive on kafka dlq when hbase/hdfs go down.
        HyperIoTDlqBolt deserializationDlqBolt = new HyperIoTDlqBolt();
        builder.setBolt(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr), deserializationDlqBolt, kafkaDlqBoltParallelism).
                shuffleGrouping(String.format(KAFKA_DLQ_SPOUT_ID, projectIdStr));
    }

    /**
     * @param project
     * @param builder
     * @param projectIdStr
     */
    private static void buildSpeedAndServingBoltsLayer(Map<String, Object> props, HProject project, TopologyBuilder builder, String projectIdStr) {
        final int HBASE_BATCH_SIZE = (Integer) props.get("it.acsoftware.hyperiot.storm.hbase.client.batch.size");
        final int HBASE_FLUSH_INTERVAL_SECONDS = (Integer) props.get("it.acsoftware.hyperiot.storm.hbase.client.flush.interval.seconds");

        HyperIoTWrapperHBaseBolt avroTableBolt = new HyperIoTWrapperHBaseBolt(
                new HyperIoTHBaseBolt("hproject_" + projectIdStr, new AvroHBaseMapper()
                        .withColumnFamily(StormConstants.HBASE_COLUMN_FAMILY_HPACKET)
                        .withAttachmentsColumnFamily(StormConstants.HBASE_COLUMN_FAMILY_HPACKET_ATTACHMENTS)
                        .withRowKeyField(StormConstants.HBASE_ROWKEY))
                        .withConfigKey(StormConstants.HBASE_BOLT_CONF)
                        .withBatchSize(HBASE_BATCH_SIZE)
                        .withFlushIntervalSecs(HBASE_FLUSH_INTERVAL_SECONDS),
                MessageConversionStrategy.HBASE_DLQ_AVRO_TABLE_MESSAGE_STRATEGY,
                String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr)
        );

        HyperIoTWrapperHBaseBolt timelineTableBolt = new HyperIoTWrapperHBaseBolt(
                new HyperIoTHBaseBolt(String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, projectIdStr),
                        new TimelineHBaseMapper().
                                withRowKeyField(StormConstants.HBASE_ROWKEY))
                        .withConfigKey(StormConstants.HBASE_BOLT_CONF)
                        .withBatchSize(HBASE_BATCH_SIZE)
                        .withFlushIntervalSecs(HBASE_FLUSH_INTERVAL_SECONDS),
                MessageConversionStrategy.HBASE_DLQ_TIMELINE_TABLE_MESSAGE_STRATEGY,
                String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr)
        );

        HyperIoTWrapperHBaseBolt alarmTableBolt = new HyperIoTWrapperHBaseBolt(
                new HyperIoTHBaseBolt("alarm_" + projectIdStr,
                        new AlarmHBaseMapper()
                                .withColumnFamilies()
                                .withRowKeyField(StormConstants.HBASE_ROWKEY))
                        .withConfigKey(StormConstants.HBASE_BOLT_CONF)
                        .withBatchSize(1)
                        .withFlushIntervalSecs(HBASE_FLUSH_INTERVAL_SECONDS),
                MessageConversionStrategy.HBASE_DLQ_ALARM_TABLE_MESSAGE_STRATEGY,
                String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr)
        );

        HyperIoTWrapperHBaseBolt alarmEventRuleStateTableBolt = new HyperIoTWrapperHBaseBolt(
                new HyperIoTHBaseBolt(String.format(EVENT_RULE_STATE_TABLE_NAME_PREFIX, projectIdStr),
                        new EventRuleStateHBaseMapper()
                                .withColumnFamily(HBASE_COLUMN_FAMILY_RULE)
                                .withRowKeyField(EVENT_RULE_ID))
                        .withConfigKey(HBASE_BOLT_CONF)
                        .withBatchSize(1)
                        .withFlushIntervalSecs(HBASE_FLUSH_INTERVAL_SECONDS),
                MessageConversionStrategy.HBASE_DLQ_EVENT_RULE_STATE_MESSAGE_STRATEGY,
                String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr));

        HyperIoTWrapperHBaseBolt eventTableBolt = new HyperIoTWrapperHBaseBolt(
                new HyperIoTHBaseBolt("event_" + projectIdStr, new SimpleHBaseMapper()
                        .withColumnFamily("event")
                        .withRowKeyField("timestamp")
                        .withColumnFields(new Fields("eventColumn")))
                        .withConfigKey(StormConstants.HBASE_BOLT_CONF)
                        .withBatchSize(1)
                        .withFlushIntervalSecs(HBASE_FLUSH_INTERVAL_SECONDS),
                MessageConversionStrategy.HBASE_DLQ_EVENT_TABLE_MESSAGE_STRATEGY,
                String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr)
        );
        //Add CustomHBaseBolt to the topology.
        builder.setBolt(StormConstants.HBASE_AVRO_BOLT_PREFIX + projectIdStr, avroTableBolt, getAvroBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "hproject_" + projectIdStr).
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr), StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_AVRO);

        builder.setBolt(StormConstants.HBASE_TIMELINE_BOLT_PREFIX + projectIdStr, timelineTableBolt, getTimelineBoltParallelism(project))
                .shuffleGrouping(StormConstants.DESERIALIZATION_BOLT_ID, String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, projectIdStr))
                .shuffleGrouping(StormConstants.SELECTION_BOLT_ID, String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, projectIdStr))
                .shuffleGrouping(StormConstants.ENRICHMENT_BOLT_ID, String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, projectIdStr))
                .shuffleGrouping(StormConstants.EVENT_BOLT_ID, String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, projectIdStr))
                .shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr), StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_TIMELINE);

        builder.setBolt(StormConstants.HBASE_EVENT_BOLT_PREFIX + projectIdStr, eventTableBolt, getEventSourcingBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, String.format(EVENT_HPROJECT_STREAM_ID_PREFIX, projectIdStr)).
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr), StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT);

        builder.setBolt(StormConstants.HBASE_ALARM_BOLT_PREFIX + projectIdStr, alarmTableBolt, getAlarmBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, String.format(StormConstants.ALARM_EVENT_HPROJECT_STREAM_ID_PREFIX, projectIdStr)).
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr), KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ALARM);

        builder.setBolt(HBASE_EVENT_RULE_STATE_BOLT_PREFIX + projectIdStr, alarmEventRuleStateTableBolt, getAlarmEventRuleStateBoltParallelism(project))
                .shuffleGrouping(StormConstants.EVENT_BOLT_ID, String.format(EVENT_RULE_STATE_HBASE_STREAM_ID, projectIdStr))
                .shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr), KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT_RULE_STATE);


    }

    /**
     * @param props
     * @param project
     * @param builder
     */
    private static void buildBatchBoltsLayer(Map<String, Object> props, HProject project, TopologyBuilder builder) {
        //Define CustomAvroGenericBolt for write on hdfs, and handle hdfs failure.

        HyperIoTWrapperAvroGenericBolt customPerHourPersistence = new HyperIoTWrapperAvroGenericBolt(
                createBatchLayerPersistenceBolt(props, project, new HyperiotPartitioner().withDepth("hour")),
                KAKFA_DLQ_STREAM_HDFS_HOUR,
                MessageConversionStrategy.HDFS_DLQ_HOUR_MESSAGE_STRATEGY,
                String.format(StormConstants.KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()));

        HyperIoTWrapperAvroGenericBolt customPerDayPersistence = new HyperIoTWrapperAvroGenericBolt(
                createBatchLayerPersistenceBolt(props, project, new HyperiotPartitioner().withDepth("day")),
                KAKFA_DLQ_STREAM_HDFS_DAY,
                MessageConversionStrategy.HDFS_DLQ_DAY_MESSAGE_STRATEGY,
                String.format(StormConstants.KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()));

        HyperIoTWrapperAvroGenericBolt customPerMonthPersistence = new HyperIoTWrapperAvroGenericBolt(
                createBatchLayerPersistenceBolt(props, project, new HyperiotPartitioner().withDepth("month")),
                KAKFA_DLQ_STREAM_HDFS_MONTH,
                MessageConversionStrategy.HDFS_DLQ_MONTH_MESSAGE_STRATEGY,
                String.format(StormConstants.KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()));

        HyperIoTWrapperAvroGenericBolt customPerYearPersistence = new HyperIoTWrapperAvroGenericBolt(
                createBatchLayerPersistenceBolt(props, project, new HyperiotPartitioner().withDepth("year")),
                KAKFA_DLQ_STREAM_HDFS_YEAR,
                MessageConversionStrategy.HDFS_DLQ_YEAR_MESSAGE_STRATEGY,
                String.format(StormConstants.KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()));

        HyperIoTWrapperAvroGenericBolt customPerQuarterPersistence = new HyperIoTWrapperAvroGenericBolt(
                createBatchLayerPersistenceBolt(props, project, new HyperiotPartitioner().withDepth("quarter")),
                KAKFA_DLQ_STREAM_HDFS_QUARTER,
                MessageConversionStrategy.HDFS_DLQ_QUARTER_MESSAGE_STRATEGY,
                String.format(StormConstants.KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()));

        HyperIoTWrapperAvroGenericBolt customPerSemesterPersistence = new HyperIoTWrapperAvroGenericBolt(
                createBatchLayerPersistenceBolt(props, project, new HyperiotPartitioner().withDepth("semester")),
                KAKFA_DLQ_STREAM_HDFS_SEMESTER,
                MessageConversionStrategy.HDFS_DLQ_SEMESTER_MESSAGE_STRATEGY,
                String.format(StormConstants.KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()));

        //Add hdfs bolt  to topology
        builder.setBolt(String.format(StormConstants.HDFS_PERSISTENCE_HOUR, project.getId()), customPerHourPersistence, getHDFSHourBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "hour").
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()), KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_HOUR);

        builder.setBolt(String.format(StormConstants.HDFS_PERSISTENCE_DAY, project.getId()), customPerDayPersistence, getHDFSDayBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "day").
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()), KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_DAY);

        builder.setBolt(String.format(StormConstants.HDFS_PERSISTENCE_MONTH, project.getId()), customPerMonthPersistence, getHDFSMonthBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "month").
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()), KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_MONTH);

        builder.setBolt(String.format(StormConstants.HDFS_PERSISTENCE_YEAR, project.getId()), customPerYearPersistence, getHDFSYearBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "year").
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()), KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_YEAR);

        builder.setBolt(String.format(StormConstants.HDFS_PERSISTENCE_SEMESTER, project.getId()), customPerSemesterPersistence, getHDFSSemesterBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "semester").
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()), KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_SEMESTER);

        builder.setBolt(String.format(StormConstants.HDFS_PERSISTENCE_QUARTER, project.getId()), customPerQuarterPersistence, getHDFSQuarterBoltParallelism(project)).
                shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "quarter").
                shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, project.getId()), KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_QUARTER);

    }


    /**
     * @param builder
     * @param projectIdStr
     */
    private static void buildErrorBoltsLayer(Map<String, Object> props, TopologyBuilder builder, String projectIdStr) {

        final int HBASE_BATCH_SIZE = (Integer) (props.get("it.acsoftware.hyperiot.storm.hbase.client.batch.size"));
        final int HBASE_FLUSH_INTERVAL_SECONDS = (Integer) (props.get("it.acsoftware.hyperiot.storm.hbase.client.flush.interval.seconds"));

        HyperIoTWrapperHBaseBolt errorTableBolt = new HyperIoTWrapperHBaseBolt(
                new HyperIoTHBaseBolt("hproject_error_" + projectIdStr,
                        new SimpleHBaseMapper().
                                withColumnFamily("error").
                                withRowKeyField("timestamp").
                                withColumnFields(new Fields("message", "received_packet"))).
                        withConfigKey(StormConstants.HBASE_BOLT_CONF).
                        withBatchSize(HBASE_BATCH_SIZE).
                        withFlushIntervalSecs(HBASE_FLUSH_INTERVAL_SECONDS),
                MessageConversionStrategy.HBASE_DLQ_ERROR_TABLE_MESSAGE_STRATEGY,
                String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr)
        );

        builder.setBolt(StormConstants.HBASE_ERROR_BOLT_PREFIX + projectIdStr, errorTableBolt, 1)
                .shuffleGrouping(StormConstants.DESERIALIZATION_BOLT_ID, "hproject_error_" + projectIdStr)
                .shuffleGrouping(StormConstants.SELECTION_BOLT_ID, "hdfs_error_" + projectIdStr)
                .shuffleGrouping(StormConstants.ENRICHMENT_BOLT_ID, String.format(HPROJECT_ERROR_STREAM_ID, projectIdStr))
                .shuffleGrouping(StormConstants.EVENT_BOLT_ID, String.format(HPROJECT_ERROR_STREAM_ID, projectIdStr))
                .shuffleGrouping(String.format(KAKFA_DLQ_BOLT_DESERIALIZATION_ID, projectIdStr), StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ERROR);

    }

    /**
     * Returns Kafka Spout Base on HProject Configuration
     *
     * @param props
     * @param project
     * @return
     */
    private static KafkaSpout<byte[], byte[]> createKafkaSpout(Map<String, Object> props, HProject project) {
        String bootstrapServers = (String) props.get(StormConstants.KAFKA_BOOTSTRAP_SERVERS);
        StringBuilder topics = new StringBuilder();
        topics.append("streaming.").append(project.getId());
        KafkaSpoutConfig.Builder config = new KafkaSpoutConfig.Builder(bootstrapServers, topics.toString());
        config.setProp("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        config.setProp("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        config.setProp("group.id", "group-" + project.getId());
        KafkaSpout<byte[], byte[]> kafkaSpout = new KafkaSpout<>(config.build());
        return kafkaSpout;
    }

    private static KafkaSpout<byte[], byte[]> createKafkaDLQSpout(Map<String, Object> props, HProject project, String topicPrefix) {
        String bootstrapServers = (String) props.get(StormConstants.KAFKA_BOOTSTRAP_SERVERS);
        StringBuilder topics = new StringBuilder();
        topics.append(topicPrefix).append(project.getId());
        KafkaSpoutConfig.Builder config = new KafkaSpoutConfig.Builder(bootstrapServers, topics.toString());
        config.setProp("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        config.setProp("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        config.setProp("group.id", "group-" + project.getId());
        //Add a retry back off logic to the spout
        config.setRetry(createKafkaDlqRetryService(props));
        KafkaSpout<byte[], byte[]> kafkaSpout = new KafkaSpout<>(config.build());
        return kafkaSpout;
    }

    private static KafkaSpoutRetryService createKafkaDlqRetryService(Map<String, Object> props) {

        final long initialDelayKafkaSpoutHDFSError = (Integer) props.get("it.acsoftware.hyperiot.storm.initialDelayKafkaSpoutDLQ.seconds");
        final long progressiveDelayFactorKafkaSpoutHDFSError = (Integer) props.get("it.acsoftware.hyperiot.storm.progressiveDelayFactorKafkaSpoutDLQ.seconds");
        final int maxRetryKafkaSpoutHDFSError = (Integer) props.get("it.acsoftware.hyperiot.storm.maxRetryKafkaSpoutDLQ");
        final long maxDelayKafkaSpoutHDFSError = (Integer) props.get("it.acsoftware.hyperiot.storm.maxDelayKafkaSpoutDLQ.seconds");

        //todo Change this settings to seconds , when go in production.
        return new KafkaSpoutRetryExponentialBackoff(
                KafkaSpoutRetryExponentialBackoff.TimeInterval.seconds(initialDelayKafkaSpoutHDFSError),
                KafkaSpoutRetryExponentialBackoff.TimeInterval.seconds(progressiveDelayFactorKafkaSpoutHDFSError),
                maxRetryKafkaSpoutHDFSError,
                KafkaSpoutRetryExponentialBackoff.TimeInterval.seconds(maxDelayKafkaSpoutHDFSError)
        );

    }

    /**
     * @return
     */
    private static KafkaBolt<byte[], byte[]> createKafkaBolt(Map<String, Object> props, HProject project, String keySerializer, String valueSerializer, String topicSelectorString, String boltKeyField, String boltMessageField) {
        KafkaBolt<byte[], byte[]> kafkaBolt = new KafkaBolt<>();
        Properties kafkaBoltProps = new Properties();
        DefaultTopicSelector topicSelector = new DefaultTopicSelector(topicSelectorString);
        FieldNameBasedTupleToKafkaMapper fieldNameBasedTupleToKafkaMapper = new FieldNameBasedTupleToKafkaMapper<>(boltKeyField, boltMessageField);
        kafkaBoltProps.put("bootstrap.servers", props.get(StormConstants.KAFKA_BOOTSTRAP_SERVERS));
        kafkaBoltProps.put("acks", "1");
        kafkaBoltProps.put("key.serializer", keySerializer);
        kafkaBoltProps.put("value.serializer", valueSerializer);
        kafkaBolt.withProducerProperties(kafkaBoltProps);
        kafkaBolt.withTopicSelector(topicSelector);
        kafkaBolt.withTupleToKafkaMapper(fieldNameBasedTupleToKafkaMapper);
        return kafkaBolt;
    }

    /**
     * @param props
     * @param project
     * @param partitioner
     * @return
     */
    private static AvroGenericRecordBolt createBatchLayerPersistenceBolt(Map<String, Object> props, HProject project, Partitioner partitioner) {
        AvroGenericRecordBolt avroGenericRecordBolt = new AvroGenericRecordBolt();
        DefaultFileNameFormat defaultFileNameFormat = new DefaultFileNameFormat().withPath((String) props.get("hdfs.write.dir")).withExtension(".avro");
        FileSizeRotationPolicy fileSizeRotationPolicy = new FileSizeRotationPolicy(1, FileSizeRotationPolicy.Units.GB);
        CountSyncPolicy countSyncPolicy = new CountSyncPolicy(10);
        //Omit this configuration to override default.
        //avroGenericRecordBolt.withConfigKey("hdfs.config-1");
        avroGenericRecordBolt.withFileNameFormat(defaultFileNameFormat);
        avroGenericRecordBolt.withFsUrl((String) props.get("hdfs.url"));
        avroGenericRecordBolt.withRotationPolicy(fileSizeRotationPolicy);
        avroGenericRecordBolt.withSyncPolicy(countSyncPolicy);
        avroGenericRecordBolt.withPartitioner(partitioner);
        return avroGenericRecordBolt;
    }

    /**
     * Returns parallelism of kafka spout based on project configuration
     *
     * @param project
     * @return
     */
    private static int getKafkaSpoutParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getKafkaSpoutParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getDeserializationBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getDeserializationParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getEnrichmentBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getEnrichmentParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getEventBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getEventsProcessingParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getEventToKafkaBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getEventsToKafkaParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getEventSourcingBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getEventSourcingParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getKafkaRealtimeBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getKafkaRealtimeParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getAlarmBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getAlarmManagementParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getAlarmCountBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getAlarmCountParallelism();
    }

    private static int getAlarmEventRuleStateBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getAlarmEventRuleParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getSelectionBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getSelectionBoltParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getTimelineBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getTimelineParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getAvroBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getAvroProcessingParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getHDFSHourBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getHdfsHourParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getHDFSDayBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getHdfsDayParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getHDFSMonthBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getHdfsMonthParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getHDFSYearBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getHdfsYearParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getHDFSSemesterBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getHdfsSemesterParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getHDFSQuarterBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getHdfsQuarterParallelism();
    }

    /**
     * @param project
     * @return
     */
    private static int getKafkaDqlBoltParallelism(HProject project) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(project).getDlqParallelism();
    }

}

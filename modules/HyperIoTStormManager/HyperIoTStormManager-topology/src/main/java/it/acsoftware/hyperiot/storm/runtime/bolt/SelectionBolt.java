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

package it.acsoftware.hyperiot.storm.runtime.bolt;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.hpacket.model.HPacketTrafficPlan;
import it.acsoftware.hyperiot.hproject.api.hbase.timeline.HProjectTimelineUtil;
import it.acsoftware.hyperiot.hproject.model.ModelType;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectTimelineUtilImpl;
import it.acsoftware.hyperiot.storm.runtime.bolt.util.BoltConstants;
import it.acsoftware.hyperiot.storm.runtime.bolt.util.BoltUtil;
import it.acsoftware.hyperiot.storm.runtime.bolt.util.TimelineUtil;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static it.acsoftware.hyperiot.storm.util.StormConstants.*;

/**
 * This bolt supports HDFS and HBase write. It takes HPacket in AVRO format and sends it to HDFS and HBase bolts
 */
@SuppressWarnings("unused")
public class SelectionBolt extends BaseRichBolt {

    private static final Logger log =
            LoggerFactory.getILoggerFactory().getLogger(SelectionBolt.class.getName());
    @SuppressWarnings({"FieldCanBeLocal", "rawtypes"})
    private Map config;
    private long hprojectId;
    private ObjectMapper objectMapper;
    private OutputCollector collector;
    private HProjectTimelineUtil hProjectTimelineUtil;

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(YEAR_STREAM_ID, new Fields(StormConstants.PACKET_FIELD));
        declarer.declareStream(MONTH_STREAM_ID, new Fields(StormConstants.PACKET_FIELD));
        declarer.declareStream(DAY_STREAM_ID, new Fields(StormConstants.PACKET_FIELD));
        declarer.declareStream(HOUR_STREAM_ID, new Fields(StormConstants.PACKET_FIELD));
        declarer.declareStream(QUARTER_STREAM_ID, new Fields(StormConstants.PACKET_FIELD));
        declarer.declareStream(SEMESTER_STREAM_ID, new Fields(StormConstants.PACKET_FIELD));
        declarer.declareStream(String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, hprojectId), new Fields(StormConstants.ROWKEY_FIELD, StormConstants.TIMESTAMP_FIELD, StormConstants.STEP_FIELD));
        declarer.declareStream(String.format(AVRO_HPROJECT_STREAM_ID_PREFIX, hprojectId), new Fields(StormConstants.ROWKEY_FIELD, StormConstants.HPACKET_ID_FIELD, StormConstants.AVRO_HPACKET_FIELD, StormConstants.AVRO_HPACKET_ATTACHMENTS));
        declarer.declareStream(String.format(HDFS_ERROR_STREAM_ID_PREFIX, hprojectId), new Fields(StormConstants.TIMESTAMP_FIELD, StormConstants.MESSAGE_FIELD, StormConstants.RECEIVED_PACKET_FIELD));
        declarer.declareStream(String.format(EVENT_HPROJECT_STREAM_ID_PREFIX, hprojectId), new Fields(StormConstants.TIMESTAMP_FIELD, StormConstants.EVENT_COLUMN_FIELD));
        declarer.declareStream(String.format(ALARM_EVENT_HPROJECT_STREAM_ID_PREFIX, hprojectId), new Fields(StormConstants.ROWKEY_FIELD, StormConstants.HDEVICE_ID_FIELD, StormConstants.ALARM_STATE_FIELD, StormConstants.EVENT_COLUMN_FIELD));
    }

    @Override
    public void execute(Tuple input) {
        // Pay attention! Tuples are not anchored. In case of error, they are not retransmitted
        log.debug("Tuple received, deserialize it to HPacket object instance");
        HPacket packet = (HPacket) input.getValueByField(StormConstants.PACKET_FIELD);
        ModelType modelType;
        long timestamp;
        String avroHPacket = null;
        String rowKeyBeginning;
        try {
            if (packet.getId() == 0 && packet.getName().endsWith(BoltConstants.EVENT_PACKET_SUFFIX)) {
                modelType = ModelType.EVENT;
                rowKeyBeginning = modelType.getSimpleName();
            } else if (packet.getId() == 0 && packet.getName().endsWith(BoltConstants.ALARM_EVENT_PACKET_SUFFIX)) {
                modelType = ModelType.ALARMEVENT;
                log.debug("In Selection Bolt before deserialize deviceId related to alarm event");
                long deviceId = ((Long) (input.getValueByField(StormConstants.ALARM_EVENT_DEVICE_ID)));
                log.debug("In Selection Bolt after deviceId related to alarm event  {}", deviceId);
                rowKeyBeginning = String.join(".", "Alarm", String.valueOf(deviceId));
            } else {
                log.debug("Send HPacket to HDFS");
                sendToHDFS(packet, packet.getTrafficPlan());
                modelType = ModelType.HPACKET;
                rowKeyBeginning = String.join(".", modelType.getSimpleName(), String.valueOf(packet.getId()));
            }
            timestamp = (long) packet.getFieldValue(packet.getTimestampField());
            log.debug("Send HPacket to HBase");
            Map<Long, byte[]> attachments = stripAttachments(packet);
            //now packet has no attachments since are stripped and it can be converted to avro
            avroHPacket = BoltUtil.getAvroHPacket(packet);
            sendToHBase(avroHPacket, attachments, packet.getId(), modelType, timestamp, rowKeyBeginning, input);
        } catch (Throwable t) {
            handleError(t, avroHPacket);
        }
    }

    /**
     * This method strips FILE type fields from hpacket.
     * With this approach hbase will save the basic content to the standard column family
     * and attachments into a separated column family.
     *
     * @param packet
     * @return
     */
    private Map<Long, byte[]> stripAttachments(HPacket packet) {
        Map<Long, byte[]> attachmentsMap = new HashMap<>();
        packet.getFields().forEach(field -> {
            stripAttachmentFromField(attachmentsMap, field);
        });
        return attachmentsMap;
    }

    /**
     * Strips file attachments recursively from hpacket
     *
     * @param attachments
     * @param field
     */
    private void stripAttachmentFromField(Map<Long, byte[]> attachments, HPacketField field) {
        if (field.getType() == HPacketFieldType.OBJECT) {
            field.getInnerFields().stream().forEach(innerField -> {
                stripAttachmentFromField(attachments, innerField);
            });
        }

        if (field.getType() != HPacketFieldType.FILE)
            return;
        log.debug("FOUND ATTACHMENT: Field ID: {}", field.getId());
        //adding file to attachments map and resetting its value
        attachments.put(field.getId(), field.getValue().toString().getBytes(StandardCharsets.UTF_8));
        field.setValue(null);
    }

    private void handleError(Throwable t, String avroHPacket) {
        log.error(t.getMessage(), t);
        String rowKey = String.valueOf(Instant.now().toEpochMilli());
        log.info("Send error message \"{}\" and packet \"{}\" to HBase error table",
                t.getMessage(), avroHPacket);
        collector.emit(String.format(HDFS_ERROR_STREAM_ID_PREFIX, hprojectId), new Values(rowKey, t.getMessage(), avroHPacket));
    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector outputCollector) {
        config = stormConf;
        collector = outputCollector;
        hProjectTimelineUtil = new HProjectTimelineUtilImpl();
        objectMapper = new ObjectMapper();
    }

    private void sendToHBase(String avroHPacket, Map<Long, byte[]> attachments, long packetId, ModelType modelType, long timestamp, String rowKeyBeginning, Tuple input) throws IOException {
        log.debug("Serialized Avro String: {}", avroHPacket);
        String streamId;
        if (modelType.equals(ModelType.HPACKET)) {
            streamId = String.format(AVRO_HPROJECT_STREAM_ID_PREFIX, hprojectId);
            collector.emit(streamId, new Values(timestamp, packetId, avroHPacket, attachments));
        } else if (modelType.equals(ModelType.EVENT)) {
            streamId = String.format(EVENT_HPROJECT_STREAM_ID_PREFIX, hprojectId);
            log.debug("In Selection bolt emit versus hbase bolt event,  streamId : {}", streamId);
            int ruleId = (int) input.getValueByField(EVENT_RULE_ID);
            String rowKey = String.valueOf(timestamp).concat("_").concat(String.valueOf(ruleId));
            log.debug("In Selection Bolt emit versus hbase bolt event, timestamp is : {} ", timestamp);
            log.debug("In Selection bolt emit versus hbase bolt event, rowKey is : {} ", rowKey);
            collector.emit(streamId, new Values(rowKey, avroHPacket));
        } else if (modelType.equals(ModelType.ALARMEVENT)) {
            streamId = String.format(ALARM_EVENT_HPROJECT_STREAM_ID_PREFIX, hprojectId);
            long deviceId = ((Long) (input.getValueByField(StormConstants.ALARM_EVENT_DEVICE_ID)));
            String alarmState = (String) input.getValueByField(StormConstants.ALARM_STATE_FIELD);
            log.debug("In Selection bolt emit versus hbase bolt alarm, deviceId is : {} , alarmState is {} ", deviceId, alarmState);
            collector.emit(streamId, new Values(timestamp, deviceId, alarmState, avroHPacket));
        }
        TimelineUtil.emitTuplesToTimelineTable(collector, rowKeyBeginning, hprojectId, timestamp);
        log.debug("Tuples emitted");
    }

    /**
     * Send Avro HPacket to HDFS.
     * Depending on HPacket traffic plan, Storm can save into multiple directories.
     * Actually, this is an append operation on an existing file (HDFS bolt will create it, if it does not exist)
     *
     * @param packet HPacket
     * @param plan   HPacket traffic plan
     */
    private void sendToHDFS(HPacket packet, HPacketTrafficPlan plan) {
        Values values = new Values(packet);
        log.debug("Send HPacket with {} traffic plan", plan.getName());
        // Send always to year path: this allows to batch layer to process all data apart from traffic plan:
        // i.e., jobs will not worry about where data are depending on traffic plan
        collector.emit(YEAR_STREAM_ID, values);

        log.debug("********TrafficPlan is : {}", plan);
        switch (plan) {
            case LOW:
                break;
            case MEDIUM:
                collector.emit(MONTH_STREAM_ID, values);
                break;
            case HIGH:
                collector.emit(DAY_STREAM_ID, values);
                collector.emit(QUARTER_STREAM_ID, values);
                collector.emit(SEMESTER_STREAM_ID, values);
                break;
            case INTENSIVE:
                collector.emit(HOUR_STREAM_ID, values);
                collector.emit(QUARTER_STREAM_ID, values);
                collector.emit(SEMESTER_STREAM_ID, values);
                break;
            default:
                log.error("Unknown plan {}", plan.getName());
        }


    }

    /**
     * This is a config method, it is called in topology.yaml file
     */
    public SelectionBolt withHProject(String hprojectId) {
        this.hprojectId = Long.parseLong(hprojectId);
        return this;
    }

}

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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketInfo;
import it.acsoftware.hyperiot.hproject.deserialization.service.builder.HPacketDeserializerBuilder;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.storm.runtime.bolt.util.BoltUtil;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import it.acsoftware.hyperiot.stormmanager.model.HyperIoTTopologyError;

import org.apache.storm.Constants;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

import static it.acsoftware.hyperiot.storm.util.StormConstants.HPROJECT_ERROR_STREAM_ID;
import static it.acsoftware.hyperiot.storm.util.StormConstants.TIMELINE_HPROJECT_STREAM_ID_PREFIX;

public class DeserializationBolt extends BaseBasicBolt {

    private static final Logger log = LoggerFactory.getILoggerFactory().getLogger(DeserializationBolt.class.getName());
    private static final String STREAMING_TOPIC_PREFIX = "streaming.";
    private static final String DESERIALIZATION_OK_STREAM_ID = "deserializationOk";
    private static final String KAFKA_PAYLOAD = "payload";
    private static final String SYSTEM_TICK_STREAM_ID = "systemTick";
    private Map config;
    private long hprojectId;
    private final Integer aliveTickFrequency;
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // Default constructor
    public DeserializationBolt() {
        //TODO: disabling tick tuple in, please verify performance and uncomment or remove it
        //aliveTickFrequency = 10; // Default to 10 seconds
        aliveTickFrequency = 0;
    }

    // Constructor that sets emit frequency
    public DeserializationBolt(Integer frequency) {
        aliveTickFrequency = frequency;
    }

    //Configure frequency of tick tuples for this bolt
    //This delivers a 'tick' tuple on a specific interval,
    //which is used to trigger certain actions
    /*@Override
    public Map<String, Object> getComponentConfiguration() {
        Config conf = new Config();
        conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, aliveTickFrequency);
        return conf;
    }*/

    @Override
    public void prepare(Map stormConf, TopologyContext context) {
        log.debug("prepare with config {} context {}", stormConf, context);
        config = stormConf;
    }

    @Override
    public void execute(Tuple input, BasicOutputCollector collector) {
        log.trace("Execute method with input: {}", input);
        //If it's a tick tuple, emit all words and counts
        if (input.getSourceComponent().equals(Constants.SYSTEM_COMPONENT_ID)
                && input.getSourceStreamId().equals(Constants.SYSTEM_TICK_STREAM_ID)) {
            this.emitTickTuple(collector);
            return;
        }
        // check if topic is valid
        String topic = input.getValue(0).toString();
        if (topic.startsWith(STREAMING_TOPIC_PREFIX)) {
            long projectId = Long.parseLong(topic.split("\\.")[1]);
            //int partition = input.getLong(1);
            //long offset = input.getLong(2);
            byte[] kafkaKey = this.deserializeKafkaKey(input, collector);
            byte[] kafkaValue = this.deserializeKafkaValue(input, collector);
            if (kafkaKey != null && kafkaValue != null) {
                this.deserializeFromKafka(kafkaKey, kafkaValue, projectId, collector, input);
            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(SYSTEM_TICK_STREAM_ID, new Fields(StormConstants.DEVICEID_PACKETID_FIELD, StormConstants.PACKET_FIELD));
        outputFieldsDeclarer.declareStream(DESERIALIZATION_OK_STREAM_ID, new Fields(StormConstants.HPACKET_FIELD));
        outputFieldsDeclarer.declareStream(String.format(HPROJECT_ERROR_STREAM_ID, hprojectId), new Fields(StormConstants.TIMESTAMP_FIELD, StormConstants.MESSAGE_FIELD, StormConstants.RECEIVED_PACKET_FIELD));
        outputFieldsDeclarer.declareStream(String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, hprojectId),
                new Fields(StormConstants.ROWKEY_FIELD, StormConstants.TIMESTAMP_FIELD, StormConstants.STEP_FIELD));
    }

    /**
     * This is a config method, it is called in topology.yaml file
     */
    @SuppressWarnings("unused")
    public DeserializationBolt withHProject(String hprojectId) {
        this.hprojectId = Long.parseLong(hprojectId);
        return this;
    }

    /**
     * @param collector
     */
    private void emitTickTuple(BasicOutputCollector collector) {
        long unixTime = System.currentTimeMillis();
        Date now = new Date();
        log.debug("Emitting alive tick {}", unixTime);

        HDevice device = new HDevice();
        device.setEntityCreateDate(now);
        device.setEntityModifyDate(now);
        device.setCategoryIds(new long[0]);
        device.setTagIds(new long[0]);
        HProject hProject = new HProject();
        hProject.setEntityCreateDate(now);
        hProject.setEntityModifyDate(now);
        hProject.setCategoryIds(new long[0]);
        hProject.setTagIds(new long[0]);
        device.setProject(hProject);

        HPacket packet = new HPacket();
        packet.setEntityCreateDate(now);
        packet.setEntityModifyDate(now);
        packet.setName("systemTick");
        packet.setTimestampFormat("");
        packet.setTimestampField("timestamp");
        packet.setTrafficPlan(HPacketTrafficPlan.INTENSIVE);
        packet.setDevice(device);
        packet.setType(HPacketType.OUTPUT);
        packet.setCategoryIds(new long[0]);
        packet.setTagIds(new long[0]);
        packet.setUnixTimestamp(true);
        packet.setUnixTimestampFormatSeconds(false);

        Set<HPacketField> packetFields = new HashSet<>();
        HPacketField timestamp = new HPacketField();
        HPacketField projectIdField = new HPacketField();
        timestamp.setEntityCreateDate(now);
        timestamp.setEntityModifyDate(now);
        timestamp.setValue(unixTime);
        timestamp.setName(packet.getTimestampField());
        timestamp.setType(HPacketFieldType.TIMESTAMP);
        timestamp.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        timestamp.setCategoryIds(new long[0]);
        timestamp.setTagIds(new long[0]);
        timestamp.setPacket(packet);

        projectIdField.setEntityCreateDate(now);
        projectIdField.setEntityModifyDate(now);
        projectIdField.setValue(hprojectId);
        projectIdField.setName("hProjectId");
        projectIdField.setType(HPacketFieldType.LONG);
        projectIdField.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        projectIdField.setCategoryIds(new long[0]);
        projectIdField.setTagIds(new long[0]);
        projectIdField.setPacket(packet);
        packetFields.add(timestamp);
        packetFields.add(projectIdField);
        packet.defineFields(new ArrayList<>(packetFields));
        collector.emit(SYSTEM_TICK_STREAM_ID, new Values("0.0", packet));
    }

    /**
     * @param input
     * @param collector
     * @return
     */
    private byte[] deserializeKafkaKey(Tuple input, BasicOutputCollector collector) {
        try {
            return input.getBinary(3);
        } catch (Throwable e) {
            log.error("Could not deserialize Kafka key: {}", e.getMessage());
            HyperIoTTopologyError hyperIoTTopologyError = HyperIoTTopologyError.builder()
                    .errorType(e.getClass().getSimpleName())
                    .errorMessage(e.getMessage())
                    .build();
            BoltUtil.handleHyperIoTError(collector, hyperIoTTopologyError, hprojectId, new String(input.getBinary(3)),
                    Instant.now().toEpochMilli(), String.format(HPROJECT_ERROR_STREAM_ID, hprojectId));
            return null;
        }
    }

    /**
     * @param input
     * @param collector
     * @return
     */
    private byte[] deserializeKafkaValue(Tuple input, BasicOutputCollector collector) {
        try {
            return input.getBinary(4);
        } catch (Throwable e) {
            log.error("Could not deserialize Kafka value: {}", e.getMessage());
            HyperIoTTopologyError hyperIoTTopologyError = HyperIoTTopologyError.builder()
                    .errorType(e.getClass().getSimpleName())
                    .errorMessage(e.getMessage())
                    .build();
            BoltUtil.handleHyperIoTError(collector, hyperIoTTopologyError, hprojectId, new String(input.getBinary(4)),
                    Instant.now().toEpochMilli(), String.format(HPROJECT_ERROR_STREAM_ID, hprojectId));
        }
        return null;
    }

    /**
     * @param kafkaKeyRaw
     * @param kafkaValueRaw
     * @param projectId
     * @param collector
     */
    private void deserializeFromKafka(byte[] kafkaKeyRaw, byte[] kafkaValueRaw,
                                      long projectId, BasicOutputCollector collector, Tuple input) {
        String kafkaKey = new String(kafkaKeyRaw);
        String kafkaValue = new String(kafkaValueRaw);
        String[] parts = kafkaKey.split("\\.");
        //topic like streaming/<projectId>/<deviceId>/<packetId>
        log.debug("Kafka key parts: {}", (Object) parts);
        if (parts.length == 2) {
            // parse projectId, deviceId and packetId from kafka topic name
            log.debug("Parsing deviceId and packetId from parts: {}", (Object) parts);
            long deviceId = Long.parseLong(parts[0]);
            long packetId = Long.parseLong(parts[1]);
            log.debug("projectId: {}, deviceId: {}, packetId: {}", projectId, deviceId, packetId);
            try {
                // get fields schema from topology config ("schema.<packet_id>")
                if (config.get("packet." + packetId) == null) {
                    log.error("Packet with id {} does not exist: check if Kafka topic is a valid one!", packetId);
                    return;
                }
                String jsonSchema = config.get("packet." + packetId).toString();
                log.debug("JSON Schema for packet {}: {}", packetId, jsonSchema);
                HPacketInfo packetInfo = objectMapper.readValue(jsonSchema, HPacketInfo.class);
                packetInfo.setHProjectId(projectId);
                packetInfo.setHDeviceId(deviceId);
                packetInfo.setHPacketId(packetId);
                log.debug("PacketInfo Constructed {}", packetInfo);
                // parse incoming raw message based on the schema.type
                byte[] rawMessage = Base64.getDecoder().decode(kafkaValue);
                log.debug("PacketInfo tuple is {}", packetInfo.getSchema().getType());
                HPacketDeserializer hPacketDeserializer =
                        this.getHPacketDeserializer(packetInfo, kafkaValue, collector, input);
                HPacket packet = hPacketDeserializer.deserialize(rawMessage, packetInfo);
                if (packet.getFields() != null) {
                    collector.emit(DESERIALIZATION_OK_STREAM_ID, new Values(packet));
                } else {
                    log.error("Could not deserialized packet: {}", packet);
                    HyperIoTTopologyError hyperIoTTopologyError = HyperIoTTopologyError.builder()
                            .errorMessage("Could not deserialized packet")
                            .build();
                    BoltUtil.handleHyperIoTError(collector, hyperIoTTopologyError, hprojectId,
                            kafkaValue, Instant.now().toEpochMilli(),
                            String.format(HPROJECT_ERROR_STREAM_ID, hprojectId));
                }
            } catch (Exception e) {
                log.error("Unrecognized data type", e);
                HyperIoTTopologyError hyperIoTTopologyError = HyperIoTTopologyError.builder()
                        .errorMessage(e.getMessage())
                        .errorType(e.getClass().getSimpleName())
                        .build();
                BoltUtil.handleHyperIoTError(collector, hyperIoTTopologyError, hprojectId,
                        kafkaValue, Instant.now().toEpochMilli(),
                        String.format(HPROJECT_ERROR_STREAM_ID, hprojectId));
            }
        }
    }

    /**
     * @param packetInfo
     * @param kafkaValue
     * @param collector
     * @return
     */
    private HPacketDeserializer getHPacketDeserializer(HPacketInfo packetInfo, String kafkaValue,
                                                       BasicOutputCollector collector, Tuple input) {
        try {
            return HPacketDeserializerBuilder
                    .getDeserializer(HPacketFormat.valueOf(packetInfo.getSchema().getType().toUpperCase()));
        } catch (HyperIoTRuntimeException e) {
            log.error("Unknown packet type: {}", packetInfo.getSchema().getType());
            HyperIoTTopologyError hyperIoTTopologyError = HyperIoTTopologyError.builder()
                    .errorMessage("Unknown packet type")
                    .build();
            BoltUtil.handleHyperIoTError(collector, hyperIoTTopologyError, hprojectId,
                    kafkaValue, Instant.now().toEpochMilli(),
                    String.format(HPROJECT_ERROR_STREAM_ID, hprojectId));
        }
        return null;
    }

}

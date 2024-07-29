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

package it.acsoftware.hyperiot.storm.runtime.bolt.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import it.acsoftware.hyperiot.alarm.service.actions.AlarmAction;
import it.acsoftware.hyperiot.base.exception.HyperIoTException;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.JsonAvroHPacketSerializer;
import it.acsoftware.hyperiot.storm.alarm.AlarmState;
import it.acsoftware.hyperiot.stormmanager.model.HyperIoTTopologyError;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;


public final class BoltUtil {

    private static final Logger log = LoggerFactory.getLogger(BoltUtil.class);
    private static final EventFormat eventFormat;
    private static final ObjectMapper objectMapper;

    private static final HPacketSerializer hPacketSerializer;

    static {
        objectMapper = new ObjectMapper();
        hPacketSerializer = JsonAvroHPacketSerializer.getInstance();
        eventFormat = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
    }

    private BoltUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * This method constructs a new HPacket, which contains event information
     * @param packet Original HPacket instance
     * @param rule Rule
     * @param jsonAction Action to be executed
     * @return HPacket
     */
    public static HPacket createHPacketEvent(HPacket packet, Map<String, Object> rule, String jsonAction) {

        final String EVENT_PACKET_FIELD_NAME = "event";

        HDevice device = new HDevice();
        Date now = new Date();
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

        Set<HPacketField> packetFields = new HashSet<>();

        log.debug("In BoltUtil packet timestamp field is : {} , timestamp format is : {} ",packet.getTimestampField(), packet.getTimestampFormat());

        HPacket packetEvent = createHPacket(packet.getName() + BoltConstants.EVENT_PACKET_SUFFIX,
            HPacketType.OUTPUT, null, null, device, null, false,
            packet.getTimestampField(), packet.getTimestampFormat(), HPacketTrafficPlan.LOW, new long[0], new long[0]);

        log.debug("Adding to HPacket event the following action: {}", jsonAction);

        // create event according to Cloud Event specs
        CloudEvent event = CloudEventBuilder.v1()
                .withId(String.valueOf((int) rule.get("ruleId")))
                .withType((String) rule.get("ruleName"))
                .withSource(URI.create("http://dashboard-test.hyperiot.cloud/"))
                .withData(jsonAction.getBytes())
                .build();

        HPacketField action = createHPacketField(EVENT_PACKET_FIELD_NAME, null, HPacketFieldType.TEXT,
                HPacketFieldMultiplicity.SINGLE, null, packetEvent, null, null,
                new String(eventFormat.serialize(event)), new long[0], new long[0]);

        packetFields.add(action);
        packetFields.addAll(packet.getFields());
        packetEvent.defineFields(new ArrayList<>(packetFields));
        return packetEvent;
    }

    /**
     * Return HPacket in Avro String format
     * @param hPacket HPacket
     * @return byte array containing avro HPacket
     * @throws IOException Throws exception, which is managed by caller
     */
    public static String getAvroHPacket(HPacket hPacket) throws IOException {
        return new String(hPacketSerializer.serialize(hPacket), StandardCharsets.UTF_8);
    }

    public static HPacket createHPacket(String name, HPacketType type, HPacketFormat format,
                                        HPacketSerialization serialization, HDevice device, String version,
                                        boolean valid, String timestampField, String timestampFormat,
                                        HPacketTrafficPlan trafficPlan, long[] categoryIds, long[] tagIds) {
        HPacket hPacket = new HPacket();
        Date now = new Date();
        hPacket.setEntityCreateDate(now);
        hPacket.setEntityModifyDate(now);
        hPacket.setName(name);
        hPacket.setType(type);
        hPacket.setFormat(format);
        hPacket.setSerialization(serialization);
        hPacket.setDevice(device);
        hPacket.setVersion(version);
        hPacket.setValid(valid);
        hPacket.setTimestampFormat(timestampFormat);
        hPacket.setTimestampField(timestampField);
        hPacket.setTrafficPlan(trafficPlan);
        hPacket.setCategoryIds(categoryIds);
        hPacket.setTagIds(tagIds);
        return hPacket;
    }

    public static HPacketField createHPacketField(String name, String description, HPacketFieldType type,
                                                  HPacketFieldMultiplicity multiplicity, String unit, HPacket hPacket,
                                                  HPacketField parentField, Set<HPacketField> innerFields,
                                                  Object value, long[] categoryIds, long[] tagIds) {
        HPacketField hPacketField = new HPacketField();
        Date now = new Date();
        hPacketField.setEntityCreateDate(now);
        hPacketField.setEntityModifyDate(now);
        hPacketField.setName(name);
        hPacketField.setDescription(description);
        hPacketField.setType(type);
        hPacketField.setMultiplicity(multiplicity);
        hPacketField.setUnit(unit);
        hPacketField.setPacket(hPacket);
        hPacketField.setParentField(parentField);
        hPacketField.setInnerFields(innerFields);
        hPacketField.setValue(value);
        hPacketField.setCategoryIds(categoryIds);
        hPacketField.setTagIds(tagIds);
        return hPacketField;
    }

    public static void handleHyperIoTError(BasicOutputCollector collector, HyperIoTTopologyError hyperIoTTopologyError,
                                           long hProjectId, Object packet, long timestamp, String streamId) {
        try {
            String jsonError = objectMapper.writeValueAsString(hyperIoTTopologyError);
            log.debug("Send error message {} and packet {} on stream {}", jsonError, packet, streamId);
            collector.emit(streamId, new Values(timestamp, jsonError, packet));
            TimelineUtil.emitTuplesToTimelineTable(collector, "Error", hProjectId, timestamp);
            log.debug("Tuple sent to the proper HBase error table");
        } catch (Throwable ex) {
            log.error("Could not send error to hbase: ", ex);
        }
    }

    public static void handleHyperIoTError(Tuple tuple, OutputCollector collector, HyperIoTTopologyError hyperIoTTopologyError,
                                    long hProjectId, Object packet, long timestamp, String streamId) {
        try {
            String jsonError = objectMapper.writeValueAsString(hyperIoTTopologyError);
            log.debug("Send error message {} and packet {} on stream {}", jsonError, packet, streamId);
            collector.emit(streamId, new Values(timestamp, jsonError, packet));
            TimelineUtil.emitTuplesToTimelineTable(collector, "Error", hProjectId, timestamp);
            collector.ack(tuple);
            log.debug("Tuple sent to the proper HBase error table");
        } catch (Throwable ex) {
            log.error("Could not send error to hbase: ", ex);
        }
    }


    /**
     * This method constructs a new HPacket, which contains event information
     * @param alarmState the state of the alarm (DOWN, UP)
     * @param packet Original HPacket instance
     * @param rule Rule
     * @param jsonAction Action to be executed
     * @return HPacket
     */
    public static HPacket createHPacketForEventRelatedToAlarm(HPacket packet, Map<String, Object> rule, String jsonAction, AlarmState alarmState) {

        final String EVENT_PACKET_FIELD_NAME = "event";

        HDevice device = new HDevice();
        Date now = new Date();
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

        Set<HPacketField> packetFields = new HashSet<>();

        log.debug("In BoltUtil packet timestamp field is : {} , timestamp format is : {} ",packet.getTimestampField(), packet.getTimestampFormat());

        HPacket packetEvent = createHPacket(packet.getName() + BoltConstants.ALARM_EVENT_PACKET_SUFFIX,
                HPacketType.OUTPUT, null, null, device, null, false,
                packet.getTimestampField(), packet.getTimestampFormat(), HPacketTrafficPlan.LOW, new long[0], new long[0]);

        long deviceId = packet.getDevice().getId();
        String dataEventPayload = createEventDataPayLoadForAlarmEvent(jsonAction, alarmState, deviceId);
        log.debug("In BoltUtil : adding to HPacket event the following event payload: {}", dataEventPayload);

        // create event according to Cloud Event specs
        CloudEvent event = CloudEventBuilder.v1()
                .withId(String.valueOf((int) rule.get("ruleId")))
                .withType((String) rule.get("ruleName"))
                .withSource(URI.create("http://dashboard-test.hyperiot.cloud/"))
                .withData(dataEventPayload.getBytes())
                .build();

        HPacketField action = createHPacketField(EVENT_PACKET_FIELD_NAME, null, HPacketFieldType.TEXT,
                HPacketFieldMultiplicity.SINGLE, null, packetEvent, null, null,
                new String(eventFormat.serialize(event)), new long[0], new long[0]);

        packetFields.add(action);
        packetFields.addAll(packet.getFields());
        packetEvent.defineFields(new ArrayList<>(packetFields));
        return packetEvent;
    }

    private static String createEventDataPayLoadForAlarmEvent(String jsonAction, AlarmState alarmState, long deviceId){
        try {
            log.debug("In BoltUtil createEventDataPayload jsonAction is : {}", jsonAction);
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
            HashMap<String, Object> jsonActionMap = objectMapper.readValue(jsonAction, typeRef);
            jsonActionMap.remove(ALARM_STATE_PROPERTY);
            jsonActionMap.put(ALARM_STATE_PROPERTY, alarmState);
            jsonActionMap.remove(ALARM_DEVICE_ID_PROPERTY);
            jsonActionMap.put(ALARM_DEVICE_ID_PROPERTY, deviceId);
            return objectMapper.writeValueAsString(jsonActionMap);
        } catch (Exception e){
            log.debug("In Bolt Util fail to add AlarmState property");
            log.error(e.getMessage(), e);
            return jsonAction;
        }
    }

    private static final String ALARM_STATE_PROPERTY = "alarmState";

    private static final String ALARM_DEVICE_ID_PROPERTY = "deviceId";

}

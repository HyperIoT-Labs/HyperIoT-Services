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

package it.acsoftware.hyperiot.hproject.model.hbase;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineHPacket;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineHPacketField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains information about HPacket.
 * Property hPacketId is HPacket ID, while values contains HPacket values.
 */
@SuppressWarnings("unused")
public class HProjectScan {

    private static final Logger LOGGER = LoggerFactory.getLogger(HProjectScan.class.getName());
    private static final String TIMESTAMP_DEFAULT_FIELD_NAME = "timestamp";
    private static final String ERROR_COLUMM = "error";
    private static final String ERROR_MESSAGE_COLUMM = "errorMessage";
    public static final String PACKET_COLUMN = "packet";
    public static final String RECEIVED_PACKET_COLUMN = "received_packet";

    private long hPacketId;
    private List<TimelineHPacket> values;
    private long rowKeyUpperBound;

    /**
     * TODO we cannot move these constants inside HProjectHBaseConstants class due to circular dependency
     *  May we move them inside a cfg file?
     */
    private static final String EVENT_PACKET_FIELD_NAME = "event";
    private static final String EVENT_PACKET_NAME_SUFFIX = "_event";
    private static final String EVENT_TIMESTAMP_PACKET_FIELD_NAME = "timestamp";
    private static final String ALARM_EVENT_PACKET_FIELD_NAME = "alarmEvent";
    private static final String ALARM_EVENT_PACKET_NAME_SUFFIX = "_event_alarm";
    private static final String ALARM_EVENT_TIMESTAMP_PACKET_FIELD_NAME = "timestamp";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private EventFormat eventFormat;

    public HProjectScan(long hPacketId) {
        this.hPacketId = hPacketId;
        values = new ArrayList<>();
        EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();
        eventFormatProvider.registerFormat(new JsonFormat());
        eventFormat = eventFormatProvider.resolveFormat(JsonFormat.CONTENT_TYPE);
    }

    public long gethPacketId() {
        return hPacketId;
    }

    public void sethPacketId(long hPacketId) {
        this.hPacketId = hPacketId;
    }

    public List<TimelineHPacket> getValues() {
        return values;
    }

    public void setValues(List<TimelineHPacket> values) {
        this.values = values;
    }

    public long getRowKeyUpperBound() {
        return rowKeyUpperBound;
    }

    public void setRowKeyUpperBound(long rowKeyUpperBound) {
        this.rowKeyUpperBound = rowKeyUpperBound;
    }

    /**
     * Method to add ordinary packet or event
     *
     * @param hPacket hpacket
     */
    public void addValue(HPacket hPacket) {
        TimelineHPacket timelineHPacket = new TimelineHPacket();
        if (hPacket.getId() == 0 && hPacket.getName().endsWith(EVENT_PACKET_NAME_SUFFIX)) {
            // it is a event packet: create its representation for Timeline
            timelineHPacket.setTimestampField(EVENT_TIMESTAMP_PACKET_FIELD_NAME);
            // add two fields ...
            List<TimelineHPacketField> timelineHPacketFieldList = new ArrayList<>();
            // ... timestamp ...
            timelineHPacketFieldList.add(createEventTimelineHPacketField(hPacket,
                    EVENT_TIMESTAMP_PACKET_FIELD_NAME));
            // ... and event
            // At the moment, add event name only
            TimelineHPacketField eventPacketField = createEventTimelineHPacketField(hPacket,
                    EVENT_PACKET_FIELD_NAME);
            try {
                // get event according to Cloud Event specs
                CloudEvent cloudEvent = eventFormat.deserialize(((String) eventPacketField.getValue()).getBytes());
                HProjectEvent hProjectEvent =
                        objectMapper.readValue(new String(Objects.requireNonNull(cloudEvent.getData()).toBytes()), HProjectEvent.class);
                eventPacketField.setValue(hProjectEvent.getRuleName());
            } catch (Throwable e) {
                LOGGER.error("Error parsing event", e);
                eventPacketField.setValue("No value found");
            }
            timelineHPacketFieldList.add(eventPacketField);
            timelineHPacket.setFields(timelineHPacketFieldList);
        } else if (hPacket.getId() == 0 && hPacket.getName().endsWith(ALARM_EVENT_PACKET_NAME_SUFFIX)) {
            //it's an alarm event packet : create its representation for timeline.
            timelineHPacket.setTimestampField(ALARM_EVENT_TIMESTAMP_PACKET_FIELD_NAME);
            List<TimelineHPacketField> timelineHPacketFieldList = new ArrayList<>();
            timelineHPacketFieldList.add(createEventTimelineHPacketField(hPacket,
                    ALARM_EVENT_TIMESTAMP_PACKET_FIELD_NAME));
            TimelineHPacketField alarmEventPacketField = createEventTimelineHPacketField(hPacket,
                    EVENT_PACKET_FIELD_NAME);
            try {
                // get event according to Cloud Event specs
                CloudEvent cloudEvent = eventFormat.deserialize(((String) alarmEventPacketField.getValue()).getBytes());
                //Deserialize as a map of value.
                String eventPayload = objectMapper.writeValueAsString(
                        objectMapper.readValue(new String(Objects.requireNonNull(cloudEvent.getData()).toBytes()), Map.class));
                alarmEventPacketField.setValue(eventPayload);
            } catch (Throwable e) {
                LOGGER.error("Error parsing event", e);
                alarmEventPacketField.setValue("No value found");
            }
            timelineHPacketFieldList.add(alarmEventPacketField);
            timelineHPacket.setFields(timelineHPacketFieldList);
        } else {
            timelineHPacket.setTimestampField(hPacket.getTimestampField());
            List timelineHPacketFields = getFieldsHierarchy(hPacket.getFieldsMap());
            timelineHPacket.setFields(timelineHPacketFields);
        }
        values.add(timelineHPacket);
    }

    private List<TimelineHPacketField> getFieldsHierarchy(final Map<String,HPacketField> fields) {
        return fields.keySet().stream().map(path -> {
            TimelineHPacketField timelineHPacketField = new TimelineHPacketField();
            timelineHPacketField.setName(path);
            timelineHPacketField.setValue(fields.get(path).getValue());
            return timelineHPacketField;
        }).collect(Collectors.toList());
    }

    /**
     * Method to add error
     *
     * @param error     error
     * @param timestamp timestamp
     */
    public void addValue(Map<String, Object> error, long timestamp) {
        TimelineHPacket timelineHPacket = new TimelineHPacket();
        TimelineHPacketField timestampField = new TimelineHPacketField();
        timestampField.setName(TIMESTAMP_DEFAULT_FIELD_NAME);
        timestampField.setValue(timestamp);
        timelineHPacket.setTimestampField(TIMESTAMP_DEFAULT_FIELD_NAME);
        TimelineHPacketField errorField = new TimelineHPacketField();
        errorField.setName(ERROR_COLUMM);
        errorField.setValue(error.get(ERROR_MESSAGE_COLUMM));
        TimelineHPacketField errorPacketField = new TimelineHPacketField();
        errorPacketField.setName(PACKET_COLUMN);
        errorPacketField.setValue(error.get(PACKET_COLUMN));
        List<TimelineHPacketField> fields = new ArrayList<>();
        fields.add(timestampField);
        fields.add(errorField);
        fields.add(errorPacketField);
        timelineHPacket.setFields(fields);
        values.add(timelineHPacket);
    }

    private TimelineHPacketField createEventTimelineHPacketField(HPacket originalHPacket, String fieldName) {
        TimelineHPacketField hPacketEventField = new TimelineHPacketField();
        hPacketEventField.setName(fieldName);
        HPacketField eventHPacketField = originalHPacket.getFields()
                .stream()
                .filter(hPacketField -> hPacketField.getName().equals(fieldName))
                .findFirst()
                .orElse(null);
        if (eventHPacketField == null)
            hPacketEventField.setValue("No value found");
        else
            hPacketEventField.setValue(eventHPacketField.getFieldValue());
        return hPacketEventField;
    }

}

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

package it.acsoftware.hyperiot.hproject.deserialization.service.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketInfo;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketSchema;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketTimestamp;
import it.acsoftware.hyperiot.hproject.deserialization.model.exception.TimestampConversionException;
import it.acsoftware.hyperiot.hproject.deserialization.model.exception.TimestampFieldNotFoundException;
import it.acsoftware.hyperiot.hproject.deserialization.model.exception.TimestampFormatException;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class HPacketDeserializerUtil {

    private static final Logger log = LoggerFactory.getLogger(HPacketDeserializerUtil.class);

    private HPacketDeserializerUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates HPacketInfo necesseray from deserialization starting from a known HPacket
     *
     * @param packet
     * @return
     */
    public static HPacketInfo createPacketInfoFromHPacket(HPacket packet) {
        HPacketInfo info = new HPacketInfo();
        info.setHPacketId(packet.getId());
        info.setType(packet.getType().toString());
        info.setHDeviceId(packet.getDevice().getId());
        info.setName(packet.getName());
        info.setUnixTimestamp(packet.isUnixTimestamp());
        info.setUnixTimestampFormatSeconds(packet.isUnixTimestampFormatSeconds());
        info.setTrafficPlan(packet.getTrafficPlan().toString());
        info.setHProjectId(packet.getDevice().getProject().getId());
        HPacketSchema schema = new HPacketSchema();
        schema.setType(packet.getType().toString());
        schema.setFields(packet.getFlatFieldsMap());
        info.setSchema(schema);
        HPacketTimestamp timestamp = new HPacketTimestamp();
        timestamp.setField(packet.getTimestampField());
        timestamp.setFormat(packet.getTimestampFormat());
        info.setTimestamp(timestamp);
        return info;
    }

    /**
     * Add a field specified by `path` from the source `node` tree to the
     * HPacketField `fields` collection belonging to the given `packet`.
     *
     * @param packet            target packet (required to set a reference to in newly added fields)
     * @param fields            output fields collection where to add parsed key/value
     * @param node              input node where to search for path
     * @param path              path of the value to be added (supports dotted notation, eg. 'gps.latitude')
     * @param innerFieldDataObj Map containing field id and type
     */
    public static void addField(HPacket packet, Set<HPacketField> fields, HashMap<String, Object> node, String path,
                                Object innerFieldDataObj) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("ADDING FIELD DEBUG INFO \n PATH: {}", path);
            log.debug("------ NODE MAP------");
            AtomicReference<HashMap> atomicReference = new AtomicReference<>(node);
            node.keySet().forEach(key -> log.debug("{} : {}", key, atomicReference.get().get(key)));
            log.debug("------ FIELDS------");
            fields.forEach(field -> log.debug("{}", field));
            log.debug("------ INNER FIELDS------");
            log.debug("{}", innerFieldDataObj);
        }
        HashMap<String, Object> innerFieldData = (HashMap<String, Object>) innerFieldDataObj;
        String p = path;
        int dotIndex = path.indexOf(".");
        if (dotIndex > 0) {
            p = path.substring(0, dotIndex);
            path = path.substring(dotIndex + 1);
        }
        if (node.containsKey(p)) {
            log.debug("node contains {}", p);
            if (node.get(p) instanceof LinkedHashMap) {
                log.debug("field is array");
                final String cp = p;
                HPacketField field = fields.stream().filter((f) -> f.getName().equals(cp)).findFirst().orElse(null);
                if (field == null) {
                    log.debug("field found, create packet field");
                    field = new HPacketField();
                    Date now = new Date();
                    field.setEntityCreateDate(now);
                    field.setEntityModifyDate(now);
                    field.setName(p);
                    field.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
                    field.setType(HPacketFieldType.OBJECT);
                    field.setCategoryIds(new long[0]);
                    field.setTagIds(new long[0]);
                    field.setPacket(packet);
                    fields.add(field);
                }
                Set<HPacketField> innerFields = field.getInnerFields();
                if (innerFields == null) {
                    innerFields = new HashSet<>();
                    field.setInnerFields(innerFields);
                }
                node = (HashMap<String, Object>) node.get(p);
                addField(packet, innerFields, node, path, innerFieldData);
            } else {
                log.debug("field is not array");
                String value = node.get(p).toString();
                log.debug("[METHOD] addField -> parsing field \"{}\" with value \"{}\"", p, value);
                int multiplicity = guessMultiplicity(value);
                ObjectMapper mapper = new ObjectMapper();
                HPacketField field = new HPacketField();
                Date now = new Date();
                field.setEntityCreateDate(now);
                field.setEntityModifyDate(now);
                field.setName(p);
                Number fieldId = null;

                if (innerFieldData.get("id") instanceof String)
                    fieldId = Long.parseLong((String) innerFieldData.get("id"));
                else if (innerFieldData.get("id") instanceof Number)
                    fieldId = (Number) innerFieldData.get("id");

                field.setId(fieldId.longValue());
                if (multiplicity == 1) {
                    field.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
                } else if (multiplicity > 1) {
                    throw new UnsupportedOperationException();
                } else {
                    field.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
                }
                field.setType(HPacketFieldType.valueOf(innerFieldData.get("type").toString().toUpperCase()));
                switch (field.getType()) {
                    case BOOLEAN:
                        if (multiplicity == 1) {
                            // Array
                            TypeReference<List<Boolean>> typeRef = new TypeReference<List<Boolean>>() {
                            };
                            List<Boolean> valueArray = mapper.readValue(value, typeRef);
                            field.setValue(valueArray);
                        } else {
                            // Single value
                            field.setValue(Boolean.valueOf(value));
                        }
                        break;
                    case DOUBLE:
                        if (multiplicity == 1) {
                            // Array
                            TypeReference<List<Double>> typeRef = new TypeReference<List<Double>>() {
                            };
                            List<Double> valueArray = mapper.readValue(value, typeRef);
                            field.setValue(valueArray);
                        } else {
                            // Single value
                            field.setValue(Double.valueOf(value));
                        }
                        break;
                    case FLOAT:
                        if (multiplicity == 1) {
                            // Array
                            TypeReference<List<Float>> typeRef = new TypeReference<List<Float>>() {
                            };
                            List<Float> valueArray = mapper.readValue(value, typeRef);
                            field.setValue(valueArray);
                        } else {
                            // Single value
                            field.setValue(Float.valueOf(value));
                        }
                        break;
                    case INTEGER:
                        if (multiplicity == 1) {
                            // Array
                            TypeReference<List<Integer>> typeRef = new TypeReference<List<Integer>>() {
                            };
                            List<Integer> valueArray = mapper.readValue(value, typeRef);
                            field.setValue(valueArray);
                        } else {
                            // Single value
                            field.setValue(Integer.valueOf(value));
                        }
                        break;
                    case DATE:
                        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
                        TemporalAccessor accessor = timeFormatter.parse(value);
                        field.setValue(Date.from(Instant.from(accessor)));
                        break;
                    case TIMESTAMP:
                        field.setValue(Long.valueOf(value));
                        break;
                    default:
                        field.setValue(node.get(p));
                }
                field.setCategoryIds(new long[0]);
                field.setTagIds(new long[0]);
                field.setPacket(packet);
                fields.add(field);
            }
        } else {
            log.debug("path not found for {}", p);
            StringBuilder sb = new StringBuilder();
            fields.forEach(hPacketField -> sb.append(hPacketField.getName()));
            log.debug("Print fields {}", sb);
        }
    }

    /**
     * Creates HDevice starting from deviceId and HProject object
     *
     * @param deviceId
     * @param project
     * @return
     */
    public static HDevice createHDevice(long deviceId, HProject project) {
        Date now = new Date();
        HDevice device = new HDevice();
        device.setId(deviceId);
        device.setEntityCreateDate(now);
        device.setEntityModifyDate(now);
        device.setCategoryIds(new long[0]);
        device.setTagIds(new long[0]);
        device.setProject(project);
        return device;
    }

    public static HPacket createHPacket(HPacketInfo hPacketInfo, HashMap<String, Object> message) {
        long projectId = hPacketInfo.getHProjectId();
        HProject project = HPacketDeserializerUtil.createHProject(projectId);
        long deviceId = hPacketInfo.getHDeviceId();
        HDevice device = HPacketDeserializerUtil.createHDevice(deviceId, project);
        long packetId = hPacketInfo.getHPacketId();
        return HPacketDeserializerUtil.createHPacket(packetId, device, hPacketInfo, message);
    }

    /**
     * Creates HPacket instance starting from HPacketInfo,Device and packetId
     *
     * @param packetId
     * @param device
     * @param hPacketInfo
     * @param message
     * @return
     */
    public static HPacket createHPacket(long packetId, HDevice device, HPacketInfo hPacketInfo, HashMap<String, Object> message) {
        Date now = new Date();
        HPacket packet = new HPacket();
        packet.setEntityCreateDate(now);
        packet.setEntityModifyDate(now);
        packet.setName(hPacketInfo.getName());
        packet.setTimestampFormat(hPacketInfo.getTimestamp().getFormat());
        packet.setTimestampField(hPacketInfo.getTimestamp().getField());
        packet.setUnixTimestamp(hPacketInfo.isUnixTimestamp());
        packet.setUnixTimestampFormatSeconds(hPacketInfo.isUnixTimestampFormatSeconds());
        packet.setTrafficPlan(HPacketTrafficPlan.valueOf(hPacketInfo.getTrafficPlan().toUpperCase()));
        packet.setId(packetId);
        packet.setDevice(device);
        packet.setType(HPacketType.valueOf(hPacketInfo.getType().toUpperCase()));
        packet.setCategoryIds(new long[0]);
        packet.setTagIds(new long[0]);
        Set<HPacketField> packetFields = new HashSet<>();
        final HashMap<String, Object> m = message;
        hPacketInfo.getSchema().getFields().forEach((key, innerFieldData) -> {
            try {
                //Timestamp field is added manually later
                if (!key.equalsIgnoreCase(packet.getTimestampField()))
                    HPacketDeserializerUtil.addField(packet, packetFields, m, key, innerFieldData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        //finally we always add timestamp field
        HPacketField convertedTimestamp = createTimestampField(packet, hPacketInfo, m);
        packetFields.add(convertedTimestamp);
        if (packetFields.size() > 0)
            packet.defineFields(new ArrayList<>(packetFields));
        return packet;
    }

    /**
     * Creates HProject instance from projectId
     *
     * @param projectId
     * @return
     */
    public static HProject createHProject(long projectId) {
        Date now = new Date();
        HProject project = new HProject();
        project.setId(projectId);
        project.setEntityCreateDate(now);
        project.setEntityModifyDate(now);
        project.setCategoryIds(new long[0]);
        project.setTagIds(new long[0]);
        return project;
    }

    private static long createHPacketTimestamp(HPacket packet, HPacketInfo hPacketInfo, HashMap<String, Object> fields) {
        long timestamp = 0L;
        try {
            timestamp = getConvertedTimestamp(packet, fields);
            log.debug("Converted timestamp is: {}", timestamp);
        } catch (TimestampFieldNotFoundException e) {
            if (hPacketInfo.getTimestamp().isCreateDefaultIfNotExists()) {
                timestamp = setHPacketDefaultTimestamp(packet);
            } else {
                timestamp = Instant.now().toEpochMilli();
            }
        } catch (Exception e1) {
            //in case of error we add default timestamp
            timestamp = setHPacketDefaultTimestamp(packet);
        }
        //After the conversion packet has always UNIX timestamp in MILLISECONDS
        packet.setUnixTimestamp(true);
        packet.setUnixTimestampFormatSeconds(false);
        return timestamp;
    }

    /**
     * Add timestamp default values.
     * Notice that these default values aren't persisted: the HPacket received will get always
     * timestamp values submitted by user. However, during streaming process,
     * there isn't timestamp field, so we need a default value.
     *
     * @param packet Received HPacket
     * @return Default timestamp, i.e. The number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
     */
    private static long setHPacketDefaultTimestamp(HPacket packet) {
        log.debug("Error during conversion, or timestamp field \"{}\" isn't present in raw message: add timestamp default values",
                packet.getTimestampField());
        packet.setUnixTimestamp(true);
        packet.setUnixTimestampFormatSeconds(false);
        packet.setTimestampField("timestamp-default");
        packet.setTimestampFormat("UTC-default");
        long timestamp = Instant.now().toEpochMilli();
        log.debug("Current timestamp: {}", timestamp);
        return timestamp;
    }


    /**
     * It returns packet timestamp. It can be a number (millis Unix Epoch time) or a String,
     * which need to match HPacket timestamp format. In the latter case, there's being a timestamp conversion,
     * returning timestamp in Unix Epoch Time.
     *
     * @param packet HPacket
     * @param fields Map derived from raw message
     * @return The number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
     */
    public static long getConvertedTimestamp(HPacket packet, HashMap<String, Object> fields) throws TimestampFieldNotFoundException, TimestampFormatException, TimestampConversionException {
        if (!fields.containsKey(packet.getTimestampField())) {
            throw new TimestampFieldNotFoundException();
        }
        Object timestamp = fields.get(packet.getTimestampField());
        log.debug("Received timestamp: {} is Unix timestamp ? {}", timestamp, packet.isUnixTimestamp());
        if (packet.isUnixTimestamp()) {
            //forcing using long values
            if (timestamp instanceof Integer) {
                log.debug("Timestamp received as Integer, converting to long");
                timestamp = ((Integer) timestamp).longValue();
            }

            //not inserting else because it must enter in the second if
            if (timestamp instanceof Long) {
                //converting to milliseconds
                if (packet.isUnixTimestampFormatSeconds()) {
                    log.debug("Timestamp received is UNIX in SECONDS, converting to millis");
                    return (((Long) timestamp).longValue() * 1000L);
                } else {
                    log.debug("Timestamp received is UNIX in MILLISECONDS");
                    return (long) timestamp;
                }
            }
        } else if (timestamp instanceof String) {
            log.debug("Timestamp received is String parsing....");
            return timestampConversion((String) timestamp, packet);
        }
        //timestamp isn't number or String, invalid value: add default value
        log.debug("Invalid received timestamp {}. returning -1", timestamp);
        throw new TimestampFormatException();
    }

    /**
     * Creates HPacket timestamp field based on timetsamp
     *
     * @param hPacketInfo
     * @param fields
     * @param packet
     * @return
     */
    public static HPacketField createTimestampField(HPacket packet, HPacketInfo hPacketInfo, HashMap<String, Object> fields) {
        long timestamp = createHPacketTimestamp(packet, hPacketInfo, fields);
        Date now = new Date();
        HPacketField timestampField = new HPacketField();
        timestampField.setEntityCreateDate(now);
        timestampField.setEntityModifyDate(now);
        timestampField.setValue(timestamp);
        timestampField.setName(packet.getTimestampField());
        timestampField.setType(HPacketFieldType.TIMESTAMP);
        timestampField.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        timestampField.setCategoryIds(new long[0]);
        timestampField.setTagIds(new long[0]);
        timestampField.setPacket(packet);
        return timestampField;
    }

    /**
     * It does timestamp conversion.
     * If conversion throws ParseException, add timestamp default value.
     *
     * @param timestamp Received String timestamp
     * @param packet    HPacket
     * @return The number of milliseconds from the epoch of 1970-01-01T00:00:00Z.
     */
    private static long timestampConversion(String timestamp, HPacket packet) throws TimestampConversionException {
        log.debug("Convert timestamp. HPacket timestamp format: {}", packet.getTimestampFormat());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(packet.getTimestampFormat());
        try {
            Date date = simpleDateFormat.parse(timestamp);
            return date.toInstant().toEpochMilli();
        } catch (ParseException e) {
            log.warn("Error during conversion, add timestamp default value");
            throw new TimestampConversionException();
        }
    }

    /**
     * @param value
     * @return
     */
    private static int guessMultiplicity(String value) {
        value = value.replaceAll("\\s", "");
        if (value.charAt(0) == '[') {
            if (value.charAt(1) == '[') return 2;
            return 1;
        }
        return 0;
    }

}

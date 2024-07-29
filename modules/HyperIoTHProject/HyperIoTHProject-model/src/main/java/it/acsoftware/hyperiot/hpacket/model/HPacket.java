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

package it.acsoftware.hyperiot.hpacket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedChildResource;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.model.HProjectJSONView;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static javax.persistence.CascadeType.ALL;

/**
 * @author Aristide Cittadino Model class for HPacket of HyperIoT platform. This
 * class is used to map HPacket with the database.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "device_id", "version"})})
public class HPacket extends HyperIoTAbstractEntity
        implements HyperIoTProtectedEntity, HyperIoTOwnedChildResource, GenericRecord {

    /**
     * Packet name, used to identify it
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private String name;
    /**
     * Packet type, indicates if it is input,output or both
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private HPacketType type;

    /**
     * Transmission format es. json,xml,csv
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private HPacketFormat format;
    /**
     * Type of serialization, es. none or avro
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private HPacketSerialization serialization;

    /**
     * The related device which sends the packet
     */
    @JsonView({HyperIoTJSONView.Public.class})
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    private HDevice device;

    /**
     * Packet version
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private String version;

    /**
     * Packet fields
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private Set<HPacketField> fields;

    /**
     * runtime copy of fields
     */
    private HashMap<String, HPacketField> fieldsMap;

    /**
     * Transient boolean which indicates the current packet is valid. Not persistent
     * because it's related to data streaming not data definition
     */
    @JsonView({HyperIoTJSONView.Public.class, HProjectJSONView.Export.class})
    private boolean valid;

    /**
     * Packet timestamp field
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private String timestampField;

    /**
     * Packet timestamp format
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private String timestampFormat;

    /**
     * true if packet is sent with unix timestamp
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private boolean unixTimestamp;

    /**
     * true if packet has seconds format in timestamp value not milliseconds
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private boolean unixTimestampFormatSeconds;


    /**
     * Packet traffic plan: it indicates how many bytes per day are sent for its
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    private HPacketTrafficPlan trafficPlan;

    /**
     *
     */
    public HPacket() {
        this.unixTimestamp = true;
        this.unixTimestampFormatSeconds = false;
    }

    /**
     * @return packet name
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Size(max = 255)
    public String getName() {
        return name;
    }

    /**
     * @param name packet name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return packet type
     */
    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public HPacketType getType() {
        return type;
    }

    /**
     * @param type packet type
     */
    public void setType(HPacketType type) {
        this.type = type;
    }

    /**
     * @return packet format
     */
    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public HPacketFormat getFormat() {
        return format;
    }

    /**
     * @param format packet format
     */
    public void setFormat(HPacketFormat format) {
        this.format = format;
    }

    /**
     * @return serialization type
     */
    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public HPacketSerialization getSerialization() {
        return serialization;
    }

    /**
     * @param serialization serialization type
     */
    public void setSerialization(HPacketSerialization serialization) {
        this.serialization = serialization;
    }

    /**
     * Get packet version.
     *
     * @return packet version
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Size(max = 255)
    public String getVersion() {
        return version;
    }

    /**
     * @param version packet version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get packet's fields.
     *
     * @return packet fields
     */

    @OneToMany(targetEntity = HPacketField.class, cascade = {ALL}, mappedBy = "packet", fetch = FetchType.EAGER, orphanRemoval = true)
    @Where(clause = "parentField_id IS NULL")
    @Fetch(FetchMode.SUBSELECT)
    public Set<HPacketField> getFields() {
        return fields;
    }

    /**
     * Set packet's fields.
     *
     * @param fields HPacket field list
     */
    public void setFields(Set<HPacketField> fields) {
        this.fields = fields;
    }

    /**
     * Get the bound device.
     *
     * @return the related device
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HDevice.class)
    public HDevice getDevice() {
        return device;
    }

    /**
     * Set the bound device.
     *
     * @param device HDevice
     */
    public void setDevice(HDevice device) {
        this.device = device;
    }

    @Transient
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Get field containing HPacket timestamp
     *
     * @return timestamp field
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Size(max = 255)
    public String getTimestampField() {
        return timestampField;
    }

    /**
     * Set field containing HPacket timestamp
     *
     * @param timestampField packet timestamp field
     */
    public void setTimestampField(String timestampField) {
        this.timestampField = timestampField;
    }

    /**
     * Get HPacket timestamp format
     *
     * @return timestamp format
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Size(max = 255)
    public String getTimestampFormat() {
        return timestampFormat;
    }

    /**
     * Set HPacket timestamp format
     *
     * @param timestampFormat packet timestamp format
     */
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = timestampFormat;
    }

    /**
     * @return true if packet has unix timestamp format
     */
    @Column(columnDefinition = "BOOLEAN default true")
    public boolean isUnixTimestamp() {
        return unixTimestamp;
    }

    /**
     * @param unixTimestamp true if is unix timestamp format
     */
    public void setUnixTimestamp(boolean unixTimestamp) {
        this.unixTimestamp = unixTimestamp;
    }

    /**
     * @return true if unix timestamp is in seconds
     */
    @Column(columnDefinition = "BOOLEAN default false")
    public boolean isUnixTimestampFormatSeconds() {
        return unixTimestampFormatSeconds;
    }

    /**
     * @param unixTimestampFormatSeconds true if unix timestamp is in seconds
     */
    public void setUnixTimestampFormatSeconds(boolean unixTimestampFormatSeconds) {
        this.unixTimestampFormatSeconds = unixTimestampFormatSeconds;
    }

    /**
     * Converts the current hpacket timestamp into a valid date string
     *
     * @param dateFormatOutput
     * @return
     */
    @Transient
    @JsonIgnore
    public String getTimestampValueAsString(String dateFormatOutput) {
        Optional<HPacketField> timestampFieldOpt = this.getFields().stream().filter(packetField -> packetField.getName().equalsIgnoreCase(this.getTimestampField())).findAny();
        if (timestampFieldOpt.isPresent()) {
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormatOutput);
            HPacketField timestampField = timestampFieldOpt.get();
            try {
                //converting timestamp into readable date
                String dateStr = "";
                long timestampMillis;

                if (this.isUnixTimestamp()) {
                    Long unixTimestamp = (Long) timestampField.getValue();
                    timestampMillis = this.isUnixTimestampFormatSeconds() ? unixTimestamp * 1000 : unixTimestamp;
                } else {
                    DateFormat dateFormat = new SimpleDateFormat(this.getTimestampFormat());
                    timestampMillis = dateFormat.parse(timestampField.getValue().toString()).getTime();
                }
                dateStr = sdf.format(new Date(timestampMillis));
                return dateStr;
            } catch (Exception e) {
                LoggerFactory.getLogger(HPacket.class).error("Error while converting timestamp {}", this.getTimestampField());
            }
        }
        return "no-timestamp";
    }

    /**
     * Converts the current hpacket timestamp into a valid date string
     *
     * @return
     */
    @Transient
    @JsonIgnore
    public String getTimestampValueAsString() {
        return getTimestampValueAsString("yyyy-MM-dd HH:mm:SS Z");
    }

    /**
     * @return packet traffic plan
     */
    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public HPacketTrafficPlan getTrafficPlan() {
        return trafficPlan;
    }

    /**
     * @param trafficPlan packet traffic plan
     */
    public void setTrafficPlan(HPacketTrafficPlan trafficPlan) {
        this.trafficPlan = trafficPlan;
    }

    /**
     * Removes a packet field regardless its position inside fields hierarchy
     *
     * @param toRemove
     */
    public void removeField(HPacketField toRemove) {
        removeHPacketFieldFromInnerFields(getFields(), toRemove);
    }

    private boolean removeHPacketFieldFromInnerFields(Set<HPacketField> fields, HPacketField toRemove) {
        if (fields.contains(toRemove)) {
            fields.remove(toRemove);
            toRemove.setPacket(null);
            toRemove.setParentField(null);
            return true;
        }
        for (HPacketField innerField : fields) {
            boolean removed = removeHPacketFieldFromInnerFields(innerField.getInnerFields(), toRemove);
            if (removed)
                return true;
        }
        return false;
    }

    /**
     * Reload fields map
     */
    @Transient
    @JsonIgnore
    public HashMap<String, HPacketField> getFieldsMap() {
        //TODO find more effiecient way, no post load because it breaks the hpacket field loading
        this.reloadFields(true);
        return fieldsMap;
    }

    /**
     * Recalculates fields map based on field values
     */
    @Transient
    @JsonIgnore
    public void defineFields(List<HPacketField> fields) {
        this.setFields(new HashSet<>(fields));
        this.reloadFields(true);
    }

    /**
     * Recalculates fieldsMap
     */
    private void reloadFields(boolean forceRecalculation) {
        if (fieldsMap == null || forceRecalculation) {
            fieldsMap = new HashMap<>();
            for (HPacketField field : fields)
                getInnerFields(field, field.getName(), fieldsMap);
        }
    }

    /**
     * Used to construct raw json of hpacket
     *
     * @return Return flat map of fields
     */
    @Transient
    @JsonIgnore
    public HashMap<String, Object> getFlatFieldsMapWithValues() {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        this.getFieldsMap().forEach((k, f) -> {
            if (f.getType() == HPacketFieldType.OBJECT) {
                HashMap<String, Object> innerFieldData = getFlatInnerFieldsValues(f);
                fields.put(k, innerFieldData);
            } else {
                fields.put(k, f.getValue());
            }
        });
        return fields;
    }

    /**
     * Used to construct raw json of hpacket
     *
     * @return Return flat map of fields
     */
    @Transient
    @JsonIgnore
    private HashMap<String, Object> getFlatInnerFieldsValues(HPacketField field) {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        field.getInnerFields().forEach((k) -> {
            if (k.getType() == HPacketFieldType.OBJECT) {
                HashMap<String, Object> innerFieldData = getFlatInnerFieldsValues(k);
                fields.put(k.getName(), innerFieldData);
            } else {
                fields.put(k.getName(), k.getValue());
            }
        });
        return fields;
    }

    /**
     * @return Return flat map of fields
     */
    @Transient
    @JsonIgnore
    public HashMap<String, Object> getFlatFieldsMap() {
        HashMap<String, Object> fields = new HashMap<String, Object>();
        this.getFieldsMap().forEach((k, f) -> {
            HashMap<String, Object> innerFieldData = new HashMap<>();
            if (f.getType() != HPacketFieldType.OBJECT) {
                innerFieldData.put("id", f.getId());
                innerFieldData.put("type", f.getType().toString().toLowerCase());
                fields.put(k, innerFieldData);
            }
        });
        return fields;
    }

    /**
     * @param hPacketField Leaf or parent field
     * @param path         key or part of another one to put into inner field flat map
     * @param fieldsMap    inner field flat map
     */
    private void getInnerFields(HPacketField hPacketField, String path, HashMap<String, HPacketField> fieldsMap) {
        if (hPacketField.getInnerFields() == null || hPacketField.getInnerFields().size() == 0)
            fieldsMap.put(path, hPacketField);
        else
            for (HPacketField innerField : hPacketField.getInnerFields())
                getInnerFields(innerField, path + "." + innerField.getName(), fieldsMap);
    }

    @Transient
    public Object getFieldValue(String fieldPath) {
        fieldPath = fieldPath.trim().replace("packet.", "");
        HPacketField field = null;
        try {
            long hPacketFieldId = Long.parseLong(fieldPath);
            // get hPacketField having its id
            field = this.getFieldsMap().values().stream()
                    .filter(hPacketField -> hPacketField.getId() == hPacketFieldId)
                    .findFirst()
                    .orElse(null);

        } catch (NumberFormatException e) {
            // retro-compatibility: get hPacketField from its value
            field = this.getFieldsMap().get(fieldPath);
        }
        if (field != null)
            return field.getFieldValue();
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((device == null) ? 0 : (new Long(device.getId())).hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HPacket other = (HPacket) obj;
        if (other.getId() > 0 && this.getId() > 0)
            return other.getId() == this.getId();

        if (device == null) {
            if (other.device != null)
                return false;
        } else if (!device.equals(other.device))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    @Transient
    @JsonIgnore
    public HyperIoTBaseEntity getParent() {
        return this.device;
    }

    /**
     * From flat field map, get field list
     *
     * @param packet       Packet which get field list for
     * @param fields       List to built
     * @param hPacketField HPacketField leaf, from flat map
     * @param current      String path containing HPacketField and its inner fields
     */
    private void addField(HPacket packet, Set<HPacketField> fields, HPacketField hPacketField, String current) {
        String child = null;
        int dotIndex = current.indexOf(".");
        if (dotIndex > 0)
            child = current.substring(dotIndex + 1);
        //currentField it's a parent field
        if (child != null) {
            HPacketField parentField = fields.stream().filter(f -> f.getName().equals(current.substring(0, dotIndex))).findFirst().orElse(null);
            //it's the first time I've got it
            if (parentField == null) {
                parentField = new HPacketField();
                parentField.setName(current.substring(0, dotIndex));
                parentField.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
                parentField.setType(HPacketFieldType.OBJECT);
                Set<HPacketField> innerFields = new HashSet<>();
                parentField.setInnerFields(innerFields);
            }
            //I've already got it
            addField(packet, parentField.getInnerFields(), hPacketField, child);
            fields.add(parentField);
        } else
            fields.add(hPacketField);
    }

    /* AVRO GenericRecord Interface */
    @Transient
    private int getKeyIndex(String key) {
        int i = -1;
        switch (key) {
            case "name":
                i = 0;
                break;
            case "type":
                i = 1;
                break;
            case "format":
                i = 2;
                break;
            case "serialization":
                i = 3;
                break;
            case "device":
                i = 4;
                break;
            case "version":
                i = 5;
                break;
            case "fields":
                i = 6;
                break;
            case "valid":
                i = 7;
                break;
            case "id":
                i = 8;
                break;
            case "categoryIds":
                i = 9;
                break;
            case "tagIds":
                i = 10;
                break;
            case "timestampField":
                i = 11;
                break;
            case "timestampFormat":
                i = 12;
                break;
            case "trafficPlan":
                i = 13;
                break;
            case "unixTimestamp":
                i = 14;
                break;
            case "unixTimestampFormatSeconds":
                i = 15;
                break;
        }
        return i;
    }

    @Override
    public void put(String key, Object v) {
        put(getKeyIndex(key), v);
    }

    @Override
    @Transient
    public Object get(String key) {
        return get(getKeyIndex(key));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void put(int i, Object v) {
        if (v == null)
            return;
        switch (i) {
            case 0:
                name = v.toString();
                break;
            case 1:
                type = HPacketType.valueOf(v.toString());
                break;
            case 2:
                format = HPacketFormat.valueOf(v.toString());
                break;
            case 3:
                serialization = HPacketSerialization.valueOf(v.toString());
                break;
            case 4:
                if ((long) v > 0) {
                    device = new HDevice();
                    device.setId((long) v);
                }
                break;
            case 5:
                version = v.toString();
                break;
            case 6:
                //from flat field map, build field list
                Set<HPacketField> packetFields = new HashSet<>();
                HashMap<Utf8, Object> incomingFields = null;
                //noinspection unchecked: it's Avro Map type
                incomingFields = (HashMap<Utf8, Object>) v;
                for (Utf8 key : incomingFields.keySet())
                    addField(this, packetFields, (HPacketField) incomingFields.get(key), key.toString());
                setFields(new HashSet<>(packetFields));
                break;
            case 7:
                valid = (boolean) v;
                break;
            case 8:
                setId((long) v);
                break;
            case 9:
                Object[] vals = ((GenericData.Array) v).toArray();
                long[] cats = new long[vals.length];
                for (int c = 0; c < vals.length; c++) {
                    cats[c] = (long) vals[c];
                }
                setCategoryIds(cats);
                break;
            case 10:
                Object[] tvals = ((GenericData.Array) v).toArray();
                long[] tags = new long[tvals.length];
                for (int c = 0; c < tvals.length; c++) {
                    tags[c] = (long) tvals[c];
                }
                setTagIds(tags);
                break;
            case 11:
                timestampField = v.toString();
                break;
            case 12:
                timestampFormat = v.toString();
                break;
            case 13:
                trafficPlan = HPacketTrafficPlan.valueOf(v.toString());
                break;
            case 14:
                unixTimestamp = (boolean) v;
                break;
            case 15:
                unixTimestampFormatSeconds = (boolean) v;
                break;
            default:
                throw new AvroRuntimeException("Unknown field index " + i);
        }
    }

    @Override
    @Transient
    public Object get(int i) {
        switch (i) {
            case 0:
                return name;
            case 1:
                return type.getName().toUpperCase();
            case 2:
                return format == null ? null : format.getName().toUpperCase();
            case 3:
                return serialization == null ? null : serialization.getName().toUpperCase();
            case 4:
                return getDevice() != null ? getDevice().getId() : 0L;
            case 5:
                return version;
            case 6:
                return getFieldsMap();
            case 7:
                return valid;
            case 8:
                return getId();
            case 9:
                return Arrays.stream(getCategoryIds()).boxed().collect(Collectors.toList());
            case 10:
                return Arrays.stream(getTagIds()).boxed().collect(Collectors.toList());
            case 11:
                return timestampField;
            case 12:
                return timestampFormat;
            case 13:
                return trafficPlan.getName().toUpperCase();
            case 14:
                return unixTimestamp;
            case 15:
                return unixTimestampFormatSeconds;
            default:
                throw new AvroRuntimeException("Unknown field index " + i);
        }
    }

    @SuppressWarnings("resource")
    @Override
    @Transient
    @JsonIgnore
    public Schema getSchema() {
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(getJsonSchema());
    }

    @Transient
    @JsonIgnore
    public String getJsonSchema() {
        String hPacketSchema = null;
        String hPacketFieldSchema = null;
        hPacketSchema = new Scanner(HPacketField.class.getClassLoader().getResourceAsStream("HPacket.avsc"), "UTF-8").useDelimiter("\\A").next();
        hPacketFieldSchema = new Scanner(HPacketField.class.getClassLoader().getResourceAsStream("HPacketField.avsc"), "UTF-8").useDelimiter("\\A").next();
        // manual replace of "it.acsoftware.hyperiot.hpacket.model.HPacketField" with its schema
        hPacketSchema = hPacketSchema.replace(HPacketField.class.getName(), hPacketFieldSchema);
        return hPacketSchema;
    }

}

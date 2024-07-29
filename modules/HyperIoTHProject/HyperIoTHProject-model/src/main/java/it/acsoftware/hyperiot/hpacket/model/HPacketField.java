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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hproject.model.HProjectJSONView;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Aristide Cittadino Entity which maps the concept of packet field.
 */
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"packet_id", "parentField_id", "name"})})
public class HPacketField extends HyperIoTAbstractEntity implements GenericRecord {

    protected Logger log = LoggerFactory.getLogger(HPacketField.class.getName());

    /**
     * Field name, unique for each packet
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class,HProjectJSONView.Export.class})
    private String name;
    /**
     * Field description
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class,HProjectJSONView.Export.class})
    private String description;
    /**
     * Field Type, es. integer,float,...
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class,HProjectJSONView.Export.class})
    private HPacketFieldType type;
    /**
     * Field multiplicity single,array or matrix
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class,HProjectJSONView.Export.class})
    private HPacketFieldMultiplicity multiplicity;
    /**
     * Field value measurement unit
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Extended.class,HProjectJSONView.Export.class})
    private String unit;
    /**
     * Packet the fiels is related to
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private HPacket packet;

    /**
     * Parent PacketField, used for composite objects
     */
    private HPacketField parentField;

    /**
     * Inner fields
     */
    private Set<HPacketField> innerFields;

    /**
     *
     */
    private HashMap<String, HPacketField> innerFieldsMap;

    /**
     * Not persisted
     */
    private Object value;

    /**
     * @return field name
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Size( max = 255)
    public String getName() {
        return name;
    }
    /**
     * @param name field name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return field description
     */
    @NoMalitiusCode
    @Size( max = 255)
    public String getDescription() {
        return description;
    }
    /**
     * @param description field description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return field type
     */
    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public HPacketFieldType getType() {
        return type;
    }

    /**
     * @param type field type
     */
    public void setType(HPacketFieldType type) {
        this.type = type;
    }

    /**
     * @return multiplicity
     */
    @NotNullOnPersist
    @Enumerated(EnumType.STRING)
    public HPacketFieldMultiplicity getMultiplicity() {
        return multiplicity;
    }

    /**
     * @param multiplicity multiplicity
     */
    public void setMultiplicity(HPacketFieldMultiplicity multiplicity) {
        this.multiplicity = multiplicity;
    }

    /**
     * @return unit
     */
    @NoMalitiusCode
    @Size( max = 255)
    public String getUnit() {
        return unit;
    }
    /**
     * @param unit unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the related packet
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HPacket.class)
    public HPacket getPacket() {
        return packet;
    }
    /**
     * @param packet the related packet
     */
    public void setPacket(HPacket packet) {
        this.packet = packet;
    }


    @ManyToOne(targetEntity = HPacketField.class)
    @JsonBackReference
    public HPacketField getParentField() {
        return parentField;
    }

    public void setParentField(HPacketField parentField) {
        this.parentField = parentField;
    }

    // Eager because it needs inner fields info immediately
    @OneToMany(mappedBy = "parentField", fetch = FetchType.EAGER, cascade = {CascadeType.ALL},orphanRemoval = true)
    public Set<HPacketField> getInnerFields() {
        return innerFields;
    }

    public void setInnerFields(Set<HPacketField> innerFields) {
        this.innerFields = innerFields;
    }

    @Transient
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Transient
    private HashMap<String, HPacketField> getInnerFieldsMap() {
        if (innerFieldsMap == null) {
            innerFieldsMap = new HashMap<>();

            for (HPacketField field : innerFields) {
                innerFieldsMap.put(field.getName(), field);
            }
        }
        return innerFieldsMap;

    }

    @Transient
    @JsonIgnore
    public Object getFieldValue() {
        Object value = getValue();
        if (multiplicity != HPacketFieldMultiplicity.SINGLE) {
            return value;
        }
        if (value instanceof Utf8) {
            // handle Avro strings on retrieval from HBase
            Utf8 utf8 = (Utf8) value;
            return utf8.toString();
        }
        return this.getType().getClassType().cast(value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parentField == null) ? 0 : parentField.hashCode());
        result = prime * result + ((packet == null) ? 0 : (new Long(packet.getId())).hashCode());
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
        HPacketField other = (HPacketField) obj;
        if (other.getId() > 0 && this.getId() > 0)
            return other.getId() == this.getId();

        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (packet == null) {
            if (other.packet != null)
                return false;
        } else if (!packet.equals(other.packet))
            return false;
        if (parentField == null) {
            if (other.parentField != null)
                return false;
        } else if (!parentField.equals(other.parentField))
            return false;
        return true;
    }

    @Transient
    private int getKeyIndex(String key) {
        int i = -1;
        switch (key) {
            case "name":
                i = 0;
                break;
            case "description":
                i = 1;
                break;
            case "type":
                i = 2;
                break;
            case "multiplicity":
                i = 3;
                break;
            case "packet":
                i = 4;
                break;
            case "value":
                i = 5;
                break;
            case "id":
                i = 6;
                break;
            case "categoryIds":
                i = 7;
                break;
            case "tagIds":
                i = 8;
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
                description = v.toString();
                break;
            case 2:
                type = HPacketFieldType.valueOf(v.toString());
                break;
            case 3:
                multiplicity = HPacketFieldMultiplicity.valueOf(v.toString());
                break;
            case 4:
                if ((long) v > 0) {
                    packet = new HPacket();
                    packet.setId((long) v);
                }
                break;
            case 5:
                value = v;
                break;
            case 6:
                setId((long) v);
                break;
            case 7:
                Object[] vals = ((GenericData.Array) v).toArray();
                long[] cats = new long[vals.length];
                for (int c = 0; c < vals.length; c++) {
                    cats[c] = (long) vals[c];
                }
                setCategoryIds(cats);
                break;
            case 8:
                Object[] tvals = ((GenericData.Array) v).toArray();
                long[] tags = new long[tvals.length];
                for (int c = 0; c < tvals.length; c++) {
                    tags[c] = (long) tvals[c];
                }
                setTagIds(tags);
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
                return description;
            case 2:
                return type.name();
            case 3:
                return multiplicity.getName().toUpperCase();
            case 4:
                return packet != null ? packet.getId() : 0L;
            case 5:
                return getFieldValue();
            case 6:
                return getId();
            case 7:
                return Arrays.stream(getCategoryIds()).boxed().collect(Collectors.toList());
            case 8:
                return Arrays.stream(getTagIds()).boxed().collect(Collectors.toList());
            default:
                throw new AvroRuntimeException("Unknown field index " + i);
        }
    }

    @Override
    @Transient
    @JsonIgnore
    public Schema getSchema() {
        Schema.Parser parser = new Schema.Parser();
        try {
            return parser.parse(
                    HPacketField.class.getClassLoader().getResourceAsStream("HPacketField.avsc"));
        } catch (IOException e) {
            log.error( e.getMessage(), e);
        }
        return null;
    }

}

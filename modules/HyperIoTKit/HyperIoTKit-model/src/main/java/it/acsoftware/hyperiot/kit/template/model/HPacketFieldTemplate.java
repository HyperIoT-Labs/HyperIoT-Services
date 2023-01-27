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

package it.acsoftware.hyperiot.kit.template.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldMultiplicity;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"packet_id", "parentField_id", "name"})})
public class HPacketFieldTemplate extends HyperIoTBaseEntityTemplate {

    /**
     * Field name, unique for each packet
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private String name;
    /**
     * Field description
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private String description;
    /**
     * Field Type, es. integer,float,...
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private HPacketFieldType type;
    /**
     * Field multiplicity single,array or matrix
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private HPacketFieldMultiplicity multiplicity;
    /**
     * Field value measurement unit
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private String unit;

    /**
     * Packet the fiels is related to
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private HPacketTemplate packet;

    /**
     * Parent PacketField, used for composite objects
     */
    private HPacketFieldTemplate parentField;

    /**
     * Inner fields
     */
    private Set<HPacketFieldTemplate> innerFields;


    /**
     * @return field name
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
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
    @ManyToOne(targetEntity = HPacketTemplate.class)
    public HPacketTemplate getPacket() {
        return packet;
    }
    /**
     * @param packet the related packet
     */
    public void setPacket(HPacketTemplate packet) {
        this.packet = packet;
    }


    @ManyToOne(targetEntity = HPacketFieldTemplate.class)
    @JsonBackReference
    public HPacketFieldTemplate getParentField() {
        return parentField;
    }

    public void setParentField(HPacketFieldTemplate parentField) {
        this.parentField = parentField;
    }

    // Eager because it needs inner fields info immediately
    @OneToMany(mappedBy = "parentField", fetch = FetchType.EAGER, cascade = {CascadeType.REMOVE,CascadeType.PERSIST})
    public Set<HPacketFieldTemplate> getInnerFields() {
        return innerFields;
    }

    public void setInnerFields(Set<HPacketFieldTemplate> innerFields) {
        this.innerFields = innerFields;
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
        HPacketFieldTemplate other = (HPacketFieldTemplate) obj;
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









}

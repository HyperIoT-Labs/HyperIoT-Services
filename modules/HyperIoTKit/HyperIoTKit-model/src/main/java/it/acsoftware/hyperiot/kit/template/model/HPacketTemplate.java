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

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hpacket.model.HPacketSerialization;
import it.acsoftware.hyperiot.hpacket.model.HPacketTrafficPlan;
import it.acsoftware.hyperiot.hpacket.model.HPacketType;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.List;

import static javax.persistence.CascadeType.*;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "device_id", "version"})})
public class HPacketTemplate extends HyperIoTBaseEntityTemplate {


    /**
     * Packet template name, used to identify it
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private String name;
    /**
     * Packet template type, indicates if it is input,output or both
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private HPacketType type;

    /**
     * Transmission template format es. json,xml,csv
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private HPacketFormat format;
    /**
     * Type of serialization, es. none or avro
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private HPacketSerialization serialization;

    /**
     * The related device template which sends the packet
     */
    @JsonView({HyperIoTJSONView.Public.class})
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    private HDeviceTemplate device;

    /**
     * Packet template template version
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private String version;

    /**
     * Packet template timestamp field
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private String timestampField;

    /**
     * Packet template timestamp format
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private String timestampFormat;

    /**
     * true if packet is sent with unix timestamp
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private boolean unixTimestamp;


    /**
     * true if packet has seconds format in timestamp value not milliseconds
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private boolean unixTimestampFormatSeconds;


    /**
     * Packet traffic plan: it indicates how many bytes per day are sent for its
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private HPacketTrafficPlan trafficPlan;

    /**
     * Packet fields template
     */
    @JsonView({HyperIoTJSONView.Public.class})
    private List<HPacketFieldTemplate> fields;

    /**
     * @return packet name
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
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
     * Get the bound device.
     *
     * @return the related device
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HDeviceTemplate.class)
    public HDeviceTemplate getDevice() {
        return device;
    }

    /**
     * Set the bound device.
     *
     * @param device HDevice
     */
    public void setDevice(HDeviceTemplate device) {
        this.device = device;
    }

    /**
     * Get packet's fields.
     *
     * @return packet fields
     */
    @OneToMany(targetEntity = HPacketFieldTemplate.class, cascade = {REMOVE, REFRESH,PERSIST}, mappedBy = "packet", fetch = FetchType.EAGER, orphanRemoval = true)
    public List<HPacketFieldTemplate> getFields() {
        return fields;
    }

    /**
     * Set packet's fields.
     *
     * @param fields HPacket field list
     */
    public void setFields(List<HPacketFieldTemplate> fields) {
        this.fields = fields;
    }


    /**
     * Get field containing HPacket timestamp
     *
     * @return timestamp field
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
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
     * @return packet traffic plan
     */
    @NotNullOnPersist
    public HPacketTrafficPlan getTrafficPlan() {
        return trafficPlan;
    }

    /**
     * @param trafficPlan packet traffic plan
     */
    public void setTrafficPlan(HPacketTrafficPlan trafficPlan) {
        this.trafficPlan = trafficPlan;
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
        HPacketTemplate other = (HPacketTemplate) obj;
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

}

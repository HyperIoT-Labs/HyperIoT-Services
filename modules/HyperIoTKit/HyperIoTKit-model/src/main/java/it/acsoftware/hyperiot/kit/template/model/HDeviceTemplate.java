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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.kit.model.Kit;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.List;


@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"deviceLabel","kit_id"})})
@Entity
public class HDeviceTemplate extends HyperIoTBaseEntityTemplate {

    /**
     * Field name used to label device template
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class})
    private String deviceLabel;


    /**
     * Device brand, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class})
    private String brand;
    /**
     * Device template model, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class})
    private String model;
    /**
     * Device template firmware version, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class})
    private String firmwareVersion;
    /**
     * Device template software version, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class})
    private String softwareVersion;

    @JsonView({HyperIoTJSONView.Internal.class, HyperIoTJSONView.Public.class})
    private List<HPacketTemplate> packets;

    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class})
    private String description;

    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    private Kit kit;

    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
    }
    @NoMalitiusCode
    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    @NoMalitiusCode
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @NoMalitiusCode
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    @NotNullOnPersist
    @ManyToOne(targetEntity = Kit.class)
    public Kit getKit() {
        return kit;
    }

    public void setKit(Kit kit) {
        this.kit = kit;
    }

    @OneToMany(mappedBy = "device", cascade = {CascadeType.REMOVE,CascadeType.PERSIST}, targetEntity = HPacketTemplate.class, fetch = FetchType.EAGER)
    @JsonIgnore
    public List<HPacketTemplate> getPackets() {
        return packets;
    }

    public void setPackets(List<HPacketTemplate> packets) {
        this.packets = packets;
    }

    @NoMalitiusCode
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    @NoMalitiusCode
    @Length(max = 80)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HDeviceTemplate other = (HDeviceTemplate) obj;

        if (other.getId() > 0 && this.getId() > 0)
            return other.getId() == this.getId();

        if (deviceLabel == null) {
            if (other.deviceLabel != null)
                return false;
        }
        else if (!deviceLabel.equals(other.deviceLabel)) {
            return false;
        }
        if (kit == null) {
            if (other.kit != null)
                return false;
        }
        else if (!kit.equals(other.kit)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deviceLabel == null) ? 0 : deviceLabel.hashCode());
        result = prime * result + ((kit == null) ? 0 : (new Long(kit.getId())).hashCode());
        return result;
    }
}

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

package it.acsoftware.hyperiot.hdevice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedChildResource;
import it.acsoftware.hyperiot.base.api.HyperIoTRole;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.*;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.model.HProjectJSONView;
import org.apache.avro.reflect.AvroIgnore;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.*;

/**
 * @author Aristide Cittadino Model class for HDevice of HyperIoT platform. This
 * class is used to map HDevice with the database.
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"deviceName"})})
@PasswordMustMatch
@ValidPassword
public class HDevice extends HyperIoTAbstractEntity
        implements HyperIoTProtectedEntity, HyperIoTAuthenticable, HyperIoTOwnedChildResource {
    /**
     * Device name used to login in case the device can connect to the network
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class,HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String deviceName;

    /**
     * Device password, stored has MD5
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @AvroIgnore
    private String password;

    /**
     * Device confirm password
     */
    @AvroIgnore
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordConfirm;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordResetCode;

    /**
     * Device brand, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class,HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String brand;
    /**
     * Device model, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class,HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String model;
    /**
     * Device firmware version, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class,HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String firmwareVersion;
    /**
     * Device software version, not required
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class,HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String softwareVersion;
    /**
     * Device general description
     */
    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class,HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String description;
    /**
     * The HProject the device belongs to
     */

    @JsonView({HyperIoTJSONView.Public.class, HyperIoTJSONView.Compact.class})
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    @AvroIgnore
    private HProject project;

    /**
     * List of HPacket related to this device
     */
    @JsonView({HyperIoTJSONView.Internal.class, HyperIoTJSONView.Extended.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private Set<HPacket> packets;

    /**
     * Technical user, this flag is always forced to be false from outside
     */
    @JsonView(HyperIoTJSONView.Internal.class)
    @AvroIgnore
    private boolean admin;

    @JsonView({HyperIoTJSONView.Public.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private boolean loginWithSSLCert;

    @JsonView({HyperIoTJSONView.Internal.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private byte[] pubKey;

    //Not saved , returned to the user at the moment of the creation
    @JsonView({HyperIoTJSONView.Public.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String x509Cert;

    //Not saved , returned to the user at the moment of the creation
    @JsonView({HyperIoTJSONView.Public.class, HProjectJSONView.Export.class})
    @AvroIgnore
    private String x509CertKey;


    @AvroIgnore
    protected List<AreaDevice> areaDevices = new ArrayList<>();

    /**
     * @return the device name
     */
    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9]+$")
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * @param deviceName the device name
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    /**
     * @return the encrypted password
     * Password not null is checked by @ValidPassword on HDevice Class
     */
    @NoMalitiusCode
    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the encrypted password
     */
    @Transient
    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String password) {
        this.passwordConfirm = password;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getPasswordResetCode() {
        return passwordResetCode;
    }

    public void setPasswordResetCode(String passwordResetCode) {
        this.passwordResetCode = passwordResetCode;
    }

    /**
     * @return the device brand
     */
    @NoMalitiusCode
    @Size(max = 255)
    public String getBrand() {
        return brand;
    }

    /**
     * @param brand the device brand to be set
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /**
     * @return the device model
     */
    @NoMalitiusCode
    @Size(max = 255)
    public String getModel() {
        return model;
    }

    /**
     * @param model the device model
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * @return the firmware version
     */
    @NoMalitiusCode
    @Size(max = 255)
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * @param firmwareVersion the firmware version
     */
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * @return the software version
     */
    @NoMalitiusCode
    @Size(max = 255)
    public String getSoftwareVersion() {
        return softwareVersion;
    }

    /**
     * @param softwareVersion the software version
     */
    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    /**
     * @return the device description
     */
    @NoMalitiusCode
    @Column(length = 3000)
    @Size(max = 3000)
    public String getDescription() {
        return description;
    }

    /**
     * @param description the device description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the related HProject
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HProject.class)
    public HProject getProject() {
        return project;
    }

    /**
     * @param project the related HProject
     */
    public void setProject(HProject project) {
        this.project = project;
    }

    /**
     * @return HPacket list
     */
    @OneToMany(mappedBy = "device", cascade = {CascadeType.REMOVE, CascadeType.PERSIST}, targetEntity = HPacket.class, fetch = FetchType.EAGER)
    @JsonIgnore
    @Fetch(FetchMode.SUBSELECT)
    public Set<HPacket> getPackets() {
        return packets;
    }

    public void setPackets(Set<HPacket> packets) {
        this.packets = packets;
    }

    @Override
    @Transient
    @JsonIgnore
    public HyperIoTBaseEntity getParent() {
        return this.project;
    }

    /**
     * @return empty role list, at now Devices do not have roles
     */
    @Override
    @Transient
    @JsonIgnore
    public Collection<? extends HyperIoTRole> getRoles() {
        return new HashSet<>();
    }

    /**
     * @return deviceName as screenname for login
     */
    @Override
    @Transient
    @ApiModelProperty(hidden = true)
    @JsonIgnore
    public String getScreenName() {
        return this.getDeviceName();
    }

    @Transient
    @ApiModelProperty(hidden = true)
    @JsonView(HyperIoTJSONView.Internal.class)
    @Override
    public String getScreenNameFieldName() {
        return "deviceName";
    }

    /**
     * @return true for technical users
     */
    @Override
    public boolean isAdmin() {
        return admin;
    }

    /**
     * @param admin
     */
    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    /**
     * @return true if ssl cert login is enabled
     */
    @Transient
    public boolean isLoginWithSSLCert() {
        return loginWithSSLCert;
    }

    /**
     * @param loginWithSSLCert
     */
    public void setLoginWithSSLCert(boolean loginWithSSLCert) {
        this.loginWithSSLCert = loginWithSSLCert;
    }

    /**
     * @return public key of the device
     */
    public byte[] getPubKey() {
        return pubKey;
    }

    /**
     * @param pubKey
     */
    public void setPubKey(byte[] pubKey) {
        this.pubKey = pubKey;
    }

    /**
     * @return
     */
    @Transient
    public String getX509Cert() {
        return x509Cert;
    }

    /**
     * @param x509Cert
     */
    public void setX509Cert(String x509Cert) {
        this.x509Cert = x509Cert;
    }


    @Transient
    public String getX509CertKey() {
        return x509CertKey;
    }

    public void setX509CertKey(String x509CertKey) {
        this.x509CertKey = x509CertKey;
    }

    /**
     * @return always true, devices are always automatically activated by default
     */
    @Transient
    @Override
    @JsonIgnore
    public boolean isActive() {
        return true;
    }

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public List<AreaDevice> getAreaDevices() {
        return areaDevices;
    }

    public void setAreaDevices(List<AreaDevice> areaDevices) {
        this.areaDevices = areaDevices;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((deviceName == null) ? 0 : deviceName.hashCode());
        result = prime * result + ((project == null) ? 0 : new Long(project.getId()).hashCode());
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

        HDevice other = (HDevice) obj;

        if (other.getId() > 0 && this.getId() > 0)
            return other.getId() == this.getId();

        if (deviceName == null) {
            if (other.deviceName != null)
                return false;
        } else if (!deviceName.equals(other.deviceName))
            return false;

        if (project == null) {
            if (other.project != null)
                return false;
        } else if (!project.equals(other.project))
            return false;
        return true;
    }

}

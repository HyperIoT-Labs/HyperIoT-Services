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

package it.acsoftware.hyperiot.kit.model;

import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.kit.template.model.HDeviceTemplate;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Objects;

import static javax.persistence.CascadeType.*;

/**
 * 
 * @author Aristide Cittadino Model class for Kit of HyperIoT platform. This
 *         class is used to map Kit with the database.
 *
 */

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"kitVersion","label","projectId"})})
public class Kit extends HyperIoTAbstractEntity{

    /*
    Kit with project id equals to 0 must be considerer System's kit.
    Kit with with project id greater than 0 are kit related to a specific project and are owned by the user.
     */
    @JsonView({HyperIoTJSONView.Extended.class,HyperIoTJSONView.Public.class,HyperIoTJSONView.Compact.class})
    private long projectId;

    @JsonView({HyperIoTJSONView.Extended.class,HyperIoTJSONView.Public.class,HyperIoTJSONView.Compact.class})
    private String label;

    @JsonView({HyperIoTJSONView.Extended.class,HyperIoTJSONView.Public.class,HyperIoTJSONView.Compact.class})
    private String kitVersion;


    @JsonView({HyperIoTJSONView.Internal.class, HyperIoTJSONView.Public.class})
    private List<HDeviceTemplate> devices;

    @OneToMany(targetEntity = HDeviceTemplate.class, cascade = {REMOVE, REFRESH,PERSIST}, mappedBy = "kit", fetch = FetchType.EAGER, orphanRemoval = true)
    public List<HDeviceTemplate> getDevices() {
        return devices;
    }

    public void setDevices(List<HDeviceTemplate> devices) {
        this.devices = devices;
    }

    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @NotNullOnPersist
    @NotEmpty
    @NoMalitiusCode
    public String getKitVersion() {
        return kitVersion;
    }

    public void setKitVersion(String kitVersion) {
        this.kitVersion = kitVersion;
    }

    @Min(value=0, message="project id must be non negative number")
    public long getProjectId(){
        return projectId;
    }

    public void setProjectId(long projectId){
        this.projectId=projectId;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Kit other = (Kit) obj;
        if (other.getId() > 0 && this.getId() > 0) {
            return other.getId() == this.getId();
        }
        if (label == null) {
            if (other.label!= null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (kitVersion == null) {
            if (other.kitVersion!= null) {
                return false;
            }
        } else if (!kitVersion.equals(other.kitVersion)) {
            return false;
        }
        if(projectId != other.getProjectId()){
            return false;}
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((kitVersion == null) ? 0 : kitVersion.hashCode());
        result = prime * result + (Objects.hashCode(projectId));
        return result;
    }
}
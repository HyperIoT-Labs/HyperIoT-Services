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

package it.acsoftware.hyperiot.dashboard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedChildResource;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import it.acsoftware.hyperiot.hproject.model.HProject;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Collection;

/**
 * @author Aristide Cittadino Model class for Dashboard of HyperIoT platform.
 * This class is used to map Dashboard with the database.
 */

@Entity
public class Dashboard extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity, HyperIoTOwnedChildResource {

    /**
     * Dashboard name
     */
    private String name;
    /**
     *
     */
    private HProject hProject;
    /**
     * Dashboard type, i.e. REALTIME or OFFLINE
     */
    private DashboardType dashboardType;

    /**
     *
     */
    private Collection<DashboardWidget> widgets;

    /**
     *
     */
    private Area area;

    private long deviceId;

    /**
     * Get dashboard name
     *
     * @return dashboard name
     */
    @NoMalitiusCode
    @NotEmpty
    @NotNullOnPersist
    @Size( max = 255)
    public String getName() {
        return name;
    }

    /**
     * Set dashboard name
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return HProject instance
     *
     * @return hProject
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HProject.class)
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    public HProject getHProject() {
        return hProject;
    }

    /**
     * Set HProject
     *
     * @param hproject
     */
    public void setHProject(HProject hproject) {
        this.hProject = hproject;
    }

    /**
     * Get dashboard type
     *
     * @return dashboard type
     */
    @Enumerated(EnumType.STRING)
    public DashboardType getDashboardType() {
        return dashboardType;
    }

    /**
     * Set dashboard type
     *
     * @param dashboardType
     */
    public void setDashboardType(DashboardType dashboardType) {
        this.dashboardType = dashboardType;
    }

    @ManyToOne(targetEntity = Area.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    @Column(columnDefinition = "bigint DEFAULT 0")
    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    @OneToMany(mappedBy = "dashboard", targetEntity = DashboardWidget.class, cascade = CascadeType.REMOVE,fetch = FetchType.EAGER)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    public Collection<DashboardWidget> getWidgets() {
        return widgets;
    }

    public void setWidgets(Collection<DashboardWidget> widgets) {
        this.widgets = widgets;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dashboardType == null) ? 0 : dashboardType.hashCode());
        result = prime * result + ((hProject == null) ? 0 : hProject.hashCode());
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
        Dashboard other = (Dashboard) obj;
        if (this.getId() > 0 && other.getId() > 0)
            return this.getId() == other.getId();
        if (dashboardType != other.dashboardType)
            return false;
        if (hProject == null) {
            if (other.hProject != null)
                return false;
        } else if (!hProject.equals(other.hProject))
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
        return this.hProject;
    }

}

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

package it.acsoftware.hyperiot.dashboard.widget.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedChildResource;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;

/**
 * @author Aristide Cittadino Model class for DashboardWidget of HyperIoT
 * platform. This class is used to map DashboardWidget with the
 * database.
 */

@Entity
public class DashboardWidget extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity, HyperIoTOwnedChildResource {

    /**
     * String, in JSON format, containing widget configurations on dashboard
     */
    private String widgetConf;
    /**
     * Dashboard in which widget is located
     */
    private Dashboard dashboard;

    /**
     * Get widget configurations, such as position and data collected
     *
     * @return JSON, containing widget configurations
     */
    @NoMalitiusCode
    @NotNullOnPersist
    @NotEmpty
    @Column(columnDefinition = "TEXT")
    public String getWidgetConf() {
        return widgetConf;
    }

    /**
     * Set a new widget configuration
     *
     * @param widgetConf
     */
    public void setWidgetConf(String widgetConf) {
        this.widgetConf = widgetConf;
    }

    /**
     * Get the dashboard containing this widget
     *
     * @return dashboard
     */
    @ManyToOne(fetch = FetchType.EAGER,targetEntity = Dashboard.class)
    @NotNullOnPersist
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Dashboard getDashboard() {
        return dashboard;
    }

    /**
     * Set the dashboard in which locate the widget
     *
     * @param dashboard
     */
    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    @Transient
    @JsonIgnore
    public HyperIoTBaseEntity getParent() {
        return this.getDashboard();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dashboard == null) ? 0 : dashboard.hashCode());
        result = prime * result + ((widgetConf == null) ? 0 : widgetConf.hashCode());
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
        DashboardWidget other = (DashboardWidget) obj;
        if (this.getId() > 0 && other.getId() > 0)
            return this.getId() == other.getId();
        if (dashboard == null) {
            if (other.dashboard != null)
                return false;
        } else if (!dashboard.equals(other.dashboard))
            return false;
        if (widgetConf == null) {
            if (other.widgetConf != null)
                return false;
        } else if (!widgetConf.equals(other.widgetConf))
            return false;
        return true;
    }

}
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

package it.acsoftware.hyperiot.area.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.model.HProjectJSONView;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table
public class AreaDevice extends HyperIoTAbstractEntity
        implements HyperIoTProtectedEntity {

    private Area area;

    @JsonView({HProjectJSONView.Export.class,HyperIoTJSONView.Extended.class,HyperIoTJSONView.Public.class,HyperIoTJSONView.Compact.class})
    private HDevice device;

    @JsonView({HProjectJSONView.Export.class, HyperIoTJSONView.Extended.class,HyperIoTJSONView.Public.class,HyperIoTJSONView.Compact.class})
    private AreaMapInfo mapInfo;

    @ManyToOne(targetEntity = Area.class)
    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    /**
     * Get device
     * @return device
     */
    @NotNullOnPersist
    @ManyToOne(targetEntity = HDevice.class)
    public HDevice getDevice() {
        return device;
    }

    /**
     * Set device
     * @param device device
     */
    public void setDevice(HDevice device) {
        this.device = device;
    }

    /**
     * Gets area map info (eg. coordinates, icon and other map-related data)
     * @return map info
     */
    public AreaMapInfo getMapInfo() {
        return mapInfo;
    }

    /**
     * Sets area map info
     * @param mapInfo The AreaMapInfo object containing all map-related data
     */
    public void setMapInfo(AreaMapInfo mapInfo) {
        this.mapInfo = mapInfo;
    }

}

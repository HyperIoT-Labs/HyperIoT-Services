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

package it.acsoftware.hyperiot.area.api;

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;

import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Aristide Cittadino Interface component for AreaSystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface AreaSystemApi extends HyperIoTBaseEntitySystemApi<Area> {

    /**
     * Gets nested areas of the given area
     * @param area The area
     */
    void getAll(Area area);

    /**
     * Removes an area along with its nested areas
     *
     * @param area The area instance to remove
     */
    void removeAll(Area area);

    void removeByHProjectId(long hProjectId);

    /**
     * Get area path from area tree root to the given area node.
     *
     * @param area The area
     * @return Areas in the path.
     */
    Collection<Area> getAreaPath(Area area);

    /**
     * Gets an area device by id
     *
     * @param areaDeviceId The AreaDevice id
     * @return AreaDevice object
     */
    AreaDevice getAreaDevice(long areaDeviceId);

    /**
     * Adds or updates (if AreaDevice.id > 0) the area device
     *
     * @param area The Area instance
     * @param device The AreaDevice instance to persist
     */
    void saveAreaDevice(Area area, AreaDevice device);

    /**
     * Removes the area device with id `areaDeviceId` from the area with id `areaId`
     *
     * @param area The Area instance
     * @param areaDeviceId
     */
    void removeAreaDevice(Area area, long areaDeviceId);

    /**
     * @param area The Area instance
     * @return
     */
    Collection<AreaDevice> getAreaDevicesList(Area area);

    /**
     * Gets a list of devices belonging to the are with id `areaId` and its sub-areas
     *
     * @param area The Area instance
     * @return List of devices
     */
    Collection<AreaDevice> getAreaDevicesDeepList(Area area);

    /**
     *
     * @param projectId The id of the project
     * @return List of root area of the project. (An area is root if it hasn't parent)
     */
    List<Area> getRootProjectArea(long projectId);


    Collection<Area> getAreaListByProjectId(long projectId);

    /**
     * Updates the area entity preserving the previous saved image data.
     *
     * @param entity
     * @return
     */
    Area updateAndPreserveImageData(Area entity);

}

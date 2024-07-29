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
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseRepository;

import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Aristide Cittadino Interface component for Area Repository.
 *         It is used for CRUD operations,
 *         and to interact with the persistence layer.
 *
 */
public interface AreaRepository extends HyperIoTBaseRepository<Area> {

    Collection<AreaDevice> getAreaDevicesDeepList(long areaId);

    /**
     *
     * @param projectId The id of the project
     * @return List of root area of the project. (An area is root if it hasn't parent)
     */
    List<Area> getRootProjectArea(long projectId);


    Collection<Area> getAreaListByProjectId(long projectId);

    Collection<Area> getInnerArea(long areaId);

}

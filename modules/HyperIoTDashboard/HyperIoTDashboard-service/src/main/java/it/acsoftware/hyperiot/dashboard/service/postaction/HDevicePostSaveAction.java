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

package it.acsoftware.hyperiot.dashboard.service.postaction;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostSaveAction;
import it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = HyperIoTPostSaveAction.class, property = {"type=it.acsoftware.hyperiot.hdevice.model.HDevice"},immediate = true)
public class HDevicePostSaveAction<T extends HyperIoTBaseEntity> implements HyperIoTPostSaveAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HDevicePostSaveAction.class.getName());
    private DashboardSystemApi dashboardSystemApi;

    @Override
    public void execute(T entity) {
        LOGGER.debug("Create default online dashboard and offline dashboard automatically");
        HDevice hDevice = (HDevice) entity;
        try {
            this.dashboardSystemApi.createDeviceDashboard(hDevice);
        } catch (Exception e) {
            LOGGER.error( e.getMessage(), e);
        }
    }

    @Reference
    public void setDashboardSystemApi(DashboardSystemApi dashboardSystemApi) {
        this.dashboardSystemApi = dashboardSystemApi;
    }

}

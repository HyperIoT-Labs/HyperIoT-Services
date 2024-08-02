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

package it.acsoftware.hyperiot.dashboard.service;

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.dashboard.actions.HyperIoTDashboardAction;
import it.acsoftware.hyperiot.dashboard.api.DashboardRepository;
import it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.List;

/**
 * @author Aristide Cittadino Implementation class of the DashboardSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = DashboardSystemApi.class, immediate = true)
public final class DashboardSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Dashboard>
        implements DashboardSystemApi {

    /**
     * Injecting the DashboardRepository to interact with persistence layer
     */
    private DashboardRepository repository;

    /**
     * Injecting the PermissionSystemApi to interact with persistence layer
     */
    private PermissionSystemApi permissionSystemApi;

    /**
     * Constructor for a DashboardSystemServiceImpl
     */
    public DashboardSystemServiceImpl() {
        super(Dashboard.class);
    }

    /**
     * Return the current repository
     */
    protected DashboardRepository getRepository() {
        getLog().debug( "invoking getRepository, returning: {}" , this.repository);
        return repository;
    }

    /**
     * @param dashboardRepository The current value of DashboardRepository to
     *                            interact with persistence layer
     */
    @Reference
    protected void setRepository(DashboardRepository dashboardRepository) {
        getLog().debug( "invoking setRepository, setting: {}" , dashboardRepository);
        this.repository = dashboardRepository;
    }

    /**
     * @param permissionSystemApi Injecting via OSGi DS current PermissionSystemApi
     */
    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    /**
     * On Bundle activated
     */
    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExists();
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        String resourceName = Dashboard.class.getName();
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTDashboardAction.FIND_WIDGETS));
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    @Override
    public void createHProjectDashboard(HProject project) {
        this.repository.createHProjectDashboard(project);
    }

    @Override
    public void createAreaDashboard(Area area) {
        this.repository.createAreaDashboard(area);
    }

    @Override
    public void createDeviceDashboard(HDevice device) {
        this.repository.createDeviceDashboard(device);
    }

    @Override
    public void removeByAreaId(long areaId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("areaId", areaId);
        repository.executeUpdateQuery("delete from Dashboard dashboard where dashboard.area.id = :areaId", params);
    }

    @Override
    public void removeByHProjectId(long hProjectId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("hProjectId", hProjectId);
        repository.executeUpdateQuery("delete from Dashboard dashboard where dashboard.HProject.id = :hProjectId", params);
    }

    @Override
    public void removeByHDeviceId(long hDeviceId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("hDeviceId", hDeviceId);
        repository.executeUpdateQuery("delete from Dashboard dashboard where dashboard.deviceId = :hDeviceId", params);
    }

}

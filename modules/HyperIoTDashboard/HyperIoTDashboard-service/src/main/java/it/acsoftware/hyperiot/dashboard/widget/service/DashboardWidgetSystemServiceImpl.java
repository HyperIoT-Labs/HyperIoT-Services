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

package it.acsoftware.hyperiot.dashboard.widget.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl ;
import it.acsoftware.hyperiot.dashboard.widget.api.DashboardWidgetRepository;
import it.acsoftware.hyperiot.dashboard.widget.api.DashboardWidgetSystemApi;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;

/**
 *
 * @author Aristide Cittadino Implementation class of the DashboardWidgetSystemApi
 *         interface. This  class is used to implements all additional
 *         methods to interact with the persistence layer.
 */
@Component(service = DashboardWidgetSystemApi.class, immediate = true)
public final class DashboardWidgetSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<DashboardWidget>   implements DashboardWidgetSystemApi {

	/**
	 * Injecting the DashboardWidgetRepository to interact with persistence layer
	 */
	private DashboardWidgetRepository repository;

	/**
	 * Injecting the PermissionSystemApi to interact with persistence layer
	 */
	private PermissionSystemApi permissionSystemApi;

	/**
	 * Constructor for a DashboardWidgetSystemServiceImpl
	 */
	public DashboardWidgetSystemServiceImpl() {
		super(DashboardWidget.class);
	}

	/**
	 * Return the current repository
	 */
	protected DashboardWidgetRepository getRepository() {
		getLog().debug( "invoking getRepository, returning: " + this.repository);
		return repository;
	}

	/**
	 * @param dashboardWidgetRepository The current value of DashboardWidgetRepository to interact with persistence layer
	 */
	@Reference
	protected void setRepository(DashboardWidgetRepository dashboardWidgetRepository) {
		getLog().debug( "invoking setRepository, setting: " + dashboardWidgetRepository);
		this.repository = dashboardWidgetRepository;
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
		String resourceName = DashboardWidget.class.getName();
		List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
		this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
	}

	@Override
	public void updateDashboardWidget(DashboardWidget[] widgetConfiguration, HyperIoTContext ctx) {
		this.repository.updateDashboardWidget(widgetConfiguration);
	}

    @Override
    public void removeByProjectId(long projectId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        repository.executeUpdateQuery("delete from DashboardWidget as dashboardWidget where dashboardWidget.dashboard.HProject.id = :projectId", params);
    }

	@Override
	public Collection<DashboardWidget> getAllDashboardWidget(long dashboardId) {
		return repository.getAllDashboardWidget(dashboardId);
	}

	@Override
	public Collection<DashboardWidget> getAllDashboardWidgetByPacketId(long packetId) {
		return repository.getAllDashboardWidgetByPacketId(packetId);
	}

	@Override
	public Collection<DashboardWidget> getAllDashboardWidgetByHProjectAlgorithmId(long hProjectAlgorithmId) {
		return repository.getAllDashboardWidgetByHProjectAlgorithmId(hProjectAlgorithmId);
	}


}

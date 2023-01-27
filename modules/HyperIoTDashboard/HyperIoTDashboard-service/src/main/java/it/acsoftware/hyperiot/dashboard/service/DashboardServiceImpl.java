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

import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTOwnedChildBaseEntityServiceImpl;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl ;
import it.acsoftware.hyperiot.dashboard.api.DashboardApi;
import it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;

/**
 *
 * @author Aristide Cittadino Implementation class of DashboardApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = DashboardApi.class, immediate = true)
public final class DashboardServiceImpl extends HyperIoTOwnedChildBaseEntityServiceImpl<Dashboard> implements DashboardApi, HyperIoTOwnershipResourceService {

	/**
	 * Injecting the DashboardSystemApi
	 */
	private DashboardSystemApi systemService;

	/**
	 * Constructor for a DashboardServiceImpl
	 */
	public DashboardServiceImpl() {
		super(Dashboard.class);
	}

	/**
	 *
	 * @return The current DashboardSystemApi
	 */
	protected DashboardSystemApi getSystemService() {
		getLog().debug( "invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 *
	 * @param dashboardSystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(DashboardSystemApi dashboardSystemService) {
		getLog().debug( "invoking setSystemService, setting: {}" , systemService);
		this.systemService = dashboardSystemService ;
	}

	@Override
	public String getOwnerFieldPath() {
		return "HProject.user.id";
	}

	@Override
	protected String getRootParentFieldPath() {
		return "HProject.id";
	}

	@Override
	protected Class<? extends HyperIoTOwnedResource> getParentResourceClass() {
		return HProject.class;
	}

}

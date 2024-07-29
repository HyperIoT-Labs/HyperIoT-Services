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

package it.acsoftware.hyperiot.hproject.algorithm.service;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.security.annotations.AllowGenericPermissions;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTOwnedChildBaseEntityServiceImpl;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.algorithm.actions.HProjectAlgorithmAction;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmApi;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmSystemApi;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collection;


/**
 *
 * @author Aristide Cittadino Implementation class of HProjectAlgorithmApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = HProjectAlgorithmApi.class, immediate = true)
public final class HProjectAlgorithmServiceImpl extends HyperIoTOwnedChildBaseEntityServiceImpl<HProjectAlgorithm> implements HProjectAlgorithmApi, HyperIoTOwnershipResourceService {
	/**
	 * Injecting the HProjectAlgorithmSystemApi
	 */
	private HProjectAlgorithmSystemApi systemService;

	/**
	 * Constructor for a HProjectAlgorithmServiceImpl
	 */
	public HProjectAlgorithmServiceImpl() {
		super(HProjectAlgorithm.class);
	}

	/**
	 *
	 * @return The current HProjectAlgorithmSystemApi
	 */
	protected HProjectAlgorithmSystemApi getSystemService() {
		getLog().debug( "invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	@Override
	@AllowPermissions(actions = HyperIoTHProjectAction.Names.ALGORITHMS_MANAGEMENT, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
	public Collection<HProjectAlgorithm> findByHProjectId(HyperIoTContext hyperIoTContext, long hProjectId) {
		getLog().debug( "invoking findByHProjectId, on project: {}" , hProjectId);
		return systemService.findByHProjectId(hProjectId);
	}


	@Override
	@AllowPermissions(actions = HProjectAlgorithmAction.Names.UPDATE_CONFIG, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmSystemApi")
	public HProjectAlgorithm updateConfig(HyperIoTContext context, long hProjectAlgorithmId, HProjectAlgorithmConfig config) {
		getLog().debug( "invoking updateConfig, on HProjectAlgorithm: {}" , hProjectAlgorithmId);
		if (config == null)
			throw new HyperIoTRuntimeException("Config must not be null");
		return systemService.updateConfig(hProjectAlgorithmId, config);
	}

	/**
	 *
	 * @param hProjectAlgorithmSystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(HProjectAlgorithmSystemApi hProjectAlgorithmSystemService) {
		getLog().debug( "invoking setSystemService, setting: {}" , systemService);
		this.systemService = hProjectAlgorithmSystemService;
	}

	@Override
	public String getOwnerFieldPath() {
		return "project.user.id";
	}


	@Override
	protected String getRootParentFieldPath() {
		return "project.id";
	}

	@Override
	protected Class<? extends HyperIoTOwnedResource> getParentResourceClass() {
		return HProject.class;
	}
}

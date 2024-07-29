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
package it.acsoftware.hyperiot.stormmanager.service;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerApi;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;

/**
 * @author Aristide Cittadino Implementation class of StormManagerApi interface.
 * It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = StormManagerApi.class, immediate = true)
public final class StormManagerServiceImpl extends HyperIoTBaseServiceImpl implements StormManagerApi {

    /**
     * Injecting the StormManagerSystemApi
     */
    private StormManagerSystemApi systemService;

    /**
     * Injecting HProjectSystemApii
     */
    private HProjectSystemApi hProjectSystemApi;

    /**
     * @return The current StormManagerSystemApi
     */
    protected StormManagerSystemApi getSystemService() {
        getLog().debug( "invoking getSystemService, returning: {}", this.systemService);
        return systemService;
    }

    /**
     * @param stormManagerSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(StormManagerSystemApi stormManagerSystemService) {
        getLog().debug( "invoking setSystemService, setting: {}", systemService);
        this.systemService = stormManagerSystemService;
    }

    /**
     * @param hProjectSystemApi
     */
    @Reference
    public void sethProjectSystemApi(HProjectSystemApi hProjectSystemApi) {
        this.hProjectSystemApi = hProjectSystemApi;
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.GET_TOPOLOGY, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public TopologyInfo getTopologyStatus(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        return systemService.getTopologyStatus(projectId);
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.ACTIVATE_TOPOLOGY, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public void activateTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = this.hProjectSystemApi.find(projectId, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        systemService.activateTopology(systemService.getTopologyName(projectId));
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.DEACTIVATE_TOPOLOGY, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public void deactivateTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = this.hProjectSystemApi.find(projectId, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        systemService.deactivateTopology(systemService.getTopologyName(projectId));
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.KILL_TOPOLOGY, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public void killTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = this.hProjectSystemApi.find(projectId, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        systemService.killTopology(systemService.getTopologyName(projectId));
    }

    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.ADD_TOPOLOGY, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public void submitProjectTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        this.systemService.submitProjectTopology(projectId);
    }

}

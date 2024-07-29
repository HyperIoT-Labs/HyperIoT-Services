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

package it.acsoftware.hyperiot.rule.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTOwnedChildBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.rule.api.RuleEngineApi;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.*;

/**
 * @author Aristide Cittadino Implementation class of RuleEngineApi interface.
 * It is used to implement all additional methods in order to interact
 * with the system layer.
 */
@Component(service = RuleEngineApi.class, immediate = true)
public final class RuleEngineServiceImpl extends HyperIoTOwnedChildBaseEntityServiceImpl<Rule>
        implements RuleEngineApi, HyperIoTOwnershipResourceService {

    private final String resourceName = Rule.class.getName();
    /**
     * Injecting the RuleEngineSystemApi
     */
    private RuleEngineSystemApi systemService;

    /**
     * Injecting HProjectSystemApi
     */
    private HProjectSystemApi hProjectSystemApi;

    /**
     * Injecting HPacketSystemApi
     */
    private HPacketSystemApi hPacketSystemApi;

    /**
     * Constructor for a RuleEngineServiceImpl
     */
    public RuleEngineServiceImpl() {
        super(Rule.class);
    }


    /**
     * @return The current RuleEngineSystemApi
     */
    protected RuleEngineSystemApi getSystemService() {
        getLog().debug( "invoking getSystemService, returning: {}" , this.systemService);
        return systemService;
    }

    /**
     * @param ruleEngineSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(RuleEngineSystemApi ruleEngineSystemService) {
        getLog().debug( "invoking setSystemService, setting: {}" , systemService);
        this.systemService = ruleEngineSystemService;
    }

    /**
     * @param hProjectSystemApi
     */
    @Reference
    public void sethProjectSystemApi(HProjectSystemApi hProjectSystemApi) {
        this.hProjectSystemApi = hProjectSystemApi;
    }

    /**
     * @param hPacketSystemApi
     */
    @Reference
    public void sethPacketSystemApi(HPacketSystemApi hPacketSystemApi) {
        this.hPacketSystemApi = hPacketSystemApi;
    }

    /**
     * Method to retrieve the drools definition for all rules defined by the user
     * for a specific project
     */
    @Override
    @AllowPermissions(actions = HyperIoTHProjectAction.Names.MANAGE_RULES, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public String getDroolsForProject(HyperIoTContext context, long projectId, RuleType ruleType) {
        return this.systemService.getDroolsForProject(projectId, ruleType);
    }

    @Override
    public List<RuleAction> findRuleActions(String type) {
        try {
            Collection<ServiceReference<RuleAction>> actions = HyperIoTUtil.getBundleContext(this.getClass()).getServiceReferences(RuleAction.class, "(it.acsoftware.hyperiot.rule.action.type=" + type + ")");
            if (actions != null && !actions.isEmpty()) {
                List<RuleAction> actionsList = new ArrayList<>();
                Iterator<ServiceReference<RuleAction>> it = actions.iterator();
                while (it.hasNext()) {
                    ServiceReference<RuleAction> ref = it.next();
                    RuleAction a = HyperIoTUtil.getBundleContext(this).getService(ref);
                    actionsList.add(a);
                }
                return actionsList;
            }
            return Collections.emptyList();
        } catch (InvalidSyntaxException e) {

        }
        return Collections.emptyList();
    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.FINDALL, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi")
    public Collection<Rule> findAllRuleByPacketId(HyperIoTContext context, long packetId) {
        return this.systemService.findAllRuleByPacketId(packetId);
    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.FINDALL, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hproject.api.HProjectSystemApi")
    public Collection<Rule> findAllRuleByProjectId(HyperIoTContext context, long projectId) {
        return this.systemService.findAllRuleByProjectId(projectId);
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

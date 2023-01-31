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

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hpacket.api.HPacketRepository;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import it.acsoftware.hyperiot.rule.api.RuleEngineRepository;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleDroolsBuilder;
import it.acsoftware.hyperiot.rule.model.RuleType;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Aristide Cittadino Implementation class of the RuleEngineSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = RuleEngineSystemApi.class, immediate = true)
public final class RuleEngineSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Rule>
        implements RuleEngineSystemApi {

    /**
     * Injecting the RuleEngineRepository to interact with persistence layer
     */
    private RuleEngineRepository repository;

    private HPacketRepository hPacketRepository;

    private HProjectRepository hProjectRepository;

    /**
     * Constructor for a RuleEngineSystemServiceImpl
     */
    public RuleEngineSystemServiceImpl() {
        super(Rule.class);
    }

    /**
     * Return the current repository
     */
    protected RuleEngineRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param ruleEngineRepository The current value of RuleEngineRepository to
     *                             interact with persistence layer
     */
    @Reference
    protected void setRepository(RuleEngineRepository ruleEngineRepository) {
        getLog().debug("invoking setRepository, setting: {}", ruleEngineRepository);
        this.repository = ruleEngineRepository;
    }

    @Reference
    protected void setHPacketRepository(HPacketRepository hPacketRepository) {
        this.hPacketRepository = hPacketRepository;
    }

    @Reference
    protected void setHProjectRepository(HProjectRepository hProjectRepository) {
        this.hProjectRepository = hProjectRepository;
    }

    /**
     * @param projectId
     * @param ruleType
     * @return the complete list of rules defined for specified project
     */
    public String getDroolsForProject(long projectId, RuleType ruleType) {
        try {
            hProjectRepository.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        Collection<Rule> rules = repository.getDroolsForProject(projectId, ruleType);
        return RuleDroolsBuilder.buildHProjectDrools(rules, ruleType);
    }

    @Override
    public Collection<Rule> findAllRuleByPacketId(long packetId) {
        try {
            this.hPacketRepository.find(packetId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        return repository.findAllRuleByPacketId(packetId);
    }

    @Override
    public Collection<Rule> findAllRuleByProjectId(long projectId) {
        try {
            hProjectRepository.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        return repository.findAllRuleByProjectId(projectId);
    }

    @Override
    public void removeByHPacketId(long hPacketId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("hPacketId", hPacketId);
        repository.executeUpdateQuery("delete from Rule rule where rule.packet.id = :hPacketId", params);
    }

    @Override
    public void removeByHProjectId(long hProjectId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("hProjectId", hProjectId);
        repository.executeUpdateQuery("delete from Rule rule where rule.project.id = :hProjectId", params);
    }

    @Override
    public Collection<Rule> findAllRuleByProjectIdAndRuleType(long projectId, RuleType ruleType) {
        return repository.findAllRuleByProjectIdAndRuleType(projectId, ruleType);
    }

    @Override
    public Collection<Rule> findAllRuleByHPacketId(long hPacketId) {
        return repository.findAllRuleByHPacketId(hPacketId);
    }

    @Override
    public Collection<Rule> findAllRuleByHPacketFieldId(long hPacketFieldId) {
        return repository.findAllRuleByHPacketFieldId(hPacketFieldId);
    }

    /**
     * On Bundle activated
     */
    @Activate
    public void activateRuleEngine() {
        this.checkRegisteredUserRoleExists();
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        try {
            String resourceName = Rule.class.getName();
            List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
            PermissionSystemApi permissionSystemApi = (PermissionSystemApi) HyperIoTUtil.getService(PermissionSystemApi.class);
            permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
        } catch (Throwable t) {
            getLog().error(t.getMessage(), t);
        }
    }

}

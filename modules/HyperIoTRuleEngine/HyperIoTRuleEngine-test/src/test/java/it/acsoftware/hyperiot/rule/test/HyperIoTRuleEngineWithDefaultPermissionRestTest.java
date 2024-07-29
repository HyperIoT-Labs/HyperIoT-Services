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

package it.acsoftware.hyperiot.rule.test;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.service.actions.ValidateHPacketRuleAction;
import it.acsoftware.hyperiot.rule.service.rest.RuleEngineRestApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.*;

import static it.acsoftware.hyperiot.rule.test.HyperIoTRuleEngineConfiguration.nameRegisteredPermission;
import static it.acsoftware.hyperiot.rule.test.HyperIoTRuleEngineConfiguration.permissionRule;

/**
 * @author Aristide Cittadino Interface component for RuleEngine System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRuleEngineWithDefaultPermissionRestTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    @SuppressWarnings("unused")
    private HyperIoTAction getHyperIoTAction(String resourceName, HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder.createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @Test
    public void test00_hyperIoTFrameworkShouldBeInstalled() {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class,0);
        String features = executeCommand("feature:list -i");
        //HyperIoTCore
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTMail-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTCompany-features ", features);
        assertContains("HyperIoTAssetCategory-features", features);
        assertContains("HyperIoTAssetTag-features", features);
        assertContains("HyperIoTSharedEntity-features", features);
        //HyperIoTServices
        assertContains("HyperIoTHProject-features ", features);


        assertContains("HyperIoTAlgorithm-features ", features);

        assertContains("HyperIoTHadoopManager-features ", features);
        assertContains("HyperIoTDashboard-features ", features);

        assertContains("HyperIoTRuleEngine-features ", features);

        assertContains("HyperIoTStormManager-features ", features);
        assertContains("HyperIoTHBaseConnector-features ", features);
        assertContains("HyperIoTSparkManager-features ", features);
        assertContains("HyperIoTKafkaConnector-features ", features);
        String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01_ruleEngineModuleShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call checkModuleWorking checks if Rule module working
        // correctly
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("RuleEngine Module works!", restResponse.getEntity());
    }


    // Rule action save: 1
    @Test
    public void test02_saveRuleWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, save Rule with the following call saveRule
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule 1");
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 23 AND humidity > 36");
        rule.setProject(hproject);

        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{123});
        ValidateHPacketRuleAction hpacketAction = new ValidateHPacketRuleAction();
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        actions.add(hpacketAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Rule) restResponse.getEntity()).getId());
        Assert.assertEquals(rule.getName(), ((Rule) restResponse.getEntity()).getName());
        Assert.assertEquals("Rule defined by huser: " + hproject.getUser().getUsername(), ((Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals(RuleType.ENRICHMENT, ((Rule) restResponse.getEntity()).getType());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.enrichments", ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("temperature >= 23 AND humidity > 36", ((Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals(hproject.getId(), ((Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((Rule) restResponse.getEntity()).getProject().getUser().getId());
    }


    // Rule action update: 2
    @Test
    public void test03_updateRuleWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, update Rule with the following call updateRule
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRule(hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        Date date = new Date();
        rule.setDescription("Description edited in date: " + date);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getEntityVersion() + 1,
                ((Rule) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("Description edited in date: " + date,
                ((Rule) restResponse.getEntity()).getDescription());
    }


    // Rule action remove: 4
    @Test
    public void test04_deleteRuleWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, delete Rule with the following call deleteRule
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.deleteRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // Rule action find: 8
    @Test
    public void test05_findRuleWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, find Rule with the following call findRule
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getId(), ((Rule) restResponse.getEntity()).getId());
    }


    // Rule action find-all: 16
    @Test
    public void test06_findAllRuleWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, find all Rule with the following call findAllRule
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRule(hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRule();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Rule> listRules = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRules.isEmpty());
        Assert.assertEquals(1, listRules.size());
        boolean ruleFound = false;
        for (Rule r : listRules) {
            if (rule.getId() == r.getId()) {
                Assert.assertEquals(hproject.getId(), r.getProject().getId());
                Assert.assertEquals(huser.getId(), r.getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
    }


    // Rule action find-all: 16
    @Test
    public void test07_findAllRulePaginatedWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with default permission,
        // find all Rules with pagination
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        int delta = 4;
        int page = 1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 4;
        for (int i = 0; i < numbEntities; i++) {
            Rule rule = createRule(hproject, null);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
            rules.add(rule);
        }
        Assert.assertEquals(numbEntities, rules.size());
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(delta, listRules.getResults().size());
        Assert.assertEquals(delta, listRules.getDelta());
        Assert.assertEquals(page, listRules.getCurrentPage());
        Assert.assertEquals(page, listRules.getNextPage());
        // delta is 4, page is 1: 4 entities stored in database
        Assert.assertEquals(1, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    // HPacket action find-all: 16 (RuleEngine)
    @Test
    public void test013_findAllRuleByPacketIdWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, find all Rule by packetId with the following
        // call findAllRuleByPacketId
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRule(hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<Rule> listRulesByPacketId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRulesByPacketId.isEmpty());
        Assert.assertEquals(1, listRulesByPacketId.size());
        boolean ruleFound = false;
        for (Rule r : listRulesByPacketId) {
            if (rule.getId() == r.getId()) {
                Assert.assertEquals(hproject.getId(), r.getProject().getId());
                Assert.assertEquals(huser.getId(), r.getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
    }

    @Test
    public void test011_findAllRuleByProjectIdWithDefaultPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, find all Rule by projectId with the following
        // call findAllRuleByProjectId
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<Rule> listRulesByProjectId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRulesByProjectId.isEmpty());
        Assert.assertEquals(1, listRulesByProjectId.size());
        boolean ruleFound = false;
        for (Rule r : listRulesByProjectId) {
            if (rule.getId() == r.getId()) {
                Assert.assertEquals(hproject.getId(), r.getProject().getId());
                Assert.assertEquals(huser.getId(), r.getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    private HUser huserWithDefaultPermissionInHyperIoTFramework(boolean isActive) {
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hUserRestApi, adminUser);
        String username = "TestUser";
        List<Object> roles = new ArrayList<>();
        HUser huser = new HUser();
        huser.setName("name");
        huser.setLastname("lastname");
        huser.setUsername(username + UUID.randomUUID().toString().replaceAll("-", ""));
        huser.setEmail(huser.getUsername() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        huser.setActive(false);
        Assert.assertNull(huser.getActivateCode());
        Response restResponse = hUserRestApi.register(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
        Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
        Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
        Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
        Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
        Assert.assertFalse(huser.isAdmin());
        Assert.assertFalse(huser.isActive());
        Assert.assertTrue(roles.isEmpty());
        if (isActive) {
            //Activate huser and checks if default role has been assigned
            Role role = null;
            Assert.assertFalse(huser.isActive());
            String activationCode = huser.getActivateCode();
            Assert.assertNotNull(activationCode);
            Response restResponseActivateUser = hUserRestApi.activate(huser.getEmail(), activationCode);
            Assert.assertEquals(200, restResponseActivateUser.getStatus());
            huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
            roles = Arrays.asList(huser.getRoles().toArray());
            Assert.assertFalse(roles.isEmpty());
            Assert.assertTrue(huser.isActive());

            // checks: default role has been assigned to new huser
            Assert.assertEquals(1, huser.getRoles().size());
            Assert.assertEquals(roles.size(), huser.getRoles().size());
            Assert.assertFalse(roles.isEmpty());
            for (int i = 0; i < roles.size(); i++) {
                role = ((Role) roles.get(i));
            }
            Assert.assertNotNull(role);
            Assert.assertEquals("RegisteredUser", role.getName());
            Assert.assertEquals("Role associated with the registered user",
                    role.getDescription());
            PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
            Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
            Assert.assertFalse(listPermissions.isEmpty());
            boolean resourceNameFound = false;
            for (Permission permission : listPermissions) {
                // it.acsoftware.hyperiot.rule.model.Rule (31)
                // save                     1
                // update                   2
                // remove                   4
                // find                     8
                // find_all                 16
                if (permission.getEntityResourceName().contains(permissionRule)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionRule, permission.getEntityResourceName());
                    Assert.assertEquals(permissionRule + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(31, permission.getActionIds());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameFound = true;
                }
            }
            Assert.assertTrue(resourceNameFound);
        }
        return huser;
    }

    private HProject createHProject(HUser huser) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HProject hproject = new HProject();
        hproject.setName("Project " + UUID.randomUUID());
        hproject.setDescription("Project of user: " + huser.getUsername());
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals(hproject.getDescription(), ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
    }

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HUser ownerHUser = hproject.getUser();
        HDevice hdevice = new HDevice();
        hdevice.setBrand("ACSoftware");
        hdevice.setDescription("Description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestApi, ownerHUser);
        Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals("ACSoftware",
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals("Description",
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals("model",
                ((HDevice) restResponse.getEntity()).getModel());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getSoftwareVersion());
        Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
        Assert.assertEquals(hproject.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(ownerHUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
    }

    private HPacket createHPacket(HDevice hdevice) {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("version" + UUID.randomUUID().toString().replaceAll("-", ""));

        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");

        HUser ownerHUser = hdevice.getProject().getUser();
        this.impersonateUser(hPacketRestApi, hdevice.getProject().getUser());
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        return hpacket;
    }

    private Rule createRule(HProject hproject, HPacket hpacket) {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        HUser ownerHUser = hproject.getUser();

        Rule rule = new Rule();
        rule.setName("Add category rule 1" + UUID.randomUUID().toString());
        rule.setDescription("Rule defined by huser: " + hproject.getUser().getUsername());
        rule.setType(RuleType.EVENT);
        rule.setRuleDefinition("temperature >= 23 AND humidity > 36");
        rule.setProject(hproject);

        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, ownerHUser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Rule) restResponse.getEntity()).getId());
        Assert.assertEquals(rule.getName(), ((Rule) restResponse.getEntity()).getName());
        Assert.assertEquals("Rule defined by huser: " + ownerHUser.getUsername(), ((Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals(RuleType.EVENT, ((Rule) restResponse.getEntity()).getType());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events", ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("temperature >= 23 AND humidity > 36", ((Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals(hproject.getId(), ((Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((Rule) restResponse.getEntity()).getProject().getUser().getId());
        return rule;
    }


    // RuleEngine is Owned Resource: only huser is able to find/findAll his entities
    private HUser huser;

    @After
    public void afterTest() {
        // Remove projects and delete in cascade all RuleEngine created in every test
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        if ((huser != null) && (huser.isActive())) {
            this.impersonateUser(hprojectRestService, huser);
            Response restResponse = hprojectRestService.findAllHProject();
            List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
            });
            if (!listHProjects.isEmpty()) {
                Assert.assertFalse(listHProjects.isEmpty());
                for (HProject project : listHProjects) {
                    this.impersonateUser(hprojectRestService, huser);
                    Response restResponse1 = hprojectRestService.deleteHProject(project.getId());
                    Assert.assertEquals(200, restResponse1.getStatus());
                }
            }
        }

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        // Remove all husers created in every test
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        this.impersonateUser(huserRestService, adminUser);
        Response restResponseUsers = huserRestService.findAllHUser();
        List<HUser> listHUsers = restResponseUsers.readEntity(new GenericType<List<HUser>>() {
        });
        if (!listHUsers.isEmpty()) {
            Assert.assertFalse(listHUsers.isEmpty());
            for (HUser huser : listHUsers) {
                if (!huser.isAdmin()) {
                    this.impersonateUser(huserRestService, adminUser);
                    Response restResponse1 = huserRestService.deleteHUser(huser.getId());
                    Assert.assertEquals(200, restResponse1.getStatus());
                    Assert.assertNull(restResponse1.getEntity());
                }
            }
        }
//        String sqlHProject = "select * from hproject";
//        String resultHProject = executeCommand("jdbc:query hyperiot " + sqlHProject);
//        System.out.println(resultHProject);
//
//        String sqlRule = "select * from rule";
//        String resultRule = executeCommand("jdbc:query hyperiot " + sqlRule);
//        System.out.println(resultRule);
//
//        String sqlHUser = "select * from huser";
//        String resultHUser = executeCommand("jdbc:query hyperiot " + sqlHUser);
//        System.out.println(resultHUser);
    }


}

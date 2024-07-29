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
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
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
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
import it.acsoftware.hyperiot.role.api.RoleRepository;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.service.actions.AddTagRuleAction;
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
import java.io.IOException;
import java.util.*;

import static it.acsoftware.hyperiot.rule.test.HyperIoTRuleEngineConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for RuleEngine System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRuleEngineWithPermissionRestTest extends KarafTestSupport {

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
        huser = createHUser(null);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("RuleEngine Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_saveRuleWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, save Rule with the following call saveRule
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
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

    @Test
    public void test03_saveRuleWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to save Rule with the following call saveRule
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
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
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test04_updateRuleWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, update Rule with the following call updateRule
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setRuleDefinition("temperature >= 24");
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getEntityVersion() + 1,
                ((Rule) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("temperature >= 24",
                ((Rule) restResponse.getEntity()).getRuleDefinition());
    }

    @Test
    public void test05_updateRuleWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to update Rule with the following call updateRule
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setRuleDefinition("temperature >= 24");
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findRuleWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find Rule with the following call findRule
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getId(), ((Rule) restResponse.getEntity()).getId());
    }

    @Test
    public void test07_findRuleWithPermissionShouldFailIfEntityNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to find Rule with the following call findRule,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findRule(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test08_findRuleWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to find Rule with the following call
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findRule(rule.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findRuleNotFoundWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to find Rule not found with the following call findRule
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findRule(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test10_findAllRuleWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find all Rule with the following call findAllRule
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRule();
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
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test11_findAllRuleWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to find all Rule with the following call findAllRule
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRule();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteRuleWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, delete Rule with the following call deleteRule
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.deleteRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test13_deleteRuleWithPermissionShouldFailIfEntityNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to delete Rule with the following call deleteRule,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.deleteRule(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_deleteRuleWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to delete Rule with the following call deleteRule
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.deleteRule(rule.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test15_deleteRuleNotFoundWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to delete Rule not found with the following call deleteRule
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.deleteRule(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test16_saveRuleWithPermissionShouldFailIfNameIsNull() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveRule,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName(null);
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 25 AND humidity > 40");
        rule.setProject(hproject);

        AddCategoryRuleAction categoryRuleAction = new AddCategoryRuleAction();
        categoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryRuleAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test17_saveRuleWithPermissionShouldFailIfNameIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveRule,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("");
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 25 AND humidity > 40");
        rule.setProject(hproject);

        AddCategoryRuleAction categoryRuleAction = new AddCategoryRuleAction();
        categoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryRuleAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test18_saveRuleWithPermissionShouldFailIfNameIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveRule,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("src='test malicious code'");
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 25 AND humidity > 40");
        rule.setProject(hproject);

        AddCategoryRuleAction categoryRuleAction = new AddCategoryRuleAction();
        categoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryRuleAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test19_saveRuleWithJsonActionsWithPermissionShouldWork() throws IOException {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, save Rule with the following call saveRule
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule 1");
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("latitude >= 3 AND temperature > 23");
        rule.setProject(hproject);


        rule.setJsonActions(HyperIoTRuleEngineRestTest.jsonActionsCategoryAction);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(1, ((Rule) restResponse.getEntity()).getActions().size());
        Assert.assertEquals("it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction", ((Rule) restResponse.getEntity()).getActions().get(0).getActionName());
        long[] categoryIds = ((AddCategoryRuleAction) rule.getActions().get(0)).getCategoryIds();
        Assert.assertEquals(123, categoryIds[0]);
        Assert.assertEquals(HyperIoTRuleEngineRestTest.jsonActionsCategoryAction, ((Rule) restResponse.getEntity()).getJsonActions());
    }

    @Test
    public void test20_saveRuleWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveRule,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("expression(test malicious code)");
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 23 AND humidity > 36");
        rule.setProject(hproject);

        AddCategoryRuleAction addCategoryRuleAction = new AddCategoryRuleAction();
        addCategoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(addCategoryRuleAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test21_saveRuleWithPermissionShouldFailIfRuleDefinitionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveRule,
        // but rule definition is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("eval(test malicious code)");
        rule.setProject(hproject);

        AddCategoryRuleAction addCategoryRuleAction = new AddCategoryRuleAction();
        addCategoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(addCategoryRuleAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-ruledefinition", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getRuleDefinition(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test22_saveRuleWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveHDevice,
        // but HProject belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        huser2 = createHUser(null);
        HProject hprojectOfHUser2 = createHProject(huser2);
        Assert.assertNotEquals(0, hprojectOfHUser2.getId());
        Assert.assertEquals(huser2.getId(), hprojectOfHUser2.getUser().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        Rule rule = new Rule();
        rule.setName("Add category rule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("latitude >= 3 AND temperature > 23");
        rule.setProject(hprojectOfHUser2);

        AddCategoryRuleAction addCategoryRuleAction = new AddCategoryRuleAction();
        addCategoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(addCategoryRuleAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test23_saveRuleWithPermissionShouldFailIfActionsIsMissing() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveRule,
        // but actions is missing
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + huser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("latitude >= 3 AND temperature > 23");
        rule.setProject(hproject);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-jsonactions", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("rule-jsonactions", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test24_updateRuleWithPermissionShouldFailIfNameIsNull() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateRule,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setName(null);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test25_updateRuleWithPermissionShouldFailIfNameIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateRule,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setName("");
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test26_updateRuleWithPermissionShouldFailIfNameIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateRule,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setName("onload(test malicious code)=");
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test27_updateRuleWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateRule,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setDescription("<script test malicious code>");
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test28_updateRuleWithPermissionShouldFailIfRuleDefinitionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateRule,
        // but rule definition is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setRuleDefinition("src='test malicious code'");
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("rule-ruledefinition", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(rule.getRuleDefinition(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test29_updateRuleWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateHDevice,
        // but HProject belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        huser2 = createHUser(null);
        HProject hprojectOfHUser2 = createHProject(huser2);
        Assert.assertNotEquals(0, hprojectOfHUser2.getId());
        Assert.assertEquals(huser2.getId(), hprojectOfHUser2.getUser().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        rule.setProject(hprojectOfHUser2);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test30_findAllRulePaginatedWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with permission, find all Rules with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 6;
        int page = 2;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 8;
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
        Assert.assertEquals(numbEntities - delta, listRules.getResults().size());
        Assert.assertEquals(delta, listRules.getDelta());
        Assert.assertEquals(page, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // delta is 6, page is 2: 8 entities stored in database
        Assert.assertEquals(2, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponsePage1 = ruleEngineRestApi.findAllRulePaginated(delta, 1);
        HyperIoTPaginableResult<Rule> listRulesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listRulesPage1.getResults().size());
        Assert.assertEquals(delta, listRulesPage1.getDelta());
        Assert.assertEquals(defaultPage, listRulesPage1.getCurrentPage());
        Assert.assertEquals(page, listRulesPage1.getNextPage());
        // delta is 6, page is 1: 8 entities stored in database
        Assert.assertEquals(2, listRulesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test31_findAllRulePaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with permission, find all Rules with pagination
        // if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 6;
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
        Assert.assertEquals(numbEntities, listRules.getResults().size());
        Assert.assertEquals(defaultDelta, listRules.getDelta());
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // default delta is 10, default page is 1: 6 entities stored in database
        Assert.assertEquals(1, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test32_findAllRulePaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with permission,
        // find all Rules with pagination
        // if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = -1;
        int page = 1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 5;
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
        Assert.assertEquals(numbEntities, listRules.getResults().size());
        Assert.assertEquals(defaultDelta, listRules.getDelta());
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // default delta is 10, default page is 1: 6 entities stored in database
        Assert.assertEquals(1, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test33_findAllRulePaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with permission,
        // find all Rules with pagination
        // if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 0;
        int page = 2;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 14;
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
        Assert.assertEquals(numbEntities - defaultDelta, listRules.getResults().size());
        Assert.assertEquals(defaultDelta, listRules.getDelta());
        Assert.assertEquals(page, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // default delta is 10, page is 2: 14 entities stored in database
        Assert.assertEquals(2, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponsePage1 = ruleEngineRestApi.findAllRulePaginated(delta, 1);
        HyperIoTPaginableResult<Rule> listRulesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listRulesPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listRulesPage1.getDelta());
        Assert.assertEquals(defaultPage, listRulesPage1.getCurrentPage());
        Assert.assertEquals(page, listRulesPage1.getNextPage());
        // default delta is 10, page is 1: 14 entities stored in database
        Assert.assertEquals(2, listRulesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test34_findAllRulePaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with permission, find all Rules with pagination
        // if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 6;
        int page = -1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 3;
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
        Assert.assertEquals(numbEntities, listRules.getResults().size());
        Assert.assertEquals(delta, listRules.getDelta());
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // delta is 6, default page is 1: 3 entities stored in database
        Assert.assertEquals(1, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test35_findAllRulePaginatedWithPermissionShouldWorkIfPageIsZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with permission,
        // find all Rules with pagination
        // if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 5;
        int page = 0;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 7;
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
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listRules.getNextPage());
        // delta is 5, default page is 1: 7 entities stored in database
        Assert.assertEquals(2, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponsePage2 = ruleEngineRestApi.findAllRulePaginated(delta, 2);
        HyperIoTPaginableResult<Rule> listRulesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listRulesPage2.getResults().size());
        Assert.assertEquals(delta, listRulesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listRulesPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listRulesPage2.getNextPage());
        // delta is 5, page is 2: 7 entities stored in database
        Assert.assertEquals(2, listRulesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test36_findAllRulePaginatedWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, without permission,
        // tries to find all Rules with pagination
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test37_updateRuleWithPermissionShouldFailIfEntityNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateRule,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        // entity isn't stored in database
        Rule rule = new Rule();
        rule.setName("Rule name edited");
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test38_saveRuleWithPermissionShouldFailIfEntityIsDuplicated() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to save Rule with the following call saveRule,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        Rule duplicateRule = new Rule();
        duplicateRule.setName(rule.getName());
        duplicateRule.setDescription("entity is duplicated");
        duplicateRule.setType(RuleType.ENRICHMENT);
        duplicateRule.setRuleDefinition("latitude >= 3 AND temperature > 23");
        duplicateRule.setProject(rule.getProject());

        AddCategoryRuleAction addCategoryRuleAction = new AddCategoryRuleAction();
        addCategoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(addCategoryRuleAction);
        duplicateRule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(duplicateRule);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        boolean projectIdIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("project_id")) {
                Assert.assertEquals("project_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                projectIdIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(projectIdIsDuplicated);
    }

    @Test
    public void test39_updateRuleWithPermissionShouldFailIfEntityIsDuplicated() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to update Rule with the following call updateRule,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        Rule duplicateRule = createRule(hproject, null);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(hproject.getId(), duplicateRule.getProject().getId());
        Assert.assertEquals(huser.getId(), duplicateRule.getProject().getUser().getId());

        duplicateRule.setName(rule.getName());
        duplicateRule.setProject(rule.getProject());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(duplicateRule);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        boolean projectIdIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("project_id")) {
                Assert.assertEquals("project_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                projectIdIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(projectIdIsDuplicated);
    }

    @Test
    public void test40_saveRuleDuplicatedWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to save a Rule duplicated
        // with the following call saveRule
        // response status code '403' HyperIoTDuplicateEntityException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        Rule duplicateRule = new Rule();
        duplicateRule.setName(rule.getName());
        duplicateRule.setDescription("entity is duplicated");
        duplicateRule.setType(RuleType.ENRICHMENT);
        duplicateRule.setRuleDefinition("latitude >= 3 AND temperature > 23");
        duplicateRule.setProject(rule.getProject());

        AddCategoryRuleAction addCategoryRuleAction = new AddCategoryRuleAction();
        addCategoryRuleAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(addCategoryRuleAction);
        duplicateRule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(duplicateRule);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test41_updateRuleDuplicatedWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to update a Rule duplicated
        // with the following call updateRule
        // response status code '403' HyperIoTDuplicateEntityException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        Rule duplicateRule = createRule(hproject, null);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(hproject.getId(), duplicateRule.getProject().getId());
        Assert.assertEquals(huser.getId(), duplicateRule.getProject().getUser().getId());

        duplicateRule.setName(rule.getName());
        duplicateRule.setProject(rule.getProject());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(duplicateRule);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test42_updateRuleWithPermissionShouldWorkWithNewJsonActions() throws IOException {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, update Rule with the following call updateRule
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());



        rule.setJsonActions(HyperIoTRuleEngineRestTest.jsonActionsTagAction);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getEntityVersion() + 1,
                ((Rule) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals(1, ((Rule) restResponse.getEntity()).getActions().size());
        Assert.assertEquals("it.acsoftware.hyperiot.rule.service.actions.AddTagRuleAction", ((Rule) restResponse.getEntity()).getActions().get(0).getActionName());
        long[] tagIds = ((AddTagRuleAction) rule.getActions().get(0)).getTagIds();
        Assert.assertEquals(123, tagIds[0]);
        Assert.assertEquals(HyperIoTRuleEngineRestTest.jsonActionsTagAction, ((Rule) restResponse.getEntity()).getJsonActions());
    }

    @Test
    public void test43_findAllRuleByProjectIdWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find all Rule by projectId with the following
        // call findAllRuleByProjectId
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hprojectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());


        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
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
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test44_findAllRuleByProjectIdWithPermissionShouldWorkIfListRulesIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find all Rule by projectId with
        // the following call findAllRuleByProjectId
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hprojectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
        List<Rule> listRulesByProjectId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertTrue(listRulesByProjectId.isEmpty());
        Assert.assertEquals(0, listRulesByProjectId.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test45_findAllRuleByProjectIdWithPermissionShouldFailIfHProjectNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to finds all Rule by projectId with
        // the following call findAllRuleByProjectId
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hprojectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test46_findAllRuleByProjectIdWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to finds all Rule by projectId
        // with the following call findAllRuleByProjectId
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test47_findAllRuleByPacketIdWithPermissionShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find all Rule by packetId with the following
        // call findAllRuleByPacketId
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hpacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
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
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test48_findAllRuleByPacketIdWithPermissionShouldWorkIfListRulesIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find all Rule by packetId with the following
        // call findAllRuleByPacketId
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hpacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
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

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(hpacket.getId());
        List<Rule> listRulesByPacketId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertTrue(listRulesByPacketId.isEmpty());
        Assert.assertEquals(0, listRulesByPacketId.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test49_findAllRuleByPacketIdWithPermissionShouldFailIfHPacketNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, tries to finds all Rule by packetId with the following
        // call findAllRuleByPacketId, but HPacket not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hpacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test50_findAllRuleByPacketIdWithoutPermissionShouldFail() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, without permission, tries to finds all Rule by packetId with the following
        // call findAllRuleByPacketId
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
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
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test51_findAllRuleWithPermissionShouldWorkIfListIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find all Rule with the following call findAllRule
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRule();
        List<Rule> listRules = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertTrue(listRules.isEmpty());
        Assert.assertEquals(0, listRules.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test52_findAllRulePaginatedWithPermissionShouldWorkIfListIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with permission, find all Rules with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ruleResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(defaultDelta, defaultPage);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertTrue(listRules.getResults().isEmpty());
        Assert.assertEquals(0, listRules.getResults().size());
        Assert.assertEquals(defaultDelta, listRules.getDelta());
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // default delta is 10, default page is 1: there are not entities stored in database
        Assert.assertEquals(0, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


    private HUser createHUser(HyperIoTAction action) {
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
        huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        huser.setActive(true);
        Response restResponse = hUserRestApi.saveHUser(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
        Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
        Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
        Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
        Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
        Assert.assertFalse(huser.isAdmin());
        Assert.assertTrue(huser.isActive());
        Assert.assertTrue(roles.isEmpty());
        if (action != null) {
            Role role = createRole();
            huser.addRole(role);
            RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
            Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
            Assert.assertEquals(200, restUserRole.getStatus());
            Assert.assertTrue(huser.hasRole(role));
            roles = Arrays.asList(huser.getRoles().toArray());
            Assert.assertFalse(roles.isEmpty());
            Permission permission = utilGrantPermission(huser, role, action);
            Assert.assertNotEquals(0, permission.getId());
            Assert.assertEquals(ruleResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
            Assert.assertEquals(action.getActionId(), permission.getActionIds());
            Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
            Assert.assertEquals(role.getId(), permission.getRole().getId());
        }
        return huser;
    }

    private Permission utilGrantPermission(HUser huser, Role role, HyperIoTAction action) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        if (action == null) {
            Assert.assertNull(action);
            return null;
        } else {
            PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
            Permission testPermission = permissionSystemApi.findByRoleAndResourceName(role, action.getResourceName());
            if (testPermission == null) {
                Permission permission = new Permission();
                permission.setName(ruleResourceName + " assigned to huser_id " + huser.getId());
                permission.setActionIds(action.getActionId());
                permission.setEntityResourceName(action.getResourceName());
                permission.setRole(role);
                this.impersonateUser(permissionRestApi, adminUser);
                Response restResponse = permissionRestApi.savePermission(permission);
                testPermission = permission;
                Assert.assertEquals(200, restResponse.getStatus());
            } else {
                this.impersonateUser(permissionRestApi, adminUser);
                testPermission.addPermission(action);
                Response restResponseUpdate = permissionRestApi.updatePermission(testPermission);
                Assert.assertEquals(200, restResponseUpdate.getStatus());
            }
            Assert.assertTrue(huser.hasRole(role.getId()));
            return testPermission;
        }
    }

    private Role createRole() {
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(roleRestApi, adminUser);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        Response restResponse = roleRestApi.saveRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Role) restResponse.getEntity()).getId());
        Assert.assertEquals(role.getName(), ((Role) restResponse.getEntity()).getName());
        Assert.assertEquals(role.getDescription(), ((Role) restResponse.getEntity()).getDescription());
        return role;
    }

    private HProject createHProject(HUser huser) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Project of user: " + huser.getUsername());
        hproject.setUser(huser);
        addDefaultPermission(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals(hproject.getDescription(), ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
        removeDefaultPermission(huser);
        return hproject;
    }

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HUser ownerHUser = hproject.getUser();
        HDevice hdevice = new HDevice();
        hdevice.setBrand("ACSoftware");
        hdevice.setDescription("Description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(hproject);
        addDefaultPermission(ownerHUser);
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
        removeDefaultPermission(ownerHUser);
        return hdevice;
    }

    private HPacket createHPacket(HDevice hdevice) {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));

        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");

        HUser ownerHUser = hdevice.getProject().getUser();
        addDefaultPermission(ownerHUser);
        this.impersonateUser(hPacketRestApi, hdevice.getProject().getUser());
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        removeDefaultPermission(ownerHUser);
        return hpacket;
    }

    private Rule createRule(HProject hproject, HPacket hpacket) {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        HUser ownerHUser = hproject.getUser();

        Rule rule = new Rule();
        rule.setName("Add category rule 1" + java.util.UUID.randomUUID().toString());
        rule.setDescription("Rule defined by huser: " + hproject.getUser().getUsername());
        rule.setType(RuleType.EVENT);
        rule.setRuleDefinition("temperature >= 23 AND humidity > 36");
        rule.setProject(hproject);

        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        rule.setActions(actions);

        addDefaultPermission(ownerHUser);
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
        removeDefaultPermission(ownerHUser);
        return rule;
    }


    private Role defaultRole;

    @Before
    public void beforeTest() {
        // find default Role "RegisteredUser"
        RoleRepository roleRepository = getOsgiService(RoleRepository.class);
        defaultRole = roleRepository.findByName("RegisteredUser");
    }

    private void addDefaultPermission(HUser huser) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        huser.addRole(defaultRole);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(defaultRole.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(defaultRole));
    }

    private void removeDefaultPermission(HUser huser) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        huser.removeRole(defaultRole);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.deleteUserRole(defaultRole.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertFalse(huser.hasRole(defaultRole));
    }


    // RuleEngine is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;
    private HUser huser2;

    @After
    public void afterTest() {
        // Remove projects and delete in cascade all RuleEngine created in every test
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hprojectResourceName,
                HyperIoTCrudAction.FINDALL);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hprojectResourceName,
                HyperIoTCrudAction.REMOVE);
        if ((huser != null) && (huser.isActive())) {
            addPermission(huser, action);
            addPermission(huser, action1);
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
        if ((huser2 != null) && (huser2.isActive())) {
            addPermission(huser2, action);
            addPermission(huser2, action1);
            this.impersonateUser(hprojectRestService, huser2);
            Response restResponse = hprojectRestService.findAllHProject();
            List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
            });
            if (!listHProjects.isEmpty()) {
                Assert.assertFalse(listHProjects.isEmpty());
                for (HProject project : listHProjects) {
                    this.impersonateUser(hprojectRestService, huser2);
                    Response restResponse1 = hprojectRestService.deleteHProject(project.getId());
                    Assert.assertEquals(200, restResponse1.getStatus());
                }
            }
        }

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        // Remove all roles and permissions (in cascade mode) created in every test
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestService, adminUser);
        Response restResponseRole = roleRestService.findAllRoles();
        List<Role> listRoles = restResponseRole.readEntity(new GenericType<List<Role>>() {
        });
        if (!listRoles.isEmpty()) {
            Assert.assertFalse(listRoles.isEmpty());
            for (Role role : listRoles) {
                if (!role.getName().contains("RegisteredUser")) {
                    this.impersonateUser(roleRestService, adminUser);
                    Response restResponseRole1 = roleRestService.deleteRole(role.getId());
                    Assert.assertEquals(200, restResponseRole1.getStatus());
                    Assert.assertNull(restResponseRole1.getEntity());
                }
            }
        }

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


    private Permission addPermission(HUser huser, HyperIoTAction action) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        Permission permission = utilGrantPermission(huser, role, action);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertEquals(ruleResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }


}

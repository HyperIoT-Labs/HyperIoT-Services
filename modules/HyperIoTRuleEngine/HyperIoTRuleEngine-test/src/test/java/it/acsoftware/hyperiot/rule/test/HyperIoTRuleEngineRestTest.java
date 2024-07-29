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
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.service.RuleEngine;
import it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.service.actions.FourierTransformRuleAction;
import it.acsoftware.hyperiot.rule.service.actions.ValidateHPacketRuleAction;
import it.acsoftware.hyperiot.rule.service.rest.RuleEngineRestApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static it.acsoftware.hyperiot.rule.test.HyperIoTRuleEngineConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for RuleEngine System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRuleEngineRestTest extends KarafTestSupport {
    public static final String jsonActionsCategoryAction = "[\"{ \\\"actionName\\\":\\\"it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction\\\", \\\"categoryIds\\\":[  123 ] }\"]";
    public static final String jsonActionsTagAction = "[\"{ \\\"actionName\\\":\\\"it.acsoftware.hyperiot.rule.service.actions.AddTagRuleAction\\\", \\\"tagIds\\\":[  123 ] }\"]";

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
        assertServiceAvailable(FeaturesService.class, 0);
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
        // the following call checkModuleWorking checks if RuleEngine module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("RuleEngine Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_saveRuleEngineShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin save Rule with the following call saveRule
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule 1");
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
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

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Rule) restResponse.getEntity()).getId());
        Assert.assertEquals(rule.getName(), ((Rule) restResponse.getEntity()).getName());
        Assert.assertEquals("Rule defined by huser: " + hproject.getUser().getUsername(), ((Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals(RuleType.ENRICHMENT, ((Rule) restResponse.getEntity()).getType());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.enrichments", ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("temperature >= 23 AND humidity > 36", ((Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals(hproject.getId(), ((Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Rule) restResponse.getEntity()).getProject().getUser().getId());
        //System.out.println(rule.droolsDefinition());
        // TODO: the following code is for testing SendMailAction
        // and should be put on a separate test
        /*
         * SendMailAction mailAction = new SendMailAction();
         * mailAction.setBundleContext(HyperIoTUtil.getBundleContext(ruleEngineRestApi))
         * ; mailAction.setRuleId(rule.getId()); mailAction.setSubject("Test mail");
         * mailAction.setRecipients("generoso.martello@acsoftware.it"); mailAction.
         * setBody("Hello world, this is an alert for project ${project.name} ...");
         * mailAction.run();
         */
    }

    @Test
    public void test03_saveRuleEngineShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries to save Rule, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule 1");
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
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

        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test04_updateRuleEngineShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin update Rule with the following call updateRule
        // respose status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        rule.setRuleDefinition("temperature >= 30 AND humidity > 45");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getEntityVersion() + 1,
                ((Rule) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("temperature >= 30 AND humidity > 45",
                ((Rule) restResponse.getEntity()).getRuleDefinition());
    }

    @Test
    public void test05_updateRuleEngineShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries to update Rule, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());

        rule.setRuleDefinition("temperature >= 30 AND humidity > 45");
        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findRuleEngineShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin find Rule with the following call findRule
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getId(), ((Rule) restResponse.getEntity()).getId());
    }

    @Test
    public void test07_findRuleEngineShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries to find Rule, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.findRule(rule.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findRuleEngineShouldFailIfEntityNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to find Rule with the following call findRule,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findRule(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findAllRuleEngineShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin find all Rule with the following call findAllRule
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRule();
        List<Rule> listRules = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRules.isEmpty());
        Assert.assertEquals(1, listRules.size());
        boolean ruleFound = false;
        for (Rule r : listRules) {
            if (rule.getId() == r.getId()) {
                Assert.assertEquals(hproject.getId(), rule.getProject().getId());
                Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test10_findAllRuleEngineShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries to find all Rule, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.findAllRule();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test11_deleteRuleEngineShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin delete Rule with the following call deleteRule
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.deleteRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test12_deleteRuleEngineShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries to delete Rule, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.deleteRule(rule.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_deleteRuleEngineShouldFailIfEntityNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to delete Rule with the following call deleteRule,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.deleteRule(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_saveRuleEngineShouldFailIfNameIsNull() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to save Rule with the following call saveRule,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName(null);
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 25 AND humidity > 40");
        rule.setProject(hproject);

        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test15_saveRuleEngineShouldFailIfNameIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to save Rule with the following call saveRule,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("");
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 25 AND humidity > 40");
        rule.setProject(hproject);

        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test16_saveRuleEngineShouldFailIfNameIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to save Rule with the following call saveRule,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("javascript:");
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 25 AND humidity > 40");
        rule.setProject(hproject);

        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test17_saveRuleEngineShouldFailIfDescriptionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to save Rule with the following call saveRule,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule 1");
        rule.setDescription("vbscript:");
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("temperature >= 25 AND humidity > 40");
        rule.setProject(hproject);

        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test18_saveRuleEngineShouldFailIfRuleDefinitionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to save Rule with the following call saveRule,
        // but rule definition is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule 1");
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("</script>");
        rule.setProject(hproject);

        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test19_saveRuleEngineShouldFailIfActionsIsOmitted() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to save Rule with the following call saveRule,
        // but rule actions is omitted
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add tag rule 1");
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("humidity > 36");
        rule.setProject(hproject);

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test20_saveRuleEngineWithJsonActionsShouldWork() throws IOException {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin save Rule with a json actions with the following call saveRule
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add tag rule 1");
        rule.setDescription("Rule defined by huser: " + adminUser.getUsername());
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("humidity > 36");
        rule.setProject(hproject);

        rule.setJsonActions(jsonActionsCategoryAction);
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(1, ((Rule) restResponse.getEntity()).getActions().size());
        Assert.assertEquals("it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction", ((Rule) restResponse.getEntity()).getActions().get(0).getActionName());
        long[] categoryIds = ((AddCategoryRuleAction) rule.getActions().get(0)).getCategoryIds();
        Assert.assertEquals(123, categoryIds[0]);
        Assert.assertEquals(jsonActionsCategoryAction, ((Rule) restResponse.getEntity()).getJsonActions());
    }


    @Test
    public void test21_updateRuleEngineShouldFailIfNameIsNull() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        rule.setName(null);
        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test22_updateRuleEngineShouldFailIfNameIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        rule.setName("");
        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test23_updateRuleEngineShouldFailIfNameIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        rule.setName("</script>");
        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test24_updateRuleEngineShouldFailIfDescriptionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        rule.setDescription("</script>");
        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test25_updateRuleEngineShouldFailIfRuleDefinitionIsMaliciousCode() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but rule definition is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        rule.setRuleDefinition("</script>");
        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test26_updateRuleEngineWithJsonActionsShouldWork() throws IOException {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but rule definition is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        rule.setJsonActions(jsonActionsCategoryAction);
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getEntityVersion() + 1,
                ((Rule) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals(1, ((Rule) restResponse.getEntity()).getActions().size());
        Assert.assertEquals("it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction", ((Rule) restResponse.getEntity()).getActions().get(0).getActionName());
        long[] categoryIds = ((AddCategoryRuleAction) rule.getActions().get(0)).getCategoryIds();
        Assert.assertEquals(123, categoryIds[0]);
        Assert.assertEquals(jsonActionsCategoryAction, ((Rule) restResponse.getEntity()).getJsonActions());
    }

    @SuppressWarnings("serial")
    @Test
    public void test27_checkRuleShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket packet1 = new HPacket();
        packet1.setDevice(hdevice);
        packet1.setVersion("1.0");
        packet1.setName("MultiSensor data");
        packet1.setType(HPacketType.OUTPUT);
        packet1.setFormat(HPacketFormat.JSON);
        packet1.setSerialization(HPacketSerialization.AVRO);

        packet1.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        packet1.setTimestampField(String.valueOf(timestamp));
        packet1.setTimestampFormat("String");

        HPacketField field1 = new HPacketField();
        field1.setPacket(packet1);
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(21.52d);

        HPacketField field2 = new HPacketField();
        field2.setPacket(packet1);
        field2.setName("humidity");
        field2.setDescription("Humidity");
        field2.setType(HPacketFieldType.DOUBLE);
        field2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field2.setValue(42.75);

        packet1.setFields(new HashSet<>() {
            {
                add(field1);
                add(field2);
            }
        });

        HPacket packet2 = new HPacket();
        packet2.setDevice(hdevice);
        packet2.setVersion("1.0");
        packet2.setName("GPS data");
        packet2.setType(HPacketType.OUTPUT);
        packet2.setFormat(HPacketFormat.JSON);
        packet2.setSerialization(HPacketSerialization.AVRO);

        packet2.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp2 = new Date();
        packet2.setTimestampField(String.valueOf(timestamp2));
        packet2.setTimestampFormat("String");

        HPacketField field3 = new HPacketField();
        field3.setPacket(packet2);
        field3.setName("gps");
        field3.setDescription("GPS");
        field3.setType(HPacketFieldType.OBJECT);
        field3.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
        HPacketField field3_1 = new HPacketField();
        field3_1.setName("longitude");
        field3_1.setDescription("GPS Longitude");
        field3_1.setType(HPacketFieldType.DOUBLE);
        field3_1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field3_1.setParentField(field3);
        field3_1.setValue(48.243d);
        HPacketField field3_2 = new HPacketField();
        field3_2.setName("latitude");
        field3_2.setDescription("GPS Latitude");
        field3_2.setType(HPacketFieldType.DOUBLE);
        field3_2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field3_2.setParentField(field3);
        field3_2.setValue(38.123d);

        HashSet<HPacketField> gpsFields = new HashSet<>();
        gpsFields.add(field3_1);
        gpsFields.add(field3_2);
        field3.setInnerFields(gpsFields);

        packet2.setFields(new HashSet<>() {
            {
                add(field3);
            }
        });

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePacket1 = hPacketRestApi.saveHPacket(packet1);
        Assert.assertEquals(200, restResponsePacket1.getStatus());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePacket2 = hPacketRestApi.saveHPacket(packet2);
        Assert.assertEquals(200, restResponsePacket2.getStatus());

        Rule rule1 = new Rule();
        rule1.setName("Add category rule 1");
        rule1.setDescription("Rule description");
        rule1.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule1.setActions(actions);
        rule1.setProject(hproject);
        rule1.setRuleDefinition(field1.getId() + " >= 23 AND " + field2.getId() + " > 36");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponseRule = ruleEngineRestApi.saveRule(rule1);
        Assert.assertEquals(200, restResponseRule.getStatus());

        StringBuilder header = new StringBuilder();
        header.append("package ").append(rule1.getType().getDroolsPackage()).append(";\n\n")
                .append("import it.acsoftware.hyperiot.hpacket.model.HPacket;\n")
                .append("import it.acsoftware.hyperiot.rule.model.actions.RuleAction;\n").append("\n")
                .append("global it.acsoftware.hyperiot.hpacket.model.HPacket packet;\n\n")
                .append("dialect  \"mvel\"\n\n").append(rule1.droolsDefinition());
        System.out.println(header);

        Rule rule2 = new Rule();
        rule2.setName("Add category rule 2");
        rule2.setDescription("Rule description");
        rule2.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action2 = new AddCategoryRuleAction();
        action2.setCategoryIds(new long[]{123});
        List<RuleAction> actions2 = new ArrayList<>();
        actions2.add(action2);
        rule2.setActions(actions2);
        rule2.setProject(hproject);
        rule1.setRuleDefinition(field1.getId() + " >= 23 AND " + field2.getId() + " > 36");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponseRule2 = ruleEngineRestApi.saveRule(rule2);
        Assert.assertEquals(200, restResponseRule2.getStatus());

        StringBuilder header2 = new StringBuilder();
        header2.append("package ").append(rule2.getType().getDroolsPackage()).append(";\n\n")
                .append("import it.acsoftware.hyperiot.hpacket.model.HPacket;\n")
                .append("import it.acsoftware.hyperiot.rule.model.actions.RuleAction;\n").append("\n")
                .append("global it.acsoftware.hyperiot.hpacket.model.HPacket packet;\n\n")
                .append("dialect  \"mvel\"\n\n").append(rule2.droolsDefinition());
        System.out.println(header2);


//		RuleEngine engine = new RuleEngine(header.toString());
//		engine.check(packet1);
    }

    @Test
    public void test28_findAllRulePaginatedShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, hadmin find all Rules with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 8;
        int page = 1;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Rule rule = createRule(hproject, null);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
            
            rules.add(rule);
        }
        Assert.assertEquals(defaultDelta, rules.size());
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(delta, listRules.getResults().size());
        Assert.assertEquals(delta, listRules.getDelta());
        Assert.assertEquals(page, listRules.getCurrentPage());
        Assert.assertEquals(page + 1, listRules.getNextPage());
        // delta is 8, page is 1: 10 entities stored in database
        Assert.assertEquals(2, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponsePage2 = ruleEngineRestApi.findAllRulePaginated(delta, 2);
        HyperIoTPaginableResult<Rule> listRulesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listRulesPage2.getResults().size());
        Assert.assertEquals(delta, listRulesPage2.getDelta());
        Assert.assertEquals(page + 1, listRulesPage2.getCurrentPage());
        Assert.assertEquals(page, listRulesPage2.getNextPage());
        // delta is 8, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listRulesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test29_findAllRulePaginatedShouldWorkIfDeltaAndPageAreNull() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, hadmin find all Rules with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Rule rule = createRule(hproject, null);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
            
            rules.add(rule);
        }
        Assert.assertEquals(defaultDelta, rules.size());
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listRules.getResults().size());
        Assert.assertEquals(defaultDelta, listRules.getDelta());
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test30_findAllRulePaginatedShouldWorkIfDeltaIsLowerThanZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, hadmin find all Rules with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 2;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 13;
        for (int i = 0; i < numbEntities; i++) {
            Rule rule = createRule(hproject, null);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
            
            rules.add(rule);
        }
        Assert.assertEquals(numbEntities, rules.size());
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listRules.getResults().size());
        Assert.assertEquals(defaultDelta, listRules.getDelta());
        Assert.assertEquals(page, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // default delta is 10, page is 2: 13 entities stored in database
        Assert.assertEquals(2, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponsePage1 = ruleEngineRestApi.findAllRulePaginated(delta, 1);
        HyperIoTPaginableResult<Rule> listRulesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listRulesPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listRulesPage1.getDelta());
        Assert.assertEquals(defaultPage, listRulesPage1.getCurrentPage());
        Assert.assertEquals(page, listRulesPage1.getNextPage());
        // default delta is 10, page is 1: 13 entities stored in database
        Assert.assertEquals(2, listRulesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test31_findAllRulePaginatedShouldWorkIfDeltaIsZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, hadmin find all Rules with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 3;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 26;
        for (int i = 0; i < numbEntities; i++) {
            Rule rule = createRule(hproject, null);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
            
            rules.add(rule);
        }
        Assert.assertEquals(numbEntities, rules.size());
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(numbEntities - (defaultDelta * 2), listRules.getResults().size());
        Assert.assertEquals(defaultDelta, listRules.getDelta());
        Assert.assertEquals(page, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage, listRules.getNextPage());
        // default delta is 10, page is 3: 26 entities stored in database
        Assert.assertEquals(3, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponsePage1 = ruleEngineRestApi.findAllRulePaginated(delta, 1);
        HyperIoTPaginableResult<Rule> listRulesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listRulesPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listRulesPage1.getDelta());
        Assert.assertEquals(defaultPage, listRulesPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listRulesPage1.getNextPage());
        // default delta is 10, page is 1: 26 entities stored in database
        Assert.assertEquals(3, listRulesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponsePage2 = ruleEngineRestApi.findAllRulePaginated(delta, 2);
        HyperIoTPaginableResult<Rule> listRulesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listRulesPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listRulesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listRulesPage2.getCurrentPage());
        Assert.assertEquals(page, listRulesPage2.getNextPage());
        // default delta is 10, page is 2: 26 entities stored in database
        Assert.assertEquals(3, listRulesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test32_findAllRulePaginatedShouldWorkIfPageIsLowerThanZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, hadmin find all Rules with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 7;
        int page = -1;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Rule rule = createRule(hproject, null);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
            
            rules.add(rule);
        }
        Assert.assertEquals(defaultDelta, rules.size());
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(delta, listRules.getResults().size());
        Assert.assertEquals(delta, listRules.getDelta());
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listRules.getNextPage());
        // delta is 7, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponsePage2 = ruleEngineRestApi.findAllRulePaginated(delta, 2);
        HyperIoTPaginableResult<Rule> listRulesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listRulesPage2.getResults().size());
        Assert.assertEquals(delta, listRulesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listRulesPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listRulesPage2.getNextPage());
        // delta is 7, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listRulesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test33_findAllRulePaginatedShouldWorkIfPageIsZero() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, hadmin find all Rules with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 4;
        int page = 0;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Rule> rules = new ArrayList<>();
        int numbEntities = 7;
        for (int i = 0; i < numbEntities; i++) {
            Rule rule = createRule(hproject, null);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
            
            rules.add(rule);
        }
        Assert.assertEquals(numbEntities, rules.size());
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(delta, listRules.getResults().size());
        Assert.assertEquals(delta, listRules.getDelta());
        Assert.assertEquals(defaultPage, listRules.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listRules.getNextPage());
        // delta is 4, default page is 1: 7 entities stored in database
        Assert.assertEquals(2, listRules.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponsePage2 = ruleEngineRestApi.findAllRulePaginated(delta, 2);
        HyperIoTPaginableResult<Rule> listRulesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertFalse(listRulesPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listRulesPage2.getResults().size());
        Assert.assertEquals(delta, listRulesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listRulesPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listRulesPage2.getNextPage());
        // delta is 4, page is 2: 7 entities stored in database
        Assert.assertEquals(2, listRulesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test34_findAllRulePaginatedShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries to find all RuleEngine with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test35_saveRuleEngineShouldFailIfEntityIsDuplicated() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to save Rule with the following call saveRule,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        Rule duplicateRule = new Rule();
        duplicateRule.setName(rule.getName());
        duplicateRule.setDescription("entity is duplicated");
        duplicateRule.setType(RuleType.ENRICHMENT);
        duplicateRule.setRuleDefinition("temperature >= 23 AND humidity > 36");
        duplicateRule.setProject(rule.getProject());

        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{456});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        duplicateRule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test36_updateRuleEngineShouldFailIfEntityIsDuplicated() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        Rule duplicateRule = createRule(hproject, null);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(hproject.getId(), duplicateRule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), duplicateRule.getProject().getUser().getId());

        duplicateRule.setName(rule.getName());
        duplicateRule.setProject(rule.getProject());

        this.impersonateUser(ruleEngineRestApi, adminUser);
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
    public void test37_updateRuleEngineShouldFailIfEntityNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to update Rule with the following call updateRule,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        // entity isn't stored in database
        Rule rule = new Rule();
        rule.setName("Rule Not Found");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test38_findAllRuleByProjectIdShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin find all Rule by projectId with the following call findAllRuleByProjectId
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
        List<Rule> listRulesByProjectId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRulesByProjectId.isEmpty());
        Assert.assertEquals(1, listRulesByProjectId.size());
        boolean ruleFound = false;
        for (Rule r : listRulesByProjectId) {
            if (rule.getId() == r.getId()) {
                Assert.assertEquals(hproject.getId(), r.getProject().getId());
                Assert.assertEquals(adminUser.getId(), r.getProject().getUser().getId());
                
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test39_findAllRuleByProjectIdShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries find all Rule by projectId,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Rule rule = createRule(hproject, null);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());
        
        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test40_findAllRuleByProjectIdShouldWorkIfListRulesIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin find all Rule by projectId with the following call findAllRuleByProjectId
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
        List<Rule> listRulesByProjectId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertTrue(listRulesByProjectId.isEmpty());
        Assert.assertEquals(0, listRulesByProjectId.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test41_findAllRuleByProjectIdShouldFailIfHProjectNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to finds all Rule by projectId with the following
        // call findAllRuleByProjectId, but HProject not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test42_findAllRuleByPacketIdShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin find all Rule by packetId with the following call findAllRuleByPacketId
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRule(hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(hpacket.getId());
        List<Rule> listRulesByPacketId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRulesByPacketId.isEmpty());
        Assert.assertEquals(1, listRulesByPacketId.size());
        boolean ruleFound = false;
        for (Rule r : listRulesByPacketId) {
            if (rule.getId() == r.getId()) {
                Assert.assertEquals(hproject.getId(), r.getProject().getId());
                Assert.assertEquals(adminUser.getId(), r.getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test43_findAllRuleByPacketIdShouldFailIfNotLogged() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // the following call tries find all Rule by packetId,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRule(hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(adminUser.getId(), rule.getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, null);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test44_findAllRuleByPacketIdShouldWorkIfListRulesIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin find all Rule by packetId with the following call findAllRuleByPacketId
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacket(hdevice);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(hpacket.getId());
        List<Rule> listRulesByPacketId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertTrue(listRulesByPacketId.isEmpty());
        Assert.assertEquals(0, listRulesByPacketId.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test45_findAllRuleByPacketIdShouldFailIfHPacketNotFound() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin tries to finds all Rule by packetId with the following
        // call findAllRuleByPacketId, but HPacket not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test46_findAllRuleEngineShouldWorkIfListIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // hadmin find all Rule with the following call findAllRule
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.findAllRule();
        List<Rule> listRules = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertTrue(listRules.isEmpty());
        Assert.assertEquals(0, listRules.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test47_findAllRulePaginatedShouldWorkIfListIsEmpty() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, hadmin find all Rules with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(ruleEngineRestApi, adminUser);
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


    @Test
    public void test50_fourierTransformRuleAction() {
        //String features = executeCommand("feature:list -i");
        //System.out.println(features);
        //System.out.println(executeCommand("package:exports | grep org.kie"));
        //System.out.println(executeCommand("package:exports | grep org.drools"));
        //System.out.println(executeCommand("feature:list | grep drools"));
        //System.out.println(executeCommand("feature:list | grep kie"));
        HPacket packet = new HPacket();
        packet.setName("pippo");
        Set<HPacketField> fields = new HashSet<>();

        int dataLength = 2048; // 3200 for DFT, use 2048 (2^11) for FFT
        // Set input data array
        //double [] accelT = new double[dataLength];
        //double [] accelX = new double[dataLength];
        //double [] accelY = new double[dataLength];
        //double [] accelZ = new double[dataLength];
        ArrayList<Double> accelX = new ArrayList<>();

        // read sample data
        try {
            InputStreamReader fileStream = new InputStreamReader(RuleEngine.class.getClassLoader().getResourceAsStream("DFT_accel_data.txt"));
            BufferedReader br = new BufferedReader(fileStream);
            String row;
            int i = 0;
            while (i < dataLength && (row = br.readLine()) != null) {
                String[] cols = row.split("\t");
                //accelT[i] = Double.parseDouble(cols[0]);
                //accelX[i] = Double.parseDouble(cols[1]);
                //accelY[i] = Double.parseDouble(cols[2]);
                //accelZ[i] = Double.parseDouble(cols[3]);
                accelX.add(Double.parseDouble(cols[1]));
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        HPacketField x = new HPacketField();
        x.setId(1000);
        x.setName("x");
        x.setType(HPacketFieldType.OBJECT);
        x.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
        x.setValue(accelX);

        fields.add(x);

        // prepare output fields
        HPacketField otx = new HPacketField();
        otx.setId(2000);
        otx.setName("tx");
        otx.setType(HPacketFieldType.OBJECT);
        otx.setMultiplicity(HPacketFieldMultiplicity.ARRAY);

        fields.add(otx);

        long outputFieldId = 2000;
        String outputFieldName = "tx";

        packet.setFields(fields);

        // TODO: packet should be saved in order to make this test work properly

        FourierTransformRuleAction action = new FourierTransformRuleAction();
        action.setInputFieldId(x.getId());
        action.setOutputFieldId(outputFieldId);
        action.setOutputFieldName(outputFieldName);

        // NOTE: the following 3 lines are for using FFT algorithm
        action.setTransformMethod(FourierTransformRuleAction.MethodType.FAST);
        action.setFftNormalization(FourierTransformRuleAction.FftNormalization.STANDARD);
        action.setTransformType(FourierTransformRuleAction.FftTransformType.FORWARD);
        // NOTE: the following line is for using DFT algorithm (comment the 3 lines above before uncommenting the following one - mutually exclusive)
        //action.setTransformMethod(FourierTransformRuleAction.MethodType.DISCRETE);

        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);

        Rule r = new Rule();
        r.setName("Fourier Transform rule");
        r.setActions(actions);
        r.setType(RuleType.ENRICHMENT);
        // 1 == 1  ->  always execute the rule action
        //r.setRuleDefinition("1 == 1");

        String ruleBody = r.droolsDefinition();
        String ruleDroolsCode = "package " + r.getType().getDroolsPackage() + ";\n" +
                "import it.acsoftware.hyperiot.hpacket.model.HPacket;\n" +
                "import it.acsoftware.hyperiot.rule.model.actions.RuleAction;\n" +
                "import it.acsoftware.hyperiot.rule.service.actions.FourierTransformRuleAction;\n" +
                "import java.util.ArrayList;\n" +
                "global java.util.ArrayList actions;\n" +
                "dialect  \"mvel\"\n" + ruleBody;

        System.out.println(ruleDroolsCode);

        RuleEngine ruleEngine = new RuleEngine(Collections.singletonList(ruleDroolsCode), 0);
        long startTime = System.nanoTime();
        ruleEngine.check(packet,System.currentTimeMillis());
        long elapsedTime = System.nanoTime() - startTime;

        packet.getFields().forEach(f -> {
            //double[] valueArray = (double[])f.getValue();
            //System.out.println(f.getName() + " = " + Arrays.toString(valueArray));
            System.out.println(f.getName() + " = " + f.getValue());
        });

        System.out.println("\n\n");

        ArrayList<Double> tx = (ArrayList<Double>) packet.getFieldValue("tx");
        if (tx != null) {
            for (int i = 0; i < tx.size(); i++) {
                System.out.println(tx.get(i));
            }
        }

        System.out.println("\nElapsed time: " + elapsedTime + " nanoseconds");
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


    private HProject createHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Project of user: " + adminUser.getUsername());
        hproject.setUser((HUser) adminUser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals("Project of user: " + adminUser.getUsername(),
                ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
    }

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("ACSoftware");
        hdevice.setDescription("Property of: " + hproject.getUser().getUsername());
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals("ACSoftware",
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals("Property of: " + hproject.getUser().getUsername(),
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
        Assert.assertEquals(adminUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
    }

    private HPacket createHPacket(HDevice hdevice) {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

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

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        return hpacket;
    }

    private Rule createRule(HProject hproject, HPacket hpacket) {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Rule) restResponse.getEntity()).getId());
        Assert.assertEquals(rule.getName(), ((Rule) restResponse.getEntity()).getName());
        Assert.assertEquals("Rule defined by huser: " + hproject.getUser().getUsername(), ((Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals(RuleType.EVENT, ((Rule) restResponse.getEntity()).getType());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events", ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("temperature >= 23 AND humidity > 36", ((Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals(hproject.getId(), ((Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Rule) restResponse.getEntity()).getProject().getUser().getId());
        return rule;
    }

    @After
    public void afterTest() {
        // Remove all projects and delete in cascade all associated entities (RuleEngine) in every tests
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.findAllHProject();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        if (!listHProjects.isEmpty()) {
            Assert.assertFalse(listHProjects.isEmpty());
            for (HProject project : listHProjects) {
                this.impersonateUser(hprojectRestService, adminUser);
                Response restResponse1 = hprojectRestService.deleteHProject(project.getId());
                Assert.assertEquals(200, restResponse1.getStatus());
            }
        }
//        String sqlHProject = "select * from hproject";
//        String resultHProject = executeCommand("jdbc:query hyperiot " + sqlHProject);
//        System.out.println(resultHProject);
//
//        String sqlRule = "select * from rule";
//        String resultRule = executeCommand("jdbc:query hyperiot " + sqlRule);
//        System.out.println(resultRule);
    }

}

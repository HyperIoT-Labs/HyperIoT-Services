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

package it.acsoftware.hyperiot.hproject.algorithm.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.algorithm.service.rest.AlgorithmRestApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmUtil;
import it.acsoftware.hyperiot.hproject.algorithm.job.HProjectAlgorithmJob;
import it.acsoftware.hyperiot.hproject.algorithm.model.*;
import it.acsoftware.hyperiot.hproject.algorithm.service.rest.HProjectAlgorithmRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
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
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static it.acsoftware.hyperiot.hproject.algorithm.test.HyperIoTHProjectAlgorithmConfiguration.*;


/**
 * @author Aristide Cittadino Interface component for HProjectAlgorithm System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHProjectAlgorithmRestTest extends KarafTestSupport {
    public static final String DEFAULT_ALG_VAL = "{\"input\":[],\"output\":[],\"customConfig\":null}";
    /*
    TODO
         When we save an HProjectAlgorithm we can specificy that the project is active.
         What happen when we save an HProjectAlgorithm with this settings ?
         When we update an HProjectAlgorithm we can load the HProject related to it from db
         The autosetting of the algorithm must not be supported because user can associate a different algorithm to the HProject but
         we must check that find all of the algorithm pass throw AlgorithmApi such that we trigger security verification.
         We can add custom validation to cronExpressionParameter.
         We can add custom validation on HProjectAlgorithms config .(Assertion relatie to the json contained in the field).
     */


    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    private HyperIoTAction getHyperIoTAction(String resourceName,
                                             HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder
                .createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }


    @Test
    public void test000_hyperIoTFrameworkShouldBeInstalled() {
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
    public void test001_hprojectAlgorithmModuleShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call checkModuleWorking checks if HProjectAlgorithm module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser user = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hProjectAlgorithmRestService, user);
        Response restResponse = hProjectAlgorithmRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProjectAlgorithm Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_hprojectAlgorithmModuleShouldWorkIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call checkModuleWorking checks if HProjectAlgorithm module working
        // correctly, if HUser not logged
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.checkModuleWorking();
        Assert.assertNotNull(hProjectAlgorithmRestService);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProjectAlgorithm Module works!", restResponse.getEntity());
    }

    @Test
    public void test003_saveHProjectAlgorithmShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin save HProject with the following call saveHProjectAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String cronExpression = hProjectAlgorithmTemplate.getCronExpression();
        String config = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(restResponse, project, algorithm, hprojectAlgorithmName, config, cronExpression);
    }

    @Test
    public void test004_saveHProjectAlgorithmShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to save HProjectAlgorithm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(HyperIoTHProjectAlgorithmConfiguration.hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test005_saveHProjectAlgorithmShouldFailIfNameIsEmpty() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        hProjectAlgorithmTemplate.setName("");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and empty value validation constraint
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getInvalidValue());
    }

    @Test
    public void test006_saveHProjectAlgorithmShouldFailIfNameIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        hProjectAlgorithmTemplate.setName(null);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test007_saveHProjectAlgorithmShouldFailIfNameIsMaliciousCode() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        hProjectAlgorithmTemplate.setName("<script>console.log()</script>");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and malicious code constraint
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getInvalidValue());
    }

    @Test
    public void test008_saveHProjectAlgorithmShouldFailIfNameNotMatchPattern() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but name not match expected Pattern
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set one of the possible invalid value
        hProjectAlgorithmTemplate.setName("??????????????????");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test009_saveHProjectAlgorithmShouldFailIfConfigIsEmpty() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but config is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set config with empty value
        hProjectAlgorithmTemplate.setConfig("");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test010_saveHProjectAlgorithmShouldFailIfConfigIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but config is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set config with null value
        hProjectAlgorithmTemplate.setConfig(null);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test011_saveHProjectAlgorithmShouldFailIfConfigIsMaliciousValue() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but config is malicious value
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set config with malicious value
        hProjectAlgorithmTemplate.setConfig("<script>console.log()</script>");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test012_saveHProjectAlgorithmShouldFailIfCronExpressionIsEmpty() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but cron expression is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set cronExpression with empty value
        hProjectAlgorithmTemplate.setCronExpression("");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getCronExpression(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test013_saveHProjectAlgorithmShouldFailIfCronExpressionIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but cron expression is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set cronexpression with null value
        hProjectAlgorithmTemplate.setCronExpression(null);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test014_saveHProjectAlgorithmShouldFailIfCronExpressionIsMaliciousValue() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but cron expression is malicious value
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set cron expression with malicious value
        hProjectAlgorithmTemplate.setCronExpression("<script>console.log()</script>");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getCronExpression(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test015_saveHProjectAlgorithmShouldFailIfProjectIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but project is null
        //Set hproject throw NullPointerException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set project with null value
        boolean triggerNullPointer = false;
        try {
            hProjectAlgorithmTemplate.setProject(null);
        } catch (NullPointerException exc) {
            triggerNullPointer = true;
        }
        Assert.assertTrue(triggerNullPointer);
    }

    @Test
    public void test016_saveHProjectAlgorithmShouldFailIfProjectIsNotPersisted() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but project is not persisted
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Associate a project that is not persisted on hprojectalgorithm
        HProject fakeProject = new HProject();
        fakeProject.setUserOwner(adminUser);
        fakeProject.setName("projectName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hProjectAlgorithmTemplate.setProject(fakeProject);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test017_saveHProjectAlgorithmShouldFailIfAlgorithmIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but algorithm is null
        //Set algorithm throw NullPointerException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Set algorithm with null value.
        boolean triggerNullPointer = false;
        try {
            hProjectAlgorithmTemplate.setAlgorithm(null);
        } catch (NullPointerException exc) {
            triggerNullPointer = true;
        }
        Assert.assertTrue(triggerNullPointer);

    }

    @Test
    public void test018_saveHProjectAlgorithmShouldFailIfAlgorithmIsNotPersisted() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but algorithm related to hproject algorithm  is not persisted
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        //Associate a project that is not persisted on hprojectalgorithm
        Algorithm algorithm1 = createAlgorithmWithInputAndOutputField();
        algorithm1.setId(0);
        hProjectAlgorithmTemplate.setAlgorithm(algorithm1);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test019_findHProjectAlgorithmShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin find HProjectAlgorithm with the following call findHProjectAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String cronExpression = hProjectAlgorithmTemplate.getCronExpression();
        String config = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(restResponse, project, algorithm, hprojectAlgorithmName, config, cronExpression);
        HProjectAlgorithm savedHProjectAlgorithm = (HProjectAlgorithm) restResponse.getEntity();

        Response findResponse = hProjectAlgorithmRestService.findHProjectAlgorithm(savedHProjectAlgorithm.getId());
        Assert.assertEquals(200, findResponse.getStatus());
        Assert.assertEquals(((HProjectAlgorithm) findResponse.getEntity()).getId(), savedHProjectAlgorithm.getId());
    }

    @Test
    public void test020_findHProjectAlgorithmShouldFailIfEntityNotFound() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find HProjectAlgorithm,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.findHProjectAlgorithm(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test021_findHProjectAlgorithmShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find HProjectAlgorithm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String cronExpression = hProjectAlgorithmTemplate.getCronExpression();
        String config = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(restResponse, project, algorithm, hprojectAlgorithmName, config, cronExpression);
        HProjectAlgorithm savedHProjectAlgorithm = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertNotEquals(0, savedHProjectAlgorithm.getId());
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response findResponse = hProjectAlgorithmRestService.findHProjectAlgorithm(savedHProjectAlgorithm.getId());
        Assert.assertEquals(403, findResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) findResponse.getEntity()).getType());
    }

    @Test
    public void test022_findAllHProjectShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin find all HProjectAlgorithm with the following call findAllHProjectAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String cronExpression = hProjectAlgorithmTemplate.getCronExpression();
        String config = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(restResponse, project, algorithm, hprojectAlgorithmName, config, cronExpression);
        HProjectAlgorithm savedHProjectAlgorithm = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertNotEquals(0, savedHProjectAlgorithm.getId());


        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response findAllHProjectAlgorithmResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithm();
        Assert.assertEquals(200, findAllHProjectAlgorithmResponse.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList = findAllHProjectAlgorithmResponse.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertFalse(hProjectAlgorithmsList.isEmpty());
        Assert.assertEquals(1, hProjectAlgorithmsList.size());
        boolean hprojectFound = false;
        for (HProjectAlgorithm projectAlgorithm : hProjectAlgorithmsList) {
            if (savedHProjectAlgorithm.getId() == projectAlgorithm.getId()) {
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
    }

    @Test
    public void test023_findAllHProjectAlgorithmShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find all HProjectAlgorithm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithm();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test024_updateHProjectAlgorithmShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin update HProjectAlgorithm with the following call updateHProjectAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        String projectAlgorithmNewName = projectAlgorithm.getName().concat("newName");
        projectAlgorithm.setName(projectAlgorithmNewName);
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(projectAlgorithm.getEntityVersion() + 1,
                (((HProjectAlgorithm) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals(projectAlgorithmNewName,
                (((HProjectAlgorithm) restResponse.getEntity()).getName()));
        Assert.assertEquals(projectAlgorithm.getId(), ((HProjectAlgorithm) restResponse.getEntity()).getId());
    }

    @Test
    public void test025_updateHProjectAlgorithmShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        String projectAlgorithmNewName = projectAlgorithm.getName().concat("newName");
        projectAlgorithm.setName(projectAlgorithmNewName);
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test026_updateHProjectAlgorithmShouldFailIfEntityNotFound() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but HProjectNotExist
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        String projectAlgorithmNewName = projectAlgorithm.getName().concat("newName");
        projectAlgorithm.setName(projectAlgorithmNewName);
        projectAlgorithm.setId(0);
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test027_updateHProjectAlgorithmShouldFailIfDuplicateEntity() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but exist another HProjectAlgorithm such that the name field is the same.
        // response status code '409' HyperIoTDuplicatedEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        HProjectAlgorithm otherProjectAlgorithm = createHProjectAlgorithm();
        otherProjectAlgorithm.setName(projectAlgorithm.getName());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(otherProjectAlgorithm);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test028_updateHProjectAlgorithmShouldFailIfNameIsEmpty() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but name is Empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setName("");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and empty value validation constraint
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
        Assert.assertEquals(hProjectAlgorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getInvalidValue());
    }

    @Test
    public void test029_updateHProjectAlgorithmShouldFailIfNameIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but name is Null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setName(null);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        //Invalid pattern and empty value validation constraint
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test030_updateHProjectAlgorithmShouldFailIfNameIsMaliciousCode() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setName(defaultMaliciousValue);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and malicious code constraint
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
        Assert.assertEquals(hProjectAlgorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getInvalidValue());
    }

    @Test
    public void test031_updateHProjectAlgorithmShouldFailIfNameNotMatchPattern() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but name  not match expectedPattern
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setName("?????????????");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test032_updateHProjectAlgorithmShouldFailIfConfigIsEmpty() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but config is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setConfig("");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithm.getConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test033_updateHProjectAlgorithmShouldFailIfConfigIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but config is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setConfig(null);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test034_updateHProjectAlgorithmShouldFailIfConfigIsMaliciousValue() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but config is malicious value
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setConfig(defaultMaliciousValue);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-config", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithm.getConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test035_updateHProjectAlgorithmShouldFailIfCronExpressionIsEmpty() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but cronExpression is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setCronExpression("");
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithm.getCronExpression(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test036_updateHProjectAlgorithmShouldFailIfCronExpressionIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but cronExpression is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setCronExpression(null);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test037_updateHProjectAlgorithmShouldFailIfCronExpressionIsMaliciousValue() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but cronExpression is malicious value
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        hProjectAlgorithm.setCronExpression(defaultMaliciousValue);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithm.getCronExpression(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test038_updateHProjectAlgorithmShouldFailIfAlgorithmIsNotPersisted() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm,
        // but Algorithm related to HProjectAlgorithm is not persisted
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Algorithm fakeAlgorithm = createAlgorithmWithInputAndOutputField();
        fakeAlgorithm.setId(0);
        hProjectAlgorithm.setAlgorithm(fakeAlgorithm);
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.updateHProjectAlgorithm(hProjectAlgorithm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test039_deleteHProjectAlgorithmShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin delete HProjectAlgorithm with the following call deleteHProject
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.deleteHProjectAlgorithm(hProjectAlgorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test040_deleteHProjectAlgorithmShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to delete HProjectAlgorithm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.deleteHProjectAlgorithm(hProjectAlgorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test041_deleteHProjectAlgorithmShouldFailIfEntityNotFound() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to delete HProjectAlgorithm,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.deleteHProjectAlgorithm(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test042_findAllHProjectAlgorithmPaginatedShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmPaginated, hadmin finds all
        // HProjectsAlgorithms with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = 2;
        List<HProjectAlgorithm> hProjectAlgorithms = new ArrayList<>();
        int numbEntities = 9;
        for (int i = 0; i < numbEntities; i++) {
            HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
            Assert.assertNotEquals(0, hProjectAlgorithm.getId());
            hProjectAlgorithms.add(hProjectAlgorithm);
        }
        Assert.assertEquals(numbEntities, hProjectAlgorithms.size());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithms.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listHProjectAlgorithms.getResults().size());
        Assert.assertEquals(delta, listHProjectAlgorithms.getDelta());
        Assert.assertEquals(page, listHProjectAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectAlgorithms.getNextPage());
        // delta is 5, page 2: 9 entities stored in database
        Assert.assertEquals(2, listHProjectAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponsePage1 = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, 1);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithmsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithmsPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjectAlgorithmsPage1.getResults().size());
        Assert.assertEquals(delta, listHProjectAlgorithmsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectAlgorithmsPage1.getCurrentPage());
        Assert.assertEquals(page, listHProjectAlgorithmsPage1.getNextPage());
        // delta is 5, page is 1: 9 entities stored in database
        Assert.assertEquals(2, listHProjectAlgorithmsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test043_findAllHProjectAlgorithmPaginatedShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find all HProjectAlgorithms with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(null, null);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test044_findAllHProjectAlgorithmPaginatedShouldWorkIfDeltaAndPageAreNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmPaginated, hadmin find all HProjectAlgorithms with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        List<HProjectAlgorithm> hProjectAlgorithms = new ArrayList<>();
        int numbEntities = 6;
        for (int i = 0; i < numbEntities; i++) {
            HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
            Assert.assertNotEquals(0, hProjectAlgorithm.getId());
            hProjectAlgorithms.add(hProjectAlgorithm);
        }
        Assert.assertEquals(numbEntities, hProjectAlgorithms.size());
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.findAllHProjectAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listHProjects.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjects.getDelta());
        Assert.assertEquals(defaultPage, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // default delta is 10, default page is 1: 6 entities stored in database
        Assert.assertEquals(1, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test045_findAllHProjectAlgorithmPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmPaginated, hadmin find all HProjectAlgorithms with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 1;
        List<HProjectAlgorithm> hProjectAlgorithms = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
            Assert.assertNotEquals(0, hProjectAlgorithm.getId());
            hProjectAlgorithms.add(hProjectAlgorithm);
        }
        Assert.assertEquals(defaultDelta, hProjectAlgorithms.size());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponsePage1 = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithmsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithmsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithmsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithmsPage1.getDelta());
        Assert.assertEquals(page, listHProjectAlgorithmsPage1.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectAlgorithmsPage1.getNextPage());
        // default delta is 10, page is 1: 10 entities stored in database
        Assert.assertEquals(1, listHProjectAlgorithmsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        // checks with page = 2 (no result showed)
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponsePage2 = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, 2);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithmsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertTrue(listHProjectAlgorithmsPage2.getResults().isEmpty());
        Assert.assertEquals(0, listHProjectAlgorithmsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithmsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHProjectAlgorithmsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectAlgorithmsPage2.getNextPage());
        // default delta is 10, page is 2: 10 entities stored in database
        Assert.assertEquals(1, listHProjectAlgorithmsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test046_findAllHProjectAlgorithmPaginatedShouldWorkIfDeltaIsZero() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmPaginated, hadmin find all HProjectAlgorithms with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 3;
        List<HProjectAlgorithm> hProjectAlgorithms = new ArrayList<>();
        int numEntities = 25;
        for (int i = 0; i < numEntities; i++) {
            HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
            Assert.assertNotEquals(0, hProjectAlgorithm.getId());
            hProjectAlgorithms.add(hProjectAlgorithm);
        }
        Assert.assertEquals(numEntities, hProjectAlgorithms.size());
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.findAllHProjectAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithms.getResults().isEmpty());
        Assert.assertEquals(numEntities - (defaultDelta * 2), listHProjectAlgorithms.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithms.getDelta());
        Assert.assertEquals(page, listHProjectAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectAlgorithms.getNextPage());
        // because delta is 10, page is 3: 25 entities stored in database
        Assert.assertEquals(3, listHProjectAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponsePage1 = hProjectAlgorithmRestApi.findAllHProjectAlgorithmPaginated(delta, 1);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithmsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithmsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithmsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithmsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectAlgorithmsPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHProjectAlgorithmsPage1.getNextPage());
        // default delta is 10, page is 1: 25 entities stored in database
        Assert.assertEquals(3, listHProjectAlgorithmsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponsePage2 = hProjectAlgorithmRestApi.findAllHProjectAlgorithmPaginated(delta, 2);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithmsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithmsPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithmsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectAlgorithmsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHProjectAlgorithmsPage2.getCurrentPage());
        Assert.assertEquals(page, listHProjectAlgorithmsPage2.getNextPage());
        // default delta is 10, page is 2: 25 entities stored in database
        Assert.assertEquals(3, listHProjectAlgorithmsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test047_findAllHProjectAlgorithmPaginatedShouldWorkIfPageIsLowerThanZero() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmPaginated, hadmin find all HProjectsAlgorithm with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = -1;
        List<HProjectAlgorithm> hProjectAlgorithms = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
            Assert.assertNotEquals(0, hProjectAlgorithm.getId());
            hProjectAlgorithms.add(hProjectAlgorithm);
        }
        Assert.assertEquals(delta, hProjectAlgorithms.size());
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.findAllHProjectAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithms.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjectAlgorithms.getResults().size());
        Assert.assertEquals(delta, listHProjectAlgorithms.getDelta());
        Assert.assertEquals(defaultPage, listHProjectAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectAlgorithms.getNextPage());
        // delta is 5, default page 1: 5 entities stored in database
        Assert.assertEquals(1, listHProjectAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test048_findAllHProjectAlgorithmPaginatedShouldWorkIfPageIsZero() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmsPaginated, hadmin find all HProjectAlgorithms with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 8;
        int page = 0;
        List<HProjectAlgorithm> hProjectAlgorithms = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
            Assert.assertNotEquals(0, hProjectAlgorithm.getId());
            hProjectAlgorithms.add(hProjectAlgorithm);
        }
        Assert.assertEquals(defaultDelta, hProjectAlgorithms.size());
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.findAllHProjectAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithms.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjectAlgorithms.getResults().size());
        Assert.assertEquals(delta, listHProjectAlgorithms.getDelta());
        Assert.assertEquals(defaultPage, listHProjectAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHProjectAlgorithms.getNextPage());
        // delta is 8, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listHProjectAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponsePage2 = hProjectAlgorithmRestApi.findAllHProjectAlgorithmPaginated(delta, 2);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithmsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithmsPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listHProjectAlgorithmsPage2.getResults().size());
        Assert.assertEquals(delta, listHProjectAlgorithmsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHProjectAlgorithmsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectAlgorithmsPage2.getNextPage());
        // delta is 8, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listHProjectAlgorithmsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test049_findByHProjectIdShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin find HProjectAlgorithm with the following call findByHProjectId
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.findByHProjectId(hProjectAlgorithm.getProject().getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList = restResponse.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertFalse(hProjectAlgorithmsList.isEmpty());
        Assert.assertEquals(1, hProjectAlgorithmsList.size());
        boolean hprojectFound = false;
        for (HProjectAlgorithm projectAlgorithm : hProjectAlgorithmsList) {
            if (hProjectAlgorithm.getId() == projectAlgorithm.getId()) {
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
    }

    @Test
    public void test050_findByHProjectIdShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find HProjectAlgorithm by HProjectId
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.findByHProjectId(hProjectAlgorithm.getProject().getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test051_findByHProjectIdShouldFailIfHProjectEntityNotFound() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find HProjectAlgorithm by HProjectId
        // but HProject entity not found
        // response status code '404' HyperIoTNoResultException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.findByHProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTNoResultException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test052_findByHProjectIdShouldWorkIfHProjectAlgorithmsListIsEmpty() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find HProjectAlgorithm by HProjectId (HProject's algorithms list is empty).
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hProject = createHProject();
        Assert.assertNotEquals(0, hProject.getId());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.findByHProjectId(hProject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList = restResponse.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertTrue(hProjectAlgorithmsList.isEmpty());
    }

    @Test
    public void test053_updateBaseConfigShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithm config field  with updateBaseConfig
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        String hprojectAlgorithmConfig = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmCronExpression = hProjectAlgorithmTemplate.getCronExpression();
        Response saveAlgorithmResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(saveAlgorithmResponse, project, algorithm, hprojectAlgorithmName, hprojectAlgorithmConfig, hprojectAlgorithmCronExpression);
        HProjectAlgorithm savedAlgorithm = (HProjectAlgorithm) saveAlgorithmResponse.getEntity();
        Assert.assertNotEquals(0, savedAlgorithm.getId());

        //Crea a new device with a new packet and a new field to create a new HProjectAlgorithConfig.
        HDevice anotherDevice = createHDevice(project);
        HPacket otherPacket = createHPacketAndAddHPacketField(anotherDevice, true);
        Assert.assertTrue(otherPacket.getFields() != null && otherPacket.getFields().size() == 1);
        HPacketField otherPacketField = otherPacket.getFields().iterator().next();

        //Create a new config for the algorithm
        Algorithm newAlgorithm = createAlgorithmWithInputAndOutputField();
        AlgorithmConfig newAlgorithmConfig = getAlgorithmConfig(newAlgorithm);
        Assert.assertTrue(newAlgorithmConfig.getOutput() != null && newAlgorithmConfig.getOutput().size() == 1);
        AlgorithmIOField newOutputField = newAlgorithmConfig.getOutput().get(0);
        Assert.assertTrue(newAlgorithmConfig.getInput() != null && newAlgorithmConfig.getInput().size() == 1);
        AlgorithmIOField newInputField = newAlgorithmConfig.getInput().get(0);
        MappedInput mappedInput = new MappedInput();
        mappedInput.setPacketFieldId(otherPacketField.getId());
        mappedInput.setAlgorithmInput(newInputField);
        HProjectAlgorithmInputField hProjectAlgorithmInputField = new HProjectAlgorithmInputField();
        hProjectAlgorithmInputField.setPacketId(packet.getId());
        List<MappedInput> mappedInputList = new LinkedList<>();
        mappedInputList.add(mappedInput);
        hProjectAlgorithmInputField.setMappedInputList(mappedInputList);
        HProjectAlgorithmConfig newConfig = new HProjectAlgorithmConfig();
        newConfig.setInput(new ArrayList<>());
        newConfig.setOutput(new ArrayList<>());
        newConfig.getOutput().add(newOutputField);
        newConfig.getInput().add(hProjectAlgorithmInputField);


        HProjectAlgorithmUtil hProjectAlgorithmUtil = getOsgiService(HProjectAlgorithmUtil.class);
        String expectedConfig = null;
        try {
            expectedConfig = hProjectAlgorithmUtil.getConfigString(newConfig);
        } catch (Throwable exc) {
            Assert.fail();
            throw new RuntimeException();
        }
        //Assert that we update the config.
        Assert.assertNotEquals(savedAlgorithm.getConfig(), expectedConfig);

        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.updateBaseConfig(savedAlgorithm.getId(), newConfig);
        Assert.assertEquals(200, restResponse.getStatus());
        HProjectAlgorithm entityResponse = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertEquals(entityResponse.getId(), savedAlgorithm.getId());
        //Assert that the config is how we expected.
        Assert.assertEquals(expectedConfig, entityResponse.getConfig());
    }

    @Test
    public void test054_updateBaseConfigShouldFailIfNotLogged() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithmConfig with updateBaseConfig
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.updateBaseConfig(hProjectAlgorithm.getId(), new HProjectAlgorithmConfig());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test055_updateBaseConfigShouldFailIfHProjectAlgorithmEntityNotFound() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithmConfig with updateBaseConfig
        //but HProjectAlgorithmEntityNotExist
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.updateBaseConfig(0, new HProjectAlgorithmConfig());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTNoResultException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test056_updateBaseConfigShouldFailIfHProjectAlgorithmConfigIsNull() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to update HProjectAlgorithmConfig with updateBaseConfig
        //but HProjectAlgorithmConfig is null
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.updateBaseConfig(hProjectAlgorithm.getId(), null);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test057_getAlgorithmOutputsShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find HProjectAlgorithmHBaseResult with getAlgorithmOutput
        // response status code '200' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        impersonateUser(hProjectAlgorithmRestService, adminUser);
        forceAlgorithmTableCreation(hProjectAlgorithm);
        Response restResponse = hProjectAlgorithmRestService.getAlgorithmOutputs(hProjectAlgorithm.getProject().getId(), hProjectAlgorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotNull(restResponse.getEntity());
        Assert.assertTrue(((HProjectAlgorithmHBaseResult) restResponse.getEntity()).getRows().isEmpty());
    }

    @Test
    public void test058_saveHProjectAlgorithmShouldFailIfNameIsGreaterThan255Chars() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but name is greater than 255 chars .
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        hProjectAlgorithmTemplate.setName(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and malicious code constraint
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }


    @Test
    public void test059_saveHProjectAlgorithmShouldFailIfCronExpressionIsGreaterThan255Chars() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin tries to save HProjectAlgorithm with the following call saveHProjectAlgorithm,
        // but cron expression is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        hProjectAlgorithmTemplate.setCronExpression(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hProjectAlgorithmRestApi, adminUser);
        Response restResponse = hProjectAlgorithmRestApi.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and malicious code constraint
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hProjectAlgorithmTemplate.getCronExpression(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test060_updateHProjectAlgorithmShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin update HProjectAlgorithm with the following call updateHProjectAlgorithm
        // but name is greater than 255 character
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        projectAlgorithm.setName(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and malicious code constraint
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(projectAlgorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test061_updateHProjectAlgorithmShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hadmin update HProjectAlgorithm with the following call updateHProjectAlgorithm
        // but cron expression is greater than 255 character
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        projectAlgorithm.setCronExpression(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Invalid pattern and malicious code constraint
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hprojectalgorithm-cronexpression", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(projectAlgorithm.getCronExpression(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }






    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    private String createStringFieldWithSpecifiedLenght(int length) {
        String symbol = "a";
        String field = String.format("%" + length + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(length, field.length());
        return field;
    }

    private void forceAlgorithmTableCreation(HProjectAlgorithm hProjectAlgorithm) {
        HBaseConnectorSystemApi hBaseConnectorSystemApi = getOsgiService(HBaseConnectorSystemApi.class);
        String algorithmTable = String.format("algorithm_%s", hProjectAlgorithm.getAlgorithm().getId());
        try {
            List<String> columnFamilies = new ArrayList<>();
            columnFamilies.add("value");
            hBaseConnectorSystemApi.createTable(algorithmTable, columnFamilies);
            hBaseConnectorSystemApi.tableExists(algorithmTable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AlgorithmConfig getAlgorithmConfig(Algorithm algorithm) {
        AlgorithmConfig baseConfig;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            baseConfig = objectMapper.readValue(algorithm.getBaseConfig(), AlgorithmConfig.class);
        } catch (Throwable t) {
            Assert.fail();
            throw new RuntimeException();
        }
        return baseConfig;
    }

    private Algorithm createAlgorithmWithInputAndOutputField() {
        Algorithm algorithm = createAlgorithm();
        createAlgorithmIOField(algorithm, "INPUT");
        createAlgorithmIOField(algorithm, "OUTPUT");
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAlgorithm(algorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Algorithm algorithmWithConfiguration = (Algorithm) restResponse.getEntity();
        return algorithmWithConfiguration;
    }

    private Algorithm createAlgorithm() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("Algorithm defined by huser: " + adminUser.getUsername());
        algorithm.setMainClassname(algorithmResourceName);
        algorithm.setType(AlgorithmType.STATISTICS);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Algorithm) restResponse.getEntity()).getId());
        Assert.assertEquals(algorithm.getName(), ((Algorithm) restResponse.getEntity()).getName());
        Assert.assertEquals("Algorithm defined by huser: " + adminUser.getUsername(), ((Algorithm) restResponse.getEntity()).getDescription());
        Assert.assertEquals(algorithmResourceName, ((Algorithm) restResponse.getEntity()).getMainClassname());
        Assert.assertEquals(DEFAULT_ALG_VAL, ((Algorithm) restResponse.getEntity()).getBaseConfig());
        return algorithm;
    }


    private AlgorithmIOField createAlgorithmIOField(Algorithm algorithm, String algorithmFieldType) {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        if (algorithm == null) {
            algorithm = createAlgorithm();
        }

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOFieldName " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithmIOField.setDescription("IOField description " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        if (algorithmFieldType == "INPUT") {
            algorithmIOField.setType(AlgorithmFieldType.INPUT);
            this.impersonateUser(algorithmRestApi, adminUser);
            Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
            Assert.assertEquals(200, restResponse.getStatus());
            //checks if inputField has been added in baseConfig
            String jsonInputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
            if ((((Algorithm) restResponse.getEntity()).getEntityVersion() - 1) == algorithmIOField.getId()) {
                Assert.assertTrue(
                        jsonInputField.contains(
                                "{\"id\":" + algorithmIOField.getId() + "," +
                                        "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                        "\"description\":\"" + algorithmIOField.getDescription() + "\"," +
                                        "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                        "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                        "\"type\":\"" + algorithmIOField.getType() + "\"}"
                        )
                );
            }
        }
        if (algorithmFieldType == "OUTPUT") {
            algorithmIOField.setType(AlgorithmFieldType.OUTPUT);
            this.impersonateUser(algorithmRestApi, adminUser);
            Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
            Assert.assertEquals(200, restResponse.getStatus());
            //checks if outputField has been added in baseConfig
            String jsonOutputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
            if ((((Algorithm) restResponse.getEntity()).getEntityVersion() - 1) == algorithmIOField.getId()) {
                Assert.assertTrue(
                        jsonOutputField.contains(
                                "{\"id\":" + algorithmIOField.getId() + "," +
                                        "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                        "\"description\":\"" + algorithmIOField.getDescription() + "\"," +
                                        "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                        "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                        "\"type\":\"" + algorithmIOField.getType() + "\"}"
                        )
                );
            }
        }
        if (algorithmFieldType != "INPUT" && algorithmFieldType != "OUTPUT") {
            Assert.assertEquals(0, algorithmIOField.getId());
            System.out.println("algorithmIOField is null, field not created...");
            System.out.println("allowed values: INPUT, OUTPUT");
            return null;
        }
        return algorithmIOField;
    }


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
            Permission permission = utilGrantPermission(huser, role, action, 0);
            Assert.assertNotEquals(0, permission.getId());
            Assert.assertEquals(hProjectResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
            Assert.assertEquals(action.getActionId(), permission.getActionIds());
            Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
            Assert.assertEquals(role.getId(), permission.getRole().getId());
        }
        return huser;
    }

    private Permission utilGrantPermission(HUser huser, Role role, HyperIoTAction action, long resourceId) {
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
                permission.setName(hProjectResourceName + " assigned to huser_id " + huser.getId());
                permission.setActionIds(action.getActionId());
                permission.setEntityResourceName(action.getResourceName());
                if (resourceId > 0)
                    permission.setResourceId(resourceId);
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

    private HProjectAlgorithm createHProjectAlgorithm() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String cronExpression = hProjectAlgorithmTemplate.getCronExpression();
        String config = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(restResponse, project, algorithm, hprojectAlgorithmName, config, cronExpression);
        return (HProjectAlgorithm) restResponse.getEntity();
    }

    private HProjectAlgorithm createHProjectAlgorithmTemplate(HProject project, Algorithm algorithm, HPacket packet, HPacketField packetField) {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        HProjectAlgorithm hProjectAlgorithm = new HProjectAlgorithm();
        hProjectAlgorithm.setAlgorithm(algorithm);
        hProjectAlgorithm.setProject(project);
        hProjectAlgorithm.setConfig(createHProjectAlgorithmConfig(project, algorithm, packet, packetField));
        hProjectAlgorithm.setCronExpression(createCronExpression());
        hProjectAlgorithm.setName(createHProjectAlgorithmName());
        return hProjectAlgorithm;
    }


    private void assertSavedHProjectAlgorithm(Response restResponse, HProject project, Algorithm algorithm,
                                              String hprojectAlgorithmName, String config, String cronExpression) {
        //Assert response status.
        Assert.assertEquals(200, restResponse.getStatus());
        HProjectAlgorithm entityResponse = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertNotNull(entityResponse);
        //Assert the project related to hprojectAlgorithms
        Assert.assertNotNull(entityResponse.getProject());
        Assert.assertEquals(entityResponse.getProject().getId(), project.getId());
        //Assert the algorithm related to hprojectAlgorithms
        Assert.assertNotNull(entityResponse.getAlgorithm());
        Assert.assertEquals(entityResponse.getAlgorithm().getId(), algorithm.getId());
        //Assertion relative to hprojectAlgorithm
        Assert.assertNotEquals(0, entityResponse.getId());
        Assert.assertEquals(entityResponse.getClassName(), HProjectAlgorithmJob.class.getName());
        Assert.assertEquals(entityResponse.getName(), hprojectAlgorithmName);
        Assert.assertEquals(entityResponse.getConfig(), config);
        Assert.assertEquals(entityResponse.getCronExpression(), cronExpression);
        Assert.assertFalse(entityResponse.isActive());
        AlgorithmUtil algorithmUtil = getOsgiService(AlgorithmUtil.class);
        //Assertion relative to jobDetail
        JobDetail algorithmJobDetail = entityResponse.getJobDetail();
        Assert.assertNotNull(algorithmJobDetail);
        Assert.assertEquals(algorithmJobDetail.getJobClass(), HProjectAlgorithmJob.class);
        Assert.assertEquals(algorithmJobDetail.getKey().getName(), Long.toString(entityResponse.getId()));
        Assert.assertEquals(algorithmJobDetail.getKey().getGroup(), "DEFAULT");
        JobDataMap dataMap = algorithmJobDetail.getJobDataMap();
        Assert.assertNotNull(dataMap);
        Assert.assertEquals(dataMap.get("cronExpression"), cronExpression);
        Assert.assertEquals(dataMap.get("name"), entityResponse.getName());
        Assert.assertEquals(dataMap.get("projectId").toString(), Long.toString(project.getId()));
        Assert.assertEquals(dataMap.get("config"), config);
        Assert.assertEquals(dataMap.get("algorithmId").toString(), Long.toString(algorithm.getId()));
        Assert.assertEquals(dataMap.get("mainClass"), algorithm.getMainClassname());
        Assert.assertEquals(dataMap.get("appResource"), algorithmUtil.getJarFullPath(algorithm));
        Assert.assertEquals(dataMap.get("spark.jars"), algorithmUtil.getJarFullPath(algorithm));
        //Assertion relative to job params.
        Map<String, Object> jobParams = entityResponse.getJobParams();
        Assert.assertNotNull(jobParams);
        Assert.assertEquals(jobParams.get("name"), entityResponse.getName());
        Assert.assertEquals(jobParams.get("projectId").toString(), Long.toString(project.getId()));
        Assert.assertEquals(jobParams.get("config"), config);
        Assert.assertEquals(jobParams.get("algorithmId").toString(), Long.toString(algorithm.getId()));
        Assert.assertEquals(jobParams.get("mainClass"), algorithm.getMainClassname());
        Assert.assertEquals(jobParams.get("cronExpression"), cronExpression);
        Assert.assertEquals(jobParams.get("appResource"), algorithmUtil.getJarFullPath(algorithm));
        Assert.assertEquals(jobParams.get("spark.jars"), algorithmUtil.getJarFullPath(algorithm));
        JobKey jobKey = entityResponse.getJobKey();
        Assert.assertNotNull(jobKey);
        Assert.assertEquals(jobKey.getName(), Long.toString(entityResponse.getId()));
        Assert.assertEquals(jobKey.getGroup(), "DEFAULT");
    }


    private String createHProjectAlgorithmConfig(HProject project, Algorithm algorithm, HPacket packet, HPacketField hPacketField) {
        long packetId = packet.getId();
        long hpacketFieldId = hPacketField.getId();
        ObjectMapper objectMapper = new ObjectMapper();
        AlgorithmConfig baseConfig;
        try {
            baseConfig = objectMapper.readValue(algorithm.getBaseConfig(), AlgorithmConfig.class);
        } catch (Throwable t) {
            Assert.fail();
            throw new RuntimeException();
        }
        Assert.assertTrue(baseConfig.getInput() != null && baseConfig.getInput().size() == 1);
        AlgorithmIOField inputField = baseConfig.getInput().get(0);
        long algorithmInputFieldId = inputField.getId();
        String algorithInputFieldName = inputField.getName();
        String algorithmInputFieldDescription = inputField.getDescription();
        AlgorithmIOFieldType algorithmInputFieldType = inputField.getFieldType();
        AlgorithmIOFieldMultiplicity algorithmInputFieldMultiplicity = inputField.getMultiplicity();
        AlgorithmFieldType algorithmInputType = inputField.getType();
        Assert.assertTrue(baseConfig.getOutput() != null && baseConfig.getOutput().size() == 1);
        AlgorithmIOField outputField = baseConfig.getOutput().get(0);
        long algorithmOutputFieldId = outputField.getId();
        String algorithOutputFieldName = outputField.getName();
        String algorithmOutputFieldDescription = outputField.getDescription();
        AlgorithmIOFieldType algorithmOutputFieldType = outputField.getFieldType();
        AlgorithmIOFieldMultiplicity algorithmOutputFieldMultiplicity = outputField.getMultiplicity();
        AlgorithmFieldType algorithmOutputType = outputField.getType();
        return String.format("{\"input\":[{\"packetId\"" + ": %s ,\"" +
                        "mappedInputList\":[{\"packetFieldId\"" + ":  %s," +
                        "\"algorithmInput\":{\"id\":\" %s \",\"name\":\" %s \"," +
                        "\"description\":\" %s \",\"fieldType\":\" %s \"," +
                        "\"multiplicity\":\" %s \",\"type\":\" %s \"}}]}]," +
                        "\"output\":[{\"id\": %s,\"name\":\" %s \",\"description\": \" %s \"," +
                        "\"fieldType\":\" %s \",\"multiplicity\":\" %s \",\"type\":\" %s \"}]}\"",
                packetId, hpacketFieldId,//packet and packet field settings
                algorithmInputFieldId, algorithInputFieldName, algorithmInputFieldDescription, algorithmInputFieldType, algorithmInputFieldMultiplicity, algorithmInputType, //input field settings
                algorithmOutputFieldId, algorithOutputFieldName, algorithmOutputFieldDescription, algorithmOutputFieldType, algorithmOutputFieldMultiplicity, algorithmOutputType
        );
    }

    private String createCronExpression() {
        return "0 0 10 ? 1 MON#1 *";
    }

    private String createHProjectAlgorithmName() {
        return "hprojectAlgorithm" + java.util.UUID.randomUUID().toString().replaceAll("-", "");
    }

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
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
        Assert.assertEquals("Brand",
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

    private HPacket createHPacketAndAddHPacketField(HDevice hdevice, boolean createField) {
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

        if (createField) {
            HPacketField field1 = new HPacketField();
            field1.setPacket(hpacket);
            field1.setName("temperature" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
            field1.setDescription("Temperature");
            field1.setType(HPacketFieldType.DOUBLE);
            field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
            field1.setValue(24.0);


            hpacket.setFields(new HashSet<>() {
                {
                    add(field1);
                }
            });

            // add field1
            this.impersonateUser(hPacketRestApi, adminUser);
            Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
            Assert.assertEquals(200, responseAddField1.getStatus());
            Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
            Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
            Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());

            //check restResponse field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getFields().iterator().next().getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

        }
        return hpacket;
    }

    private Permission addPermission(HUser huser, HyperIoTAction action, long resourceId) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        Permission permission = utilGrantPermission(huser, role, action, resourceId);
        return permission;
    }

    @After
    public void afterTest() {
        // Remove all projects and delete in cascade all associated entities in every tests
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
                Assert.assertNull(restResponse1.getEntity());
            }
        }

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

        // Remove all Algorithms created in every tests
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponseAlgorithms = algorithmRestApi.findAllAlgorithm(AlgorithmType.STATISTICS);
        List<Algorithm> listAlgorithm = restResponseAlgorithms.readEntity(new GenericType<List<Algorithm>>() {
        });
        if (!listAlgorithm.isEmpty()) {
            Assert.assertFalse(listAlgorithm.isEmpty());
            for (Algorithm algorithm : listAlgorithm) {
                try {
                    // delete jar file inside /spark/jobs
                    HadoopManagerSystemApi hadoopManagerSystemApi = getOsgiService(HadoopManagerSystemApi.class);
                    String pathJarFile = "/spark/jobs/";
                    String jarName = algorithm.getName().replaceAll(" ", "_").toLowerCase() + ".jar";
                    hadoopManagerSystemApi.deleteFile(pathJarFile + jarName);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                this.impersonateUser(algorithmRestApi, adminUser);
                Response restResponse1 = algorithmRestApi.deleteAlgorithm(algorithm.getId());
                Assert.assertEquals(200, restResponse1.getStatus());
                Assert.assertNull(restResponse1.getEntity());
            }
        }
    }


}

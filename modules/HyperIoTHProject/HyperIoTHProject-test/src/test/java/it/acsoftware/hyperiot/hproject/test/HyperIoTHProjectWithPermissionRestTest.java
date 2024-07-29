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

package it.acsoftware.hyperiot.hproject.test;

import it.acsoftware.hyperiot.area.actions.HyperIoTAreaAction;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.actions.HyperIoTHDeviceAction;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.model.AutoRegisterChallengeRequest;
import it.acsoftware.hyperiot.hproject.model.AutoRegisterProjectCredentials;
import it.acsoftware.hyperiot.hproject.model.AutoRegisterProjectRequest;
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
import java.nio.charset.StandardCharsets;
import java.util.*;

import static it.acsoftware.hyperiot.hproject.test.HyperIoTHProjectConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for HProject System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHProjectWithPermissionRestTest extends KarafTestSupport {

    /*
        TODO
            To solve problema related to field type mapped on postgres database relative to HProject's pubkey field
            (the problem is that the field pubkey must be mapped in database to bytea type)
            To solve the problem we need to put @Basic Annotation on getPubKey accessor in HProject class.
            But, when we test (using h2 database in memory), when h2 generate database mapping he put VARBINARY(255) on
            pubkey field(This cause problema because this settings limit the field's size to 255 byte array).
            When need to alter this data type's constraint to generate the pubkey field like when we put the @Lob annotation on
            accessor getPubKey in HProject class.
            So in the first test of the class (or alternatively in a Before test), we need to alter pubkey's datatype to
            BLOB(2147483647).
            It's better ,for manutenibility reason, to put the execution of this script at the startup of the karaf container
            used for testing purpose.
     */

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
    }

    @SuppressWarnings("unused")
    private HyperIoTAction getHyperIoTAction(String resourceName, HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder.createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
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
        assertContains("HyperIoTSparkManager-features ", features);
        assertContains("HyperIoTKafkaConnector-features ", features);
        assertContains("JobScheduler-features ", features);
        assertContains("HyperIoTZookeeperConnector-features ", features);
        //HyperIoTServices
        assertContains("HyperIoTHProject-features ", features);


        assertContains("HyperIoTAlgorithm-features ", features);

        assertContains("HyperIoTHadoopManager-features ", features);
        assertContains("HyperIoTDashboard-features ", features);

        assertContains("HyperIoTRuleEngine-features ", features);

        assertContains("HyperIoTStormManager-features ", features);
        assertContains("HyperIoTHBaseConnector-features ", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("hyperiot", datasource);
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));

        String updatePubKeyDataTypeToBlobCommand = "jdbc:execute hyperiot ALTER TABLE HPROJECT ALTER COLUMN PUBKEY SET DATA TYPE BLOB(2147483647)";
        executeCommand(updatePubKeyDataTypeToBlobCommand);
        //String columnAfterChangeProject = executeCommand("jdbc:query hyperiot SHOW COLUMNS FROM HPROJECT");
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @Test
    public void test01_hprojectModuleShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call checkModuleWorking checks if HProject module working
        // correctly
        huser = createHUser(null);
        Assert.assertNotEquals(0, huser.getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.checkModuleWorking();
        Assert.assertNotNull(hprojectRestService);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProject Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_saveHProjectWithPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, save HProject with the following call saveHProject
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Project of user: " + huser.getUsername());
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals(hproject.getDescription(), ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }

    @Test
    public void test03_saveHProjectWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to save HProject with the following call
        // saveHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Description");
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test04_updateHProjectWithPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, update HProject with the following call
        // updateHProject
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Date timestamp = new Date();
        hproject.setDescription("Description edited in date: " + timestamp);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hproject.getEntityVersion() + 1,
                (((HProject) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals("Description edited in date: " + timestamp,
                (((HProject) restResponse.getEntity()).getDescription()));
        Assert.assertEquals(hproject.getId(), ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }

    @Test
    public void test05_updateHProjectWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to update HProject with the following call
        // updateHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hproject.setDescription("Description Edited" + java.util.UUID.randomUUID());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findHProjectWithPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, find HProject with the following call findHProject
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponse.getEntity()).getId());
    }

    @Test
    public void test07_findHProjectWithPermissionShouldFailIfEntityNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to find HProject with the following call
        // findHProject, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findHProject(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findHProjectWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find HProject with the following call
        // findHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findHProjectNotFoundWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find HProject not found with the
        // following call findHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findHProject(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test10_findAllHProjectWithPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, find all HProject with the following call
        // findAllHProject
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findAllHProject();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        Assert.assertEquals(1, listHProjects.size());
        boolean hprojectFound = false;
        for (HProject project : listHProjects) {
            if (hproject.getId() == project.getId()) {
                Assert.assertEquals(huser.getId(), project.getUser().getId());
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test11_findAllHProjectWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find all HProject with the following call
        // findAllHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findAllHProject();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteHProjectWithPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, delete HProject with the following call
        // deleteHProject
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test13_deleteHProjectWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to delete HProject with the following call
        // deleteHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_deleteHProjectWithPermissionShouldFailIfEntityNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to delete HProject with the following call
        // deleteHProject, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.deleteHProject(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test15_deleteHProjectNotFoundWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to delete HProject not found with the
        // following call deleteHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.deleteHProject(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test16_saveHProjectShouldFailIfHProjectBelongsToAnotherUser() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the
        // following call saveHProject, but HProject belongs to another HUser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        huser2 = createHUser(null);
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getUsername(), huser2.getUsername());
        HProject anotherHProject = new HProject();
        anotherHProject.setName("Project " + java.util.UUID.randomUUID());
        anotherHProject.setDescription("Description");
        anotherHProject.setUser(huser2);
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(anotherHProject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test17_updateHProjectWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the
        // following call updateHProject, but HProject belongs to another HUser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        huser2 = createHUser(null);
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getUsername(), huser2.getUsername());

        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());
        Assert.assertNotEquals(huser.getId(), anotherHProject.getUser().getId());
        anotherHProject.setDescription("Unauthorized - edited failed");
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(anotherHProject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test18_saveHProjectWithPermissionShouldFailIfNameIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the following call
        // saveHProject, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = new HProject();
        hproject.setName(null);
        hproject.setDescription("Description");
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test19_saveHProjectWithPermissionShouldFailIfNameIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the following call
        // saveHProject, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = new HProject();
        hproject.setName("");
        hproject.setDescription("Description");
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test20_saveHProjectWithPermissionShouldFailIfNameIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the following call
        // saveHProject, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = new HProject();
        hproject.setName("</script>");
        hproject.setDescription("Description");
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test21_saveHProjectWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the following call
        // saveHProject, but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("vbscript:");
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test22_saveHProjectWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the following call
        // saveHProject, but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        int lengthDescription = 3001;
        hproject.setDescription(testMaxDescription(lengthDescription));
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals(lengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }

    @Test
    public void test23_saveHProjectWithPermissionShouldWorkIfUserIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the following call
        // saveHProject, but HUser is null
        // The system set automatically HProject's user as current logged user.
        // response status code 200.
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Description...");
        hproject.setUser(null);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        HProject responseEntity = (HProject) restResponse.getEntity();
        Assert.assertNotEquals(0, responseEntity.getId());
        Assert.assertNotNull(responseEntity.getUser());
        Assert.assertEquals(huser.getId(), responseEntity.getUser().getId());
    }

    @Test
    public void test24_updateHProjectWithPermissionShouldFailIfNameIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call
        // updateHProject, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hproject.setName("");
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test25_updateHProjectWithPermissionShouldFailIfNameIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call
        // updateHProject, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hproject.setName(null);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test26_updateHProjectWithPermissionShouldFailIfNameIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call
        // updateHProject, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hproject.setName("javascript:");
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test27_updateHProjectWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call
        // updateHProject, but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hproject.setDescription("javascript:");
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test28_updateHProjectWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call
        // updateHProject, but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        int lengthDescription = 3001;
        hproject.setDescription(testMaxDescription(lengthDescription));
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hproject.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals(lengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }

    @Test
    public void test29_updateHProjectWithPermissionShouldWorkIfUserIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call
        // updateHProject, but HUser is null
        // The system set automatically project's user  with the user that save the project.
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hproject.setUser(null);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        HProject responseEntity = (HProject) restResponse.getEntity();
        Assert.assertNotEquals(0, responseEntity.getId());
        Assert.assertNotNull(responseEntity.getUser());
        Assert.assertEquals(huser.getId(), responseEntity.getUser().getId());
    }

    @Test
    public void test30_getHProjectAreaListWithPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to find all HProject Area list with the
        // following call getHProjectAreaList
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.AREAS_MANAGEMENT);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertFalse(listHProjectAreas.isEmpty());
        Assert.assertEquals(1, listHProjectAreas.size());
        boolean hprojectAreaFound = false;
        for (Area a : listHProjectAreas) {
            if (area.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(huser.getId(), a.getProject().getUser().getId());
                hprojectAreaFound = true;
            }
        }
        Assert.assertTrue(hprojectAreaFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test31_getHProjectAreaListWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find all HProject Area list with the
        // following call getHProjectAreaList
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());

    }

    @Test
    public void test32_getHProjectAreaListWithPermissionShouldFailIfHProjectNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to find all HProject Area list with the
        // following call getHProjectAreaList, but HProject not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.AREAS_MANAGEMENT);
        huser = createHUser(action);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test33_getHProjectAreaListNotFoundWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find all HProject Area list with the
        // following call getHProjectAreaList, but HProject not found
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test34_getHProjectAreaListWithPermissionShouldWorkIfHProjectAreaListIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to find all HProject Area list with the
        // following call getHProjectAreaList, if listHProjectAreas is empty
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.AREAS_MANAGEMENT);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertTrue(listHProjectAreas.isEmpty());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test35_getHProjectAreaListEmptyWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find all HProject Area list empty with
        // the following call getHProjectAreaList
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test36_saveHProjectWithPermissionShouldFailIfEntityIsDuplicated() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to save HProject with the following call
        // saveHProject, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HProject duplicateHProject = new HProject();
        duplicateHProject.setName(hproject.getName());
        duplicateHProject.setDescription("Description");
        duplicateHProject.setUser(hproject.getUser());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(duplicateHProject);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean hprojectNameIsDuplicated = false;
        boolean hprojectHUserIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hprojectNameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("user_id")) {
                Assert.assertEquals("user_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hprojectHUserIsDuplicated = true;
            }
        }
        Assert.assertTrue(hprojectNameIsDuplicated);
        Assert.assertTrue(hprojectHUserIsDuplicated);
    }

    @Test
    public void test37_saveHProjectDuplicatedWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to duplicate HProject with the following
        // call saveHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HProject duplicateHProject = new HProject();
        duplicateHProject.setName(hproject.getName());
        duplicateHProject.setDescription("Description");
        duplicateHProject.setUser(hproject.getUser());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(duplicateHProject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test38_updateHProjectWithPermissionShouldFailIfEntityNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call
        // updateHProject, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        // entity isn't stored in database
        HProject hproject = new HProject();
        hproject.setDescription("Description Edited" + java.util.UUID.randomUUID());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test39_updateHProjectNotFoundWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to update HProject not found with the
        // following call updateHProject
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        // entity isn't stored in database
        HProject hproject = new HProject();
        hproject.setDescription("Description Edited" + java.util.UUID.randomUUID());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
    }


    @Test
    public void test40_getProjectTreeViewWithPermissionJsonShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, finds all HPacket by hprojectId with the following
        // call getProjectTreeViewJson
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(3, listHPackets.size());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        boolean hpacket3Found = false;
        for (HPacket packet : listHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket2Found = true;
            }
            if (hpacket3.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket3Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertTrue(hpacket3Found);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test41_getProjectTreeViewJsonWithPermissionShouldWorkIfHProjectNotHaveHPacket() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, finds all HPacket by hprojectId with the following
        // call getHProjectTreeView but hproject not have a hpacket.
        // This call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertTrue(listHPackets.isEmpty());
        Assert.assertEquals(0, listHPackets.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test42_getProjectTreeViewJsonWithPermissionShouldFailIfHProjectNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to find all HPacket by hprojectId with the following
        // call getHProjectTreeView, but hproject not found.
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        huser = createHUser(action);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test43_getProjectTreeViewJsonWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find all HPacket by hprojectId with the following
        // call getProjectTreeViewJson
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test44_getProjectTreeViewJsonWithoutPermissionShouldFailIfHProjectNotHaveHPacket() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to finds all HPacket by hprojectId
        // with the following call getHProjectTreeView but hproject not have a hpacket.
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test45_getProjectTreeViewJsonWithoutPermissionShouldFailIfHProjectNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to find all HPacket by hprojectId with the following
        // call getHProjectTreeView, but hproject not found.
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test46_cardsViewWithPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, finds all HProject with the following call cardsView
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.cardsView();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        Assert.assertEquals(1, listHProjects.size());
        boolean hprojectFound = false;
        for (HProject project : listHProjects) {
            if (hproject.getId() == project.getId()) {
                Assert.assertEquals(huser.getId(), project.getUser().getId());
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test47_cardsViewWithoutPermissionShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to finds all HProject with
        // the following call cardsView
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.cardsView();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test48_createChallengeForAutoRegisterShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser create challenge for AutoRegister HProject
        // response status code '200'
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.createChallengeForAutoRegister(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotNull(((AutoRegisterChallengeRequest) restResponse.getEntity()).getPlainTextChallenge());
    }


    @Test
    public void test49_createChallengeForAutoRegisterShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser create challenge for AutoRegister HProject
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.createChallengeForAutoRegister(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test50_updateHProjectWithPermissionShouldFailIfEntityIsDuplicated() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject with the following call updateHProject,
        // but hproject is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HProject duplicateHProject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), duplicateHProject.getUser().getId());

        Assert.assertEquals(hproject.getUser().getId(), duplicateHProject.getUser().getId());
        Assert.assertNotEquals(hproject.getId(), duplicateHProject.getId());

        duplicateHProject.setName(hproject.getName());
        duplicateHProject.setUser(hproject.getUser());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(duplicateHProject);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean hprojectNameIsDuplicated = false;
        boolean hprojectHUserIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hprojectNameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("user_id")) {
                Assert.assertEquals("user_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hprojectHUserIsDuplicated = true;
            }
        }
        Assert.assertTrue(hprojectNameIsDuplicated);
        Assert.assertTrue(hprojectHUserIsDuplicated);
    }


    @Test
    public void test51_updateHProjectWithoutPermissionShouldFailIfEntityIsDuplicated() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, without permission, tries to update HProject with the following call updateHProject,
        // but hproject is duplicated
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HProject duplicateHProject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), duplicateHProject.getUser().getId());

        Assert.assertEquals(hproject.getUser().getId(), duplicateHProject.getUser().getId());
        Assert.assertNotEquals(hproject.getId(), duplicateHProject.getId());

        duplicateHProject.setName(hproject.getName());
        duplicateHProject.setUser(hproject.getUser());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(duplicateHProject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test52_findAllHProjectPaginatedWithPermissionShouldWork() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all
        // HProjects with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 5;
        int page = 2;
        List<HProject> hprojects = new ArrayList<>();
        int numbEntities = 7;
        for (int i = 0; i < numbEntities; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(numbEntities, hprojects.size());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listHProjects.getResults().size());
        Assert.assertEquals(delta, listHProjects.getDelta());
        Assert.assertEquals(page, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // delta is 5, page 2: 7 entities stored in database
        Assert.assertEquals(2, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponsePage1 = hProjectRestApi.findAllHProjectPaginated(delta, 1);
        HyperIoTPaginableResult<HProject> listHProjectsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjectsPage1.getResults().size());
        Assert.assertEquals(delta, listHProjectsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectsPage1.getCurrentPage());
        Assert.assertEquals(page, listHProjectsPage1.getNextPage());
        // delta is 5, page is 1: 7 entities stored in database
        Assert.assertEquals(2, listHProjectsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test53_findAllHProjectPaginatedWithoutPermissionShouldFail() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser without permission
        // tries to find all HProjects with pagination
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test54_findAllHProjectPaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all HProjects with pagination
        // if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        List<HProject> hprojects = new ArrayList<>();
        int numbEntities = 3;
        for (int i = 0; i < numbEntities; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(numbEntities, hprojects.size());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listHProjects.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjects.getDelta());
        Assert.assertEquals(defaultPage, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // default delta is 10, default page is 1: 3 entities stored in database
        Assert.assertEquals(1, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test55_findAllHProjectPaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all HProjects with pagination
        // if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = -1;
        int page = 2;
        List<HProject> hprojects = new ArrayList<>();
        int numbEntities = 12;
        for (int i = 0; i < numbEntities; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(numbEntities, hprojects.size());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponsePage2 = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjectsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listHProjectsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage2.getDelta());
        Assert.assertEquals(page, listHProjectsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectsPage2.getNextPage());
        // default delta is 10, page is 2: 12 entities stored in database
        Assert.assertEquals(2, listHProjectsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponsePage1 = hProjectRestApi.findAllHProjectPaginated(delta, 1);
        HyperIoTPaginableResult<HProject> listHProjectsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectsPage1.getCurrentPage());
        Assert.assertEquals(page, listHProjectsPage1.getNextPage());
        // default delta is 10, page is 1: 12 entities stored in database
        Assert.assertEquals(2, listHProjectsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test56_findAllHProjectPaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all HProjects with pagination
        // if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 0;
        int page = 2;
        List<HProject> hprojects = new ArrayList<>();
        int numEntities = 14;
        for (int i = 0; i < numEntities; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(numEntities, hprojects.size());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponsePage2 = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjectsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage2.getResults().isEmpty());
        Assert.assertEquals(numEntities - defaultDelta, listHProjectsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage2.getDelta());
        Assert.assertEquals(page, listHProjectsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectsPage2.getNextPage());
        // default delta is 10, page is 2: 14 entities stored in database
        Assert.assertEquals(2, listHProjectsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponsePage1 = hProjectRestApi.findAllHProjectPaginated(delta, 1);
        HyperIoTPaginableResult<HProject> listHProjectsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectsPage1.getCurrentPage());
        Assert.assertEquals(page, listHProjectsPage1.getNextPage());
        // default delta is 10, page is 2: 14 entities stored in database
        Assert.assertEquals(2, listHProjectsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test57_findAllHProjectPaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all HProjects with pagination
        // if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 5;
        int page = -1;
        List<HProject> hprojects = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(delta, hprojects.size());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjects.getResults().size());
        Assert.assertEquals(delta, listHProjects.getDelta());
        Assert.assertEquals(defaultPage, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // delta is 5, default page 1: 5 entities stored in database
        Assert.assertEquals(1, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test58_findAllHProjectPaginatedWithPermissionShouldWorkIfPageIsZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all HProjects with pagination
        // if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 6;
        int page = 0;
        List<HProject> hprojects = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(defaultDelta, hprojects.size());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjects.getResults().size());
        Assert.assertEquals(delta, listHProjects.getDelta());
        Assert.assertEquals(defaultPage, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHProjects.getNextPage());
        // delta is 6, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponsePage2 = hProjectRestApi.findAllHProjectPaginated(delta, 2);
        HyperIoTPaginableResult<HProject> listHProjectsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listHProjectsPage2.getResults().size());
        Assert.assertEquals(delta, listHProjectsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHProjectsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectsPage2.getNextPage());
        // delta is 6, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listHProjectsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    /*
     *
     *
     * CUSTOM TESTS
     *
     *
     */

    @Test
    public void test62_deleteHProjectWithPermissionDeleteInCascadeAllAreas() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser deletes HProject, with Area associated, with the following call deleteHProject,
        // this call deletes in cascade all areas
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(huser.getId(), area2.getProject().getUser().getId());

        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.AREAS_MANAGEMENT);
        addPermission(huser, action1);

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertEquals(2, listHProjectAreas.size());
        Assert.assertFalse(listHProjectAreas.isEmpty());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area area : listHProjectAreas) {
            if (area1.getId() == area.getId()) {
                Assert.assertEquals(area1.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // huser deletes hproject with call deleteHProject
        // this call deletes in cascade all areas
        this.impersonateUser(hprojectRestService, huser);
        Response responseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());
        Assert.assertNull(responseDeleteHProject.getEntity());

        // checks if areas exists in database
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseFindArea1 = areaRestApi.findArea(area1.getId());
        Assert.assertEquals(404, restResponseFindArea1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea1.getEntity()).getType());

        Response restResponseFindArea2 = areaRestApi.findArea(area2.getId());
        Assert.assertEquals(404, restResponseFindArea2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea2.getEntity()).getType());
    }


    @Test
    public void test63_deleteAreaWithPermissionNotDeleteInCascadeHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser deletes areas, with HProject associated, with the following call deleteArea,
        // hproject is not deleted in cascade mode
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(huser.getId(), area2.getProject().getUser().getId());

        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.AREAS_MANAGEMENT);
        addPermission(huser, action1);

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertEquals(2, listHProjectAreas.size());
        Assert.assertFalse(listHProjectAreas.isEmpty());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area area : listHProjectAreas) {
            if (area1.getId() == area.getId()) {
                Assert.assertEquals(area1.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // deletes areas with deleteArea calls
        // this calls not deletes hproject in cascade
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        this.impersonateUser(areaRestApi, huser);
        Response responseDeleteArea1 = areaRestApi.deleteArea(area1.getId());
        Assert.assertEquals(200, responseDeleteArea1.getStatus());
        Assert.assertNull(responseDeleteArea1.getEntity());

        this.impersonateUser(areaRestApi, huser);
        Response responseDeleteArea2 = areaRestApi.deleteArea(area2.getId());
        Assert.assertEquals(200, responseDeleteArea2.getStatus());
        Assert.assertNull(responseDeleteArea2.getEntity());

        // checks if hproject is still stored in database
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseHProject.getEntity()).getId());
        Assert.assertEquals(huser.getId(), ((HProject) restResponseHProject.getEntity()).getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponseHProjectAreaList = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreasEmpty = restResponseHProjectAreaList.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertEquals(0, listHProjectAreasEmpty.size());
        Assert.assertTrue(listHProjectAreasEmpty.isEmpty());
        Assert.assertEquals(200, restResponseHProjectAreaList.getStatus());
    }


    @Test
    public void test64_deleteHProjectWithPermissionDeleteInCascadeAllHDevices() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser deletes HProject, with HDevice associated, with the following
        // call deleteHProject, this call deletes in cascade all devices
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice2.getProject().getUser().getId());

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        addPermission(huser, action1);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponse = hDeviceRestApi.findAllHDeviceByProjectId(hproject.getId());
        Collection<HDevice> listHDevices = restResponse.readEntity(new GenericType<Collection<HDevice>>() {
        });
        Assert.assertEquals(2, listHDevices.size());
        Assert.assertFalse(listHDevices.isEmpty());
        boolean device1Found = false;
        boolean device2Found = false;
        for (HDevice device : listHDevices) {
            if (hdevice1.getId() == device.getId()) {
                Assert.assertEquals(hdevice1.getId(), device.getId());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
                Assert.assertEquals(hdevice2.getId(), device.getId());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                device2Found = true;
            }
        }
        Assert.assertTrue(device1Found);
        Assert.assertTrue(device2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // huser deletes hproject with call deleteHProject
        // this call deletes in cascade all devices
        this.impersonateUser(hprojectRestService, huser);
        Response responseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());
        Assert.assertNull(responseDeleteHProject.getEntity());

        // checks if devices exists in database
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseFindHDevice1 = hDeviceRestApi.findHDevice(hdevice1.getId());
        Assert.assertEquals(404, responseFindHDevice1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindHDevice1.getEntity()).getType());
        Response responseFindHDevice2 = hDeviceRestApi.findHDevice(hdevice2.getId());
        Assert.assertEquals(404, responseFindHDevice2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindHDevice2.getEntity()).getType());
    }


    @Test
    public void test65_deleteHDeviceWithPermissionNotDeleteInCascadeHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser deletes devices, with HProject associated, with deleteHDevice calls,
        // hproject is not deleted in cascade mode
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice2.getProject().getUser().getId());

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        addPermission(huser, action1);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponse = hDeviceRestApi.findAllHDeviceByProjectId(hproject.getId());
        Collection<HDevice> listHDevices = restResponse.readEntity(new GenericType<Collection<HDevice>>() {
        });
        Assert.assertEquals(2, listHDevices.size());
        Assert.assertFalse(listHDevices.isEmpty());
        boolean device1Found = false;
        boolean device2Found = false;
        for (HDevice device : listHDevices) {
            if (hdevice1.getId() == device.getId()) {
                Assert.assertEquals(hdevice1.getId(), device.getId());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
                Assert.assertEquals(hdevice2.getId(), device.getId());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                device2Found = true;
            }
        }
        Assert.assertTrue(device1Found);
        Assert.assertTrue(device2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // deletes devices with deleteHDevice calls
        // this calls not deletes hproject in cascade
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseDeleteHDevice1 = hDeviceRestApi.deleteHDevice(hdevice1.getId());
        Assert.assertEquals(200, responseDeleteHDevice1.getStatus());
        Assert.assertNull(responseDeleteHDevice1.getEntity());

        this.impersonateUser(hDeviceRestApi, huser);
        Response responseDeleteHDevice2 = hDeviceRestApi.deleteHDevice(hdevice2.getId());
        Assert.assertEquals(200, responseDeleteHDevice2.getStatus());
        Assert.assertNull(responseDeleteHDevice2.getEntity());

        // checks if hproject is still stored in database
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hprojectRestService, huser);
        Response responseFindHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, responseFindHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) responseFindHProject.getEntity()).getId());
        Assert.assertEquals(huser.getId(), ((HProject) responseFindHProject.getEntity()).getUser().getId());

        // hproject hasn't devices
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseDeviceByProject = hDeviceRestApi.findAllHDeviceByProjectId(hproject.getId());
        Collection<HDevice> listDeviceByProject = responseDeviceByProject.readEntity(new GenericType<Collection<HDevice>>() {
        });
        Assert.assertEquals(0, listDeviceByProject.size());
        Assert.assertTrue(listDeviceByProject.isEmpty());
        Assert.assertEquals(200, responseDeviceByProject.getStatus());
    }


    @Test
    public void test66_createCompleteHProjectWithPermissionWithAssociatedAreasDashboardsDashboardWidgetsHDevicesHPackets() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser creates complete hproject with dashboards, widgets, areas, devices and packets
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);

        /*
         *
         * Start: Create complete hproject with dashboards, widgets, areas, devices and packets
         *
         */

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(huser.getId(), area2.getProject().getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice2.getProject().getUser().getId());

        HDevice hdevice3 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice3.getId());
        Assert.assertEquals(hproject.getId(), hdevice3.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice3.getProject().getUser().getId());

        HDevice hdevice4 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice4.getId());
        Assert.assertEquals(hproject.getId(), hdevice4.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice4.getProject().getUser().getId());

        HDevice hdevice5 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice5.getId());
        Assert.assertEquals(hproject.getId(), hdevice5.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice5.getProject().getUser().getId());

        AreaDevice ad1 = createAreaDevice(area1, hdevice1);
        Assert.assertNotEquals(0, ad1.getId());
        Assert.assertEquals(hdevice1.getId(), ad1.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad1.getArea().getId());
        Assert.assertEquals(huser.getId(), ad1.getArea().getProject().getUser().getId());

        AreaDevice ad2 = createAreaDevice(area1, hdevice2);
        Assert.assertNotEquals(0, ad2.getId());
        Assert.assertEquals(hdevice2.getId(), ad2.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad2.getArea().getId());
        Assert.assertEquals(huser.getId(), ad2.getArea().getProject().getUser().getId());

        AreaDevice ad3 = createAreaDevice(area2, hdevice3);
        Assert.assertNotEquals(0, ad3.getId());
        Assert.assertEquals(hdevice3.getId(), ad3.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad3.getArea().getId());
        Assert.assertEquals(huser.getId(), ad3.getArea().getProject().getUser().getId());

        AreaDevice ad4 = createAreaDevice(area2, hdevice4);
        Assert.assertNotEquals(0, ad4.getId());
        Assert.assertEquals(hdevice4.getId(), ad4.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad4.getArea().getId());
        Assert.assertEquals(huser.getId(), ad4.getArea().getProject().getUser().getId());

        AreaDevice ad5 = createAreaDevice(area2, hdevice5);
        Assert.assertNotEquals(0, ad5.getId());
        Assert.assertEquals(hdevice5.getId(), ad5.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad5.getArea().getId());
        Assert.assertEquals(huser.getId(), ad5.getArea().getProject().getUser().getId());

        // creates hpackets
        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        HPacket hpacket4 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket4.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket4.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket4.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket4.getDevice().getProject().getUser().getId());

        HPacket hpacket5 = createHPacketAndAddHPacketField(hdevice2, false);
        Assert.assertNotEquals(0, hpacket5.getId());
        Assert.assertEquals(hdevice2.getId(), hpacket5.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket5.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket5.getDevice().getProject().getUser().getId());

        HPacket hpacket6 = createHPacketAndAddHPacketField(hdevice3, false);
        Assert.assertNotEquals(0, hpacket6.getId());
        Assert.assertEquals(hdevice3.getId(), hpacket6.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket6.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket6.getDevice().getProject().getUser().getId());

        HPacket hpacket7 = createHPacketAndAddHPacketField(hdevice4, false);
        Assert.assertNotEquals(0, hpacket7.getId());
        Assert.assertEquals(hdevice4.getId(), hpacket7.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket7.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket7.getDevice().getProject().getUser().getId());

        HPacket hpacket8 = createHPacketAndAddHPacketField(hdevice5, false);
        Assert.assertNotEquals(0, hpacket8.getId());
        Assert.assertEquals(hdevice5.getId(), hpacket8.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket8.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket8.getDevice().getProject().getUser().getId());

        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        addPermission(huser, action1);
        this.impersonateUser(areaRestApi, huser);
        //checks if AreaDevice exists
        Response responseGetAreaDeviceList1 = areaRestApi.getAreaDeviceList(area1.getId());
        List<AreaDevice> listAreaDevices1 = responseGetAreaDeviceList1.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertEquals(2, listAreaDevices1.size());
        Assert.assertFalse(listAreaDevices1.isEmpty());
        Assert.assertEquals(200, responseGetAreaDeviceList1.getStatus());

        AreaDevice areaDevice1 = listAreaDevices1.get(0);
        Assert.assertNotEquals(0, areaDevice1.getId());
        Assert.assertEquals(hdevice1.getId(), areaDevice1.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        AreaDevice areaDevice2 = listAreaDevices1.get(1);
        Assert.assertNotEquals(0, areaDevice2.getId());
        Assert.assertEquals(hdevice2.getId(), areaDevice2.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice2.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice2.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice2.getId(), ad2.getId());
        Assert.assertEquals(areaDevice2.getDevice().getId(), ad2.getDevice().getId());
        Assert.assertEquals(areaDevice2.getArea().getId(), ad2.getArea().getId());

        //checks if AreaDevice exists
        Response responseGetAreaDeviceList2 = areaRestApi.getAreaDeviceList(area2.getId());
        List<AreaDevice> listSecondAreaDevices = responseGetAreaDeviceList2.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertEquals(3, listSecondAreaDevices.size());
        Assert.assertFalse(listSecondAreaDevices.isEmpty());
        Assert.assertEquals(200, responseGetAreaDeviceList2.getStatus());

        AreaDevice areaDevice3 = listSecondAreaDevices.get(0);
        Assert.assertNotEquals(0, areaDevice3.getId());
        Assert.assertEquals(hdevice3.getId(), areaDevice3.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice3.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice3.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice3.getId(), ad3.getId());
        Assert.assertEquals(areaDevice3.getDevice().getId(), ad3.getDevice().getId());
        Assert.assertEquals(areaDevice3.getArea().getId(), ad3.getArea().getId());

        AreaDevice areaDevice4 = listSecondAreaDevices.get(1);
        Assert.assertNotEquals(0, areaDevice4.getId());
        Assert.assertEquals(hdevice4.getId(), areaDevice4.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice4.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice4.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice4.getId(), ad4.getId());
        Assert.assertEquals(areaDevice4.getDevice().getId(), ad4.getDevice().getId());
        Assert.assertEquals(areaDevice4.getArea().getId(), ad4.getArea().getId());

        AreaDevice areaDevice5 = listSecondAreaDevices.get(2);
        Assert.assertNotEquals(0, areaDevice5.getId());
        Assert.assertEquals(hdevice5.getId(), areaDevice5.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice5.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice5.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice5.getId(), ad5.getId());
        Assert.assertEquals(areaDevice5.getDevice().getId(), ad5.getDevice().getId());
        Assert.assertEquals(areaDevice5.getArea().getId(), ad5.getArea().getId());


        //checks if Area HProject exists
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.AREAS_MANAGEMENT);
        addPermission(huser, action2);
        this.impersonateUser(hprojectRestService, huser);
        Response responseProjectArea = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = responseProjectArea.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertEquals(2, listHProjectAreas.size());
        Assert.assertFalse(listHProjectAreas.isEmpty());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area area : listHProjectAreas) {
            if (area1.getId() == area.getId()) {
                Assert.assertEquals(area1.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, responseProjectArea.getStatus());

        //checks if device packets exists
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        HyperIoTAction action4 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        addPermission(huser, action4);
        this.impersonateUser(hPacketRestApi, huser);
        Response responseGetHDevicePacketList1 = hPacketRestApi.getHDevicePacketList(hdevice1.getId());
        Collection<HPacket> listFirstDeviceHPackets = responseGetHDevicePacketList1.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(4, listFirstDeviceHPackets.size());
        Assert.assertFalse(listFirstDeviceHPackets.isEmpty());
        boolean packet1Found = false;
        boolean packet2Found = false;
        boolean packet3Found = false;
        boolean packet4Found = false;
        for (HPacket packet : listFirstDeviceHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet2Found = true;
            }
            if (hpacket3.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet3Found = true;
            }
            if (hpacket4.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet4Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertTrue(packet3Found);
        Assert.assertTrue(packet4Found);
        Assert.assertEquals(200, responseGetHDevicePacketList1.getStatus());

        Response responseGetHDevicePacketList2 = hPacketRestApi.getHDevicePacketList(hdevice2.getId());
        Collection<HPacket> listSecondDeviceHPackets = responseGetHDevicePacketList2.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listSecondDeviceHPackets.size());
        Assert.assertFalse(listSecondDeviceHPackets.isEmpty());
        boolean packet5Found = false;
        for (HPacket packet : listSecondDeviceHPackets) {
            if (hpacket5.getId() == packet.getId()) {
                Assert.assertEquals(hdevice2.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet5Found = true;
            }
        }
        Assert.assertTrue(packet5Found);
        Assert.assertEquals(200, responseGetHDevicePacketList2.getStatus());

        Response responseGetHDevicePacketList3 = hPacketRestApi.getHDevicePacketList(hdevice3.getId());
        Collection<HPacket> listThirdDeviceHPackets = responseGetHDevicePacketList3.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listThirdDeviceHPackets.size());
        Assert.assertFalse(listThirdDeviceHPackets.isEmpty());
        boolean packet6Found = false;
        for (HPacket packet : listThirdDeviceHPackets) {
            if (hpacket6.getId() == packet.getId()) {
                Assert.assertEquals(hdevice3.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet6Found = true;
            }
        }
        Assert.assertTrue(packet6Found);
        Assert.assertEquals(200, responseGetHDevicePacketList3.getStatus());

        Response responseGetHDevicePacketList4 = hPacketRestApi.getHDevicePacketList(hdevice4.getId());
        Collection<HPacket> listFourthDeviceHPackets = responseGetHDevicePacketList4.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listFourthDeviceHPackets.size());
        Assert.assertFalse(listFourthDeviceHPackets.isEmpty());
        boolean packet7Found = false;
        for (HPacket packet : listFourthDeviceHPackets) {
            if (hpacket7.getId() == packet.getId()) {
                Assert.assertEquals(hdevice4.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet7Found = true;
            }
        }
        Assert.assertTrue(packet7Found);
        Assert.assertEquals(200, responseGetHDevicePacketList4.getStatus());

        Response responseGetHDevicePacketList5 = hPacketRestApi.getHDevicePacketList(hdevice5.getId());
        Collection<HPacket> listFifthDeviceHPackets = responseGetHDevicePacketList5.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listFifthDeviceHPackets.size());
        Assert.assertFalse(listFifthDeviceHPackets.isEmpty());
        boolean packet8Found = false;
        for (HPacket packet : listFifthDeviceHPackets) {
            if (hpacket8.getId() == packet.getId()) {
                Assert.assertEquals(hdevice5.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet8Found = true;
            }
        }
        Assert.assertTrue(packet8Found);
        Assert.assertEquals(200, responseGetHDevicePacketList5.getStatus());

        /*
         *
         * End: Complete hproject has been created
         *
         */

    }


    @Test
    public void test67_deleteCompleteHProjectWithPermissionDeleteInCascadeAllAreasDashboardsDashboardWidgetsHDevicesHPackets() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser deletes hproject with the following call deleteHProject; this call deletes
        // all entity associated (dashboards, widgets, areas, devices and packets, AreaDevice)
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);

        /*
         *
         * Start: Create complete hproject with dashboards, widgets, areas, devices and packets
         *
         */

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(huser.getId(), area2.getProject().getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice2.getProject().getUser().getId());

        HDevice hdevice3 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice3.getId());
        Assert.assertEquals(hproject.getId(), hdevice3.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice3.getProject().getUser().getId());

        HDevice hdevice4 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice4.getId());
        Assert.assertEquals(hproject.getId(), hdevice4.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice4.getProject().getUser().getId());

        HDevice hdevice5 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice5.getId());
        Assert.assertEquals(hproject.getId(), hdevice5.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice5.getProject().getUser().getId());

        AreaDevice ad1 = createAreaDevice(area1, hdevice1);
        Assert.assertNotEquals(0, ad1.getId());
        Assert.assertEquals(hdevice1.getId(), ad1.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad1.getArea().getId());
        Assert.assertEquals(huser.getId(), ad1.getArea().getProject().getUser().getId());

        AreaDevice ad2 = createAreaDevice(area1, hdevice2);
        Assert.assertNotEquals(0, ad2.getId());
        Assert.assertEquals(hdevice2.getId(), ad2.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad2.getArea().getId());
        Assert.assertEquals(huser.getId(), ad2.getArea().getProject().getUser().getId());

        AreaDevice ad3 = createAreaDevice(area2, hdevice3);
        Assert.assertNotEquals(0, ad3.getId());
        Assert.assertEquals(hdevice3.getId(), ad3.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad3.getArea().getId());
        Assert.assertEquals(huser.getId(), ad3.getArea().getProject().getUser().getId());

        AreaDevice ad4 = createAreaDevice(area2, hdevice4);
        Assert.assertNotEquals(0, ad4.getId());
        Assert.assertEquals(hdevice4.getId(), ad4.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad4.getArea().getId());
        Assert.assertEquals(huser.getId(), ad4.getArea().getProject().getUser().getId());

        AreaDevice ad5 = createAreaDevice(area2, hdevice5);
        Assert.assertNotEquals(0, ad5.getId());
        Assert.assertEquals(hdevice5.getId(), ad5.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad5.getArea().getId());
        Assert.assertEquals(huser.getId(), ad5.getArea().getProject().getUser().getId());

        // creates hpackets
        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        HPacket hpacket4 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket4.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket4.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket4.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket4.getDevice().getProject().getUser().getId());

        HPacket hpacket5 = createHPacketAndAddHPacketField(hdevice2, false);
        Assert.assertNotEquals(0, hpacket5.getId());
        Assert.assertEquals(hdevice2.getId(), hpacket5.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket5.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket5.getDevice().getProject().getUser().getId());

        HPacket hpacket6 = createHPacketAndAddHPacketField(hdevice3, false);
        Assert.assertNotEquals(0, hpacket6.getId());
        Assert.assertEquals(hdevice3.getId(), hpacket6.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket6.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket6.getDevice().getProject().getUser().getId());

        HPacket hpacket7 = createHPacketAndAddHPacketField(hdevice4, false);
        Assert.assertNotEquals(0, hpacket7.getId());
        Assert.assertEquals(hdevice4.getId(), hpacket7.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket7.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket7.getDevice().getProject().getUser().getId());

        HPacket hpacket8 = createHPacketAndAddHPacketField(hdevice5, false);
        Assert.assertNotEquals(0, hpacket8.getId());
        Assert.assertEquals(hdevice5.getId(), hpacket8.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket8.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket8.getDevice().getProject().getUser().getId());

        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        addPermission(huser, action1);
        this.impersonateUser(areaRestApi, huser);
        //checks if AreaDevice exists
        Response responseGetAreaDeviceList1 = areaRestApi.getAreaDeviceList(area1.getId());
        List<AreaDevice> listAreaDevices1 = responseGetAreaDeviceList1.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertEquals(2, listAreaDevices1.size());
        Assert.assertFalse(listAreaDevices1.isEmpty());
        Assert.assertEquals(200, responseGetAreaDeviceList1.getStatus());

        AreaDevice areaDevice1 = listAreaDevices1.get(0);
        Assert.assertNotEquals(0, areaDevice1.getId());
        Assert.assertEquals(hdevice1.getId(), areaDevice1.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        AreaDevice areaDevice2 = listAreaDevices1.get(1);
        Assert.assertNotEquals(0, areaDevice2.getId());
        Assert.assertEquals(hdevice2.getId(), areaDevice2.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice2.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice2.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice2.getId(), ad2.getId());
        Assert.assertEquals(areaDevice2.getDevice().getId(), ad2.getDevice().getId());
        Assert.assertEquals(areaDevice2.getArea().getId(), ad2.getArea().getId());

        //checks if AreaDevice exists
        Response responseGetAreaDeviceList2 = areaRestApi.getAreaDeviceList(area2.getId());
        List<AreaDevice> listSecondAreaDevices = responseGetAreaDeviceList2.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertEquals(3, listSecondAreaDevices.size());
        Assert.assertFalse(listSecondAreaDevices.isEmpty());
        Assert.assertEquals(200, responseGetAreaDeviceList2.getStatus());

        AreaDevice areaDevice3 = listSecondAreaDevices.get(0);
        Assert.assertNotEquals(0, areaDevice3.getId());
        Assert.assertEquals(hdevice3.getId(), areaDevice3.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice3.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice3.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice3.getId(), ad3.getId());
        Assert.assertEquals(areaDevice3.getDevice().getId(), ad3.getDevice().getId());
        Assert.assertEquals(areaDevice3.getArea().getId(), ad3.getArea().getId());

        AreaDevice areaDevice4 = listSecondAreaDevices.get(1);
        Assert.assertNotEquals(0, areaDevice4.getId());
        Assert.assertEquals(hdevice4.getId(), areaDevice4.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice4.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice4.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice4.getId(), ad4.getId());
        Assert.assertEquals(areaDevice4.getDevice().getId(), ad4.getDevice().getId());
        Assert.assertEquals(areaDevice4.getArea().getId(), ad4.getArea().getId());

        AreaDevice areaDevice5 = listSecondAreaDevices.get(2);
        Assert.assertNotEquals(0, areaDevice5.getId());
        Assert.assertEquals(hdevice5.getId(), areaDevice5.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice5.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice5.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice5.getId(), ad5.getId());
        Assert.assertEquals(areaDevice5.getDevice().getId(), ad5.getDevice().getId());
        Assert.assertEquals(areaDevice5.getArea().getId(), ad5.getArea().getId());


        //checks if Area HProject exists
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.AREAS_MANAGEMENT);
        addPermission(huser, action2);
        this.impersonateUser(hprojectRestService, huser);
        Response responseProjectArea = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = responseProjectArea.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertEquals(2, listHProjectAreas.size());
        Assert.assertFalse(listHProjectAreas.isEmpty());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area area : listHProjectAreas) {
            if (area1.getId() == area.getId()) {
                Assert.assertEquals(area1.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, responseProjectArea.getStatus());

        //checks if device packets exists
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        HyperIoTAction action4 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        addPermission(huser, action4);
        this.impersonateUser(hPacketRestApi, huser);
        Response responseGetHDevicePacketList1 = hPacketRestApi.getHDevicePacketList(hdevice1.getId());
        Collection<HPacket> listFirstDeviceHPackets = responseGetHDevicePacketList1.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(4, listFirstDeviceHPackets.size());
        Assert.assertFalse(listFirstDeviceHPackets.isEmpty());
        boolean packet1Found = false;
        boolean packet2Found = false;
        boolean packet3Found = false;
        boolean packet4Found = false;
        for (HPacket packet : listFirstDeviceHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet2Found = true;
            }
            if (hpacket3.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet3Found = true;
            }
            if (hpacket4.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet4Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertTrue(packet3Found);
        Assert.assertTrue(packet4Found);
        Assert.assertEquals(200, responseGetHDevicePacketList1.getStatus());

        Response responseGetHDevicePacketList2 = hPacketRestApi.getHDevicePacketList(hdevice2.getId());
        Collection<HPacket> listSecondDeviceHPackets = responseGetHDevicePacketList2.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listSecondDeviceHPackets.size());
        Assert.assertFalse(listSecondDeviceHPackets.isEmpty());
        boolean packet5Found = false;
        for (HPacket packet : listSecondDeviceHPackets) {
            if (hpacket5.getId() == packet.getId()) {
                Assert.assertEquals(hdevice2.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet5Found = true;
            }
        }
        Assert.assertTrue(packet5Found);
        Assert.assertEquals(200, responseGetHDevicePacketList2.getStatus());

        Response responseGetHDevicePacketList3 = hPacketRestApi.getHDevicePacketList(hdevice3.getId());
        Collection<HPacket> listThirdDeviceHPackets = responseGetHDevicePacketList3.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listThirdDeviceHPackets.size());
        Assert.assertFalse(listThirdDeviceHPackets.isEmpty());
        boolean packet6Found = false;
        for (HPacket packet : listThirdDeviceHPackets) {
            if (hpacket6.getId() == packet.getId()) {
                Assert.assertEquals(hdevice3.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet6Found = true;
            }
        }
        Assert.assertTrue(packet6Found);
        Assert.assertEquals(200, responseGetHDevicePacketList3.getStatus());

        Response responseGetHDevicePacketList4 = hPacketRestApi.getHDevicePacketList(hdevice4.getId());
        Collection<HPacket> listFourthDeviceHPackets = responseGetHDevicePacketList4.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listFourthDeviceHPackets.size());
        Assert.assertFalse(listFourthDeviceHPackets.isEmpty());
        boolean packet7Found = false;
        for (HPacket packet : listFourthDeviceHPackets) {
            if (hpacket7.getId() == packet.getId()) {
                Assert.assertEquals(hdevice4.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet7Found = true;
            }
        }
        Assert.assertTrue(packet7Found);
        Assert.assertEquals(200, responseGetHDevicePacketList4.getStatus());

        Response responseGetHDevicePacketList5 = hPacketRestApi.getHDevicePacketList(hdevice5.getId());
        Collection<HPacket> listFifthDeviceHPackets = responseGetHDevicePacketList5.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(1, listFifthDeviceHPackets.size());
        Assert.assertFalse(listFifthDeviceHPackets.isEmpty());
        boolean packet8Found = false;
        for (HPacket packet : listFifthDeviceHPackets) {
            if (hpacket8.getId() == packet.getId()) {
                Assert.assertEquals(hdevice5.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet8Found = true;
            }
        }
        Assert.assertTrue(packet8Found);
        Assert.assertEquals(200, responseGetHDevicePacketList5.getStatus());

        /*
         *
         * End: Complete hproject has been created
         *
         */

        // HPROJECT
        // huser deletes hproject with deleteHProject call and deletes all entity associated
        HyperIoTAction action5 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        addPermission(huser, action5);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
        Assert.assertNull(restResponseDeleteHProject.getEntity());

        // AREA
        // checks: areas has been deleted in cascade mode with call deleteHProject
        HyperIoTAction action6 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action6);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseFindFirstArea = areaRestApi.findArea(area1.getId());
        Assert.assertEquals(404, restResponseFindFirstArea.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindFirstArea.getEntity()).getType());
        Response restResponseFindSecondArea = areaRestApi.findArea(area2.getId());
        Assert.assertEquals(404, restResponseFindSecondArea.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindSecondArea.getEntity()).getType());

        // HDEVICE
        // checks: devices has been deleted in cascade mode with call deleteHProject
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction action7 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action7);
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseFindDevice1 = hDeviceRestApi.findHDevice(hdevice1.getId());
        Assert.assertEquals(404, responseFindDevice1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice1.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseFindDevice2 = hDeviceRestApi.findHDevice(hdevice2.getId());
        Assert.assertEquals(404, responseFindDevice2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice2.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseFindDevice3 = hDeviceRestApi.findHDevice(hdevice3.getId());
        Assert.assertEquals(404, responseFindDevice3.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice3.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseFindDevice4 = hDeviceRestApi.findHDevice(hdevice4.getId());
        Assert.assertEquals(404, responseFindDevice4.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice4.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, huser);
        Response responseFindDevice5 = hDeviceRestApi.findHDevice(hdevice5.getId());
        Assert.assertEquals(404, responseFindDevice5.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice5.getEntity()).getType());

        // HPACKET
        // checks: packets has been deleted in cascade mode with call deleteHProject
        HyperIoTAction action8 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action8);
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, responseFindPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket1.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, responseFindPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket2.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket3 = hPacketRestApi.findHPacket(hpacket3.getId());
        Assert.assertEquals(404, responseFindPacket3.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket3.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket4 = hPacketRestApi.findHPacket(hpacket4.getId());
        Assert.assertEquals(404, responseFindPacket4.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket4.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket5 = hPacketRestApi.findHPacket(hpacket5.getId());
        Assert.assertEquals(404, responseFindPacket5.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket5.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket6 = hPacketRestApi.findHPacket(hpacket6.getId());
        Assert.assertEquals(404, responseFindPacket6.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket6.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket7 = hPacketRestApi.findHPacket(hpacket7.getId());
        Assert.assertEquals(404, responseFindPacket7.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket7.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, huser);
        Response responseFindPacket8 = hPacketRestApi.findHPacket(hpacket8.getId());
        Assert.assertEquals(404, responseFindPacket8.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket8.getEntity()).getType());

        // All entities dashboards, areas, devices and packets has been deleted
        // in cascade mode with deleteHProject call
    }


    @Test
    public void test68_findAllHProjectWithPermissionShouldWorkIfListIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, find all HProject with the following call findAllHProject
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findAllHProject();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertTrue(listHProjects.isEmpty());
        Assert.assertEquals(0, listHProjects.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test69_findAllHProjectPaginatedWithPermissionShouldWorkIfListIsEmpty() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser find all HProject with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);

        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(defaultDelta, defaultPage);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertTrue(listHProjects.getResults().isEmpty());
        Assert.assertEquals(0, listHProjects.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjects.getDelta());
        Assert.assertEquals(defaultPage, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // default delta is 10, default page is 1: there are not entities stored in database
        Assert.assertEquals(0, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test70_huserTriesToBeOwnerOfProjectAssociatedWithHUser2ShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, tries to update HProject to be a new owner of HProject associated with huser2
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        huser2 = createHUser(null);
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getUsername(), huser2.getUsername());

        HProject projectOfUser2 = createHProject(huser2);
        Assert.assertNotEquals(0, projectOfUser2.getId());
        Assert.assertEquals(huser2.getId(), projectOfUser2.getUser().getId());

        Assert.assertEquals(huser2.getId(), projectOfUser2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), projectOfUser2.getUser().getId());
        projectOfUser2.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(projectOfUser2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test71_AutoregisterProjectByGatewat() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser huser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hprojectRestService, huser);
        HProject project = new HProject();
        project.setName("AutogeneratedProject");
        project.setDescription("Autogenerated description");
        project.setUser(huser);
        project.setEntityVersion(1);
        Response restResponse = hprojectRestService.saveAutoRegisteredHProject(project);
        AutoRegisterProjectCredentials cred = (AutoRegisterProjectCredentials) restResponse.getEntity();
        restResponse = hprojectRestService.createChallengeForAutoRegister(cred.getProject().getId());
        Assert.assertEquals(200, restResponse.getStatus());
        AutoRegisterChallengeRequest challengeRequest = (AutoRegisterChallengeRequest) restResponse.getEntity();
        String plainTextChallenge = challengeRequest.getPlainTextChallenge();
        List<HPacket> packets = new ArrayList<>();
        List<HPacketField> fields = new ArrayList<>();
        HPacket p = new HPacket();
        HPacketField field = new HPacketField();
        field.setPacket(p);
        field.setType(HPacketFieldType.DOUBLE);
        field.setName("temperature");
        field.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field.setEntityVersion(1);
        fields.add(field);
        p.setName("AutoRegisteredHPacket");
        p.setFormat(HPacketFormat.JSON);
        p.setSerialization(HPacketSerialization.NONE);
        p.setTimestampField("timestamp");
        p.setTimestampFormat("HH:mm:ss");
        p.setTrafficPlan(HPacketTrafficPlan.LOW);
        p.setType(HPacketType.INPUT);
        p.setEntityVersion(1);
        HDevice device = new HDevice();
        device.setProject(project);
        device.setDeviceName("hdevice1");
        device.setPassword("HDevice1.");
        device.setPasswordConfirm("HDevice1.");
        device.setEntityVersion(1);
        p.setDevice(device);
        p.setFields(new HashSet<>(fields));
        byte[] encryptedChallenge = HyperIoTSecurityUtil.encodeMessageWithPrivateKey(plainTextChallenge.getBytes(StandardCharsets.UTF_8), cred.getPrivateKey().getBytes(StandardCharsets.UTF_8));
        AutoRegisterProjectRequest autoregisterRequest = new AutoRegisterProjectRequest();
        autoregisterRequest.setProjectId(cred.getProject().getId());
        autoregisterRequest.setPackets(packets);
        autoregisterRequest.setCipherTextChallenge(new String(encryptedChallenge));
        restResponse = hprojectRestService.autoRegisterHProject(autoregisterRequest);
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
            Assert.assertEquals(hProjectResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
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
                permission.setName(hProjectResourceName + " assigned to huser_id " + huser.getId());
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

    private Permission addPermission(HUser huser, HyperIoTAction action) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser user = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, user);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        Permission permission = utilGrantPermission(huser, role, action);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertEquals(hProjectResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }

    private String testMaxDescription(int lengthDescription) {
        String symbol = "a";
        String description = String.format("%" + lengthDescription + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(lengthDescription, description.length());
        return description;
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
        addDefaultPermission(ownerHUser);
        this.impersonateUser(hDeviceRestApi, ownerHUser);
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
        Assert.assertEquals(ownerHUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        removeDefaultPermission(ownerHUser);
        return hdevice;
    }

    private HPacket createHPacketAndAddHPacketField(HDevice hdevice, boolean createField) {
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

        if (createField) {
            HPacketField field1 = new HPacketField();
            field1.setPacket(hpacket);
            field1.setName("temperature" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
            field1.setDescription("Temperature");
            field1.setType(HPacketFieldType.DOUBLE);
            field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
            field1.setValue(24.0);

            HPacketField field2 = new HPacketField();
            field2.setPacket(hpacket);
            field2.setName("humidity" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
            field2.setDescription("Humidity");
            field2.setType(HPacketFieldType.DOUBLE);
            field2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
            field2.setValue(40.00);

            hpacket.setFields(new HashSet<>() {
                {
                    add(field1);
                    add(field2);
                }
            });

            // add field1
            this.impersonateUser(hPacketRestApi, ownerHUser);
            Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
            Assert.assertEquals(200, responseAddField1.getStatus());
            Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
            Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
            Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
            Assert.assertEquals(ownerHUser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());

            // add field2
            this.impersonateUser(hPacketRestApi, ownerHUser);
            Response responseAddField2 = hPacketRestApi.addHPacketField(hpacket.getId(), field2);
            Assert.assertEquals(200, responseAddField2.getStatus());
            Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());
            Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getId());
            Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getId());
            Assert.assertEquals(ownerHUser.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getUser().getId());

            //check restResponse field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            List<HPacketField> fields = new ArrayList<>(((HPacket) restResponse.getEntity()).getFields());
            Assert.assertEquals(fields.get(0).getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

            //check restResponse field2 is equals to responseAddField2 field2
            Assert.assertEquals(field2.getId(), ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertEquals(fields.get(1).getId(), ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());

            Assert.assertEquals(2, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        removeDefaultPermission(ownerHUser);
        return hpacket;
    }

    private Area createArea(HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HUser ownerHUser = hproject.getUser();
        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        addDefaultPermission(ownerHUser);
        this.impersonateUser(areaRestApi, ownerHUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals(area.getDescription(), ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        removeDefaultPermission(ownerHUser);
        return area;
    }

    private AreaDevice createAreaDevice(Area area, HDevice hdevice) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Assert.assertEquals(area.getProject().getUser(), hdevice.getProject().getUser());
        HUser ownerHUser = area.getProject().getUser();
        addDefaultPermission(ownerHUser);
        this.impersonateUser(areaRestApi, ownerHUser);
        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(ownerHUser.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(ownerHUser.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        removeDefaultPermission(ownerHUser);
        return areaDevice;
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


    // HProject is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;
    private HUser huser2;

    @After
    public void afterTest() {
        // Remove projects and delete in cascade all devices created in every test
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
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
                    Assert.assertNull(restResponse1.getEntity());
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
//        String sqlHUser = "select h.id, h.username, h.admin from huser h";
//        String resultHUser = executeCommand("jdbc:query hyperiot " + sqlHUser);
//        System.out.println(resultHUser);
//
//        String sqlRole = "select * from role";
//        String resultRole = executeCommand("jdbc:query hyperiot " + sqlRole);
//        System.out.println(resultRole);
    }


}

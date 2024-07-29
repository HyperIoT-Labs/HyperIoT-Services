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

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.model.AutoRegisterChallengeRequest;
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
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi;
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

import static it.acsoftware.hyperiot.hproject.test.HyperIoTHProjectConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for HProject System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHProjectRestTest extends KarafTestSupport {

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
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01_hprojectModuleShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call checkModuleWorking checks if HProject module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser user = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hprojectRestService, user);
        Response restResponse = hprojectRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProject Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_hprojectModuleShouldWorkIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call checkModuleWorking checks if HProject module working
        // correctly, if HUser not logged
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.checkModuleWorking();
        Assert.assertNotNull(hprojectRestService);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProject Module works!", restResponse.getEntity());
    }

    @Test
    public void test03_saveHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin save HProject with the following call saveHProject
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Project of user: " + adminUser.getUsername());
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals("Project of user: " + adminUser.getUsername(),
                ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }

    @Test
    public void test04_saveHProjectShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to save HProject,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Project of user: " + adminUser.getUsername());
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test05_saveHProjectShouldFailIfNameIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotNull(adminUser);
        HProject hproject = new HProject();
        hproject.setName("");
        hproject.setDescription("Description");
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test06_saveHProjectShouldFailIfNameIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName(null);
        hproject.setDescription("Description");
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test07_saveHProjectShouldFailIfNameIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("<script>console.log()</script>");
        hproject.setDescription("Description");
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test08_saveHProjectShouldFailIfDescriptionIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("</script>");
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test09_saveHProjectShouldFailIfMaxDescriptionIsOver3000Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject, but
        // description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("Project " + UUID.randomUUID());
        int lengthDescription = 3001;
        hproject.setDescription(testMaxDescription(lengthDescription));
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test10_saveHProjectShouldWorkIfUserIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject,
        // but HUser is null
        // The system set automatically HProject's user as current logged user.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Description " + java.util.UUID.randomUUID());
        hproject.setUser(null);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        HProject responseEntity = (HProject) restResponse.getEntity();
        Assert.assertNotEquals(0, responseEntity.getId());
        Assert.assertNotNull( responseEntity.getUser());
        Assert.assertEquals(((HUser) adminUser).getId() , responseEntity.getUser().getId());

    }

    @Test
    public void test11_findHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin find HProject with the following call findHProject
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }

    @Test
    public void test12_findHProjectShouldFailIfEntityNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to find HProject,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.findHProject(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_findHProjectShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to find HProject,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_findAllHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin find all HProject with the following call findAllHProject
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.findAllHProject();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        Assert.assertEquals(1, listHProjects.size());
        boolean hprojectFound = false;
        for (HProject project : listHProjects) {
            if (hproject.getId() == project.getId()) {
                Assert.assertEquals(adminUser.getId(), project.getUser().getId());
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test15_findAllHProjectShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to find all HProject,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.findAllHProject();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_updateHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin update HProject with the following call updateHProject
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Date timestamp = new Date();
        hproject.setDescription("Description edited in date: " + timestamp);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hproject.getEntityVersion() + 1,
                (((HProject) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals("Description edited in date: " + timestamp,
                (((HProject) restResponse.getEntity()).getDescription()));
        Assert.assertEquals(hproject.getId(), ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }

    @Test
    public void test17_updateHProjectShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to update HProject,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hproject.setDescription("Description edited");
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test18_updateHProjectShouldFailIfNameIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        hproject.setName("");
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test19_updateHProjectShouldFailIfNameIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        hproject.setName(null);
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test20_updateHProjectShouldFailIfNameIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        hproject.setName("</script>");
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test21_updateHProjectShouldFailIfDescriptionIsMaliciousCode() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        hproject.setDescription("javascript:");
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test22_updateHProjectShouldFailIfMaxDescriptionIsOver3000Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        int lengthDescription = 3001;
        hproject.setDescription(testMaxDescription(lengthDescription));
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hproject-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(lengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }

    @Test
    public void test23_updateHProjectShouldWorkIfUserIsNull() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but HUser is null
        // The system set automatically project's user  with the user that save the project.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        hproject.setUser(null);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals( 200 , restResponse.getStatus() );
        HProject responseEntity = (HProject) restResponse.getEntity();
        Assert.assertNotEquals( 0 , responseEntity.getId());
        Assert.assertNotNull( responseEntity.getUser() );
        Assert.assertEquals( ((HUser)adminUser).getId() , responseEntity.getUser().getId() );
    }

    @Test
    public void test24_deleteHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin delete HProject with the following call deleteHProject
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test25_deleteHProjectShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to delete HProject,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test26_deleteHProjectShouldFailIfEntityNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to delete HProject,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.deleteHProject(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test27_getHProjectAreaListShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin find all HProject Area list with the following call
        // getHProjectAreaList
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertFalse(listHProjectAreas.isEmpty());
        Assert.assertEquals(1, listHProjectAreas.size());
        boolean hprojectAreaFound = false;
        for (Area a : listHProjectAreas) {
            if (area.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                hprojectAreaFound = true;
            }
        }
        Assert.assertTrue(hprojectAreaFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test28_getHProjectAreaListShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to find all HProject Area list with the following call
        // getHProjectAreaList, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test29_getHProjectAreaListShouldFailIfHProjectNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to find all HProject Area list with the following call
        // getHProjectAreaList, but HProject not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.getHProjectAreaList(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test30_getHProjectAreaListShouldWorkIfHProjectAreaListIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin find all HProject Area list with the following call
        // getHProjectAreaList, listHProjectAreas is empty
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertTrue(listHProjectAreas.isEmpty());
        Assert.assertEquals(0, listHProjectAreas.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test31_saveHProjectShouldFailIfEntityIsDuplicated() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HProject duplicateHProject = new HProject();
        duplicateHProject.setName(hproject.getName());
        duplicateHProject.setDescription("Description");
        duplicateHProject.setUser(hproject.getUser());
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test32_updateHProjectShouldFailIfEntityNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        // entity isn't stored in database
        HProject hproject = new HProject();
        hproject.setDescription("Description edited");
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test33_getProjectTreeViewJsonShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin finds all HPacket by hprojectId with the following call getProjectTreeViewJson
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {});
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(3, listHPackets.size());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        boolean hpacket3Found = false;
        for (HPacket packet : listHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket2Found = true;
            }
            if (hpacket3.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket3Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertTrue(hpacket3Found);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test34_getProjectTreeViewJsonShouldWorkIfHProjectNotHaveHPacket() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin finds all HPacket by hprojectId with the following call getHProjectTreeView
        // but hproject not have a hpacket. This call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {});
        Assert.assertTrue(listHPackets.isEmpty());
        Assert.assertEquals(0, listHPackets.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test35_getProjectTreeViewJsonShouldFailIfHProjectNotFound() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to find all HPacket by hprojectId with the following
        // call getHProjectTreeView, but hproject not found.
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.getHProjectTreeView(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test36_getProjectTreeViewJsonShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to find all HPacket by hprojectId with the
        // following call getProjectTreeViewJson, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(adminUser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(adminUser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test37_cardsViewShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin find all HProject with the following call cardsView
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.cardsView();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        Assert.assertEquals(1, listHProjects.size());
        boolean hprojectFound = false;
        for (HProject project : listHProjects) {
            if (hproject.getId() == project.getId()) {
                Assert.assertEquals(adminUser.getId(), project.getUser().getId());
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test38_cardsViewShouldFailIfNotLogged() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to find all HProject,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(hprojectRestService, null);
        Response restResponse = hprojectRestService.cardsView();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test39_createChallengeForAutoRegisterShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin create challenge for AutoRegister HProject
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.createChallengeForAutoRegister(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotNull(((AutoRegisterChallengeRequest) restResponse.getEntity()).getPlainTextChallenge());
    }


    @Test
    public void test40_createChallengeForAutoRegisterShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin create challenge for AutoRegister HProject
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.createChallengeForAutoRegister(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test41_updateHProjectShouldFailIfEntityIsDuplicated() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but hproject is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HProject duplicateHProject = createHProject();
        Assert.assertNotEquals(0, duplicateHProject.getId());
        Assert.assertEquals(adminUser.getId(), duplicateHProject.getUser().getId());

        duplicateHProject.setName(hproject.getName());
        duplicateHProject.setUser(hproject.getUser());
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test42_findAllHProjectPaginatedShouldWork() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, hadmin finds all
        // HProjects with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = 2;
        List<HProject> hprojects = new ArrayList<>();
        int numbEntities = 9;
        for (int i = 0; i < numbEntities; i++) {
            HProject hproject = createHProject();
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(numbEntities, hprojects.size());
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listHProjects.getResults().size());
        Assert.assertEquals(delta, listHProjects.getDelta());
        Assert.assertEquals(page, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // delta is 5, page 2: 9 entities stored in database
        Assert.assertEquals(2, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponsePage1 = hProjectRestApi.findAllHProjectPaginated(delta, 1);
        HyperIoTPaginableResult<HProject> listHProjectsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjectsPage1.getResults().size());
        Assert.assertEquals(delta, listHProjectsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectsPage1.getCurrentPage());
        Assert.assertEquals(page, listHProjectsPage1.getNextPage());
        // delta is 5, page is 1: 9 entities stored in database
        Assert.assertEquals(2, listHProjectsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test43_findAllHProjectPaginatedShouldFailIfNotLogged() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // the following call tries to find all HProjects with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(hProjectRestApi, null);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(null, null);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test44_findAllHProjectPaginatedShouldWorkIfDeltaAndPageAreNull() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, hadmin find all HProjects with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        List<HProject> hprojects = new ArrayList<>();
        int numbEntities = 6;
        for (int i = 0; i < numbEntities; i++) {
            HProject hproject = createHProject();
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(numbEntities, hprojects.size());
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
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
    public void test45_findAllHProjectPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, hadmin find all HProjects with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 1;
        List<HProject> hprojects = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HProject hproject = createHProject();
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(defaultDelta, hprojects.size());
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponsePage1 = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjectsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getDelta());
        Assert.assertEquals(page, listHProjectsPage1.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectsPage1.getNextPage());
        // default delta is 10, page is 1: 10 entities stored in database
        Assert.assertEquals(1, listHProjectsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        // checks with page = 2 (no result showed)
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponsePage2 = hProjectRestApi.findAllHProjectPaginated(delta, 2);
        HyperIoTPaginableResult<HProject> listHProjectsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertTrue(listHProjectsPage2.getResults().isEmpty());
        Assert.assertEquals(0, listHProjectsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHProjectsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectsPage2.getNextPage());
        // default delta is 10, page is 2: 10 entities stored in database
        Assert.assertEquals(1, listHProjectsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test46_findAllHProjectPaginatedShouldWorkIfDeltaIsZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, hadmin find all HProjects with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 3;
        List<HProject> hprojects = new ArrayList<>();
        int numEntities = 25;
        for (int i = 0; i < numEntities; i++) {
            HProject hproject = createHProject();
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(numEntities, hprojects.size());
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(numEntities - (defaultDelta * 2), listHProjects.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjects.getDelta());
        Assert.assertEquals(page, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // because delta is 10, page is 3: 25 entities stored in database
        Assert.assertEquals(3, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponsePage1 = hProjectRestApi.findAllHProjectPaginated(delta, 1);
        HyperIoTPaginableResult<HProject> listHProjectsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectsPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHProjectsPage1.getNextPage());
        // default delta is 10, page is 1: 25 entities stored in database
        Assert.assertEquals(3, listHProjectsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponsePage2 = hProjectRestApi.findAllHProjectPaginated(delta, 2);
        HyperIoTPaginableResult<HProject> listHProjectsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHProjectsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHProjectsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHProjectsPage2.getCurrentPage());
        Assert.assertEquals(page, listHProjectsPage2.getNextPage());
        // default delta is 10, page is 2: 25 entities stored in database
        Assert.assertEquals(3, listHProjectsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test47_findAllHProjectPaginatedShouldWorkIfPageIsLowerThanZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, hadmin find all HProjects with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = -1;
        List<HProject> hprojects = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HProject hproject = createHProject();
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(delta, hprojects.size());
        this.impersonateUser(hProjectRestApi, adminUser);
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
    public void test48_findAllHProjectPaginatedShouldWorkIfPageIsZero() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, hadmin find all HProjects with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 8;
        int page = 0;
        List<HProject> hprojects = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HProject hproject = createHProject();
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(defaultDelta, hprojects.size());
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjects.getResults().size());
        Assert.assertEquals(delta, listHProjects.getDelta());
        Assert.assertEquals(defaultPage, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHProjects.getNextPage());
        // delta is 8, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponsePage2 = hProjectRestApi.findAllHProjectPaginated(delta, 2);
        HyperIoTPaginableResult<HProject> listHProjectsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listHProjectsPage2.getResults().size());
        Assert.assertEquals(delta, listHProjectsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHProjectsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectsPage2.getNextPage());
        // delta is 8, page is 2: 10 entities stored in database
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
    public void test52_deleteHProjectWithAreaDeleteInCascadeAllAreas() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes HProject, with Area associated, with deleteHProject call,
        // this call deletes in cascade all areas
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area2.getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, adminUser);
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
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // hadmin deletes hproject with call deleteHProject
        // this call deletes in cascade all areas
        this.impersonateUser(hprojectRestService, adminUser);
        Response responseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());
        Assert.assertNull(responseDeleteHProject.getEntity());

        // checks if areas exists in database
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        this.impersonateUser(areaRestApi, adminUser);
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
    public void test53_deleteAreaWithHProjectNotDeleteInCascadeHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes areas, with HProject associated, with the following call deleteArea,
        // hproject is not deleted in cascade mode
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area2.getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, adminUser);
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
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // deletes areas with deleteArea calls
        // this calls not deletes hproject in cascade
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        this.impersonateUser(areaRestApi, adminUser);
        Response responseDeleteArea1 = areaRestApi.deleteArea(area1.getId());
        Assert.assertEquals(200, responseDeleteArea1.getStatus());
        Assert.assertNull(responseDeleteArea1.getEntity());

        this.impersonateUser(areaRestApi, adminUser);
        Response responseDeleteArea2 = areaRestApi.deleteArea(area2.getId());
        Assert.assertEquals(200, responseDeleteArea2.getStatus());
        Assert.assertNull(responseDeleteArea2.getEntity());

        // checks if hproject is already stored in database
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseHProject.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponseHProject.getEntity()).getUser().getId());

        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseHProjectAreaList = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreasEmpty = restResponseHProjectAreaList.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertEquals(0, listHProjectAreasEmpty.size());
        Assert.assertTrue(listHProjectAreasEmpty.isEmpty());
        Assert.assertEquals(200, restResponseHProjectAreaList.getStatus());
    }

    @Test
    public void test54_deleteHProjectWithHDeviceDeleteInCascadeAllHDevices() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes HProject, with HDevice associated, with the following
        // call deleteHProject, this call deletes in cascade all devices
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice2.getProject().getUser().getId());

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
                Assert.assertEquals(hdevice2.getId(), device.getId());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                device2Found = true;
            }
        }
        Assert.assertTrue(device1Found);
        Assert.assertTrue(device2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // hadmin deletes hproject with call deleteHProject
        // this call deletes in cascade all devices
        this.impersonateUser(hprojectRestService, adminUser);
        Response responseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());
        Assert.assertNull(responseDeleteHProject.getEntity());

        // checks if devices exists in database
        this.impersonateUser(hDeviceRestApi, adminUser);
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
    public void test55_deleteHDeviceWithHProjectNotDeleteInCascadeHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes devices, with HProject associated, with deleteHDevice calls,
        // hproject is not deleted in cascade mode
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice2.getProject().getUser().getId());

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
                Assert.assertEquals(hdevice2.getId(), device.getId());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                device2Found = true;
            }
        }
        Assert.assertTrue(device1Found);
        Assert.assertTrue(device2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // deletes devices with deleteHDevice calls
        // this calls not deletes hproject in cascade
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseDeleteHDevice1 = hDeviceRestApi.deleteHDevice(hdevice1.getId());
        Assert.assertEquals(200, responseDeleteHDevice1.getStatus());
        Assert.assertNull(responseDeleteHDevice1.getEntity());

        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseDeleteHDevice2 = hDeviceRestApi.deleteHDevice(hdevice2.getId());
        Assert.assertEquals(200, responseDeleteHDevice2.getStatus());
        Assert.assertNull(responseDeleteHDevice2.getEntity());

        // checks if hproject is still stored in database
        this.impersonateUser(hprojectRestService, adminUser);
        Response responseFindHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, responseFindHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) responseFindHProject.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) responseFindHProject.getEntity()).getUser().getId());

        // hproject hasn't devices associated
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseDeviceByProject = hDeviceRestApi.findAllHDeviceByProjectId(hproject.getId());
        Collection<HDevice> listDeviceByProject = responseDeviceByProject.readEntity(new GenericType<Collection<HDevice>>() {
        });
        Assert.assertEquals(0, listDeviceByProject.size());
        Assert.assertTrue(listDeviceByProject.isEmpty());
        Assert.assertEquals(200, responseDeviceByProject.getStatus());
    }

    @Test
    public void test56_createCompleteHProjectWithAreasDashboardsDashboardWidgetsHDevicesHPackets(){
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin creates complete hproject with dashboards, widgets, areas, devices and packets
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        /*
         *
         * Start: Create complete hproject with dashboards, widgets, areas, devices and packets
         *
         */

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area2.getProject().getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice2.getProject().getUser().getId());

        HDevice hdevice3 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice3.getId());
        Assert.assertEquals(hproject.getId(), hdevice3.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice3.getProject().getUser().getId());

        HDevice hdevice4 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice4.getId());
        Assert.assertEquals(hproject.getId(), hdevice4.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice4.getProject().getUser().getId());

        HDevice hdevice5 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice5.getId());
        Assert.assertEquals(hproject.getId(), hdevice5.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice5.getProject().getUser().getId());

        AreaDevice ad1 = createAreaDevice(area1, hdevice1);
        Assert.assertNotEquals(0, ad1.getId());
        Assert.assertEquals(hdevice1.getId(), ad1.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad1.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad1.getArea().getProject().getUser().getId());

        AreaDevice ad2 = createAreaDevice(area1, hdevice2);
        Assert.assertNotEquals(0, ad2.getId());
        Assert.assertEquals(hdevice2.getId(), ad2.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad2.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad2.getArea().getProject().getUser().getId());

        AreaDevice ad3 = createAreaDevice(area2, hdevice3);
        Assert.assertNotEquals(0, ad3.getId());
        Assert.assertEquals(hdevice3.getId(), ad3.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad3.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad3.getArea().getProject().getUser().getId());

        AreaDevice ad4 = createAreaDevice(area2, hdevice4);
        Assert.assertNotEquals(0, ad4.getId());
        Assert.assertEquals(hdevice4.getId(), ad4.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad4.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad4.getArea().getProject().getUser().getId());

        AreaDevice ad5 = createAreaDevice(area2, hdevice5);
        Assert.assertNotEquals(0, ad5.getId());
        Assert.assertEquals(hdevice5.getId(), ad5.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad5.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad5.getArea().getProject().getUser().getId());

        // creates hpackets
        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        HPacket hpacket4 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket4.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket4.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket4.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket4.getDevice().getProject().getUser().getId());

        HPacket hpacket5 = createHPacketAndAddHPacketField(hdevice2, false);
        Assert.assertNotEquals(0, hpacket5.getId());
        Assert.assertEquals(hdevice2.getId(), hpacket5.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket5.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket5.getDevice().getProject().getUser().getId());

        HPacket hpacket6 = createHPacketAndAddHPacketField(hdevice3, false);
        Assert.assertNotEquals(0, hpacket6.getId());
        Assert.assertEquals(hdevice3.getId(), hpacket6.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket6.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket6.getDevice().getProject().getUser().getId());

        HPacket hpacket7 = createHPacketAndAddHPacketField(hdevice4, false);
        Assert.assertNotEquals(0, hpacket7.getId());
        Assert.assertEquals(hdevice4.getId(), hpacket7.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket7.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket7.getDevice().getProject().getUser().getId());

        HPacket hpacket8 = createHPacketAndAddHPacketField(hdevice5, false);
        Assert.assertNotEquals(0, hpacket8.getId());
        Assert.assertEquals(hdevice5.getId(), hpacket8.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket8.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket8.getDevice().getProject().getUser().getId());

        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        this.impersonateUser(areaRestApi, adminUser);
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
        Assert.assertEquals(adminUser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        AreaDevice areaDevice2 = listAreaDevices1.get(1);
        Assert.assertNotEquals(0, areaDevice2.getId());
        Assert.assertEquals(hdevice2.getId(), areaDevice2.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice2.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice2.getArea().getProject().getUser().getId());

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
        Assert.assertEquals(adminUser.getId(), areaDevice3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice3.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice3.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice3.getId(), ad3.getId());
        Assert.assertEquals(areaDevice3.getDevice().getId(), ad3.getDevice().getId());
        Assert.assertEquals(areaDevice3.getArea().getId(), ad3.getArea().getId());

        AreaDevice areaDevice4 = listSecondAreaDevices.get(1);
        Assert.assertNotEquals(0, areaDevice4.getId());
        Assert.assertEquals(hdevice4.getId(), areaDevice4.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice4.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice4.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice4.getId(), ad4.getId());
        Assert.assertEquals(areaDevice4.getDevice().getId(), ad4.getDevice().getId());
        Assert.assertEquals(areaDevice4.getArea().getId(), ad4.getArea().getId());

        AreaDevice areaDevice5 = listSecondAreaDevices.get(2);
        Assert.assertNotEquals(0, areaDevice5.getId());
        Assert.assertEquals(hdevice5.getId(), areaDevice5.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice5.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice5.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice5.getId(), ad5.getId());
        Assert.assertEquals(areaDevice5.getDevice().getId(), ad5.getDevice().getId());
        Assert.assertEquals(areaDevice5.getArea().getId(), ad5.getArea().getId());

        //checks if Area HProject exists
        this.impersonateUser(hprojectRestService, adminUser);
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
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, responseProjectArea.getStatus());

        //checks if device packets exists
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet2Found = true;
            }
            if (hpacket3.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet3Found = true;
            }
            if (hpacket4.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
    public void test57_deleteCompleteHProjectDeleteInCascadeAllAreasDashboardsDashboardWidgetsHDevicesHPackets(){
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes hproject with the following call deleteHProject; this call deletes
        // all entity associated (dashboards, widgets, areas, devices and packets, AreaDevice)
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        /*
         *
         * Start: Create complete hproject with dashboards, widgets, areas, devices and packets
         *
         */


        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area1.getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area2.getProject().getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice2.getProject().getUser().getId());

        HDevice hdevice3 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice3.getId());
        Assert.assertEquals(hproject.getId(), hdevice3.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice3.getProject().getUser().getId());

        HDevice hdevice4 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice4.getId());
        Assert.assertEquals(hproject.getId(), hdevice4.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice4.getProject().getUser().getId());

        HDevice hdevice5 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice5.getId());
        Assert.assertEquals(hproject.getId(), hdevice5.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice5.getProject().getUser().getId());

        AreaDevice ad1 = createAreaDevice(area1, hdevice1);
        Assert.assertNotEquals(0, ad1.getId());
        Assert.assertEquals(hdevice1.getId(), ad1.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad1.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad1.getArea().getProject().getUser().getId());

        AreaDevice ad2 = createAreaDevice(area1, hdevice2);
        Assert.assertNotEquals(0, ad2.getId());
        Assert.assertEquals(hdevice2.getId(), ad2.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad2.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad2.getArea().getProject().getUser().getId());

        AreaDevice ad3 = createAreaDevice(area2, hdevice3);
        Assert.assertNotEquals(0, ad3.getId());
        Assert.assertEquals(hdevice3.getId(), ad3.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad3.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad3.getArea().getProject().getUser().getId());

        AreaDevice ad4 = createAreaDevice(area2, hdevice4);
        Assert.assertNotEquals(0, ad4.getId());
        Assert.assertEquals(hdevice4.getId(), ad4.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad4.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad4.getArea().getProject().getUser().getId());

        AreaDevice ad5 = createAreaDevice(area2, hdevice5);
        Assert.assertNotEquals(0, ad5.getId());
        Assert.assertEquals(hdevice5.getId(), ad5.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), ad5.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad5.getArea().getProject().getUser().getId());

        // creates hpackets
        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        HPacket hpacket4 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket4.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket4.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket4.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket4.getDevice().getProject().getUser().getId());

        HPacket hpacket5 = createHPacketAndAddHPacketField(hdevice2, false);
        Assert.assertNotEquals(0, hpacket5.getId());
        Assert.assertEquals(hdevice2.getId(), hpacket5.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket5.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket5.getDevice().getProject().getUser().getId());

        HPacket hpacket6 = createHPacketAndAddHPacketField(hdevice3, false);
        Assert.assertNotEquals(0, hpacket6.getId());
        Assert.assertEquals(hdevice3.getId(), hpacket6.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket6.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket6.getDevice().getProject().getUser().getId());

        HPacket hpacket7 = createHPacketAndAddHPacketField(hdevice4, false);
        Assert.assertNotEquals(0, hpacket7.getId());
        Assert.assertEquals(hdevice4.getId(), hpacket7.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket7.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket7.getDevice().getProject().getUser().getId());

        HPacket hpacket8 = createHPacketAndAddHPacketField(hdevice5, false);
        Assert.assertNotEquals(0, hpacket8.getId());
        Assert.assertEquals(hdevice5.getId(), hpacket8.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket8.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket8.getDevice().getProject().getUser().getId());

        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        this.impersonateUser(areaRestApi, adminUser);
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
        Assert.assertEquals(adminUser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        AreaDevice areaDevice2 = listAreaDevices1.get(1);
        Assert.assertNotEquals(0, areaDevice2.getId());
        Assert.assertEquals(hdevice2.getId(), areaDevice2.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice2.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice2.getArea().getProject().getUser().getId());

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
        Assert.assertEquals(adminUser.getId(), areaDevice3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice3.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice3.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice3.getId(), ad3.getId());
        Assert.assertEquals(areaDevice3.getDevice().getId(), ad3.getDevice().getId());
        Assert.assertEquals(areaDevice3.getArea().getId(), ad3.getArea().getId());

        AreaDevice areaDevice4 = listSecondAreaDevices.get(1);
        Assert.assertNotEquals(0, areaDevice4.getId());
        Assert.assertEquals(hdevice4.getId(), areaDevice4.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice4.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice4.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice4.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice4.getId(), ad4.getId());
        Assert.assertEquals(areaDevice4.getDevice().getId(), ad4.getDevice().getId());
        Assert.assertEquals(areaDevice4.getArea().getId(), ad4.getArea().getId());

        AreaDevice areaDevice5 = listSecondAreaDevices.get(2);
        Assert.assertNotEquals(0, areaDevice5.getId());
        Assert.assertEquals(hdevice5.getId(), areaDevice5.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice5.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area2.getId(), areaDevice5.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice5.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice5.getId(), ad5.getId());
        Assert.assertEquals(areaDevice5.getDevice().getId(), ad5.getDevice().getId());
        Assert.assertEquals(areaDevice5.getArea().getId(), ad5.getArea().getId());

        //checks if Area HProject exists
        this.impersonateUser(hprojectRestService, adminUser);
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
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == area.getId()) {
                Assert.assertEquals(area2.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), area.getProject().getId());
                Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, responseProjectArea.getStatus());

        //checks if device packets exists
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet2Found = true;
            }
            if (hpacket3.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet3Found = true;
            }
            if (hpacket4.getId() == packet.getId()) {
                Assert.assertEquals(hdevice1.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet4Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertTrue(packet3Found);
        Assert.assertTrue(packet4Found);
        Assert.assertEquals(200, responseGetHDevicePacketList1.getStatus());

        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet5Found = true;
            }
        }
        Assert.assertTrue(packet5Found);
        Assert.assertEquals(200, responseGetHDevicePacketList2.getStatus());

        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet6Found = true;
            }
        }
        Assert.assertTrue(packet6Found);
        Assert.assertEquals(200, responseGetHDevicePacketList3.getStatus());

        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet7Found = true;
            }
        }
        Assert.assertTrue(packet7Found);
        Assert.assertEquals(200, responseGetHDevicePacketList4.getStatus());

        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
        // hadmin deletes hproject with deleteHProject call and deletes all entity associated
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
        Assert.assertNull(restResponseDeleteHProject.getEntity());


        // AREA
        // checks: areas has been deleted in cascade mode with call deleteHProject
        this.impersonateUser(hprojectRestService, adminUser);
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
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseFindDevice1 = hDeviceRestApi.findHDevice(hdevice1.getId());
        Assert.assertEquals(404, responseFindDevice1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice1.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseFindDevice2 = hDeviceRestApi.findHDevice(hdevice2.getId());
        Assert.assertEquals(404, responseFindDevice2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice2.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseFindDevice3 = hDeviceRestApi.findHDevice(hdevice3.getId());
        Assert.assertEquals(404, responseFindDevice3.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice3.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseFindDevice4 = hDeviceRestApi.findHDevice(hdevice4.getId());
        Assert.assertEquals(404, responseFindDevice4.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice4.getEntity()).getType());
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response responseFindDevice5 = hDeviceRestApi.findHDevice(hdevice5.getId());
        Assert.assertEquals(404, responseFindDevice5.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDevice5.getEntity()).getType());

        // HPACKET
        // checks: packets has been deleted in cascade mode with call deleteHProject
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, responseFindPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket1.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, responseFindPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket2.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket3 = hPacketRestApi.findHPacket(hpacket3.getId());
        Assert.assertEquals(404, responseFindPacket3.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket3.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket4 = hPacketRestApi.findHPacket(hpacket4.getId());
        Assert.assertEquals(404, responseFindPacket4.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket4.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket5 = hPacketRestApi.findHPacket(hpacket5.getId());
        Assert.assertEquals(404, responseFindPacket5.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket5.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket6 = hPacketRestApi.findHPacket(hpacket6.getId());
        Assert.assertEquals(404, responseFindPacket6.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket6.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket7 = hPacketRestApi.findHPacket(hpacket7.getId());
        Assert.assertEquals(404, responseFindPacket7.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket7.getEntity()).getType());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseFindPacket8 = hPacketRestApi.findHPacket(hpacket8.getId());
        Assert.assertEquals(404, responseFindPacket8.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindPacket8.getEntity()).getType());

        // All entities dashboards, areas, devices and packets has been deleted
        // in cascade mode with deleteHProject call
    }


    @Test
    public void test58_findAllHProjectWithPermissionShouldWorkIfListIsEmpty() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin find all HProject with the following call findAllHProject
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.findAllHProject();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertTrue(listHProjects.isEmpty());
        Assert.assertEquals(0, listHProjects.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test59_findAllHProjectPaginatedWithPermissionShouldWorkIfListIsEmpty() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, hadmin find all HProject with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        this.impersonateUser(hProjectRestApi, adminUser);
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
    public void test60_checkIfHProjectIsASharedEntity() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // this test checks if HProject is make a Shared Entity
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = new SharedEntity();
        sharedEntity.setEntityId(hproject.getId());
        sharedEntity.setEntityResourceName(hProjectResourceName);
        sharedEntity.setUserId(huser.getId());

        this.impersonateUser(sharedEntityRestApi, adminUser);
        Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hproject.getId(), ((SharedEntity) restResponse.getEntity()).getEntityId());
        Assert.assertEquals(huser.getId(), ((SharedEntity) restResponse.getEntity()).getUserId());
        Assert.assertEquals(hProjectResourceName, ((SharedEntity) restResponse.getEntity()).getEntityResourceName());
    }


    @Test
    public void test61_huserUpdateProjectSharedAfterSharedOperationShouldWork() {
        // hadmin save SharedEntity with the following call saveSharedEntity,
        // after shared operation huser update entityExample
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his hproject with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action, 0);
        // add specific permission with resourceId
        addPermission(huser, action, hproject.getId());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        hproject.setDescription("Description edited by huser: " + huser.getUsername());

        this.impersonateUser(hProjectRestApi, huser);
        Response restResponseUpdateProject = hProjectRestApi.updateHProject(hproject);
        Assert.assertEquals(200, restResponseUpdateProject.getStatus());
        Assert.assertEquals(hproject.getId(),
                ((HProject) restResponseUpdateProject.getEntity()).getId());
        Assert.assertEquals("Description edited by huser: " + huser.getUsername(),
                ((HProject) restResponseUpdateProject.getEntity()).getDescription());
        Assert.assertEquals(hproject.getEntityVersion() + 1,
                (((HProject) restResponseUpdateProject.getEntity()).getEntityVersion()));
        Assert.assertEquals(adminUser.getId(),
                ((HProject) restResponseUpdateProject.getEntity()).getUser().getId());
    }


    @Test
    public void test62_huserWithoutPermissionTriesToUpdateProjectSharedAfterSharedOperationShouldFail() {
        // hadmin save SharedEntity with the following call saveSharedEntity,
        // huser, without permission, tries to update hproject after shared entity operation
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        hproject.setDescription("Description edited by huser: " + huser.getUsername());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.updateHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test63_huserWithoutSpecificPermissionTriesToUpdateProjectSharedAfterSharedOperationShouldSuccess() {
        // hadmin save SharedEntity with the following call saveSharedEntity,
        // huser, without specific permission, tries to update project after shared entity operation
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        //Create a specific role and after that add the permission to this specific role
        Role specificRole = createRole();
        //Add specific permission to HProject saved before
        PermissionSystemApi permissionSystemService = getOsgiService(PermissionSystemApi.class);
        List<HyperIoTAction> updateAction = new LinkedList<>();
        updateAction.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,HyperIoTCrudAction.UPDATE));
        permissionSystemService.checkOrCreateRoleWithPermissionsSpecificToEntity(specificRole.getName(),hproject.getId(),updateAction);

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action, 0);
        // specific permission isn't assigned

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        hproject.setDescription("Description edited by huser: " + huser.getUsername());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.updateHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test64_huser2TriesToUpdateProjectShouldFailIfItIsNotAssociatedWithSharedEntity() {
        // adminUser save Project with the following call saveHProject, and
        // huser2 tries to update project, after shared operation, with the following call updateHProject,
        // huser2 has permission (UPDATE) but it's unauthorized because isn't associated with shared entity
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // huser2 isn't associated in SharedEntity and isn't the owner hproject
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser2 = createHUser(null);
        addPermission(huser2, action, 0);
        // add specific permission with resourceId
        addPermission(huser2, action, hproject.getId());

        hproject.setDescription("Description edited by huser: " + huser2.getUsername());
        this.impersonateUser(hProjectRestApi, huser2);
        Response restResponse = hProjectRestApi.updateHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test65_huserInsertedInSharedEntityTriesToBecomeNewOwnerResource() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // hadmin save SharedEntity with the following call saveSharedEntity, after shared operation
        // huser tries to be owner of entity example with the following call updateSharedEntityExample
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area1.getProject().getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action, 0);
        // add specific permission with resourceId
        addPermission(huser, action, hproject.getId());

        hproject.setUser(huser);
        this.impersonateUser(hProjectRestApi, huser);
        // user on shared resource cannot change the owner
        Response restResponse = hProjectRestApi.updateHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError)restResponse.getEntity()).getType());
    }


    @Test
    public void test66_huserTriesToMakeHUser2NewOwnerOfHProjectSharedAfterSharedOperation() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // hadmin save SharedEntity with the following call saveSharedEntity, and
        // huser tries to make huser2 the new owner of hproject;
        // huser is associated with hproject, but that isn't an allowed operation
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area1.getProject().getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice1, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice1.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        // adminUser share his hproject with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser2 isn't associated in SharedEntity and isn't the owner hproject
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action, 0);
        // add specific permission with resourceId
        addPermission(huser, action, hproject.getId());

        HUser huser2 = createHUser(null);
        hproject.setUser(huser2);
        this.impersonateUser(hprojectRestService, huser);
        // user on shared resource cannot change the owner
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError)restResponse.getEntity()).getType());
    }


    @Test
    public void test67_huserFindProjectSharedAfterSharedOperationShouldWork() {
        // hadmin save Project with the following call saveHProject, and
        // huser find project after shared entity operation
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // huser find project after shared operation
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action, 0);
        // add specific permission with resourceId
        addPermission(huser, action, hproject.getId());

        this.impersonateUser(hProjectRestApi, huser);
        Response responseFindProject = hProjectRestApi.findHProject(hproject.getId());
        Assert.assertEquals(200, responseFindProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) responseFindProject.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) responseFindProject.getEntity()).getUser().getId());
    }


    @Test
    public void test68_huserWithoutPermissionTriesToFindProjectSharedAfterSharedOperationShouldFail() {
        // hadmin save Project with the following call saveHProject.
        // huser, without permission, tries to find project after shared entity operation
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // huser tries to find project after shared operation
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findHProject(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
    }


    @Test
    public void test69_huserWithoutSpecificPermissionTriesToFindProjectSharedAfterSharedOperationShouldSuccess() {
        // hadmin save Project with the following call saveHProject.
        // huser, without specific permission, tries to find Project after shared entity operation
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        //Create a specific role and after that add the permission to this specific role
        Role specificRole = createRole();
        //Add specific permission to HProject saved before
        PermissionSystemApi permissionSystemService = getOsgiService(PermissionSystemApi.class);
        List<HyperIoTAction> findAction = new LinkedList<>();
        findAction.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,HyperIoTCrudAction.FIND));
        permissionSystemService.checkOrCreateRoleWithPermissionsSpecificToEntity(specificRole.getName(),hproject.getId(),findAction);

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // huser tries to find project after shared operation
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action, 0);
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test70_huserFindAllProjectsSharedAfterSharedOperation() {
        // hadmin save Project with the following call saveHProject, and
        // huser find all hprojects after shared operation
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject1 = createHProject();
        Assert.assertNotEquals(0, hproject1.getId());
        Assert.assertEquals(adminUser.getId(), hproject1.getUser().getId());

        HProject hproject2 = createHProject();
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(adminUser.getId(), hproject2.getUser().getId());

        HProject hproject3 = createHProject();
        Assert.assertNotEquals(0, hproject3.getId());
        Assert.assertEquals(adminUser.getId(), hproject3.getUser().getId());

        // adminUser share his entity with huser
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);

        SharedEntity sharedEntity1 = createSharedEntity(hproject1, (HUser) adminUser, huser);
        Assert.assertEquals(hproject1.getId(), sharedEntity1.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity1.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity1.getEntityResourceName());

        // second SharedEntity
        SharedEntity sharedEntity2 = createSharedEntity(hproject2, (HUser) adminUser, huser);
        Assert.assertEquals(hproject2.getId(), sharedEntity2.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity2.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity2.getEntityResourceName());

        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findAllHProject();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        Assert.assertEquals(2, listHProjects.size());
        // hproject3 isn't not shared
        boolean entityFound1 = false;
        boolean entityFound2 = false;
        for (HProject project : listHProjects) {
            if (hproject1.getId() == project.getId()) {
                Assert.assertEquals(adminUser.getId(), project.getUser().getId());
                entityFound1 = true;
            }
            if (hproject2.getId() == project.getId()) {
                Assert.assertEquals(adminUser.getId(), project.getUser().getId());
                entityFound2 = true;
            }
        }
        Assert.assertTrue(entityFound1);
        Assert.assertTrue(entityFound2);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test71_huserDeleteProjectAfterSharedOperationShouldWork() {
        // hadmin save Project with the following call saveHProject, and
        // huser delete Project after shared operation
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        addPermission(huser, action, 0);
        // add specific permission with resourceId
        addPermission(huser, action, hproject.getId());

        this.impersonateUser(hProjectRestApi, huser);
        Response restResponseDeleteHProject = hProjectRestApi.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
        Assert.assertNull(restResponseDeleteHProject.getEntity());
    }


    @Test
    public void test72_huserWithoutPermissionTriesToDeleteProjectAfterSharedOperationShouldFail() {
        // hadmin save Project with the following call saveHProject, and
        // huser, without permission, tries to delete Project after shared entity operation
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.deleteHProject(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
    }


    @Test
    public void test73_huserWithoutSpecificPermissionTriesToDeleteProjectAfterSharedOperationShouldSuccess() {
        // hadmin save Project with the following call saveHProject, and
        // huser, without specific permission, tries to delete Project after shared entity operation
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        //Create a specific role and after that add the permission to this specific role
        Role specificRole = createRole();
        //Add specific permission to HProject saved before
        PermissionSystemApi permissionSystemService = getOsgiService(PermissionSystemApi.class);
        List<HyperIoTAction> removeAction = new LinkedList<>();
        removeAction.add(HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,HyperIoTCrudAction.REMOVE));
        permissionSystemService.checkOrCreateRoleWithPermissionsSpecificToEntity(specificRole.getName(),hproject.getId(),removeAction);

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        addPermission(huser, action, 0);
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test74_huser2TriesToDeleteProjectShouldFailIfItIsNotAssociatedWithSharedEntity() {
        // adminUser save Project with the following call saveHProject, and
        // huser2 tries to delete project, after shared operation, with the following call deleteHProject,
        // huser2 has permission (REMOVE) but it's unauthorized because isn't associated with shared entity
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        // adminUser share his entity with huser
        HUser huser = createHUser(null);

        SharedEntity sharedEntity = createSharedEntity(hproject, (HUser) adminUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // huser2 isn't associated in SharedEntity and isn't the owner hproject
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser2 = createHUser(null);
        addPermission(huser2, action, 0);
        // add specific permission with resourceId
        addPermission(huser2, action, hproject.getId());

        this.impersonateUser(hProjectRestApi, huser2);
        Response restResponse = hProjectRestApi.deleteHProject(hproject.getId());
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test75_saveHProjectShouldFailIfNameIsGreaterThan255Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to save HProject with the following call saveHProject,
        // but name is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName(createStringFieldWithSpecifiedLenght(256));
        hproject.setDescription("Description");
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test76_updateHProjectShouldFailIfNameIsGreaterThan255Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // but name is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        hproject.setName(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hprojectRestService, adminUser);
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
    public void test77_saveHProjectShouldWorkIfDescriptionIs3000Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin save HProject with the following call saveHProject
        // description's length is 3000 chars.
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription(createStringFieldWithSpecifiedLenght(3000));
        hproject.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals(3000, ((HProject)restResponse.getEntity()).getDescription().length());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }

    @Test
    public void test78_updateHProjectShouldWorkIfDescriptionIs3000Chars() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject with the following call updateHProject,
        // description's length is 3000 chars.
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        hproject.setDescription(createStringFieldWithSpecifiedLenght(3000));
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(200 , restResponse.getStatus());
        Assert.assertEquals(3000, ((HProject)restResponse.getEntity()).getDescription().length());
    }

    @Test
    public void test79_updateHProjectOwnerShouldWork(){
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin tries to update HProject's owner with the following call updateHProject,
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        HUser user = createHUser(null);
        Assert.assertNotEquals(0, user.getId());
        Response restResponse = hprojectRestService.updateHProjectOwner(hproject.getId(), user.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        impersonateUser(hprojectRestService, adminUser);
        HProject responseProject = (HProject) restResponse.getEntity();
        Assert.assertEquals(responseProject.getId(), hproject.getId());
        Assert.assertNotNull(responseProject.getUser());
        Assert.assertEquals(responseProject.getUser().getId(), user.getId());

    }

    @Test
    public void test80_updateHProjectOwnerShouldFailIfNotLogged(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to update HProject's owner
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        HUser user = createHUser(null);
        Assert.assertNotEquals(0, user.getId());
        impersonateUser(hProjectRestService, null);
        Response restResponse = hProjectRestService.updateHProjectOwner(hproject.getId(), user.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test81_updateHProjectOwnerShouldFailIfHProjectNotExist(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to update HProject's owner
        // but HProject not exist
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HUser user = createHUser(null);
        Assert.assertNotEquals(0, user.getId());
        impersonateUser(hProjectRestService, adminUser);
        Response restResponse = hProjectRestService.updateHProjectOwner(0, user.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test82_updateHProjectOwnerShouldFailIfHUserNotExist(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // the following call tries to update HProject's owner
        // but HUser not exist
        // response status code '404' HyperIoTNoResultException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        impersonateUser(hProjectRestService, adminUser);
        Response restResponse = hProjectRestService.updateHProjectOwner(hproject.getId(), 0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTNoResultException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
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
            this.impersonateUser(hPacketRestApi, adminUser);
            Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
            Assert.assertEquals(200, responseAddField1.getStatus());
            Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
            Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
            Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());

            // add field2
            this.impersonateUser(hPacketRestApi, adminUser);
            Response responseAddField2 = hPacketRestApi.addHPacketField(hpacket.getId(), field2);
            Assert.assertEquals(200, responseAddField2.getStatus());
            Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());
            Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getId());
            Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getUser().getId());

            //check restResponse field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            List<HPacketField> fields = new ArrayList<>();
            fields.addAll(((HPacket) restResponse.getEntity()).getFields());
            Assert.assertEquals(fields.get(0).getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

            //check restResponse field2 is equals to responseAddField2 field2
            Assert.assertEquals(field2.getId(), ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertEquals(fields.get(1).getId(), ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());

            Assert.assertEquals(2, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        return hpacket;
    }

    private Area createArea(HProject hproject) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals("Description", ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        return area;
    }

    private AreaDevice createAreaDevice(Area area, HDevice hdevice) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        return areaDevice;
    }

    private HProject createHProjectAndDashboards(){
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        HProject hprojectDashboard = new HProject();
        hprojectDashboard.setName("Project " + java.util.UUID.randomUUID());
        hprojectDashboard.setDescription("Description");
        hprojectDashboard.setUser((HUser) adminUser);
        Response restResponse = hprojectRestService.saveHProject(hprojectDashboard);
        Assert.assertEquals(200, restResponse.getStatus());

        return hprojectDashboard;
    }

    private SharedEntity createSharedEntity(HyperIoTBaseEntity hyperIoTBaseEntity, HUser ownerUser, HUser huser) {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);

        SharedEntity sharedEntity = new SharedEntity();
        sharedEntity.setEntityId(hyperIoTBaseEntity.getId());
        sharedEntity.setEntityResourceName(hyperIoTBaseEntity.getResourceName()); // "it.acsoftware.hyperiot.shared.entity.example.HyperIoTSharedEntityExample"
        sharedEntity.setUserId(huser.getId());

        if (ownerUser == null) {
            AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
            ownerUser = (HUser) authService.login("hadmin", "admin");
            Assert.assertTrue(ownerUser.isAdmin());
        }
        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hyperIoTBaseEntity.getId(), ((SharedEntity) restResponse.getEntity()).getEntityId());
        Assert.assertEquals(huser.getId(), ((SharedEntity) restResponse.getEntity()).getUserId());
        Assert.assertEquals(hyperIoTBaseEntity.getResourceName(), ((SharedEntity) restResponse.getEntity()).getEntityResourceName());
        return sharedEntity;
    }


    private String testMaxDescription(int lengthDescription) {
        String symbol = "a";
        String description = String.format("%" + lengthDescription + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(lengthDescription, description.length());
        return description;
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

}

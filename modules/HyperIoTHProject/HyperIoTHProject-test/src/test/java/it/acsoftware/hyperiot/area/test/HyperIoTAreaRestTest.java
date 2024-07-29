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
package it.acsoftware.hyperiot.area.test;

import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.model.AreaViewType;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static it.acsoftware.hyperiot.area.test.HyperIoTAreaConfiguration.*;


/**
 * @author Vincenzo Longo (vincenzo.longo@acsoftware.it)
 * @version 2019-04-29 Initial release
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAreaRestTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }


    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
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
    public void test001_areaModuleShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call checkModuleWorking checks if Area module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Area Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_saveAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin save Area with the following call saveArea
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Area of user: " + adminUser.getUsername());
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponse.getEntity()).getName());
        Assert.assertEquals("Area of user: " + hproject.getUser().getUsername(), ((Area) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponse.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test003_saveAreaShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to save Area, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Area area = new Area();
        area.setName("Area" + UUID.randomUUID().toString().replaceAll("-", ""));
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test004_updateAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin update Area with the following call updateArea
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        Date date = new Date();
        area.setDescription("Description updated in date: " + date);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponse.getEntity()).getEntityVersion());
        Assert.assertNotEquals(0, ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(adminUser.getUsername(),
                ((Area) restResponse.getEntity()).getProject().getUser().getUsername());
        Assert.assertEquals("Description updated in date: " + date,
                ((Area) restResponse.getEntity()).getDescription());
    }


    @Test
    public void test005_updateAreaShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to update Area, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        area.setDescription("Description");
        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test006_findAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find Area with the following call findArea
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(area.getId(), ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((Area) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponse.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test007_findAreaShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to find Area, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.findArea(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test008_findAreaShouldFailIfEntityNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to find Area with the following call findArea,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findArea(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test009_findAllAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find all Area with the following call findAllArea
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllArea();
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertFalse(listAreas.isEmpty());
        Assert.assertEquals(1, listAreas.size());
        boolean areaFound = false;
        for (Area a : listAreas) {
            if (area.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                areaFound = true;
            }
        }
        Assert.assertTrue(areaFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test010_findAllAreaShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to find all Area, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.findAllArea();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test011_deleteAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin delete Area with the following call deleteArea
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.deleteArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test012_deleteAreaShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to delete Area, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.deleteArea(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test013_deleteAreaShouldFailIfEntityNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to delete Area with the following call deleteArea,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.deleteArea(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test014_saveAreaShouldFailIfNameIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName(null);
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test015_saveAreaShouldFailIfNameIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("");
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test016_saveAreaShouldFailIfNameIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("</script>");
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test017_saveAreaShouldFailIfDescriptionIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID().toString().replaceAll("-", ""));
        area.setDescription("javascript:");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test018_saveAreaShouldFailIfMaxDescriptionIsOver3000Chars() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID().toString().replaceAll("-", ""));
        int lengthDescription = 3001;
        area.setDescription(testMaxDescription(lengthDescription));
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(lengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }


    @Test
    public void test019_saveAreaShouldFailIfHProjectIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID().toString().replaceAll("-", ""));
        area.setDescription("Description");
        area.setProject(null);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-project", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test020_updateAreaShouldFailIfNameIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setName(null);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test021_updateAreaShouldFailIfNameIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setName("");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test022_updateAreaShouldFailIfNameIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setName("<script malicious code>");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test023_updateAreaShouldFailIfDescriptionIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setDescription("onload(malicious code)=");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test024_updateAreaShouldFailIfMaxDescriptionIsOver3000Chars() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        int lengthDescription = 3001;
        area.setDescription(testMaxDescription(lengthDescription));
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(lengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }


    @Test
    public void test025_updateAreaShouldFailIfHProjectIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setProject(null);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-project", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test026_addAreaDeviceShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin add an AreaDevice with the following call addAreaDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test027_addAreaDeviceShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to add an AreaDevice,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test028_addAreaDeviceShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to add an AreaDevice with the following call addAreaDevice,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.addAreaDevice(0, areaDevice);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test029_addAreaDeviceShouldFailIfHDeviceNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to add an AreaDevice with the following call addAreaDevice,
        // but HDevice not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test030_addAreaDeviceShouldFailIfEntityIsDuplicated() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to add an AreaDevice with the following call addAreaDevice,
        // but Entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseDuplicate = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(409, restResponseDuplicate.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponseDuplicate.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseDuplicate.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseDuplicate.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test031_removeAreaDeviceShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin remove an AreaDevice with the following call removeAreaDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test032_removeAreaDeviceShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to remove an AreaDevice,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), areaDevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test033_removeAreaDeviceShouldFailIfAreaDeviceNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to remove an AreaDevice with the following call removeAreaDevice,
        // but AreaDevice not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), 0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test034_removeAreaDeviceShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to remove an AreaDevice with the following call removeAreaDevice,
        // but area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.removeAreaDevice(0, areaDevice.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test035_removeAreaDeviceShouldFailIfArea2NotFoundInAreaDevice() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to remove an AreaDevice with the following call removeAreaDevice,
        // but area2 not found in AreaDevice
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area2.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), area2.getProject().getId());
        Assert.assertNotEquals(area.getId(), area2.getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.removeAreaDevice(area2.getId(), areaDevice.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test036_getAreaDevicesListShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find AreaDevice list with the following call getAreaDeviceList
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaDeviceList(area.getId());
        List<AreaDevice> listAreaDevices = restResponse.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertFalse(listAreaDevices.isEmpty());
        Assert.assertEquals(1, listAreaDevices.size());
        boolean areaDeviceFound = false;
        boolean hasArea = false;
        boolean hasDevice = false;
        for (AreaDevice ad : listAreaDevices) {
            if (areaDevice.getId() == ad.getId()) {
                areaDeviceFound = true;
                if (hdevice.getId() == ad.getDevice().getId()) {
                    Assert.assertEquals(area.getId(), ad.getArea().getId());
                    Assert.assertEquals(adminUser.getId(), ad.getArea().getProject().getUser().getId());
                    hasDevice = true;
                }
                if (area.getId() == ad.getArea().getId()) {
                    Assert.assertEquals(hdevice.getId(), ad.getDevice().getId());
                    Assert.assertEquals(adminUser.getId(), ad.getDevice().getProject().getUser().getId());
                    hasArea = true;
                }
            }
        }
        Assert.assertTrue(areaDeviceFound);
        Assert.assertTrue(hasDevice);
        Assert.assertTrue(hasArea);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test037_getAreaDevicesListShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to find AreaDevice list,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.getAreaDeviceList(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test038_getAreaDevicesListShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to find AreaDevice list with the following call getAreaDeviceList,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaDeviceList(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test039_getAreaDevicesListShouldWorkIfListIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find AreaDevice list with the following call getAreaDeviceList
        // if listAreaDevices is empty. There are no HDevice in Area.
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaDeviceList(area.getId());
        List<AreaDevice> listAreaDevices = restResponse.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertTrue(listAreaDevices.isEmpty());
        Assert.assertEquals(0, listAreaDevices.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test040_saveAreaShouldFailIfEntityIsDuplicated() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        Area parentArea = createArea(hproject);
        Assert.assertNotEquals(0, parentArea.getId());
        Assert.assertEquals(hproject.getId(), parentArea.getProject().getId());
        Assert.assertEquals(adminUser.getId(), parentArea.getProject().getUser().getId());

        Set<Area> parentAreas = new HashSet<>();
        parentAreas.add(parentArea);
        area.setParentArea(parentArea);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseUpdate = areaRestApi.update(area);
        Assert.assertEquals(200, restResponseUpdate.getStatus());
        Assert.assertNotNull(((Area) restResponseUpdate.getEntity()).getParentArea());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseUpdate.getEntity()).getEntityVersion());
        Assert.assertEquals(area.getId(), ((Area) restResponseUpdate.getEntity()).getId());
        Assert.assertEquals(area.getParentArea().getId(),
                ((Area) restResponseUpdate.getEntity()).getParentArea().getId());
        Assert.assertEquals(area.getProject().getId(),
                ((Area) restResponseUpdate.getEntity()).getProject().getId());
        Assert.assertEquals(area.getParentArea().getProject().getId(),
                ((Area) restResponseUpdate.getEntity()).getParentArea().getProject().getId());
        Assert.assertEquals(adminUser.getId(),
                ((Area) restResponseUpdate.getEntity()).getProject().getUser().getId());

        Area duplicateArea = new Area();
        duplicateArea.setName(area.getName());
        duplicateArea.setParentArea(parentArea);
        duplicateArea.setProject(area.getProject());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(duplicateArea);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean areaNameIsDuplicated = false;
        boolean hprojectIdIsDuplicated = false;
        boolean parentAreaIdIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                areaNameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("project_id")) {
                Assert.assertEquals("project_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hprojectIdIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("parentArea_id")) {
                Assert.assertEquals("parentArea_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                parentAreaIdIsDuplicated = true;
            }
        }
        Assert.assertTrue(areaNameIsDuplicated);
        Assert.assertTrue(hprojectIdIsDuplicated);
        Assert.assertTrue(parentAreaIdIsDuplicated);
    }


    @Test
    public void test041_updateAreaShouldFailIfEntityIsDuplicated() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        Area parentArea = createArea(hproject);
        Assert.assertNotEquals(0, parentArea.getId());
        Assert.assertEquals(hproject.getId(), parentArea.getProject().getId());
        Assert.assertEquals(adminUser.getId(), parentArea.getProject().getUser().getId());

        Set<Area> parentAreas = new HashSet<>();
        parentAreas.add(parentArea);
        area.setParentArea(parentArea);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseUpdate = areaRestApi.update(area);
        Assert.assertEquals(200, restResponseUpdate.getStatus());
        Assert.assertNotNull(((Area) restResponseUpdate.getEntity()).getParentArea());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseUpdate.getEntity()).getEntityVersion());
        Assert.assertEquals(area.getId(), ((Area) restResponseUpdate.getEntity()).getId());
        Assert.assertEquals(area.getParentArea().getId(),
                ((Area) restResponseUpdate.getEntity()).getParentArea().getId());
        Assert.assertEquals(area.getProject().getId(),
                ((Area) restResponseUpdate.getEntity()).getProject().getId());
        Assert.assertEquals(area.getParentArea().getProject().getId(),
                ((Area) restResponseUpdate.getEntity()).getParentArea().getProject().getId());
        Assert.assertEquals(adminUser.getId(),
                ((Area) restResponseUpdate.getEntity()).getProject().getUser().getId());

        Area duplicateArea = createArea(hproject);
        Assert.assertNotEquals(0, duplicateArea.getId());
        Assert.assertEquals(hproject.getId(), duplicateArea.getProject().getId());
        Assert.assertEquals(adminUser.getId(), duplicateArea.getProject().getUser().getId());

        Assert.assertNotEquals(area.getId(), duplicateArea.getId());
        Assert.assertNotEquals(parentArea.getId(), duplicateArea.getId());

        duplicateArea.setName(area.getName());
        duplicateArea.setParentArea(area.getParentArea());
        duplicateArea.setProject(area.getProject());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(duplicateArea);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean areaNameIsDuplicated = false;
        boolean hprojectIdIsDuplicated = false;
        boolean parentAreaIdIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                areaNameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("project_id")) {
                Assert.assertEquals("project_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hprojectIdIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("parentArea_id")) {
                Assert.assertEquals("parentArea_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                parentAreaIdIsDuplicated = true;
            }
        }
        Assert.assertTrue(areaNameIsDuplicated);
        Assert.assertTrue(hprojectIdIsDuplicated);
        Assert.assertTrue(parentAreaIdIsDuplicated);
    }


    @Test
    public void test042_updateAreaShouldFailIfEntityNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        // entity isn't stored in database
        Area area = new Area();
        area.setDescription("entity not found");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test043_getAreaDeviceShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find AreaDevice with the following call getAreaDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(areaDevice.getId(), ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getId());

        Assert.assertEquals(hproject.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getId());
        Assert.assertEquals(hproject.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getId());

        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test044_getAreaDeviceShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to find AreaDevice, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test045_getAreaDeviceShouldFailIfAreaDeviceNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to find AreaDevice with the following call getAreaDevice,
        // but AreaDevice not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test046_getAreaDeviceShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to find AreaDevice with the following call getAreaDevice,
        // but area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        areaRestApi.deleteArea(area.getId());
        Response restResponse = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test047_getAreaDeviceShouldFailIfArea2NotFoundInAreaDevice() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to find AreaDevice with the following call getAreaDevice,
        // but area2 not found in AreaDevice
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area2.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), area2.getProject().getId());
        Assert.assertNotEquals(area.getId(), area2.getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(areaDevice.getArea().getId(), area2.getId());
    }


    @Test
    public void test048_getAreaPathShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find AreaPath list with the following call getAreaPath
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaPath(area.getId());
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertFalse(listAreas.isEmpty());
        Assert.assertEquals(1, listAreas.size());
        boolean areaFound = false;
        for (Area a : listAreas) {
            if (a.getId() == area.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                areaFound = true;
            }
        }
        Assert.assertTrue(areaFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test049_getAreaPathShouldWorkMoreAreas() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find AreaPath list with the following call getAreaPath
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        Area parentArea1 = createArea(hproject);
        Assert.assertNotEquals(0, parentArea1.getId());
        Assert.assertEquals(hproject.getId(), parentArea1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), parentArea1.getProject().getUser().getId());

        Set<Area> parentAreas = new HashSet<>();
        parentAreas.add(parentArea1);

        area.setParentArea(parentArea1);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseUpdateArea = areaRestApi.updateArea(area);
        Assert.assertEquals(200, restResponseUpdateArea.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseUpdateArea.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaPath(area.getId());
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertFalse(listAreas.isEmpty());
        Assert.assertEquals(2, listAreas.size());
        boolean areaFound = false;
        boolean areaHasParentFound = false;
        boolean parentArea1Found = false;
        for (Area a : listAreas) {
            if ((a.getId() == area.getId())) {
                Assert.assertEquals(a.getId(), area.getId());
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                areaFound = true;
                Assert.assertNotNull(a.getParentArea());
                Assert.assertEquals(a.getParentArea().getId(), parentArea1.getId());
                areaHasParentFound = true;
            }
            if ((a.getId() == parentArea1.getId())) {
                Assert.assertEquals(a.getId(), parentArea1.getId());
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                parentArea1Found = true;
                Assert.assertNull(a.getParentArea());
            }
        }
        Assert.assertTrue(areaFound);
        Assert.assertTrue(areaHasParentFound);
        Assert.assertTrue(parentArea1Found);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test050_getAreaPathShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to find AreaPath list,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.getAreaPath(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test051_getAreaPathShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find AreaPath list with the following call getAreaPath
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaPath(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test052_findInnerAreasShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find inner areas with the following call findInnerAreas
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        Area parentArea = createArea(hproject);
        Assert.assertNotEquals(0, parentArea.getId());
        Assert.assertEquals(hproject.getId(), parentArea.getProject().getId());
        Assert.assertEquals(adminUser.getId(), parentArea.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        parentArea.setParentArea(area);
        Response restResponseUpdateArea = areaRestApi.updateArea(parentArea);
        Assert.assertEquals(200, restResponseUpdateArea.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseUpdateArea.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findInnerAreas(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonAreaTree = (String) restResponse.getEntity();
        System.out.println("--------------------------\n" + jsonAreaTree);
        Assert.assertTrue(
                jsonAreaTree.contains(
                        "{\"id\":" + area.getId() + "," +
                                "\"entityVersion\":" + area.getEntityVersion() + "," +
                                "\"entityCreateDate\":" + area.getEntityCreateDate().getTime() + "," +
                                "\"entityModifyDate\":" + area.getEntityModifyDate().getTime() + "," +
                                "\"name\":\"" + area.getName() + "\"," +
                                "\"description\":\"" + area.getDescription() + "\"," +
                                "\"areaConfiguration\":null," +
                                "\"areaViewType\":\"IMAGE\"," +
                                "\"mapInfo\":null," +
                                "\"innerArea\":[" +
                                "{\"id\":" + parentArea.getId() + "," +
                                "\"entityVersion\":" + ((Area) restResponseUpdateArea.getEntity()).getEntityVersion() + "," +
                                "\"entityCreateDate\":" + parentArea.getEntityCreateDate().getTime() + "," +
                                "\"entityModifyDate\":" + ((Area) restResponseUpdateArea.getEntity()).getEntityModifyDate().getTime() + "," +
                                "\"name\":\"" + parentArea.getName() + "\"," +
                                "\"description\":\"" + parentArea.getDescription() + "\"," +
                                "\"areaConfiguration\":null," +
                                "\"areaViewType\":\"IMAGE\"," +
                                "\"mapInfo\":null," +
                                "\"innerArea\":[]}" +
                                "]}"
                )
        );
    }


    @Test
    public void test053_findInnerAreasShouldFailIfParentAreaIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find inner areas with the following call findInnerAreas,
        // inner areas is empty
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getParentArea());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findInnerAreas(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonAreaTree = (String) restResponse.getEntity();

        Assert.assertTrue(
                jsonAreaTree.contains(
                        "{\"id\":" + area.getId() + "," +
                                "\"entityVersion\":" + area.getEntityVersion() + "," +
                                "\"entityCreateDate\":" + area.getEntityCreateDate().getTime() + "," +
                                "\"entityModifyDate\":" + area.getEntityModifyDate().getTime() + "," +
                                "\"name\":\"" + area.getName() + "\"," +
                                "\"description\":\"" + area.getDescription() + "\"," +
                                "\"areaConfiguration\":null," +
                                "\"areaViewType\":\"IMAGE\"," +
                                "\"mapInfo\":null," +
                                "\"innerArea\":[]}"
                )
        );

    }


    @Test
    public void test054_findInnerAreasShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to find inner areas with the following call findInnerAreas,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        Area parentArea = createArea(hproject);
        Assert.assertNotEquals(0, parentArea.getId());
        Assert.assertEquals(hproject.getId(), parentArea.getProject().getId());
        Assert.assertEquals(adminUser.getId(), parentArea.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        parentArea.setParentArea(area);
        Response restResponseUpdateArea = areaRestApi.updateArea(parentArea);
        Assert.assertEquals(200, restResponseUpdateArea.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseUpdateArea.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, null); // admin not logged
        Response restResponse = areaRestApi.findInnerAreas(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test055_findInnerAreasShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to find inner areas with the following call findInnerAreas,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findInnerAreas(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test056_setAreaImageShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin set an Area image with the following call setAreaImage
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNotNull(((Area) restResponseImage.getEntity()).getImagePath());

        String newImageName = String.valueOf(area.getId()).concat("_img.").concat(fileExtension);
        Assert.assertTrue(((Area) restResponseImage.getEntity()).getImagePath().contains(newImageName));
    }


    @Test
    public void test057_setAreaImageShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to set an Area image with the following call setAreaImage,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.setAreaImage(0, jpgAttachment);
        Assert.assertEquals(404, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test058_setAreaImageShouldFailIfImageIsMissing() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to set an Area image with the following call setAreaImage,
        // but image file is missing
        // response status code '422' java.io.IOException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), null);
        Assert.assertEquals(422, restResponseImage.getStatus());
        Assert.assertEquals("java.io.IOException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
        Assert.assertEquals(1,
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().size());
        Assert.assertEquals("Missing image file",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().get(0));
    }


    @Test
    public void test059_setAreaImageShouldFailIfContentDispositionHeaderIsNotSupported() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin set an Area image with the following call setAreaImage,
        // but content disposition header is not supported
        // response status code '500' java.io.IOException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment not valid";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(422, restResponseImage.getStatus());
        Assert.assertEquals("java.io.IOException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
        Assert.assertEquals(1,
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().size());
        Assert.assertEquals("File type not supported.",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().get(0));
    }


    @Test
    public void test060_setAreaImageShouldFailIfFileExtensionIsNotSupported() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin set an Area image with the following call setAreaImage,
        // but file extension is not supported
        // response status code '500' java.io.IOException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"image.docx\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        Attachment jpgAttachment = new Attachment("", imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(422, restResponseImage.getStatus());
        Assert.assertEquals("java.io.IOException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
        Assert.assertEquals(1,
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().size());
        Assert.assertEquals("File type not supported.",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().get(0));
    }


    @Test
    public void test061_setAreaImageShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to set an Area image with the following call setAreaImage,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, null);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(403, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test062_getAreaImageShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin get an Area image with the following call getAreaImage
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.getAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(new File(area.getImagePath()), restResponseImage.getEntity());
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Type")
                .contains("application/octet-stream"));
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Disposition")
                .contains("attachment; filename=\"" + area.getId() + "_img.jpg\""));
    }


    @Test
    public void test063_getAreaImageShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to get an Area image with the following call getAreaImage,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.getAreaImage(0);
        Assert.assertEquals(404, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test064_getAreaImageShouldFailIfImagePathIsNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to get an Area image with the following call getAreaImage,
        // but image path is not found
        // response status code '500' java.io.IOException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        area.setImagePath("../../../src/main/resources/folderNotFound/");
        this.impersonateUser(areaRestApi, adminUser);
        int previousAreaVersion = area.getEntityVersion();
        area = areaSystemApi.update(area, null);
        Assert.assertEquals(previousAreaVersion + 1, area.getEntityVersion());
        Assert.assertNotEquals(0, area.getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.getAreaImage(area.getId());
        Assert.assertEquals(500, restResponseImage.getStatus());
        Assert.assertEquals("java.io.IOException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
        Assert.assertEquals(1,
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().size());
        Assert.assertEquals("Image file not found",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getErrorMessages().get(0));
    }


    @Test
    public void test065_getAreaImageShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to get an Area image with the following call getAreaImage,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, null);
        Response restResponseImage = areaRestApi.getAreaImage(area.getId());
        Assert.assertEquals(403, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test066_unsetAreaImageShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin unset an Area image with the following call unsetAreaImage
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNull(((Area) restResponseImage.getEntity()).getImagePath());
    }


    @Test
    public void test067_unsetAreaImageShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to unset an Area image with the following call unsetAreaImage,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.unsetAreaImage(0);
        Assert.assertEquals(404, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test068_unsetAreaImageShouldWorkIfImagePathIsNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to unset an Area image with the following call unsetAreaImage,
        // but image path is not found
        // response status code '500' java.io.IOException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        area.setImagePath("../../../src/main/resources/folderNotFound/");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseUpdateArea = areaRestApi.updateArea(area);
        Assert.assertEquals(200, restResponseUpdateArea.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseUpdateArea.getEntity()).getEntityVersion());
        Assert.assertNotEquals(0, ((Area) restResponseUpdateArea.getEntity()).getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(((Area) restResponseUpdateArea.getEntity()).getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNull(((Area) restResponseImage.getEntity()).getImagePath());
    }


    @Test
    public void test069_unsetAreaImageShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to unset an Area image with the following call unsetAreaImage,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, null);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(403, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test070_findAllAreaPaginatedShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, hadmin find all areas with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 7;
        int page = 2;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(defaultDelta, areas.size());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listAreas.getResults().size());
        Assert.assertEquals(delta, listAreas.getDelta());
        Assert.assertEquals(page, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // delta is 7, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponsePage1 = areaRestApi.findAllAreaPaginated(delta, 1);
        HyperIoTPaginableResult<Area> listAreasPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listAreasPage1.getResults().size());
        Assert.assertEquals(delta, listAreasPage1.getDelta());
        Assert.assertEquals(defaultPage, listAreasPage1.getCurrentPage());
        Assert.assertEquals(page, listAreasPage1.getNextPage());
        // delta is 7, page is 1: 10 entities stored in database
        Assert.assertEquals(2, listAreasPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test071_findAllAreaPaginatedShouldFailIfNotLogged() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // the following call tries to find all Areas with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(areaRestApi, null);
        Response restResponse = areaRestApi.findAllAreaPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test072_findAllAreaPaginatedShouldWorkIfDeltaAndPageAreNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, hadmin find all Areas with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(defaultDelta, areas.size());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAreas.getResults().size());
        Assert.assertEquals(defaultDelta, listAreas.getDelta());
        Assert.assertEquals(defaultPage, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test073_findAllAreaPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, hadmin find all Areas with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 2;

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 17;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listAreas.getResults().size());
        Assert.assertEquals(defaultDelta, listAreas.getDelta());
        Assert.assertEquals(page, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // default delta is 10, page is 2: 17 entities stored in database
        Assert.assertEquals(2, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponsePage1 = areaRestApi.findAllAreaPaginated(delta, 1);
        HyperIoTPaginableResult<Area> listAreasPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAreasPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listAreasPage1.getDelta());
        Assert.assertEquals(defaultPage, listAreasPage1.getCurrentPage());
        Assert.assertEquals(page, listAreasPage1.getNextPage());
        // default delta is 10, page is 1: 17 entities stored in database
        Assert.assertEquals(2, listAreasPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test074_findAllAreaPaginatedShouldWorkIfDeltaIsZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, hadmin find all Areas with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 3;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 22;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(numbEntities - (defaultDelta * 2), listAreas.getResults().size());
        Assert.assertEquals(defaultDelta, listAreas.getDelta());
        Assert.assertEquals(page, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // default delta is 10, page is 3: 22 entities stored in database
        Assert.assertEquals(3, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponsePage1 = areaRestApi.findAllAreaPaginated(delta, 1);
        HyperIoTPaginableResult<Area> listAreasPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAreasPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listAreasPage1.getDelta());
        Assert.assertEquals(defaultPage, listAreasPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listAreasPage1.getNextPage());
        // default delta is 10, page is 1: 22 entities stored in database
        Assert.assertEquals(3, listAreasPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponsePage2 = areaRestApi.findAllAreaPaginated(delta, 2);
        HyperIoTPaginableResult<Area> listAreasPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAreasPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listAreasPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listAreasPage2.getCurrentPage());
        Assert.assertEquals(page, listAreasPage2.getNextPage());
        // default delta is 10, page is 2: 22 entities stored in database
        Assert.assertEquals(3, listAreasPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test075_findAllAreaPaginatedShouldWorkIfPageIsLowerThanZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, hadmin find all Areas with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 9;
        int page = -1;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 6;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listAreas.getResults().size());
        Assert.assertEquals(delta, listAreas.getDelta());
        Assert.assertEquals(defaultPage, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // delta is 9, default page is 1: 6 entities stored in database
        Assert.assertEquals(1, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test076_findAllAreaPaginatedShouldWorkIfPageIsZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, hadmin find all Areas with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 6;
        int page = 0;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 8;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(delta, listAreas.getResults().size());
        Assert.assertEquals(delta, listAreas.getDelta());
        Assert.assertEquals(defaultPage, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listAreas.getNextPage());
        // delta is 6, default page is 1: 8 entities stored in database
        Assert.assertEquals(2, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponsePage2 = areaRestApi.findAllAreaPaginated(delta, 2);
        HyperIoTPaginableResult<Area> listAreasPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listAreasPage2.getResults().size());
        Assert.assertEquals(delta, listAreasPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listAreasPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreasPage2.getNextPage());
        // delta is 6, default page is 2: 8 entities stored in database
        Assert.assertEquals(2, listAreasPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test077_deleteAreaWithHProjectNotDeleteInCascadeHProject() {
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
        Assert.assertFalse(listHProjectAreas.isEmpty());
        Assert.assertEquals(2, listHProjectAreas.size());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area a : listHProjectAreas) {
            if (area1.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
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
        Response restResponseDeleteArea1 = areaRestApi.deleteArea(area1.getId());
        Assert.assertEquals(200, restResponseDeleteArea1.getStatus());
        Assert.assertNull(restResponseDeleteArea1.getEntity());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseDeleteArea2 = areaRestApi.deleteArea(area2.getId());
        Assert.assertEquals(200, restResponseDeleteArea2.getStatus());
        Assert.assertNull(restResponseDeleteArea2.getEntity());

        // checks if hproject is still stored in database
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseFindHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseFindHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseFindHProject.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponseFindHProject.getEntity()).getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseHProjectAreaList = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreasEmpty = restResponseHProjectAreaList.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertTrue(listHProjectAreasEmpty.isEmpty());
        Assert.assertEquals(0, listHProjectAreasEmpty.size());
        Assert.assertEquals(200, restResponseHProjectAreaList.getStatus());
    }


    @Test
    public void test078_deleteHProjectWithAreasDeleteInCascadeAllAreas() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes hproject, with areas associated, with the following call deleteHProject,
        // all areas is deleted in cascade mode
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
        Assert.assertFalse(listHProjectAreas.isEmpty());
        Assert.assertEquals(2, listHProjectAreas.size());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area a : listHProjectAreas) {
            if (area1.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(adminUser.getId(), a.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // delete hproject and all areas associated
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
        Assert.assertNull(restResponseDeleteHProject.getEntity());

        // checks: all areas not found;
        // area has been deleted in cascade mode with deleteHProject call
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseFindArea1 = areaRestApi.findArea(area1.getId());
        Assert.assertEquals(404, restResponseFindArea1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea1.getEntity()).getType());
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseFindArea2 = areaRestApi.findArea(area2.getId());
        Assert.assertEquals(404, restResponseFindArea2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea2.getEntity()).getType());
    }


    @Test
    public void test079_removeHProjectDeleteInCascadeModeAreaAndAreaDevice() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin deletes hproject, with area and areaDevice associated, with the
        // following call deleteHProject; this call deletes in cascade mode all
        // area and areaDevice associated
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.getAreaDeviceList(area.getId());
        List<AreaDevice> listAreaDevices = restResponse.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertFalse(listAreaDevices.isEmpty());
        Assert.assertEquals(1, listAreaDevices.size());
        boolean areaDeviceFound = false;
        boolean hasArea = false;
        boolean hasDevice = false;
        for (AreaDevice ad : listAreaDevices) {
            if (areaDevice.getId() == ad.getId()) {
                areaDeviceFound = true;
                if (hdevice.getId() == ad.getDevice().getId()) {
                    Assert.assertEquals(area.getId(), ad.getArea().getId());
                    Assert.assertEquals(adminUser.getId(), ad.getArea().getProject().getUser().getId());
                    hasDevice = true;
                }
                if (area.getId() == ad.getArea().getId()) {
                    Assert.assertEquals(hdevice.getId(), ad.getDevice().getId());
                    Assert.assertEquals(adminUser.getId(), ad.getDevice().getProject().getUser().getId());
                    hasArea = true;
                }
            }
        }
        Assert.assertTrue(areaDeviceFound);
        Assert.assertTrue(hasDevice);
        Assert.assertTrue(hasArea);
        Assert.assertEquals(200, restResponse.getStatus());

        // delete hproject and all Area and AreaDevice
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
        Assert.assertNull(restResponseDeleteHProject.getEntity());

        // checks: area and areaDevice not found;
        // area and areaDevice has been deleted in cascade mode with deleteHProject call
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseFindArea = areaRestApi.findArea(area.getId());
        Assert.assertEquals(404, restResponseFindArea.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea.getEntity()).getType());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseFindAreaDevice = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(404, restResponseFindAreaDevice.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindAreaDevice.getEntity()).getType());
    }


    @Test
    public void test080_findInnerMoreAreasShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin finds inner areas with the following call findInnerAreas
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area1.getProject().getUser().getId());

        Area parentArea1 = createArea(hproject);
        Assert.assertNotEquals(0, parentArea1.getId());
        Assert.assertEquals(hproject.getId(), parentArea1.getProject().getId());
        Assert.assertEquals(adminUser.getId(), parentArea1.getProject().getUser().getId());

        Area parentArea2 = createArea(hproject);
        Assert.assertNotEquals(0, parentArea2.getId());
        Assert.assertEquals(hproject.getId(), parentArea2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), parentArea2.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        parentArea1.setParentArea(area1);
        Response restResponseUpdateParentArea1 = areaRestApi.updateArea(parentArea1);
        Assert.assertEquals(200, restResponseUpdateParentArea1.getStatus());
        Assert.assertEquals(parentArea1.getId(), ((Area) restResponseUpdateParentArea1.getEntity()).getId());
        Assert.assertEquals(parentArea1.getEntityVersion() + 1,
                ((Area) restResponseUpdateParentArea1.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, adminUser);
        parentArea2.setParentArea(area1);
        Response restResponseUpdateParentArea2 = areaRestApi.updateArea(parentArea2);
        Assert.assertEquals(200, restResponseUpdateParentArea2.getStatus());
        Assert.assertEquals(parentArea2.getId(), ((Area) restResponseUpdateParentArea2.getEntity()).getId());
        Assert.assertEquals(parentArea2.getEntityVersion() + 1,
                ((Area) restResponseUpdateParentArea2.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findInnerAreas(area1.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonAreaTree = (String) restResponse.getEntity();

        boolean containsArea1 = false;
        boolean containsParentAreas = false;

        //jsonAreaTree contains area1 data
        if (jsonAreaTree.contains("{\"id\":" + area1.getId() + ",")) {
            Assert.assertTrue(
                    jsonAreaTree.contains(
                            "{\"id\":" + area1.getId() + "," +
                                    "\"entityVersion\":" + area1.getEntityVersion() + "," +
                                    "\"entityCreateDate\":" + area1.getEntityCreateDate().getTime() + "," +
                                    "\"entityModifyDate\":" + area1.getEntityModifyDate().getTime() + "," +
                                    "\"name\":\"" + area1.getName() + "\"," +
                                    "\"description\":\"" + area1.getDescription() + "\"," +
                                    "\"areaConfiguration\":null," +
                                    "\"areaViewType\":\"IMAGE\"," +
                                    "\"mapInfo\":null," +
                                    "\"innerArea\":["
                    )
            );
            containsArea1 = true;
        }
        //jsonAreaTree contains parentArea1 and parentArea2 data
        if (jsonAreaTree.contains("\"innerArea\":[{\"id\":" + parentArea1.getId() + ",")) {
            Assert.assertTrue(
                    jsonAreaTree.contains(
                            "{\"id\":" + parentArea1.getId() + "," +
                                    "\"entityVersion\":" + (parentArea1.getEntityVersion() + 1) + "," +
                                    "\"entityCreateDate\":" + parentArea1.getEntityCreateDate().getTime() + "," +
                                    "\"entityModifyDate\":" + ((Area) restResponseUpdateParentArea1.getEntity()).getEntityModifyDate().getTime() + "," +
                                    "\"name\":\"" + parentArea1.getName() + "\"," +
                                    "\"description\":\"" + parentArea1.getDescription() + "\"," +
                                    "\"areaConfiguration\":null," +
                                    "\"areaViewType\":\"IMAGE\"," +
                                    "\"mapInfo\":null," +
                                    "\"innerArea\":[]}," +
                                    "{\"id\":" + parentArea2.getId() + "," +
                                    "\"entityVersion\":" + (parentArea2.getEntityVersion() + 1) + "," +
                                    "\"entityCreateDate\":" + parentArea2.getEntityCreateDate().getTime() + "," +
                                    "\"entityModifyDate\":" + ((Area) restResponseUpdateParentArea2.getEntity()).getEntityModifyDate().getTime() + "," +
                                    "\"name\":\"" + parentArea2.getName() + "\"," +
                                    "\"description\":\"" + parentArea2.getDescription() + "\"," +
                                    "\"areaConfiguration\":null," +
                                    "\"areaViewType\":\"IMAGE\"," +
                                    "\"mapInfo\":null," +
                                    "\"innerArea\":[]}" +
                                    "]}"
                    )
            );
            containsParentAreas = true;
        }
        //jsonAreaTree contains parentArea2 and parentArea1 data
        if (jsonAreaTree.contains("\"innerArea\":[{\"id\":" + parentArea2.getId() + ",")) {
            Assert.assertTrue(
                    jsonAreaTree.contains(
                            "{\"id\":" + parentArea2.getId() + "," +
                                    "\"entityVersion\":" + (parentArea2.getEntityVersion() + 1) + "," +
                                    "\"entityCreateDate\":" + parentArea2.getEntityCreateDate().getTime() + "," +
                                    "\"entityModifyDate\":" + ((Area) restResponseUpdateParentArea2.getEntity()).getEntityModifyDate().getTime() + "," +
                                    "\"name\":\"" + parentArea2.getName() + "\"," +
                                    "\"description\":\"" + parentArea2.getDescription() + "\"," +
                                    "\"areaConfiguration\":null," +
                                    "\"areaViewType\":\"IMAGE\"," +
                                    "\"mapInfo\":null," +
                                    "\"innerArea\":[]}," +
                                    "{\"id\":" + parentArea1.getId() + "," +
                                    "\"entityVersion\":" + (parentArea1.getEntityVersion() + 1) + "," +
                                    "\"entityCreateDate\":" + parentArea1.getEntityCreateDate().getTime() + "," +
                                    "\"entityModifyDate\":" + ((Area) restResponseUpdateParentArea1.getEntity()).getEntityModifyDate().getTime() + "," +
                                    "\"name\":\"" + parentArea1.getName() + "\"," +
                                    "\"description\":\"" + parentArea1.getDescription() + "\"," +
                                    "\"areaConfiguration\":null," +
                                    "\"areaViewType\":\"IMAGE\"," +
                                    "\"mapInfo\":null," +
                                    "\"innerArea\":[]}" +
                                    "]}"
                    )
            );
            containsParentAreas = true;
        }
        Assert.assertTrue(containsArea1);
        Assert.assertTrue(containsParentAreas);
    }


    @Test
    public void test081_removeAreaDeleteInCascadeAllAreaDeviceNotDeleteAllDevices() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin remove an Area with call deleteArea and deletes in cascade mode
        // all AreaDevice; this call not deletes all devices
        // response status code '200'
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

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice2.getProject().getUser().getId());

        HDevice hdevice3 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice3.getId());
        Assert.assertEquals(hproject.getId(), hdevice3.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice3.getProject().getUser().getId());

        Assert.assertEquals(area1.getProject().getId(), hdevice1.getProject().getId());
        Assert.assertEquals(area1.getProject().getId(), hdevice2.getProject().getId());
        Assert.assertEquals(area1.getProject().getId(), hdevice3.getProject().getId());

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

        AreaDevice ad3 = createAreaDevice(area1, hdevice3);
        Assert.assertNotEquals(0, ad3.getId());
        Assert.assertEquals(hdevice3.getId(), ad3.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad3.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad3.getArea().getProject().getUser().getId());

        //checks if AreaDevice exists
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseAreaDevice = areaRestApi.getAreaDeviceList(area1.getId());
        List<AreaDevice> listFirstAreaDevices = restResponseAreaDevice.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertEquals(3, listFirstAreaDevices.size());
        Assert.assertFalse(listFirstAreaDevices.isEmpty());
        Assert.assertEquals(200, restResponseAreaDevice.getStatus());

        AreaDevice areaDevice1 = listFirstAreaDevices.get(0);
        Assert.assertNotEquals(0, areaDevice1.getId());
        Assert.assertEquals(hdevice1.getId(), areaDevice1.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        AreaDevice areaDevice2 = listFirstAreaDevices.get(1);
        Assert.assertNotEquals(0, areaDevice2.getId());
        Assert.assertEquals(hdevice2.getId(), areaDevice2.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice2.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice2.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice2.getId(), ad2.getId());
        Assert.assertEquals(areaDevice2.getDevice().getId(), ad2.getDevice().getId());
        Assert.assertEquals(areaDevice2.getArea().getId(), ad2.getArea().getId());

        AreaDevice areaDevice3 = listFirstAreaDevices.get(2);
        Assert.assertNotEquals(0, areaDevice3.getId());
        Assert.assertEquals(hdevice3.getId(), areaDevice3.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice3.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice3.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice3.getId(), ad3.getId());
        Assert.assertEquals(areaDevice3.getDevice().getId(), ad3.getDevice().getId());
        Assert.assertEquals(areaDevice3.getArea().getId(), ad3.getArea().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseDeleteArea1 = areaRestApi.deleteArea(area1.getId());
        Assert.assertEquals(200, restResponseDeleteArea1.getStatus());
        Assert.assertNull(restResponseDeleteArea1.getEntity());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseAreaDeviceNotFound = areaRestApi.getAreaDeviceList(area1.getId());
        Assert.assertEquals(404, restResponseAreaDeviceNotFound.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseAreaDeviceNotFound.getEntity()).getType());

        // ckecks: devices hasn't been deleted in cascade mode
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponseFindDevice1 = hDeviceRestService.findHDevice(hdevice1.getId());
        Assert.assertEquals(200, restResponseFindDevice1.getStatus());
        Assert.assertEquals(hdevice1.getId(), ((HDevice) restResponseFindDevice1.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice1.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HDevice) restResponseFindDevice1.getEntity()).getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponseFindDevice2 = hDeviceRestService.findHDevice(hdevice2.getId());
        Assert.assertEquals(200, restResponseFindDevice2.getStatus());
        Assert.assertEquals(hdevice2.getId(), ((HDevice) restResponseFindDevice2.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice2.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HDevice) restResponseFindDevice2.getEntity()).getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponseFindDevice3 = hDeviceRestService.findHDevice(hdevice3.getId());
        Assert.assertEquals(200, restResponseFindDevice3.getStatus());
        Assert.assertEquals(hdevice3.getId(), ((HDevice) restResponseFindDevice3.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice3.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HDevice) restResponseFindDevice3.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test082_deleteDeviceAssociatedWithAreaDeviceShouldCascade() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to remove device associated with an AreaDevice;
        // this call not deletes device because referential integrity constraint is violated
        // response status code '500' javax.persistence.PersistenceException
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

        AreaDevice ad1 = createAreaDevice(area1, hdevice1);
        Assert.assertNotEquals(0, ad1.getId());
        Assert.assertEquals(hdevice1.getId(), ad1.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ad1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad1.getArea().getId());
        Assert.assertEquals(adminUser.getId(), ad1.getArea().getProject().getUser().getId());

        //checks if AreaDevice exists
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseAreaDevice = areaRestApi.getAreaDeviceList(area1.getId());
        List<AreaDevice> listFirstAreaDevices = restResponseAreaDevice.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertFalse(listFirstAreaDevices.isEmpty());
        Assert.assertEquals(1, listFirstAreaDevices.size());
        Assert.assertEquals(200, restResponseAreaDevice.getStatus());

        AreaDevice areaDevice1 = listFirstAreaDevices.get(0);
        Assert.assertNotEquals(0, areaDevice1.getId());
        Assert.assertEquals(hdevice1.getId(), areaDevice1.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        // ckecks: devices has been deleted in cascade mode
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseDeleteDevice1 = hDeviceRestService.deleteHDevice(hdevice1.getId());
        Assert.assertEquals(200, restResponseDeleteDevice1.getStatus());
    }


    @Test
    public void test083_removeAreaDeviceNotDeleteInCascadeModeAreaOrDevice() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin remove an AreaDevice with the following call removeAreaDevice;
        // this call not removes in cascade mode Area or HDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(adminUser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());

        // checks if Area exists
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseArea = areaRestApi.findArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(area.getId(), ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());

        // checks if HDevice exists
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponseFindDevice = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseFindDevice.getStatus());
        Assert.assertEquals(hdevice.getId(), ((HDevice) restResponseFindDevice.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HDevice) restResponseFindDevice.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test084_findAllAreaShouldWorkIfListIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin find all Area with the following call findAllArea
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllArea();
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertTrue(listAreas.isEmpty());
        Assert.assertEquals(0, listAreas.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test085_findAllAreaPaginatedShouldWorkIfListIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, hadmin find all areas with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.findAllAreaPaginated(defaultDelta, defaultPage);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertTrue(listAreas.getResults().isEmpty());
        Assert.assertEquals(0, listAreas.getResults().size());
        Assert.assertEquals(defaultDelta, listAreas.getDelta());
        Assert.assertEquals(defaultPage, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // default delta is 10, default page is 1: there are not entities stored in database
        Assert.assertEquals(0, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test086_saveAreaShouldFailIfNameIsGreaterThan255Chars() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but name is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName(createStringFieldWithSpecifiedLenght(256));
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test087_saveAreaShouldFailIfImagePathIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but image path is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        area.setImagePath("<script>console.log('hello')</script>");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-imagepath", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getImagePath(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test088_saveAreaShouldFailIfImagePathIsGreaterThan255Char() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // but image path is greater than 255 char
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        area.setImagePath(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-imagepath", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getImagePath(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test089_updateAreaShouldFailIfNameIsGreaterThan255Char() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin update Area with the following call updateArea
        // but name is greater than 255 chars.
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setName(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test090_updateAreaShouldFailIfImagePathIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin update Area with the following call updateArea
        // but image path is malicious code.
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setImagePath("<script>console.log('hello')</script>");
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-imagepath", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getImagePath(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test090_updateAreaShouldFailIfImagePathIsGreaterThan255Char() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin update Area with the following call updateArea
        // but image path  is greater than 255 chars.
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        area.setImagePath(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-imagepath", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(area.getImagePath(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test091_saveAreaShouldWorkIfDescriptionIs2999Chars() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to save Area with the following call saveArea,
        // description length is 2999 chars
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID().toString().replaceAll("-", ""));
        area.setDescription(createStringFieldWithSpecifiedLenght(2999));
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(((Area) restResponse.getEntity()).getDescription().length(), 2999);

    }

    @Test
    public void test092_updateAreaShouldFailIfDescriptionIs2999Chars() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin tries to update Area with the following call updateArea,
        //  description length is 2999 chars
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Area area = createArea(hproject);
        area.setDescription(createStringFieldWithSpecifiedLenght(2999));
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(((Area) restResponse.getEntity()).getDescription().length(), 2999);
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


    private HProject createHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        HProject hproject = new HProject();
        hproject.setName("Project " + UUID.randomUUID());
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

    private String testMaxDescription(int lengthDescription) {
        String symbol = "a";
        String description = String.format("%" + lengthDescription + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(lengthDescription, description.length());
        return description;
    }


    private Area createArea(HProject hproject) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Area of user: " + hproject.getUser().getUsername());
        area.setProject(hproject);
        area.setAreaViewType(AreaViewType.IMAGE);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals("Area of user: " + hproject.getUser().getUsername(), ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        return area;
    }

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Property of: " + hproject.getUser().getUsername());
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
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

    private Area setNewAreaImage(HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNotNull(((Area) restResponseImage.getEntity()).getImagePath());

        String newImageName = String.valueOf(area.getId()).concat("_img.").concat(fileExtension);
        Assert.assertTrue(((Area) restResponseImage.getEntity()).getImagePath().contains(newImageName));
        return (Area) restResponseImage.getEntity();
    }


    @After
    public void afterTest() {
        HyperIoTAreaTestUtil.eraseDatabase(this);
    }

}

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

import it.acsoftware.hyperiot.area.actions.HyperIoTAreaAction;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.model.AreaViewType;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
import it.acsoftware.hyperiot.role.api.RoleRepository;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
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
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static it.acsoftware.hyperiot.area.test.HyperIoTAreaConfiguration.*;

/**
 * @author Vincenzo Longo (vincenzo.longo@acsoftware.it)
 * @version 2019-04-19 Initial release
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAreaWithPermissionRestTest extends KarafTestSupport {

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
        huser = createHUser(null);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Area Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_saveAreaWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, save Area with the following call saveArea
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Area of user: " + huser.getUsername());
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponse.getEntity()).getName());
        Assert.assertEquals("Area of user: " + hproject.getUser().getUsername(), ((Area) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((Area) restResponse.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test003_saveAreaWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to save Area with the following call saveArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID().toString().replaceAll("-", ""));
        area.setDescription("Description");
        area.setProject(hproject);
        //removing default user
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test004_updateAreaWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, update Area with the following call updateArea
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Date date = new Date();
        area.setDescription("Description updated in date: " + date);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals(huser.getUsername(),
                ((Area) restResponse.getEntity()).getProject().getUser().getUsername());
        Assert.assertEquals("Description updated in date: " + date,
                ((Area) restResponse.getEntity()).getDescription());
    }


    @Test
    public void test005_updateAreaWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to update Area with the following call updateArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        area.setDescription("update failed");
        //removing default user
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test006_findAreaWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, find Area with the following call findArea
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(area.getId(), ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((Area) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((Area) restResponse.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test007_findAreaWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to find Area with the following call findArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        //removing default user
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findArea(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test008_findAreaWithPermissionShouldFailIfEntityNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // Huser, with permission, tries to find Area with the following call findArea,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findArea(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test009_findAreaNotFoundWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to find Area not found with the following call findArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findArea(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test010_findAllAreaWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, find all Area with the following call findAllArea
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllArea();
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertFalse(listAreas.isEmpty());
        Assert.assertEquals(1, listAreas.size());
        boolean areaFound = false;
        for (Area a : listAreas) {
            if (area.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(huser.getId(), a.getProject().getUser().getId());
                areaFound = true;
            }
        }
        Assert.assertTrue(areaFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test011_findAllAreaWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to find all Area with the following call findAllArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        //removing default user
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllArea();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test012_deleteAreaWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, delete Area with the following call deleteArea
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.deleteArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test013_deleteAreaWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to delete Area with the following call deleteArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        //removing default user
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.deleteArea(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test014_deleteAreaWithPermissionShouldFailIfEntityNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to delete Area with the following call deleteArea,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.deleteArea(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test015_deleteAreaNotFoundWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to delete Area not found with the following call deleteArea
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.deleteArea(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test016_saveAreaWithPermissionShouldFailIfNameIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = new Area();
        area.setName(null);
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
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
    public void test017_saveAreaWithPermissionShouldFailIfNameIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = new Area();
        area.setName("");
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
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
    public void test018_saveAreaWithPermissionShouldFailIfNameIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("</script>");
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
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
    public void test019_saveAreaWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("javascript:");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
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
    public void test020_saveAreaWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        int maxDescription = 3001;
        area.setDescription(testMaxDescription(maxDescription));
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maxDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }


    @Test
    public void test021_saveAreaWithPermissionShouldFailIfHProjectIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID().toString().replaceAll("-", ""));
        area.setDescription("Description");
        area.setProject(null);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-project", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test022_saveAreaWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but HProject belongs to another HUser
        // response status code '403' HyperIoTUnauthorizedException
        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID().toString().replaceAll("-", ""));
        area.setDescription("Description");
        area.setProject(anotherHProject);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());
        Assert.assertNotEquals(huser.getId(), anotherHProject.getUser().getId());
        Assert.assertNotEquals(huser.getId(), huser2.getId());
    }


    @Test
    public void test023_updateAreaWithPermissionShouldFailIfNameIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        area.setName(null);
        this.impersonateUser(areaRestApi, huser);
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
    public void test024_updateAreaWithPermissionShouldFailIfNameIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        area.setName("");
        this.impersonateUser(areaRestApi, huser);
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
    public void test025_updateAreaWithPermissionShouldFailIfNameIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        area.setName("<script malicious code>");
        this.impersonateUser(areaRestApi, huser);
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
    public void test026_updateAreaWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        area.setDescription("onload(malicious code)=");
        this.impersonateUser(areaRestApi, huser);
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
    public void test027_updateAreaWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        int maxDescription = 3001;
        area.setDescription(testMaxDescription(maxDescription));
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maxDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }


    @Test
    public void test028_updateAreaWithPermissionShouldFailIfHProjectIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        area.setProject(null);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("area-project", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test029_updateAreaWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but HProject belongs to another HUser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        area.setProject(anotherHProject);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());
        Assert.assertNotEquals(huser.getId(), anotherHProject.getUser().getId());
        Assert.assertNotEquals(huser.getId(), huser2.getId());
    }


    @Test
    public void test030_addAreaDeviceWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, add an AreaDevice with the following call addAreaDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(huser.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test031_addAreaDeviceWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to add an AreaDevice with
        // the following call addAreaDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        //removing default user
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test032_addAreaDeviceWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to add an AreaDevice with the following call addAreaDevice,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(0, areaDevice);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test033_addAreaDeviceWithoutPermissionShouldFailAndAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to add an AreaDevice with an Area not found with
        // the following call addAreaDevice
        // response status code '403' HyperIoTEntityNotFound
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(0, areaDevice);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test034_addAreaDeviceWithPermissionShouldFailIfHDeviceNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to add an AreaDevice with the following call addAreaDevice,
        // but HDevice is not assigned
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        //hdevice is not assigned with areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test035_addAreaDeviceWithoutPermissionShouldFailAndHDeviceNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to add an AreaDevice with HDevice not found with
        // the following call addAreaDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        //hdevice is not assigned with areaDevice.setDevice(hdevice);
        //removing default user
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test036_addAreaDeviceWithPermissionShouldFailIfEntityIsDuplicated() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to add an AreaDevice with the following call addAreaDevice,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        AreaDevice areaDeviceDuplicated = new AreaDevice();
        areaDeviceDuplicated.setDevice(hdevice);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseDuplicate = areaRestApi.addAreaDevice(area.getId(), areaDeviceDuplicated);
        Assert.assertEquals(409, restResponseDuplicate.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponseDuplicate.getEntity()).getType());
        Assert.assertEquals(1,
                ((HyperIoTBaseError) restResponseDuplicate.getEntity()).getErrorMessages().size());
        Assert.assertEquals("Device already mapped",
                ((HyperIoTBaseError) restResponseDuplicate.getEntity()).getErrorMessages().get(0));
    }


    @Test
    public void test037_addAreaDeviceDuplicateWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to add an AreaDevice duplicated with the following
        // call addAreaDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        AreaDevice areaDeviceDuplicated = new AreaDevice();
        areaDeviceDuplicated.setDevice(hdevice);
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseDuplicate = areaRestApi.addAreaDevice(area.getId(), areaDeviceDuplicated);
        Assert.assertEquals(403, restResponseDuplicate.getStatus());
    }


    @Test
    public void test038_removeAreaDeviceWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, remove an AreaDevice with the following call removeAreaDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test039_removeAreaDeviceWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to remove an AreaDevice with the
        // following call removeAreaDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), areaDevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test040_removeAreaDeviceWithPermissionShouldFailIfAreaDeviceNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to remove an AreaDevice with the
        // following call removeAreaDevice, but AreaDevice not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), 0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test041_removeAreaDeviceNotFoundWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to remove an AreaDevice not found with the
        // following call removeAreaDevice
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), 0);
        Assert.assertEquals(403, restResponse.getStatus());
    }


    @Test
    public void test042_removeAreaDeviceWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to remove an AreaDevice with the
        // following call removeAreaDevice, but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(0, areaDevice.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test043_removeAreaDeviceWithPermissionShouldFailIfArea2NotFoundInAreaDevice() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission tries to remove an AreaDevice with the following call removeAreaDevice,
        // but area2 not found in AreaDevice
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        Area area2 = createArea(hproject);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject.getId(), area2.getProject().getId());
        Assert.assertEquals(huser.getId(), area2.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), area2.getProject().getId());
        // checks: same huser but different area
        Assert.assertEquals(area.getProject().getUser().getId(), area2.getProject().getUser().getId());
        Assert.assertNotEquals(area.getId(), area2.getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(area2.getId(), areaDevice.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test044_removeAreaDeviceWithPermissionShouldFailIfAreaDeviceBelongsToAnotherUser() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to remove areaDevice with the
        // following call removeAreaDevice, but areaDevice belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);

        //different areaDevice: AreaDevice belongs to huser2
        huser2 = createHUser(null);
        HProject anotherHproject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHproject.getId());
        Assert.assertEquals(huser2.getId(), anotherHproject.getUser().getId());

        Area anotherArea = createArea(anotherHproject);
        Assert.assertNotEquals(0, anotherArea.getId());
        Assert.assertEquals(anotherHproject.getId(), anotherArea.getProject().getId());
        Assert.assertEquals(huser2.getId(), anotherArea.getProject().getUser().getId());

        HDevice anotherHdevice = createHDevice(anotherHproject);
        Assert.assertNotEquals(0, anotherHdevice.getId());
        Assert.assertEquals(anotherHproject.getId(), anotherHdevice.getProject().getId());
        Assert.assertEquals(huser2.getId(), anotherHdevice.getProject().getUser().getId());

        Assert.assertEquals(anotherArea.getProject().getId(), anotherHdevice.getProject().getId());

        AreaDevice anotherAreaDevice = createAreaDevice(anotherArea, anotherHdevice);
        Assert.assertNotEquals(0, anotherAreaDevice.getId());
        Assert.assertEquals(anotherArea.getId(), anotherAreaDevice.getArea().getId());
        Assert.assertEquals(huser2.getId(), anotherAreaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(anotherHdevice.getId(), anotherAreaDevice.getDevice().getId());
        Assert.assertEquals(huser2.getId(), anotherAreaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        // anotherAreaDevice -> huser2
        Response restResponse = areaRestApi.removeAreaDevice(anotherArea.getId(), anotherAreaDevice.getId());
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test045_getAreaDevicesListWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, find AreaDevice list with the following call getAreaDeviceList
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
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
                    Assert.assertEquals(huser.getId(), ad.getArea().getProject().getUser().getId());
                    hasDevice = true;
                }
                if (area.getId() == ad.getArea().getId()) {
                    Assert.assertEquals(hdevice.getId(), ad.getDevice().getId());
                    Assert.assertEquals(huser.getId(), ad.getDevice().getProject().getUser().getId());
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
    public void test046_getAreaDevicesListWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to find AreaDevice list with the
        // following call getAreaDeviceList
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDeviceList(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test047_getAreaDevicesListWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to find AreaDevice list with the following call getAreaDeviceList,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDeviceList(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test048_getAreaDevicesNotFoundWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to find AreaDevice not found with
        // the following call getAreaDeviceList
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDeviceList(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test049_getAreaDevicesListWithPermissionShouldWorkIfListIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, find AreaDevice list with the following call getAreaDeviceList
        // if listAreaDevices is empty. There are no HDevice in Area.
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDeviceList(area.getId());
        List<HDevice> listAreaDevices = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertEquals(0, listAreaDevices.size());
        Assert.assertTrue(listAreaDevices.isEmpty());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test050_saveAreaWithPermissionShouldFailIfEntityIsDuplicated() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to save Area with the following call saveArea,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Area duplicateArea = new Area();
        duplicateArea.setName(area.getName());
        duplicateArea.setParentArea(area.getParentArea());
        duplicateArea.setProject(area.getProject());
        this.impersonateUser(areaRestApi, huser);
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
    public void test051_saveAreaDuplicatedWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to save duplicate Area with the following call saveArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Area duplicateArea = new Area();
        duplicateArea.setName(area.getName());
        duplicateArea.setParentArea(area.getParentArea());
        duplicateArea.setProject(area.getProject());
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(duplicateArea);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test052_updateAreaWithPermissionShouldFailIfEntityIsDuplicated() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Area duplicateArea = createArea(hproject);
        Assert.assertNotEquals(0, duplicateArea.getId());
        Assert.assertEquals(hproject.getId(), duplicateArea.getProject().getId());
        Assert.assertEquals(huser.getId(), duplicateArea.getProject().getUser().getId());

        Assert.assertNotEquals(area.getId(), duplicateArea.getId());

        duplicateArea.setName(area.getName());
        duplicateArea.setParentArea(area.getParentArea());
        duplicateArea.setProject(area.getProject());
        this.impersonateUser(areaRestApi, huser);
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
    public void test053_updateAreaDuplicatedWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to update duplicate Area with the following call updateArea
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Area duplicateArea = createArea(hproject);
        Assert.assertNotEquals(0, duplicateArea.getId());
        Assert.assertEquals(hproject.getId(), duplicateArea.getProject().getId());
        Assert.assertEquals(huser.getId(), duplicateArea.getProject().getUser().getId());

        Assert.assertNotEquals(area.getId(), duplicateArea.getId());

        duplicateArea.setName(area.getName());
        duplicateArea.setParentArea(area.getParentArea());
        duplicateArea.setProject(area.getProject());
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(duplicateArea);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test054_updateAreaWithPermissionShouldFailIfEntityNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        // entity isn't stored in database
        Area area = new Area();
        area.setDescription("Description edited");
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test055_updateAreaNotFoundWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to update Area not found with the following call updateArea
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        // entity isn't stored in database
        Area area = new Area();
        area.setDescription("Description edited");
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test056_addAreaDeviceWithPermissionShouldFailIfAreaBelongsToHUser2() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to add an areaDevice with the following call addAreaDevice,
        // but Area belongs to huser2
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject1 = createHProject(huser);
        Assert.assertNotEquals(0, hproject1.getId());
        Assert.assertEquals(huser.getId(), hproject1.getUser().getId());

        HDevice hdevice1 = createHDevice(hproject1);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject1.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        AreaDevice areaDevice1 = new AreaDevice();
        areaDevice1.setDevice(hdevice1);

        //different HUser
        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Area area2 = createArea(hproject2);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject2.getId(), area2.getProject().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());

        Assert.assertNotEquals(huser2.getId(), hproject1.getUser().getId());
        Assert.assertNotEquals(huser2.getId(), hdevice1.getProject().getUser().getId());
        Assert.assertNotEquals(hproject1.getId(), hproject2.getId());
        Assert.assertNotEquals(hproject1.getId(), area2.getProject().getId());
        Assert.assertNotEquals(hproject2.getId(), hdevice1.getProject().getId());

        this.impersonateUser(areaRestApi, huser);
        // area2         -> huser2
        // areaDevice1   -> huser
        Response restResponse = areaRestApi.addAreaDevice(area2.getId(), areaDevice1);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test057_addAreaDeviceWithPermissionShouldFailIfHDeviceBelongsToHuser2() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to add an AreaDevice with the following call addAreaDevice,
        // but HDevice is associated with huser2
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject1 = createHProject(huser);
        Assert.assertNotEquals(0, hproject1.getId());
        Assert.assertEquals(huser.getId(), hproject1.getUser().getId());

        Area area1 = createArea(hproject1);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject1.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        // different huser
        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        HDevice hdevice2 = createHDevice(hproject2);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject2.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser2.getId(), hdevice2.getProject().getUser().getId());

        AreaDevice areaDevice2 = new AreaDevice();
        areaDevice2.setDevice(hdevice2);

        //checks: area1 is associated to huser, hdevice2 is associated to huser2
        Assert.assertNotEquals(huser.getId(), huser2.getId());

        Assert.assertNotEquals(area1.getProject().getId(), hdevice2.getProject().getId());
        Assert.assertNotEquals(area1.getProject().getUser().getId(),
                hdevice2.getProject().getUser().getId());

        // area1          -> huser
        // areaDevice2    -> huser2
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area1.getId(), areaDevice2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test058_huserTriesToAddAreaDeviceAssociatedToHUser2WithPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to add an AreaDevice with the following call addAreaDevice,
        // but AreaDevice is associated with huser2
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);

        // different huser
        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        HDevice hdevice2 = createHDevice(hproject2);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject2.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser2.getId(), hdevice2.getProject().getUser().getId());

        Area area2 = createArea(hproject2);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject2.getId(), area2.getProject().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice2);
        //checks: area2 is associated to huser2, hdevice2 is associated to huser2
        Assert.assertEquals(area2.getProject().getId(), hdevice2.getProject().getId());
        Assert.assertEquals(area2.getProject().getUser().getId(),
                hdevice2.getProject().getUser().getId());

        // checks: huser isn't associated with hproject2, area2, hdevice2
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), area2.getProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), hdevice2.getProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());

        // area2        -> huser2
        // areaDevice   -> huser2
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area2.getId(), areaDevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test059_getAreaDeviceWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, find AreaDevice with the following call getAreaDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());
        Assert.assertEquals(area.getProject().getUser().getId(), hdevice.getProject().getUser().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(areaDevice.getId(), ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getId());

        Assert.assertEquals(hproject.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getId());
        Assert.assertEquals(hproject.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getId());

        Assert.assertEquals(huser.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test060_getAreaDeviceWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, without permission, tries to find AreaDevice with the following call getAreaDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());
        Assert.assertEquals(area.getProject().getUser().getId(), hdevice.getProject().getUser().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test061_getAreaDeviceWithPermissionShouldFailIfAreaDeviceNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, tries to find AreaDevice with the following call getAreaDevice,
        // but AreaDevice not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test062_getAreaDeviceWithPermissionShouldShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, tries to find AreaDevice with the following call getAreaDevice,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());
        Assert.assertEquals(area.getProject().getUser().getId(), hdevice.getProject().getUser().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test063_getAreaDeviceWithPermissionShouldFailIfAreaDeviceBelongsToHuser2() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, tries to find AreaDevice with the following call getAreaDevice,
        // but areaDevice belongs to huser2
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Area area2 = createArea(hproject2);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject2.getId(), area2.getProject().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject2);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject2.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser2.getId(), hdevice2.getProject().getUser().getId());

        Assert.assertEquals(area2.getProject().getId(), hdevice2.getProject().getId());
        Assert.assertEquals(area2.getProject().getUser().getId(), hdevice2.getProject().getUser().getId());

        AreaDevice areaDevice2 = createAreaDevice(area2, hdevice2);
        Assert.assertNotEquals(0, areaDevice2.getId());
        Assert.assertEquals(area2.getId(), areaDevice2.getArea().getId());
        Assert.assertEquals(huser2.getId(), areaDevice2.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice2.getId(), areaDevice2.getDevice().getId());
        Assert.assertEquals(huser2.getId(), areaDevice2.getDevice().getProject().getUser().getId());

        // checks: huser isn't associated with hproject2, area2, hdevice2, areaDevice2
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), area2.getProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), hdevice2.getProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), areaDevice2.getArea().getProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), areaDevice2.getDevice().getProject().getUser().getId());

        // checks: huser2 is associated with hproject2, area2, hdevice2, areaDevice2
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), hdevice2.getProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), areaDevice2.getArea().getProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), areaDevice2.getDevice().getProject().getUser().getId());

        // huser tries to find areaDevice2. AreaDevice2 belongs to huser2
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDevice(areaDevice2.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test064_removeAreaWithPermissionShouldFailIfAreaBelongsToHuser2() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to remove Area with the following call deleteArea,
        // but area belongs to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);

        // another huser
        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Area area2 = createArea(hproject2);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject2.getId(), area2.getProject().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());

        // checks: huser isn't associated with hproject2, area2
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), area2.getProject().getUser().getId());

        // checks: huser2 is associated with hproject2, area2
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());
        Assert.assertEquals(hproject2.getId(), area2.getProject().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.deleteArea(area2.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test065_getAreaPathWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser find AreaPath list with the following call getAreaPath
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaPath(area.getId());
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertFalse(listAreas.isEmpty());
        Assert.assertEquals(1, listAreas.size());
        boolean areaFound = false;
        for (Area a : listAreas) {
            if (a.getId() == area.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(huser.getId(), a.getProject().getUser().getId());
                areaFound = true;
            }
        }
        Assert.assertTrue(areaFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test066_getAreaPathWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, without permission, tries to find AreaPath list
        // with the following call getAreaPath
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaPath(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test067_getAreaPathWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser find AreaPath list with the following call getAreaPath
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaPath(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test068_getAreaPathWithPermissionShouldFailIfAreaPathBelongsToHuser2() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, tries to find AreaPath list with the following call getAreaPath,
        // but area belongs to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Area area2 = createArea(hproject2);
        Assert.assertNotEquals(0, area2.getId());
        Assert.assertEquals(hproject2.getId(), area2.getProject().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());

        // checks: huser2 is associated with hproject2, area2
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), area2.getProject().getUser().getId());
        Assert.assertEquals(hproject2.getId(), area2.getProject().getId());

        // checks: huser isn't associated with hproject2, area2
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), area2.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaPath(area2.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test069_findInnerAreasWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, find inner areas with the following call findInnerAreas
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Area parentArea = createArea(hproject);
        Assert.assertNotEquals(0, parentArea.getId());
        Assert.assertEquals(hproject.getId(), parentArea.getProject().getId());
        Assert.assertEquals(huser.getId(), parentArea.getProject().getUser().getId());

        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        this.impersonateUser(areaRestApi, huser);
        parentArea.setParentArea(area);
        Response restResponseUpdateArea = areaRestApi.updateArea(parentArea);
        Assert.assertEquals(200, restResponseUpdateArea.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseUpdateArea.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, huser);
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
    public void test070_findInnerAreasWithPermissionShouldFailIfParentAreaIsNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, find inner areas with the following call findInnerAreas,
        // inner areas is empty
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getParentArea());

        this.impersonateUser(areaRestApi, huser);
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
    public void test071_findInnerAreasWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, without permission, tries to find inner areas with the following
        // call findInnerAreas
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getParentArea());
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findInnerAreas(area.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test072_findInnerAreasWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, tries to find inner areas with the following call findInnerAreas,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findInnerAreas(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test073_findInnerMoreAreasWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, find inner areas with the following call findInnerAreas
        // response status code '200'
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

        Area parentArea1 = createArea(hproject);
        Assert.assertNotEquals(0, parentArea1.getId());
        Assert.assertEquals(hproject.getId(), parentArea1.getProject().getId());
        Assert.assertEquals(huser.getId(), parentArea1.getProject().getUser().getId());

        Area parentArea2 = createArea(hproject);
        Assert.assertNotEquals(0, parentArea2.getId());
        Assert.assertEquals(hproject.getId(), parentArea2.getProject().getId());
        Assert.assertEquals(huser.getId(), parentArea2.getProject().getUser().getId());

        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        this.impersonateUser(areaRestApi, huser);
        parentArea1.setParentArea(area1);
        Response restResponseUpdateParentArea1 = areaRestApi.updateArea(parentArea1);
        Assert.assertEquals(200, restResponseUpdateParentArea1.getStatus());
        Assert.assertEquals(parentArea1.getId(), ((Area) restResponseUpdateParentArea1.getEntity()).getId());
        Assert.assertEquals(parentArea1.getEntityVersion() + 1,
                ((Area) restResponseUpdateParentArea1.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, huser);
        parentArea2.setParentArea(area1);
        Response restResponseUpdateParentArea2 = areaRestApi.updateArea(parentArea2);
        Assert.assertEquals(200, restResponseUpdateParentArea2.getStatus());
        Assert.assertEquals(parentArea2.getId(), ((Area) restResponseUpdateParentArea2.getEntity()).getId());
        Assert.assertEquals(parentArea2.getEntityVersion() + 1,
                ((Area) restResponseUpdateParentArea2.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, huser);
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
    public void test074_huser2WithPermissionTriesToFindInnerAreasAssociatedToHUser2ShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser2, with permission, tries to find inner areas associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject1 = createHProject(huser);
        Assert.assertNotEquals(0, hproject1.getId());
        Assert.assertEquals(huser.getId(), hproject1.getUser().getId());

        Area area1 = createArea(hproject1);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject1.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        Area parentArea1 = createArea(hproject1);
        Assert.assertNotEquals(0, parentArea1.getId());
        Assert.assertEquals(hproject1.getId(), parentArea1.getProject().getId());
        Assert.assertEquals(huser.getId(), parentArea1.getProject().getUser().getId());

        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        this.impersonateUser(areaRestApi, huser);
        parentArea1.setParentArea(area1);
        Response restResponseUpdateParentArea1 = areaRestApi.updateArea(parentArea1);
        Assert.assertEquals(200, restResponseUpdateParentArea1.getStatus());
        Assert.assertEquals(parentArea1.getId(), ((Area) restResponseUpdateParentArea1.getEntity()).getId());
        Assert.assertEquals(parentArea1.getEntityVersion() + 1,
                ((Area) restResponseUpdateParentArea1.getEntity()).getEntityVersion());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findInnerAreas(area1.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonAreaTree = (String) restResponse.getEntity();

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
                                "\"innerArea\":[" +
                                "{\"id\":" + parentArea1.getId() + "," +
                                "\"entityVersion\":" + ((Area) restResponseUpdateParentArea1.getEntity()).getEntityVersion() + "," +
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

        // huser2, with permission, tries to find inner areas associated to another huser
        huser2 = createHUser(action);
        addPermission(huser2, action1);

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser2.getId(), hproject1.getUser().getId());
        Assert.assertNotEquals(huser2.getId(), area1.getProject().getUser().getId());
        Assert.assertNotEquals(huser2.getId(), parentArea1.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser2);
        Response restResponse2 = areaRestApi.findInnerAreas(area1.getId());
        Assert.assertEquals(403, restResponse2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse2.getEntity()).getType());
    }


    @Test
    public void test075_updateAreaWithPermissionShouldFailIfParentAreaBelongsToHUser2() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, tries to update area with parentArea associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area1 = createArea(hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Area parentArea2 = createArea(hproject2);
        Assert.assertNotEquals(0, parentArea2.getId());
        Assert.assertEquals(hproject2.getId(), parentArea2.getProject().getId());
        Assert.assertEquals(huser2.getId(), parentArea2.getProject().getUser().getId());

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), parentArea2.getProject().getUser().getId());

        Assert.assertNotEquals(huser2.getId(), hproject.getUser().getId());
        Assert.assertNotEquals(huser2.getId(), area1.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        parentArea2.setParentArea(area1);
        Response restResponseUpdate1 = areaRestApi.updateArea(parentArea2);
        Assert.assertEquals(403, restResponseUpdate1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseUpdate1.getEntity()).getType());
    }


    @Test
    public void test076_setAreaImageWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, set an Area image with the following call setAreaImage
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNotNull(((Area) restResponseImage.getEntity()).getImagePath());

        String newImageName = String.valueOf(area.getId()).concat("_img.").concat(fileExtension);
        Assert.assertTrue(((Area) restResponseImage.getEntity()).getImagePath().contains(newImageName));
    }


    @Test
    public void test077_setAreaImageWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to set an Area image with the following call setAreaImage,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.setAreaImage(0, jpgAttachment);
        Assert.assertEquals(404, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test078_setAreaImageWithPermissionShouldFailIfImageIsMissing() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to set an Area image with the following call setAreaImage,
        // but image file is missing
        // response status code '422' java.io.IOException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        this.impersonateUser(areaRestApi, huser);
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
    public void test079_setAreaImageWithPermissionShouldFailIfContentDispositionHeaderIsNotSupported() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, set an Area image with the following call setAreaImage,
        // but content disposition header is not supported
        // response status code '500' java.io.IOException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment not valid";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, huser);
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
    public void test080_setAreaImageWithPermissionShouldFailIfFileExtensionIsNotSupported() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, set an Area image with the following call setAreaImage,
        // but file extension is not supported
        // response status code '500' java.io.IOException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"image.pdf\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        Attachment jpgAttachment = new Attachment("", imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, huser);
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
    public void test081_setAreaImageWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, set an Area image with the following call setAreaImage
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(403, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test082_huser2WithPermissionTriesToSetAreaImageAssociatedToHUser2ShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser2, with permission, tries to set an Area image associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject1 = createHProject(huser);
        Assert.assertNotEquals(0, hproject1.getId());
        Assert.assertEquals(huser.getId(), hproject1.getUser().getId());

        Area area1 = createArea(hproject1);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject1.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());
        Assert.assertNull(area1.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        // huser2, with permission, tries to set an Area image associated to another huser
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser2 = createHUser(action);
        this.impersonateUser(areaRestApi, huser2);
        Response restResponseImage = areaRestApi.setAreaImage(area1.getId(), jpgAttachment);
        Assert.assertEquals(404, restResponseImage.getStatus());
    }


    @Test
    public void test083_getAreaImageWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, get an Area image with the following call getAreaImage
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.getAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(new File(area.getImagePath()), restResponseImage.getEntity());
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Type")
                .contains("application/octet-stream"));
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Disposition")
                .contains("attachment; filename=\"" + area.getId() + "_img.jpg\""));
    }


    @Test
    public void test084_getAreaImageWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to get an Area image with the following call getAreaImage,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.getAreaImage(0);
        Assert.assertEquals(404, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test085_getAreaImageWithPermissionShouldFailIfImagePathIsNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        // HUser, with permission, tries to get an Area image with the following call getAreaImage,
        // but image path is not found
        // response status code '500' java.io.IOException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        area.setImagePath("../../../src/main/resources/folderNotFound/");
        this.impersonateUser(areaRestApi, huser);
        int areaPreviousVersion = area.getEntityVersion();
        area = areaSystemApi.update(area, null);
        Assert.assertEquals(areaPreviousVersion + 1,
                area.getEntityVersion());
        Assert.assertNotEquals(0, area.getId());

        this.impersonateUser(areaRestApi, huser);
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
    public void test086_getAreaImageWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, tries to get an Area image with the following
        // call getAreaImage
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.getAreaImage(area.getId());
        Assert.assertEquals(403, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test087_huser2WithPermissionTriesToGetAreaImageAssociatedToHUserShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser2, with permission, tries to get an Area image associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());
        huser2 = createHUser(action1);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.getAreaImage(area.getId());
        this.impersonateUser(areaRestApi, huser2);
        Response restResponseImage1 = areaRestApi.getAreaImage(area.getId());
        //checks huser can see the image
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(new File(area.getImagePath()), restResponseImage.getEntity());
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Type")
                .contains("application/octet-stream"));
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Disposition")
                .contains("attachment; filename=\"" + area.getId() + "_img.jpg\""));
        //Checks huser2 cannot see that image
        Assert.assertEquals(404, restResponseImage1.getStatus());
    }


    @Test
    public void test088_unsetAreaImageWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, unset an Area image with the following call unsetAreaImage
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNull(((Area) restResponseImage.getEntity()).getImagePath());
    }


    @Test
    public void test089_unsetAreaImageWithPermissionShouldFailIfAreaNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to unset an Area image with the following call unsetAreaImage,
        // but Area not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.unsetAreaImage(0);
        Assert.assertEquals(404, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test090_unsetAreaImageWithPermissionShouldWorkIfImagePathIsNotFound() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        // HUser, with permission, tries to unset an Area image with the following call unsetAreaImage,
        // but image path is not found
        // response status code '500' java.io.IOException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        area.setImagePath("../../../src/main/resources/folderNotFound/");
        this.impersonateUser(areaRestApi, huser);
        int areaPreviousVersion = area.getEntityVersion();
        area = areaSystemApi.update(area, null);
        Assert.assertEquals(areaPreviousVersion + 1,
                area.getEntityVersion());
        Assert.assertNotEquals(0, area.getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNull(((Area) restResponseImage.getEntity()).getImagePath());
    }


    @Test
    public void test091_unsetAreaImageWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, without permission, unset an Area image with the following call unsetAreaImage
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());
        removeDefaultPermission(huser);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(403, restResponseImage.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseImage.getEntity()).getType());
    }


    @Test
    public void test092_huser2WithPermissionTriesToUnsetAreaImageAssociatedTohuser2ShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser2, with permission, tries to unset an Area image associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        addPermission(huser, action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = setNewAreaImage(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        // huser2, with permission, tries to unset an Area image associated to another huser
        huser2 = createHUser(action1);
        this.impersonateUser(areaRestApi, huser2);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(404, restResponseImage.getStatus());
    }


    @Test
    public void test093_findAllAreaPaginatedWithPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all areas with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 6;
        int page = 2;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(defaultDelta, areas.size());
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listAreas.getResults().size());
        Assert.assertEquals(delta, listAreas.getDelta());
        Assert.assertEquals(page, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // delta is 6, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(areaRestApi, huser);
        Response restResponsePage1 = areaRestApi.findAllAreaPaginated(delta, 1);
        HyperIoTPaginableResult<Area> listAreasPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listAreasPage1.getResults().size());
        Assert.assertEquals(delta, listAreasPage1.getDelta());
        Assert.assertEquals(defaultPage, listAreasPage1.getCurrentPage());
        Assert.assertEquals(page, listAreasPage1.getNextPage());
        // delta is 6, page is 1: 10 entities stored in database
        Assert.assertEquals(2, listAreasPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test094_findAllAreaPaginatedWithoutPermissionShouldFail() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, HUser, without permission,
        // tries to find all Areas with pagination
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test095_findAllAreaPaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all Areas with pagination
        // if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 7;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listAreas.getResults().size());
        Assert.assertEquals(defaultDelta, listAreas.getDelta());
        Assert.assertEquals(defaultPage, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // default delta is 10, default page is 1: 7 entities stored in database
        Assert.assertEquals(1, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test096_findAllAreaPaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all Areas with pagination
        // if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = -1;
        int page = 2;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 9;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        // entities not showed in page 2
        Assert.assertTrue(listAreas.getResults().isEmpty());
        Assert.assertEquals(0, listAreas.getResults().size());
        Assert.assertEquals(defaultDelta, listAreas.getDelta());
        Assert.assertEquals(page, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // default delta is 10, page is 2: 9 entities stored in database
        Assert.assertEquals(1, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(areaRestApi, huser);
        Response restResponsePage1 = areaRestApi.findAllAreaPaginated(delta, 1);
        HyperIoTPaginableResult<Area> listAreasPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage1.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listAreasPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listAreasPage1.getDelta());
        Assert.assertEquals(defaultPage, listAreasPage1.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreasPage1.getNextPage());
        // default delta is 10, page is 1: 9 entities stored in database
        Assert.assertEquals(1, listAreasPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test097_findAllAreaPaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all Areas with pagination
        // if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 0;
        int page = 2;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 12;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listAreas.getResults().size());
        Assert.assertEquals(defaultDelta, listAreas.getDelta());
        Assert.assertEquals(page, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // default delta is 10, page is 2: 12 entities stored in database
        Assert.assertEquals(2, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(areaRestApi, huser);
        Response restResponsePage1 = areaRestApi.findAllAreaPaginated(delta, 1);
        HyperIoTPaginableResult<Area> listAreasPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAreasPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listAreasPage1.getDelta());
        Assert.assertEquals(defaultPage, listAreasPage1.getCurrentPage());
        Assert.assertEquals(page, listAreasPage1.getNextPage());
        // default delta is 10, page is 1: 12 entities stored in database
        Assert.assertEquals(2, listAreasPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test098_findAllAreaPaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all Areas with pagination
        // if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 7;
        int page = -1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 5;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listAreas.getResults().size());
        Assert.assertEquals(delta, listAreas.getDelta());
        Assert.assertEquals(defaultPage, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // delta is 7, default page is 1: 5 entities stored in database
        Assert.assertEquals(1, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test099_findAllAreaPaginationWithPermissionShouldWorkIfPageIsZero() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all Areas with pagination
        // if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 5;
        int page = 0;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<Area> areas = new ArrayList<>();
        int numbEntities = 8;
        for (int i = 0; i < numbEntities; i++) {
            Area area = createArea(hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        Assert.assertEquals(numbEntities, areas.size());
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(delta, listAreas.getResults().size());
        Assert.assertEquals(delta, listAreas.getDelta());
        Assert.assertEquals(defaultPage, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listAreas.getNextPage());
        // delta is 5, default page is 1: 8 entities stored in database
        Assert.assertEquals(2, listAreas.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(areaRestApi, huser);
        Response restResponsePage2 = areaRestApi.findAllAreaPaginated(delta, 2);
        HyperIoTPaginableResult<Area> listAreasPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertFalse(listAreasPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listAreasPage2.getResults().size());
        Assert.assertEquals(delta, listAreasPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listAreasPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreasPage2.getNextPage());
        // delta is 5, page is 2: 8 entities stored in database
        Assert.assertEquals(2, listAreasPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test100_updateAreaWithPermissionShouldFailIfAreaBelongsToAnotherUser() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to update Area with the following call updateArea,
        // but HProject belongs to another HUser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);

        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        Area anotherArea = createArea(anotherHProject);
        Assert.assertNotEquals(0, anotherArea.getId());
        Assert.assertEquals(anotherHProject.getId(), anotherArea.getProject().getId());
        Assert.assertEquals(huser2.getId(), anotherArea.getProject().getUser().getId());

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), anotherHProject.getUser().getId());

        anotherArea.setDescription("Edited failed...");

        //huser tries to update an Area associated to another huser
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(anotherArea);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test101_deleteAreaWithPermissionNotDeleteInCascadeHProject() {
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
        Assert.assertFalse(listHProjectAreas.isEmpty());
        Assert.assertEquals(2, listHProjectAreas.size());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area a : listHProjectAreas) {
            if (area1.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(huser.getId(), a.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(huser.getId(), a.getProject().getUser().getId());
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
        Response restResponseDeleteArea1 = areaRestApi.deleteArea(area1.getId());
        Assert.assertEquals(200, restResponseDeleteArea1.getStatus());
        Assert.assertNull(restResponseDeleteArea1.getEntity());
        this.impersonateUser(areaRestApi, huser);
        Response restResponseDeleteArea2 = areaRestApi.deleteArea(area2.getId());
        Assert.assertEquals(200, restResponseDeleteArea2.getStatus());
        Assert.assertNull(restResponseDeleteArea2.getEntity());

        // checks if hproject is still stored in database
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseFindHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseFindHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseFindHProject.getEntity()).getId());
        Assert.assertEquals(huser.getId(), ((HProject) restResponseFindHProject.getEntity()).getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponseHProjectAreaList = hprojectRestService.getHProjectAreaList(hproject.getId());
        Collection<Area> listHProjectAreasEmpty = restResponseHProjectAreaList.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertEquals(0, listHProjectAreasEmpty.size());
        Assert.assertTrue(listHProjectAreasEmpty.isEmpty());
        Assert.assertEquals(200, restResponseHProjectAreaList.getStatus());
    }


    @Test
    public void test102_deleteHProjectWithPermissionDeleteInCascadeAllAreas() {
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
        Assert.assertFalse(listHProjectAreas.isEmpty());
        Assert.assertEquals(2, listHProjectAreas.size());
        boolean area1Found = false;
        boolean area2Found = false;
        for (Area a : listHProjectAreas) {
            if (area1.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(huser.getId(), a.getProject().getUser().getId());
                area1Found = true;
            }
            if (area2.getId() == a.getId()) {
                Assert.assertEquals(hproject.getId(), a.getProject().getId());
                Assert.assertEquals(huser.getId(), a.getProject().getUser().getId());
                area2Found = true;
            }
        }
        Assert.assertTrue(area1Found);
        Assert.assertTrue(area2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // huser deletes hproject with call deleteHProject
        // this call deletes in cascade all areas
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
        Assert.assertNull(restResponseDeleteHProject.getEntity());

        // checks: all areas not found;
        // area has been deleted in cascade mode with deleteHProject call
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseFindArea1 = areaRestApi.findArea(area1.getId());
        Assert.assertEquals(404, restResponseFindArea1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea1.getEntity()).getType());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseFindArea2 = areaRestApi.findArea(area2.getId());
        Assert.assertEquals(404, restResponseFindArea2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea2.getEntity()).getType());
    }


    @Test
    public void test103_removeHProjectDeleteInCascadeModeAreaAndAreaDevice() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // hadmin deletes hproject, with area and areaDevice associated, with the
        // following call deleteHProject; this call deletes in cascade mode all
        // area and areaDevice associated
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        addPermission(huser, action1);
        this.impersonateUser(areaRestApi, huser);
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
                    Assert.assertEquals(huser.getId(), ad.getArea().getProject().getUser().getId());
                    hasDevice = true;
                }
                if (area.getId() == ad.getArea().getId()) {
                    Assert.assertEquals(hdevice.getId(), ad.getDevice().getId());
                    Assert.assertEquals(huser.getId(), ad.getDevice().getProject().getUser().getId());
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
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
        Assert.assertNull(restResponseDeleteHProject.getEntity());

        // checks: area and areaDevice not found;
        // Area and AreaDevice has been deleted in cascade mode with deleteHProject call
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseFindArea = areaRestApi.findArea(area.getId());
        Assert.assertEquals(404, restResponseFindArea.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindArea.getEntity()).getType());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseFindAreaDevice = areaRestApi.getAreaDevice(areaDevice.getId());
        Assert.assertEquals(404, restResponseFindAreaDevice.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindAreaDevice.getEntity()).getType());
    }


    @Test
    public void test104_removeAreaWithPermissionDeleteInCascadeAllAreaDeviceNotDeleteAllDevices() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, remove an Area with call deleteArea and deletes in cascade mode
        // all AreaDevice; this call not deletes all devices
        // response status code '200'
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

        Assert.assertEquals(area1.getProject().getId(), hdevice1.getProject().getId());
        Assert.assertEquals(area1.getProject().getId(), hdevice2.getProject().getId());
        Assert.assertEquals(area1.getProject().getId(), hdevice3.getProject().getId());

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

        AreaDevice ad3 = createAreaDevice(area1, hdevice3);
        Assert.assertNotEquals(0, ad3.getId());
        Assert.assertEquals(hdevice3.getId(), ad3.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad3.getArea().getId());
        Assert.assertEquals(huser.getId(), ad3.getArea().getProject().getUser().getId());

        //checks if AreaDevice exists
        this.impersonateUser(areaRestApi, huser);
        Response restResponseAreaDevice = areaRestApi.getAreaDeviceList(area1.getId());
        List<AreaDevice> listFirstAreaDevices = restResponseAreaDevice.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertEquals(3, listFirstAreaDevices.size());
        Assert.assertFalse(listFirstAreaDevices.isEmpty());
        Assert.assertEquals(200, restResponseAreaDevice.getStatus());

        AreaDevice areaDevice1 = listFirstAreaDevices.get(0);
        Assert.assertNotEquals(0, areaDevice1.getId());
        Assert.assertEquals(hdevice1.getId(), areaDevice1.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        AreaDevice areaDevice2 = listFirstAreaDevices.get(1);
        Assert.assertNotEquals(0, areaDevice2.getId());
        Assert.assertEquals(hdevice2.getId(), areaDevice2.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice2.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice2.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice2.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice2.getId(), ad2.getId());
        Assert.assertEquals(areaDevice2.getDevice().getId(), ad2.getDevice().getId());
        Assert.assertEquals(areaDevice2.getArea().getId(), ad2.getArea().getId());

        AreaDevice areaDevice3 = listFirstAreaDevices.get(2);
        Assert.assertNotEquals(0, areaDevice3.getId());
        Assert.assertEquals(hdevice3.getId(), areaDevice3.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice3.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice3.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice3.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice3.getId(), ad3.getId());
        Assert.assertEquals(areaDevice3.getDevice().getId(), ad3.getDevice().getId());
        Assert.assertEquals(areaDevice3.getArea().getId(), ad3.getArea().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseDeleteArea1 = areaRestApi.deleteArea(area1.getId());
        Assert.assertEquals(200, restResponseDeleteArea1.getStatus());
        Assert.assertNull(restResponseDeleteArea1.getEntity());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseAreaDeviceNotFound = areaRestApi.getAreaDeviceList(area1.getId());
        Assert.assertEquals(404, restResponseAreaDeviceNotFound.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseAreaDeviceNotFound.getEntity()).getType());

        // ckecks: devices hasn't been deleted in cascade mode
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction actionFindDevice = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, actionFindDevice);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponseFindDevice1 = hDeviceRestService.findHDevice(hdevice1.getId());
        Assert.assertEquals(200, restResponseFindDevice1.getStatus());
        Assert.assertEquals(hdevice1.getId(), ((HDevice) restResponseFindDevice1.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice1.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((HDevice) restResponseFindDevice1.getEntity()).getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponseFindDevice2 = hDeviceRestService.findHDevice(hdevice2.getId());
        Assert.assertEquals(200, restResponseFindDevice2.getStatus());
        Assert.assertEquals(hdevice2.getId(), ((HDevice) restResponseFindDevice2.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice2.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((HDevice) restResponseFindDevice2.getEntity()).getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponseFindDevice3 = hDeviceRestService.findHDevice(hdevice3.getId());
        Assert.assertEquals(200, restResponseFindDevice3.getStatus());
        Assert.assertEquals(hdevice3.getId(), ((HDevice) restResponseFindDevice3.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice3.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((HDevice) restResponseFindDevice3.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test105_deleteDeviceAssociatedWithAreaDeviceShouldCascade() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, tries to remove device associated with an AreaDevice;
        // this call not deletes device because referential integrity constraint is violated
        // response status code '500' javax.persistence.PersistenceException
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

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        AreaDevice ad1 = createAreaDevice(area1, hdevice1);
        Assert.assertNotEquals(0, ad1.getId());
        Assert.assertEquals(hdevice1.getId(), ad1.getDevice().getId());
        Assert.assertEquals(huser.getId(), ad1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), ad1.getArea().getId());
        Assert.assertEquals(huser.getId(), ad1.getArea().getProject().getUser().getId());

        //checks if AreaDevice exists
        this.impersonateUser(areaRestApi, huser);
        Response restResponseAreaDevice = areaRestApi.getAreaDeviceList(area1.getId());
        List<AreaDevice> listFirstAreaDevices = restResponseAreaDevice.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertEquals(1, listFirstAreaDevices.size());
        Assert.assertFalse(listFirstAreaDevices.isEmpty());
        Assert.assertEquals(200, restResponseAreaDevice.getStatus());

        AreaDevice areaDevice1 = listFirstAreaDevices.get(0);
        Assert.assertNotEquals(0, areaDevice1.getId());
        Assert.assertEquals(hdevice1.getId(), areaDevice1.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(area1.getId(), areaDevice1.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice1.getArea().getProject().getUser().getId());

        Assert.assertEquals(areaDevice1.getId(), ad1.getId());
        Assert.assertEquals(areaDevice1.getDevice().getId(), ad1.getDevice().getId());
        Assert.assertEquals(areaDevice1.getArea().getId(), ad1.getArea().getId());

        // ckecks: devices hasn't been deleted in cascade mode
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction actionFindDevice = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.REMOVE);
        addPermission(huser, actionFindDevice);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponseDeleteDevice1 = hDeviceRestService.deleteHDevice(hdevice1.getId());
        Assert.assertEquals(200, restResponseDeleteDevice1.getStatus());
    }


    @Test
    public void test106_removeAreaDeviceNotDeleteInCascadeModeAreaOrDevice() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with permission, remove an AreaDevice with the following call removeAreaDevice;
        // this call not removes in cascade mode Area or HDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTAreaAction.AREA_DEVICE_MANAGER);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertEquals(area.getProject().getId(), hdevice.getProject().getId());

        AreaDevice areaDevice = createAreaDevice(area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(area.getId(), areaDevice.getArea().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getId(), areaDevice.getDevice().getId());
        Assert.assertEquals(huser.getId(), areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());

        // checks if Area exists
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action1);
        this.impersonateUser(areaRestApi, huser);
        Response restResponseArea = areaRestApi.findArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(area.getId(), ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());

        // checks if HDevice exists
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponseFindDevice = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseFindDevice.getStatus());
        Assert.assertEquals(hdevice.getId(), ((HDevice) restResponseFindDevice.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseFindDevice.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((HDevice) restResponseFindDevice.getEntity()).getProject().getUser().getId());
    }


    @Test
    public void test107_findAllAreaWithPermissionShouldWorkIfListIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, find all Area with the following call findAllArea
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllArea();
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertTrue(listAreas.isEmpty());
        Assert.assertEquals(0, listAreas.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test108_findAllAreaPaginatedWithPermissionShouldWorkIfListIsEmpty() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all areas with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(areaRestApi, huser);
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
            Assert.assertEquals(areaResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
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
                permission.setName(areaResourceName + " assigned to huser_id " + huser.getId());
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
        role.setName("Role" + UUID.randomUUID());
        role.setDescription("Description");
        Response restResponse = roleRestApi.saveRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Role) restResponse.getEntity()).getId());
        Assert.assertEquals(role.getName(), ((Role) restResponse.getEntity()).getName());
        Assert.assertEquals(role.getDescription(), ((Role) restResponse.getEntity()).getDescription());
        return role;
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
        hproject.setName("Project " + UUID.randomUUID());
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
        return hproject;
    }


    private Area createArea(HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HUser ownerHUser = hproject.getUser();
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Area of user: " + hproject.getUser().getUsername());
        area.setProject(hproject);
        area.setAreaViewType(AreaViewType.IMAGE);
        addDefaultPermission(ownerHUser);
        this.impersonateUser(areaRestApi, ownerHUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals("Area of user: " + hproject.getUser().getUsername(), ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        return area;
    }


    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HUser ownerHUser = hproject.getUser();
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Property of: " + ownerHUser.getUsername());
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
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
        Assert.assertEquals("Property of: " + ownerHUser.getUsername(),
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


    private AreaDevice createAreaDevice(Area area, HDevice hdevice) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HUser ownerHUser = area.getProject().getUser();
        Assert.assertEquals(ownerHUser.getId(), hdevice.getProject().getUser().getId());
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
        return areaDevice;
    }


    private Area setNewAreaImage(HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Area area = createArea(hproject);
        HUser ownerHUser = hproject.getUser();
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), area.getProject().getUser().getId());
        Assert.assertNull(area.getImagePath());
        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        FileInputStream imageFile = HyperIoTAreaTestUtil.getImageAttachment();
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        addDefaultPermission(ownerHUser);
        this.impersonateUser(areaRestApi, ownerHUser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNotNull(((Area) restResponseImage.getEntity()).getImagePath());

        String newImageName = String.valueOf(area.getId()).concat("_img.").concat(fileExtension);
        Assert.assertTrue(((Area) restResponseImage.getEntity()).getImagePath().contains(newImageName));
        return (Area) restResponseImage.getEntity();
    }

    // Area is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;
    private HUser huser2;

    @After
    public void afterTest() {
        HyperIoTAreaTestUtil.eraseDatabase(this);
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
        Assert.assertEquals(areaResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }

    private Role getDefaultRole() {
        RoleRepository roleRepository = getOsgiService(RoleRepository.class);
        return roleRepository.findByName("RegisteredUser");
    }

    private void addDefaultPermission(HUser huser) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role defaultRole = getDefaultRole();
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
        Role defaultRole = getDefaultRole();
        huser.addRole(defaultRole);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.deleteUserRole(defaultRole.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(defaultRole));
    }

}

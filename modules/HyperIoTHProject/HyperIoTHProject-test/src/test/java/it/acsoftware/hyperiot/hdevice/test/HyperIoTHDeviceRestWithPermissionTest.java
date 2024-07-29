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

package it.acsoftware.hyperiot.hdevice.test;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.model.HyperIoTValidationError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.actions.HyperIoTHDeviceAction;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
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
import java.util.*;

import static it.acsoftware.hyperiot.hdevice.test.HyperIoTHDeviceConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for HDevice System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHDeviceRestWithPermissionTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
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
    public void test01_hdeviceModuleShouldWorkIfLogged() {
        HDeviceRestApi hdeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call checkModuleWorking checks if HDevice module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hdeviceRestService, adminUser);
        Response restResponse = hdeviceRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HDevice Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_hdeviceModuleShouldWorkIfNotLogged() {
        HDeviceRestApi hdeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call checkModuleWorking checks if HDevice module working
        // correctly
        this.impersonateUser(hdeviceRestService, null);
        Response restResponse = hdeviceRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HDevice Module works!", restResponse.getEntity());
    }

    @Test
    public void test03_saveHDeviceWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, save HDevice with the following call saveHDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals("Brand",
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals("Description",
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals("1.0",
                ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals("model",
                ((HDevice) restResponse.getEntity()).getModel());
        Assert.assertEquals("1.0",
                ((HDevice) restResponse.getEntity()).getSoftwareVersion());
        Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
        Assert.assertEquals(hproject.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
    }

    @Test
    public void test04_saveHDeviceWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to save HDevice with the following call
        // saveHDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test05_updateHDeviceWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, update HDevice with the following call updateHDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        Date date = new Date();
        hdevice.setDescription("Description edited in date: " + date);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Description edited in date: " + date,
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hdevice.getEntityVersion() + 1,
                ((HDevice) restResponse.getEntity()).getEntityVersion());
    }

    @Test
    public void test06_updateHDeviceWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to update HDevice with the following call
        // updateHDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setDescription("just a description");
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test07_findHDeviceWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, find HDevice with the following call findHDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hdevice.getDeviceName(), ((HDevice) restResponse.getEntity()).getDeviceName());
    }

    @Test
    public void test08_findHDeviceWithPermissionShouldFailIfEntityNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to find HDevice with the following call
        // findPermission, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findHDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findHDeviceWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to find HDevice with the following call
        // findHDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_findHDeviceWithoutPermissionShouldFailAndEntityNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to find Permission not found with the
        // following call findHDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findHDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test11_findAllHDeviceWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, find all HDevice with the following call
        // findAllHDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevice();
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(1, listHDevice.size());
        boolean hdeviceFound = false;
        for (HDevice device : listHDevice) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test12_findAllHDeviceWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to find all HDevice with the following
        // call findAllHDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevice();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_deleteHDeviceWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, delete HDevice with the following call deleteHDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test14_deleteHDeviceWithPermissionShouldFailIfEntityNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to delete HDevice with the following call
        // deletePermission, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.deleteHDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test15_deleteHDeviceWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to delete HDevice with the following call
        // deleteHDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.deleteHDevice(hdevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_deleteHDeviceWithoutPermissionShouldFailAndEntityNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to delete Permission not found with the
        // following call deleteHDevice
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.deleteHDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test17_saveHDeviceWithPermissionShouldFailIfBrandIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but brand is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("javascript:");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-brand", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getBrand(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test18_saveHDeviceWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("</script>");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test19_saveHDeviceWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        int maxDescription = 3001;
        hdevice.setDescription(testMaxDescription(maxDescription));
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals(maxDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }

    @Test
    public void test20_saveHDeviceWithPermissionShouldFailIfDeviceNameIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but device name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("");
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test21_saveHDeviceWithPermissionShouldFailIfDeviceNameIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but device name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName(null);
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test22_saveHDeviceWithPermissionShouldFailIfDeviceNameIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but device name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("</script>");
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test23_saveHDeviceWithPermissionShouldFailIfDeviceNameIsMalformed() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but device name is malformed
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName&&&&&");
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getDeviceName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test24_saveHDeviceWithPermissionShouldFailIfFirmwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but firmware is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("</script>");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-firmwareversion", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getFirmwareVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test25_saveHDeviceWithPermissionShouldFailIfSoftwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but software is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("</script>");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-softwareversion", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getSoftwareVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test26_saveHDeviceWithPermissionShouldFailIfModelIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but model is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("</script>");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-model", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getModel(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test27_saveHDeviceWithPermissionShouldFailIfPasswordIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but password is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword(null);
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test28_saveHDeviceWithPermissionShouldFailIfPasswordIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but password is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test29_saveHDeviceWithPermissionShouldFailIfPasswordIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but password is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("vbscript:");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(5, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test30_saveHDeviceWithPermissionShouldFailIfPasswordIsMalformed() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but password is malformed
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordMalformed");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test31_saveHDeviceWithPermissionShouldFailIfPasswordConfirmIsMalformed() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but passwordConfirm is malformed
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordMalformed");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test32_saveHDeviceWithPermissionShouldFailIfIfPasswordAndPasswordConfirmAreDifferent() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but passwordConfirm is not equals to password
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("pAßß0rdPAßß&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }


    @Test
    public void test33_updateHDeviceWithPermissionShouldFailIfBrandIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but brand is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setBrand("vbscript:");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-brand", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getBrand(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test34_updateHDeviceWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setDescription("javascript:");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test35_updateHDeviceWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        int maxDescription = 3001;
        hdevice.setDescription(testMaxDescription(maxDescription));
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals(maxDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }

    @Test
    public void test36_updateHDeviceWithPermissionShouldFailIfDeviceNameIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but device name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(HDevice.class.getName(),
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setDeviceName("");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test37_updateHDeviceWithPermissionShouldFailIfDeviceNameIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but device name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setDeviceName(null);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test38_updateHDeviceWithPermissionShouldFailIfDeviceNameIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but device name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setDeviceName("</script>");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test39_updateHDeviceWithPermissionShouldFailIfDeviceNameIsMalformed() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but device name is malformed
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setDeviceName("deviceName&&&&&");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-devicename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getDeviceName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test40_updateHDeviceWithPermissionShouldFailIfFirmwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but firmware is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setFirmwareVersion("</script>");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-firmwareversion", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getFirmwareVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test41_updateHDeviceWithPermissionShouldFailIfSoftwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but software is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setSoftwareVersion("javascript:");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-softwareversion", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getSoftwareVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test42_updateHDeviceWithPermissionShouldFailIfModelIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but model is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setModel("</script>");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-model", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getModel(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test43_updateHDeviceWithPermissionShouldFailIfTryToChangePassword() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update password HDevice but fails. Password
        // can be changed with updateHDevicePassword method
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, hdevice.getPassword()));
        String hdeviceNewPassword = hdeviceOldPassword.concat("!!!");
        hdevice.setPassword(hdeviceNewPassword);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        HDevice hdeviceResponse = ((HDevice) restResponse.getEntity());
        Assert.assertNotNull(hdeviceResponse);
        Assert.assertEquals(hdeviceResponse.getId(), hdevice.getId());
        Assert.assertFalse(HyperIoTUtil.passwordMatches(hdeviceNewPassword, hdeviceResponse.getPassword()));
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, hdeviceResponse.getPassword()));
    }

    @Test
    public void test44_changePasswordHDeviceWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, changes password of HDevice with the following call
        // updateHDevicePassword
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(oldPassword, hdevice.getPassword()));
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertTrue(HyperIoTUtil.passwordMatches(newPassword, ((HDevice) restResponse.getEntity()).getPassword()));
        Assert.assertTrue(HyperIoTUtil.passwordMatches(passwordConfirm, ((HDevice) restResponse.getEntity()).getPasswordConfirm()));
    }

    @Test
    public void test45_changePasswordHDeviceWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to change password of HDevice with the
        // following call updateHDevicePassword
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test46_changePasswordHDeviceWithPermissionShouldFailIfDeviceNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but HDevice not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(0, oldPassword, newPassword, passwordConfirm);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test47_changePasswordHDeviceWithPermissionShouldFailIfOldPasswordIsMalformed() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but oldPassword is malformed
        // response status code '500' HyperIoTRuntimeException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "wrongPass";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }

    @Test
    public void test48_changePasswordHDeviceWithPermissionShouldFailIfOldPasswordIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but oldPassword is null
        // response status code '500' HyperIoTRuntimeException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), null, newPassword,
                passwordConfirm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }

    @Test
    public void test49_changePasswordHDeviceWithPermissionShouldFailIfNewPasswordIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but newPassword is null
        // response status code '500' HyperIoTRuntimeException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, null,
                passwordConfirm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }

    @Test
    public void test50_changePasswordHDeviceWithPermissionShouldFailIfNewPasswordIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but newPassword is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test51_changePasswordHDeviceWithPermissionShouldFailIfNewPasswordIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but newPassword is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "eval(malicious code)";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(5, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        boolean passwordIsMaliciousCode = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        List<HyperIoTValidationError> validationError = ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors();
        for (HyperIoTValidationError error : validationError) {
            if (error.getField() != null && error.getField().equals("hdevice-password") && error.getInvalidValue() != null && error.getInvalidValue().equals("eval(malicious code)")) {
                passwordIsMaliciousCode = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
        Assert.assertTrue(passwordIsMaliciousCode);
    }

    @Test
    public void test52_changePasswordHDeviceWithPermissionShouldFailIfPasswordConfirmIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but passwordConfirm is null
        // response status code '500' HyperIoTRuntimeException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                null);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }

    @Test
    public void test53_changePasswordHDeviceWithPermissionShouldFailIfPasswordConfirmIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but passwordConfirm is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test54_changePasswordHDeviceWithPermissionShouldFailIfPasswordConfirmIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but passwordConfirm is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "javascript:";
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean invalidPassword = false;
        boolean invalidPasswordConfirm = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-password")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-password", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPassword = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("hdevice-passwordConfirm")) {
                Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().isEmpty());
                Assert.assertEquals("hdevice-passwordConfirm", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
                invalidPasswordConfirm = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
    }

    @Test
    public void test55_updateHDeviceWithPermissionShouldFailIfHProjectIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to change password of HDevice with the
        // following call updateHDevicePassword, but HProject is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setProject(null);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-project", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test56_saveHDeviceWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the
        // following call saveHDevice, but HProject belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description ");
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model ");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        huser2 = createHUser(null);
        HProject differentHProject = createHProject(huser2);
        Assert.assertNotEquals(0, differentHProject.getId());
        Assert.assertEquals(huser2.getId(), differentHProject.getUser().getId());
        Assert.assertNotEquals(huser.getId(), differentHProject.getUser().getId());

        hdevice.setProject(differentHProject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertNotEquals(huser.getId(), hdevice.getProject().getUser().getId());
        Assert.assertEquals(hdevice.getProject().getUser().getId(), huser2.getId());
    }


    @Test
    public void test57_saveHDeviceWithPermissionShouldFailIfScreenNameAlreadyExists() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but screenName already exists
        // response status code '422' HyperIoTScreenNameAlreadyExistsException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdeviceScreenNameDuplicate = new HDevice();
        hdeviceScreenNameDuplicate.setBrand("brand");
        hdeviceScreenNameDuplicate.setDescription("description");
        hdeviceScreenNameDuplicate.setDeviceName(hdevice1.getDeviceName());
        hdeviceScreenNameDuplicate.setFirmwareVersion("1.");
        hdeviceScreenNameDuplicate.setModel("model");
        hdeviceScreenNameDuplicate.setPassword("passwordPass&01");
        hdeviceScreenNameDuplicate.setPasswordConfirm("passwordPass&01");
        hdeviceScreenNameDuplicate.setSoftwareVersion("1.");
        hdeviceScreenNameDuplicate.setAdmin(false);
        HProject hproject2 = createHProject(huser);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser.getId(), hproject2.getUser().getId());
        hdeviceScreenNameDuplicate.setProject(hproject2);

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(hdeviceScreenNameDuplicate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTScreenNameAlreadyExistsException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("deviceName", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice1.getDeviceName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test58_updateHDeviceWithPermissionShouldFailIfEntityIsDuplicated() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call
        // updateHDevice, but screenName already exists
        // response status code '422' HyperIoTScreenNameAlreadyExistsException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        HDevice duplicateHDevice = createHDevice(hproject);
        Assert.assertNotEquals(0, duplicateHDevice.getId());
        Assert.assertEquals(huser.getId(), duplicateHDevice.getProject().getUser().getId());

        duplicateHDevice.setDeviceName(hdevice.getDeviceName());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(duplicateHDevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTScreenNameAlreadyExistsException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("deviceName", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(duplicateHDevice.getDeviceName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test59_findAllHDeviceByProjectIdWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, finds list of all available HDevice for the given project id
        // with the following call getProjectDevicesList
        // response status code 200
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(1, listHDevice.size());
        boolean hdeviceFound = false;
        for (HDevice device : listHDevice) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test60_findAllHDeviceByProjectIdWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to find list of all available HDevice for
        // the given project id with the following call getProjectDevicesList
        // response status code 403 HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test61_findAllHDeviceByProjectIdWithPermissionShouldWorkIfListHDeviceIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, finds list of all available HDevice for the given project id
        // with the following call getProjectDevicesList, listHDevice is empty
        // response status code 200
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertTrue(listHDevice.isEmpty());
        Assert.assertEquals(0, listHDevice.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test62_findAllHDeviceByProjectIdWithoutPermissionShouldFailAndListHDeviceIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to find list of all available HDevice for
        // the given project id with the following call getProjectDevicesList
        // response status code 403 HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        removeDefaultPermission(huser);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test63_findAllHDeviceByProjectIdWithPermissionShouldFailIfHProjectNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to find list of all available HDevice for the given project id
        // with the following call getProjectDevicesList, but HProject not found
        // response status code 404 HyperIoTNoResultException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        huser = createHUser(action);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTNoResultException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test64_findAllHDeviceByProjectIdWithoutPermissionShouldFailAndHProjectNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, without permission, tries to find list of all available HDevice for the given project not found
        // with the following call getProjectDevicesList
        // response status code 404 HyperIoTNoResultException
        huser = createHUser(null);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTNoResultException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test65_updateHDeviceWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the
        // following call updateHDevice, but HProject belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        huser2 = createHUser(null);
        HProject differentHProject = createHProject(huser2);

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), differentHProject.getUser().getId());
        Assert.assertEquals(huser2.getId(), differentHProject.getUser().getId());

        HProject hproject = createHProject(huser);
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        hdevice.setProject(differentHProject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertNotEquals(huser.getId(), hdevice.getProject().getUser().getId());
        Assert.assertEquals(hdevice.getProject().getUser().getId(), huser2.getId());
    }


    @Test
    public void test66_updateHDeviceWithPermissionShouldFailIfEntityNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to update HDevice with the following call updateHDevice,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HDevice hdevice = new HDevice();
        hdevice.setDescription("description");
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test67_saveHDeviceWithPermissionShouldFailIfEntityIsDuplicated() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with permission, tries to save HDevice with the following call
        // saveHDevice, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice duplicateHDevice = new HDevice();
        duplicateHDevice.setBrand("brand");
        duplicateHDevice.setDescription("description");
        duplicateHDevice.setDeviceName(hdevice1.getDeviceName()); //same name
        duplicateHDevice.setFirmwareVersion("1.");
        duplicateHDevice.setModel("model");
        duplicateHDevice.setPassword("passwordPass&01");
        duplicateHDevice.setPasswordConfirm("passwordPass&01");
        duplicateHDevice.setSoftwareVersion("1.");
        duplicateHDevice.setAdmin(false);
        duplicateHDevice.setProject(hproject); //same hproject

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.saveHDevice(duplicateHDevice);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertEquals("deviceName", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
    }


    @Test
    public void test68_findAllHDevicePaginatedWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all
        // HProjects with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 5;
        int page = 1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            Assert.assertEquals(huser.getId(), hDevice.getProject().getUser().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(delta, devices.size());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(delta, listHDevices.getResults().size());
        Assert.assertEquals(delta, listHDevices.getDelta());
        Assert.assertEquals(page, listHDevices.getCurrentPage());
        Assert.assertEquals(page, listHDevices.getNextPage());
        // delta is 5, page is 1: 5 entities stored in database
        Assert.assertEquals(1, listHDevices.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test69_findAllHDevicePaginatedWithoutPermissionShouldFail() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser without permission
        // tries to find all HDevices with pagination
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test70_findAllHDevicePaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all HDevices with pagination
        // if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            Assert.assertEquals(huser.getId(), hDevice.getProject().getUser().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(defaultDelta, devices.size());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHDevices.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevices.getDelta());
        Assert.assertEquals(defaultPage, listHDevices.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevices.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listHDevices.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test71_findAllHDevicePaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all HDevices with pagination
        // if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = -1;
        int page = 1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<HDevice> devices = new ArrayList<>();
        int numbEntities = 7;
        for (int i = 0; i < numbEntities; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            Assert.assertEquals(huser.getId(), hDevice.getProject().getUser().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(numbEntities, devices.size());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(7, listHDevices.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevices.getDelta());
        Assert.assertEquals(page, listHDevices.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevices.getNextPage());
        // default delta is 10, page is 1: 7 entities stored in database
        Assert.assertEquals(1, listHDevices.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test72_findAllHDevicePaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all HDevices with pagination
        // if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 0;
        int page = 1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<HDevice> devices = new ArrayList<>();
        int numbEntities = 4;
        for (int i = 0; i < numbEntities; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(numbEntities, devices.size());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listHDevices.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevices.getDelta());
        Assert.assertEquals(page, listHDevices.getCurrentPage());
        Assert.assertEquals(page, listHDevices.getNextPage());
        // default delta is 10, page is 1: 4 entities stored in database
        Assert.assertEquals(1, listHDevices.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test73_findAllHDevicePaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all HDevices with pagination
        // if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 5;
        int page = -1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(delta, devices.size());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(delta, listHDevices.getResults().size());
        Assert.assertEquals(delta, listHDevices.getDelta());
        Assert.assertEquals(defaultPage, listHDevices.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevices.getNextPage());
        // delta is 5, default page is 1: 5 entities stored in database
        Assert.assertEquals(1, listHDevices.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test74_findAllHDevicePaginatedWithPermissionShouldWorkIfPageIsZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all HDevices with pagination
        // if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 5;
        int page = 0;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(delta, devices.size());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(delta, listHDevices.getResults().size());
        Assert.assertEquals(delta, listHDevices.getDelta());
        Assert.assertEquals(defaultPage, listHDevices.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevices.getNextPage());
        // delta is 5, default page is 1: 5 entities stored in database
        Assert.assertEquals(1, listHDevices.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test75_deleteHDeviceWithPermissionNotDeleteInCascadeHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser deletes devices, with HProject associated, with the following call deleteHDevice,
        // hproject is not deleted in cascade mode
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());

        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());
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
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
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
        Response restResponseDeleteDevice1 = hDeviceRestApi.deleteHDevice(hdevice1.getId());
        Assert.assertEquals(200, restResponseDeleteDevice1.getStatus());
        Assert.assertNull(restResponseDeleteDevice1.getEntity());
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseDeleteDevice2 = hDeviceRestApi.deleteHDevice(hdevice2.getId());
        Assert.assertEquals(200, restResponseDeleteDevice2.getStatus());
        Assert.assertNull(restResponseDeleteDevice2.getEntity());

        // checks if hproject is still stored in database
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseFindProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseFindProject.getStatus());

        // hproject hasn't devices
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseFindDeviceByProjectId = hDeviceRestApi.findAllHDeviceByProjectId(hproject.getId());
        Collection<HDevice> listHDevices1 = restResponseFindDeviceByProjectId.readEntity(new GenericType<Collection<HDevice>>() {
        });
        Assert.assertEquals(0, listHDevices1.size());
        Assert.assertTrue(listHDevices1.isEmpty());
        Assert.assertEquals(200, restResponseFindDeviceByProjectId.getStatus());
    }


    @Test
    public void test76_deleteHProjectWithPermissionDeleteInCascadeAllHDevices() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser deletes hproject with the following call deleteHProject,
        // all hdevices has been deleted in cascade mode
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());

        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());
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
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                device2Found = true;
            }
        }
        Assert.assertTrue(device1Found);
        Assert.assertTrue(device2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // deletes hproject with deleteHProject call
        // this calls deletes hdevices in cascade mode
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseDeleteProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteProject.getStatus());
        Assert.assertNull(restResponseDeleteProject.getEntity());

        // checks: hdevices has been deleted in cascade mode with the call deleteHProject
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseFindHDevice1 = hDeviceRestApi.findHDevice(hdevice1.getId());
        Assert.assertEquals(404, restResponseFindHDevice1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindHDevice1.getEntity()).getType());

        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseFindHDevice2 = hDeviceRestApi.findHDevice(hdevice2.getId());
        Assert.assertEquals(404, restResponseFindHDevice2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindHDevice2.getEntity()).getType());
    }


    @Test
    public void test77_deleteHDeviceWithPermissionNotDeleteHProjectButDeleteInCascadeHPackets() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, deletes hdevice with the following call deleteHDevice.
        // This call delete in cascade all hpackets but not deletes hproject
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket1.getId());
        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket2.getId());

        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        //checks if device packets exists
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        addPermission(huser, action1);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseHPacket = hPacketRestApi.getHDevicePacketList(hdevice.getId());
        Collection<HPacket> listDeviceHPackets = restResponseHPacket.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(2, listDeviceHPackets.size());
        Assert.assertFalse(listDeviceHPackets.isEmpty());
        boolean packet1Found = false;
        boolean packet2Found = false;
        for (HPacket packet : listDeviceHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet2Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertEquals(200, restResponseHPacket.getStatus());


        Response restResponsePacketByProject = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        List<HPacket> listPacketByProjects = restResponsePacketByProject.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertEquals(2, listPacketByProjects.size());
        Assert.assertFalse(listPacketByProjects.isEmpty());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        for (HPacket packet : listPacketByProjects) {
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
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertEquals(200, restResponsePacketByProject.getStatus());

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseDeleteDevice = hDeviceRestApi.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseDeleteDevice.getStatus());
        Assert.assertNull(restResponseDeleteDevice.getEntity());


        // checks if hproject is still stored in database
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseFindProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseFindProject.getStatus());

        // checks: hpackets has been deleted in cascade mode with the call deleteHDevice
        HyperIoTAction action3 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action3);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseFindPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseFindPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket1.getEntity()).getType());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseFindPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseFindPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket2.getEntity()).getType());
    }


    @Test
    public void test78_deleteHProjectWithPermissionDeleteInCascadeAllHDeviceAndHPackets() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, deletes hproject with the following call deleteHProject.
        // This call delete in cascade all hdevice and hpackets
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket1.getId());
        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket2.getId());

        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        //checks if device packets exists
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        addPermission(huser, action1);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseHPacket = hPacketRestApi.getHDevicePacketList(hdevice.getId());
        Collection<HPacket> listDeviceHPackets = restResponseHPacket.readEntity(new GenericType<Collection<HPacket>>() {
        });
        Assert.assertEquals(2, listDeviceHPackets.size());
        Assert.assertFalse(listDeviceHPackets.isEmpty());
        boolean packet1Found = false;
        boolean packet2Found = false;
        for (HPacket packet : listDeviceHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                packet2Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertEquals(200, restResponseHPacket.getStatus());


        Response restResponsePacketByProject = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        List<HPacket> listPacketByProjects = restResponsePacketByProject.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertEquals(2, listPacketByProjects.size());
        Assert.assertFalse(listPacketByProjects.isEmpty());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        for (HPacket packet : listPacketByProjects) {
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
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertEquals(200, restResponsePacketByProject.getStatus());

        // deletes hproject: this call deletes all devices and packets
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponseDeleteProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteProject.getStatus());
        Assert.assertNull(restResponseDeleteProject.getEntity());

        // checks: hdevice has been deleted in cascade mode with the call deleteHProject
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseFindDevice = hDeviceRestApi.findHDevice(hdevice.getId());
        Assert.assertEquals(404, restResponseFindDevice.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindDevice.getEntity()).getType());

        // checks: hpackets has been deleted in cascade mode with the call deleteHProject
        HyperIoTAction action3 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action3);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseFindPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseFindPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket1.getEntity()).getType());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseFindPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseFindPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket2.getEntity()).getType());
    }


    // HDevice is Owned Resource: only huser or huser2 is able to find/findAll his entities
    @Test
    public void test79_hadminFindAllHDevicesAssociatedWithAnotherHUserShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin find all HDevice associated with another huser with the following call findAllHDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevice();
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        boolean hdeviceFound = false;
        for (HDevice device : listHDevice) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
        Assert.assertEquals(200, restResponse.getStatus());

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
        Response responseByAdmin = hDeviceRestService.findAllHDevice();
        List<HDevice> listHDevice1 = responseByAdmin.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertTrue(listHDevice1.size() > 0);
        Assert.assertEquals(200, responseByAdmin.getStatus());
    }


    // HDevice is Owned Resource: only huser or huser2 is able to find/findAll his entities
    @Test
    public void test80_hadminFindHDevicesAssociatedWithAnotherHUserShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin find HDevice associated with another huser with the following call findHDevice
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        HDevice hdevice = createHDevice(hproject);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(huser.getId(), ((HDevice) restResponse.getEntity()).getProject().getUser().getId());

        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
        Response responseByAdmin = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, responseByAdmin.getStatus());
    }


    @Test
    public void test81_hadminFindAllHDeviceByProjectIdWithPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin finds list of all available HDevice for the given project id
        // with the following call findAllHDeviceByProjectId
        // response status code 200
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTHProjectAction.DEVICE_LIST);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(1, listHDevice.size());
        boolean hdeviceFound = false;
        for (HDevice device : listHDevice) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
        Assert.assertEquals(200, restResponse.getStatus());


        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
        Response responseByAdmin = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice1 = responseByAdmin.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice1.isEmpty());
        Assert.assertEquals(1, listHDevice1.size());
        boolean hdeviceFound1 = false;
        for (HDevice device : listHDevice1) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                Assert.assertNotEquals(adminUser.getId(), device.getProject().getUser().getId());
                hdeviceFound1 = true;
            }
        }
        Assert.assertTrue(hdeviceFound1);
        Assert.assertEquals(200, responseByAdmin.getStatus());
    }


    @Test
    public void test82_findAllHDevicePaginatedWithPermissionShouldWorkIfListIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all
        // HProjects with pagination
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(defaultDelta, defaultPage);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertTrue(listHDevices.getResults().isEmpty());
        Assert.assertEquals(0, listHDevices.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevices.getDelta());
        Assert.assertEquals(defaultPage, listHDevices.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevices.getNextPage());
        // default delta is 10, default page is 1: there are not entities stored in database
        Assert.assertEquals(0, listHDevices.getNumPages());
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
            Assert.assertEquals(hDeviceResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
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
                permission.setName(hDeviceResourceName + " assigned to huser_id " + huser.getId());
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

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HUser ownerHUser = hproject.getUser();
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Description");
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

    private HPacket createHPacketAndAddHPacketField(HDevice hdevice, boolean createField) {
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
            field1.setName("temperature" + UUID.randomUUID().toString().replaceAll("-", ""));
            field1.setDescription("Temperature");
            field1.setType(HPacketFieldType.DOUBLE);
            field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
            field1.setValue(24.0);

            HPacketField field2 = new HPacketField();
            field2.setPacket(hpacket);
            field2.setName("humidity" + UUID.randomUUID().toString().replaceAll("-", ""));
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

            ArrayList<HPacketField> fields = new ArrayList<>(((HPacket) restResponse.getEntity()).getFields());
            //check restResponse field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() == ((HPacketField) responseAddField1.getEntity()).getId()));
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

            //check restResponse field2 is equals to responseAddField2 field2
            Assert.assertEquals(field2.getId(), ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() == ((HPacketField) responseAddField2.getEntity()).getId()));
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());

            Assert.assertEquals(2, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        return hpacket;
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
        Assert.assertEquals(hDeviceResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }


    // HDevice is Owned Resource: only huser or huser2 is able to find/findAll his entities
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
    }

    public Role getRegisteredUserRole() {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HDeviceRepository hDeviceRepository = getOsgiService(HDeviceRepository.class);

        HDevice hDeviceAdmin = hDeviceRepository.findHDeviceAdmin();
        if (hDeviceAdmin != null) {
            AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
            HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
            this.impersonateUser(hDeviceRestApi, adminUser);
            Response restResponse = hDeviceRestApi.deleteHDevice(hDeviceAdmin.getId());
            Assert.assertEquals(200, restResponse.getStatus());
            Assert.assertNull(restResponse.getEntity());
        }
        // find default Role "RegisteredUser"
        RoleRepository roleRepository = getOsgiService(RoleRepository.class);
        return roleRepository.findByName("RegisteredUser");
    }


    private void addDefaultPermission(HUser huser) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role registeredUserRole = getRegisteredUserRole();
        huser.addRole(registeredUserRole);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(registeredUserRole.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(registeredUserRole));
    }

    private void removeDefaultPermission(HUser huser) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role defaultRole = getRegisteredUserRole();
        huser.addRole(defaultRole);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.deleteUserRole(defaultRole.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(defaultRole));
    }


}

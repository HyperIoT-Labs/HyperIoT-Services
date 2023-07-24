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

import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.model.HyperIoTValidationError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
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
public class HyperIoTHDeviceRestTest extends KarafTestSupport {

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
    public void checkHBaseConnectorInstalled() {
        String packageExports = executeCommand("package:exports | grep it.acsoftware.hbase.connector");
        System.out.println(packageExports);
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
    public void test01_hdeviceModuleShouldWork() {
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
    public void test03_saveHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin save HDevice with the following call saveHDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
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
        Assert.assertEquals(adminUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
    }

    @Test
    public void test03_1_saveHDeviceShouldPersistHDeviceEntityWithNullPasswordResetCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin save HDevice with the following call saveHDevice
        // hadmin save HDevice with password reset code not null
        // response status code '200'
        //Assert that password reset code is set to null
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        String pwdResetCode = "pwdResetCode";
        hdevice.setPasswordResetCode(pwdResetCode);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
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
        Assert.assertEquals(adminUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        //Assert password reset code is null on returned object
        Assert.assertNull(((HDevice) restResponse.getEntity()).getProject().getUser().getPasswordResetCode());
        //Assert that persisted entity has null password reset code .
        HDeviceRepository hDeviceRepository = getOsgiService(HDeviceRepository.class);
        HDevice persistedDevice = hDeviceRepository.find(((HDevice) restResponse.getEntity()).getId(), null);
        Assert.assertNull(persistedDevice.getPasswordResetCode());
    }

    @Test
    public void test04_saveHDeviceShouldFailIfNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call tries to save HDevice, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, null);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test05_findAllHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin find all HDevice with the following call findAllHDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findAllHDevice();
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(1, listHDevice.size());
        boolean hdeviceFound = false;
        for (HDevice device : listHDevice) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test06_findAllHDeviceShouldFailIfNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call tries to find all HDevice, but HUser is not logged
        // response status code '403'
        this.impersonateUser(hDeviceRestService, null);
        Response restResponse = hDeviceRestService.findAllHDevice();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test07_findHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin find all HDevice with the following call findHDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hdevice.getDeviceName(), ((HDevice) restResponse.getEntity()).getDeviceName());
    }

    @Test
    public void test08_findHDeviceShouldFailIfNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call tries to find HDevice, but HUser is not logged
        // response status code '403'
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, null);
        Response restResponse = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_updateHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin updates HDevice name with the following call updateHDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, adminUser);
        hdevice.setFirmwareVersion("2.0");
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("2.0", ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals(hdevice.getEntityVersion() + 1,
                ((HDevice) restResponse.getEntity()).getEntityVersion());
    }

    @Test
    public void test09_1_updateHDeviceShouldNotUpdateHDevicePasswordResetCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin updates HDevice name with the following call updateHDevice
        // hadmin update HDevice with password reset code not null
        // response status code '200'
        // Assert that password reset code is set to null
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, adminUser);
        String pwdResetCode = "pwdResetCode";
        hdevice.setPasswordResetCode(pwdResetCode);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hdevice.getId(), ((HDevice) restResponse.getEntity()).getId());
        //Assert password reset code is null on returned object
        Assert.assertNull(((HDevice) restResponse.getEntity()).getProject().getUser().getPasswordResetCode());
        //Assert that persisted entity has null password reset code .
        HDeviceRepository hDeviceRepository = getOsgiService(HDeviceRepository.class);
        HDevice persistedDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNull(persistedDevice.getPasswordResetCode());
    }

    @Test
    public void test10_updateHDeviceShouldFailIfNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call tries to update HDevice, but HUser is not logged
        // response status code '403'
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, null);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test11_deleteHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin delete HDevice name with the following call deleteHDevice
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test12_deleteHDeviceShouldFailIfNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // this call tries to delete HDevice, but HUser is not logged
        // response status code '403'
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, null);
        Response restResponse = hDeviceRestService.deleteHDevice(hdevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test13_saveHDeviceShouldFailIfBrandIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // brand is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("</script>");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model ");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test14_saveHDeviceShouldFailIfDescriptionIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("javascript:");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test15_saveHDeviceShouldFailIfMaxDescriptionIsOver3000Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        int maxDescription = 3001;
        hdevice.setDescription(testMaxDescription(maxDescription));
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model ");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test16_saveHDeviceShouldFailIfDeviceNameIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // device name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test17_saveHDeviceShouldFailIfDeviceNameIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // device name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test18_saveHDeviceShouldFailIfDeviceNameIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // device name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test19_saveHDeviceShouldFailIfDeviceNameIsMalformed() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // device name is malformed
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test20_saveHDeviceShouldFailIfFirmwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // firmware is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("javascript:");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test21_saveHDeviceShouldFailIfSoftwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // software is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("</script>");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test22_saveHDeviceShouldFailIfModelIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // model is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("javascript:");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test23_saveHDeviceShouldFailIfPasswordIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // password is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword(null);
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test24_saveHDeviceShouldFailIfPasswordIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // password is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test25_saveHDeviceShouldFailIfPasswordIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // password is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("javascript:");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test26_saveHDeviceShouldFailIfPasswordConfirmIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // passwordConfirm is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm(null);
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test27_saveHDeviceShouldFailIfPasswordConfirmIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // passwordConfirm is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test28_saveHDeviceShouldFailIfPasswordConfirmIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // passwordConfirm is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("javascript:");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test29_saveHDeviceShouldFailIfHProjectIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // HProject is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(null);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-project", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test30_updateHDeviceShouldFailIfBrandIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // brand is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setBrand("</script>");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test31_updateHDeviceShouldFailIfDescriptionIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setDescription("</script>");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test32_updateHDeviceShouldFailIfMaxDescriptionIsOver3000Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        int maxDescription = 3001;
        hdevice.setDescription(testMaxDescription(maxDescription));
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test33_updateHDeviceShouldFailIfDeviceNameIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // device name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setDeviceName(null);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test34_updateHDeviceShouldFailIfDeviceNameIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // device name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setDeviceName("");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test35_updateHDeviceShouldFailIfDeviceNameIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // device name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setDeviceName("<script>console.log()</script>");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test36_updateHDeviceShouldFailIfDeviceNameIsMalformed() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // device name is malformed
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setDeviceName("device&&&&&");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test37_updateHDeviceShouldFailIfFirmwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // firmware is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setFirmwareVersion("</script>");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test38_updateHDeviceShouldFailIfSoftwareIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // software is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setSoftwareVersion("</script>");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test39_updateHDeviceShouldFailIfModelIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // model is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setModel("</script>");
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test40_updateHDeviceShouldNotUpdateHDevicePassword() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to change the HUser password specify a new Password with the following call updateHDevice
        // Assert that updateHDevice works (Response status code '200' OK)
        // Assert that device's password not change ( HUser can change password only with updateHDevicePassword/resetPassword rest service).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, hdevice.getPassword()));
        String hdeviceNewPassword = hdeviceOldPassword.concat("!!!");
        hdevice.setPassword(hdeviceNewPassword);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        HDevice hdeviceResponse = ((HDevice) restResponse.getEntity());
        Assert.assertNotNull(hdeviceResponse);
        Assert.assertEquals(hdeviceResponse.getId(), hdevice.getId());
        Assert.assertFalse(HyperIoTUtil.passwordMatches(hdeviceNewPassword, hdeviceResponse.getPassword()));
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, hdeviceResponse.getPassword()));
    }

    @Test
    public void test41_changePasswordHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser changes password of HDevice with the following call
        // updateHDevicePassword
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, adminUser);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(oldPassword, hdevice.getPassword()));
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertTrue(HyperIoTUtil.passwordMatches(newPassword, ((HDevice) restResponse.getEntity()).getPassword()));
        Assert.assertTrue(HyperIoTUtil.passwordMatches(passwordConfirm, ((HDevice) restResponse.getEntity()).getPasswordConfirm()));
    }

    @Test
    public void test42_changePasswordHDeviceShouldFailIfDeviceNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but HDevice not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(0, oldPassword, newPassword, passwordConfirm);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test43_changePasswordHDeviceShouldFailIfOldPasswordIsWrong() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but oldPassword is malformed
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "wrongPass";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }

    @Test
    public void test44_changePasswordHDeviceShouldFailIfNewPasswordIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but newPassword is null
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = null;
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }

    @Test
    public void test45_changePasswordHDeviceShouldFailIfNewPasswordIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but newPassword is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test46_changePasswordHDeviceShouldFailIfNewPasswordIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but newPassword is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "javascript:";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(hDeviceRestService, adminUser);
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
            if (error.getField() != null && error.getField().equals("hdevice-password") && error.getInvalidValue() != null && error.getInvalidValue().equals("javascript:")) {
                passwordIsMaliciousCode = true;
            }
        }
        Assert.assertTrue(invalidPassword);
        Assert.assertTrue(invalidPasswordConfirm);
        Assert.assertTrue(passwordIsMaliciousCode);
    }

    @Test
    public void test47_changePasswordHDeviceShouldFailIfPasswordConfirmIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but passwordConfirm is null
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevicePassword(hdevice.getId(), oldPassword, newPassword,
                null);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }

    @Test
    public void test48_changePasswordHDeviceShouldFailIfPasswordConfirmIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but passwordConfirm is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "";
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test49_changePasswordHDeviceShouldFailIfPasswordConfirmIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to change password of HDevice, with the following
        // call, but passwordConfirm is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "javascript:";
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test50_updateHDeviceShouldFailIfHProjectIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setProject(null);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hdevice-project", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test51_findHDeviceShouldFailIfEntityNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to find HDevice, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findHDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test52_deleteHDeviceShouldFailIfEntityNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to delete HDevice, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.deleteHDevice(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test53_saveHDeviceShouldFailIfPasswordAndPasswordConfirmAreDifferent() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // password and passwordConfirm are different
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01/Different");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test54_saveHDeviceShouldFailIfScreenNameAlreadyExists() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice,
        // but screenName already exists
        // response status code '422' HyperIoTScreenNameAlreadyExistsException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject1 = createHProject();
        Assert.assertNotEquals(0, hproject1.getId());
        HDevice hdevice1 = createHDevice(hproject1);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject1.getId(), hdevice1.getProject().getId());

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
        HProject hproject2 = createHProject();
        Assert.assertNotEquals(0, hproject2.getId());
        hdeviceScreenNameDuplicate.setProject(hproject2);

        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test55_updateHDeviceShouldFailIfScreenNameAlreadyExists() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice,
        // but screenName already exists
        // response status code '422' HyperIoTScreenNameAlreadyExistsException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HDevice hdeviceScreenNameDuplicate = createHDevice(hproject);
        Assert.assertEquals(hproject.getId(), hdeviceScreenNameDuplicate.getProject().getId());

        hdeviceScreenNameDuplicate.setDeviceName(hdevice.getDeviceName());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevice(hdeviceScreenNameDuplicate);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTScreenNameAlreadyExistsException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("deviceName", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hdevice.getDeviceName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test56_getProjectDevicesListShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin find list of all available HDevice for the given project id
        // with the following call getProjectDevicesList
        // response status code 200
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(1, listHDevice.size());
        boolean hdeviceFound = false;
        for (HDevice device : listHDevice) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test57_getProjectDevicesListShouldWorkIfListHDeviceIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin find list of all available HDevice for the given project id
        // with the following call getProjectDevicesList,
        // list HDevice is empty
        // response status code 200
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertTrue(listHDevice.isEmpty());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test58_getProjectDevicesListShouldFailIfHProjectNotFound() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to find list of all HDevice for the given project id
        // with the following call getProjectDevicesList,
        // but HProject not found
        // response status code 404 HyperIoTNoResultException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTNoResultException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test59_findAllHDevicePaginatedShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, hadmin find all Companies with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = 2;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        List<HDevice> devices = new ArrayList<>();
        int numbEntities = 9;
        for (int i = 0; i < numbEntities; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(numbEntities, devices.size());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(4, listHDevices.getResults().size());
        Assert.assertEquals(delta, listHDevices.getDelta());
        Assert.assertEquals(page, listHDevices.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevices.getNextPage());
        // delta is 5, page is 2: 9 entities stored in database
        Assert.assertEquals(2, listHDevices.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponsePage1 = hDeviceRestService.findAllHDevicePaginated(delta, 1);
        HyperIoTPaginableResult<HDevice> listHDevicesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevicesPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listHDevicesPage1.getResults().size());
        Assert.assertEquals(delta, listHDevicesPage1.getDelta());
        Assert.assertEquals(defaultPage, listHDevicesPage1.getCurrentPage());
        Assert.assertEquals(page, listHDevicesPage1.getNextPage());
        // delta is 5, page is 1: 9 entities stored in database
        Assert.assertEquals(2, listHDevicesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test60_findAllHDevicePaginatedShouldFailIfNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call tries to find all HDevices with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(hDeviceRestService, null);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test61_findAllHDevicePaginatedShouldWorkIfDeltaAndPageAreNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, hadmin find all HDevices with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(defaultDelta, devices.size());
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test62_findAllHDevicePaginatedShouldWorkIfDeltaIsLowerThanZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, hadmin find all HDevices with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 2;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        List<HDevice> devices = new ArrayList<>();
        int numbEntities = 14;
        for (int i = 0; i < numbEntities; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(numbEntities, devices.size());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponsePage2 = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevicesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevicesPage2.getResults().isEmpty());
        Assert.assertEquals(4, listHDevicesPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevicesPage2.getDelta());
        Assert.assertEquals(page, listHDevicesPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevicesPage2.getNextPage());
        // default delta is 10, page is 2: 14 entities stored in database
        Assert.assertEquals(2, listHDevicesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());

        //checks with page = 1
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponsePage1 = hDeviceRestService.findAllHDevicePaginated(delta, 1);
        HyperIoTPaginableResult<HDevice> listHDevicesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevicesPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHDevicesPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevicesPage1.getDelta());
        Assert.assertEquals(defaultPage, listHDevicesPage1.getCurrentPage());
        Assert.assertEquals(page, listHDevicesPage1.getNextPage());
        // default delta is 10, page is 1: 14 entities stored in database
        Assert.assertEquals(2, listHDevicesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test63_findAllHDevicePaginatedShouldWorkIfDeltaIsZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, hadmin find all HDevices with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 3;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        List<HDevice> devices = new ArrayList<>();
        int numbEntities = 21;
        for (int i = 0; i < numbEntities; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(numbEntities, devices.size());
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponsePage3 = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevicesPage3 = restResponsePage3
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevicesPage3.getResults().isEmpty());
        Assert.assertEquals(1, listHDevicesPage3.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevicesPage3.getDelta());
        Assert.assertEquals(page, listHDevicesPage3.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevicesPage3.getNextPage());
        // default delta is 10, page is 3: 21 entities stored in database
        Assert.assertEquals(3, listHDevicesPage3.getNumPages());
        Assert.assertEquals(200, restResponsePage3.getStatus());

        //checks with page = 1
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponsePage1 = hDeviceRestService.findAllHDevicePaginated(delta, 1);
        HyperIoTPaginableResult<HDevice> listHDevicesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevicesPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHDevicesPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevicesPage1.getDelta());
        Assert.assertEquals(defaultPage, listHDevicesPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHDevicesPage1.getNextPage());
        // default delta is 10, page is 1: 21 entities stored in database
        Assert.assertEquals(3, listHDevicesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponsePage2 = hDeviceRestService.findAllHDevicePaginated(delta, 2);
        HyperIoTPaginableResult<HDevice> listHDevicesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevicesPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHDevicesPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHDevicesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHDevicesPage2.getCurrentPage());
        Assert.assertEquals(page, listHDevicesPage2.getNextPage());
        // default delta is 10, page is 2: 21 entities stored in database
        Assert.assertEquals(3, listHDevicesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test64_findAllHDevicePaginatedShouldWorkIfPageIsLowerThanZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, hadmin find all HDevices with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = -1;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(delta, devices.size());
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test65_findAllHDevicePaginatedShouldWorkIfPageIsZero() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, hadmin find all HDevices with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = 0;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HDevice hDevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hDevice.getId());
            Assert.assertEquals(hproject.getId(), hDevice.getProject().getId());
            devices.add(hDevice);
        }
        Assert.assertEquals(delta, devices.size());
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test66_saveHDeviceShouldFailIfEntityIsDuplicated() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HDevice duplicateHDevice = new HDevice();
        duplicateHDevice.setBrand("brand");
        duplicateHDevice.setDescription("description");
        duplicateHDevice.setDeviceName(hdevice.getDeviceName());
        duplicateHDevice.setFirmwareVersion("1.");
        duplicateHDevice.setModel("model");
        duplicateHDevice.setPassword("passwordPass&01");
        duplicateHDevice.setPasswordConfirm("passwordPass&01");
        duplicateHDevice.setSoftwareVersion("1.");
        duplicateHDevice.setAdmin(false);
        duplicateHDevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.saveHDevice(duplicateHDevice);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertEquals("deviceName", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
    }


    @Test
    public void test67_deleteHDeviceNotDeleteInCascadeHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes devices with the following call deleteHDevice,
        // hproject is not deleted in cascade mode
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());

        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());
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
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
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
        Response restResponseDeleteDevice1 = hDeviceRestApi.deleteHDevice(hdevice1.getId());
        Assert.assertEquals(200, restResponseDeleteDevice1.getStatus());
        Assert.assertNull(restResponseDeleteDevice1.getEntity());
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseDeleteDevice2 = hDeviceRestApi.deleteHDevice(hdevice2.getId());
        Assert.assertEquals(200, restResponseDeleteDevice2.getStatus());
        Assert.assertNull(restResponseDeleteDevice2.getEntity());

        // checks if hproject is still stored in database
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseFindProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseFindProject.getStatus());

        // hproject hasn't devices
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseFindDeviceByProjectId = hDeviceRestApi.findAllHDeviceByProjectId(hproject.getId());
        Collection<HDevice> listHDevices1 = restResponseFindDeviceByProjectId.readEntity(new GenericType<Collection<HDevice>>() {
        });
        Assert.assertEquals(0, listHDevices1.size());
        Assert.assertTrue(listHDevices1.isEmpty());
        Assert.assertEquals(200, restResponseFindDeviceByProjectId.getStatus());
    }


    @Test
    public void test68_deleteHProjectDeleteInCascadeAllHDevices() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin deletes hproject with the following call deleteHProject,
        // all hdevices has been deleted in cascade mode
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());

        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Assert.assertEquals(adminUser.getId(), hdevice1.getProject().getUser().getId());
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
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                device1Found = true;
            }
            if (hdevice2.getId() == device.getId()) {
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(adminUser.getId(), device.getProject().getUser().getId());
                device2Found = true;
            }
        }
        Assert.assertTrue(device1Found);
        Assert.assertTrue(device2Found);
        Assert.assertEquals(200, restResponse.getStatus());

        // deletes hproject with deleteHProject call
        // this calls deletes hdevices in cascade mode
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseDeleteProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteProject.getStatus());
        Assert.assertNull(restResponseDeleteProject.getEntity());

        // checks: hdevices has been deleted in cascade mode with the call deleteHProject
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseFindHDevice1 = hDeviceRestApi.findHDevice(hdevice1.getId());
        Assert.assertEquals(404, restResponseFindHDevice1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindHDevice1.getEntity()).getType());

        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseFindHDevice2 = hDeviceRestApi.findHDevice(hdevice2.getId());
        Assert.assertEquals(404, restResponseFindHDevice2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindHDevice2.getEntity()).getType());
    }


    @Test
    public void test69_deleteHDeviceNotDeleteHProjectButDeleteInCascadeHPackets() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin deletes hdevice with the following call deleteHDevice.
        // This call delete in cascade all hpackets but not deletes hproject
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, true);
        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, true);

        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        //checks if device packets exists
        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket2Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertEquals(200, restResponsePacketByProject.getStatus());

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseDeleteDevice = hDeviceRestApi.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseDeleteDevice.getStatus());
        Assert.assertNull(restResponseDeleteDevice.getEntity());

        // checks if hproject is still stored in database
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseFindProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseFindProject.getStatus());

        // checks: hpackets has been deleted in cascade mode with the call deleteHDevice
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseFindPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseFindPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket1.getEntity()).getType());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseFindPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseFindPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket2.getEntity()).getType());
    }


    @Test
    public void test70_deleteHProjectDeleteInCascadeAllHDeviceAndHPackets() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin deletes hproject with the following call deleteHProject.
        // This call delete in cascade all hdevice and hpackets
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, true);
        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, true);

        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), hpacket1.getDevice().getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        //checks if device packets exists
        this.impersonateUser(hPacketRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
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
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket2Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertEquals(200, restResponsePacketByProject.getStatus());

        // deletes hproject: this call deletes all devices and packets
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseDeleteProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseDeleteProject.getStatus());
        Assert.assertNull(restResponseDeleteProject.getEntity());

        // checks: hdevice has been deleted in cascade mode with the call deleteHProject
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseFindDevice = hDeviceRestApi.findHDevice(hdevice.getId());
        Assert.assertEquals(404, restResponseFindDevice.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindDevice.getEntity()).getType());


        // checks: hpackets has been deleted in cascade mode with the call deleteHProject
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseFindPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseFindPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket1.getEntity()).getType());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseFindPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseFindPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseFindPacket2.getEntity()).getType());
    }


    @Test
    public void test71_findAllHDeviceShouldWorkIfListIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin find all HDevice with the following call findAllHDevice; list is empty
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.findAllHDevice();
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertTrue(listHDevice.isEmpty());
        Assert.assertEquals(0, listHDevice.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test72_findAllHDevicePaginatedShouldWorkIfListIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, hadmin find all Companies with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hDeviceRestService, adminUser);
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

    @Test
    public void test73_saveHDeviceShouldFailIfDeviceNameGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // device name is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName(createStringFieldWithSpecifiedLenght(256));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test74_saveHDeviceShouldFailIfBrandIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // but brand is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand(createStringFieldWithSpecifiedLenght(256));
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model ");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test75_saveHDeviceShouldFailIfModelIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // but model is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel(createStringFieldWithSpecifiedLenght(256));
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test76_saveHDeviceShouldFailIfFirmwareVersionIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // but firmware version is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion(createStringFieldWithSpecifiedLenght(256));
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test77_saveHDeviceShouldFailIfSoftwareVersionIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice, but
        // but software version is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        hdevice.setDescription("description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion(createStringFieldWithSpecifiedLenght(256));
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test78_updateHDeviceShouldFailIfDeviceNameIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // but device name is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setDeviceName(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test79_updateHDeviceShouldFailIfBrandIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // but brand is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setBrand(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test80_updateHDeviceShouldFailIfModelIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // but model  is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setModel(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test81_updateHDeviceShouldFailIfFirmwareVersionIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // but software version  is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setFirmwareVersion(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test82_updateHDeviceShouldFailIfSoftwareVersionIsGreaterThan255Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice, but
        // but software version  is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        hdevice.setSoftwareVersion(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hDeviceRestService, adminUser);
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
    public void test83_saveHDeviceShouldWorkIfDescriptionIs2999Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to save HDevice with the following call saveHDevice
        // description's length is 2999 chars
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("brand");
        int maxDescription = 3001;
        hdevice.setDescription(createStringFieldWithSpecifiedLenght(2999));
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("model ");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.0");
        hdevice.setAdmin(false);
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(2999, ((HDevice) restResponse.getEntity()).getDescription().length());
    }

    @Test
    public void test84_updateHDeviceShouldWorkIfDescriptionIs2999Chars() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to update HDevice with the following call updateHDevice
        // description's length is 2999 chars
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        HDevice hDevice = createHDevice(project);
        hDevice.setDescription(createStringFieldWithSpecifiedLenght(2999));
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.updateHDevice(hDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(2999, ((HDevice) restResponse.getEntity()).getDescription().length());
    }

    @Test
    public void test85_resetPasswordRequestShouldFailWhenHUserIsNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser tries to request  HDevice  password reset with the following call resetPasswordRequest
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice hDevice = createHDevice(project);
        Assert.assertNotEquals(0, hDevice.getId());
        impersonateUser(hDeviceRestService, null);
        Response restResponse = hDeviceRestService.resetPasswordRequest(hDevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test86_resetPasswordRequestShouldFailWhenHDeviceNotExist() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to request  HDevice  password reset with the following call resetPasswordRequest
        // but hdevice not exist
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice hDevice = createHDevice(project);
        Assert.assertNotEquals(0, hDevice.getId());
        impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.resetPasswordRequest(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test87_resetPasswordRequestShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // hadmin tries to request  HDevice  password reset with the following call resetPasswordRequest
        // response status code '200' OK
        // Assert that hdevice's password reset code field is not null
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice hDevice = createHDevice(project);
        Assert.assertNotEquals(0, hDevice.getId());
        impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.resetPasswordRequest(hDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        HDeviceRepository hDeviceRepository = getOsgiService(HDeviceRepository.class);
        HDevice deviceAfterOperation = hDeviceRepository.find(hDevice.getId(), null);
        Assert.assertNotNull(deviceAfterOperation.getPasswordResetCode());
    }

    @Test
    public void test88_resetPasswordShouldFailWhenHUserIsNotLogged() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice hDevice = createHDevice(project);
        Assert.assertNotEquals(0, hDevice.getId());
        impersonateUser(hDeviceRestService, null);

        Response restResponse = hDeviceRestService.resetPassword(hDevice.getId(), "", "", "");
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test89_resetPasswordShouldFailWhenHDeviceNotExist() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        // but Hdevice not exist
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice hDevice = createHDevice(project);
        Assert.assertNotEquals(0, hDevice.getId());
        impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.resetPassword(0, "", "", "");
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test90_resetPasswordShouldFailWhenPasswordResetCodeIsWrong() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        // but password reset code is wrong
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice hDevice = createHDevice(project);
        Assert.assertNotEquals(0, hDevice.getId());
        String hdeviceOldPassword = "passwordPass&01";
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hDevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hDevice);
        String wrongResetCode = "wrong";
        impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.resetPassword(hDevice.getId(), wrongResetCode, "Password123!", "Password123!");
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Assert that password not change
        HDevice reloadHDevice = hDeviceRepository.find(hDevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test91_resetPasswordShouldFailWhenNewPasswordIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        //  but newPassword is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertNotEquals(0, hdevice.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, null,
                "Password123!");
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Assert that password not change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test92_resetPasswordShouldFailWhenPasswordConfirmIsNull() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        // but passwordconfirm is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertNotEquals(0, hdevice.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, "Password123!",
                null);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Assert that password not change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test93_resetPasswordShouldFailWhenNewPasswordIsEmpty() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        //  but newPassword is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertNotEquals(0, hdevice.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        this.impersonateUser(hDeviceRestService, adminUser);
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, "",
                "Password123!");
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Assert that password not change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test94_resetPasswordShouldFailWhenNewPasswordIsMaliciousCode() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        // but newPassword is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertNotEquals(0, hdevice.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        this.impersonateUser(hDeviceRestService, adminUser);
        String maliciousPassword = "javascript:";
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, maliciousPassword,
                maliciousPassword);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Assert that password not change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test95_resetPasswordShouldFailWhenNewPasswordNotMatchPasswordConfirm() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        // but newPassword not match password confirm
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertNotEquals(0, hdevice.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        this.impersonateUser(hDeviceRestService, adminUser);
        String newPassword = "Password123!";
        String passwordConfirm = "Password123!!??";
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, newPassword,
                passwordConfirm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Assert that password not change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test96_resetPasswordShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // adminUser tries to reset password of HDevice, with the following call resetPassword,
        // response status code '200' OK
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        HDevice hdevice = createHDevice(hproject);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertNotEquals(0, hdevice.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        this.impersonateUser(hDeviceRestService, adminUser);
        String newPassword = "Password123!";
        String passwordConfirm = "Password123!";
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, newPassword,
                passwordConfirm);
        Assert.assertEquals(200, restResponse.getStatus());
        //Assert that password change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertFalse(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
        Assert.assertTrue(HyperIoTUtil.passwordMatches(newPassword, reloadHDevice.getPassword()));
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


    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Description");
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
        Assert.assertEquals(adminUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
    }


    private HProject createHProject() {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertNotNull(adminUser);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hproject.setDescription("Description");
        hproject.setUser((HUser) adminUser);
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals("Description",
                ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(adminUser.getId(),
                ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
    }

    private String testMaxDescription(int lengthDescription) {
        String symbol = "a";
        String description = String.format("%" + lengthDescription + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(lengthDescription, description.length());
        return description;
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


    @After
    public void afterTest() {
        // Remove projects and delete in cascade all devices created in every test
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
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
        this.impersonateUser(hDeviceRestApi, adminUser);
        restResponse = hDeviceRestApi.findAllHDevice();
        List<HDevice> devices = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        if (!devices.isEmpty()) {
            for (HDevice device : devices) {
                this.impersonateUser(hDeviceRestApi, adminUser);
                Response restResponse1 = hDeviceRestApi.deleteHDevice(device.getId());
                Assert.assertEquals(200, restResponse1.getStatus());
            }
        }
    }

}

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
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
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
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntityRepository;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi;
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
import java.util.stream.Collectors;

import static it.acsoftware.hyperiot.hdevice.test.HyperIoTHDeviceConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for HDevice System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHDeviceRestWithDefaultPermissionTest extends KarafTestSupport {

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
    public void test01_hdeviceModuleShouldWorkIfHUserIsActive() {
        HDeviceRestApi hdeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call checkModuleWorking checks if HDevice module working
        // correctly
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hdeviceRestService, huser);
        Response restResponse = hdeviceRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HDevice Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_hdeviceModuleShouldWorkIfHUserIsNotActive() {
        HDeviceRestApi hdeviceRestService = getOsgiService(HDeviceRestApi.class);
        // the following call checkModuleWorking checks if HDevice module working
        // correctly
        huser = huserWithDefaultPermissionInHyperIoTFramework(false);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertFalse(huser.isActive());
        this.impersonateUser(hdeviceRestService, huser);
        Response restResponse = hdeviceRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HDevice Module works!", restResponse.getEntity());
    }


    // HDevice action save: 1
    @Test
    public void test03_saveHDeviceWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        // huser, with default permission, save HDevice with the following call saveHDevice
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

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

        this.impersonateUser(hDeviceRestApi, huser);
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
        Assert.assertEquals(hproject.getUser().getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
    }


    // HDevice action update: 2
    @Test
    public void test04_updateHDeviceWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, update HDevice with the following call updateHDevice
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


    // HDevice action remove: 4
    @Test
    public void test05_deleteHDeviceWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, delete HDevice with the following call deleteHDevice
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

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HDevice action find: 8
    @Test
    public void test06_findHDeviceWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, find HDevice with the following call findHDevice
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

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hdevice.getId(),
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getDeviceName(),
                ((HDevice) restResponse.getEntity()).getDeviceName());
        Assert.assertEquals(hdevice.getBrand(),
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals(hdevice.getDescription(),
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hdevice.getFirmwareVersion(),
                ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals(hdevice.getModel(),
                ((HDevice) restResponse.getEntity()).getModel());
        Assert.assertEquals(hdevice.getSoftwareVersion(),
                ((HDevice) restResponse.getEntity()).getSoftwareVersion());
        Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
        Assert.assertEquals(hproject.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
    }


    // HDevice action find-all: 16
    @Test
    public void test07_findAllHDeviceWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, find all HDevice with the following call
        // findAllHDevice
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

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevice();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        boolean hdeviceFound = false;
        for (HDevice device : listHDevice) {
            if (hdevice.getId() == device.getId()) {
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
    }


    // HDevice action find-all: 16
    @Test
    public void test08_findAllHDevicePaginatedWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all
        // HProjects with pagination
        // response status code '200'
        int delta = 5;
        int page = 2;
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HDevice hdevice = createHDevice(hproject);
            Assert.assertNotEquals(0, hdevice.getId());
            Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
            Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
            devices.add(hdevice);
        }
        Assert.assertEquals(defaultDelta, devices.size());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponsePage2 = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevicesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertFalse(listHDevicesPage2.getResults().isEmpty());
        Assert.assertEquals(delta, listHDevicesPage2.getResults().size());
        Assert.assertEquals(delta, listHDevicesPage2.getDelta());
        Assert.assertEquals(page, listHDevicesPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHDevicesPage2.getNextPage());
        // delta is 5, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listHDevicesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    // HDevice action packets_management: 32
    @Test
    public void test09_getHDevicePacketListWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, find list HDevice packets with the following call
        // getHDevicePacketList
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

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestService, huser);
        Response restResponse = hPacketRestService.getHDevicePacketList(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHDevicePackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertEquals(2, listHDevicePackets.size());
        Assert.assertFalse(listHDevicePackets.isEmpty());

        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        for (HPacket packet : listHDevicePackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hpacket1.getDevice().getProject().getUser().getId(),
                        packet.getDevice().getProject().getUser().getId());
                Assert.assertEquals(2, packet.getFields().size());
                Assert.assertEquals(hpacket1.getFields().size(), packet.getFields().size());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hpacket2.getDevice().getProject().getUser().getId(),
                        packet.getDevice().getProject().getUser().getId());
                Assert.assertEquals(packet.getFields(), packet.getFields());
                hpacket2Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
    }


    // HDevice action packets_management: 32
    @Test
    public void test10_findAllHPacketByProjectIdWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, finds all HPacket by hprojectId with the following
        // call findAllHPacketByProjectId
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

        HPacket hpacket1 = createHPacketAndAddHPacketField(hdevice, true);
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

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(3, listHPackets.size());
        boolean hpacket1Found = false;
        boolean fieldOfHPacket1Found = false;
        boolean hpacket2Found = false;
        boolean hpacket3Found = false;
        for (HPacket packet : listHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket1Found = true;
                Assert.assertEquals(2, packet.getFields().size());
                fieldOfHPacket1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket2Found = true;
                Assert.assertEquals(0, packet.getFields().size());
            }
            if (hpacket3.getId() == packet.getId()) {
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket3Found = true;
                Assert.assertEquals(0, packet.getFields().size());
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(fieldOfHPacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertTrue(hpacket3Found);
    }

    @Test
    public void test11_getProjectDevicesListWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser, with permission, finds project's device list with the following
        // call getProjectDevicesList
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

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(1, listHDevice.size());
        Assert.assertEquals(listHDevice.get(0).getId(), hdevice.getId());
    }

    @Test
    public void test12_getProjectDevicesListWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser2, with permission, finds project's device list with the following
        // call getProjectDevicesList
        //but huser2 not owns and not share this project.
        // response status code '403' HyperIoTUnauthorizedException
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

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        this.impersonateUser(hDeviceRestService, huser2);
        Assert.assertNotEquals(hproject.getUser().getId(), huser2.getId());
        Assert.assertFalse(userIsInHProjectSharingUserList(hproject, huser2));
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_getProjectDevicesListWithDefaultPermissionShouldWorkIfUserNotOwnsProjectButProjectIsSharedWithHim() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser2, with permission, finds project's device list with the following
        // call getProjectDevicesList
        // the project is shared with huser2
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

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        createSharedEntity(hproject, huser, huser2);
        this.impersonateUser(hDeviceRestService, huser2);
        Assert.assertNotEquals(hproject.getUser().getId(), huser2.getId());
        Assert.assertTrue(userIsInHProjectSharingUserList(hproject, huser2));
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(1, listHDevice.size());
        Assert.assertEquals(listHDevice.get(0).getId(), hdevice.getId());
    }

    @Test
    public void test14_resetPasswordRequestWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser, with permission, request device password reset with the following call resetPasswordRequest
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.resetPasswordRequest(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test15_resetPasswordWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser, with permission, reset device password  with the following call resetPasswordRequest
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        String hdeviceOldPassword = "passwordPass&01";
        Assert.assertNotEquals(0, hdevice.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        this.impersonateUser(hDeviceRestService, huser);
        String newPassword = "Password123!";
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, newPassword, newPassword);
        Assert.assertEquals(200, restResponse.getStatus());
        //Assert that password change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertFalse(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
        Assert.assertTrue(HyperIoTUtil.passwordMatches(newPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test16_resetPasswordWithDefaultPermissionShouldFailWhenHUser2NotOwnHDeviceProject() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser, register hdevice
        // huser2 , with permission but now owns hdevice's project, reset device password  with the following call resetPasswordRequest
        // response status code '404' HyperIoTEntityNotFound (filter avoid that huser2 can retrieve device)
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        String hdeviceOldPassword = "passwordPass&01";
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        this.impersonateUser(hDeviceRestService, huser2);
        String newPassword = "Password123!";
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, newPassword, newPassword);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        //Assert that password not change
        HDevice reloadHDevice = hDeviceRepository.find(hdevice.getId(), null);
        Assert.assertNotNull(reloadHDevice);
        Assert.assertFalse(HyperIoTUtil.passwordMatches(newPassword, reloadHDevice.getPassword()));
        Assert.assertTrue(HyperIoTUtil.passwordMatches(hdeviceOldPassword, reloadHDevice.getPassword()));
    }

    @Test
    public void test17_resetPasswordWithDefaultPermissionShouldSuccessWhenHUser2ShareNotOwnHDeviceProject() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // huser, register hdevice
        // huser2 (with permission , with share relationship,  but now owns hdevice's project) reset device password  with the following call resetPasswordRequest
        // response status code '404' HyperIoTUnauthorizedException
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        String hdeviceOldPassword = "passwordPass&01";
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        hdevice.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(hdevice);
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        //share hproject
        createSharedEntity(hproject, huser, huser2);
        this.impersonateUser(hDeviceRestService, huser2);
        String newPassword = "Password123!";
        Response restResponse = hDeviceRestService.resetPassword(hdevice.getId(), passwordResetCode, newPassword, newPassword);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

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

    private boolean userIsInHProjectSharingUserList(HProject project, HUser sharingUser) {
        List<HyperIoTUser> projectSharingUser = getProjectSharingUser(project);
        if (projectSharingUser.isEmpty()) {
            return false;
        }
        return projectSharingUser.stream().map(HyperIoTUser::getUsername).collect(Collectors.toList()).contains(sharingUser.getUsername());
    }

    private List<HyperIoTUser> getProjectSharingUser(HProject project) {
        Assert.assertNotEquals(0, project.getId());
        SharedEntityRepository sharedEntityRepository = getOsgiService(SharedEntityRepository.class);
        return sharedEntityRepository.getSharingUsers(hProjectResourceName, project.getId());
    }

    private HProject createHProject(HUser huser) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HProject hproject = new HProject();
        hproject.setName("Project " + UUID.randomUUID());
        hproject.setDescription("Description");
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals("Description",
                ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
    }


    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        huser = hproject.getUser();
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
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
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
        Assert.assertEquals(hproject.getUser().getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
    }


    private HPacket createHPacketAndAddHPacketField(HDevice hdevice, boolean createField) {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        huser = hdevice.getProject().getUser();
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

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());

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


            List<HPacketField> fields = new ArrayList<>();
            fields.addAll(((HPacket) restResponse.getEntity()).getFields());
            Assert.assertEquals(0, fields.get(0).getId());
            Assert.assertEquals(0, fields.get(1).getId());

            // add field1
            this.impersonateUser(hPacketRestApi, huser);
            Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
            Assert.assertEquals(200, responseAddField1.getStatus());
            // field1 has been saved
            Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() != 0));
            // field2 hasn't been saved
            Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() == 0));

            //check restSaveHPacket field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(),
                    ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() == ((HPacketField) responseAddField1.getEntity()).getId()));

            Assert.assertEquals(((HPacketField) responseAddField1.getEntity()).getPacket().getId(),
                    ((HPacket) restResponse.getEntity()).getId());

            // add field2
            Response responseAddField2 = hPacketRestApi.addHPacketField(hpacket.getId(), field2);
            Assert.assertEquals(200, responseAddField2.getStatus());

            // field2 has been saved
            Assert.assertTrue(
                    fields.stream().anyMatch(field -> field.getId() != 0));
            //check restSaveHPacket field2 is equals to responseAddField2 field2
            Assert.assertEquals(field2.getId(),
                    ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() ==
                    ((HPacketField) responseAddField2.getEntity()).getId()));

            Assert.assertEquals(((HPacketField) responseAddField2.getEntity()).getPacket().getId(),
                    ((HPacket) restResponse.getEntity()).getId());
            Assert.assertEquals(2, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        return hpacket;
    }

    // HDevice is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;

    @After
    public void afterTest() {
        // Remove projects and delete in cascade all devices created in every test
        if ((huser != null) && (huser.isActive())) {
            HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
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
        // Remove all husers created in every test
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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
            boolean resourceNameHDevice = false;
            for (Permission permission : listPermissions) {
                if (permission.getEntityResourceName().contains(permissionHDevice)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionHDevice, permission.getEntityResourceName());
                    Assert.assertEquals(permissionHDevice + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(63, permission.getActionIds());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameHDevice = true;
                }
            }
            Assert.assertTrue(resourceNameHDevice);
        }
        return huser;
    }

}

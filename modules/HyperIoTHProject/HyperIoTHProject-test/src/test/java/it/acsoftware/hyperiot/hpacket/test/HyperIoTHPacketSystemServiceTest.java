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

package it.acsoftware.hyperiot.hpacket.test;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Aristide Cittadino Interface component for HPacket System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHPacketSystemServiceTest extends KarafTestSupport {

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
    public void test00s_hyperIoTFrameworkShouldBeInstalled() {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class, 0);
        String features = executeCommand("feature:list -i");
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTMail-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTHProject-features ", features);
        assertContains("HyperIoTDashboard-features", features);
        String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }


    @Test
    public void test01s_findByDeviceIdShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find device by id with the following call findByDeviceId
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject((HUser) adminUser);
        HDevice hdevice = createHDevice(hproject);
        HPacket hpacket = createHPacket(hdevice);
        this.impersonateUser(hPacketRestApi, adminUser);
        Collection<HPacket> listPackets = hPacketSystemApi.findByDeviceId(hdevice.getId());
        Assert.assertFalse(listPackets.isEmpty());
        boolean packetFound = false;
        for (HPacket packets : listPackets) {
            if (hpacket.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(0)).getDevice().getProject().getId());
                packetFound = true;
            }
        }
        Assert.assertTrue(packetFound);
    }

    @Test
    public void test02s_findByDeviceIdShouldWorkIfListPacketsIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find device by id with the following call findByDeviceId, listPackets
        // is empty
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        HProject hproject = createHProject((HUser) adminUser);
        HDevice hdevice = createHDevice(hproject);
        Collection<HPacket> listPackets = hPacketSystemApi.findByDeviceId(hdevice.getId());
        Assert.assertEquals(0, listPackets.size());
        Assert.assertTrue(listPackets.isEmpty());
    }


    @Test(expected = HyperIoTEntityNotFound.class)
    public void test03s_findByDeviceIdShouldWorkIfHDeviceNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find device by id with the following call findByDeviceId
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        hPacketSystemApi.findByDeviceId(0);
    }


    @Test
    public void test04s_getPacketsListShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by deviceId with the following call getPacketsList
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject((HUser) adminUser);
        HDevice hdevice = createHDevice(hproject);
        HPacket hpacket1 = createHPacket(hdevice);
        HPacket hpacket2 = createHPacket(hdevice);
        HPacket hpacket3 = createHPacket(hdevice);
        this.impersonateUser(hPacketRestApi, adminUser);
        Collection<HPacket> listPackets = hPacketSystemApi.getPacketsList(hdevice.getId());
        Assert.assertFalse(listPackets.isEmpty());
        Assert.assertEquals(3, listPackets.size());
        boolean packet1Found = false;
        boolean packet2Found = false;
        boolean packet3Found = false;
        for (HPacket packets : listPackets) {
            if (hpacket1.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(0)).getDevice().getProject().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(1)).getDevice().getProject().getId());
                packet2Found = true;
            }
            if (hpacket3.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(2)).getDevice().getProject().getId());
                packet3Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertTrue(packet3Found);
    }


    @Test
    public void test05s_getPacketsListShouldWorkIfListPacketsIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by deviceId with the following call getPacketsList,
        // listPackets is empty
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject((HUser) adminUser);
        HDevice hdevice = createHDevice(hproject);
        this.impersonateUser(hPacketRestApi, adminUser);
        Collection<HPacket> listPackets = hPacketSystemApi.getPacketsList(hdevice.getId());
        Assert.assertTrue(listPackets.isEmpty());
        Assert.assertEquals(0, listPackets.size());
    }


    @Test(expected = HyperIoTEntityNotFound.class)
    public void test06s_getPacketsListShouldWorkIfHDeviceNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by deviceId with the following call getPacketsList,
        // listPackets is empty
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        hPacketSystemApi.getPacketsList(0);
    }


    @Test
    public void test07s_getProjectPacketsListShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by projectId with the following call getProjectPacketsList
        HPacketApi hPacketApi = getOsgiService(HPacketApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject((HUser) adminUser);
        HDevice hdevice = createHDevice(hproject);
        HPacket hpacket1 = createHPacket(hdevice);
        HPacket hpacket2 = createHPacket(hdevice);
        HPacket hpacket3 = createHPacket(hdevice);
        HyperIoTContext ctx = hPacketRestApi.impersonate(adminUser);
        Collection<HPacket> listPackets = hPacketApi.getProjectPacketsList(ctx,hproject.getId());
        Assert.assertFalse(listPackets.isEmpty());
        Assert.assertEquals(3, listPackets.size());
        boolean packet1Found = false;
        boolean packet2Found = false;
        boolean packet3Found = false;
        for (HPacket packets : listPackets) {
            if (hpacket1.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(0)).getDevice().getProject().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(1)).getDevice().getProject().getId());
                packet2Found = true;
            }
            if (hpacket3.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(2)).getDevice().getProject().getId());
                packet3Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertTrue(packet3Found);
    }


    @Test
    public void test08s_getProjectPacketsListShouldWorkIfListPacketsIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by projectId with the following call getProjectPacketsList,
        // listPackets is empty
        HPacketApi hPacketApi = getOsgiService(HPacketApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject((HUser) adminUser);
        HyperIoTContext ctx = hPacketRestApi.impersonate(adminUser);
        Collection<HPacket> listPackets = hPacketApi.getProjectPacketsList(ctx,hproject.getId());
        Assert.assertTrue(listPackets.isEmpty());
        Assert.assertEquals(0, listPackets.size());
    }


    @Test(expected = HyperIoTEntityNotFound.class)
    public void test09s_getProjectPacketsListShouldWorkIfHProjectNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by projectId with the following call getProjectPacketsList,
        // listPackets is empty
        HPacketApi hPacketApi = getOsgiService(HPacketApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        HyperIoTContext ctx = hPacketRestApi.impersonate(adminUser);
        hPacketApi.getProjectPacketsList(ctx,0);
    }


    @Test
    public void test10s_getProjectPacketsTreeShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by projectId with the following call getProjectPacketsTree
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject((HUser) adminUser);
        HDevice hdevice = createHDevice(hproject);
        HPacket hpacket1 = createHPacket(hdevice);
        HPacket hpacket2 = createHPacket(hdevice);
        HPacket hpacket3 = createHPacket(hdevice);
        this.impersonateUser(hPacketRestApi, adminUser);
        Collection<HPacket> listPackets = hPacketSystemApi.getProjectPacketsTree(hproject.getId());
        Assert.assertFalse(listPackets.isEmpty());
        Assert.assertEquals(3, listPackets.size());
        boolean packet1Found = false;
        boolean packet2Found = false;
        boolean packet3Found = false;
        for (HPacket packets : listPackets) {
            if (hpacket1.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(0)).getDevice().getProject().getId());
                packet1Found = true;
            }
            if (hpacket2.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(1)).getDevice().getProject().getId());
                packet2Found = true;
            }
            if (hpacket3.getId() == packets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listPackets).get(2)).getDevice().getProject().getId());
                packet3Found = true;
            }
        }
        Assert.assertTrue(packet1Found);
        Assert.assertTrue(packet2Found);
        Assert.assertTrue(packet3Found);
    }


    @Test
    public void test11s_getProjectPacketsTreeShouldWorkIfListPacketsIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by projectId with the following call getProjectPacketsTree,
        // listPackets is empty
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject((HUser) adminUser);
        this.impersonateUser(hPacketRestApi, adminUser);
        Collection<HPacket> listPackets = hPacketSystemApi.getProjectPacketsTree(hproject.getId());
        Assert.assertTrue(listPackets.isEmpty());
        Assert.assertEquals(0, listPackets.size());
    }


    @Test(expected = HyperIoTEntityNotFound.class)
    public void test12s_getProjectPacketsTreeShouldWorkIfHProjectNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find packetLists by projectId with the following call getProjectPacketsTree,
        // listPackets is empty
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        hPacketSystemApi.getProjectPacketsTree(0);
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    @After
    public void afterTest() {
        // Remove all projects and delete in cascade all associated entities (HDevice, HPacket, HPacketField) in every tests
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

    private HProject createHProject(HUser huser) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertNotNull(adminUser);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hproject.setDescription("Description");
        hproject.setUser(huser);
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

}

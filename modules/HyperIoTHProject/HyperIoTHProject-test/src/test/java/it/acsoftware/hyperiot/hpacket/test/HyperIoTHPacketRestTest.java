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

import static it.acsoftware.hyperiot.hpacket.test.HyperIoTHPacketConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for HPacket System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHPacketRestTest extends KarafTestSupport {

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
    public void test01_hpacketModuleShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call checkModuleWorking checks if HPacket module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HPacket Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_saveHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin save HPacket with the following call saveHPacket
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
        Assert.assertNotEquals(0, ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }

    @Test
    public void test03_saveHPacketShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to save HPacket, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

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
        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test04_updateHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket with the following call updateHPacket
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setVersion("version edited");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hpacket.getEntityVersion() + 1,
                (((HPacket) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals("version edited",
                (((HPacket) restResponse.getEntity()).getVersion()));
        Assert.assertEquals(hproject.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(adminUser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }

    @Test
    public void test05_updateHPacketShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to update HPacket, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        hpacket.setName("edit failed");
        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_deleteHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin delete HPacket with the following call deleteHPacket
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test07_deleteHPacketShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to delete HPacket, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find HPacket with the following call findHPacket
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(adminUser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }

    @Test
    public void test09_findHPacketShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to find HPacket, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.findHPacket(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_findAllHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin find all HPacket with the following call findAllHPacket
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacket();
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(1, listHPackets.size());
        boolean hpacketFound = false;
        for (HPacket packet : listHPackets) {
            if (hpacket.getId() == packet.getId()) {
                Assert.assertEquals(hdevice.getId(), packet.getDevice().getId());
                Assert.assertEquals(hproject.getId(), packet.getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacketFound = true;
            }
        }
        Assert.assertTrue(hpacketFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test11_findAllHPacketShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to find all HPacket, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.findAllHPacket();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_saveHPacketShouldFailIfNameIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName(null);
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
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test13_saveHPacketShouldFailIfNameIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("");
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
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test14_saveHPacketShouldFailIfNameIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("javascript:");
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
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test15_saveHPacketShouldFailIfHPacketTypeIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but hpacket type is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(null);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test16_saveHPacketShouldFailIfHPacketFormatIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but hpacket format is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(null);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.INPUT);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-format", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test17_saveHPacketShouldFailIfHPacketSerializationIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but hpacket serialization is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.CSV);
        hpacket.setSerialization(null);
        hpacket.setType(HPacketType.INPUT);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-serialization", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test18_saveHPacketShouldFailIfVersionIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but version is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion(null);
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test19_saveHPacketShouldFailIfVersionIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but version is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("");
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test20_saveHPacketShouldFailIfVersionIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but version is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("javascript:");
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test21_saveHPacketShouldFailIfHDeviceIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but HDevice is null
        // response status code '404' HyperIoTEntityNotFound(Cannot save a Packet without specify the device to which it's related)
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(null);
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
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(((HyperIoTBaseError) restResponse.getEntity()).getType(), hyperIoTException + "HyperIoTEntityNotFound");

    }

    @Test
    public void test22_updateHPacketShouldFailIfNameIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setName(null);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test23_updateHPacketShouldFailIfNameIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setName("");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test24_updateHPacketShouldFailIfNameIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setName("vbscript:");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test25_updateHPacketShouldFailIfHPacketTypeIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // hpacket type is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setType(null);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test26_updateHPacketShouldFailIfHPacketFormatIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // hpacket format is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setFormat(null);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-format", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test27_updateHPacketShouldFailIfHPacketSerializationIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // hpacket serialization is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setSerialization(null);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-serialization", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test28_updateHPacketShouldFailIfVersionIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // version is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setVersion(null);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test29_updateHPacketShouldFailIfVersionIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // version is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setVersion("");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test30_updateHPacketShouldFailIfVersionIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // version is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setVersion("</script>");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test31_updateHPacketShouldFailIfHDeviceIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // HDevice is null
        // response status code '404' HyperIoTEntityNotFound (Cannot update an HPacket without specify the device tho which it's related)
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setDevice(null);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test32_deleteHPacketShouldFailIfEntityNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to delete HPacket with the following call deleteHPacket, but
        // entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.deleteHPacket(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test33_updateHPacketShouldFailIfEntityNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        //Save project
        HProject project = createHProject();
        //Save device
        HDevice device = createHDevice(project);
        // entity isn't stored in database
        HPacket hpacket = new HPacket();
        //Add a device that exist in database to the packet.
        hpacket.setDevice(device);
        hpacket.setFormat(HPacketFormat.CSV);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test33_01_updateHPacketShouldFailIfDeviceRelatedToPacketIsNotPresentInDabase() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // hdevice related to the hpacket not exist in database
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        //Save project
        HProject project = createHProject();
        //Device entity isn't stored in database
        HDevice device = new HDevice();
        // Packet entity isn't stored in database
        HPacket hpacket = new HPacket();
        hpacket.setDevice(device);
        hpacket.setFormat(HPacketFormat.CSV);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test34_saveHPacketShouldFailIfEntityIsDuplicated() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacket duplicateHPacket = new HPacket();
        duplicateHPacket.setName(hpacket.getName());
        duplicateHPacket.setDevice(hpacket.getDevice());
        duplicateHPacket.setFormat(HPacketFormat.JSON);
        duplicateHPacket.setSerialization(HPacketSerialization.AVRO);
        duplicateHPacket.setType(HPacketType.IO);
        duplicateHPacket.setVersion(hpacket.getVersion());
        duplicateHPacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        duplicateHPacket.setTimestampField(String.valueOf(timestamp));
        duplicateHPacket.setTimestampFormat("String");

        Assert.assertEquals(hpacket.getName(), duplicateHPacket.getName());
        Assert.assertEquals(hdevice.getId(), duplicateHPacket.getDevice().getId());
        Assert.assertEquals(hpacket.getVersion(), duplicateHPacket.getVersion());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(duplicateHPacket);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean hpacketNameIsDuplicated = false;
        boolean hdeviceIdIsDuplicated = false;
        boolean hpacketVersionIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hpacketNameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("device_id")) {
                Assert.assertEquals("device_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hdeviceIdIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("version")) {
                Assert.assertEquals("version", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hpacketVersionIsDuplicated = true;
            }
        }
        Assert.assertTrue(hpacketNameIsDuplicated);
        Assert.assertTrue(hdeviceIdIsDuplicated);
        Assert.assertTrue(hpacketVersionIsDuplicated);
    }


    @Test
    public void test35_findListHDevicePacketsShouldWork() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // hadmin find list of HDevice packets with the following call getHDevicePacketList
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestService, adminUser);
        Response restResponse = hPacketRestService.getHDevicePacketList(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHDevicePackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertEquals(1, listHDevicePackets.size());
        Assert.assertFalse(listHDevicePackets.isEmpty());
        Assert.assertNotEquals(0, listHDevicePackets.get(0).getId());
        Assert.assertEquals(hpacket.getId(), listHDevicePackets.get(0).getId());
        Assert.assertEquals(hdevice.getId(), listHDevicePackets.get(0).getDevice().getId());
        Assert.assertEquals(hproject.getId(), listHDevicePackets.get(0).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), listHDevicePackets.get(0).getDevice().getProject().getUser().getId());
    }

    @Test
    public void test36_findListHDevicePacketsShouldWorkAndListHDevicePacketsIsEmpty() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // hadmin find list HDevice packets with the following call
        // getHDevicePacketList. listHDevicePackets is empty
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

        this.impersonateUser(hPacketRestService, adminUser);
        Response restResponse = hPacketRestService.getHDevicePacketList(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHDevicePackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertEquals(0, listHDevicePackets.size());
        Assert.assertTrue(listHDevicePackets.isEmpty());
    }

    @Test
    public void test37_findListHDevicePacketsShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // the following call tries to find list HDevice packets,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        this.impersonateUser(hPacketRestService, null);
        Response restResponse = hPacketRestService.getHDevicePacketList(hdevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test38_findListHDevicePacketsShouldFailIfEntityNotFound() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // hadmin tries to find list HDevice packets with the following call
        // getHDevicePacketList, but HDevice not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestService, adminUser);
        Response restResponse = hPacketRestService.getHDevicePacketList(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test39_addHPacketFieldsShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        HPacketField field2 = new HPacketField();
        field2.setPacket(hpacket);
        field2.setName("humidity");
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
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(200, responseAddField1.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());

        // add field2
        Response responseAddField2 = hPacketRestApi.addHPacketField(hpacket.getId(), field2);
        Assert.assertEquals(200, responseAddField2.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getUser().getId());

        List<HPacketField> fields = new ArrayList<>(((HPacket) restSaveHPacket.getEntity()).getFields());
        //check restSaveHPacket field1 is equals to responseAddField1 field1
        Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
        Assert.assertEquals(fields.get(0).getId(), ((HPacketField) responseAddField1.getEntity()).getId());
        Assert.assertEquals(((HPacket) restSaveHPacket.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

        //check restSaveHPacket field2 is equals to responseAddField2 field2
        Assert.assertEquals(field2.getId(), ((HPacketField) responseAddField2.getEntity()).getId());
        Assert.assertEquals(fields.get(1).getId(), ((HPacketField) responseAddField2.getEntity()).getId());
        Assert.assertEquals(((HPacket) restSaveHPacket.getEntity()).getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());

        Assert.assertEquals(2, ((HPacket) restSaveHPacket.getEntity()).getFields().size());
    }


    @Test
    public void test40_addHPacketFieldsShouldFailIfHPacketNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to add HPacket fields with the following call addHPacketField,
        // but HPacket not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HPacketField field1 = new HPacketField();
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseAddField1 = hPacketRestApi.addHPacketField(0, field1);
        Assert.assertEquals(404, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
    }


    @Test
    public void test41_addHPacketFieldsShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to add HPacket fields, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        HPacketField field = new HPacketField();
        field.setPacket(hpacket);
        field.setName("temperature");
        field.setDescription("Temperature");
        field.setType(HPacketFieldType.DOUBLE);
        field.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field.setValue(24.0);

        hpacket.setFields(new HashSet<>() {
            {
                add(field);
            }
        });

        Assert.assertEquals(0, hpacket.getFields().iterator().next().getId());

        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.addHPacketField(hpacket.getId(), field);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test42_updateHpacketFieldsShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        Date timestamp = new Date();
        updateField.setDescription("Temperature edited in date " + timestamp);

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(200, restResponseUpdateField.getStatus());
        Assert.assertEquals(hpacket.getEntityVersion() + 1,
                (((HPacketField) restResponseUpdateField.getEntity()).getEntityVersion()));
        Assert.assertEquals("Temperature edited in date " + timestamp,
                (((HPacketField) restResponseUpdateField.getEntity()).getDescription()));
    }


    @Test
    public void test43_updateHpacketFieldsShouldFailIfHPacketNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket fields with the following call updateHPacketField,
        // but HPacket not found
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        Date timestamp = new Date();
        updateField.setDescription("Edit failed in date " + timestamp);

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(0, updateField);
        Assert.assertEquals(404, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
    }


    @Test
    public void test44_updateHpacketFieldsShouldFailIfHPacketNotHaveField() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket fields with the following call updateHPacketField,
        // but HPacket not have a field
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField updateField = new HPacketField();
        Date timestamp = new Date();
        updateField.setDescription("Edit failed in date " + timestamp);

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(404, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
    }

    @Test
    public void test45_updateHpacketFieldsShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to update HPacket fields, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());

        HPacketField updateField = field;
        Date timestamp = new Date();
        updateField.setDescription("Edit failed in date " + timestamp);

        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test46_deleteHpacketFieldsShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin deletes HPacket fields with the following call deleteHPacketField
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());
        Iterator<HPacketField> itF = hpacket.getFields().iterator();
        HPacketField field1 = itF.next();
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field1.getPacket().getDevice().getProject().getUser().getId());

        HPacketField field2 = itF.next();
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field2.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseDeleteField1 = hPacketRestApi.deleteHPacketField(field1.getId());
        Assert.assertEquals(200, responseDeleteField1.getStatus());
        Assert.assertNull(responseDeleteField1.getEntity());
    }


    @Test
    public void test47_deleteHpacketFieldsShouldFailIfHPacketFieldNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to delete HPacket fields with the following call deleteHPacketField,
        // but HPacketField not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response respDeleteHPacketField = hPacketRestApi.deleteHPacketField(0);
        Assert.assertEquals(404, respDeleteHPacketField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) respDeleteHPacketField.getEntity()).getType());
    }


    @Test
    public void test48_deleteHpacketFieldsShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to delete HPacket fields, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        Iterator<HPacketField> itF = hpacket.getFields().iterator();
        HPacketField field1 = itF.next();
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());

        HPacketField field2 = itF.next();
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());

        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.deleteHPacketField(field1.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test49_findTreeFieldsShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin finds tree fields with the following call findTreeFields
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Iterator<HPacketField> itF = hpacket.getFields().iterator();
        HPacketField field1 = itF.next();
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field1.getPacket().getDevice().getProject().getUser().getId());

        HPacketField field2 = itF.next();
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field2.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findTreeFields(hpacket.getId());
        List<HPacketField> listHPacketFields = restResponse.readEntity(new GenericType<List<HPacketField>>() {
        });
        Assert.assertFalse(listHPacketFields.isEmpty());
        Assert.assertEquals(2, listHPacketFields.size());
        boolean field1Found = false;
        boolean field2Found = false;
        for (HPacketField field : listHPacketFields) {
            if (field1.getId() == field.getId()) {
                Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
                Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
                Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());
                field1Found = true;
            }
            if (field2.getId() == field.getId()) {
                Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
                Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
                Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
                Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());
                field2Found = true;
            }
        }
        Assert.assertTrue(field1Found);
        Assert.assertTrue(field2Found);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test50_findTreeFieldsShouldFailIfFieldNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin finds tree fields with the following call findTreeFields,
        // but fields not found. This call return an empty list
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findTreeFields(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacketField> listHPacketFields = restResponse.readEntity(new GenericType<List<HPacketField>>() {
        });
        Assert.assertTrue(listHPacketFields.isEmpty());
        Assert.assertEquals(0, listHPacketFields.size());
    }


    @Test
    public void test51_findTreeFieldsShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to find HPacket fields, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());

        Iterator<HPacketField> itF = hpacket.getFields().iterator();
        HPacketField field1 = itF.next();
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());

        HPacketField field2 = itF.next();
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());

        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.findTreeFields(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test52_findTreeFieldsShouldFailIfHPacketNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to find tree fields with the following call findTreeFields,
        // but HPacket not found.
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findTreeFields(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test53_updateHPacketShouldFailIfEntityIsDuplicated() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacket duplicateHPacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, duplicateHPacket.getId());
        Assert.assertNotEquals(hpacket.getId(), duplicateHPacket.getId());
        Assert.assertEquals(hdevice.getId(), duplicateHPacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), duplicateHPacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), duplicateHPacket.getDevice().getProject().getUser().getId());

        duplicateHPacket.setName(hpacket.getName());
        duplicateHPacket.setDevice(hpacket.getDevice());
        duplicateHPacket.setVersion(hpacket.getVersion());

        Assert.assertEquals(hpacket.getName(), duplicateHPacket.getName());
        Assert.assertEquals(hdevice.getId(), duplicateHPacket.getDevice().getId());
        Assert.assertEquals(hpacket.getVersion(), duplicateHPacket.getVersion());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(duplicateHPacket);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean hpacketNameIsDuplicated = false;
        boolean hdeviceIdIsDuplicated = false;
        boolean hpacketVersionIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hpacketNameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("device_id")) {
                Assert.assertEquals("device_id", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hdeviceIdIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("version")) {
                Assert.assertEquals("version", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hpacketVersionIsDuplicated = true;
            }
        }
        Assert.assertTrue(hpacketNameIsDuplicated);
        Assert.assertTrue(hdeviceIdIsDuplicated);
        Assert.assertTrue(hpacketVersionIsDuplicated);
    }


    @Test
    public void test54_findAllHPacketByProjectIdShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin finds all HPacket by hprojectId with the following call findAllHPacketByProjectId
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

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
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
    public void test55_findAllHPacketByProjectIdShouldWorkIfHProjectNotHaveHPacket() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin finds all HPacket by hprojectId with the following call findAllHPacketByProjectId
        // but hpoject not have a hpacket. This call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertTrue(listHPackets.isEmpty());
        Assert.assertEquals(0, listHPackets.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test56_findAllHPacketByProjectIdShouldFailIfHProjectNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to find all HPacket by hprojectId with the following
        // call findAllHPacketByProjectId, but hproject not found.
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test57_findAllHPacketPaginatedShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, hadmin finds all
        // HPackets with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 7;
        int page = 1;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(defaultDelta, hpackets.size());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(delta, listHPackets.getResults().size());
        Assert.assertEquals(delta, listHPackets.getDelta());
        Assert.assertEquals(page, listHPackets.getCurrentPage());
        Assert.assertEquals(page + 1, listHPackets.getNextPage());
        // delta is 7, page 1: 10 entities stored in database
        Assert.assertEquals(2, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePage2 = hPacketRestApi.findAllHPacketPaginated(delta, 2);
        HyperIoTPaginableResult<HPacket> listHPacketsPage1 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage1.getResults().isEmpty());
        Assert.assertEquals(3, listHPacketsPage1.getResults().size());
        Assert.assertEquals(delta, listHPacketsPage1.getDelta());
        Assert.assertEquals(page + 1, listHPacketsPage1.getCurrentPage());
        Assert.assertEquals(page, listHPacketsPage1.getNextPage());
        // delta is 7, page 2: 10 entities stored in database
        Assert.assertEquals(2, listHPacketsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test58_findAllHPacketPaginatedShouldFailIfNotLogged() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call tries to find all HPackets with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(hPacketRestApi, null);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test59_findAllHPacketPaginatedShouldWorkIfDeltaAndPageAreNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, hadmin find all HPackets with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());
        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 9;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listHPackets.getResults().size());
        Assert.assertEquals(defaultDelta, listHPackets.getDelta());
        Assert.assertEquals(defaultPage, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPackets.getNextPage());
        // default delta is 10, default page is 1: 9 entities stored in database
        Assert.assertEquals(1, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test60_findAllHPacketPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, hadmin find all HPackets with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 2;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 11;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(1, listHPackets.getResults().size());
        Assert.assertEquals(defaultDelta, listHPackets.getDelta());
        Assert.assertEquals(page, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPackets.getNextPage());
        // default delta is 10, page is 2: 11 entities stored in database
        Assert.assertEquals(2, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePage1 = hPacketRestApi.findAllHPacketPaginated(delta, 1);
        HyperIoTPaginableResult<HPacket> listHPacketsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHPacketsPage1.getCurrentPage());
        Assert.assertEquals(page, listHPacketsPage1.getNextPage());
        // default delta is 10, page is 1: 11 entities stored in database
        Assert.assertEquals(2, listHPacketsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test61_findAllHPacketPaginatedShouldWorkIfDeltaIsZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, hadmin find all HPackets with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 3;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 21;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(1, listHPackets.getResults().size());
        Assert.assertEquals(defaultDelta, listHPackets.getDelta());
        Assert.assertEquals(page, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPackets.getNextPage());
        // default delta is 10, page is 3: 21 entities stored in database
        Assert.assertEquals(3, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePage1 = hPacketRestApi.findAllHPacketPaginated(delta, 1);
        HyperIoTPaginableResult<HPacket> listHPacketsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHPacketsPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHPacketsPage1.getNextPage());
        // default delta is 10, page is 1: 21 entities stored in database
        Assert.assertEquals(3, listHPacketsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        // checks with page = 2
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePage2 = hPacketRestApi.findAllHPacketPaginated(delta, 2);
        HyperIoTPaginableResult<HPacket> listHPacketsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHPacketsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHPacketsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHPacketsPage2.getCurrentPage());
        Assert.assertEquals(page, listHPacketsPage2.getNextPage());
        // default delta is 10, page is 2: 21 entities stored in database
        Assert.assertEquals(3, listHPacketsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test62_findAllHPacketPaginatedShouldWorkIfPageIsLowerThanZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, hadmin find all HPackets with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 8;
        int page = -1;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 12;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(delta, listHPackets.getResults().size());
        Assert.assertEquals(delta, listHPackets.getDelta());
        Assert.assertEquals(defaultPage, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHPackets.getNextPage());
        // delta is 8, default page is 1: 12 entities stored in database
        Assert.assertEquals(2, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePage2 = hPacketRestApi.findAllHPacketPaginated(delta, 2);
        HyperIoTPaginableResult<HPacket> listHPacketsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage2.getResults().isEmpty());
        Assert.assertEquals(4, listHPacketsPage2.getResults().size());
        Assert.assertEquals(delta, listHPacketsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHPacketsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPacketsPage2.getNextPage());
        // delta is 8, page is 2: 12 entities stored in database
        Assert.assertEquals(2, listHPacketsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test63_findAllHPacketPaginatedShouldWorkIfPageIsZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, hadmin find all HPackets with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 4;
        int page = 0;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 5;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(delta, listHPackets.getResults().size());
        Assert.assertEquals(delta, listHPackets.getDelta());
        Assert.assertEquals(defaultPage, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHPackets.getNextPage());
        // delta is 4, default page is 1: 5 entities stored in database
        Assert.assertEquals(2, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponsePage2 = hPacketRestApi.findAllHPacketPaginated(delta, 2);
        HyperIoTPaginableResult<HPacket> listHPacketsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage2.getResults().isEmpty());
        Assert.assertEquals(1, listHPacketsPage2.getResults().size());
        Assert.assertEquals(delta, listHPacketsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHPacketsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPacketsPage2.getNextPage());
        // delta is 4, page is 2: 5 entities stored in database
        Assert.assertEquals(2, listHPacketsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test64_deleteHPacketNotDeleteInCascadeHProjectOrHDevice() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin deletes HPacket with the following call deleteHPacket. This call
        // not removes in cascade mode HDevice or HProject
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());

        // checks if HDevice already exists
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseHDevice = hDeviceRestApi.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseHDevice.getStatus());
        Assert.assertEquals(hdevice.getId(), ((HDevice) restResponseHDevice.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseHDevice.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HDevice) restResponseHDevice.getEntity()).getProject().getUser().getId());

        // checks if HProject already exists
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponseHProject = hProjectRestApi.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseHProject.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponseHProject.getEntity()).getUser().getId());
    }


    @Test
    public void test65_deleteHDeviceRemoveInCascadeHPacketsButNotDeleteHProject() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin deletes HDevice with the following call deleteHDevice. This call
        // removes in cascade mode any HPackets but not delete HProject
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseHDevice = hDeviceRestApi.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseHDevice.getStatus());
        Assert.assertNull(restResponseHDevice.getEntity());

        // checks if hpackets already exists
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseHPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseHPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket1.getEntity()).getType());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseHPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseHPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket2.getEntity()).getType());

        // checks if HProject already exists
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponseHProject = hProjectRestApi.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseHProject.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponseHProject.getEntity()).getUser().getId());
    }


    @Test
    public void test66_deleteHProjectRemoveInCascadeHDeviceAndHPackets() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin deletes HProject with the following call deleteHProject. This call
        // removes in cascade mode any associated HDevice and HPackets
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
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

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponseHProject = hProjectRestApi.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertNull(restResponseHProject.getEntity());

        // checks if hdevice already exists
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseHDevice = hDeviceRestApi.findHDevice(hdevice.getId());
        Assert.assertEquals(404, restResponseHDevice.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHDevice.getEntity()).getType());

        // checks if hpackets already exists
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseHPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseHPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket1.getEntity()).getType());

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseHPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseHPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket2.getEntity()).getType());
    }

    @Test
    public void test67_saveHPacketShouldFailIfNameIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but name is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName(createStringFieldWithSpecifiedLenght(256));
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
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test68_saveHPacketShouldFailIfVersionIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but version is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion(createStringFieldWithSpecifiedLenght(256));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test69_saveHPacketShouldFailIfTimestampFieldIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but timestamp field is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        hpacket.setTimestampField(createStringFieldWithSpecifiedLenght(256));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-timestampfield", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getTimestampField(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test70_saveHPacketShouldFailIfTimestampFormatIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to save HPacket with the following call saveHPacket,
        // but timestamp format is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        hpacket.setTimestampFormat(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-timestampformat", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getTimestampFormat(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test71_updateHPacketShouldFailIfNameIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // but name is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setName(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test72_updateHPacketShouldFailIfVersionIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // but version is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setVersion(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-version", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getVersion(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test73_updateHPacketShouldFailIfTimestampFieldIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // but timestamp field is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setTimestampField(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-timestampfield", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getTimestampField(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test74_updateHPacketShouldFailIfTimestampFormatIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket, but
        // but timestamp format is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setTimestampFormat(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-timestampformat", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getTimestampFormat(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test75_addHPacketFieldsShouldFailIfNameIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but name is null
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName(null);
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(1).getField());

    }

    @Test
    public void test76_addHPacketFieldsShouldFailIfNameIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but name is empty
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(field1.getName(), ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test77_addHPacketFieldsShouldFailIfNameIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but name is malicious code
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("<script>console.log('hello')</script>");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(field1.getName(), ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test78_addHPacketFieldsShouldFailIfNameIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but name is greater than 255 chars
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName(createStringFieldWithSpecifiedLenght(256));
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(field1.getName(), ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test79_addHPacketFieldsShouldFailIfDescriptionIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but description is malicious code
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("Temperature");
        field1.setDescription("<script>console.log('hello')</script>");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-description", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(field1.getDescription(), ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test80_addHPacketFieldsShouldFailIfDescriptionIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but description is greater than 255 chars
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature");
        field1.setDescription(createStringFieldWithSpecifiedLenght(256));
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-description", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(field1.getDescription(), ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test81_addHPacketFieldsShouldFailIfTypeIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but type is null
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature");
        field1.setDescription("description");
        field1.setType(null);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-type", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());

    }

    @Test
    public void test82_addHPacketFieldsShouldFailIfMultiplicityIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but multiplicity is null
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature");
        field1.setDescription("description");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(null);
        field1.setValue(24.0);

        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-multiplicity", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());

    }

    @Test
    public void test82_addHPacketFieldsShouldFailIfUnitIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but unit is malicious code
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature");
        field1.setDescription("description");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);
        field1.setUnit("<script>console.log('hello')</script>");
        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-unit", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(field1.getUnit(), ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test83_addHPacketFieldsShouldFailIfUnitIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin add HPacket fields with the following call addHPacketField
        // but unit is greater than 255 chars.
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

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
        Response restSaveHPacket = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restSaveHPacket.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restSaveHPacket.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getId());
        Assert.assertEquals(hproject.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacket) restSaveHPacket.getEntity()).getDevice().getProject().getUser().getId());


        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature");
        field1.setDescription("description");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);
        field1.setUnit(createStringFieldWithSpecifiedLenght(256));
        // add field1
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(422, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-unit", ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(field1.getUnit(), ((HyperIoTBaseError) responseAddField1.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test84_updateHpacketFieldsShouldFailIfNameIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but name is null
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setName(null);

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(1).getField());

    }

    @Test
    public void test85_updateHpacketFieldsShouldFailIfNameIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but name is empty
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setName("");

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(updateField.getName(), ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test86_updateHpacketFieldsShouldFailIfNameIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but name is malicious code
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setName("<script>console.log('hello')</script>");

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(updateField.getName(), ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test87_updateHpacketFieldsShouldFailIfNameIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but name is greater than 255 chars .
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setName(createStringFieldWithSpecifiedLenght(256));

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-name", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(updateField.getName(), ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test88_updateHpacketFieldsShouldFailIfDescriptionIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but description is malicious code .
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setDescription("<script>console.log('hello')</script>");

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-description", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(updateField.getDescription(), ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }


    @Test
    public void test89_updateHpacketFieldsShouldFailIfDescriptionIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but description is greater than 255 chars .
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setDescription(createStringFieldWithSpecifiedLenght(256));

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-description", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(updateField.getDescription(), ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }

    @Test
    public void test90_updateHpacketFieldsShouldFailIfTypeIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but type is null
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setType(null);

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-type", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());

    }


    @Test
    public void test91_updateHpacketFieldsShouldFailIfMultiplicityIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but multiplicity is null
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setMultiplicity(null);

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-multiplicity", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());

    }

    @Test
    public void test92_updateHpacketFieldsShouldFailIfDescriptionIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but unit is malicious code .
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setUnit("<script>console.log('hello')</script>");

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-unit", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(updateField.getUnit(), ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getInvalidValue());

    }


    @Test
    public void test93_updateHpacketFieldsShouldFailIfUnitIsGreaterThan255Chars() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin update HPacket fields with the following call updateHPacketField
        // but unit is greater than 255 chars .
        // response status code '422' (HyperIoTValidationException).
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(adminUser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        updateField.setUnit(createStringFieldWithSpecifiedLenght(256));

        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(422, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacketfield-unit", ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(updateField.getUnit(), ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getValidationErrors().get(0).getInvalidValue());

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

            Set<HPacketField> fields = ((HPacket) restResponse.getEntity()).getFields();
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
    }

}

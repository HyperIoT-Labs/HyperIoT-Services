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
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.actions.HyperIoTHDeviceAction;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
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

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHPacketRestWithPermissionTest extends KarafTestSupport {

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
    /*
    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }
     */

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
        huser = createHUser(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HPacket Module works!", restResponse.getEntity());
    }


    @Test
    public void test02_saveHPacketWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, save HPacket with the following call saveHPacket
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        HDevice hdevice = createHDevice(hproject);
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
        Assert.assertEquals(huser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test03_saveHPacketWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to save HPacket with the following call
        // saveHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        HDevice hdevice = createHDevice(hproject);
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
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test04_updateHPacketWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, update HPacket with the following call updateHPacket
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setVersion("version edited");
        this.impersonateUser(hPacketRestApi, huser);
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
        Assert.assertEquals(huser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test05_updateHPacketWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to update HPacket with the following call
        // updateHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setVersion("version edited");
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test06_findHPacketWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, find HPacket with the following call
        // findHPacket
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test07_findHPacketWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to find HPacket with the following call
        // findHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findHPacket(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test08_findAllHPacketWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, find all HPacket with the following call
        // findAllHPacket
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        this.impersonateUser(hPacketRestApi, huser);
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
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacketFound = true;
            }
        }
        Assert.assertTrue(hpacketFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test09_findAllHPacketWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to find all HPacket with the following call
        // findAllHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacket();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test10_deleteHPacketWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, delete HPacket with the following call
        // deleteHPacket
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test11_deleteHPacketWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to delete HPacket with the following call
        // deleteHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test12_saveHPacketWithPermissionShouldFailIfNameIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName(null);
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
    public void test13_saveHPacketWithPermissionShouldFailIfNameIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("");
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
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test14_saveHPacketWithPermissionShouldFailIfNameIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("</script>");
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
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(hpacket.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test15_saveHPacketWithPermissionShouldFailIfHPacketTypeIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but packet type is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(null);
        hpacket.setVersion("version" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test16_saveHPacketWithPermissionShouldFailIfHPacketFormatIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but packet format is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(null);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.INPUT);
        hpacket.setVersion("version" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-format", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test17_saveHPacketWithPermissionShouldFailIfHPacketSerializationIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but packet serialization is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.CSV);
        hpacket.setSerialization(null);
        hpacket.setType(HPacketType.INPUT);
        hpacket.setVersion("version" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-serialization", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test18_saveHPacketWithPermissionShouldFailIfVersionIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but version is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion(null);
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test19_saveHPacketWithPermissionShouldFailIfVersionIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but version is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("");
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test20_saveHPacketWithPermissionShouldFailIfVersionIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but version is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("</script>");
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test21_saveHPacketWithPermissionShouldFailIfHDeviceIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but HDevice is null
        // response status code '404' HyperIoTEntityNotFound (Cannot save an HPacket without specify the HDevice to which it's related).
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(null);
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
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test22_saveHPacketWithPermissionShouldFailIfHDeviceBelongsToAnotherUser() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the
        // following call saveHPacket, but HDevice belongs to another user
        // response status code  '404' HyperIoTUnauthorizedException, because the user has not visibility on the device related to the packet
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        HDevice anotherHDevice = createHDevice(anotherHProject);
        Assert.assertNotEquals(0, anotherHDevice.getId());
        Assert.assertEquals(anotherHProject.getId(), anotherHDevice.getProject().getId());
        Assert.assertEquals(huser2.getId(), anotherHDevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(anotherHDevice);
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
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());

        Assert.assertEquals(0, hpacket.getId());
        Assert.assertNotEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), hpacket.getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), anotherHDevice.getProject().getUser().getId());
        Assert.assertEquals(huser2.getUsername(), anotherHDevice.getProject().getUser().getUsername());
        Assert.assertNotEquals(huser.getId(), anotherHDevice.getProject().getUser().getId());
        Assert.assertNotEquals(huser.getUsername(), anotherHDevice.getProject().getUser().getUsername());
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getUsername(), huser2.getUsername());
    }


    @Test
    public void test23_updateHPacketWithPermissionShouldFailIfNameIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setName(null);
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test24_updateHPacketWithPermissionShouldFailIfNameIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setName("");
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test25_updateHPacketWithPermissionShouldFailIfNameIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setName("javascript:");
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test26_updateHPacketWithPermissionShouldFailIfHPacketTypeIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but packet type is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setType(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test27_updateHPacketWithPermissionShouldFailIfHPacketFormatIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but packet format is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        hpacket.setFormat(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-format", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test28_updateHPacketWithPermissionShouldFailIfHPacketSerializationIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but packet serialization is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setSerialization(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("hpacket-serialization", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test29_updateHPacketWithPermissionShouldFailIfVersionIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but version is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setVersion(null);
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test30_updateHPacketWithPermissionShouldFailIfVersionIsEmpty() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but version is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setVersion("");
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test31_updateHPacketWithPermissionShouldFailIfVersionIsMaliciousCode() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but version is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setVersion("</script>");
        this.impersonateUser(hPacketRestApi, huser);
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
    public void test32_updateHPacketWithPermissionShouldFailIfHDeviceIsNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but HDevice is null
        // response status code '404' HyperIoTEntityNotFound. (Cannot update an HPacket without specifiy the HDevice to which it's related)
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        hpacket.setDevice(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test33_updateHPacketWithPermissionShouldFailIfHDeviceBelongsToAnotherUser() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // updateHPacket, but HDevice belongs to another HUser
        // response status code  '404' HyperIoTUnauthorizedException, because the user has not visibility on the device related to the packet
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        HDevice anotherHDevice = createHDevice(anotherHProject);
        Assert.assertNotEquals(0, anotherHDevice.getId());
        Assert.assertEquals(anotherHProject.getId(), anotherHDevice.getProject().getId());
        Assert.assertEquals(huser2.getId(), anotherHDevice.getProject().getUser().getId());

        hpacket.setDevice(anotherHDevice);

        Assert.assertNotEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());

        Assert.assertEquals(huser2.getId(), anotherHDevice.getProject().getUser().getId());
        Assert.assertEquals(huser2.getUsername(), anotherHDevice.getProject().getUser().getUsername());
        Assert.assertNotEquals(huser.getId(), anotherHDevice.getProject().getUser().getId());
        Assert.assertNotEquals(huser.getUsername(), anotherHDevice.getProject().getUser().getUsername());
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getUsername(), huser2.getUsername());
    }


    @Test
    public void test34_findHPacketWithPermissionShouldFailIfEntityNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to find HPacket with the following call
        // findHPacket, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findHPacket(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test35_findHPacketNotFoundWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to find HPacket not found with the following
        // call findHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findHPacket(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test36_deleteHPacketWithPermissionShouldFailIfEntityNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to delete HPacket with the following call
        // deleteHPacket, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacket(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test37_deleteHPacketNotFoundWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to delete HPacket not found with the
        // following call deleteHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacket(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test38_updateHPacketWithPermissionShouldFailIfEntityNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to update HPacket with the following call
        // updateHPacket, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject project = createHProject(huser);
        HDevice device = createHDevice(project);
        // entity isn't stored in database
        HPacket hpacket = new HPacket();
        hpacket.setDevice(device);
        hpacket.setType(HPacketType.IO);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test39_updateHPacketNotFoundWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to update HPacket not found with the
        // following call updateHPacket
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject project = createHProject(huser);
        HDevice device = createHDevice(project);
        // entity isn't stored in database
        HPacket hpacket = new HPacket();
        hpacket.setDevice(device);
        hpacket.setName("name" + UUID.randomUUID().toString().replaceAll("-", ""));
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test40_saveHPacketWithPermissionShouldFailIfEntityIsDuplicated() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to save HPacket with the following call
        // saveHPacket, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

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

        this.impersonateUser(hPacketRestApi, huser);
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
                Assert.assertEquals("name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hpacketNameIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("device_id")) {
                Assert.assertEquals("device_id",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hdeviceIdIsDuplicated = true;
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("version")) {
                Assert.assertEquals("version",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                hpacketVersionIsDuplicated = true;
            }
        }
        Assert.assertTrue(hpacketNameIsDuplicated);
        Assert.assertTrue(hdeviceIdIsDuplicated);
        Assert.assertTrue(hpacketVersionIsDuplicated);
    }


    @Test
    public void test41_saveHPacketDuplicatedWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to save HPacket duplicated with the
        // following call saveHPacket
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

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
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.saveHPacket(duplicateHPacket);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test42_findListHDevicePacketsWithPermissionShouldWork() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, find list HDevice packets with the following call
        // getHDevicePacketList
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestService, huser);
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
        Assert.assertEquals(huser.getId(), listHDevicePackets.get(0).getDevice().getProject().getUser().getId());
    }


    @Test
    public void test43_findListHDevicePacketsWithPermissionShouldWorkAndListHDevicePacketsIsEmpty() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, find list HDevice packets with the following call
        // getHDevicePacketList. listHDevicePackets is empty
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        this.impersonateUser(hPacketRestService, huser);
        Response restResponse = hPacketRestService.getHDevicePacketList(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHDevicePackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertEquals(0, listHDevicePackets.size());
        Assert.assertTrue(listHDevicePackets.isEmpty());
    }


    @Test
    public void test44_findListHDevicePacketsWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to find list HDevice packets with the
        // following call getHDevicePacketList
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestService, huser);
        Response restResponse = hPacketRestService.getHDevicePacketList(hdevice.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test45_findListHDevicePacketsWithPermissionShouldFailIfEntityNotFound() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // HUser, with permission, tries to find list HDevice packets with the following
        // call getHDevicePacketList, but HDevice not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        huser = createHUser(action);
        this.impersonateUser(hPacketRestService, huser);
        Response restResponse = hPacketRestService.getHDevicePacketList(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test46_findListHDevicePacketsNotFoundWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // HUser, without permission, tries to find list HDevice packets not found with
        // the following call getHDevicePacketList
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hPacketRestService, huser);
        Response restResponse = hPacketRestService.getHDevicePacketList(0);
        Assert.assertEquals(404, restResponse.getStatus());

    }


    @Test
    public void test47_addHPacketFieldsWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, add HPacket fields with the following call addHPacketField
        // response status code '200'
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action1);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

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
        this.impersonateUser(hPacketRestApi, huser);
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(200, responseAddField1.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());

        // add field2
        this.impersonateUser(hPacketRestApi, huser);
        Response responseAddField2 = hPacketRestApi.addHPacketField(hpacket.getId(), field2);
        Assert.assertEquals(200, responseAddField2.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getDevice().getProject().getUser().getId());

        ArrayList<HPacketField> fields = new ArrayList<>(hpacket.getFields());
        //check restResponse field1 is equals to responseAddField1 field1
        Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
        Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() == ((HPacketField) responseAddField1.getEntity()).getId()));
        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

        //check restResponse field2 is equals to responseAddField2 field2
        Assert.assertEquals(field2.getId(), ((HPacketField) responseAddField2.getEntity()).getId());
        Assert.assertTrue(fields.stream().anyMatch(field -> field.getId() == ((HPacketField) responseAddField2.getEntity()).getId()));
        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());

        Assert.assertEquals(2, hpacket.getFields().size());
    }


    @Test
    public void test48_addHPacketFieldsWithPermissionShouldFailIfHPacketNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, tries to add HPacket fields with the following
        // call addHPacketField, but HPacket not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action1);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);

        HPacketField field1 = new HPacketField();
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        this.impersonateUser(hPacketRestApi, huser);
        Response responseAddField1 = hPacketRestApi.addHPacketField(0, field1);
        Assert.assertEquals(404, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
    }


    @Test
    public void test49_addHPacketFieldsWithoutPermissionShouldFailIfHPacketNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to add HPacket fields with the following
        // call addHPacketField
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);

        HPacketField field1 = new HPacketField();
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        this.impersonateUser(hPacketRestApi, huser);
        Response responseAddField1 = hPacketRestApi.addHPacketField(0, field1);
        Assert.assertEquals(404, responseAddField1.getStatus());
    }


    @Test
    public void test50_addHPacketFieldsWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to add HPacket fields with the
        // following call addHPacketField
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);

        hpacket.setFields(new HashSet<>() {
            {
                add(field1);
            }
        });

        this.impersonateUser(hPacketRestApi, huser);
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(403, responseAddField1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) responseAddField1.getEntity()).getType());
    }


    @Test
    public void test51_updateHpacketFieldsWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, update HPacket fields with the following call updateHPacketField
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());

        HPacketField updateField = field;
        Date timestamp = new Date();
        updateField.setDescription("Temperature edited in date " + timestamp);

        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(200, restResponseUpdateField.getStatus());
        Assert.assertEquals(hpacket.getEntityVersion() + 1,
                (((HPacketField) restResponseUpdateField.getEntity()).getEntityVersion()));
        Assert.assertEquals("Temperature edited in date " + timestamp,
                (((HPacketField) restResponseUpdateField.getEntity()).getDescription()));
    }


    @Test
    public void test52_updateHpacketFieldsWithPermissionShouldFailIfHPacketNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, tries to update HPacket fields with the following
        // call updateHPacketField, but HPacket not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        Date timestamp = new Date();
        updateField.setDescription("Edit failed in date " + timestamp);

        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(0, updateField);
        Assert.assertEquals(404, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
    }


    @Test
    public void test53_updateHpacketFieldsWithPermissionShouldFailIfHPacketNotHaveField() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, tries to update HPacket fields with the following call
        // updateHPacketField, but HPacket not have a field
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action1);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField updateField = new HPacketField();
        Date timestamp = new Date();
        updateField.setDescription("Edit failed in date " + timestamp);

        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(404, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
    }


    @Test
    public void test54_updateHpacketFieldsWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to update HPacket fields with the following
        // call updateHPacketField
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        Date timestamp = new Date();
        updateField.setDescription("Temperature edited in date " + timestamp);

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(403, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
    }


    @Test
    public void test55_updateHpacketFieldsWithoutPermissionShouldFailIfHPacketNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to update HPacket fields with the following
        // call updateHPacketField, but HPacket not found
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().iterator().next();
        Assert.assertNotEquals(0, field.getId());
        Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field.getPacket().getDevice().getProject().getUser().getId());

        HPacketField updateField = field;
        Date timestamp = new Date();
        updateField.setDescription("Edit failed in date " + timestamp);

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(0, updateField);
        Assert.assertEquals(404, restResponseUpdateField.getStatus());
    }


    @Test
    public void test56_updateHpacketFieldsWithoutPermissionShouldFailIfHPacketNotHaveField() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to update HPacket fields with the following call
        // updateHPacketField, but HPacket not have a field
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        // HPacketField isn't stored in database
        HPacketField updateField = new HPacketField();
        Date timestamp = new Date();
        updateField.setDescription("Edit failed in date " + timestamp);

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseUpdateField = hPacketRestApi.updateHPacketField(hpacket.getId(), updateField);
        Assert.assertEquals(403, restResponseUpdateField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseUpdateField.getEntity()).getType());
    }


    @Test
    public void test57_deleteHpacketFieldsWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, deletes HPacket fields with the following call deleteHPacketField
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        ArrayList<HPacketField> fields = new ArrayList<>(hpacket.getFields());
        HPacketField field1 = fields.get(0);
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field1.getPacket().getDevice().getProject().getUser().getId());

        HPacketField field2 = fields.get(1);
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field2.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response respDeleteHPacketField1 = hPacketRestApi.deleteHPacketField(field1.getId());
        Assert.assertEquals(200, respDeleteHPacketField1.getStatus());
        Assert.assertNull(respDeleteHPacketField1.getEntity());
    }


    @Test
    public void test58_deleteHpacketFieldsWithPermissionShouldFailIfHPacketFieldNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, tries to delete HPacket fields with the following call
        // deleteHPacketField, but HPacketField not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        addPermissionToFindPacket(huser);
        this.impersonateUser(hPacketRestApi, huser);
        Response respDeleteHPacketField = hPacketRestApi.deleteHPacketField(0);
        Assert.assertEquals(404, respDeleteHPacketField.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) respDeleteHPacketField.getEntity()).getType());
    }


    @Test
    public void test59_deleteHpacketFieldsWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to delete HPacket fields with the following call deleteHPacketField
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        ArrayList<HPacketField> fields = new ArrayList<>(hpacket.getFields());
        HPacketField field1 = fields.get(0);
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field1.getPacket().getDevice().getProject().getUser().getId());

        HPacketField field2 = fields.get(1);
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field2.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacketField(field1.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test60_deleteHpacketFieldsWithoutPermissionShouldFailIfHPacketFieldNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to delete HPacket fields with the following call
        // deleteHPacketField, but HPacketField not found
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacketField(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test61_findTreeFieldsWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, finds tree fields with the following call findTreeFields
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        ArrayList<HPacketField> fields = new ArrayList<>(hpacket.getFields());
        HPacketField field1 = fields.get(0);
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field1.getPacket().getDevice().getProject().getUser().getId());

        HPacketField field2 = fields.get(1);
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field2.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
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
                Assert.assertEquals(huser.getId(), field.getPacket().getDevice().getProject().getUser().getId());
                field1Found = true;
            }
            if (field2.getId() == field.getId()) {
                Assert.assertEquals(hpacket.getId(), field.getPacket().getId());
                Assert.assertEquals(hdevice.getId(), field.getPacket().getDevice().getId());
                Assert.assertEquals(hproject.getId(), field.getPacket().getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(), field.getPacket().getDevice().getProject().getUser().getId());
                field2Found = true;
            }
        }
        Assert.assertTrue(field1Found);
        Assert.assertTrue(field2Found);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test62_findTreeFieldsWithPermissionShouldFailIfFieldNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, finds tree fields with the following call findTreeFields,
        // but fields not found. This call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findTreeFields(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacketField> listHPacketFields = restResponse.readEntity(new GenericType<List<HPacketField>>() {
        });
        Assert.assertTrue(listHPacketFields.isEmpty());
        Assert.assertEquals(0, listHPacketFields.size());
    }


    @Test
    public void test63_findTreeFieldsWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to find tree fields with the following call findTreeFields
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        ArrayList<HPacketField> fields = new ArrayList<>(hpacket.getFields());
        HPacketField field1 = fields.get(0);
        Assert.assertNotEquals(0, field1.getId());
        Assert.assertEquals(hpacket.getId(), field1.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field1.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field1.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field1.getPacket().getDevice().getProject().getUser().getId());

        HPacketField field2 = fields.get(1);
        Assert.assertNotEquals(0, field2.getId());
        Assert.assertEquals(hpacket.getId(), field2.getPacket().getId());
        Assert.assertEquals(hdevice.getId(), field2.getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(), field2.getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), field2.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findTreeFields(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test64_findTreeFieldsNotFoundWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, finds tree fields with the following call findTreeFields,
        // but fields not found.
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findTreeFields(hpacket.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test65_updateHPacketWithPermissionShouldFailIfEntityIsDuplicated() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // hadmin tries to update HPacket with the following call updateHPacket,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDevice(huser);
        addPermissionToFindPacket(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacket duplicateHPacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, duplicateHPacket.getId());
        Assert.assertEquals(hdevice.getId(), duplicateHPacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), duplicateHPacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), duplicateHPacket.getDevice().getProject().getUser().getId());

        duplicateHPacket.setName(hpacket.getName());
        duplicateHPacket.setDevice(hpacket.getDevice());
        duplicateHPacket.setVersion(hpacket.getVersion());

        Assert.assertEquals(hpacket.getName(), duplicateHPacket.getName());
        Assert.assertEquals(hdevice.getId(), duplicateHPacket.getDevice().getId());
        Assert.assertEquals(hpacket.getVersion(), duplicateHPacket.getVersion());

        this.impersonateUser(hPacketRestApi, huser);
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
    public void test66_findAllHPacketByProjectIdWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, finds all HPacket by hprojectId with the following
        // call findAllHPacketByProjectId
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
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

        this.impersonateUser(hPacketRestApi, huser);
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
    public void test67_findAllHPacketByProjectIdWithPermissionShouldWorkIfHProjectNotHaveHPacket() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, finds all HPacket by hprojectId with the following call findAllHPacketByProjectId
        // but hproject not have a hpacket. This call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertTrue(listHPackets.isEmpty());
        Assert.assertEquals(0, listHPackets.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test68_findAllHPacketByProjectIdWithPermissionShouldFailIfHProjectNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, tries to find all HPacket by hprojectId with the following
        // call findAllHPacketByProjectId, but hproject not found.
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
        huser = createHUser(action);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test69_findAllHPacketByProjectIdWithoutPermissionShouldPermission() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to find all HPacket by hprojectId with the following
        // call findAllHPacketByProjectId
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

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test70_findAllHPacketByProjectIdWithoutPermissionShouldWorkIfHProjectNotHaveHPacket() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to find all HPacket by hprojectId with the
        // following call findAllHPacketByProjectId
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test71_findAllHPacketByProjectIdWithoutPermissionShouldFailIfHProjectNotFound() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, without permission, tries to find all HPacket by hprojectId with the following
        // call findAllHPacketByProjectId, but hproject not found.
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test72_findAllHPacketPaginatedWithPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all
        // HPackets with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 9;
        int page = 1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(defaultDelta, hpackets.size());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(delta, listHPackets.getResults().size());
        Assert.assertEquals(delta, listHPackets.getDelta());
        Assert.assertEquals(page, listHPackets.getCurrentPage());
        Assert.assertEquals(page + 1, listHPackets.getNextPage());
        // delta is 9, page 1: 10 entities stored in database
        Assert.assertEquals(2, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponsePage2 = hPacketRestApi.findAllHPacketPaginated(delta, 2);
        HyperIoTPaginableResult<HPacket> listHPacketsPage1 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage1.getResults().isEmpty());
        Assert.assertEquals(1, listHPacketsPage1.getResults().size());
        Assert.assertEquals(delta, listHPacketsPage1.getDelta());
        Assert.assertEquals(page + 1, listHPacketsPage1.getCurrentPage());
        Assert.assertEquals(page, listHPacketsPage1.getNextPage());
        // delta is 9, page 2: 10 entities stored in database
        Assert.assertEquals(2, listHPacketsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test73_findAllHPacketPaginatedWithoutPermissionShouldFail() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser, without permission,
        // tries to find all HPackets with pagination
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test74_findAllHPacketPaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all HPackets with pagination
        // if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(defaultDelta, hpackets.size());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHPackets.getResults().size());
        Assert.assertEquals(defaultDelta, listHPackets.getDelta());
        Assert.assertEquals(defaultPage, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPackets.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test75_findAllHPacketPaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all HPackets with pagination
        // if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = -1;
        int page = 2;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 13;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(3, listHPackets.getResults().size());
        Assert.assertEquals(defaultDelta, listHPackets.getDelta());
        Assert.assertEquals(page, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPackets.getNextPage());
        // default delta is 10, page is 2: 13 entities stored in database
        Assert.assertEquals(2, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponsePage1 = hPacketRestApi.findAllHPacketPaginated(delta, 1);
        HyperIoTPaginableResult<HPacket> listHPacketsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHPacketsPage1.getCurrentPage());
        Assert.assertEquals(page, listHPacketsPage1.getNextPage());
        // default delta is 10, page is 1: 13 entities stored in database
        Assert.assertEquals(2, listHPacketsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test76_findAllHPacketPaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all HPackets with pagination
        // if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 0;
        int page = 3;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 27;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(7, listHPackets.getResults().size());
        Assert.assertEquals(defaultDelta, listHPackets.getDelta());
        Assert.assertEquals(page, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPackets.getNextPage());
        // default delta is 10, page is 3: 27 entities stored in database
        Assert.assertEquals(3, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponsePage1 = hPacketRestApi.findAllHPacketPaginated(delta, 1);
        HyperIoTPaginableResult<HPacket> listHPacketsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listHPacketsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHPacketsPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHPacketsPage1.getNextPage());
        // default delta is 10, page is 1: 27 entities stored in database
        Assert.assertEquals(3, listHPacketsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        // checks with page = 2
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponsePage2 = hPacketRestApi.findAllHPacketPaginated(delta, 2);
        HyperIoTPaginableResult<HPacket> listHPacketsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listHPacketsPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listHPacketsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHPacketsPage2.getCurrentPage());
        Assert.assertEquals(page, listHPacketsPage2.getNextPage());
        // default delta is 10, page is 2: 27 entities stored in database
        Assert.assertEquals(3, listHPacketsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test77_findAllHPacketPaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all HPackets with pagination
        // if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 6;
        int page = -1;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(defaultDelta, hpackets.size());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(delta, listHPackets.getResults().size());
        Assert.assertEquals(delta, listHPackets.getDelta());
        Assert.assertEquals(defaultPage, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listHPackets.getNextPage());
        // delta is 6, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponsePage2 = hPacketRestApi.findAllHPacketPaginated(delta, 2);
        HyperIoTPaginableResult<HPacket> listHPacketsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPacketsPage2.getResults().isEmpty());
        Assert.assertEquals(4, listHPacketsPage2.getResults().size());
        Assert.assertEquals(delta, listHPacketsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listHPacketsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPacketsPage2.getNextPage());
        // delta is 6, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listHPacketsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test78_findAllHPacketPaginatedWithPermissionShouldWorkIfPageIsZero() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all HPackets with pagination
        // if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 7;
        int page = 0;
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        int numbEntities = 6;
        for (int i = 0; i < numbEntities; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        Assert.assertEquals(numbEntities, hpackets.size());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listHPackets.getResults().size());
        Assert.assertEquals(delta, listHPackets.getDelta());
        Assert.assertEquals(defaultPage, listHPackets.getCurrentPage());
        Assert.assertEquals(defaultPage, listHPackets.getNextPage());
        // delta is 7, default page is 1: 6 entities stored in database
        Assert.assertEquals(1, listHPackets.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test79_deleteHPacketWithPermissionNotDeleteInCascadeHProjectOrHDevice() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, deletes HPacket with the following call deleteHPacket. This call
        // not removes in cascade mode HDevice or HProject
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks if HPacket has been deleted
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action1);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse1 = hPacketRestApi.findHPacket(hpacket.getId());
        Assert.assertEquals(404, restResponse1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse1.getEntity()).getType());

        // checks if HDevice already exists
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseHDevice = hDeviceRestApi.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseHDevice.getStatus());
        Assert.assertEquals(hdevice.getId(), ((HDevice) restResponseHDevice.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HDevice) restResponseHDevice.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((HDevice) restResponseHDevice.getEntity()).getProject().getUser().getId());

        // checks if HProject already exists
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action3 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action3);
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponseHProject = hProjectRestApi.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseHProject.getEntity()).getId());
        Assert.assertEquals(huser.getId(), ((HProject) restResponseHProject.getEntity()).getUser().getId());
    }


    @Test
    public void test80_deleteHDeviceWithPermissionRemoveInCascadeHPacketsButNotDeleteHProject() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, deletes HDevice with the following call deleteHDevice. This call
        // removes in cascade mode any HPackets but not delete HProject
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.REMOVE);
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

        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseHDevice = hDeviceRestApi.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponseHDevice.getStatus());
        Assert.assertNull(restResponseHDevice.getEntity());

        // checks if hpackets already exists
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action1);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseHPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseHPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket1.getEntity()).getType());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponseHPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseHPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket2.getEntity()).getType());

        // checks if HProject already exists
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action3 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action3);
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponseHProject = hProjectRestApi.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponseHProject.getEntity()).getId());
        Assert.assertEquals(huser.getId(), ((HProject) restResponseHProject.getEntity()).getUser().getId());
    }


    @Test
    public void test81_deleteHProjectWithPermissionRemoveInCascadeHDeviceAndHPackets() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, deletes HProject with the following call deleteHProject. This call
        // removes in cascade mode any associated HDevice and HPackets
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
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

        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponseHProject = hProjectRestApi.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponseHProject.getStatus());
        Assert.assertNull(restResponseHProject.getEntity());

        // checks if hdevice already exists
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action1);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseHDevice = hDeviceRestApi.findHDevice(hdevice.getId());
        Assert.assertEquals(404, restResponseHDevice.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHDevice.getEntity()).getType());

        // checks if hpackets already exists
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseHPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
        Assert.assertEquals(404, restResponseHPacket1.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket1.getEntity()).getType());

        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponseHPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
        Assert.assertEquals(404, restResponseHPacket2.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponseHPacket2.getEntity()).getType());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    //When User Save or Update a packet he must have the permission to find the device related to the packet.
    private Permission addPermissionToFindDevice(HUser huser) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(HDevice.class.getName(), HyperIoTCrudAction.FIND);
        Permission permission = new Permission();
        permission.setName(HDevice.class.getName() + " assigned to huser_id " + huser.getId());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertEquals(HDevice.class.getName() + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }

    //When User delete an HPacketField he must have the permission to find the packet related to the packet field.
    private Permission addPermissionToFindPacket(HUser huser) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(HPacket.class.getName(), HyperIoTCrudAction.FIND);
        Permission permission = new Permission();
        permission.setName(hPacketResourceName + " assigned to huser_id " + huser.getId());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertEquals(hPacketResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
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
            Permission permission = utilGrantPermission(huser, role, action);
            Assert.assertNotEquals(0, permission.getId());
            Assert.assertEquals(hPacketResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
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
                permission.setName(hPacketResourceName + " assigned to huser_id " + huser.getId());
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
        Assert.assertEquals(hPacketResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
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
        removeDefaultPermission(huser);
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
        removeDefaultPermission(ownerHUser);
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
        removeDefaultPermission(ownerHUser);
        return hpacket;
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


    // HPacket is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;
    private HUser huser2;

    @After
    public void afterTest() {
        // Remove all projects and delete in cascade all associated entities (HDevice, HPacket, HPacketField) in every tests
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

}

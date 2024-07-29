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
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
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
import it.acsoftware.hyperiot.role.model.Role;
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

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHPacketRestWithDefaultPermissionTest extends KarafTestSupport {

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
    public void test001_hpacketModuleShouldWorkIfHUserIsActive() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call checkModuleWorking checks if HPacket module working
        // correctly
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HPacket Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_hpacketModuleShouldWorkIfHUserIsNotActive() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // the following call checkModuleWorking checks if HPacket module working
        // correctly
        huser = huserWithDefaultPermissionInHyperIoTFramework(false);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertFalse(huser.isActive());
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HPacket Module works!", restResponse.getEntity());
    }


    // it.acsoftware.hyperiot.hpacket.model.HPacket (63)
    // save                     1
    // update                   2
    // remove                   4
    // find                     8
    // find_all                 16
    // fields_management        32


    // HPacket action save: 1
    @Test
    public void test003_saveHPacketWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, save HPacket with the following call saveHPacket
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


    // HPacket action update: 2
    @Test
    public void test004_updateHPacketWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, update HPacket with the following call updateHPacket
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Date date = new Date();
        hpacket.setVersion("version edited in date: " + date);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hpacket.getEntityVersion() + 1,
                (((HPacket) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals(hpacket.getVersion(),
                (((HPacket) restResponse.getEntity()).getVersion()));
        Assert.assertEquals(hproject.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    // HPacket action remove: 4
    @Test
    public void test005_deleteHPacketWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, delete HPacket with the following call
        // deleteHPacket
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HPacket action find: 8
    @Test
    public void test006_findHPacketWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, find HPacket with the following call
        // findHPacket
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, true);
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


    // HPacket action find-all: 16
    @Test
    public void test007_findAllHPacketWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, find all HPacket with the following call
        // findAllHPacket
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

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacket();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(2, listHPackets.size());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
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
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
    }


    // HPacket action find-all: 16
    @Test
    public void test008_findAllHPacketPaginatedWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all
        // HPackets with pagination
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
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
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(defaultDelta, defaultPage);
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


    // HPacket action save: 1 (fieldHPacket)
    @Test
    public void test009_addHPacketFieldWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, add new fieldHPacket with the following call
        // addHPacketField
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

        HPacket hpacket = createHPacketAndAddHPacketField(hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        Assert.assertNull(hpacket.getFields());

        HPacketField fieldHPacket = new HPacketField();
        fieldHPacket.setPacket(hpacket);
        fieldHPacket.setName("temperature" + UUID.randomUUID().toString().replaceAll("-", ""));
        fieldHPacket.setDescription("Temperature");
        fieldHPacket.setType(HPacketFieldType.DOUBLE);
        fieldHPacket.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        fieldHPacket.setValue(24.0);

        hpacket.setFields(new HashSet<>() {
            {
                add(fieldHPacket);
            }
        });

        this.impersonateUser(hPacketRestApi, huser);
        Response responseAddField = hPacketRestApi.addHPacketField(hpacket.getId(), fieldHPacket);
        Assert.assertEquals(200, responseAddField.getStatus());
        //check if fieldHPacket has been saved inside hpacket
        Assert.assertNotEquals(0, ((HPacketField) responseAddField.getEntity()).getId());
        Assert.assertEquals(fieldHPacket.getName(), ((HPacketField) responseAddField.getEntity()).getName());
        Assert.assertEquals(fieldHPacket.getDescription(), ((HPacketField) responseAddField.getEntity()).getDescription());
        Assert.assertEquals(fieldHPacket.getType(), ((HPacketField) responseAddField.getEntity()).getType());
        Assert.assertEquals(fieldHPacket.getMultiplicity(), ((HPacketField) responseAddField.getEntity()).getMultiplicity());

        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), ((HPacketField) responseAddField.getEntity()).getPacket().getDevice().getProject().getUser().getId());
    }


    // HPacket action update: 2 (fieldHPacket)
    @Test
    public void test010_updateHPacketFieldWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with default permission, update HPacket fields with the following call updateHPacketField
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

        Date date = new Date();
        field.setDescription("Temperature edited in date " + date);

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacketField(hpacket.getId(), field);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(field.getEntityVersion() + 1,
                (((HPacketField) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals("Temperature edited in date " + date,
                (((HPacketField) restResponse.getEntity()).getDescription()));

        Assert.assertEquals(hpacket.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
    }


    // HPacket action remove: 4 (fieldHPacket)
    @Test
    public void test011_deleteHPacketFieldWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with default permission, delete HPacket field with the following call deleteHPacketField
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

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacketField(field.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HPacket action find-all: 16 (fieldHPacket)
    @Test
    public void test012_findTreeFieldsWithDefaultPermissionShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with default permission, finds tree fields with the following call findTreeFields
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
        Assert.assertEquals(200, restResponse.getStatus());
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
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


    // HPacket is Owned Resource: only huser is able to find/findAll his entities
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
            boolean resourceNameHPacket = false;
            for (Permission permission : listPermissions) {
                if (permission.getEntityResourceName().contains(permissionHPacket)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionHPacket, permission.getEntityResourceName());
                    Assert.assertEquals(permissionHPacket + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(63, permission.getActionIds());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameHPacket = true;
                }
            }
            Assert.assertTrue(resourceNameHPacket);
        }
        return huser;
    }

    private HProject createHProject(HUser huser) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HProject hproject = new HProject();
        hproject.setName("Project " + UUID.randomUUID());
        hproject.setDescription("Project of user: " + huser.getUsername());
        hproject.setUser(huser);
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
            Assert.assertEquals(2, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        return hpacket;
    }

}

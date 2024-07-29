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
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
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
public class HyperIoTHProjectWithDefaultPermissionRestTest extends KarafTestSupport {

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
    public void test001_hprojectModuleShouldWorkIfHUserIsActive() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call checkModuleWorking checks if HProject module working
        // correctly
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProject Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_hprojectModuleShouldWorkIfHUserIsNotActive() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // the following call checkModuleWorking checks if HProject module working
        // correctly, if HUser not active
        huser = huserWithDefaultPermissionInHyperIoTFramework(false);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertFalse(huser.isActive());
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProject Module works!", restResponse.getEntity());
    }


      // HProject action save: 1
    @Test
    public void test003_saveHProjectWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, save HProject with the following call saveHProject
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hprojectRestService, huser);
        HProject hproject = new HProject();
        hproject.setName("Project "+ UUID.randomUUID() +" of user: " + huser.getUsername());
        Date date = new Date();
        hproject.setDescription("Description inserted in date: " + date);
        hproject.setUser(huser);
        Response restResponseSaveProject = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponseSaveProject.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponseSaveProject.getEntity()).getId());
        Assert.assertEquals(hproject.getName(),
                ((HProject) restResponseSaveProject.getEntity()).getName());
        Assert.assertEquals("Description inserted in date: " + date,
                ((HProject) restResponseSaveProject.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponseSaveProject.getEntity()).getUser().getId());
    }



    // HProject action update: 2
    @Test
    public void test005_updateHProjectWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, update HProject with the following call updateHProject
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Date date = new Date();
        hproject.setDescription("Description edited in date: " + date);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hproject.getEntityVersion() + 1,
                (((HProject) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals("Description edited in date: " + date,
                (((HProject) restResponse.getEntity()).getDescription()));
        Assert.assertEquals(hproject.getId(),
                ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponse.getEntity()).getUser().getId());
    }


    // HProject action remove: 4
    @Test
    public void test006_deleteHProjectWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, delete HProject with the following call deleteHProject
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HProject action find: 8
    @Test
    public void test007_findHProjectWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find HProject with the following call findHProject
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals(hproject.getDescription(), ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }


    // HProject action find-all: 16
    @Test
    public void test008_findAllHProjectWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find all HProject with the following call findAllHProject
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject1 = createHProject(huser);
        Assert.assertNotEquals(0, hproject1.getId());
        Assert.assertEquals(huser.getId(), hproject1.getUser().getId());

        HProject hproject2 = createHProject(huser);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser.getId(), hproject2.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findAllHProject();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        Assert.assertEquals(2, listHProjects.size());
        boolean hprojectFound1 = false;
        boolean hprojectFound2 = false;
        for (HProject project : listHProjects) {
            if (hproject1.getId() == project.getId()) {
                Assert.assertEquals(hproject1.getId(), project.getId());
                Assert.assertEquals(hproject1.getName(), project.getName());
                Assert.assertEquals(hproject1.getDescription(), project.getDescription());
                Assert.assertEquals(huser.getId(), project.getUser().getId());
                hprojectFound1 = true;
            }
            if (hproject2.getId() == project.getId()) {
                Assert.assertEquals(hproject2.getId(), project.getId());
                Assert.assertEquals(hproject2.getName(), project.getName());
                Assert.assertEquals(hproject2.getDescription(), project.getDescription());
                Assert.assertEquals(huser.getId(), project.getUser().getId());
                hprojectFound2 = true;
            }
        }
        Assert.assertTrue(hprojectFound1);
        Assert.assertTrue(hprojectFound2);
    }


    // HProject action find-all: 16
    @Test
    public void test009_findAllHProjectPaginatedWithDefaultPermissionShouldWork() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all
        // HProjects with pagination
        // response status code '200'
        int delta = 5;
        int page = 2;
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<HProject> hprojects = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        Assert.assertEquals(defaultDelta, hprojects.size());
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjects.getResults().size());
        Assert.assertEquals(delta, listHProjects.getDelta());
        Assert.assertEquals(page, listHProjects.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjects.getNextPage());
        // delta is 5, page 2: 10 entities stored in database
        Assert.assertEquals(2, listHProjects.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponsePage1 = hProjectRestApi.findAllHProjectPaginated(delta, 1);
        HyperIoTPaginableResult<HProject> listHProjectsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertFalse(listHProjectsPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjectsPage1.getResults().size());
        Assert.assertEquals(delta, listHProjectsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectsPage1.getCurrentPage());
        Assert.assertEquals(page, listHProjectsPage1.getNextPage());
        // delta is 5, page is 1: 10 entities stored in database
        Assert.assertEquals(2, listHProjectsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    // HProject action find-all: 16
    @Test
    public void test010_cardsViewWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find all HProject with the following call cardsView
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.cardsView();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        Assert.assertEquals(1, listHProjects.size());
        boolean hprojectFound = false;
        for (HProject project : listHProjects) {
            if (hproject.getId() == project.getId()) {
                Assert.assertEquals(hproject.getId(), project.getId());
                Assert.assertEquals(hproject.getName(), project.getName());
                Assert.assertEquals(hproject.getDescription(), project.getDescription());
                Assert.assertEquals(huser.getId(), project.getUser().getId());
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
    }


    // HProject action find-all: 16 (RuleEngine)



    // HProject action areas_management: 64
    @Test
    public void test012_getHProjectAreaListWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find all HProject Area list with the
        // following call getHProjectAreaList
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertFalse(listHProjectAreas.isEmpty());
        Assert.assertEquals(1, listHProjectAreas.size());
        boolean hprojectAreaFound = false;
        for (Area projectArea : listHProjectAreas) {
            if (area.getId() == projectArea.getId()) {
                Assert.assertEquals(area.getId(), projectArea.getId());
                Assert.assertEquals(area.getName(), projectArea.getName());
                Assert.assertEquals(area.getDescription(), projectArea.getDescription());
                Assert.assertEquals(area.getProject().getId(), projectArea.getProject().getId());
                Assert.assertEquals(area.getProject().getUser().getId(), projectArea.getProject().getUser().getId());
                Assert.assertEquals(huser.getId(), projectArea.getProject().getUser().getId());
                hprojectAreaFound = true;
            }
        }
        Assert.assertTrue(hprojectAreaFound);
    }


    // HProject action device_list: 128
    @Test
    public void test013_findAllHDeviceByProjectIdWithDefaultPermissionShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, finds list of all available HDevice for the given project id
        // with the following call findAllHDeviceByProjectId
        // response status code 200
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice1 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice2.getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        Assert.assertEquals(2, listHDevice.size());
        boolean hdeviceFound1 = false;
        boolean hdeviceFound2 = false;
        for (HDevice device : listHDevice) {
            if (hdevice1.getId() == device.getId()) {
                Assert.assertEquals(hdevice1.getDeviceName(), device.getDeviceName());
                Assert.assertEquals(hdevice1.getBrand(), device.getBrand());
                Assert.assertEquals(hdevice1.getDescription(), device.getDescription());
                Assert.assertEquals(hdevice1.getFirmwareVersion(), device.getFirmwareVersion());
                Assert.assertEquals(hdevice1.getModel(), device.getModel());
                Assert.assertEquals(hdevice1.getSoftwareVersion(), device.getSoftwareVersion());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(hproject.getUser().getId(), device.getProject().getUser().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                hdeviceFound1 = true;
            }
            if (hdevice2.getId() == device.getId()) {
                Assert.assertEquals(hdevice2.getDeviceName(), device.getDeviceName());
                Assert.assertEquals(hdevice2.getBrand(), device.getBrand());
                Assert.assertEquals(hdevice2.getDescription(), device.getDescription());
                Assert.assertEquals(hdevice2.getFirmwareVersion(), device.getFirmwareVersion());
                Assert.assertEquals(hdevice2.getModel(), device.getModel());
                Assert.assertEquals(hdevice2.getSoftwareVersion(), device.getSoftwareVersion());
                Assert.assertEquals(hproject.getId(), device.getProject().getId());
                Assert.assertEquals(hproject.getUser().getId(), device.getProject().getUser().getId());
                Assert.assertEquals(huser.getId(), device.getProject().getUser().getId());
                hdeviceFound2 = true;
            }
        }
        Assert.assertTrue(hdeviceFound1);
        Assert.assertTrue(hdeviceFound2);
    }


    // HProject action device_list: 128
    @Test
    public void test014_getProjectTreeViewJsonWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, finds all HPacket by hprojectId with the following
        // call getProjectTreeViewJson
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

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {});
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(2, listHPackets.size());
        boolean hpacket1Found = false;
        boolean fieldOfHPacket1Found = false;
        boolean hpacket2Found = false;
        for (HPacket packet : listHPackets) {
            if (hpacket1.getId() == packet.getId()) {
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                hpacket1Found = true;
                Assert.assertEquals(2, packet.getFields().size());
                Assert.assertEquals(hpacket1.getFields().size(), packet.getFields().size());
                fieldOfHPacket1Found = true;
            }
            if (hpacket2.getId() == packet.getId()) {
                Assert.assertEquals(huser.getId(), packet.getDevice().getProject().getUser().getId());
                Assert.assertEquals(0, packet.getFields().size());
                hpacket2Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(fieldOfHPacket1Found);
        Assert.assertTrue(hpacket2Found);
    }

    @Test
    public void test015_huserWithDefaultPermissionIsAuthorizedToShareAProjectThatHeOwns() {
        // HUser, with default permission, save HProject
        //HUser ,such that he is the project owner share a project, with another user (sharing user)
        //The operation must be authorized.
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        //Save new project
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HUser sharingUser = huserWithDefaultPermissionInHyperIoTFramework(true);
        SharedEntity sharedEntity = createSharedEntity(hproject,  huser, sharingUser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(sharingUser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());
    }

    @Test
    public void test016_huserWithDefaultPermissionIsNotAuthorizedToShareAProjectThatHeNotOwns() {
        // HUser, with default permission, save HProject
        //HUser ,project's owner , share a project with huser2
        //Huser2 ,  that not owns the project, try to share the project owned by huser with huser3
        //The operation must not be authorized
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        //Save new project
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        SharedEntity sharedEntity = createSharedEntity(hproject,  huser, huser2);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser2.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        HUser huser3 = huserWithDefaultPermissionInHyperIoTFramework(true);

        SharedEntity sharedWithHUser3 = new SharedEntity();
        sharedWithHUser3.setEntityId(hproject.getId());
        sharedWithHUser3.setEntityResourceName(hproject.getResourceName());
        sharedWithHUser3.setUserId(huser3.getId());

        SharedEntityRestApi sharedEntityRestService = getOsgiService(SharedEntityRestApi.class);
        impersonateUser(sharedEntityRestService,huser2);

        Response shareWithHUser3Response = sharedEntityRestService.saveSharedEntity(sharedWithHUser3);
        Assert.assertEquals(403,shareWithHUser3Response.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) shareWithHUser3Response.getEntity()).getType());
    }

    @Test
    public void test017_hUserWithDefaultPermissionAndSuchThatProjectIsSharedWithHimCannotUpdateProjectOwner(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject
        //HUser ,project's owner , share a project with huser2
        //Huser2 ,  that not owns the project, try to update the project to set himself as a project's owner.
        //The operation is not authorized (Status '403')
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        //Save new project
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        SharedEntity sharedEntity = createSharedEntity(hproject,  huser, huser2);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser2.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        hproject.setUser(huser2);
        hproject.setDescription(hproject.getDescription().concat("New"));
        impersonateUser(hProjectRestService,huser2);
        Response restResponse = hProjectRestService.updateHProject(hproject);
        Assert.assertEquals( 403 , restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError)restResponse.getEntity()).getType());
    }

    @Test
    public void test018_hUserWithDefaultPermissionAndSuchThatHeOwnsTheProjectCannotChangeProjectOwnerWithUpdateProject(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject
        //HUser ,project's owner , update project and specify huser2 as project's owner
        //The operation is not authorized (Status 404).
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        //Save new project
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectRestService,huser);
        //set huser2 as owner
        hproject.setUser(huser2);
        Response restResponse = hProjectRestService.updateHProject(hproject);
        Assert.assertEquals( 403 , restResponse.getStatus());
        Assert.assertEquals( hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError)restResponse.getEntity()).getType() );
    }

    @Test
    public void test019_hUserWithDefaultPermissionIsNotAuthorizedToSaveAnHProjectSuchThatProjectsOwnerIsNotHimself(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject
        // HUser specify huser2 as the Project Owner.
        // The operation is not authorized, Response Status 403.
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        //Save new project
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HProject hproject = new HProject();
        hproject.setName("Project " + UUID.randomUUID());
        hproject.setDescription("Project of user: " + huser.getUsername());
        //huser set project's user as huser2
        hproject.setUser(huser2);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(((HyperIoTBaseError)restResponse.getEntity()).getType() , hyperIoTException + "HyperIoTUnauthorizedException");
    }

    @Test
    public void test020_hUserWithDefaultPermissionIsAuthorizedToAssignNewOwnerToAProjectOwnedByHimself(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject
        // HUser specify huser2 as the new HProject's Owner with the following call updateHProjectOwner.
        // The operation is authorized, response status code '200'.
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser2.getId());
        this.impersonateUser(hProjectRestService, huser);
        Response restResponse = hProjectRestService.updateHProjectOwner(hproject.getId(), huser2.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        HProject responseProject = (HProject) restResponse.getEntity();
        Assert.assertEquals(responseProject.getId(), hproject.getId());
        Assert.assertNotNull(responseProject.getUser());
        Assert.assertEquals(responseProject.getUser().getId(), huser2.getId());
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        hProjectSystemApi.remove(responseProject.getId(), null);
    }

    @Test
    public void test021_hUserWithDefaultPermissionCannotUpdateProjectOwnerWhenNotOwnaHProject(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject
        // huser2 try to became the new HProject's owner with the following call updateHProjectOwner.
        // The operation is not authorized , response status code '404' (HyperIoTEntityNotFound).
        //(Entity not found is caused by query filter added in HyperIoTBaseEntityServiceImpl's find method.
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser2.getId());
        this.impersonateUser(hProjectRestService, huser2);
        Response restResponse = hProjectRestService.updateHProjectOwner(hproject.getId(), huser2.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(((HyperIoTBaseError)restResponse.getEntity()).getType() , hyperIoTException + "HyperIoTEntityNotFound");
    }

    @Test
    public void test022_hUserWithDefaultPermissionCanUpdateProjectOwnerWhenShareButNotOwnsHProject(){
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject
        // HUser share HProject with huser2
        // huser2 try to became the new HProject's owner, with the following call updateHProjectOwner..
        // The operation is not authorized, response status code '200'.
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser2.getId());
        SharedEntity sharedEntity = createSharedEntity(hproject,  huser, huser2);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser2.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());
        this.impersonateUser(hProjectRestService, huser2);
        Response restResponse = hProjectRestService.updateHProjectOwner(hproject.getId(), huser2.getId());
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


    private Area createArea(HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HUser ownerHUser = hproject.getUser();
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Area associated with huser: " + ownerHUser.getUsername());
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, ownerHUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals("Area associated with huser: " + ownerHUser.getUsername(),
                ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        return area;
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
        hdevice.setBrand("ACSoftware");
        hdevice.setDescription("Property of: " + hproject.getUser().getUsername());
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
        Assert.assertEquals("ACSoftware",
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
        Assert.assertEquals(ownerHUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
    }

    private HPacket createHPacketAndAddHPacketField(HDevice hdevice, boolean createField) {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);

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

            //check restResponse field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            List<HPacketField> fields = new ArrayList<>(((HPacket) restResponse.getEntity()).getFields());
            Assert.assertTrue(fields.stream().anyMatch (field -> field.getId() == ((HPacketField) responseAddField1.getEntity()).getId()));
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

            //check restResponse field2 is equals to responseAddField2 field2
            Assert.assertEquals(field2.getId(), ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertTrue(fields.stream().anyMatch (field -> field.getId() == ((HPacketField) responseAddField2.getEntity()).getId()));
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField2.getEntity()).getPacket().getId());

            Assert.assertEquals(2, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        return hpacket;
    }


    // HProject is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;

    @After
    public void afterTest() {
        // Remove projects in every test
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
            for (int i = 0; i < roles.size(); i++){
                role = ((Role) roles.get(i));
            }
            Assert.assertNotNull(role);
            Assert.assertEquals("RegisteredUser", role.getName());
            Assert.assertEquals("Role associated with the registered user",
                    role.getDescription());
            PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
            Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
            Assert.assertFalse(listPermissions.isEmpty());
            boolean resourceNameHProject = false;
            for (Permission permission : listPermissions) {
                if (permission.getEntityResourceName().contains(permissionHProject)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionHProject, permission.getEntityResourceName());
                    Assert.assertEquals(permissionHProject + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameHProject = true;
                }
            }
            Assert.assertTrue(resourceNameHProject);
        }
        return huser;
    }


}

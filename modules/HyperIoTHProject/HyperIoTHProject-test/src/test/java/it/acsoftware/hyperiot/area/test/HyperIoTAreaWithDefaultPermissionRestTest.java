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

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.model.AreaViewType;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.checkerframework.checker.units.qual.A;
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
 * @author Aristide Cittadino Interface component for Area System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAreaWithDefaultPermissionRestTest extends KarafTestSupport {

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
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Area Module works!", restResponse.getEntity());
    }

    // Area action save: 1
    @Test
    public void test002_saveAreaWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, save Area with the following call saveArea
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action update: 2
    @Test
    public void test003_updateAreaWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, update Area with the following call updateArea
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action find: 8
    @Test
    public void test004_findAreaWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, find Area with the following call findArea
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action update and find: 10
    @Test
    public void test005_setAreaImageWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, set an Area image with the following call setAreaImage
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action update and find: 10
    @Test
    public void test006_getAreaImageWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, get an Area image with the following call getAreaImage
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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
                .contains("attachment; filename=\""+area.getId()+"_img.jpg\""));
    }


    // Area action update and find: 10
    @Test
    public void test007_unsetAreaImageWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, unset an Area image with the following call unsetAreaImage
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action find-all: 16
    @Test
    public void test008_findAllAreaWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, find all Area with the following call findAllArea
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action find-all: 16
    @Test
    public void test009_findAllAreaPaginatedWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all areas with pagination
        // response status code '200'
        int delta = 4;
        int page = 2;
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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
        Assert.assertEquals(numbEntities - delta, listAreas.getResults().size());
        Assert.assertEquals(delta, listAreas.getDelta());
        Assert.assertEquals(page, listAreas.getCurrentPage());
        Assert.assertEquals(defaultPage, listAreas.getNextPage());
        // delta is 4, page is 2: 7 entities stored in database
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
        // delta is 4, page is 1: 7 entities stored in database
        Assert.assertEquals(2, listAreasPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    // Area action area_device_manager: 32
    @Test
    public void test010_deleteAreaWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, delete Area with the following call deleteArea
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action area_device_manager: 32
    @Test
    public void test011_addAreaDeviceWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, add an AreaDevice with the following call addAreaDevice
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

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

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


    // Area action area_device_manager: 32
    @Test
    public void test012_removeAreaDeviceWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, remove an AreaDevice with the following call removeAreaDevice
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

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

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


    // Area action area_device_manager: 32
    @Test
    public void test013_getAreaDevicesListWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, find AreaDevice list with the following call getAreaDeviceList
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Area action area_device_manager: 32
    @Test
    public void test014_findInnerAreasWithDefaultPermissionShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with default permission, find inner areas with the following call findInnerAreas
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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
                        "{\"id\":"+ area.getId() + "," +
                        "\"entityVersion\":" + area.getEntityVersion()+"," +
                        "\"entityCreateDate\":" + area.getEntityCreateDate().getTime() + "," +
                        "\"entityModifyDate\":" + area.getEntityModifyDate().getTime() + "," +
                        "\"name\":\"" + area.getName()+"\"," +
                        "\"description\":\"" + area.getDescription() + "\"," +
                        "\"areaConfiguration\":null," +
                        "\"areaViewType\":\"IMAGE\"," +
                        "\"mapInfo\":null," +
                        "\"innerArea\":[" +
                            "{\"id\":" + parentArea.getId() + "," +
                            "\"entityVersion\":" + ((Area) restResponseUpdateArea.getEntity()).getEntityVersion() + "," +
                            "\"entityCreateDate\":" + parentArea.getEntityCreateDate().getTime() + "," +
                            "\"entityModifyDate\":" +((Area) restResponseUpdateArea.getEntity()).getEntityModifyDate().getTime() + "," +
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


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


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


    private Area createArea(HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HUser ownerHUser = hproject.getUser();
        Area area = new Area();
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Area of user: " + ownerHUser.getUsername());
        area.setProject(hproject);
        area.setAreaViewType(AreaViewType.IMAGE);
        this.impersonateUser(areaRestApi, ownerHUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals("Area of user: " + ownerHUser.getUsername(), ((Area) restResponseArea.getEntity()).getDescription());
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

    // Area is Owned Resource: only huser is able to find/findAll his entities
    private HUser huser;

    @After
    public void afterTest() {
        HyperIoTAreaTestUtil.eraseDatabase(this);
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
            boolean resourceNameFound = false;
            for (Permission permission : listPermissions) {
                if (permission.getEntityResourceName().contains(permissionArea)) {
                    // it.acsoftware.hyperiot.area.model.Area (63)
                    // save                     1
                    // update                   2
                    // remove                   4
                    // find                     8
                    // find_all                 16
                    // area_device_manager      32
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionArea, permission.getEntityResourceName());
                    Assert.assertEquals(permissionArea + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(63, permission.getActionIds());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameFound = true;
                }
            }
            Assert.assertTrue(resourceNameFound);
        }
        return huser;
    }

}

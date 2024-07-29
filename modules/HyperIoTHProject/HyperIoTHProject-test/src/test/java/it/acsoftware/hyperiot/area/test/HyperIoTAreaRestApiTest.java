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
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.UUID;

import static it.acsoftware.hyperiot.area.test.HyperIoTAreaConfiguration.hyperIoTException;
import static org.junit.Assert.*;

/**
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-04-11 Initial release
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAreaRestApiTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void test01_areaListShouldContainTestArea() {
        Area created = getTestArea();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.findAllArea();
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        HUser user = grantTestUserPermission(areaRestApi, HyperIoTCrudAction.FINDALL, Area.class.getName());
        this.impersonateUser(areaRestApi, user);
        Response response = areaRestApi.findAllArea();
        assertEquals(200, response.getStatus());
        // should contain test area
        Collection<Area> areaList = (Collection<Area>) response.getEntity();
        Area area = areaList.stream().filter(a -> a.getName().equals(created.getName())).findAny().orElse(null);
        assertNotNull(area);
    }

    @Test
    public void test02_getAndUpdateTestArea() {
        Area created = getTestArea();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.find(created.getId());
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTCrudAction.FIND, Area.class.getName());
        Response response = areaRestApi.find(created.getId());
        assertEquals(200, response.getStatus());
        Area area = (Area) response.getEntity();
        area.setDescription(HyperIoTAreaConfiguration.testAreaUpdatedDescription);
        // try first without UPDATE permission
        response = areaRestApi.updateArea(area);
        assertEquals(403, response.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) response.getEntity()).getType());
        // grant user UPDATE permission
        grantTestUserPermission(areaRestApi, HyperIoTCrudAction.UPDATE, Area.class.getName());
        response = areaRestApi.updateArea(area);
        assertEquals(200, response.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test03_areaListShouldFindTestArea() {
        Area created = getTestArea();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.findAllArea();
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTCrudAction.FINDALL, Area.class.getName());
        Response response = areaRestApi.findAllArea();
        assertEquals(200, response.getStatus());
        // should contain test area
        Collection<Area> areaList = (Collection<Area>) response.getEntity();
        Area area = areaList.stream().filter(a -> a.getId() == created.getId()).findAny().orElse(null);
        assertNotNull(area);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void test05_areaDeviceListShouldContainTestDevice() {
        AreaDevice areaDevice = addAreaDevice();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.getAreaDeviceList(areaDevice.getArea().getId());
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTAreaAction.AREA_DEVICE_MANAGER, Area.class.getName());
        Response response = areaRestApi.getAreaDeviceList(areaDevice.getArea().getId());
        Collection<AreaDevice> areaDeviceList = (Collection<AreaDevice>) response.getEntity();
        assertEquals(200, response.getStatus());
        AreaDevice adFound = areaDeviceList.stream().filter(ad -> ad.getDevice().getId() == ad.getDevice().getId()).findFirst()
                .orElse(null);
        assertNotNull(areaDevice);
    }

    @Test
    public void test06_removeAreaDevice() {
        AreaDevice ad = addAreaDevice();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.removeAreaDevice(ad.getArea().getId(), ad.getDevice().getId());
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTAreaAction.AREA_DEVICE_MANAGER, Area.class.getName());
        Response response = areaRestApi.getAreaDeviceList(ad.getArea().getId());
        Collection<AreaDevice> areaDeviceList = (Collection<AreaDevice>) response.getEntity();
        assertEquals(200, response.getStatus());
        AreaDevice adFound = areaDeviceList.stream().filter(adItem -> adItem.getDevice().getId() == ad.getDevice().getId()).findFirst()
                .orElse(null);
        assertNotNull(adFound);
        response = areaRestApi.removeAreaDevice(ad.getArea().getId(), ad.getId());
        assertEquals(200, response.getStatus());
    }


    @Test
    public void test08_deleteTestArea() {
        Area created = getTestArea();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.remove(created.getId());
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTCrudAction.REMOVE, Area.class.getName());
        Response response = areaRestApi.remove(created.getId());
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test09_getTestAreaShouldReturnError() {
        Area created = getTestArea();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.find(created.getId());
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());

        grantTestUserPermission(areaRestApi, HyperIoTCrudAction.REMOVE, Area.class.getName());
        Response response = areaRestApi.remove(created.getId());
        assertEquals(200, response.getStatus());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTCrudAction.FIND, Area.class.getName());
        response = areaRestApi.find(created.getId());
        assertEquals(404, response.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) response.getEntity()).getType());
    }

    // Utility functions

    @SuppressWarnings("unchecked")
    private HDevice getTestDevice() {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HDeviceRestApi hdeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HUser huser = grantTestUserPermission(hdeviceRestApi, HyperIoTCrudAction.FINDALL, HDevice.class.getName());
        HProject testProject = getTestProject();
        grantTestUserPermission(hdeviceRestApi, HyperIoTCrudAction.SAVE, HDevice.class.getName());
        HDevice hdevice = new HDevice();
        hdevice.setDeviceName(UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setDescription("Just a test device");
        hdevice.setBrand("Marvellous devices Inc.");
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("Claudia Shiffer");
        hdevice.setSoftwareVersion("1.1-RollerCoaster");
        hdevice.setPassword("AmbaraBacciCiCocò_1");
        hdevice.setPasswordConfirm("AmbaraBacciCiCocò_1");
        hdevice.setProject(testProject);
        Response restResponse = hdeviceRestApi.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        return hdevice;
    }

    @SuppressWarnings("unchecked")
    private HProject getTestProject() {
        HProjectRestApi projectRestApi = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser huser = grantTestUserPermission(projectRestApi, HyperIoTCrudAction.FINDALL, HProject.class.getName());
        grantTestUserPermission(projectRestApi, HyperIoTCrudAction.SAVE, HProject.class.getName());
        HProject hproject = new HProject();
        hproject.setName(UUID.randomUUID().toString());
        hproject.setDescription("Just a test project");
        hproject.setUser(huser);
        Response restResponse = projectRestApi.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        return hproject;
    }

    private HUser getTestUser(HyperIoTBaseRestApi hyperIoTBaseRestApi) {
        // Impersonate adminUser
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        assertNotNull(adminUser);
        HyperIoTContext context = impersonateUser(hyperIoTBaseRestApi, adminUser);
        assertTrue(adminUser.isAdmin());
        // create new test user
        String username = "foo";
        HUser testUser = null;
        HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
        try {
            testUser = hUserSystemApi.findUserByUsername(username);
        } catch (Exception e) {
            // not found
        }
        if (testUser != null) {
            return testUser;
        } else {
            testUser = new HUser();
            testUser.setAdmin(false);
            testUser.setEmail("foo@bar.com");
            testUser.setName("Foo");
            testUser.setLastname("Bar");
            testUser.setUsername(username);
            testUser.setPassword("testPassword&%123");
            testUser.setPasswordConfirm("testPassword&%123");
            // save newly added user
            hUserSystemApi.save(testUser, context);
        }
        return testUser;
    }

    private HUser grantTestUserPermission(HyperIoTBaseRestApi hyperIoTBaseRestApi, HyperIoTActionName apiAction,
                                          String resourceName) {
        HUser testUser = getTestUser(hyperIoTBaseRestApi);
        // Impersonate adminUser
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        assertNotNull(adminUser);
        HyperIoTContext context = impersonateUser(hyperIoTBaseRestApi, adminUser);
        assertTrue(adminUser.isAdmin());
        // Add user role for the given `resourceName` if not already present
        Role testUserRole;
        if (!testUser.hasRole(resourceName)) {
            testUserRole = new Role();
            testUserRole.setName(UUID.randomUUID().toString());
            testUserRole.setDescription("Grant permission on '" + apiAction.getName() + "' to user '"
                    + testUser.getUsername() + "' " + java.util.UUID.randomUUID().toString());
            // save newly added role
            RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
            roleSystemApi.save(testUserRole, context);
            // add role to user
            testUser.addRole(testUserRole);
            HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
            hUserSystemApi.update(testUser, context);
        } else {
            testUserRole = testUser.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase(resourceName))
                    .findFirst().orElse(null);
        }
        // Add permission of specified `apiAction` if not already present
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(resourceName, apiAction);
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        Permission permission = permissionSystemApi.findByRoleAndResourceName(testUserRole, resourceName);
        if (permission == null) {
            permission = new Permission();
            permission.setName(action.getActionName());
            permission.setEntityResourceName(resourceName);
            permission.setRole(testUserRole);
            permission.setActionIds(action.getActionId());
            permissionSystemApi.save(permission, context);
        } else {
            permission.addPermission(action);
            permissionSystemApi.update(permission, context);
        }
        // Impersonate `testUser`
        impersonateUser(hyperIoTBaseRestApi, testUser);
        return testUser;
    }

    private HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    private Area getTestArea() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        impersonateUser(areaRestApi, null);
        Area area = new Area();
        area.setName(UUID.randomUUID().toString());
        area.setDescription("Just a test area");
        HProject testProject = getTestProject();
        area.setProject(testProject);
        // the following call without impersonation should fail with response status
        // code `403 Unauthorized`
        Response failedResponse = areaRestApi.saveArea(area);
        assertEquals(403, failedResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) failedResponse.getEntity()).getType());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTCrudAction.SAVE, Area.class.getName());
        Response response = areaRestApi.saveArea(area);
        assertEquals(200, response.getStatus());
        return area;
    }

    private AreaDevice addAreaDevice() {
        Area created = getTestArea();
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HDevice device = getTestDevice();
        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(device);
        areaDevice.setArea(null);
        impersonateUser(areaRestApi, null);
        // the following call without impersonation should fail with response status
        // code `404`
        Response failedResponse = areaRestApi.addAreaDevice(created.getId(), areaDevice);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the
        // performed action
        grantTestUserPermission(areaRestApi, HyperIoTAreaAction.AREA_DEVICE_MANAGER, Area.class.getName());
        Response response = areaRestApi.addAreaDevice(created.getId(), areaDevice);
        assertEquals(200, response.getStatus());
        return areaDevice;
    }

    @After
    public void afterTest() {
        HyperIoTAreaTestUtil.eraseDatabase(this);
    }

}

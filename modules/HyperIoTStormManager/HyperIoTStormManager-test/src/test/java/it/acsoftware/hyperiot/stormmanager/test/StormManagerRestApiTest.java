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
package it.acsoftware.hyperiot.stormmanager.test;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import it.acsoftware.hyperiot.stormmanager.service.rest.StormManagerRestApi;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-11 Initial release
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StormManagerRestApiTest extends KarafTestSupport {
    public static long projectId = 0;

    //force global configuration
    public Option[] config() {
        return null;
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @Test
    public void test00_buildTopology() throws IOException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        HUser user = grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTCrudAction.SAVE), 0);
        HProject project = StormManagerTestUtil.createHProject(this, user);
        projectId = project.getId();
        // Generate topology properties and YAML files
        TopologyInfo topologyConfig = stormManagerSystemApi.getTopologyStatus(project.getId());
        assertNotNull(topologyConfig);
    }

    @Test
    public void test01_submitTestTopology() throws IOException, InterruptedException {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        System.out.println("Waiting 10sec in order to let hbase table creation finish...");
        Thread.sleep(10000);

        impersonateUser(stormManagerRestApi, null);
        Response failedResponse = stormManagerRestApi.submitProjectTopology(projectId);
        assertEquals(403, failedResponse.getStatus());

        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.ADD_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.submitProjectTopology(projectId);
        assertEquals(200, response.getStatus());
    }


    @Test
    public void test02_getTopologyShouldFindTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.getTopology(projectId);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.GET_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.getTopology(projectId);
        assertEquals(200, response.getStatus());
        TopologyInfo topologyStatus = response.readEntity(new GenericType<TopologyInfo>() {
        });
        assertNotNull(topologyStatus);
    }

    @Test
    public void test03_activateTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.activateTopology(projectId);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.ACTIVATE_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.activateTopology(projectId);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test04_testTopologyShouldBeActive() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.GET_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.getTopology(projectId);
        assertEquals(200, response.getStatus());
        TopologyInfo topologyStatus = response.readEntity(new GenericType<TopologyInfo>() {
        });
        assertEquals("ACTIVE", topologyStatus.getStatus());
    }

    @Test
    public void test05_deactivateTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.deactivateTopology(projectId);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.DEACTIVATE_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.deactivateTopology(projectId);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test06_testTopologyShouldBeInactive() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.GET_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.getTopology(projectId);
        assertEquals(200, response.getStatus());
        TopologyInfo topologyStatus = response.readEntity(new GenericType<TopologyInfo>() {
        });
        assertEquals("INACTIVE", topologyStatus.getStatus());
    }

    @Test
    public void test07_getTopologyInfo() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.getTopology(projectId);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.GET_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.getTopology(projectId);
        assertEquals(200, response.getStatus());
    }


    @Test
    public void test09_killTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.killTopology(projectId);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.KILL_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.killTopology(projectId);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test10_topologyListShouldNotContainTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.getTopology(projectId);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, getBasicHProjectPermissionMap(HyperIoTHProjectAction.GET_TOPOLOGY), projectId);
        Response response = stormManagerRestApi.getTopology(projectId);
        assertEquals(200, response.getStatus());
        TopologyInfo topologyStatus = response.readEntity(new GenericType<TopologyInfo>() {
        });
        assertTrue("NOT FOUND".equals(topologyStatus.getStatus()) || "KILLED".equals(topologyStatus.getStatus()));
    }

    private Map<String, List<HyperIoTActionName>> getBasicHProjectPermissionMap(HyperIoTActionName action) {
        Map<String, List<HyperIoTActionName>> permissionList = new HashMap<>();
        permissionList.put(HProject.class.getName(), new ArrayList<>());
        permissionList.get(HProject.class.getName()).add(action);
        //necessary to add find permission
        permissionList.get(HProject.class.getName()).add(HyperIoTCrudAction.FIND);
        return permissionList;
    }

    private HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    private HUser createTestUser(HyperIoTBaseRestApi hyperIoTBaseRestApi) {
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
            testUser.setEmail(testUser.getUsername() + "@mail.com");
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

    private HUser grantUserPermission(HyperIoTBaseRestApi hyperIoTBaseRestApi, Map<String, List<HyperIoTActionName>> permissions, long currProjectId) {
        HUser user = createTestUser(hyperIoTBaseRestApi);
        // Impersonate adminUser
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        assertNotNull(adminUser);
        HyperIoTContext context = impersonateUser(hyperIoTBaseRestApi, adminUser);
        assertTrue(adminUser.isAdmin());

        Role testUserRole = new Role();
        testUserRole.setName(UUID.randomUUID().toString());
        testUserRole.setDescription("Grant permission on '" + testUserRole.getName() + "' to user '" + user.getUsername() + "'");
        // save newly added role
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        roleSystemApi.save(testUserRole, context);

        // add role to user
        user.addRole(testUserRole);
        HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
        hUserSystemApi.update(user, context);

        Iterator<String> it = permissions.keySet().iterator();
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        Permission permission = new Permission();
        permission.setName(UUID.randomUUID().toString());
        while (it.hasNext()) {
            String resourceName = it.next();
            int actionIds = 0;
            for (HyperIoTActionName actionName : permissions.get(resourceName)) {
                HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(resourceName, actionName);
                permission.addPermission(action);
            }
            permission.setEntityResourceName(resourceName);
            permission.setResourceId(currProjectId);
            permission.setRole(testUserRole);
            permissionSystemApi.save(permission, context);
        }

        impersonateUser(hyperIoTBaseRestApi, user);
        return user;
    }

}

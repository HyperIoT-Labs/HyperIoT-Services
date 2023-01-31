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

package it.acsoftware.hyperiot.dashboard.test;

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import it.acsoftware.hyperiot.dashboard.service.rest.DashboardRestApi;
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
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
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

import static it.acsoftware.hyperiot.dashboard.test.HyperIoTDashboardConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for Dashboard System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDashboardWithPermissionRestTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
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
    public void test01_dashboardModuleShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // the following call checkModuleWorking checks if Dashboard module working
        // correctly
        huser = createHUser(null);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Dashboard Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_saveDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, save Dashboard with the following call saveDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Dashboard dashboard = new Dashboard();
        dashboard.setName(hproject.getName() + " Online Dashboard");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard",
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals("REALTIME",
                ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    @Test
    public void test03_saveDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to save Dashboard with the following call saveDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals("Project of user: " + huser.getUsername(), hproject.getDescription());

        Dashboard dashboard = new Dashboard();
        dashboard.setName(hproject.getName() + " Online Dashboard");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test04_updateDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, update Dashboard with the following call updateDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName("Dashboard Edited");
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getEntityVersion() + 1,
                ((Dashboard) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("Dashboard Edited",
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals(dashboard.getHProject().getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    @Test
    public void test05_updateDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to update Dashboard with the following call updateDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName("dashboard edited");
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test06_findDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, update Dashboard with the following call findDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getId(), ((Dashboard) restResponse.getEntity()).getId());
        Assert.assertEquals(dashboard.getHProject().getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    @Test
    public void test07_findDashboardWithPermissionShouldFailIfEntityNotFound() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to find Dashboard with the following call findDashboard,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test08_findDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to find Dashboard with the following findDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test09_findDashboardNotFoundWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to find Dashboard not found with
        // the following call findDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test10_findAllDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, find all Dashboard with the following call findAllDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboard();
        List<Dashboard> listDashboards = restResponse.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertFalse(listDashboards.isEmpty());
        Assert.assertEquals(1, listDashboards.size());
        boolean dashboardFound = false;
        for (Dashboard d : listDashboards) {
            if (dashboard.getId() == d.getId()) {
                Assert.assertEquals(hproject.getId(), d.getHProject().getId());
                Assert.assertEquals(huser.getId(), d.getHProject().getUser().getId());
                dashboardFound = true;
            }
        }
        Assert.assertTrue(dashboardFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test11_findAllDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to find all Dashboard with
        // the following call findAllDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboard();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test12_deleteDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, delete Dashboard with the following call deleteDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test13_deleteDashboardWithPermissionShouldFailIfEntityNotFound() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to delete Dashboard with the following
        // call deleteDashboard, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(Dashboard.class.getName(),
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.deleteDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test14_deleteDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to delete Dashboard with
        // the following call deleteDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test15_deleteDashboardNotFoundWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to delete Dashboard not found with
        // the following call deleteDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.deleteDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test16_saveDashboardWithPermissionShouldFailIfNameIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to save Dashboard with the following call saveDashboard,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Dashboard dashboard = new Dashboard();
        dashboard.setName(null);
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test17_saveDashboardWithPermissionShouldFailIfNameIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to save Dashboard with the following call saveDashboard,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Dashboard dashboard = new Dashboard();
        dashboard.setName("");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboard.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test18_saveDashboardWithPermissionShouldFailIfNameIsMaliciousCode() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to save Dashboard with the following call saveDashboard,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Dashboard dashboard = new Dashboard();
        dashboard.setName("src='malicious code'");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboard.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test19_saveDashboardWithPermissionShouldFailIfHProjectIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to save Dashboard with the following call saveDashboard,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        Dashboard dashboard = new Dashboard();
        dashboard.setName("dashboard name");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(null);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-hproject", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test20_saveDashboardWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to save Dashboard with the following call saveDashboard,
        // but HProject belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2, true);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        Dashboard dashboard = new Dashboard();
        dashboard.setName("dashboard name");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(anotherHProject);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(0, dashboard.getId());
        Assert.assertNotEquals(huser.getId(), dashboard.getHProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboard.getHProject().getUser().getId());

        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());
        Assert.assertEquals(huser2.getUsername(), anotherHProject.getUser().getUsername());
        Assert.assertNotEquals(huser.getId(), anotherHProject.getUser().getId());
        Assert.assertNotEquals(huser.getUsername(), anotherHProject.getUser().getUsername());
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getUsername(), huser2.getUsername());
    }


    @Test
    public void test21_updateDashboardWithPermissionShouldFailIfNameIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to update Dashboard with the following call updateDashboard,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName(null);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test22_updateDashboardWithPermissionShouldFailIfNameIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to update Dashboard with the following call updateDashboard,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
        dashboard.setName("");
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboard.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test23_updateDashboardWithPermissionShouldFailIfNameIsMaliciousCode() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to update Dashboard with the following call updateDashboard,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
        dashboard.setName("eval(malicious code)");
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboard.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test24_updateDashboardWithPermissionShouldFailIfHProjectIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to update Dashboard with the following call updateDashboard,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setHProject(null);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-hproject", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test25_updateDashboardWithPermissionShouldFailIfHProjectBelongsToAnotherUser() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to update Dashboard with the following call updateDashboard,
        // but HProject belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        huser2 = createHUser(null);
        HProject hprojectOfUser2 = createHProject(huser2, true);
        Assert.assertNotEquals(0, hprojectOfUser2.getId());
        Assert.assertEquals(huser2.getId(), hprojectOfUser2.getUser().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        // huser tries to set hproject of huser2
        dashboard.setHProject(hprojectOfUser2);
        Assert.assertNotEquals(huser.getId(), dashboard.getHProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboard.getHProject().getUser().getId());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test26_updateDashboardWithPermissionShouldFailIfEntityNotFound() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, tries to update Dashboard with the following call updateDashboard,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        // entity isn't stored in database
        Dashboard dashboard = new Dashboard();
        dashboard.setName("entity not found...");
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test27_updateDashboardNotFoundWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, without permission, tries to update Dashboard not found
        // with the following call updateDashboard
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        // entity isn't stored in database
        Dashboard dashboard = new Dashboard();
        dashboard.setName("HyperIoTEntityNotFound...");
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(403, restResponse.getStatus());
    }

    @Test
    public void test28_findAllDashboardPaginatedWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with permission,
        // find all Dashboard with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 6;
        int page = 2;
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Dashboard dashboard = createDashboard(hproject, "OFFLINE");
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(defaultDelta, dashboards.size());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listDashboard.getResults().size());
        Assert.assertEquals(delta, listDashboard.getDelta());
        Assert.assertEquals(page, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // delta is 6, page 2: 10 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponsePage1 = dashboardRestApi.findAllDashboardPaginated(delta, 1);
        HyperIoTPaginableResult<Dashboard> listDashboardPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardPage1.getResults().size());
        Assert.assertEquals(delta, listDashboardPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardPage1.getCurrentPage());
        Assert.assertEquals(page, listDashboardPage1.getNextPage());
        // delta is 6, page 1: 10 entities stored in database
        Assert.assertEquals(2, listDashboardPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test29_findAllDashboardPaginatedWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, without permission,
        // tries to find all Dashboard with pagination
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test30_findAllDashboardPaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with permission,
        // find all Dashboard with pagination
        // if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Dashboard dashboard = createDashboard(hproject, "REALTIME");
            Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(defaultDelta, dashboards.size());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboard.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboard.getDelta());
        Assert.assertEquals(defaultPage, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test31_findAllDashboardPaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with permission,
        // find all Dashboard with pagination
        // if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = -1;
        int page = 1;

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        int numbEntities = 14;
        for (int i = 0; i < numbEntities; i++) {
            Dashboard dashboard = createDashboard(hproject, "OFFLINE");
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(numbEntities, dashboards.size());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboard.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboard.getDelta());
        Assert.assertEquals(page, listDashboard.getCurrentPage());
        Assert.assertEquals(page + 1, listDashboard.getNextPage());
        // default delta is 10, page is 1: 14 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponsePage2 = dashboardRestApi.findAllDashboardPaginated(delta, 2);
        HyperIoTPaginableResult<Dashboard> listDashboardPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listDashboardPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardPage2.getDelta());
        Assert.assertEquals(page + 1, listDashboardPage2.getCurrentPage());
        Assert.assertEquals(page, listDashboardPage2.getNextPage());
        // default delta is 10, page is 2: 14 entities stored in database
        Assert.assertEquals(2, listDashboardPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test32_findAllDashboardPaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with permission,
        // find all Dashboard with pagination
        // if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 0;
        int page = 3;

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        int numbEntities = 24;
        for (int i = 0; i < numbEntities; i++) {
            Dashboard dashboard = createDashboard(hproject, "REALTIME");
            Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(numbEntities, dashboards.size());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(numbEntities - (defaultDelta * 2), listDashboard.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboard.getDelta());
        Assert.assertEquals(page, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // default delta is 10, page 3: 24 entities stored in database
        Assert.assertEquals(3, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponsePage1 = dashboardRestApi.findAllDashboardPaginated(delta, 1);
        HyperIoTPaginableResult<Dashboard> listDashboardPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboardPage1.getNextPage());
        // default delta is 10, page 1: 24 entities stored in database
        Assert.assertEquals(3, listDashboardPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponsePage2 = dashboardRestApi.findAllDashboardPaginated(delta, 2);
        HyperIoTPaginableResult<Dashboard> listDashboardPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardPage2.getCurrentPage());
        Assert.assertEquals(page, listDashboardPage2.getNextPage());
        // default delta is 10, page 2: 24 entities stored in database
        Assert.assertEquals(3, listDashboardPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test33_findAllDashboardPaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with permission,
        // find all Dashboard with pagination
        // if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 7;
        int page = -1;

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Dashboard dashboard = createDashboard(hproject, "REALTIME");
            Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(defaultDelta, dashboards.size());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboard.getResults().size());
        Assert.assertEquals(delta, listDashboard.getDelta());
        Assert.assertEquals(defaultPage, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboard.getNextPage());
        // delta is 7, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponsePage2 = dashboardRestApi.findAllDashboardPaginated(delta, 2);
        HyperIoTPaginableResult<Dashboard> listDashboardPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listDashboardPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardPage2.getNextPage());
        // delta is 7, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listDashboardPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test34_findAllDashboardPaginatedWithPermissionShouldWorkIfPageIsZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with permission,
        // find all Dashboard with pagination
        // if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 9;
        int page = 0;

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Dashboard dashboard = createDashboard(hproject, "OFFLINE");
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(defaultDelta, dashboards.size());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboard.getResults().size());
        Assert.assertEquals(delta, listDashboard.getDelta());
        Assert.assertEquals(defaultPage, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboard.getNextPage());
        // delta is 9, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponsePage2 = dashboardRestApi.findAllDashboardPaginated(delta, 2);
        HyperIoTPaginableResult<Dashboard> listDashboardPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listDashboardPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardPage2.getNextPage());
        // delta is 9, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listDashboardPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test35_findHProjectOfflineDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with permission, finds all offline Dashboard associated to the project
        // with the following call findHProjectOfflineDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hProjectOfflineDashboard = createHProject(huser, false);
        Assert.assertNotEquals(0, hProjectOfflineDashboard.getId());
        Assert.assertEquals(huser.getId(), hProjectOfflineDashboard.getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hProjectOfflineDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        List<Dashboard> dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "OFFLINE" has been setting in findHProjectOfflineDashboard()
        Assert.assertNotEquals(0, dashboardsOffline.get(0).getId());
        Assert.assertEquals(1, dashboardsOffline.size());
        Assert.assertEquals(hProjectOfflineDashboard.getName() + " Offline Dashboard",
                dashboardsOffline.get(0).getName());
        Assert.assertEquals("OFFLINE", dashboardsOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hProjectOfflineDashboard.getId(), dashboardsOffline.get(0).getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardsOffline.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test36_findHProjectOfflineDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, without permission, tries to find all offline Dashboard associated
        // to the project with the following call findHProjectOfflineDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hProjectOfflineDashboard = createHProject(huser, false);
        Assert.assertNotEquals(0, hProjectOfflineDashboard.getId());
        Assert.assertEquals(huser.getId(), hProjectOfflineDashboard.getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hProjectOfflineDashboard.getId());
        Assert.assertEquals(403, restResponseDashboardOffline.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseDashboardOffline.getEntity()).getType());
    }

    @Test
    public void test37_findHProjectRealtimeDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with permission, finds all realtime Dashboard associated to the project
        // with the following call findHProjectRealtimeDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hProjectRealtimeDashboard = createHProject(huser, false);
        Assert.assertNotEquals(0, hProjectRealtimeDashboard.getId());
        Assert.assertEquals(huser.getId(), hProjectRealtimeDashboard.getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hProjectRealtimeDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        List<Dashboard> dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "REALTIME" has been setting in findHProjectRealtimeDashboard()
        Assert.assertNotEquals(0, dashboardsOnline.get(0).getId());
        Assert.assertEquals(1, dashboardsOnline.size());
        Assert.assertEquals(hProjectRealtimeDashboard.getName() + " Online Dashboard",
                dashboardsOnline.get(0).getName());
        Assert.assertEquals("REALTIME", dashboardsOnline.get(0).getDashboardType().getType());
        Assert.assertEquals(hProjectRealtimeDashboard.getId(), dashboardsOnline.get(0).getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardsOnline.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test38_findHProjectRealtimeDashboardWithoutPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, without permission, tries to find all realtime Dashboard associated
        // to the project with the following call findHProjectRealtimeDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hProjectRealtimeDashboard = createHProject(huser, false);
        Assert.assertNotEquals(0, hProjectRealtimeDashboard.getId());
        Assert.assertEquals(huser.getId(), hProjectRealtimeDashboard.getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hProjectRealtimeDashboard.getId());
        Assert.assertEquals(403, restResponseDashboardOnline.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseDashboardOnline.getEntity()).getType());
    }


    @Test
    public void test39_findAreaRealtimeDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with permission, finds all realtime Dashboard associated to the area
        // with the following call findAreaRealtimeDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, false);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaRealtime = dashboardRestApi.findAreaRealtimeDashboard(area.getId());
        Assert.assertEquals(200, restResponseAreaRealtime.getStatus());
        List<Dashboard> areaRealtime = restResponseAreaRealtime.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "REALTIME" has been setting in findAreaRealtimeDashboard()
        Assert.assertEquals(1, areaRealtime.size());
        Assert.assertNotEquals(0, areaRealtime.get(0).getId());
        Assert.assertEquals(area.getId(), areaRealtime.get(0).getArea().getId());
        Assert.assertEquals(huser.getId(), areaRealtime.get(0).getArea().getProject().getUser().getId());
        Assert.assertEquals("REALTIME", areaRealtime.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), areaRealtime.get(0).getHProject().getId());
        Assert.assertEquals(huser.getId(), areaRealtime.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test40_findAreaRealtimeDashboardWithPermissionShouldFailIfAreaIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with permission, finds all realtime Dashboard associated to the area
        // with the following call findAreaRealtimeDashboard. This call returns an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaRealtime = dashboardRestApi.findAreaRealtimeDashboard((long) 0);
        Assert.assertEquals(200, restResponseAreaRealtime.getStatus());
        List<Dashboard> areaRealtime = restResponseAreaRealtime.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertTrue(areaRealtime.isEmpty());
        Assert.assertEquals(0, areaRealtime.size());
    }


    @Test
    public void test41_findAreaRealtimeDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, without permission, tries to find all realtime Dashboard associated
        // to the area with the following call findAreaRealtimeDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser, false);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaRealtime = dashboardRestApi.findAreaRealtimeDashboard(area.getId());
        Assert.assertEquals(403, restResponseAreaRealtime.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseAreaRealtime.getEntity()).getType());
    }


    @Test
    public void test42_findAreaOfflineDashboardWithPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with permission, finds all offline Dashboard associated to the area
        // with the following call findAreaOfflineDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser, false);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaOffline = dashboardRestApi.findAreaOfflineDashboard(area.getId());
        Assert.assertEquals(200, restResponseAreaOffline.getStatus());
        List<Dashboard> areaOffline = restResponseAreaOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "OFFLINE" has been setting in findAreaOfflineDashboard()
        Assert.assertEquals(1, areaOffline.size());
        Assert.assertNotEquals(0, areaOffline.get(0).getId());
        Assert.assertEquals(area.getId(), areaOffline.get(0).getArea().getId());
        Assert.assertEquals(huser.getId(), areaOffline.get(0).getArea().getProject().getUser().getId());
        Assert.assertEquals("OFFLINE", areaOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), areaOffline.get(0).getHProject().getId());
        Assert.assertEquals(huser.getId(), areaOffline.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test43_findAreaOfflineDashboardWithPermissionShouldFailIfAreaIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with permission, finds all offline Dashboard associated to the area
        // with the following call findAreaOfflineDashboard. This call returns an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaOffline = dashboardRestApi.findAreaOfflineDashboard((long) 0);
        Assert.assertEquals(200, restResponseAreaOffline.getStatus());
        List<Dashboard> areaOffline = restResponseAreaOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertTrue(areaOffline.isEmpty());
        Assert.assertEquals(0, areaOffline.size());
    }


    @Test
    public void test44_findAreaOfflineDashboardWithoutPermissionShouldFail() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with permission, finds all offline Dashboard associated to the area
        // with the following call findAreaOfflineDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser, false);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaOffline = dashboardRestApi.findAreaOfflineDashboard(area.getId());
        Assert.assertEquals(403, restResponseAreaOffline.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseAreaOffline.getEntity()).getType());
    }


    @Test
    public void test45_findAllDashboardWithPermissionShouldWorkIfListIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with permission, find all Dashboard with the following call findAllDashboard
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboard();
        List<Dashboard> listDashboards = restResponse.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertTrue(listDashboards.isEmpty());
        Assert.assertEquals(0, listDashboards.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test46_findAllDashboardPaginatedWithPermissionShouldWorkIfListIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with permission,
        // find all Dashboard with pagination
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(defaultDelta, defaultPage);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertTrue(listDashboard.getResults().isEmpty());
        Assert.assertEquals(0, listDashboard.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboard.getDelta());
        Assert.assertEquals(defaultPage, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // default delta is 10, default page is 1: there are not entities stored in database
        Assert.assertEquals(0, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test47_saveHProjectWithPermissionAndCreateTwoDashboardShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, save HProject with the following call saveHProject
        // and dashboards (Offline, Realtime) will be created automatically
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        HProject hprojectDashboard = new HProject();
        hprojectDashboard.setName("Project " + java.util.UUID.randomUUID());
        hprojectDashboard.setDescription("Description");
        hprojectDashboard.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hprojectDashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hprojectDashboard.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals("Description", ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());

        //checks if dashboards has been created
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);

        //checks if dashboard Offline has been created
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        addPermission(huser, action1);

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        List<Dashboard> dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOffline.get(0).getId());
        Assert.assertEquals(1, dashboardsOffline.size());
        Assert.assertEquals(hprojectDashboard.getName() + " Offline Dashboard", dashboardsOffline.get(0).getName());
        Assert.assertEquals("OFFLINE", dashboardsOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hprojectDashboard.getId(), dashboardsOffline.get(0).getHProject().getId());
        Assert.assertEquals(hprojectDashboard.getUser().getId(), dashboardsOffline.get(0).getHProject().getUser().getId());

        //checks if dashboard Realtime has been created
        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        List<Dashboard> dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOnline.get(0).getId());
        Assert.assertEquals(1, dashboardsOnline.size());
        Assert.assertEquals(hprojectDashboard.getName() + " Online Dashboard", dashboardsOnline.get(0).getName());
        Assert.assertEquals("REALTIME", dashboardsOnline.get(0).getDashboardType().getType());
        Assert.assertEquals(hprojectDashboard.getId(), dashboardsOnline.get(0).getHProject().getId());
        Assert.assertEquals(hprojectDashboard.getUser().getId(), dashboardsOnline.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test48_deleteHProjectWithPermissionAndRemoveInCascadeAllDashboards() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // HUser, with permission, deletes hproject with call deleteHProject
        // and remove in cascade mode all dashboards
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hprojectDashboard = createHProject(huser,false);
        Assert.assertNotEquals(0, hprojectDashboard.getId());
        Assert.assertEquals(huser.getId(), hprojectDashboard.getUser().getId());

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        addPermission(huser, action1);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        List<Dashboard> dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOffline.get(0).getId());
        Assert.assertEquals(1, dashboardsOffline.size());
        Assert.assertEquals(hprojectDashboard.getName() + " Offline Dashboard", dashboardsOffline.get(0).getName());
        Assert.assertEquals("OFFLINE", dashboardsOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hprojectDashboard.getId(), dashboardsOffline.get(0).getHProject().getId());
        Assert.assertEquals(hprojectDashboard.getUser().getId(), dashboardsOffline.get(0).getHProject().getUser().getId());

        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        List<Dashboard> dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOnline.get(0).getId());
        Assert.assertEquals(1, dashboardsOnline.size());
        Assert.assertEquals(hprojectDashboard.getName() + " Online Dashboard", dashboardsOnline.get(0).getName());
        Assert.assertEquals("REALTIME", dashboardsOnline.get(0).getDashboardType().getType());
        Assert.assertEquals(hprojectDashboard.getId(), dashboardsOnline.get(0).getHProject().getId());
        Assert.assertEquals(hprojectDashboard.getUser().getId(), dashboardsOnline.get(0).getHProject().getUser().getId());

        // huser deletes hproject with call deleteHProject
        // and remove in cascade mode all dashboards
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponseRemoveHProject = hProjectRestApi.deleteHProject(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseRemoveHProject.getStatus());
        Assert.assertNull(restResponseRemoveHProject.getEntity());

        this.impersonateUser(dashboardRestApi, huser);
        restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertEquals(0, dashboardsOffline.size());
        Assert.assertTrue(dashboardsOffline.isEmpty());
        restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertEquals(0, dashboardsOnline.size());
        Assert.assertTrue(dashboardsOnline.isEmpty());

    }

    @Test
    public void test49_deleteDashboardWithPermissionNotRemovesInCascadeHProject() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // huser deletes dashboards (Offline, Realtime) with call deleteDashboard
        // this call not deletes in cascade hproject
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hprojectDashboard = createHProject(huser,false);
        Assert.assertNotEquals(0, hprojectDashboard.getId());
        Assert.assertEquals(huser.getId(), hprojectDashboard.getUser().getId());

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FINDALL);
        addPermission(huser, action1);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        List<Dashboard> dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOffline.get(0).getId());
        Assert.assertEquals(1, dashboardsOffline.size());
        Assert.assertEquals(hprojectDashboard.getName() + " Offline Dashboard", dashboardsOffline.get(0).getName());
        Assert.assertEquals("OFFLINE", dashboardsOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hprojectDashboard.getId(), dashboardsOffline.get(0).getHProject().getId());
        Assert.assertEquals(hprojectDashboard.getUser().getId(), dashboardsOffline.get(0).getHProject().getUser().getId());

        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        List<Dashboard> dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOnline.get(0).getId());
        Assert.assertEquals(1, dashboardsOnline.size());
        Assert.assertEquals(hprojectDashboard.getName() + " Online Dashboard", dashboardsOnline.get(0).getName());
        Assert.assertEquals("REALTIME", dashboardsOnline.get(0).getDashboardType().getType());
        Assert.assertEquals(hprojectDashboard.getId(), dashboardsOnline.get(0).getHProject().getId());
        Assert.assertEquals(hprojectDashboard.getUser().getId(), dashboardsOnline.get(0).getHProject().getUser().getId());

        // huser deletes dashboard Offline with call deleteDashboard
        // this call not deletes hproject
        this.impersonateUser(dashboardRestApi, huser);
        Response deleteDashboardOffline = dashboardRestApi.deleteDashboard(dashboardsOffline.get(0).getId());
        Assert.assertEquals(200, deleteDashboardOffline.getStatus());
        Assert.assertNull(deleteDashboardOffline.getEntity());

        //checks if dashboard Offline exists
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(dashboardRestApi, huser);
        Response findDashboardOffline = dashboardRestApi.findDashboard(dashboardsOffline.get(0).getId());
        Assert.assertEquals(404, findDashboardOffline.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) findDashboardOffline.getEntity()).getType());

        // dashboard Realtime is still stored in database
        restResponseDashboardOnline = dashboardRestApi.findDashboard(dashboardsOnline.get(0).getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());

        // huser deletes dashboard Realtime with call deleteDashboard
        // this call not deletes hproject
        this.impersonateUser(dashboardRestApi, huser);
        Response deleteDashboardOnline = dashboardRestApi.deleteDashboard(dashboardsOnline.get(0).getId());
        Assert.assertEquals(200, deleteDashboardOnline.getStatus());
        Assert.assertNull(deleteDashboardOnline.getEntity());

        //checks if dashboard Realtime exists
        Response findDashboardOnline = dashboardRestApi.findDashboard(dashboardsOnline.get(0).getId());
        Assert.assertEquals(404, findDashboardOnline.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) findDashboardOnline.getEntity()).getType());

        // hproject is still stored in database
        HyperIoTAction action3 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action3);
        this.impersonateUser(hProjectRestApi, huser);
        Response responseFindHProject = hProjectRestApi.findHProject(hprojectDashboard.getId());
        Assert.assertEquals(200, responseFindHProject.getStatus());
        Assert.assertEquals(hprojectDashboard.getId(), ((HProject) responseFindHProject.getEntity()).getId());
        Assert.assertEquals(huser.getId(), ((HProject) responseFindHProject.getEntity()).getUser().getId());
    }

    // HProject  action save: 1
    // Dashboard action find-all: 16
    @Test
    public void test049_saveHProjectAndCreateDefaultDashboardWithDefaultPermissionShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject with the following call saveHProject
        // and dashboards (Offline, Realtime) will be created automatically
        // response status code '200'
        huser = createHUserWithDefaultPermission();

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

        //checks if dashboards has been created
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);

        //checks if dashboard Offline has been created
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hproject.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        List<Dashboard> dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOffline.get(0).getId());
        Assert.assertEquals(hproject.getName()+ " Offline Dashboard",
                dashboardsOffline.get(0).getName());
        Assert.assertEquals("OFFLINE",
                dashboardsOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(),
                dashboardsOffline.get(0).getHProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                dashboardsOffline.get(0).getHProject().getUser().getId());

        //checks if dashboard Realtime has been created
        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hproject.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        List<Dashboard> dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOnline.get(0).getId());
        Assert.assertEquals(hproject.getName()+ " Online Dashboard",
                dashboardsOnline.get(0).getName());
        Assert.assertEquals("REALTIME",
                dashboardsOnline.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(),
                dashboardsOnline.get(0).getHProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                dashboardsOnline.get(0).getHProject().getUser().getId());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


    private Dashboard createDashboard(HProject hproject, String dashboardType) {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        HUser ownerHUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerHUser.getId(), hproject.getUser().getId());
        Assert.assertEquals("Project of user: " + ownerHUser.getUsername(), hproject.getDescription());
        addDefaultPermission(ownerHUser);

        Dashboard dashboard = new Dashboard();
        if (dashboardType == "OFFLINE") {
            dashboard.setName(hproject.getName() + " Offline Dashboard");
            dashboard.setDashboardType(DashboardType.OFFLINE);
            dashboard.setHProject(hproject);
            this.impersonateUser(dashboardRestApi, ownerHUser);
            Response restResponse = dashboardRestApi.saveDashboard(dashboard);
            Assert.assertEquals(200, restResponse.getStatus());
            Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
            Assert.assertEquals(hproject.getName() + " Offline Dashboard",
                    ((Dashboard) restResponse.getEntity()).getName());
            Assert.assertEquals("OFFLINE",
                    ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getId());
            Assert.assertEquals(ownerHUser.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
        }
        if (dashboardType == "REALTIME") {
            dashboard.setName(hproject.getName() + " Online Dashboard");
            dashboard.setDashboardType(DashboardType.REALTIME);
            dashboard.setHProject(hproject);
            this.impersonateUser(dashboardRestApi, ownerHUser);
            Response restResponse = dashboardRestApi.saveDashboard(dashboard);
            Assert.assertEquals(200, restResponse.getStatus());
            Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
            Assert.assertEquals(hproject.getName() + " Online Dashboard",
                    ((Dashboard) restResponse.getEntity()).getName());
            Assert.assertEquals("REALTIME",
                    ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getId());
            Assert.assertEquals(ownerHUser.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
        }
        if (dashboardType != "OFFLINE" && dashboardType != "REALTIME") {
            Assert.assertEquals(0, dashboard.getId());
            System.out.println("dashboardType is null, dashboard not created...");
            System.out.println("allowed values: OFFLINE, REALTIME");
            return null;
        }
        removeDefaultPermission(ownerHUser);
        return dashboard;
    }

    private HProject createHProject(HUser huser, boolean deleteDashboard) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
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
        if (deleteDashboard) {
            deleteDefaultDashboard(hproject);
        }
        removeDefaultPermission(huser);
        return hproject;
    }

    private void deleteDefaultDashboard(HProject hproject) {
        // utility method: deletes dashboard Offline and Realtime created in hproject with method createHProject()
        HUser ownerHUser = hproject.getUser();

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, ownerHUser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hproject.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        List<Dashboard> dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        if (dashboardsOffline.size() > 0) {
            Assert.assertNotEquals(0, dashboardsOffline.get(0).getId());
            Assert.assertEquals(1, dashboardsOffline.size());
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboardsOffline.get(0).getName());
            Assert.assertEquals("OFFLINE", dashboardsOffline.get(0).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(), dashboardsOffline.get(0).getHProject().getId());
            Assert.assertEquals(hproject.getUser().getId(), dashboardsOffline.get(0).getHProject().getUser().getId());

            this.impersonateUser(dashboardRestApi, ownerHUser);
            Response deleteDashboardOffline = dashboardRestApi.deleteDashboard(dashboardsOffline.get(0).getId());
            Assert.assertEquals(200, deleteDashboardOffline.getStatus());
            Assert.assertNull(deleteDashboardOffline.getEntity());
        }

        this.impersonateUser(dashboardRestApi, ownerHUser);
        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hproject.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        List<Dashboard> dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        if (dashboardsOnline.size() > 0) {
            Assert.assertNotEquals(0, dashboardsOnline.get(0).getId());
            Assert.assertEquals(1, dashboardsOnline.size());
            Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboardsOnline.get(0).getName());
            Assert.assertEquals("REALTIME", dashboardsOnline.get(0).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(), dashboardsOnline.get(0).getHProject().getId());
            Assert.assertEquals(hproject.getUser().getId(), dashboardsOnline.get(0).getHProject().getUser().getId());

            this.impersonateUser(dashboardRestApi, ownerHUser);
            Response deleteDashboardOnline = dashboardRestApi.deleteDashboard(dashboardsOnline.get(0).getId());
            Assert.assertEquals(200, deleteDashboardOnline.getStatus());
            Assert.assertNull(deleteDashboardOnline.getEntity());
        }
    }

    private Area createArea(HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        HUser ownerHUser = hproject.getUser();
        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        addDefaultPermission(ownerHUser);
        this.impersonateUser(areaRestApi, ownerHUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals(area.getDescription(), ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        removeDefaultPermission(ownerHUser);
        return area;
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
            Assert.assertEquals(dashboardResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
            Assert.assertEquals(action.getActionId(), permission.getActionIds());
            Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
            Assert.assertEquals(role.getId(), permission.getRole().getId());
        }
        return huser;
    }

    private HUser createHUserWithDefaultPermission() {
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
        addDefaultPermission(huser);
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
                permission.setName(dashboardResourceName + " assigned to huser_id " + huser.getId());
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
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        Response restResponse = roleRestApi.saveRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Role) restResponse.getEntity()).getId());
        Assert.assertEquals(role.getName(), ((Role) restResponse.getEntity()).getName());
        Assert.assertEquals(role.getDescription(), ((Role) restResponse.getEntity()).getDescription());
        return role;
    }


    // Dashboard is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;
    private HUser huser2;

    @After
    public void afterTest() {
        // Remove projects and delete in cascade all dashboardWidgets created in every test
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


    private Permission addPermission(HUser huser, HyperIoTAction action) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        Permission permission = utilGrantPermission(huser, role, action);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertEquals(dashboardResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
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

}

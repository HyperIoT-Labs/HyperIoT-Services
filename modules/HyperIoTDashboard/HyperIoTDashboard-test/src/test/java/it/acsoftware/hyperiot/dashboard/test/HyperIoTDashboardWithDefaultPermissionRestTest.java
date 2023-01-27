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
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;

import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import it.acsoftware.hyperiot.dashboard.service.rest.DashboardRestApi;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import it.acsoftware.hyperiot.dashboard.widget.service.rest.DashboardWidgetRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
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
public class HyperIoTDashboardWithDefaultPermissionRestTest extends KarafTestSupport {

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
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Dashboard Module works!", restResponse.getEntity());
    }


    // Dashboard action save: 1
    @Test
    public void test02_saveDashboardWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, save Dashboard with the following call saveDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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


    // Dashboard action update: 2
    @Test
    public void test03_updateDashboardWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, update Dashboard with the following call updateDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        Date date = new Date();
        dashboard.setName("Dashboard edited in date: " + date);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getEntityVersion() + 1,
                ((Dashboard) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("Dashboard edited in date: " + date,
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals(dashboard.getHProject().getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    // Dashboard action update: 2
    @Test
    public void test04_updateDashboardRealTimeInOfflineWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, update Dashboard with the following call updateDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName(hproject.getName() + " Offline Dashboard");
        dashboard.setDashboardType(DashboardType.OFFLINE);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getEntityVersion() + 1,
                ((Dashboard) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard",
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals("OFFLINE",
                ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
        Assert.assertEquals(dashboard.getHProject().getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    // Dashboard action remove: 4
    @Test
    public void test05_deleteDashboardWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, delete Dashboard with the following call deleteDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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
        Response restResponse = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // Dashboard action find: 8
    @Test
    public void test06_findDashboardWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, find Dashboard with the following call findDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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
        Assert.assertEquals(hproject.getName() + " Online Dashboard", ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals("REALTIME", ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(), ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    // Dashboard action find-all: 16
    @Test
    public void test07_findAllDashboardWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, find all Dashboard with the following call findAllDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

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
        Assert.assertEquals(200, restResponse.getStatus());
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
    }


    // Dashboard action find-all: 16
    @Test
    public void test08_findAllDashboardPaginatedWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with default permission,
        // find all Dashboard with pagination
        // response status code '200'
        int delta = 8;
        int page = 1;
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Dashboard dashboard = createDashboard(hproject, "OFFLINE");
            Assert.assertNotEquals(0, dashboard.getId());
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
            Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(delta, dashboards.size());
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboard.getResults().size());
        Assert.assertEquals(delta, listDashboard.getDelta());
        Assert.assertEquals(page, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // delta is 8, page is 1: 8 entities stored in database
        Assert.assertEquals(1, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // Dashboard action find-all: 16
    @Test
    public void test09_findAreaRealtimeDashboardWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with default permission, finds all realtime Dashboard associated to the area
        // with the following call findAreaRealtimeDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser, true);
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


    // Dashboard action find-all: 16
    @Test
    public void test10_findAreaOfflineDashboardWithDefaultPermissionShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with default permission, finds all offline Dashboard associated to the area
        // with the following call findAreaOfflineDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser, true);
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


    // Dashboard action find_widgets: 32 (DashboardWidget)
    @Test
    public void test11_findAllDashboardWidgetInDashboardWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find all available dashboard widgets inside a particular
        // dashboard with the following call findAllDashboardWidgetInDashboard
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser, true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboardWidget.getDashboard().getId());
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertFalse(listDashboardWidgets.isEmpty());
        Assert.assertEquals(1, listDashboardWidgets.size());
        boolean dashboardWidgetFound = false;
        for (DashboardWidget dw : listDashboardWidgets) {
            if (dashboardWidget.getId() == dw.getId()) {
                Assert.assertEquals(dashboard.getId(), dw.getDashboard().getId());
                Assert.assertEquals(hproject.getId(), dw.getDashboard().getHProject().getId());
                Assert.assertEquals(huser.getId(), dw.getDashboard().getHProject().getUser().getId());
                dashboardWidgetFound = true;
            }
        }
        Assert.assertTrue(dashboardWidgetFound);
        Assert.assertEquals(200, restResponse.getStatus());
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
        return dashboard;
    }

    private HProject createHProject(HUser huser, boolean deleteDashboard) {
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
        if (deleteDashboard){
            deleteDefaultDashboard(hproject);
        }
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
        area.setName("Area " + UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, ownerHUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals(area.getDescription(), ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        return area;
    }

    private DashboardWidget createDashboardWidget(Dashboard dashboard) {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        HUser ownerHUser = dashboard.getHProject().getUser();
        Assert.assertEquals(ownerHUser.getId(), dashboard.getHProject().getUser().getId());
        Assert.assertEquals("Project of user: " + ownerHUser.getUsername(), dashboard.getHProject().getDescription());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}");
        dashboardWidget.setDashboard(dashboard);

        this.impersonateUser(dashboardWidgetRestApi, ownerHUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + ownerHUser.getUsername() + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(dashboard.getHProject().getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
        return dashboardWidget;
    }

    // Dashboard is Owned Resource: only huser is able to find/findAll his entities
    private HUser huser;

    @After
    public void afterTest() {
        // Remove projects and delete in cascade all dashboards created in every test
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
//        String sqlHProject = "select * from hproject";
//        String resultHProject = executeCommand("jdbc:query hyperiot " + sqlHProject);
//        System.out.println(resultHProject);
//
//        String sqlDashboard = "select * from dashboard";
//        String resultDashboard = executeCommand("jdbc:query hyperiot " + sqlDashboard);
//        System.out.println(resultDashboard);
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
                if (permission.getEntityResourceName().contains(permissionDashboard)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionDashboard, permission.getEntityResourceName());
                    Assert.assertEquals(permissionDashboard + nameRegisteredPermission, permission.getName());
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

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

package it.acsoftware.hyperiot.dashboard.widget.test;

import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
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

import static it.acsoftware.hyperiot.dashboard.widget.test.HyperIoTDashboardWidgetConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for DashboardWidget System
 * Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDashboardWidgetWithDefaultPermissionRestTest extends KarafTestSupport {

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
    public void test01_dashboardWidgetModuleShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call checkModuleWorking checks if DashboardWidget module working
        // correctly
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("DashboardWidget Module works!", restResponse.getEntity());
    }


    // DashboardWidget action save: 1
    @Test
    public void test02_saveDashboardWidgetWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, save DashboardWidget with the following call saveDashboardWidget
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + huser.getUsername() + "\"}");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(hproject.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
    }


    // DashboardWidget action update: 2
    @Test
    public void test03_updateDashboardWidgetWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, update DashboardWidget with the following call updateDashboardWidget
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        Date date = new Date();
        dashboardWidget.setWidgetConf("{\"description\":\"description edited in date: \"" + date + "\"}");
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getEntityVersion() + 1,
                ((DashboardWidget) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("{\"description\":\"description edited in date: \"" + date + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
    }


    // DashboardWidget action update: 2
    @Test
    public void test04_setDashboardWidgetConfWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, update dashboard widget configuration with
        // the following call setDashboardWidgetConf
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        String widgetConf = "{\"description\":\"test setDashboardWidgetConf\"}";
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(widgetConf, ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
    }


    // DashboardWidget action remove: 4
    @Test
    public void test05_deleteDashboardWidgetWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, delete DashboardWidget with the following call deleteDashboardWidget
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // DashboardWidget action find: 8
    @Test
    public void test06_findDashboardWidgetWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find DashboardWidget with the following call findDashboardWidget
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getId(), ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals(dashboard.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(hproject.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
    }


    // DashboardWidget action find: 8
    @Test
    public void test07_findDashboardWidgetConfWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find dashboard widget configuration with the following
        // call findDashboardWidgetConf
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonDashboardWidgetConf = (String) restResponse.getEntity();
        Assert.assertEquals(dashboardWidget.getWidgetConf(), jsonDashboardWidgetConf);
    }


    // DashboardWidget action find-all: 16
    @Test
    public void test08_findAllDashboardWidgetWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find all DashboardWidget with the following call findAllDashboardWidget
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidget();
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


    // DashboardWidget action find-all: 16
    @Test
    public void test09_findAllDashboardWidgetPaginatedWithDefaultPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with default permission,
        // find all DashboardWidget with pagination
        // response status code '200'
        int delta = 7;
        int page = 2;
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(defaultDelta, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listDashboardWidget.getResults().size());
        Assert.assertEquals(delta, listDashboardWidget.getDelta());
        Assert.assertEquals(page, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidget.getNextPage());
        // delta is 7, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponsePage1 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 1);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidgetPage1.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage1.getCurrentPage());
        Assert.assertEquals(page, listDashboardWidgetPage1.getNextPage());
        // delta is 7, page is 1: 10 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
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


    // DashboardWidget is Owned Resource: only huser is able to find/findAll his entities
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
//        String sqlDashboardWidget = "select * from dashboardWidget";
//        String resultDashboardWidget = executeCommand("jdbc:query hyperiot " + sqlDashboardWidget);
//        System.out.println(resultDashboardWidget);
//
//        String sqlHUser = "select * from huser";
//        String resultHUser = executeCommand("jdbc:query hyperiot " + sqlHUser);
//        System.out.println(resultHUser);
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
                if (permission.getEntityResourceName().contains(permissionDashboardWidget)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionDashboardWidget, permission.getEntityResourceName());
                    Assert.assertEquals(permissionDashboardWidget + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(31, permission.getActionIds());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameFound = true;
                }
            }
            Assert.assertTrue(resourceNameFound);
        }
        return huser;
    }

}

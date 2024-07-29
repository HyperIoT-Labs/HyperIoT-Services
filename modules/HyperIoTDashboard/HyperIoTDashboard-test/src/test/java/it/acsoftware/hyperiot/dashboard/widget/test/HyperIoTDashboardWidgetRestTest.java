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

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
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
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import it.acsoftware.hyperiot.dashboard.widget.service.rest.DashboardWidgetRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
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
import java.util.ArrayList;
import java.util.List;

import static it.acsoftware.hyperiot.dashboard.widget.test.HyperIoTDashboardWidgetConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for DashboardWidget System
 * Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDashboardWidgetRestTest extends KarafTestSupport {

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
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("DashboardWidget Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_saveDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin save DashboardWidget with the following call saveDashboardWidget
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(hproject.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
    }

    @Test
    public void test03_saveDashboardWidgetShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to save DashboardWidget, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"description test\"}");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test04_updateDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin update DashboardWidget with the following call updateDashboardWidget
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        dashboardWidget.setWidgetConf("{\"description\":\"description edited\"}");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getEntityVersion() + 1,
                ((DashboardWidget) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("{\"description\":\"description edited\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
    }

    @Test
    public void test05_updateDashboardWidgetShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to update DashboardWidget, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        dashboardWidget.setWidgetConf("{\"description\":\"description edited\"}");
        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin find DashboardWidget with the following call findDashboardWidget
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getId(), ((DashboardWidget) restResponse.getEntity()).getId());
    }

    @Test
    public void test07_findDashboardWidgetShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to find DashboardWidget, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findDashboardWidgetShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to find DashboardWidget with the following call findDashboardWidget,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_deleteDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin delete DashboardWidget with the following call deleteDashboardWidget
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test10_deleteDashboardWidgetShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to delete DashboardWidget, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertTrue(adminUser.isAdmin());
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test11_deleteDashboardWidgetShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to delete DashboardWidget with the following call deleteDashboardWidget,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_findAllDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin find all DashboardWidget with the following call findAllDashboardWidget
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
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
                Assert.assertEquals(adminUser.getId(), dw.getDashboard().getHProject().getUser().getId());
                dashboardWidgetFound = true;
            }
        }
        Assert.assertTrue(dashboardWidgetFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test13_findAllDashboardWidgetShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to find all DashboardWidget, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidget();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_findDashboardWidgetConfShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin find dashboard widget configuration with the following call findDashboardWidgetConf
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getWidgetConf(), restResponse.getEntity());
    }

    @Test
    public void test15_findDashboardWidgetConfShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to find dashboard widget configuration, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(dashboardWidget.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_findDashboardWidgetConfShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin find dashboard widget configuration with the following call findDashboardWidgetConf,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test17_setDashboardWidgetConfShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin update dashboard widget configuration with the following call setDashboardWidgetConf
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        String widgetConf = "{\"description\":\"test setDashboardWidgetConf\"}";
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(widgetConf, (((DashboardWidget) restResponse.getEntity()).getWidgetConf()));
    }

    @Test
    public void test18_setDashboardWidgetConfShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to update dashboard widget configuration, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, null);
        String widgetConf = "{\"description\":\"test setDashboardWidgetConf\"}";
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test19_setDashboardWidgetConfShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update dashboard widget configuration with the following call setDashboardWidgetConf,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        String widgetConf = "{\"description\":\"test setDashboardWidgetConf\"}";
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(0, widgetConf);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test20_setDashboardWidgetConfShouldFailIfWidgetConfIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update dashboard widget configuration with the following call setDashboardWidgetConf,
        // but widgetConf is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), null);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test21_setDashboardWidgetConfShouldFailIfWidgetConfIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update dashboard widget configuration with the following call setDashboardWidgetConf,
        // but widgetConf is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        String widgetConf = "";
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(widgetConf, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test22_setDashboardWidgetConfShouldFailIfWidgetConfIsMaliciousCode() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update dashboard widget configuration with the following call setDashboardWidgetConf,
        // but widgetConf is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        String widgetConf = "javascript:";
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(widgetConf, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test23_findAllDashboardWidgetInDashboardShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin find all available dashboard widgets inside a particular dashboard with
        // the following call findAllDashboardWidgetInDashboard
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboard.getId());
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertFalse(listDashboardWidgets.isEmpty());
        Assert.assertEquals(1, listDashboardWidgets.size());
        boolean dashboardWidgetFound = false;
        for (DashboardWidget dw : listDashboardWidgets) {
            if (dashboardWidget.getId() == dw.getId()) {
                Assert.assertEquals(dashboard.getId(), dw.getDashboard().getId());
                Assert.assertEquals(hproject.getId(), dw.getDashboard().getHProject().getId());
                Assert.assertEquals(adminUser.getId(), dw.getDashboard().getHProject().getUser().getId());
                dashboardWidgetFound = true;
            }
        }
        Assert.assertTrue(dashboardWidgetFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test24_findAllDashboardWidgetInDashboardShouldWorkIfDashboardNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin find all available dashboard widgets inside a particular dashboard with
        // the following call findAllDashboardWidgetInDashboard, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test25_findAllDashboardWidgetInDashboardShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to find all dashboard widget configuration, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject();
        HUser adminUser = hproject.getUser();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboard.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test26_findAllDashboardWidgetInDashboardShouldWorkIfListWidgetsIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to finds all available dashboard widgets inside a particular
        // dashboard with the following call findAllDashboardWidgetInDashboard,
        // but list widget is empty
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboard.getId());
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertEquals(0, listDashboardWidgets.size());
        Assert.assertTrue(listDashboardWidgets.isEmpty());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test27_saveDashboardWidgetShouldFailIfWidgetConfIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to save DashboardWidget with the following call saveDashboardWidget,
        // but widgetConf (String in JSON format) is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf(null);
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test28_saveDashboardWidgetShouldFailIfWidgetConfIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to save DashboardWidget with the following call saveDashboardWidget,
        // but widgetConf (String in JSON format) is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboardWidget.getWidgetConf(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test29_saveDashboardWidgetShouldFailIfWidgetConfIsMaliciousCode() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to save DashboardWidget with the following call saveDashboardWidget,
        // but widgetConf (String in JSON format) is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("onload(malicious code)=");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size()));
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboardWidget.getWidgetConf(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test30_saveDashboardWidgetShouldFailIfDashboardIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to save DashboardWidget with the following call saveDashboardWidget,
        // but Dashboard is null
        // response status code 404 HyperIoTEntityNotFound (Dashboard related to DashboardWidget cannot be null)
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"it's a simple test description\"}");
        dashboardWidget.setDashboard(null);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test31_updateDashboardWidgetShouldFailIfWidgetConfIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update DashboardWidget with the following call updateDashboardWidget,
        // but widgetConf (String in JSON format) is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        dashboardWidget.setWidgetConf(null);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test32_updateDashboardWidgetShouldFailIfWidgetConfIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update DashboardWidget with the following call updateDashboardWidget,
        // but widgetConf (String in JSON format) is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        dashboardWidget.setWidgetConf("");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboardWidget.getWidgetConf(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test33_updateDashboardWidgetShouldFailIfWidgetConfIsMaliciousCode() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update DashboardWidget with the following call updateDashboardWidget,
        // but widgetConf (String in JSON format) is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        dashboardWidget.setWidgetConf("</script>");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboardwidget-widgetconf", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(dashboardWidget.getWidgetConf(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test34_updateDashboardWidgetShouldFailIfDashboardIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update DashboardWidget with the following call updateDashboardWidget,
        // but Dashboard is null
        // response status code '404' HyperIoTEntityNotFound (Dashboard related to dashboard widget cannot be null).
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        dashboardWidget.setDashboard(null);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test35_updateDashboardWidgetShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to update DashboardWidget with the following call updateDashboardWidget,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        // entity isn't stored in database
        HProject project = createHProject();
        Dashboard dashboard = createDashboard(project,"OFFLINE");
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setDashboard(dashboard);
        dashboardWidget.setWidgetConf("{\"description\":\"description edited\"}");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test36_findAllDashboardWidgetPaginatedShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget, hadmin find all DashboardWidget with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 4;
        int page = 2;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(defaultDelta, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidget.getResults().size());
        Assert.assertEquals(delta, listDashboardWidget.getDelta());
        Assert.assertEquals(page, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(page + 1, listDashboardWidget.getNextPage());
        // delta is 4, page is 2: 10 entities stored in database
        Assert.assertEquals(3, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponsePage1 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 1);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidgetPage1.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage1.getCurrentPage());
        Assert.assertEquals(page, listDashboardWidgetPage1.getNextPage());
        // delta is 4, page is 1: 10 entities stored in database
        Assert.assertEquals(3, listDashboardWidgetPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        // checks with page = 3
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponsePage3 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 3);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage3 = restResponsePage3
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage3.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - (delta * 2), listDashboardWidgetPage3.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage3.getDelta());
        Assert.assertEquals(page + 1, listDashboardWidgetPage3.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage3.getNextPage());
        // delta is 4, page is 3: 10 entities stored in database
        Assert.assertEquals(3, listDashboardWidgetPage3.getNumPages());
        Assert.assertEquals(200, restResponsePage3.getStatus());
    }

    @Test
    public void test37_findAllDashboardWidgetPaginatedShouldWorkIfDeltaAndPageAreNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget, hadmin find all DashboardWidget with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(defaultDelta, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidget.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test38_findAllDashboardWidgetPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget, hadmin find all DashboardWidget with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 3;

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        int numbEntities = 21;
        for (int i = 0; i < numbEntities; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(numbEntities, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(numbEntities - (defaultDelta * 2), listDashboardWidget.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getDelta());
        Assert.assertEquals(page, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidget.getNextPage());
        // default delta is 10, page is 3: 21 entities stored in database
        Assert.assertEquals(3, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponsePage1 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 1);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardWidgetPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidgetPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboardWidgetPage1.getNextPage());
        // default delta is 10, page is 1: 21 entities stored in database
        Assert.assertEquals(3, listDashboardWidgetPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponsePage2 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 2);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardWidgetPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidgetPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardWidgetPage2.getCurrentPage());
        Assert.assertEquals(page, listDashboardWidgetPage2.getNextPage());
        // default delta is 10, page is 2: 21 entities stored in database
        Assert.assertEquals(3, listDashboardWidgetPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test39_findAllDashboardWidgetPaginatedShouldWorkIfDeltaIsZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget, hadmin find all DashboardWidget with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 1;

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(defaultDelta, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getDelta());
        Assert.assertEquals(page, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(page, listDashboardWidget.getNextPage());
        // default delta is 10, page is 1: 10 entities stored in database
        Assert.assertEquals(1, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test40_findAllDashboardWidgetPaginatedShouldWorkIfPageIsLowerThanZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget, hadmin find all DashboardWidget with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 6;
        int page = -1;

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(defaultDelta, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidget.getResults().size());
        Assert.assertEquals(delta, listDashboardWidget.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboardWidget.getNextPage());
        // delta is 6, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponsePage2 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 2);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listDashboardWidgetPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardWidgetPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage2.getNextPage());
        // delta is 6, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test41_findAllDashboardWidgetPaginatedShouldWorkIfPageIsZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget, hadmin find all DashboardWidget with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 9;
        int page = 0;

        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(defaultDelta, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidget.getResults().size());
        Assert.assertEquals(delta, listDashboardWidget.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboardWidget.getNextPage());
        // delta is 9, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponsePage2 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 2);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listDashboardWidgetPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardWidgetPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage2.getNextPage());
        // delta is 9, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test42_findAllDashboardWidgetPaginatedShouldFailIfNotLogged() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // the following call tries to find all DashboardWidget with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(dashboardWidgetRestApi, null);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test43_saveAllDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin save all dashboard widget with the following call saveAllDashboardWidget
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget1 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget1.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget1.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget1.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget1.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget3 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget3.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget3.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget3.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget3.getDashboard().getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration = {dashboardWidget3, dashboardWidget2, dashboardWidget3};

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(dashboard.getId(), widgetConfiguration);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test44_saveAllDashboardWidgetShouldFailIfDashboardNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin tries to save all dashboard widget with the following call saveAllDashboardWidget,
        // but Dashboard not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget1 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget1.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget1.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget1.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget1.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget3 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget3.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget3.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget3.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget3.getDashboard().getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration = {dashboardWidget1, dashboardWidget2, dashboardWidget3};

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(0, widgetConfiguration);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test45_saveAllDashboardWidgetShouldWorkIfDashboardWidgetIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin save all dashboard widget with the following call saveAllDashboardWidget,
        // whit this call hadmin remove all DashboardWidget into Dashboard
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration = {};

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(dashboard.getId(), widgetConfiguration);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test46_removeHProjectDeleteInCascadeAllDashboardAndDashboardWidget() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin remove HProject with the following call deleteHProject and delete in cascade mode
        // all Dashboard and all DashboardWidget
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, adminUser);
        Response responseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());

        // checks: dashboard has been deleted with deleteHProject call
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, adminUser);
        Response responseFindDashboard = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(404, responseFindDashboard.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDashboard.getEntity()).getType());

        // checks: dashboardWidget has been deleted with deleteHProject call
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response responseFindDashboardWidget = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(404, responseFindDashboardWidget.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDashboardWidget.getEntity()).getType());
    }


    @Test
    public void test47_removeDashboardNotDeleteHProjectButDeleteInCascadeAllDashboardWidget() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin remove Dashboard with the following call deleteDashboard and delete in cascade mode
        // all DashboardWidget but not deletes HProject
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, adminUser);
        Response responseDeleteDashboard = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(200, responseDeleteDashboard.getStatus());

        // checks: hproject is already stored in database
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, adminUser);
        Response responseDeleteHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());

        // checks: dashboardWidget has been deleted with deleteDashboard call
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response responseFindDashboardWidget = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(404, responseFindDashboardWidget.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDashboardWidget.getEntity()).getType());
    }


    @Test
    public void test48_removeDashboardWidgetNotDeleteInCascadeAllHProjectAndAllDashboard() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin remove DashboardWidget with the following call deleteDashboardWidget; this call
        // not delete in cascade mode all HProject and all Dashboard
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject();
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + adminUser.getUsername() + "\"}",
                dashboardWidget.getWidgetConf());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response responseDeleteDashboardWidget = dashboardWidgetRestApi.deleteDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, responseDeleteDashboardWidget.getStatus());

        // checks: hproject is already stored in database
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, adminUser);
        Response responseFindHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, responseFindHProject.getStatus());

        // checks: dashboard is already stored in database
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, adminUser);
        Response responseFindDashboard = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(200, responseFindDashboard.getStatus());
    }


    @Test
    public void test49_findAllDashboardWidgetShouldWorkIfListIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // hadmin find all DashboardWidget with the following call findAllDashboardWidget
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidget();
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertTrue(listDashboardWidgets.isEmpty());
        Assert.assertEquals(0, listDashboardWidgets.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test50_findAllDashboardWidgetPaginatedShouldWorkIfListIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget hadmin find all DashboardWidget with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(defaultDelta, defaultPage);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertTrue(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(0, listDashboardWidget.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidget.getNextPage());
        // default delta is 10, default page is 1: there are not entities stored in database
        Assert.assertEquals(0, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


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

    private Dashboard createDashboard(HProject hproject, String dashboardType) {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");

        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());
        Assert.assertEquals("Project of user: " + adminUser.getUsername(), hproject.getDescription());

        Dashboard dashboard = new Dashboard();
        if (dashboardType == "OFFLINE") {
            dashboard.setName(hproject.getName() + " Offline Dashboard");
            dashboard.setDashboardType(DashboardType.OFFLINE);
            dashboard.setHProject(hproject);
            this.impersonateUser(dashboardRestApi, adminUser);
            Response restResponse = dashboardRestApi.saveDashboard(dashboard);
            Assert.assertEquals(200, restResponse.getStatus());
            Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
            Assert.assertEquals(hproject.getName() + " Offline Dashboard",
                    ((Dashboard) restResponse.getEntity()).getName());
            Assert.assertEquals("OFFLINE",
                    ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getId());
            Assert.assertEquals(adminUser.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
        }
        if (dashboardType == "REALTIME") {
            dashboard.setName(hproject.getName() + " Online Dashboard");
            dashboard.setDashboardType(DashboardType.REALTIME);
            dashboard.setHProject(hproject);
            this.impersonateUser(dashboardRestApi, adminUser);
            Response restResponse = dashboardRestApi.saveDashboard(dashboard);
            Assert.assertEquals(200, restResponse.getStatus());
            Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
            Assert.assertEquals(hproject.getName() + " Online Dashboard",
                    ((Dashboard) restResponse.getEntity()).getName());
            Assert.assertEquals("REALTIME",
                    ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getId());
            Assert.assertEquals(adminUser.getId(),
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
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, adminUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(dashboard.getHProject().getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(adminUser.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
        return dashboardWidget;
    }

    @After
    public void afterTest() {
        // Remove all projects and delete in cascade all associated entities (DashboardWidget) in every tests
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
//        String sqlHProject = "select * from hproject";
//        String resultHProject = executeCommand("jdbc:query hyperiot " + sqlHProject);
//        System.out.println(resultHProject);

//        String sqlDashboardWidget = "select * from dashboardWidget";
//        String resultDashboardWidget = executeCommand("jdbc:query hyperiot " + sqlDashboardWidget);
//        System.out.println(resultDashboardWidget);
    }


}

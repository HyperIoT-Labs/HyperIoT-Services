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
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import it.acsoftware.hyperiot.dashboard.service.rest.DashboardRestApi;
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

import static it.acsoftware.hyperiot.dashboard.test.HyperIoTDashboardConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for Dashboard System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDashboardRestTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @SuppressWarnings("unused")
    private HyperIoTAction getHyperIoTAction(String resourceName, HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder.createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
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
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Dashboard Module works!", restResponse.getEntity());
    }


    @Test
    public void test02_saveDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin save Dashboard with the following call saveDashboard
        // response status code '200'
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = new Dashboard();
        dashboard.setName("dashboard name");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
        Assert.assertEquals("dashboard name",
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals("REALTIME",
                ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(adminUser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    @Test
    public void test03_saveDashboardShouldFailIfNotLogged() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // the following call tries to save Dashboard, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        HUser adminUser = hproject.getUser();
        Assert.assertEquals("Project of user: " + adminUser.getUsername(), hproject.getDescription());

        Dashboard dashboard = new Dashboard();
        dashboard.setName("dashboard name");
        dashboard.setDashboardType(DashboardType.OFFLINE);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, null);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test04_updateDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin update Dashboard with the following call updateDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName("Dashboard Edited");
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getEntityVersion() + 1,
                (((Dashboard) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals(dashboard.getName(), (((Dashboard) restResponse.getEntity()).getName()));
        Assert.assertEquals("Dashboard Edited",
                ((Dashboard) restResponse.getEntity()).getName());
    }


    @Test
    public void test05_updateDashboardShouldFailIfNotLogged() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // the following call tries to update Dashboard, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        HUser adminUser = hproject.getUser();
        Assert.assertEquals("Project of user: " + adminUser.getUsername(), hproject.getDescription());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName("dashboard edited");
        this.impersonateUser(dashboardRestApi, null);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test06_findDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin find Dashboard with the following call findDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getId(), ((Dashboard) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    @Test
    public void test07_findDashboardShouldFailIfNotLogged() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // the following call tries to find Dashboard, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        HUser adminUser = hproject.getUser();
        Assert.assertEquals("Project of user: " + adminUser.getUsername(), hproject.getDescription());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, null);
        Response restResponse = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test08_findDashboardShouldFailIfEntityNotFound() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to find Dashboard with the following call findDashboard,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test09_deleteDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin delete Dashboard with the following call deleteDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test10_deleteDashboardShouldFailIfNotLogged() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // the following call tries to update Dashboard, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        HUser adminUser = hproject.getUser();
        Assert.assertEquals("Project of user: " + adminUser.getUsername(), hproject.getDescription());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, null);
        Response restResponse = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test11_deleteDashboardShouldFailIfEntityNotFound() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to delete Dashboard with the following call deleteDashboard,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.deleteDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test12_findAllDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin find all Dashboard with the following call findAllDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboard();
        List<Dashboard> listDashboards = restResponse.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertFalse(listDashboards.isEmpty());
        Assert.assertEquals(1, listDashboards.size());
        boolean dashboardFound = false;
        for (Dashboard d : listDashboards) {
            if (dashboard.getId() == d.getId()) {
                Assert.assertEquals(d.getName(), dashboard.getName());
                Assert.assertEquals(hproject.getId(), d.getHProject().getId());
                Assert.assertEquals(adminUser.getId(), d.getHProject().getUser().getId());
                dashboardFound = true;
            }
        }
        Assert.assertTrue(dashboardFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test13_findAllDashboardShouldFailIfNotLogged() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // the following call tries to find all Dashboard, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(dashboardRestApi, null);
        Response restResponse = dashboardRestApi.findAllDashboard();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test14_saveDashboardShouldFailIfNameIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to save Dashboard with the following call saveDashboard,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Dashboard dashboard = new Dashboard();
        dashboard.setName(null);
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test15_saveDashboardShouldFailIfNameIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to save Dashboard with the following call saveDashboard,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Dashboard dashboard = new Dashboard();
        dashboard.setName("");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test16_saveDashboardShouldFailIfNameIsMaliciousCode() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to save Dashboard with the following call saveDashboard,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Dashboard dashboard = new Dashboard();
        dashboard.setName("javascript:");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test17_saveDashboardShouldFailIfHProjectIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to save Dashboard with the following call saveDashboard,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Dashboard dashboard = new Dashboard();
        dashboard.setName("dashboard name");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(null);
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-hproject", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test18_updateDashboardShouldFailIfNameIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to update Dashboard with the following call updateDashboard,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName(null);
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test19_updateDashboardShouldFailIfNameIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to update Dashboard with the following call updateDashboard,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName("");
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test20_updateDashboardShouldFailIfNameIsMaliciousCode() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to update Dashboard with the following call updateDashboard,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName("expression(malicious code)");
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test21_updateDashboardShouldFailIfHProjectIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to update Dashboard with the following call updateDashboard,
        // but HProject is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());
        dashboard.setHProject(null);
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("dashboard-hproject", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test22_updateDashboardShouldFailIfEntityNotFound() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to update Dashboard with the following call updateDashboard,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        // entity isn't stored in database
        Dashboard dashboard = new Dashboard();
        dashboard.setName("entity not found...");
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test23_findAllDashboardPaginatedShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, hadmin find all Dashboard with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = 1;

        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        int numbEntities = 3;
        for (int i = 0; i < numbEntities; i++) {
            Dashboard dashboard = createDashboard(hproject, "REALTIME");
            Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(numbEntities, dashboards.size());
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listDashboard.getResults().size());
        Assert.assertEquals(delta, listDashboard.getDelta());
        Assert.assertEquals(page, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // delta is 5, page 1: 3 entities stored in database
        Assert.assertEquals(1, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test24_findAllDashboardPaginatedShouldWorkIfDeltaAndPageAreNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, hadmin find all Dashboard with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;

        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Dashboard dashboard = createDashboard(hproject, "OFFLINE");
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(defaultDelta, dashboards.size());
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboard.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboard.getDelta());
        Assert.assertEquals(defaultPage, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // default delta is 10, default page 1: 10 entities stored in database
        Assert.assertEquals(1, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test25_findAllDashboardPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, hadmin find all Dashboard with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 2;

        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        int numbEntities = 11;
        for (int i = 0; i < numbEntities; i++) {
            Dashboard dashboard = createDashboard(hproject, "OFFLINE");
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(numbEntities, dashboards.size());
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listDashboard.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboard.getDelta());
        Assert.assertEquals(page, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // default delta is 10, page 2: 11 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponsePage1 = dashboardRestApi.findAllDashboardPaginated(delta, 1);
        HyperIoTPaginableResult<Dashboard> listDashboardPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardPage1.getCurrentPage());
        Assert.assertEquals(page, listDashboardPage1.getNextPage());
        // default delta is 10, page 1: 11 entities stored in database
        Assert.assertEquals(2, listDashboardPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test26_findAllDashboardPaginatedShouldWorkIfDeltaIsZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, hadmin find all Dashboard with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 2;

        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        int numbEntities = 13;
        for (int i = 0; i < numbEntities; i++) {
            Dashboard dashboard = createDashboard(hproject, "REALTIME");
            Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(numbEntities, dashboards.size());
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listDashboard.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboard.getDelta());
        Assert.assertEquals(page, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboard.getNextPage());
        // default delta is 10, page 2: 13 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponsePage1 = dashboardRestApi.findAllDashboardPaginated(delta, 1);
        HyperIoTPaginableResult<Dashboard> listDashboardPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardPage1.getCurrentPage());
        Assert.assertEquals(page, listDashboardPage1.getNextPage());
        // default delta is 10, page 1: 13 entities stored in database
        Assert.assertEquals(2, listDashboardPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test27_findAllDashboardPaginatedShouldWorkIfPageIsLowerThanZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, hadmin find all Dashboard with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 8;
        int page = -1;

        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        int numbEntities = 14;
        for (int i = 0; i < numbEntities; i++) {
            Dashboard dashboard = createDashboard(hproject, "OFFLINE");
            Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(numbEntities, dashboards.size());
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboard.getResults().size());
        Assert.assertEquals(delta, listDashboard.getDelta());
        Assert.assertEquals(defaultPage, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboard.getNextPage());
        // delta is 8, default page is 1: 14 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponsePage2 = dashboardRestApi.findAllDashboardPaginated(delta, 2);
        HyperIoTPaginableResult<Dashboard> listDashboardPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listDashboardPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardPage2.getNextPage());
        // delta is 8, page is 2: 14 entities stored in database
        Assert.assertEquals(2, listDashboardPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test28_findAllDashboardPaginatedShouldWorkIfPageIsZero() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, hadmin find all Dashboard with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 6;
        int page = 0;

        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Dashboard dashboard = createDashboard(hproject, "REALTIME");
            Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        Assert.assertEquals(defaultDelta, dashboards.size());
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboard.getResults().size());
        Assert.assertEquals(delta, listDashboard.getDelta());
        Assert.assertEquals(defaultPage, listDashboard.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboard.getNextPage());
        // delta is 6, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listDashboard.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponsePage2 = dashboardRestApi.findAllDashboardPaginated(delta, 2);
        HyperIoTPaginableResult<Dashboard> listDashboardPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertFalse(listDashboardPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listDashboardPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardPage2.getNextPage());
        // delta is 6, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listDashboardPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test29_findAllDashboardPaginatedShouldFailIfNotLogged() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // the following call tries to find all Dashboard with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(dashboardRestApi, null);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test30_findHProjectOfflineDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin finds all offline Dashboard associated to the project
        // with the following call findHProjectOfflineDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hProjectOfflineDashboard = createHProject(false);
        Assert.assertNotEquals(0, hProjectOfflineDashboard.getId());
        Assert.assertEquals(adminUser.getId(), hProjectOfflineDashboard.getUser().getId());

        this.impersonateUser(dashboardRestApi, adminUser);
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
        Assert.assertEquals(adminUser.getId(), dashboardsOffline.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test31_findHProjectRealtimeDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin finds all realtime Dashboard associated to the project
        // with the following call findHProjectRealtimeDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hProjectRealtimeDashboard = createHProject(false);
        Assert.assertNotEquals(0, hProjectRealtimeDashboard.getId());
        Assert.assertEquals(adminUser.getId(), hProjectRealtimeDashboard.getUser().getId());

        this.impersonateUser(dashboardRestApi, adminUser);
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
        Assert.assertEquals(adminUser.getId(), dashboardsOnline.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test32_findAreaRealtimeDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin finds all realtime Dashboard associated to the area
        // with the following call findAreaRealtimeDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(false);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponseAreaRealtime = dashboardRestApi.findAreaRealtimeDashboard(area.getId());
        Assert.assertEquals(200, restResponseAreaRealtime.getStatus());
        List<Dashboard> areaRealtime = restResponseAreaRealtime.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "REALTIME" has been setting in findAreaRealtimeDashboard()
        Assert.assertEquals(1, areaRealtime.size());
        Assert.assertNotEquals(0, areaRealtime.get(0).getId());
        Assert.assertEquals(area.getId(), areaRealtime.get(0).getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaRealtime.get(0).getArea().getProject().getUser().getId());
        Assert.assertEquals("REALTIME", areaRealtime.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), areaRealtime.get(0).getHProject().getId());
        Assert.assertEquals(adminUser.getId(), areaRealtime.get(0).getHProject().getUser().getId());
    }


    @Test
    public void test33_findAreaRealtimeDashboardShouldFailIfAreaIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin finds all realtime Dashboard associated to the area with the
        // following call findAreaRealtimeDashboard. This call returns an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponseAreaRealtime = dashboardRestApi.findAreaRealtimeDashboard((long) 0);
        Assert.assertEquals(200, restResponseAreaRealtime.getStatus());
        List<Dashboard> areaRealtime = restResponseAreaRealtime.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertTrue(areaRealtime.isEmpty());
        Assert.assertEquals(0, areaRealtime.size());
    }


    @Test
    public void test34_findAreaOfflineDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin finds all offline Dashboard associated to the area
        // with the following call findAreaOfflineDashboard
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(false);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Area area = createArea(hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(adminUser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponseAreaOffline = dashboardRestApi.findAreaOfflineDashboard(area.getId());
        Assert.assertEquals(200, restResponseAreaOffline.getStatus());
        List<Dashboard> areaOffline = restResponseAreaOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "OFFLINE" has been setting in findAreaOfflineDashboard()
        Assert.assertEquals(1, areaOffline.size());
        Assert.assertNotEquals(0, areaOffline.get(0).getId());
        Assert.assertEquals(area.getId(), areaOffline.get(0).getArea().getId());
        Assert.assertEquals(adminUser.getId(), areaOffline.get(0).getArea().getProject().getUser().getId());
        Assert.assertEquals("OFFLINE", areaOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), areaOffline.get(0).getHProject().getId());
        Assert.assertEquals(adminUser.getId(), areaOffline.get(0).getHProject().getUser().getId());
    }

    @Test
    public void test35_findAreaOfflineDashboardShouldFailIfAreaIsNull() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin finds all offline Dashboard associated to the area with the
        // following call findAreaOfflineDashboard. This call returns an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponseAreaOffline = dashboardRestApi.findAreaOfflineDashboard((long) 0);
        Assert.assertEquals(200, restResponseAreaOffline.getStatus());
        List<Dashboard> areaOffline = restResponseAreaOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertTrue(areaOffline.isEmpty());
        Assert.assertEquals(0, areaOffline.size());
    }


    @Test
    public void test36_findAllDashboardShouldWorkIfListIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to find all Dashboard with the following call findAllDashboard
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        this.impersonateUser(dashboardRestApi, adminUser);
        Response restResponse = dashboardRestApi.findAllDashboard();
        List<Dashboard> listDashboards = restResponse.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertTrue(listDashboards.isEmpty());
        Assert.assertEquals(0, listDashboards.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test37_findAllDashboardPaginatedShouldWorkIfListIsEmpty() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, hadmin find all Dashboard with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test38_saveDashboardShouldFailIfNameGreaterThan255Chars() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to save Dashboard with the following call saveDashboard,
        // but name is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Dashboard dashboard = new Dashboard();
        dashboard.setName(createStringFieldWithSpecifiedLenght(256));
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test39_updateDashboardShouldFailIfNameIsGreaterThan255Chars() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // hadmin tries to update Dashboard with the following call updateDashboard,
        // but name is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject(true);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(adminUser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject,"REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(adminUser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setName(createStringFieldWithSpecifiedLenght(256));
        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test49_saveHProjectAndCreateTwoDashboardShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // hadmin save HProject with the following call saveHProject
        // and dashboards (Offline, Realtime) will be created automatically
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        HProject hprojectDashboard = new HProject();
        hprojectDashboard.setName("Project " + java.util.UUID.randomUUID());
        hprojectDashboard.setDescription("Description");
        hprojectDashboard.setUser((HUser) adminUser);
        Response restResponse = hprojectRestService.saveHProject(hprojectDashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hprojectDashboard.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals("Description", ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(adminUser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());

        //checks if dashboards has been created
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, adminUser);

        //checks if dashboard offline has been created
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

        //checks if dashboard online has been created
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
    public void test50_deleteHProjectAndRemoveInCascadeAllDashboards(){
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // hadmin deletes hproject with call deleteHProject
        // and remove in cascade mode all dashboards
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin","admin");
        HProject hprojectDashboard = createHProject(false);
        Assert.assertNotEquals(0, hprojectDashboard.getId());
        Assert.assertEquals(adminUser.getId(), hprojectDashboard.getUser().getId());

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, adminUser);
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

        // hadmin deletes hproject with call deleteHProject
        // and remove in cascade mode all dashboards
        this.impersonateUser(hProjectRestApi, adminUser);
        Response restResponseRemoveHProject = hProjectRestApi.deleteHProject(hprojectDashboard.getId());
        Assert.assertEquals(200, restResponseRemoveHProject.getStatus());
        Assert.assertNull(restResponseRemoveHProject.getEntity());

        this.impersonateUser(dashboardRestApi, adminUser);
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
    public void test51_deleteDashboardNotRemovesInCascadeHProject(){
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // hadmin deletes dashboards (Offline, Realtime) with call deleteDashboard
        // this call not deletes in cascade hproject
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin","admin");
        HProject hprojectDashboard = createHProject(false);
        Assert.assertNotEquals(0, hprojectDashboard.getId());
        Assert.assertEquals(adminUser.getId(), hprojectDashboard.getUser().getId());

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, adminUser);
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

        // hadmin delete dashboard Offline with call deleteDashboard
        // this call not deletes hproject
        this.impersonateUser(dashboardRestApi, adminUser);
        Response deleteDashboardOffline = dashboardRestApi.deleteDashboard(dashboardsOffline.get(0).getId());
        Assert.assertEquals(200, deleteDashboardOffline.getStatus());
        Assert.assertNull(deleteDashboardOffline.getEntity());

        //checks if dashboard Offline exists
        Response findDashboardOffline = dashboardRestApi.findDashboard(dashboardsOffline.get(0).getId());
        Assert.assertEquals(404, findDashboardOffline.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) findDashboardOffline.getEntity()).getType());

        // dashboard Realtime is still stored in database
        restResponseDashboardOnline = dashboardRestApi.findDashboard(dashboardsOnline.get(0).getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());

        // hadmin deletes dashboard Realtime with call deleteDashboard
        // this call not deletes hproject
        this.impersonateUser(dashboardRestApi, adminUser);
        Response deleteDashboardOnline = dashboardRestApi.deleteDashboard(dashboardsOnline.get(0).getId());
        Assert.assertEquals(200, deleteDashboardOnline.getStatus());
        Assert.assertNull(deleteDashboardOnline.getEntity());

        //checks if dashboard Realtime exists
        Response findDashboardOnline = dashboardRestApi.findDashboard(dashboardsOnline.get(0).getId());
        Assert.assertEquals(404, findDashboardOnline.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) findDashboardOnline.getEntity()).getType());

        // checks if hproject is already stored in database
        this.impersonateUser(hProjectRestApi, adminUser);
        Response responseFindHProject = hProjectRestApi.findHProject(hprojectDashboard.getId());
        Assert.assertEquals(200, responseFindHProject.getStatus());
        Assert.assertEquals(hprojectDashboard.getId(), ((HProject) responseFindHProject.getEntity()).getId());
        Assert.assertEquals(adminUser.getId(), ((HProject) responseFindHProject.getEntity()).getUser().getId());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    private String createStringFieldWithSpecifiedLenght(int length) {
        String symbol = "a";
        String field = String.format("%" + length + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(length, field.length());
        return field;
    }

    private HProject createHProject(boolean deleteDashboard) {
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
        if (deleteDashboard){
            deleteDefaultDashboard(hproject);
        }
        return hproject;
    }


    private Area createArea(HProject hproject) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertEquals("Project of user: " + adminUser.getUsername(), hproject.getDescription());
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals("Description", ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        return area;
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


    private void deleteDefaultDashboard(HProject hproject) {
        // utility method: deletes dashboard Offline and Realtime created in hproject with method createHProject()
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authenticationApi.login("hadmin","admin");

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, adminUser);
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

            this.impersonateUser(dashboardRestApi, adminUser);
            Response deleteDashboardOffline = dashboardRestApi.deleteDashboard(dashboardsOffline.get(0).getId());
            Assert.assertEquals(200, deleteDashboardOffline.getStatus());
            Assert.assertNull(deleteDashboardOffline.getEntity());
        }

        this.impersonateUser(dashboardRestApi, adminUser);
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

            this.impersonateUser(dashboardRestApi, adminUser);
            Response deleteDashboardOnline = dashboardRestApi.deleteDashboard(dashboardsOnline.get(0).getId());
            Assert.assertEquals(200, deleteDashboardOnline.getStatus());
            Assert.assertNull(deleteDashboardOnline.getEntity());
        }
    }


    @After
    public void afterTest() {
        // Remove all projects and delete in cascade all associated entities (Dashboard) in every tests
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
    }

}

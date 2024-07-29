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
import it.acsoftware.hyperiot.dashboard.actions.HyperIoTDashboardAction;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import it.acsoftware.hyperiot.dashboard.service.rest.DashboardRestApi;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import it.acsoftware.hyperiot.dashboard.widget.service.rest.DashboardWidgetRestApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.dashboard.widget.test.HyperIoTDashboardWidgetConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for DashboardWidget System
 * Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDashboardWidgetWithPermissionRestTest extends KarafTestSupport {

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
        huser = createHUser(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("DashboardWidget Module works!", restResponse.getEntity());
    }

    @Test
    public void test02_saveDashboardWidgetWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, save DashboardWidget with the following call saveDashboardWidget
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
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

    @Test
    public void test03_saveDashboardWidgetWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to save DashboardWidget with the following call saveDashboardWidget
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + huser.getUsername() + "\"}");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test04_updateDashboardWidgetWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, update DashboardWidget with the
        // following call updateDashboardWidget
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        dashboardWidget.setWidgetConf("{\"description\":\"Updated dashboard widget description\"}");
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getEntityVersion() + 1,
                ((DashboardWidget) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("{\"description\":\"Updated dashboard widget description\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
    }

    @Test
    public void test05_updateDashboardWidgetWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to update DashboardWidget with
        // the following call updateDashboardWidget
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        dashboardWidget.setWidgetConf("{\"description\":\"Updated dashboard widget description\"}");
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findDashboardWidgetWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, find DashboardWidget with
        // the following call findDashboardWidget
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getId(), ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals(dashboard.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(hproject.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
    }

    @Test
    public void test07_findDashboardWidgetWithPermissionShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to find DashboardWidget with
        // the following call findDashboardWidget, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findDashboardWidgetWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to find DashboardWidget with
        // the following call findDashboardWidget
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findDashboardWidgetNotFoundWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to find DashboardWidget not found with the following
        // call findDashboardWidget
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test10_deleteDashboardWidgetWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, delete DashboardWidget with
        // the following call deleteDashboardWidget
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

    @Test
    public void test11_deleteDashboardWidgetWithPermissionShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to delete DashboardWidget with the following
        // call deleteDashboardWidget, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteDashboardWidgetWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to delete DashboardWidget with
        // the following call deleteDashboardWidget
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_deleteDashboardWidgetNotFoundWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to delete DashboardWidget not found with the following
        // call deleteDashboardWidget
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test14_findAllDashboardWidgetWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, find all DashboardWidget with
        // the following call findAllDashboardWidget
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

    @Test
    public void test15_findAllDashboardWidgetWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to find all DashboardWidget with
        // the following call findAllDashboardWidget
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidget();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_findDashboardWidgetConfWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, find dashboard widget configuration with the following
        // call findDashboardWidgetConf
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

    @Test
    public void test17_findDashboardWidgetConfWithPermissionShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to find dashboard widget configuration with the following
        // call findDashboardWidgetConf, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FIND);
        huser = createHUser(action);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test18_findDashboardWidgetConfWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to find dashboard widget configuration with the following
        // call findDashboardWidgetConf
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(dashboardWidget.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test19_findDashboardWidgetConfNotFoundWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to find dashboard widget configuration not found
        // with the following call findDashboardWidgetConf
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test20_setDashboardWidgetConfWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, update dashboard widget configuration with
        // the following call setDashboardWidgetConf
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

        String widgetConf = "{\"description\":\"test setDashboardWidgetConf\"}";
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(widgetConf, ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
    }

    @Test
    public void test21_setDashboardWidgetConfWithPermissionShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update dashboard widget configuration with the following
        // call setDashboardWidgetConf, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        String widgetConf = "{\"description\":\"Just a simple description\"}";
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(0, widgetConf);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test22_setDashboardWidgetConfWithPermissionShouldFailIfWidgetConfIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update dashboard widget configuration with the following
        // call setDashboardWidgetConf, but widgetConf is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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
    public void test23_setDashboardWidgetConfWithPermissionShouldFailIfWidgetConfIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update dashboard widget configuration with the following
        // call setDashboardWidgetConf, but widgetConf is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

        String widgetConf = "";
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test24_setDashboardWidgetConfWithPermissionShouldFailIfWidgetConfIsMaliciousCode() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update dashboard widget configuration with the following
        // call setDashboardWidgetConf, but widgetConf is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

        String widgetConf = "onload(malicious code)=";
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test25_setDashboardWidgetConfWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to update dashboard widget configuration
        // with the following call setDashboardWidgetConf
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        String widgetConf = "{\"description\":\"Just a simple description\"}";
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test26_setDashboardWidgetConfWithoutPermissionShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to update dashboard widget configuration
        // not found with the following call setDashboardWidgetConf
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        String widgetConf = "{\"description\":\"Just a simple description\"}";
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(0, widgetConf);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test27_findAllDashboardWidgetInDashboardWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, find all available dashboard widgets inside a particular
        // dashboard with the following call findAllDashboardWidgetInDashboard
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTDashboardAction.FIND_WIDGETS);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

    @Test
    public void test28_findAllDashboardWidgetInDashboardWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to find all available dashboard widgets inside a particular
        // dashboard with the following call findAllDashboardWidgetInDashboard
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboardWidget.getDashboard().getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test29_findAllDashboardWidgetInDashboardNotFoundWithPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to find all available dashboard widgets inside a particular
        // dashboard with the following call findAllDashboardWidgetInDashboard, Dashboard not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTDashboardAction.FIND_WIDGETS);
        huser = createHUser(action);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test30_findAllDashboardWidgetInDashboardWithPermissionShouldWorkIfListWidgetsIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser tries to finds all available dashboard widgets inside a particular dashboard
        // with the following call findAllDashboardWidgetInDashboard, but list widget is empty
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTDashboardAction.FIND_WIDGETS);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboard.getId());
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertEquals(0, listDashboardWidgets.size());
        Assert.assertTrue(listDashboardWidgets.isEmpty());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test31_saveDashboardWidgetWithPermissionShouldFailIfWidgetConfIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but widgetConf (String in JSON format) is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf(null);
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test32_saveDashboardWidgetWithPermissionShouldFailIfWidgetConfIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but widgetConf (String in JSON format) is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test33_saveDashboardWidgetWithPermissionShouldFailIfWidgetConfIsMaliciousCode() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but widgetConf (String in JSON format) is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("onload(malicious code)=");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test34_saveDashboardWidgetWithPermissionShouldFailIfDashboardIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but Dashboard is null
        // response status code '404' HyperIoTEntityNotFound. (Cannot save a dashboard widget without specicify the related dashboard).
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"it's a simple test description\"}");
        dashboardWidget.setDashboard(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test35_saveDashboardWidgetWithPermissionShouldFailIfDashboardBelongsToAnotherUser() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but Dashboard belongs to another user
        // response status code '404' HyperIoTEntityNotFound (Because filter avoid that the user can find the dashboard).
        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        Dashboard anotherDashboard = createDashboard(anotherHProject, "OFFLINE");
        Assert.assertEquals(anotherHProject.getName() + " Offline Dashboard", anotherDashboard.getName());
        Assert.assertEquals(anotherHProject.getId(), anotherDashboard.getHProject().getId());
        Assert.assertEquals(huser2.getId(), anotherDashboard.getHProject().getUser().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.SAVE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"it's a simple test description\"}");
        dashboardWidget.setDashboard(anotherDashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test36_updateDashboardWidgetWithPermissionShouldFailIfWidgetConfIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update DashboardWidget with the following call
        // updateDashboardWidget, but widgetConf (String in JSON format) is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        dashboardWidget.setWidgetConf(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test37_updateDashboardWidgetWithPermissionShouldFailIfWidgetConfIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but widgetConf (String in JSON format) is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

        dashboardWidget.setWidgetConf("");
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test38_updateDashboardWidgetWithPermissionShouldFailIfWidgetConfIsMaliciousCode() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but widgetConf (String in JSON format) is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        dashboardWidget.setWidgetConf("</script>");
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
    public void test39_updateDashboardWidgetWithPermissionShouldFailIfDashboardIsNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to save DashboardWidget with the following call
        // saveDashboardWidget, but Dashboard is null
        // response status code '404' HyperIoTEntityNotFound. (Dashboard related to dashboard widget cannot be null).
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        dashboardWidget.setDashboard(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test40_updateDashboardWidgetWithPermissionShouldFailIfDashboardBelongsToAnotherUser() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update DashboardWidget with the following call
        // updateDashboardWidget, but Dashboard belongs to another user
        // response status code '403' HyperIoTUnauthorizedException
        huser2 = createHUser(null);
        HProject anotherHProject = createHProject(huser2);
        Assert.assertNotEquals(0, anotherHProject.getId());
        Assert.assertEquals(huser2.getId(), anotherHProject.getUser().getId());

        Dashboard anotherDashboard = createDashboard(anotherHProject, "OFFLINE");
        Assert.assertEquals(anotherHProject.getName() + " Offline Dashboard", anotherDashboard.getName());
        Assert.assertEquals(anotherHProject.getId(), anotherDashboard.getHProject().getId());
        Assert.assertEquals(huser2.getId(), anotherDashboard.getHProject().getUser().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        dashboardWidget.setDashboard(anotherDashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException+"HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test41_findAllDashboardWidgetPaginatedWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with permission,
        // find all DashboardWidget with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 4;
        int page = 1;

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        int numbEntities = 6;
        for (int i = 0; i < numbEntities; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(numbEntities, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidget.getResults().size());
        Assert.assertEquals(delta, listDashboardWidget.getDelta());
        Assert.assertEquals(page, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(page + 1, listDashboardWidget.getNextPage());
        // delta is 4, page is 1: 6 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponsePage2 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 2);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listDashboardWidgetPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage2.getDelta());
        Assert.assertEquals(page + 1, listDashboardWidgetPage2.getCurrentPage());
        Assert.assertEquals(page, listDashboardWidgetPage2.getNextPage());
        // delta is 4, page is 2: 6 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test42_findAllDashboardWidgetPaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with permission,
        // find all DashboardWidget with pagination if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        Integer delta = null;
        Integer page = null;

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
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
        Assert.assertEquals(defaultDelta, listDashboardWidget.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidget.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test43_findAllDashboardWidgetPaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with permission,
        // find all DashboardWidget with pagination if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = -1;
        int page = 2;

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        int numbEntities = 14;
        for (int i = 0; i < numbEntities; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(numbEntities, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listDashboardWidget.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getDelta());
        Assert.assertEquals(page, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidget.getNextPage());
        // default delta is 10, page is 2: 14 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 1
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponsePage1 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 1);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardWidgetPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidgetPage1.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage1.getCurrentPage());
        Assert.assertEquals(page, listDashboardWidgetPage1.getNextPage());
        // default delta is 10, page is 1: 14 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test44_findAllDashboardWidgetPaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with permission,
        // find all DashboardWidget with pagination if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 0;
        int page = 1;

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        int numbEntities = 11;
        for (int i = 0; i < numbEntities; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(numbEntities, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidget.getDelta());
        Assert.assertEquals(page, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(page + 1, listDashboardWidget.getNextPage());
        // default delta is 10, page is 1: 11 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponsePage2 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 2);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listDashboardWidgetPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listDashboardWidgetPage2.getDelta());
        Assert.assertEquals(page + 1, listDashboardWidgetPage2.getCurrentPage());
        Assert.assertEquals(page, listDashboardWidgetPage2.getNextPage());
        // default delta is 10, page is 2: 11 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test45_findAllDashboardWidgetPaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with permission,
        // find all DashboardWidget with pagination if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 3;
        int page = -1;

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        int numbEntities = 5;
        for (int i = 0; i < numbEntities; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(numbEntities, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidget.getResults().size());
        Assert.assertEquals(delta, listDashboardWidget.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboardWidget.getNextPage());
        // delta is 3, default page is 1: 5 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponsePage2 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 2);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listDashboardWidgetPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardWidgetPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage2.getNextPage());
        // delta is 3, page is 2: 5 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test46_findAllDashboardWidgetPaginationWithPermissionShouldWorkIfPageIsZero() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with permission,
        // find all DashboardWidget with pagination if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        int delta = 4;
        int page = 0;

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Offline Dashboard", dashboard.getName());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        int numbEntities = 5;
        for (int i = 0; i < numbEntities; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        Assert.assertEquals(numbEntities, dashboardWidgets.size());
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(delta, listDashboardWidget.getResults().size());
        Assert.assertEquals(delta, listDashboardWidget.getDelta());
        Assert.assertEquals(defaultPage, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listDashboardWidget.getNextPage());
        // delta is 4, default page is 1: 5 entities stored in database
        Assert.assertEquals(2, listDashboardWidget.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        // checks with page = 2
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponsePage2 = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, 2);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidgetPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertFalse(listDashboardWidgetPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listDashboardWidgetPage2.getResults().size());
        Assert.assertEquals(delta, listDashboardWidgetPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listDashboardWidgetPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listDashboardWidgetPage2.getNextPage());
        // delta is 4, page is 2: 5 entities stored in database
        Assert.assertEquals(2, listDashboardWidgetPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test47_findAllDashboardWidgetPaginatedWithoutPermissionShouldFail() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, without permission, tries to find
        // all DashboardWidget with pagination
        // response status code '403' HyperIoTUnauthorizedException
        huser = createHUser(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test48_updateDashboardWidgetWithPermissionShouldFailIfEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update DashboardWidget with the following call
        // updateDashboardWidget, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        addPermissionToFindDashboard(huser);
        HProject project = createHProject(huser);
        Dashboard dashboard = createDashboard(project,"OFFLINE");
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setDashboard(dashboard);
        dashboardWidget.setWidgetConf("{\"description\":\"entity not found\"}");
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test49_updateDashboardWidgetWithoutPermissionShouldFailAndEntityNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, without permission, tries to update DashboardWidget not found with the following
        // call updateDashboardWidget
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        HProject project = createHProject(huser);
        Dashboard dashboard = createDashboard(project,"OFFLINE");
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setDashboard(dashboard);
        dashboardWidget.setWidgetConf("{\"description\":\"entity not found\"}");
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(403, restResponse.getStatus());
    }


    @Test
    public void test50_findAllDashboardWidgetInDashboardWithoutPermissionShouldFailAndDashboardNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to find all available dashboard widgets (not found)
        // inside a particular dashboard with the following call findAllDashboardWidgetInDashboard
        // response status code '404' HyperIoTEntityNotFound
        huser = createHUser(null);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(0);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test51_saveAllDashboardWidgetWithPermissionShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser save all dashboard widget with the following call saveAllDashboardWidget
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget1 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget1.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget1.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget1.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget1.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget3 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget3.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget3.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget3.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget3.getDashboard().getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration = {dashboardWidget1, dashboardWidget2, dashboardWidget3};

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(dashboard.getId(), widgetConfiguration);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test52_saveAllDashboardWidgetWithPermissionShouldFailIfDashboardNotFound() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser tries to save all dashboard widget with the following call saveAllDashboardWidget,
        // but Dashboard not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget1 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget1.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget1.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget1.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget1.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        DashboardWidget dashboardWidget3 = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget3.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget3.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget3.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget3.getDashboard().getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration = {dashboardWidget1, dashboardWidget2, dashboardWidget3};

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(0, widgetConfiguration);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test53_saveAllDashboardWidgetWithPermissionShouldWorkIfDashboardWidgetIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser save all dashboard widget with the following call saveAllDashboardWidget,
        // whit this call hadmin remove all DashboardWidget into Dashboard
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration = {};

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(dashboard.getId(), widgetConfiguration);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test54_saveAllDashboardWidgetWithPermissionShouldFailIfDashboardAndWidgetConfigurationBelongsToAnotherHUser() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser tries to save all dashboard widget with the following call saveAllDashboardWidget,
        // but Dashboard and widgetConfiguration is associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Dashboard dashboard2 = createDashboard(hproject2, "REALTIME");
        Assert.assertNotEquals(0, dashboard2.getId());
        Assert.assertEquals(hproject2.getName() + " Online Dashboard", dashboard2.getName());
        Assert.assertEquals("REALTIME", dashboard2.getDashboardType().getType());
        Assert.assertEquals(hproject2.getId(), dashboard2.getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard2);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard2.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject2.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration2 = {dashboardWidget2};

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(dashboard2.getId(), widgetConfiguration2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test55_saveAllDashboardWidgetWithPermissionShouldFailIfDashboardBelongsToAnotherHUser() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser tries to save all dashboard widget with the following call saveAllDashboardWidget,
        // but Dashboard not found
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

        DashboardWidget[] widgetConfiguration = {dashboardWidget};

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Dashboard dashboard2 = createDashboard(hproject2, "REALTIME");
        Assert.assertNotEquals(0, dashboard2.getId());
        Assert.assertEquals(hproject2.getName() + " Online Dashboard", dashboard2.getName());
        Assert.assertEquals("REALTIME", dashboard2.getDashboardType().getType());
        Assert.assertEquals(hproject2.getId(), dashboard2.getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());

        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(dashboard2.getId(), widgetConfiguration);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test56_saveAllDashboardWidgetWithPermissionShouldFailIfWidgetConfigurationBelongsToAnotherHUser() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser tries to save all dashboard widget with the following call saveAllDashboardWidget,
        // but WidgetConfiguration is associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard", dashboard.getName());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Dashboard dashboard2 = createDashboard(hproject2, "REALTIME");
        Assert.assertNotEquals(0, dashboard2.getId());
        Assert.assertEquals(hproject2.getName() + " Online Dashboard", dashboard2.getName());
        Assert.assertEquals("REALTIME", dashboard2.getDashboardType().getType());
        Assert.assertEquals(hproject2.getId(), dashboard2.getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard2);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard2.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject2.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        DashboardWidget[] widgetConfiguration2 = {dashboardWidget2};

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveAllDashboardWidget(dashboard.getId(), widgetConfiguration2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test57_findAllDashboardWidgetInDashboardWithPermissionShouldFailDashboardWidgetBelongsToAnotherHuser() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to find all available dashboard widgets inside a particular
        // dashboard associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTDashboardAction.FIND_WIDGETS);
        huser = createHUser(action);

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Dashboard dashboard2 = createDashboard(hproject2, "REALTIME");
        Assert.assertNotEquals(0, dashboard2.getId());
        Assert.assertEquals(hproject2.getName() + " Online Dashboard", dashboard2.getName());
        Assert.assertEquals("REALTIME", dashboard2.getDashboardType().getType());
        Assert.assertEquals(hproject2.getId(), dashboard2.getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard2);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard2.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject2.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        Assert.assertNotEquals(huser.getId(), huser2.getId());

        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test58_setDashboardWidgetConfWithPermissionShouldFailIfDashboardWidgetBelongsToAnotherHUser() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, tries to update dashboard widget configuration with
        // the following call setDashboardWidgetConf, but DashboardWidget is associated to another huser
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.UPDATE);
        huser = createHUser(action);

        huser2 = createHUser(null);
        HProject hproject2 = createHProject(huser2);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());

        Dashboard dashboard2 = createDashboard(hproject2, "REALTIME");
        Assert.assertNotEquals(0, dashboard2.getId());
        Assert.assertEquals(hproject2.getName() + " Online Dashboard", dashboard2.getName());
        Assert.assertEquals("REALTIME", dashboard2.getDashboardType().getType());
        Assert.assertEquals(hproject2.getId(), dashboard2.getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());

        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard2);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        Assert.assertEquals(dashboard2.getId(), dashboardWidget2.getDashboard().getId());
        Assert.assertEquals(hproject2.getId(), dashboardWidget2.getDashboard().getHProject().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        String widgetConf2 = "{\"description\":\"test setDashboardWidgetConf\"}";

        Assert.assertNotEquals(huser.getId(), huser2.getId());

        Assert.assertNotEquals(huser.getId(), hproject2.getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertNotEquals(huser.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        Assert.assertEquals(huser2.getId(), hproject2.getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboard2.getHProject().getUser().getId());
        Assert.assertEquals(huser2.getId(), dashboardWidget2.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget2.getId(), widgetConf2);
        Assert.assertEquals(404, restResponse.getStatus());
    }


    @Test
    public void test59_removeHProjectWithPermissionDeleteInCascadeAllDashboardAndDashboardWidget() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser, with permission, remove HProject with the following call deleteHProject and delete
        // in cascade mode all Dashboard and all DashboardWidget
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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

        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        this.impersonateUser(hprojectRestService, huser);
        Response responseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());

        // checks: dashboard has been deleted with deleteHProject call
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action1);
        this.impersonateUser(dashboardRestApi, huser);
        Response responseFindDashboard = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(404, responseFindDashboard.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDashboard.getEntity()).getType());

        // checks: dashboardWidget has been deleted with deleteHProject call
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response responseFindDashboardWidget = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(404, responseFindDashboardWidget.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDashboardWidget.getEntity()).getType());
    }


    @Test
    public void test60_removeDashboardWithPermissionNotDeleteHProjectButDeleteInCascadeAllDashboardWidget() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser, with permission, remove Dashboard with the following call deleteDashboard and delete
        // in cascade mode all DashboardWidget but not deletes HProject
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "OFFLINE");
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

        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        this.impersonateUser(dashboardRestApi, huser);
        Response responseDeleteDashboard = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(200, responseDeleteDashboard.getStatus());

        // checks: hproject is already stored in database
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action1);
        this.impersonateUser(hprojectRestService, huser);
        Response responseDeleteHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, responseDeleteHProject.getStatus());

        // checks: dashboardWidget has been deleted with deleteDashboard call
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response responseFindDashboardWidget = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(404, responseFindDashboardWidget.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) responseFindDashboardWidget.getEntity()).getType());
    }


    @Test
    public void test61_removeDashboardWidgetWithPermissionNotDeleteInCascadeAllHProjectAndAllDashboard() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // huser, with permission, remove DashboardWidget with the following call deleteDashboardWidget;
        // this call not delete in cascade mode all HProject and all Dashboard
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.REMOVE);
        huser = createHUser(action);
        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(hproject, "REALTIME");
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
        Response responseDeleteDashboardWidget = dashboardWidgetRestApi.deleteDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, responseDeleteDashboardWidget.getStatus());

        // checks: hproject is already stored in database
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action1);
        this.impersonateUser(hprojectRestService, huser);
        Response responseFindHProject = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, responseFindHProject.getStatus());

        // checks: dashboard is already stored in database
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(dashboardResourceName,
                HyperIoTCrudAction.FIND);
        addPermission(huser, action2);
        this.impersonateUser(dashboardRestApi, huser);
        Response responseFindDashboard = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(200, responseFindDashboard.getStatus());
    }

    @Test
    public void test62_findAllDashboardWidgetWithPermissionShouldWorkIfListIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with permission, find all DashboardWidget with the following call findAllDashboardWidget
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidget();
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertTrue(listDashboardWidgets.isEmpty());
        Assert.assertEquals(0, listDashboardWidgets.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test63_findAllDashboardWidgetPaginatedWithPermissionShouldWorkIfListIsEmpty() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with permission,
        // find all DashboardWidget with pagination
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(dashboardWidgetResourceName,
                HyperIoTCrudAction.FINDALL);
        huser = createHUser(action);
        this.impersonateUser(dashboardWidgetRestApi, huser);
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
            Assert.assertEquals(dashboardWidgetResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
            Assert.assertEquals(action.getActionId(), permission.getActionIds());
            Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
            Assert.assertEquals(role.getId(), permission.getRole().getId());
        }
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
                permission.setName(dashboardWidgetResourceName + " assigned to huser_id " + huser.getId());
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
        Assert.assertEquals(dashboardWidgetResourceName + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }

    private HProject createHProject(HUser huser) {
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
        removeDefaultPermission(huser);
        return hproject;
    }

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

    private DashboardWidget createDashboardWidget(Dashboard dashboard) {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        HUser ownerHUser = dashboard.getHProject().getUser();
        Assert.assertEquals(ownerHUser.getId(), dashboard.getHProject().getUser().getId());
        Assert.assertEquals("Project of user: " + ownerHUser.getUsername(), dashboard.getHProject().getDescription());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}");
        dashboardWidget.setDashboard(dashboard);

        addDefaultPermission(ownerHUser);
        this.impersonateUser(dashboardWidgetRestApi, ownerHUser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals("{\"description\":\"dashboard widget of user " + ownerHUser.getUsername() + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(dashboard.getHProject().getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
        removeDefaultPermission(ownerHUser);
        return dashboardWidget;
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

    //When User Save or Update a dashboardwidget he must have the permission to find the dashboard related to the dashboard widget.
    private Permission addPermissionToFindDashboard(HUser huser){
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, adminUser);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(Dashboard.class.getName(),HyperIoTCrudAction.FIND);
        Permission permission = new Permission();
        permission.setName(Dashboard.class.getName() + " assigned to huser_id " + huser.getId());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertEquals(Dashboard.class.getName() + " assigned to huser_id " + huser.getId(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }


    // DashboardWidget is Owned Resource: only huser or huser2 is able to find/findAll his entities
    private HUser huser;
    private HUser huser2;

    @After
    public void afterTest() {
        // Remove projects and delete in cascade all dashboardWidgets created in every test
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FINDALL);
        HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.REMOVE);
        if ((huser != null) && (huser.isActive())) {
            addPermission(huser, action);
            addPermission(huser, action1);
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
        if ((huser2 != null) && (huser2.isActive())) {
            addPermission(huser2, action);
            addPermission(huser2, action1);
            this.impersonateUser(hprojectRestService, huser2);
            Response restResponse = hprojectRestService.findAllHProject();
            List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
            });
            if (!listHProjects.isEmpty()) {
                Assert.assertFalse(listHProjects.isEmpty());
                for (HProject project : listHProjects) {
                    this.impersonateUser(hprojectRestService, huser2);
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
    }

}

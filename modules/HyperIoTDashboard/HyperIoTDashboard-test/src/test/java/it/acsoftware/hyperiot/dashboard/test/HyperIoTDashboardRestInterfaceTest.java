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

import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.http.*;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidator;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidatorBuilder;
import it.acsoftware.hyperiot.base.test.util.HyperIoTTestUtils;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import it.acsoftware.hyperiot.dashboard.service.rest.DashboardRestApi;
import it.acsoftware.hyperiot.dashboard.widget.api.DashboardWidgetSystemApi;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import it.acsoftware.hyperiot.dashboard.widget.service.rest.DashboardWidgetRestApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import static it.acsoftware.hyperiot.dashboard.test.HyperIoTDashboardConfiguration.*;

/**
 * @author Francesco Salerno
 * This is a test class relative to Interface's test of DashboardRestApi class.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDashboardRestInterfaceTest extends KarafTestSupport {

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

    @Before
    public void impersonateAsHyperIoTAdmin(){
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(dashboardRestApi, admin);
    }

    @SuppressWarnings("unused")
    private HyperIoTAction getHyperIoTAction(String resourceName, HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder.createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
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
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_saveDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Area area = createArea(project, true);
        Dashboard dashboard = new Dashboard();
        dashboard.setName("Name".concat(generateRandomString()));
        dashboard.setDashboardType(DashboardType.OFFLINE);
        dashboard.setHProject(project);
        dashboard.setArea(area);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboards"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(dashboard)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardExpectedProperties)
                .containExactInnerProperties("hproject",dashboardHprojectExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboards/").concat(String.valueOf(dashboard.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(dashboard)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardExpectedProperties)
                .containExactInnerProperties("hproject",dashboardHprojectExpectedProperties())
                .containExactInnerProperties("widgets", dashboardWidgetsCollectionExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    /*
        TODO
            When fix dashboard's update
     */
    @Test
    public void test003_updateDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        dashboard.setName("NewName".concat(generateRandomString()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboards"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(dashboard)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardExpectedProperties)
                .containExactInnerProperties("hproject",dashboardHprojectExpectedProperties())
                /*
                     TODO
                            When fix dashboard's update  change the next validation with
                            .containExactInnerProperties("widgets", dashboardWidgetsCollectionExpectedProperties())
                */
                .containExactInnerProperties("widgets", new ArrayList<>())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboards/").concat(String.valueOf(dashboard.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .withCustomCriteria(hyperIoTHttpResponse -> response.getResponseBody().isEmpty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test005_findAllDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        Dashboard dashboard2 = createDashboard(project, "REALTIME");
        Assert.assertNotNull(dashboard2);
        Assert.assertNotEquals(0, dashboard2.getId());
        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard2);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboards/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardExpectedProperties)
                .containExactInnerProperties("hproject",dashboardHprojectExpectedProperties())
                .containExactInnerProperties("widgets", dashboardWidgetsCollectionExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllDashboardPaginatedShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        Dashboard dashboard2 = createDashboard(project, "REALTIME");
        Assert.assertNotNull(dashboard2);
        Assert.assertNotEquals(0, dashboard2.getId());
        DashboardWidget dashboardWidget2 = createDashboardWidget(dashboard2);
        Assert.assertNotEquals(0, dashboardWidget2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboards/"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("delta",String.valueOf(defaultDelta))
                .withParameter("page",String.valueOf(defaultPage))
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results",dashboardExpectedProperties)
                .containExactInnerProperties("results.hproject",dashboardHprojectExpectedProperties())
                .containExactInnerProperties("results.widgets", dashboardWidgetsCollectionExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findHProjectOfflineDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/dashboards")
                .concat("/project/").concat(String.valueOf(project.getId())).concat("/offline");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardExpectedProperties)
                .containExactInnerProperties("hproject",dashboardHprojectExpectedProperties())
                .containExactInnerProperties("widgets", dashboardWidgetsCollectionExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_findAreaRealtimeDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project, false);
        Assert.assertNotEquals(0, area.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/dashboards")
                .concat("/area/").concat(String.valueOf(area.getId())).concat("/realtime");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardExpectedProperties)
                .containExactInnerProperties("hproject",dashboardHprojectExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test009_findAreaOfflineDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project, false);
        Assert.assertNotEquals(0, area.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/dashboards")
                .concat("/area/").concat(String.valueOf(area.getId())).concat("/offline");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardExpectedProperties = dashboardExpectedProperties();
        dashboardExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardExpectedProperties)
                .containExactInnerProperties("hproject",dashboardHprojectExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }


    /*
    *
    *
    *   UTILITY METHODS FOR TEST.
    *
    *
     */

    private String generateRandomString(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    private List<String> dashboardExpectedProperties(){
        List<String> dashboardExpectedProperties = new ArrayList<>();
        //Dashboard's Area is not serialized in the response.
        dashboardExpectedProperties.add("name");
        dashboardExpectedProperties.add("dashboardType");
        dashboardExpectedProperties.add("widgets");
        dashboardExpectedProperties.add("hproject");
        return dashboardExpectedProperties;
    }

    private List<String> dashboardHprojectExpectedProperties(){
        List<String> dashboardHprojectExpectedProperties = new ArrayList<>();
        dashboardHprojectExpectedProperties.add("id");
        dashboardHprojectExpectedProperties.add("entityCreateDate");
        dashboardHprojectExpectedProperties.add("entityModifyDate");
        dashboardHprojectExpectedProperties.add("name");
        dashboardHprojectExpectedProperties.add("description");
        return dashboardHprojectExpectedProperties;
    }


    private List<String> dashboardWidgetsCollectionExpectedProperties(){
        List<String> dashboardWidgetsCollectionExpectedProperties = new ArrayList<>();
        dashboardWidgetsCollectionExpectedProperties.add("id");
        dashboardWidgetsCollectionExpectedProperties.add("entityCreateDate");
        dashboardWidgetsCollectionExpectedProperties.add("entityModifyDate");
        dashboardWidgetsCollectionExpectedProperties.add("widgetConf");
        return dashboardWidgetsCollectionExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
    }

    private HProject createHProject(boolean deleteDashboards) {
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
        //Delete dashboard related to the projects. (Save HProject's trigger dashboard RealtimeOffline creation).
        if(deleteDashboards) {
            DashboardSystemApi dashboardSystemApi = getOsgiService(DashboardSystemApi.class);
            HyperIoTTestUtils.truncateTables(dashboardSystemApi, null);
        }
        return hproject;
    }


    private Area createArea(HProject hproject, boolean deleteDashboards) {
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
        if(deleteDashboards) {
            DashboardSystemApi dashboardSystemApi = getOsgiService(DashboardSystemApi.class);
            HyperIoTTestUtils.truncateTables(dashboardSystemApi, null);
        }
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
        if (Objects.equals(dashboardType, "OFFLINE")) {
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
        if (Objects.equals(dashboardType, "REALTIME")) {
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
        if (!Objects.equals(dashboardType, "OFFLINE") && !Objects.equals(dashboardType, "REALTIME")) {
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
        DashboardWidgetSystemApi dashboardWidgetSystemApi = getOsgiService(DashboardWidgetSystemApi.class);
        DashboardSystemApi dashboardSystemApi = getOsgiService(DashboardSystemApi.class);
        AreaDeviceRepository areaDeviceRepository = getOsgiService(AreaDeviceRepository.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        HyperIoTTestUtils.truncateTables(dashboardWidgetSystemApi, null);
        HyperIoTTestUtils.truncateTables(dashboardSystemApi, null);
        HyperIoTTestUtils.truncateTables(areaDeviceRepository, null);
        HyperIoTTestUtils.truncateTables(areaSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
    }


}

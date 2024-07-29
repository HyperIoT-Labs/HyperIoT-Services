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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
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
import java.util.*;

import static it.acsoftware.hyperiot.dashboard.widget.test.HyperIoTDashboardWidgetConfiguration.defaultDelta;
import static it.acsoftware.hyperiot.dashboard.widget.test.HyperIoTDashboardWidgetConfiguration.defaultPage;

/**
 * @author Francesco Salerno
 * This is a test class relative to Interface's test of DashboardWidgetRestApi class.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTDashboardWidgetRestInterfaceTest extends KarafTestSupport {

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
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(dashboardWidgetRestApi, admin);
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
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_saveDashboardWidgetShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + "\"}");
        dashboardWidget.setDashboard(dashboard);
        String serializedDashboardWidget = serializeDashboardWidgetForRequest(dashboardWidget);
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboardwidgets"))
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .withJsonBody(serializedDashboardWidget)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardWidgetsExpectedProperties = dashboardWidgetsExpectedProperties();
        dashboardWidgetsExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardWidgetsExpectedProperties)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("widgetConf"));
                        Assert.assertTrue(node.get("widgetConf").isTextual());
                        String baseConfigResponse = node.get("widgetConf").textValue();
                        return baseConfigResponse.equals(expectedWidgetConf);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findDashboardWidgetShouldSerializeResponseCorrectly() {
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboardwidgets/").concat(String.valueOf(dashboardWidget.getId())))
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardWidgetsExpectedProperties = dashboardWidgetsExpectedProperties();
        dashboardWidgetsExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardWidgetsExpectedProperties)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("widgetConf"));
                        Assert.assertTrue(node.get("widgetConf").isTextual());
                        String baseConfigResponse = node.get("widgetConf").textValue();
                        return baseConfigResponse.equals(expectedWidgetConf);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateDashboardWidgetShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        dashboardWidget.setWidgetConf("{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + new Date()+" \"}");
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        String serializedDashboardWidget = serializeDashboardWidgetForRequest(dashboardWidget);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboardwidgets"))
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .withJsonBody(serializedDashboardWidget)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardWidgetsExpectedProperties = dashboardWidgetsExpectedProperties();
        dashboardWidgetsExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardWidgetsExpectedProperties)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("widgetConf"));
                        Assert.assertTrue(node.get("widgetConf").isTextual());
                        String baseConfigResponse = node.get("widgetConf").textValue();
                        return baseConfigResponse.equals(expectedWidgetConf);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteDashboardWidgetsShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboardwidgets/").concat(String.valueOf(dashboardWidget.getId())))
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .withCustomCriteria(hyperIoTHttpResponse -> hyperIoTHttpResponse.getResponseBody().isEmpty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test005_findAllDashboardWidgetShouldSerializeResponseCorrectly() {
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboardwidgets/all"))
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardWidgetsExpectedProperties = dashboardWidgetsExpectedProperties();
        dashboardWidgetsExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardWidgetsExpectedProperties)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        String responseBody = hyperIoTHttpResponse.getResponseBody();
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode nodeResponse = mapper.readTree(responseBody);
                        Assert.assertTrue(nodeResponse.isArray());
                        ArrayNode dashboardWidgetsNode = (ArrayNode) nodeResponse;
                        Assert.assertEquals(1, dashboardWidgetsNode.size());
                        Assert.assertTrue(dashboardWidgetsNode.get(0) instanceof ObjectNode);
                        ObjectNode dashboardWidgetNode = (ObjectNode) dashboardWidgetsNode.get(0);
                        Assert.assertTrue(dashboardWidgetNode.has("widgetConf"));
                        Assert.assertTrue(dashboardWidgetNode.get("widgetConf").isTextual());
                        return dashboardWidgetNode.get("widgetConf").textValue().equals(expectedWidgetConf);
                    } catch ( Exception e) {
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllDashboardWidgetPaginatedShouldSerializeResponseCorrectly() {
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/dashboardwidgets"))
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .withParameter("delta",String.valueOf(defaultDelta))
                .withParameter("page",String.valueOf(defaultPage))
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardWidgetsExpectedProperties = dashboardWidgetsExpectedProperties();
        dashboardWidgetsExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results",dashboardWidgetsExpectedProperties)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        String responseBody = hyperIoTHttpResponse.getResponseBody();
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode nodeResponse = mapper.readTree(responseBody);
                        Assert.assertTrue(nodeResponse instanceof ObjectNode);
                        Assert.assertTrue(nodeResponse.has("results"));
                        Assert.assertTrue(nodeResponse.get("results").isArray());
                        ArrayNode dashboardWidgetsNode = (ArrayNode) nodeResponse.get("results");
                        Assert.assertEquals(1, dashboardWidgetsNode.size());
                        Assert.assertTrue(dashboardWidgetsNode.get(0) instanceof ObjectNode);
                        ObjectNode dashboardWidgetNode = (ObjectNode) dashboardWidgetsNode.get(0);
                        Assert.assertTrue(dashboardWidgetNode.has("widgetConf"));
                        Assert.assertTrue(dashboardWidgetNode.get("widgetConf").isTextual());
                        return dashboardWidgetNode.get("widgetConf").textValue().equals(expectedWidgetConf);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findDashboardWidgetConfShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/dashboardwidgets/configuration/").concat(String.valueOf(dashboardWidget.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        return hyperIoTHttpResponse.getResponseBody().equals(expectedWidgetConf);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_setDashboardWidgetConfShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String newDashboardWidgetConf = "{\"description\":\"dashboard widget of user " + dashboard.getHProject().getUser().getUsername() + new Date()+" \"}";
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/dashboardwidgets/configuration").concat("?dashboardWidgetId=").concat(String.valueOf(dashboardWidget.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(requestUri)
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .withJsonBody(newDashboardWidgetConf)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardWidgetsExpectedProperties = dashboardWidgetsExpectedProperties();
        dashboardWidgetsExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardWidgetsExpectedProperties)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("widgetConf"));
                        Assert.assertTrue(node.get("widgetConf").isTextual());
                        String baseConfigResponse = node.get("widgetConf").textValue();
                        return baseConfigResponse.equals(newDashboardWidgetConf);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test009_findAllDashboardWidgetInDashboardShouldSerializeResponseCorrectly(){
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        String expectedWidgetConfiguration = dashboardWidget.getWidgetConf();
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/dashboardwidgets/configuration/all/").concat(String.valueOf(dashboard.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> dashboardWidgetsExpectedProperties = dashboardWidgetsExpectedProperties();
        dashboardWidgetsExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(dashboardWidgetsExpectedProperties)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        String responseBody = hyperIoTHttpResponse.getResponseBody();
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode nodeResponse = mapper.readTree(responseBody);
                        Assert.assertTrue(nodeResponse.isArray());
                        ArrayNode dashboardWidgetsNode = (ArrayNode) nodeResponse;
                        Assert.assertEquals(1, dashboardWidgetsNode.size());
                        Assert.assertTrue(dashboardWidgetsNode.get(0) instanceof ObjectNode);
                        ObjectNode dashboardWidgetNode = (ObjectNode) dashboardWidgetsNode.get(0);
                        Assert.assertTrue(dashboardWidgetNode.has("widgetConf"));
                        Assert.assertTrue(dashboardWidgetNode.get("widgetConf").isTextual());
                        return dashboardWidgetNode.get("widgetConf").textValue().equals(expectedWidgetConf);
                    } catch ( Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test010_findAllDashboardWidgetInDashboardShouldSerializeResponseCorrectly() throws JsonProcessingException {
        HProject project = createHProject(true);
        Assert.assertNotNull(project);
        Assert.assertNotEquals(0, project.getId());
        Dashboard dashboard = createDashboard(project, "OFFLINE");
        Assert.assertNotNull(dashboard);
        Assert.assertNotEquals(0, dashboard.getId());
        DashboardWidget dashboardWidget = createDashboardWidget(dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        String expectedWidgetConf = dashboardWidget.getWidgetConf();
        String serializeDashboardWidget = serializeDashboardWidgetForRequest(dashboardWidget);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(serializeDashboardWidget);
        ArrayNode arrayForRequest = mapper.createArrayNode();
        arrayForRequest.add(node);
        String arrayDashboardWidgetsSerializedForRequest = mapper.writeValueAsString(arrayForRequest);
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/dashboardwidgets/configuration/all/").concat(String.valueOf(dashboard.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(requestUri)
                .withContentTypeHeader("application/json")
                .withAuthorizationAsHyperIoTAdmin()
                .withJsonBody(arrayDashboardWidgetsSerializedForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .withCustomCriteria(hyperIoTHttpResponse -> hyperIoTHttpResponse.getResponseBody().isEmpty())
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

    private String serializeDashboardWidgetForRequest(DashboardWidget dashboardWidget){
        //We must serialize the dashboard's widgets in this manner to bypass jackson's serialization.
        //If we use the jackson's serialization , the framework look on entity's property and serialize according to them.
        //So this causes the exclusion of dashboard's field from DashboardWidget's entity
        // (This dashboard field on DashboardWidget entity must be specified when we save a DashboardWidget Entity).
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode dashboardWidgetSerialized = mapper.createObjectNode();
        dashboardWidgetSerialized.putObject("dashboard");
        ((ObjectNode)dashboardWidgetSerialized.get("dashboard")).put("id",dashboardWidget.getDashboard().getId());
        dashboardWidgetSerialized.put("widgetConf",dashboardWidget.getWidgetConf());
        dashboardWidgetSerialized.put("id",dashboardWidget.getId());
        String serializedEntity ;
        try {
            serializedEntity = mapper.writeValueAsString(dashboardWidgetSerialized);
        } catch (Exception exc){
            //Serialization error.
            throw new RuntimeException();
        }
        return serializedEntity;

    }

    private String generateRandomString(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    private List<String> dashboardWidgetsExpectedProperties(){
        List<String> dashboardWidgetsExpectedProperties = new ArrayList<>();
        dashboardWidgetsExpectedProperties.add("widgetConf");
        return dashboardWidgetsExpectedProperties;
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

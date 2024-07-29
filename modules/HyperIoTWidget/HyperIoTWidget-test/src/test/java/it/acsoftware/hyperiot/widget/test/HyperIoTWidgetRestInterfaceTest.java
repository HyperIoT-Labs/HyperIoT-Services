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

package it.acsoftware.hyperiot.widget.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.http.*;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidator;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidatorBuilder;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.widget.model.Widget;
import it.acsoftware.hyperiot.widget.model.WidgetCategory;
import it.acsoftware.hyperiot.widget.service.rest.WidgetRestApi;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static it.acsoftware.hyperiot.widget.test.HyperIoTWidgetConfiguration.*;

/**
 * @author Francesco Salerno
 *
 * This is a test class relative to Interface's test of WidgetRestApi
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTWidgetRestInterfaceTest extends KarafTestSupport {

    //force global configuration
    public Option[] config() {
        return null;
    }

    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @Before
    public void impersonateAsHyperIoTAdmin(){
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(widgetRestApi, admin);
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
        assertContains("HyperIoTWidget-features ", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_saveWidgetShouldSerializeResponseCorrectly()  {
        Widget widget = new Widget();
        widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        widget.setDescription(widgetDescription);
        widget.setWidgetCategory(WidgetCategory.ALL);
        widget.setBaseConfig("image-data");
        widget.setType("image-data");
        widget.setCols(2);
        widget.setRows(3);
        widget.setImage(widgetImageData);
        widget.setPreView(widgetImageDataPreview);
        widget.setOffline(true);
        String serializedWidgetForRequest = serializeWidgetEntityForRequest(widget);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedWidgetForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(widgetEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("widgetCategory", widgetCategoryEnumWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findWidgetShouldSerializeResponseCorrectly(){
        Widget widget = createWidget(true);
        Assert.assertNotEquals(0, widget.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets/").concat(String.valueOf(widget.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(widgetEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("widgetCategory", widgetCategoryEnumWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateWidgetShouldSerializeResponseCorrectly(){
        Widget widget = createWidget(true);
        Assert.assertNotEquals(0, widget.getId());
        widget.setDescription(widgetDescription + new Date());
        String serializedWidgetForRequest = serializeWidgetEntityForRequest(widget);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedWidgetForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(widgetEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("widgetCategory", widgetCategoryEnumWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteWidgetShouldSerializeResponseCorrectly(){
        Widget widget = createWidget(true);
        Assert.assertNotEquals(0, widget.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets/").concat(String.valueOf(widget.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
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
    public void test005_findAllWidgetsShouldSerializeResponseCorrectly(){
        Widget widget = createWidget(true);
        Assert.assertNotEquals(0, widget.getId());
        Widget widget2 = createWidget(true);
        Assert.assertNotEquals(0, widget2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(widgetEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("widgetCategory", widgetCategoryEnumWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllWidgetsPaginatedShouldSerializeResponseCorrectly(){
        Widget widget = createWidget(true);
        Assert.assertNotEquals(0, widget.getId());
        Widget widget2 = createWidget(true);
        Assert.assertNotEquals(0, widget2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets"))
                .withAuthorizationAsHyperIoTAdmin()
                .withParameter("delta", String.valueOf(defaultDelta))
                .withParameter("page", String.valueOf(defaultPage))
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results", widgetEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.widgetCategory", widgetCategoryEnumWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findAllWidgetInCategoriesShouldSerializeResponseCorrectly(){
        Widget widget = createWidget(true);
        Assert.assertNotEquals(0, widget.getId());
        Assert.assertEquals(WidgetCategory.ALL, widget.getWidgetCategory());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets").concat("/listed/").concat("ALL");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> responseMapExpectedProperties = new ArrayList<>();
        responseMapExpectedProperties.add("catInfo");
        responseMapExpectedProperties.add("widgetMap");
        HyperIoTHttpResponseValidatorBuilder testValidatorBuilder = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(responseMapExpectedProperties)
                .containExactInnerProperties("catInfo",widgetCategoryEnumStringValue() )
                .containExactInnerProperties("widgetMap", widgetCategoryEnumStringValue());
        for(String widgCategory : widgetCategoryEnumStringValue()){
            testValidatorBuilder.containExactInnerProperties("catInfo.".concat(widgCategory),widgetCategoryEnumWithJsonViewPublicExpectedProperties());
        }
        HyperIoTHttpResponseValidator testValidator= testValidatorBuilder
                .containExactInnerProperties("widgetMap.".concat(WidgetCategory.ALL.name()), widgetEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("widgetMap.".concat(WidgetCategory.ALL.name()).concat(".widgetCategory"), widgetCategoryEnumWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_rateWidgetShouldSerializeResponseCorrectly(){
        Widget widget = createWidget(true);
        Assert.assertNotEquals(0, widget.getId());
        Integer rate = 10;
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/widgets").concat("/rate/").concat(String.valueOf(rate));
        String serializedWidgetForRequest = serializeWidgetEntityForRequest(widget);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedWidgetForRequest)
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
    *
    * Utility method for test.
    *
    *
    *
     */

    private List<String> widgetCategoryEnumStringValue(){
        List<String> widgetCategoryEnumStringValue = new ArrayList<>();
        for(WidgetCategory category : WidgetCategory.values()){
            widgetCategoryEnumStringValue.add(category.name());
        }
        return widgetCategoryEnumStringValue;
    }

    private String serializeWidgetEntityForRequest(Widget widget){
        //We must serialize the Widget's entity in this manner to bypass jackson's serialization.
        //We need to serialize Widget's category field of Widget Entity with the name of the Widget's Category enum such that jackson can deserialize them.
        try{
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            String serializedWidget = mapper.writeValueAsString(widget);
            JsonNode widgetNode = mapper.readTree(serializedWidget);
            Assert.assertTrue(widgetNode.isObject());
            Assert.assertTrue(widgetNode.has("widgetCategory"));
            ObjectNode widgetObjectNode = (ObjectNode) widgetNode ;
            widgetObjectNode.remove("widgetCategory");
            Assert.assertFalse(widgetObjectNode.has("widgetCategory"));
            widgetObjectNode.put("widgetCategory", widget.getWidgetCategory().getName().toUpperCase());
            Assert.assertTrue(widgetNode.has("widgetCategory"));
            return mapper.writeValueAsString(widgetObjectNode);
        } catch (Exception e ){
            //Serialization failure.
            Assert.fail();
            throw new HyperIoTRuntimeException();
        }
    }

    private List<String> widgetEntityWithJsonViewPublicExpectedProperties(){
        List<String> widgetEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        widgetEntityWithJsonViewPublicExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        widgetEntityWithJsonViewPublicExpectedProperties.add("name");
        widgetEntityWithJsonViewPublicExpectedProperties.add("description");
        widgetEntityWithJsonViewPublicExpectedProperties.add("widgetCategory");
        widgetEntityWithJsonViewPublicExpectedProperties.add("domains");
        widgetEntityWithJsonViewPublicExpectedProperties.add("baseConfig");
        widgetEntityWithJsonViewPublicExpectedProperties.add("type");
        widgetEntityWithJsonViewPublicExpectedProperties.add("cols");
        widgetEntityWithJsonViewPublicExpectedProperties.add("rows");
        widgetEntityWithJsonViewPublicExpectedProperties.add("image");
        widgetEntityWithJsonViewPublicExpectedProperties.add("preView");
        widgetEntityWithJsonViewPublicExpectedProperties.add("avgRating");
        widgetEntityWithJsonViewPublicExpectedProperties.add("offline");
        widgetEntityWithJsonViewPublicExpectedProperties.add("realTime");
        return widgetEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> widgetCategoryEnumWithJsonViewPublicExpectedProperties(){
        List<String> widgetEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        widgetEntityWithJsonViewPublicExpectedProperties.add("id");
        widgetEntityWithJsonViewPublicExpectedProperties.add("name");
        widgetEntityWithJsonViewPublicExpectedProperties.add("fontIcon");
        return widgetEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
    }

    private Widget createWidget(boolean isOffline) {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Widget widget = new Widget();
        widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        widget.setDescription(widgetDescription);
        widget.setWidgetCategory(WidgetCategory.ALL);
        widget.setBaseConfig("image-data");
        widget.setType("image-data");
        widget.setCols(2);
        widget.setRows(3);
        widget.setImage(widgetImageData);
        widget.setPreView(widgetImageDataPreview);
        if (isOffline) {
            widget.setRealTime(false);
            widget.setOffline(true);
        }

        this.impersonateUser(widgetRestApi, adminUser);
        Response restResponse = widgetRestApi.saveWidget(widget);
        Assert.assertEquals(200, restResponse.getStatus());

        Assert.assertNotEquals(0, ((Widget) restResponse.getEntity()).getId());
        Assert.assertEquals(widget.getName(),
                ((Widget) restResponse.getEntity()).getName());
        Assert.assertEquals(widgetDescription,
                ((Widget) restResponse.getEntity()).getDescription());
        Assert.assertEquals(0,
                ((Widget) restResponse.getEntity()).getWidgetCategory().getId());
        Assert.assertEquals("all",
                ((Widget) restResponse.getEntity()).getWidgetCategory().getName());
        Assert.assertEquals("icon-hyt_layout",
                ((Widget) restResponse.getEntity()).getWidgetCategory().getFontIcon());
        Assert.assertEquals("ALL",
                ((Widget) restResponse.getEntity()).getWidgetCategory().name());
        Assert.assertEquals(0,
                ((Widget) restResponse.getEntity()).getWidgetCategory().ordinal());

        Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getBaseConfig());
        Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((Widget) restResponse.getEntity()).getCols());
        Assert.assertEquals(3, ((Widget) restResponse.getEntity()).getRows());
        Assert.assertEquals(widgetImageData, ((Widget) restResponse.getEntity()).getImage());
        Assert.assertEquals(widgetImageDataPreview, ((Widget) restResponse.getEntity()).getPreView());
        if (isOffline) {
            Assert.assertFalse(((Widget) restResponse.getEntity()).isRealTime());
            Assert.assertTrue(((Widget) restResponse.getEntity()).isOffline());
        } else {
            Assert.assertTrue(((Widget) restResponse.getEntity()).isRealTime());
            Assert.assertFalse(((Widget) restResponse.getEntity()).isOffline());
        }
        return widget;
    }

    @After
    public void afterTest() {
        // Remove all widgets in every tests
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(widgetRestApi, adminUser);
        Response restResponse = widgetRestApi.findAllWidget();
        List<Widget> listWidgets = restResponse.readEntity(new GenericType<List<Widget>>() {
        });
        if (!listWidgets.isEmpty()) {
            Assert.assertFalse(listWidgets.isEmpty());
            for (Widget widget : listWidgets) {
                this.impersonateUser(widgetRestApi, adminUser);
                Response restResponse1 = widgetRestApi.deleteWidget(widget.getId());
                Assert.assertEquals(200, restResponse1.getStatus());
                Assert.assertNull(restResponse1.getEntity());
            }
        }
    }
}

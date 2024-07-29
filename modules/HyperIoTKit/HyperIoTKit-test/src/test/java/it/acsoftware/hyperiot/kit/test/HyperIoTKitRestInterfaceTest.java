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

package it.acsoftware.hyperiot.kit.test;

import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
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
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.test.util.HyperIoTHUserTestUtils;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.kit.service.KitUtils;
import it.acsoftware.hyperiot.kit.service.rest.KitRestApi;
import it.acsoftware.hyperiot.kit.template.model.HDeviceTemplate;
import it.acsoftware.hyperiot.kit.template.model.HPacketFieldTemplate;
import it.acsoftware.hyperiot.kit.template.model.HPacketTemplate;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.test.util.HyperIoTPermissionTestUtil;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
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

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.kit.test.HyperIoTKitConfiguration.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Francesco Salerno
 * This is a test class relative to Interface's test of KitRestApi class.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTKitRestInterfaceTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        return HyperIoTServicesTestConfigurationBuilder
                .createStandardConfiguration()
                .build();
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @Before
    public void impersonateAsHyperIoTAdmin(){
        KitRestApi kitRestApi = getOsgiService(KitRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(kitRestApi, admin);
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    private HyperIoTAction getHyperIoTAction(String resourceName,
                                             HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder
                .createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
    }

    @Test
    public void test000_hyperIoTFrameworkShouldBeInstalled() throws Exception {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class,0);
        String features = executeCommand("feature:list -i");
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTSharedEntity-features",features);
        assertContains("HyperIoTKit-features", features);
        assertContains("HyperIoTHProject-features", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_saveKitShouldSerializeResponseCorrectly(){
        String jsonKit = createKitForRequest();
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(jsonKit)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(kitEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices", hdeviceTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets", hPacketTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets.fields", hPacketFieldTemplateEntityWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findKitShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits/").concat(String.valueOf(kit.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(kitEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices", hdeviceTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets", hPacketTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets.fields", hPacketFieldTemplateEntityWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);

    }

    @Test
    public void test003_updateKitShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        String jsonKit = "{\n" +
                "      \"id\": "+String.valueOf(kit.getId())+", \n" +
                "      \"label\":\""+ createRandomString() +"\",\n" +
                "      \"kitVersion\":\"3.2.2\",\n" +
                "      \"projectId\": 0 \n" +
                "}";
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(jsonKit)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(kitEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices", hdeviceTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets", hPacketTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets.fields", hPacketFieldTemplateEntityWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteKitShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits/").concat(String.valueOf(kit.getId())))
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
    public void test005_findAllKitShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(kitEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices", hdeviceTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets", hPacketTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("devices.packets.fields", hPacketFieldTemplateEntityWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllKitPaginatedShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("delta",String.valueOf(defaultDelta))
                .withParameter("page", String.valueOf(defaultPage))
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results",kitEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.devices", hdeviceTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.devices.packets", hPacketTemplateEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.devices.packets.fields", hPacketFieldTemplateEntityWithJsonViewPublicExpectedProperties())
                .withStatusEqual(200)
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findKitTagsShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        AssetTag tag = addTagToKit(kit);
        Assert.assertNotEquals(0, tag.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits/").concat(String.valueOf(kit.getId())).concat("/tags");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(assetTagEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("owner", hyperIotAssetOwnerWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_addTagToKitShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        String jsonTagForRequest = "{\n" +
                "      \"name\":\""+ createRandomString() +"\",\n" +
                "      \"description\":\""+ createRandomString() +"\",\n" +
                "      \"color\":\"red\"\n" +
                "}";
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/kits/").concat(String.valueOf(kit.getId())).concat("/tags"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(jsonTagForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(assetTagEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("owner", hyperIotAssetOwnerWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test009_deleteTagFromKitTagListShouldSerializeResponseCorrectly(){
        Kit kit = createKit();
        Assert.assertNotEquals(0, kit.getId());
        AssetTag tag = addTagToKit(kit);
        Assert.assertNotEquals(0, tag.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/kits/").concat(String.valueOf(kit.getId()))
                .concat("/tags/").concat(String.valueOf(tag.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(requestUri)
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

    /*
    *
    *
    * Utility method for test.
    *
    *
     */

    private List<String> assetTagEntityWithJsonViewPublicExpectedProperties(){
        List<String> assetTagEntityWithJsonViewPublicExpectedProperties = new ArrayList<>(hyperIoTAbstractEntityProperties());
        assetTagEntityWithJsonViewPublicExpectedProperties.add("name");
        assetTagEntityWithJsonViewPublicExpectedProperties.add("description");
        assetTagEntityWithJsonViewPublicExpectedProperties.add("color");
        assetTagEntityWithJsonViewPublicExpectedProperties.add("owner");
        return assetTagEntityWithJsonViewPublicExpectedProperties;
    }
    private List<String> hyperIotAssetOwnerWithJsonViewPublicExpectedProperties(){
        List<String> hyperIotAssetOwnerWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hyperIotAssetOwnerWithJsonViewPublicExpectedProperties.add("ownerResourceName");
        hyperIotAssetOwnerWithJsonViewPublicExpectedProperties.add("ownerResourceId");
        hyperIotAssetOwnerWithJsonViewPublicExpectedProperties.add("userId");
        hyperIotAssetOwnerWithJsonViewPublicExpectedProperties.add("resourceName");
        return hyperIotAssetOwnerWithJsonViewPublicExpectedProperties;
    }

    private List<String> kitEntityWithJsonViewPublicExpectedProperties(){
        List<String> kitEntityWithJsonViewPublicExpectedProperties = new ArrayList<>(hyperIoTAbstractEntityProperties());
        kitEntityWithJsonViewPublicExpectedProperties.add("projectId");
        kitEntityWithJsonViewPublicExpectedProperties.add("kitVersion");
        kitEntityWithJsonViewPublicExpectedProperties.add("devices");
        kitEntityWithJsonViewPublicExpectedProperties.add("label");
        return kitEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> hdeviceTemplateEntityWithJsonViewPublicExpectedProperties(){
        List<String> hdeviceTemplateWithJsonViewPublicExpectedProperties = new ArrayList<>(hyperIoTAbstractEntityProperties());
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("deviceLabel");
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("firmwareVersion");
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("model");
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("brand");
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("softwareVersion");
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("description");
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("kit");
        hdeviceTemplateWithJsonViewPublicExpectedProperties.add("packets");
        return hdeviceTemplateWithJsonViewPublicExpectedProperties;
    }

    private List<String> hPacketTemplateEntityWithJsonViewPublicExpectedProperties(){
        List<String> hPacketTemplateWithJsonViewPublicExpectedProperties = new ArrayList<>(hyperIoTAbstractEntityProperties());
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("name");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("type");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("format");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("serialization");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("device");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("version");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("timestampField");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("timestampFormat");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("unixTimestamp");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("unixTimestampFormatSeconds");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("trafficPlan");
        hPacketTemplateWithJsonViewPublicExpectedProperties.add("fields");
        return hPacketTemplateWithJsonViewPublicExpectedProperties;
    }

    private List<String> hPacketFieldTemplateEntityWithJsonViewPublicExpectedProperties(){
        List<String> hPacketFieldTemplateWithJsonViewPublicExpectedProperties = new ArrayList<>(hyperIoTAbstractEntityProperties());
        hPacketFieldTemplateWithJsonViewPublicExpectedProperties.add("name");
        hPacketFieldTemplateWithJsonViewPublicExpectedProperties.add("description");
        hPacketFieldTemplateWithJsonViewPublicExpectedProperties.add("type");
        hPacketFieldTemplateWithJsonViewPublicExpectedProperties.add("multiplicity");
        hPacketFieldTemplateWithJsonViewPublicExpectedProperties.add("unit");
        hPacketFieldTemplateWithJsonViewPublicExpectedProperties.add("innerFields");
        return hPacketFieldTemplateWithJsonViewPublicExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
    }

    public String createKitForRequest(){
        return "{\n" +
                "      \"label\":\""+ createRandomString() +"\",\n" +
                "      \"kitVersion\":\""+ createRandomString() +"\",\n" +
                "      \"projectId\":\""+ KitUtils.SYSTEM_KIT_PROJECT_ID +"\",\n"+
                "      \"devices\":[\n" +
                "         {\n" +
                "            \"deviceLabel\":\""+ createRandomString() +"\",\n" +
                "            \"brand\":\""+ createRandomString() +"\",\n" +
                "            \"model\":\""+ createRandomString() +"\",\n" +
                "            \"firmwareVersion\":\"\",\n" +
                "            \"softwareVersion\":\"\",\n" +
                "            \"description\":\"\",\n" +
                "            \"packets\":[\n" +
                "               {\n" +
                "                  \"name\":\""+ createRandomString() +"\",\n" +
                "                  \"type\":\"INPUT\",\n" +
                "                  \"format\":\"JSON\",\n" +
                "                  \"serialization\":\"NONE\",\n" +
                "                  \"version\":\"1\",\n" +
                "                  \"fields\":[\n" +
                "                     {\n" +
                "                        \"name\":\""+createRandomString()+"\",\n" +
                "                        \"type\":\"INTEGER\",\n" +
                "                        \"multiplicity\":\"SINGLE\",\n" +
                "                        \"unit\":\"\"\n" +
                "                     }\n" +
                "                  ],\n" +
                "                  \"timestampField\":\"timestamp\",\n" +
                "                  \"timestampFormat\":\"dd/MM/yyyy hh.mmZ\",\n" +
                "                  \"unixTimestamp\":true,\n" +
                "                  \"unixTimestampFormatSeconds\":false,\n" +
                "                  \"trafficPlan\":\"LOW\"\n" +
                "               }\n" +
                "            ]\n" +
                "         }\n" +
                "      ]\n" +
                "}";
    }

    private String createRandomString(){
        return java.util.UUID.randomUUID().toString().replaceAll("-", "");
    }

    public AssetTag addTagToKit(Kit kit){
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        KitRestApi kitRestApi = getOsgiService(KitRestApi.class);
        AssetTag tag = new AssetTag();
        tag.setName(createRandomString());
        tag.setDescription(createRandomString());
        tag.setColor("red");
        impersonateUser(kitRestApi, adminUser);
        Response restResponse = kitRestApi.addTagToKit(kit.getId(), tag);
        Assert.assertEquals(200, restResponse.getStatus());
        AssetTag tagFromResponse = (AssetTag)  restResponse.getEntity();
        Assert.assertNotEquals(0, tagFromResponse.getId());
        Assert.assertEquals(tag.getDescription(), tagFromResponse.getDescription());
        Assert.assertEquals(tag.getName(), tagFromResponse.getName());
        Assert.assertEquals(tag.getColor(), tagFromResponse.getColor());
        Assert.assertNotNull(tag.getOwner());
        Assert.assertEquals(Kit.class.getName(), tagFromResponse.getOwner().getOwnerResourceName());
        Assert.assertEquals(kit.getId(), (long)tagFromResponse.getOwner().getOwnerResourceId());
        return tagFromResponse;
    }

    public Kit createKit(){
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        KitRestApi kitRestApi = getOsgiService(KitRestApi.class);
        Kit kit = createKitTemplate("kitLabel", KitUtils.SYSTEM_KIT_PROJECT_ID);
        HDeviceTemplate hDeviceTemplate = createHDeviceTemplate("hDeviceLabel");
        HPacketTemplate hPacketTemplate = createHPacketTemplate("hpacketName", "1.0.0");
        HPacketFieldTemplate hPacketFieldTemplate = createHPacketFieldTemplate("packetFieldTemplate");
        addPacketFieldTemplateToPacketTemplate(hPacketTemplate, hPacketFieldTemplate);
        addPacketTemplateToDeviceTemplate(hDeviceTemplate, hPacketTemplate);
        addDeviceTemplateToKit(kit, hDeviceTemplate);
        impersonateUser(kitRestApi, adminUser);
        Response restResponse = kitRestApi.saveKit(kit);
        Assert.assertEquals(200, restResponse.getStatus());
        Kit kitFromResponse = (Kit) restResponse.getEntity();
        assertEquals(kit.getLabel(),kitFromResponse.getLabel());
        assertEquals(kit.getKitVersion(),kitFromResponse.getKitVersion());
        assertEquals(kit.getProjectId(),kitFromResponse.getProjectId());
        Assert.assertNotEquals(0, kitFromResponse.getId());
        Assert.assertNotNull(kit.getDevices());
        Assert.assertEquals(1, kit.getDevices().size());
        HDeviceTemplate deviceFromResponse = kit.getDevices().get(0);
        Assert.assertNotEquals(0, deviceFromResponse.getId());
        Assert.assertEquals(hDeviceTemplate.getDescription(), deviceFromResponse.getDescription());
        Assert.assertEquals(hDeviceTemplate.getDeviceLabel(), deviceFromResponse.getDeviceLabel());
        Assert.assertEquals(hDeviceTemplate.getBrand(), deviceFromResponse.getBrand());
        Assert.assertEquals(hDeviceTemplate.getFirmwareVersion(), deviceFromResponse.getFirmwareVersion());
        Assert.assertEquals(hDeviceTemplate.getSoftwareVersion(), deviceFromResponse.getSoftwareVersion());
        Assert.assertEquals(hDeviceTemplate.getModel(), deviceFromResponse.getModel());
        Assert.assertNotNull(deviceFromResponse.getPackets());
        Assert.assertEquals(1, deviceFromResponse.getPackets().size());
        HPacketTemplate packetFromResponse = deviceFromResponse.getPackets().get(0);
        Assert.assertNotEquals(0, packetFromResponse.getId());
        Assert.assertEquals(hPacketTemplate.getName(), packetFromResponse.getName());
        Assert.assertEquals(hPacketTemplate.getFormat(), packetFromResponse.getFormat());
        Assert.assertEquals(hPacketTemplate.getSerialization(), packetFromResponse.getSerialization());
        Assert.assertEquals(hPacketTemplate.getTimestampField(), packetFromResponse.getTimestampField());
        Assert.assertEquals(hPacketTemplate.getTimestampFormat(), packetFromResponse.getTimestampFormat());
        Assert.assertEquals(hPacketTemplate.getTrafficPlan(), packetFromResponse.getTrafficPlan());
        Assert.assertEquals(hPacketTemplate.getVersion(), packetFromResponse.getVersion());
        Assert.assertEquals(hPacketTemplate.getType(),packetFromResponse.getType());
        Assert.assertNotNull(packetFromResponse.getDevice());
        Assert.assertEquals(deviceFromResponse.getId(), packetFromResponse.getDevice().getId());
        Assert.assertNotNull(packetFromResponse.getFields());
        Assert.assertEquals(1, packetFromResponse.getFields().size());
        HPacketFieldTemplate fieldFromResponse = packetFromResponse.getFields().get(0);
        Assert.assertNotEquals(0, fieldFromResponse.getId());
        Assert.assertEquals(hPacketFieldTemplate.getDescription(), fieldFromResponse.getDescription());
        Assert.assertEquals(hPacketFieldTemplate.getName(), fieldFromResponse.getName());
        Assert.assertEquals(hPacketFieldTemplate.getMultiplicity(), fieldFromResponse.getMultiplicity());
        Assert.assertEquals(hPacketFieldTemplate.getType(), fieldFromResponse.getType());
        Assert.assertEquals(hPacketFieldTemplate.getUnit(), fieldFromResponse.getUnit());
        Assert.assertNotNull(hPacketFieldTemplate.getPacket());
        Assert.assertEquals(packetFromResponse.getId(), fieldFromResponse.getPacket().getId());
        return kitFromResponse;
    }

    private void addDeviceTemplateToKit(Kit kit, HDeviceTemplate device){
        kit.getDevices().add(device);
    }

    private void addPacketTemplateToDeviceTemplate(HDeviceTemplate deviceTemplate, HPacketTemplate packetTemplate){
        deviceTemplate.getPackets().add(packetTemplate);
    }

    private void addPacketFieldTemplateToPacketTemplate(HPacketTemplate packetTemplate , HPacketFieldTemplate hPacketFieldTemplate){
        packetTemplate.getFields().add(hPacketFieldTemplate);
    }

    private Kit createKitTemplate(String kitLabelPrefix, long projectId){
        Kit kit = new Kit();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        kit.setLabel(kitLabelPrefix.concat(randomUUID));
        kit.setKitVersion(randomUUID);
        kit.setProjectId(projectId);
        kit.setDevices(new LinkedList<>());
        return kit;
    }

    private HDeviceTemplate createHDeviceTemplate(String hDeviceLabelPrefix){
        HDeviceTemplate hDeviceTemplate = new HDeviceTemplate();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        hDeviceTemplate.setDeviceLabel(hDeviceLabelPrefix.concat(randomUUID));
        hDeviceTemplate.setBrand("brand".concat(randomUUID));
        hDeviceTemplate.setModel("model".concat(randomUUID));
        hDeviceTemplate.setFirmwareVersion("firmwareversion".concat(randomUUID));
        hDeviceTemplate.setSoftwareVersion("softwareversion".concat(randomUUID));
        hDeviceTemplate.setDescription("description".concat(randomUUID));
        hDeviceTemplate.setPackets(new LinkedList<>());
        return hDeviceTemplate;
    }

    private HPacketTemplate createHPacketTemplate(String packetNamePrefix, String versionPrefix){
        HPacketTemplate packetTemplate = new HPacketTemplate();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        packetTemplate.setName(packetNamePrefix.concat(randomUUID));
        packetTemplate.setType(HPacketType.INPUT);
        packetTemplate.setFormat(HPacketFormat.JSON);
        packetTemplate.setSerialization(HPacketSerialization.NONE);
        packetTemplate.setVersion(versionPrefix.concat(randomUUID));
        packetTemplate.setTimestampField("timestamp");
        packetTemplate.setTimestampFormat("dd/MM/yyyy hh.mmZ");
        packetTemplate.setUnixTimestamp(true);
        packetTemplate.setUnixTimestamp(true);
        packetTemplate.setUnixTimestampFormatSeconds(false);
        packetTemplate.setTrafficPlan(HPacketTrafficPlan.LOW);
        packetTemplate.setFields(new LinkedList<>());
        return packetTemplate;
    }

    private HPacketFieldTemplate createHPacketFieldTemplate(String packetFieldNamePrefix){
        HPacketFieldTemplate packetFieldTemplate = new HPacketFieldTemplate();
        String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
        packetFieldTemplate.setName(packetFieldNamePrefix.concat(randomUUID));
        packetFieldTemplate.setType(HPacketFieldType.INTEGER);
        packetFieldTemplate.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        packetFieldTemplate.setUnit("deg");
        return packetFieldTemplate;
    }

    @After
    public void afterTest(){
        AreaDeviceRepository areaDeviceRepository = getOsgiService(AreaDeviceRepository.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        KitSystemApi kitSystemApi = getOsgiService(KitSystemApi.class);
        SharedEntitySystemApi sharedEntitySystemApi = getOsgiService(SharedEntitySystemApi.class);
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
        //truncate tables after test.
        HyperIoTTestUtils.truncateTables(areaDeviceRepository, null);
        HyperIoTTestUtils.truncateTables(areaSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
        HyperIoTTestUtils.truncateTables(kitSystemApi,null);
        HyperIoTTestUtils.truncateTables(sharedEntitySystemApi, null);
        HyperIoTPermissionTestUtil.dropPermissions(roleSystemApi,permissionSystemApi);
        HyperIoTHUserTestUtils.truncateRoles(roleSystemApi);
        HyperIoTHUserTestUtils.truncateHUsers(hUserSystemApi);
    }

}

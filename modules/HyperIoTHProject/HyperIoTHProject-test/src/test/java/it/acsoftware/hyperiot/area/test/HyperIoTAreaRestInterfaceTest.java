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

package it.acsoftware.hyperiot.area.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.model.AreaMapInfo;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.http.*;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidator;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidatorBuilder;
import it.acsoftware.hyperiot.base.test.util.HyperIoTTestUtils;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static it.acsoftware.hyperiot.area.test.HyperIoTAreaConfiguration.defaultDelta;
import static it.acsoftware.hyperiot.area.test.HyperIoTAreaConfiguration.defaultPage;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAreaRestInterfaceTest extends KarafTestSupport {

    /*
       TODO
           We must add test for  getAreaDeviceDeepList, getAreaDeviceDeepListFromRoot defined on AreaRestApi.
           This service use a repository service that execute a native query on database.
           H2 (the database used for test purpose) doesn't support this type of query .
           To resolve the problem :
           1) We can use different type of database for integration test. (For example use postgres as in "production environment"
           2) We can mock the call to repository using a mocking framework like mockito
           (To do so we need to create a bundle that export mockito dependency)
     */


    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
    }

    @Before
    public void impersonateAsHyperIoTAdmin(){
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(areaRestApi, admin);
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
    public void test001_saveAreaShouldSerializeResponseCorrectly()  {
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        AreaMapInfo areaMapInfo = new AreaMapInfo();
        areaMapInfo.setZ(5.5);
        areaMapInfo.setY(5.5);
        areaMapInfo.setX(5.5);
        areaMapInfo.setIcon("");
        area.setMapInfo(areaMapInfo);
        area.setProject(project);
        String serializedAreaForRequest = serializeAreaForRequest(area);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedAreaForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(areaExpectedPropertiesWithJsonViewPublic())
                .containExactInnerProperties("mapInfo",areaMapInfoExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findAreaShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/").concat(String.valueOf(area.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(areaExpectedPropertiesWithJsonViewPublic())
                .containExactInnerProperties("mapInfo",areaMapInfoExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateAreaShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        area.setDescription("New description in data : "+ new Date());
        String serializedAreaForRequest = serializeAreaForRequest(area);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedAreaForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(areaExpectedPropertiesWithJsonViewPublic())
                .containExactInnerProperties("mapInfo",areaMapInfoExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteAreaShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/").concat(String.valueOf(area.getId())))
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
    public void test005_findAllAreaShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        Area area2 = createArea(project);
        Assert.assertNotEquals(0, area2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(areaExpectedPropertiesWithJsonViewPublic())
                .containExactInnerProperties("mapInfo",areaMapInfoExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllAreaPaginatedShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        Area area2 = createArea(project);
        Assert.assertNotEquals(0, area2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("delta",String.valueOf(defaultDelta))
                .withParameter("page",String.valueOf(defaultPage))
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results",areaExpectedPropertiesWithJsonViewCompact())
                .containExactInnerProperties("results.mapInfo",areaMapInfoExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_getConfigShouldSerializeResponseCorrectly(){
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/config"))
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .withCustomCriteria(hyperIoTHttpResponse ->{
                        long assetsFileMaxLength ;
                        try {
                            AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
                            Field assetFileMaxLengthField = areaRestApi.getClass().getDeclaredField("assetsFileMaxLength");
                            assetFileMaxLengthField.setAccessible(true);
                             assetsFileMaxLength = (long)assetFileMaxLengthField.get(areaRestApi);
                        }catch (Exception exception){
                            Assert.fail();
                            throw new HyperIoTRuntimeException();
                        }
                        String expectedConfig = String.format("{ \"maxFileSize\": %d }", assetsFileMaxLength);
                        return hyperIoTHttpResponse.getResponseBody().equals(expectedConfig);
                })
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_findInnerAreasShouldSerializeResponseCorrectly(){
        //hadmin save a project
        //hadmin save an Area called area related to this project.
        //hadmin save an Area called area2 as an inner area of the Area area saved before.
        //hadmin find all inner area with the following call findInnerAreas(URL -> /hyperiot/ares/{areaId}/tree
        //Assert that response is serialized correctly.
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        Area area2 = createInnerArea(project, area);
        Assert.assertNotEquals(0, area2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/").concat(String.valueOf(area.getId())).concat("/tree"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactInnerProperties("innerArea", areaExpectedPropertiesWithJsonViewExtended())
                .containExactProperties(areaExpectedPropertiesWithJsonViewExtended())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
        //Remove inner area explicity.
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        areaSystemApi.remove(area2.getId(),null);
    }

    @Test
    public void test009_getAreaPathShouldSerializeResponseCorrectly(){
        //hadmin save a project
        //hadmin save an Area called area related to this project.
        //hadmin save an Area called area2 as an inner area of the Area area saved before.
        //hadmin find  area path (From leaf area to root area) with the following call getAreaPath(URL -> /hyperiot/areas/{id}/path
        //Assert that response is serialized correctly.
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        Area area2 = createInnerArea(project, area);
        Assert.assertNotEquals(0, area2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/").concat(String.valueOf(area2.getId())).concat("/path"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(areaExpectedPropertiesWithJsonViewCompact())
                .containExactInnerProperties("mapInfo",areaMapInfoExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
        //Remove inner area explicity.
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        areaSystemApi.remove(area2.getId(),null);
    }

    @Test
    public void test010_getAreaDeviceListShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HDevice device2 = createHDevice(project);
        Assert.assertNotEquals(0, device2.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        AreaDevice areaDevice = createAreaDevice(area, device);
        Assert.assertNotEquals(0, areaDevice.getId());
        AreaDevice areaDevice2 = createAreaDevice(area, device2);
        Assert.assertNotEquals(0, areaDevice2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/").concat(String.valueOf(area.getId())).concat("/devices"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(areaDeviceExpectedPropertiesWithJsonViewCompact())
                .containExactInnerProperties("area", areaExpectedPropertiesWithJsonViewCompact())
                .containExactInnerProperties("device", hDeviceWithJsonViewCompact())
                .containExactInnerProperties("device.project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test011_addAreaDeviceShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(device);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/").concat(String.valueOf(area.getId())).concat("/devices"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(areaDevice)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(areaDeviceExpectedPropertiesWithJsonViewPublic())
                .containExactInnerProperties("area", areaExpectedPropertiesWithJsonViewPublic())
                .containExactInnerProperties("device", hDeviceWithJsonViewPublic())
                .containExactInnerProperties("device.project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test012_getAreaDeviceShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        AreaDevice areaDevice = createAreaDevice(area, device);
        Assert.assertNotEquals(0, areaDevice.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas").concat("/devices/").concat(String.valueOf(areaDevice.getId()));
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
                .containExactProperties(areaDeviceExpectedPropertiesWithJsonViewCompact())
                .containExactInnerProperties("area", areaExpectedPropertiesWithJsonViewCompact())
                .containExactInnerProperties("device", hDeviceWithJsonViewCompact())
                .containExactInnerProperties("device.project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test013_removeAreaDeviceShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        AreaDevice areaDevice = createAreaDevice(area, device);
        Assert.assertNotEquals(0, areaDevice.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/areas/").concat(String.valueOf(area.getId()))
                .concat("/devices/").concat(String.valueOf(areaDevice.getId()));
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
     *       Utility methods for test.
     *
     *
     */

    private String serializeAreaForRequest(Area area){
        //We must serialize the Area Entity in this manner to bypass jackson's serialization.
        //If we use the jackson's serialization , the framework look on entity's property and serialize according to them.
        //So this causes the exclusion of project field from Area entity.
        // (The project field on Area entity must be specified when we save/update an Area Entity).
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode areaNode = mapper.createObjectNode();
        areaNode.putObject("project");
        ((ObjectNode)areaNode.get("project")).put("id", area.getProject().getId());
        areaNode.put("description", area.getDescription());
        areaNode.put("name", area.getName());
        areaNode.put("id", area.getId());
        areaNode.putObject("mapInfo");
        ((ObjectNode)areaNode.get("mapInfo")).put("x", area.getMapInfo().getX());
        ((ObjectNode)areaNode.get("mapInfo")).put("y", area.getMapInfo().getY());
        ((ObjectNode)areaNode.get("mapInfo")).put("z", area.getMapInfo().getZ());
        ((ObjectNode)areaNode.get("mapInfo")).put("icon", area.getMapInfo().getIcon());
        String serializedEntity ;
        try {
            serializedEntity = mapper.writeValueAsString(areaNode);
        } catch (Exception exc){
            //Serialization error.
            throw new RuntimeException();
        }
        return serializedEntity;
    }

    private List<String> areaDeviceExpectedPropertiesWithJsonViewCompact(){
        List<String> areaDeviceExpectedPropertiesWithJsonViewCompact = new ArrayList<>();
        areaDeviceExpectedPropertiesWithJsonViewCompact.add("id");
        areaDeviceExpectedPropertiesWithJsonViewCompact.add("entityCreateDate");
        areaDeviceExpectedPropertiesWithJsonViewCompact.add("entityModifyDate");
        areaDeviceExpectedPropertiesWithJsonViewCompact.add("area");
        areaDeviceExpectedPropertiesWithJsonViewCompact.add("mapInfo");
        areaDeviceExpectedPropertiesWithJsonViewCompact.add("device");
        return areaDeviceExpectedPropertiesWithJsonViewCompact;
    }

    private List<String> hDeviceWithJsonViewCompact(){
        List<String> hDeviceWithJsonViewCompact = new ArrayList<>();
        hDeviceWithJsonViewCompact.add("id");
        hDeviceWithJsonViewCompact.add("entityCreateDate");
        hDeviceWithJsonViewCompact.add("entityModifyDate");
        hDeviceWithJsonViewCompact.add("deviceName");
        hDeviceWithJsonViewCompact.add("brand");
        hDeviceWithJsonViewCompact.add("model");
        hDeviceWithJsonViewCompact.add("firmwareVersion");
        hDeviceWithJsonViewCompact.add("softwareVersion");
        hDeviceWithJsonViewCompact.add("description");
        hDeviceWithJsonViewCompact.add("project");
        hDeviceWithJsonViewCompact.add("roles");
        return hDeviceWithJsonViewCompact;
    }

    private List<String> hProjectWithHyperIoTInnerEntitySerializerExpectedProperties(){
        List<String> hProjectWithHyperIoTInnerEntitySerializerExpectedProperties = new ArrayList<>();
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("id");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("entityCreateDate");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("entityModifyDate");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("name");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("description");
        return hProjectWithHyperIoTInnerEntitySerializerExpectedProperties;
    }

    private List<String> areaDeviceExpectedPropertiesWithJsonViewPublic(){
        List<String> areaDeviceExpectedPropertiesWithJsonViewPublic = new ArrayList<>();
        areaDeviceExpectedPropertiesWithJsonViewPublic.addAll(hyperIoTAbstractEntityProperties());
        areaDeviceExpectedPropertiesWithJsonViewPublic.add("area");
        areaDeviceExpectedPropertiesWithJsonViewPublic.add("mapInfo");
        areaDeviceExpectedPropertiesWithJsonViewPublic.add("device");
        return areaDeviceExpectedPropertiesWithJsonViewPublic;
    }

    private List<String> hDeviceWithJsonViewPublic(){
        List<String> hDeviceWithJsonViewPublic = new ArrayList<>();
        hDeviceWithJsonViewPublic.addAll(hyperIoTAbstractEntityProperties());
        hDeviceWithJsonViewPublic.add("deviceName");
        hDeviceWithJsonViewPublic.add("brand");
        hDeviceWithJsonViewPublic.add("model");
        hDeviceWithJsonViewPublic.add("firmwareVersion");
        hDeviceWithJsonViewPublic.add("softwareVersion");
        hDeviceWithJsonViewPublic.add("description");
        hDeviceWithJsonViewPublic.add("project");
        hDeviceWithJsonViewPublic.add("loginWithSSLCert");
        hDeviceWithJsonViewPublic.add("x509Cert");
        hDeviceWithJsonViewPublic.add("x509CertKey");
        hDeviceWithJsonViewPublic.add("roles");
        return hDeviceWithJsonViewPublic;
    }

    private List<String> areaExpectedPropertiesWithJsonViewCompact(){
        //Look on AreaRestApi method annotated with @JsonView(HyperIoTJSONView.Compact.class)
        List<String> areaPaginatedExpectedProperties = new ArrayList<>();
        areaPaginatedExpectedProperties.add("id");
        areaPaginatedExpectedProperties.add("entityCreateDate");
        areaPaginatedExpectedProperties.add("entityModifyDate");
        areaPaginatedExpectedProperties.add("description");
        areaPaginatedExpectedProperties.add("areaViewType");
        areaPaginatedExpectedProperties.add("areaConfiguration");
        areaPaginatedExpectedProperties.add("name");
        areaPaginatedExpectedProperties.add("mapInfo");
        return areaPaginatedExpectedProperties;
    }

    private List<String> areaExpectedPropertiesWithJsonViewExtended(){
        //Look on AreaRestApi method annotated with @JsonView(HyperIoTJSONView.Extended.class)
        List<String> areaExpectedProperties = new ArrayList<>();
        areaExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        areaExpectedProperties.add("description");
        areaExpectedProperties.add("areaViewType");
        areaExpectedProperties.add("areaConfiguration");
        areaExpectedProperties.add("name");
        areaExpectedProperties.add("mapInfo");
        areaExpectedProperties.add("innerArea");
        return areaExpectedProperties;
    }

    private List<String> areaExpectedPropertiesWithJsonViewPublic(){
        //Look on AreaRestApi method annotated with @JsonView(HyperIoTJSONView.Public.class)
        List<String> areaExpectedProperties = new ArrayList<>();
        areaExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        areaExpectedProperties.add("description");
        areaExpectedProperties.add("areaViewType");
        areaExpectedProperties.add("areaConfiguration");
        areaExpectedProperties.add("name");
        areaExpectedProperties.add("mapInfo");
        return areaExpectedProperties;
    }

    private List<String> areaMapInfoExpectedProperties(){
        List<String> areaMapInfoExpectedProperties = new ArrayList<>();
        areaMapInfoExpectedProperties.add("x");
        areaMapInfoExpectedProperties.add("y");
        areaMapInfoExpectedProperties.add("z");
        areaMapInfoExpectedProperties.add("icon");
        return areaMapInfoExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
    }

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

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Property of: " + hproject.getUser().getUsername());
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals("Brand",
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals("Property of: " + hproject.getUser().getUsername(),
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals("model",
                ((HDevice) restResponse.getEntity()).getModel());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getSoftwareVersion());
        Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
        Assert.assertEquals(hproject.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
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
        AreaMapInfo areaMapInfo = new AreaMapInfo();
        areaMapInfo.setZ(5.5);
        areaMapInfo.setY(5.5);
        areaMapInfo.setX(5.5);
        areaMapInfo.setIcon("");
        area.setMapInfo(areaMapInfo);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals("Description", ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(areaMapInfo.getX(),  ((Area) restResponseArea.getEntity()).getMapInfo().getX());
        Assert.assertEquals(areaMapInfo.getY(), ((Area) restResponseArea.getEntity()).getMapInfo().getY());
        Assert.assertEquals(areaMapInfo.getZ(), ((Area) restResponseArea.getEntity()).getMapInfo().getZ());
        Assert.assertEquals(areaMapInfo.getIcon(), ((Area) restResponseArea.getEntity()).getMapInfo().getIcon());
        return area;
    }

    private Area createInnerArea (HProject hproject, Area parentArea){
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertEquals("Project of user: " + adminUser.getUsername(), hproject.getDescription());
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        AreaMapInfo areaMapInfo = new AreaMapInfo();
        areaMapInfo.setZ(5.5);
        areaMapInfo.setY(5.5);
        areaMapInfo.setX(5.5);
        areaMapInfo.setIcon("");
        area.setMapInfo(areaMapInfo);
        area.setParentArea(parentArea);
        this.impersonateUser(areaRestApi, adminUser);
        Response restResponseArea = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponseArea.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
        Assert.assertEquals("Description", ((Area) restResponseArea.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((Area) restResponseArea.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(areaMapInfo.getX(),  ((Area) restResponseArea.getEntity()).getMapInfo().getX());
        Assert.assertEquals(areaMapInfo.getY(), ((Area) restResponseArea.getEntity()).getMapInfo().getY());
        Assert.assertEquals(areaMapInfo.getZ(), ((Area) restResponseArea.getEntity()).getMapInfo().getZ());
        Assert.assertEquals(areaMapInfo.getIcon(), ((Area) restResponseArea.getEntity()).getMapInfo().getIcon());
        return area;
    }

    private AreaDevice createAreaDevice(Area area, HDevice hdevice) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(areaRestApi, adminUser);
        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(adminUser.getId(), ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        return areaDevice;
    }

    @After
    public void afterTest() {
        AreaDeviceRepository areaDeviceRepository = getOsgiService(AreaDeviceRepository.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HPacketFieldSystemApi hPacketFieldSystemApi = getOsgiService(HPacketFieldSystemApi.class);
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        HyperIoTTestUtils.truncateTables(areaDeviceRepository, null);
        HyperIoTTestUtils.truncateTables(areaSystemApi, null);
        HyperIoTTestUtils.truncateTables(hPacketFieldSystemApi,null);
        HyperIoTTestUtils.truncateTables(hPacketSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
    }

}

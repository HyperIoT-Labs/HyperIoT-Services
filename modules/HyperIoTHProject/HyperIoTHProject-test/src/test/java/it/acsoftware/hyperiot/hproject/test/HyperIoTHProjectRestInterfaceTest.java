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

package it.acsoftware.hyperiot.hproject.test;

import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.http.*;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidator;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidatorBuilder;
import it.acsoftware.hyperiot.base.test.util.HyperIoTTestUtils;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
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

import static it.acsoftware.hyperiot.hproject.test.HyperIoTHProjectConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHProjectRestInterfaceTest extends KarafTestSupport {

    /*
        TODO
            To add test method relative to clearHadoopData service you need to enforce HBase's table creation relative to the HProject.
            Add test relative to HProject's registration with challenge.
            Add test relative to HProject's timeline service.
     */

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
    }

    @SuppressWarnings("unused")
    private HyperIoTAction getHyperIoTAction(String resourceName, HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder.createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
    }
    /*
    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

     */

    @Before
    public void impersonateAsHyperIoTAdmin(){
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(hProjectRestApi, admin);
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
    public void test001_saveHProjectShouldSerializeResponseCorrectly()  {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Project of user: " + adminUser.getUsername());
        hproject.setUser((HUser) adminUser);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojects"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(hproject)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedHProjectProperties = hprojectExpectedProperties();
        expectedHProjectProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedHProjectProperties)
                .containExactInnerProperties("user", huserReferenceInsideHProjectEntityExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findHProjectShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojects/").concat(String.valueOf(project.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedHProjectProperties = hprojectExpectedProperties();
        expectedHProjectProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedHProjectProperties)
                .containExactInnerProperties("user", huserReferenceInsideHProjectEntityExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateHProjectShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        project.setDescription("New description in data : "+ new Date());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojects"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(project)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedHProjectProperties = hprojectExpectedProperties();
        expectedHProjectProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedHProjectProperties)
                .containExactInnerProperties("user", huserReferenceInsideHProjectEntityExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        if(!testSuccessful){
            System.out.println("Current Response: "+ response.getResponseBody());
            System.out.println("Expected Response: "+ expectedHProjectProperties);
        }
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteHProjectShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojects/").concat(String.valueOf(project.getId())))
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
    public void test005_findAllHProjectShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HProject project2 = createHProject();
        Assert.assertNotEquals(0, project2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojects/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedHProjectProperties = hprojectExpectedProperties();
        expectedHProjectProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedHProjectProperties)
                .containExactInnerProperties("user", huserReferenceInsideHProjectEntityExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllHProjectPaginatedShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HProject project2 = createHProject();
        Assert.assertNotEquals(0 , project2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojects"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("delta",String.valueOf(defaultDelta))
                .withParameter("page",String.valueOf(defaultPage))
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedHProjectProperties = hprojectExpectedProperties();
        expectedHProjectProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results",expectedHProjectProperties)
                .containExactInnerProperties("results.user",huserReferenceInsideHProjectEntityExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_cardsViewShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hprojects/all/cards") ;
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
                .containExactProperties(hProjectsCardsViewExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_getHProjectTreeViewShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device  = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hprojects/").concat(String.valueOf(project.getId())).concat("/tree");
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
                .containExactProperties(hPacketExpectedProperties())
                .containExactInnerProperties("device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("fields", hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test009_getHProjectAreaListShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Area area = createArea(project);
        Assert.assertNotEquals(0, area.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hprojects/").concat(String.valueOf(project.getId())).concat("/areas");
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
                .containExactProperties(areaExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test010_updateHProjectOwnerShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HUser user = createHUser();
        Assert.assertNotEquals(0, user.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hprojects/").concat(String.valueOf(project.getId()))
                .concat("/owner/").concat(String.valueOf(user.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedHProjectProperties = hprojectExpectedProperties();
        expectedHProjectProperties.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedHProjectProperties)
                .containExactInnerProperties("user", huserReferenceInsideHProjectEntityExpectedProperties())
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
    *
     */

    private String generateRandomString(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    private List<String> areaExpectedProperties(){
        List<String> areaExpectedProperties = new ArrayList<>();
        areaExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        areaExpectedProperties.add("name");
        areaExpectedProperties.add("description");
        areaExpectedProperties.add("areaViewType");
        areaExpectedProperties.add("areaConfiguration");
        areaExpectedProperties.add("mapInfo");
        return areaExpectedProperties;

    }

    private List<String> hPacketExpectedProperties(){
        List<String> hPacketExpectedProperties = new ArrayList<>();
        hPacketExpectedProperties.add("id");
        hPacketExpectedProperties.add("entityCreateDate");
        hPacketExpectedProperties.add("entityModifyDate");
        hPacketExpectedProperties.add("entityVersion");
        hPacketExpectedProperties.add("name");
        hPacketExpectedProperties.add("type");
        hPacketExpectedProperties.add("format");
        hPacketExpectedProperties.add("serialization");
        hPacketExpectedProperties.add("version");
        hPacketExpectedProperties.add("valid");
        hPacketExpectedProperties.add("timestampField");
        hPacketExpectedProperties.add("timestampFormat");
        hPacketExpectedProperties.add("unixTimestamp");
        hPacketExpectedProperties.add("unixTimestampFormatSeconds");
        hPacketExpectedProperties.add("trafficPlan");
        hPacketExpectedProperties.add("device");
        hPacketExpectedProperties.add("fields");
        return hPacketExpectedProperties;
    }

    private List<String> hDeviceReferenceInsideHPacketEntityExpectedProperties(){
        List<String> hDeviceReferenceInsideHPacketEntityExpectedProperties = new ArrayList<>();
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("id");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("entityCreateDate");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("entityModifyDate");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("deviceName");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("brand");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("model");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("firmwareVersion");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("softwareVersion");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("description");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("project");
        hDeviceReferenceInsideHPacketEntityExpectedProperties.add("roles");
        return hDeviceReferenceInsideHPacketEntityExpectedProperties;
    }

    private List<String> hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties(){
        List<String> hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties = new ArrayList<>();
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("id");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("entityVersion");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("entityCreateDate");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("entityModifyDate");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("name");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("description");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("type");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("multiplicity");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("unit");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("value");
        hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties.add("innerFields");
        return hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties;
    }

    private List<String> hProjectReferenceInsideHDeviceExpectedProperties(){
        List<String> hProjectReferenceInsideHDeviceExpectedProperties = new ArrayList<>();
        hProjectReferenceInsideHDeviceExpectedProperties.add("id");
        hProjectReferenceInsideHDeviceExpectedProperties.add("entityCreateDate");
        hProjectReferenceInsideHDeviceExpectedProperties.add("entityModifyDate");
        hProjectReferenceInsideHDeviceExpectedProperties.add("name");
        hProjectReferenceInsideHDeviceExpectedProperties.add("description");
        return hProjectReferenceInsideHDeviceExpectedProperties;
    }

    private List<String> hProjectsCardsViewExpectedProperties(){
        List<String> hProjectsCardsViewExpectedProperties = new ArrayList<>();
        hProjectsCardsViewExpectedProperties.add("id");
        hProjectsCardsViewExpectedProperties.add("entityCreateDate");
        hProjectsCardsViewExpectedProperties.add("entityModifyDate");
        hProjectsCardsViewExpectedProperties.add("name");
        hProjectsCardsViewExpectedProperties.add("description");
        hProjectsCardsViewExpectedProperties.add("deviceCount");
        hProjectsCardsViewExpectedProperties.add("statisticsCount");
        hProjectsCardsViewExpectedProperties.add("rulesCount");
        hProjectsCardsViewExpectedProperties.add("hProjectSharingInfo");
        return hProjectsCardsViewExpectedProperties;
    }

    private List<String> hprojectExpectedProperties(){
        List<String> hprojectExpectedProperties = new ArrayList<>();
        hprojectExpectedProperties.add("name");
        hprojectExpectedProperties.add("description");
        hprojectExpectedProperties.add("user");
        return hprojectExpectedProperties;
    }

    private List<String> huserReferenceInsideHProjectEntityExpectedProperties(){
        List<String> huserInsideHProjectEntityExpectedProperties = new ArrayList<>();
        huserInsideHProjectEntityExpectedProperties.add("id");
        huserInsideHProjectEntityExpectedProperties.add("entityCreateDate");
        huserInsideHProjectEntityExpectedProperties.add("entityModifyDate");
        huserInsideHProjectEntityExpectedProperties.add("admin");
        huserInsideHProjectEntityExpectedProperties.add("imagePath");
        return huserInsideHProjectEntityExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
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

    private HPacket createHPacket(HDevice hdevice) {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, adminUser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        return hpacket;
    }

    public HPacketField createHPacketField(HPacket hpacket){
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HPacketField field1 = new HPacketField();
        field1.setPacket(hpacket);
        field1.setName("temperature" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(24.0);
        this.impersonateUser(hPacketRestApi, adminUser);
        Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
        Assert.assertEquals(200, responseAddField1.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
        Assert.assertEquals(hpacket.getDevice().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hpacket.getDevice().getProject().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());
        return (HPacketField) responseAddField1.getEntity();
    }

    private HUser createHUser() {
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
        return huser;
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

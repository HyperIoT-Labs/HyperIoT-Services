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

package it.acsoftware.hyperiot.hdevice.test;

import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.http.*;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidator;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidatorBuilder;
import it.acsoftware.hyperiot.base.test.util.HyperIoTTestUtils;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.hdevice.test.HyperIoTHDeviceConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHDeviceRestInterfaceTest extends KarafTestSupport {

    //force global config
    @Override
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
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(hDeviceRestApi, admin);
    }

    @Test
    public void test000_hyperIoTFrameworkShouldBeInstalled() {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class, 0);
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
    public void test001_saveHDeviceShouldSerializeResponseCorrectly()  {
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Property of: " + project.getUser().getUsername());
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(project);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(hdevice)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hDeviceExpectedProperties())
                .containExactInnerProperties("project",hProjectReferenceInsideHDeviceExpectedProperties())
                //device's role has empty properties, because no role is associated to the device.
                .containExactInnerProperties("roles",new ArrayList<>())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findHDeviceShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices/").concat(String.valueOf(device.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hDeviceExpectedProperties())
                .containExactInnerProperties("project",hProjectReferenceInsideHDeviceExpectedProperties())
                //device's role has empty properties, because no role is associated to the device.
                .containExactInnerProperties("roles",new ArrayList<>())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateHDeviceShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        device.setDescription("New description in data : "+ new Date());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(device)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hDeviceExpectedProperties())
                .containExactInnerProperties("project",hProjectReferenceInsideHDeviceExpectedProperties())
                //device's role has empty properties, because no role is associated to the device.
                .containExactInnerProperties("roles",new ArrayList<>())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_updateHDevicePasswordShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        String deviceOldPassword = "passwordPass&01";
        Assert.assertTrue(HyperIoTUtil.passwordMatches(deviceOldPassword, device.getPassword()));
        String deviceNewPassword = deviceOldPassword.concat("New");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices").concat("/password"))
                .withAuthorizationAsHyperIoTAdmin()
                .withParameter("deviceId",String.valueOf(device.getId()))
                .withParameter("oldPassword","passwordPass&01")
                .withParameter("newPassword",deviceNewPassword)
                .withParameter("passwordConfirm",deviceNewPassword)
                .withContentTypeHeader("application/x-www-form-urlencoded")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hDeviceExpectedProperties())
                .containExactInnerProperties("project",hProjectReferenceInsideHDeviceExpectedProperties())
                //device's role has empty properties, because no role is associated to the device.
                .containExactInnerProperties("roles",new ArrayList<>())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test005_deleteHDeviceShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices/").concat(String.valueOf(device.getId())))
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
    public void test006_findAllHDeviceShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HDevice device2 = createHDevice(project);
        Assert.assertNotEquals(0, device2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hDeviceExpectedProperties())
                .containExactInnerProperties("project",hProjectReferenceInsideHDeviceExpectedProperties())
                //device's role has empty properties, because no role is associated to the device.
                .containExactInnerProperties("roles",new ArrayList<>())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findAllHDevicePaginatedShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HDevice device2 = createHDevice(project);
        Assert.assertNotEquals(0, device2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices"))
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
                .containExactInnerProperties("results",hDeviceExpectedProperties())
                .containExactInnerProperties("results.project",hProjectReferenceInsideHDeviceExpectedProperties())
                //device's role has empty properties, because no role is associated to the device.
                .containExactInnerProperties("results.roles",new ArrayList<>())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_resetPasswordRequestShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices/")
                .concat(String.valueOf(device.getId()).concat("/resetPasswordRequest"));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(requestUri)
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
    public void test009_resetPasswordShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        //Set password reset code directly from HDeviceRepository such that we know the password reset code (Raw password reset code is sent to user through email).
        HDeviceRepository hDeviceRepository = this.getOsgiService(HDeviceRepository.class);
        String passwordResetCode = UUID.randomUUID().toString();
        device.setPasswordResetCode(HyperIoTUtil.getPasswordHash(passwordResetCode));
        hDeviceRepository.update(device);
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hdevices/resetPassword");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withParameter("hdeviceId",String.valueOf(device.getId()))
                .withParameter("passwordResetCode",passwordResetCode)
                .withParameter("newPassword", "Password123!")
                .withParameter("passwordConfirm", "Password123!")
                .withContentTypeHeader("application/x-www-form-urlencoded")
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

    private List<String> hDeviceExpectedProperties(){
        List<String> hDeviceExpectedProperties = new ArrayList<>();
        hDeviceExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        hDeviceExpectedProperties.add("deviceName");
        hDeviceExpectedProperties.add("description");
        hDeviceExpectedProperties.add("brand");
        hDeviceExpectedProperties.add("model");
        hDeviceExpectedProperties.add("firmwareVersion");
        hDeviceExpectedProperties.add("softwareVersion");
        hDeviceExpectedProperties.add("loginWithSSLCert");
        hDeviceExpectedProperties.add("x509Cert");
        hDeviceExpectedProperties.add("x509CertKey");
        hDeviceExpectedProperties.add("project");
        hDeviceExpectedProperties.add("roles");
        return hDeviceExpectedProperties;
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

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

package it.acsoftware.hyperiot.hpacket.test;

import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
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
import it.acsoftware.hyperiot.hproject.algorithm.service.rest.HProjectAlgorithmRestApi;
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
import java.util.Date;
import java.util.List;

import static it.acsoftware.hyperiot.hpacket.test.HyperIoTHPacketConfiguration.defaultDelta;
import static it.acsoftware.hyperiot.hpacket.test.HyperIoTHPacketConfiguration.defaultPage;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHPacketRestInterfaceTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
    }

    @Before
    public void impersonateAsHyperIoTAdmin(){
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(hPacketRestApi, admin);
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
    public void test001_saveHPacketShouldSerializeResponseCorrectly()  {
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(device);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hpackets"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(hpacket)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hPacketExpectedProperties())
                .containExactInnerProperties("device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
                //Assert that fields property is present
                //Assert that field property has not property related to it, because HPacket hasn's HPacket field associated.
                .containExactInnerProperties("fields", new ArrayList<>())
                .containExactInnerProperties("device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findHPacketShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hpackets/").concat(String.valueOf(packet.getId())))
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
    public void test003_updateHPacketShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        /*
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());

         */
        packet.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hpackets"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(packet)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactInnerProperties("device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
               // .containExactInnerProperties("fields", hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteHPacketsShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hpackets/").concat(String.valueOf(packet.getId())))
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
    public void test005_findAllHPacketsShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        HPacket packet2 = createHPacket(device);
        Assert.assertNotEquals(0, packet2.getId());
        HPacketField packetField2 = createHPacketField(packet2);
        Assert.assertNotEquals(0, packetField2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hpackets/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactInnerProperties("device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("fields", hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllHPacketsByProjectIdShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        HPacket packet2 = createHPacket(device);
        Assert.assertNotEquals(0, packet2.getId());
        HPacketField packetField2 = createHPacketField(packet2);
        Assert.assertNotEquals(0, packetField2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hpackets/all/").concat(String.valueOf(project.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactInnerProperties("device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("fields", hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findAllHPacketsByProjectIdAndTypeShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        HPacket packet2 = createHPacket(device);
        Assert.assertNotEquals(0, packet2.getId());
        HPacketField packetField2 = createHPacketField(packet2);
        Assert.assertNotEquals(0, packetField2.getId());
        Assert.assertEquals(packet.getType(), packet2.getType());
        String packetType = packet.getType().getName();
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hpackets/all/").concat(String.valueOf(project.getId())).concat("/types");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("types",packetType)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactInnerProperties("device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("fields", hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_findAllHPacketsPaginatedPaginatedShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        HPacket packet2 = createHPacket(device);
        Assert.assertNotEquals(0, packet2.getId());
        HPacketField packetField2 = createHPacketField(packet2);
        Assert.assertNotEquals(0, packetField2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hpackets"))
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
                .containExactInnerProperties("results.device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("results.fields", hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("results.device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test009_getHDevicePacketListShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        HPacket packet2 = createHPacket(device);
        Assert.assertNotEquals(0, packet2.getId());
        HPacketField packetField2 = createHPacketField(packet2);
        Assert.assertNotEquals(0, packetField2.getId());
        Assert.assertEquals(packet.getType(), packet2.getType());
        String packetType = packet.getType().getName();
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hpackets/devices/").concat(String.valueOf(device.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("types",packetType)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactInnerProperties("device", hDeviceReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("fields", hPacketsFieldsReferenceInsideHPacketEntityExpectedProperties())
                .containExactInnerProperties("device.project", hProjectReferenceInsideHDeviceExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test010_addHPacketFieldShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = new HPacketField();
        packetField.setPacket(packet);
        packetField.setName("temperature" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        packetField.setDescription("Temperature");
        packetField.setType(HPacketFieldType.DOUBLE);
        packetField.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        packetField.setValue(24.0);
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hpackets/").concat(String.valueOf(packet.getId())).concat("/fields");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(packetField)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hPacketFieldExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test011_deleteHPacketFieldShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hpackets/fields/").concat(String.valueOf(packetField.getId()));
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

    @Test
    public void test012_updateHPacketFieldShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        packetField.setName("newName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hpackets/").concat(String.valueOf(packet.getId())).concat("/fields");
        //String serializedPacketField = serializeHPacketFieldForRequest(packetField);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(packetField)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hPacketFieldExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test013_findTreeFieldsShouldSerializeResponseCorrecly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        HPacketField packetField2 = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField2.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hpackets/treefields/").concat(String.valueOf(packet.getId()));
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
                .containExactProperties(hPacketFieldExpectedProperties())
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

    private List<String> hPacketExpectedProperties(){
        List<String> hPacketExpectedProperties = new ArrayList<>();
        hPacketExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
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

    private List<String> hPacketFieldExpectedProperties(){
        List<String> hPacketFieldExpectedProperties = new ArrayList<>();
        hPacketFieldExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        hPacketFieldExpectedProperties.add("name");
        hPacketFieldExpectedProperties.add("description");
        hPacketFieldExpectedProperties.add("type");
        hPacketFieldExpectedProperties.add("multiplicity");
        hPacketFieldExpectedProperties.add("unit");
        hPacketFieldExpectedProperties.add("innerFields");
        hPacketFieldExpectedProperties.add("value");
        return hPacketFieldExpectedProperties;
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

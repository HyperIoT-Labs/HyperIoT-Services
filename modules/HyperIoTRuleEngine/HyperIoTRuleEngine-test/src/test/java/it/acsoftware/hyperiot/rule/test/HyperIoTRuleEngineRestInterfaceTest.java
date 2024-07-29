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

package it.acsoftware.hyperiot.rule.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
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
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.service.rest.RuleEngineRestApi;
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
import static it.acsoftware.hyperiot.rule.test.HyperIoTRuleEngineConfiguration.*;
/**
 * @author Francesco Salerno
 *
 * This is a test class relative to Interface's test of RuleEngineRestApi
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRuleEngineRestInterfaceTest extends KarafTestSupport {

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

    @Before
    public void impersonateAsHyperIoTAdmin(){
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(ruleEngineRestApi, admin);
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
    public void test001_saveRuleShouldSerializeResponseCorrectly()  {
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket hpacket = createHPacket(device);
        Assert.assertNotEquals(0, hpacket.getId());
        Rule rule = new Rule();
        rule.setName("Add category rule 1" + java.util.UUID.randomUUID().toString().replaceAll("-",""));
        rule.setDescription("Rule defined by huser: " + project.getUser().getUsername());
        rule.setType(RuleType.EVENT);
        rule.setRuleDefinition("temperature >= 23 AND humidity > 36");
        rule.setProject(project);
        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        rule.setActions(actions);
        String serializedRuleForRequest = serializeRuleForRequest(rule);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedRuleForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("actions", ruleEntityActionsFieldWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet",hpacketEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet.device", hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("packet.device.project",hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project.user",hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findRuleShouldSerializeResponseCorrectly(){
        Rule rule = createRuleForTest();
        Assert.assertNotEquals(0, rule.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules/").concat(String.valueOf(rule.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("actions", ruleEntityActionsFieldWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet",hpacketEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet.device", hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("packet.device.project",hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project.user",hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateRuleShouldSerializeResponseCorrectly(){
        Rule rule = createRuleForTest();
        Assert.assertNotEquals(0, rule.getId());
        rule.setDescription("Rule defined in date : "+ new Date());
        String serializedRuleForRequest = serializeRuleForRequest(rule);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedRuleForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("actions", ruleEntityActionsFieldWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet",hpacketEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet.device", hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("packet.device.project",hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project.user",hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteRuleShouldSerializeResponseCorrectly(){
        Rule rule = createRuleForTest();
        Assert.assertNotEquals(0, rule.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules/").concat(String.valueOf(rule.getId())))
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
    public void test005_findAllRulesShouldSerializeResponseCorrecly(){
        Rule rule = createRuleForTest();
        Assert.assertNotEquals(0, rule.getId());
        Rule rule2 = createRuleForTest();
        Assert.assertNotEquals(0, rule2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("actions", ruleEntityActionsFieldWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet",hpacketEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet.device", hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("packet.device.project",hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project.user",hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllRulesPaginatedShouldSerializeResponseCorrecly(){
        Rule rule = createRuleForTest();
        Assert.assertNotEquals(0, rule.getId());
        Rule rule2 = createRuleForTest();
        Assert.assertNotEquals(0, rule2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules"))
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
                .containExactInnerProperties("results",ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.actions", ruleEntityActionsFieldWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.packet",hpacketEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.packet.device", hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("results.packet.device.project",hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("results.project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.project.user",hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findAllRuleByProjectIdShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        Rule rule = createRule(project, packet);
        Assert.assertNotEquals(0, rule.getId());
        Rule rule2 = createRule(project, packet);
        Assert.assertNotEquals(0, rule2.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules").concat("/byproject/").concat(String.valueOf(project.getId()));
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
                .containExactProperties(ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("actions", ruleEntityActionsFieldWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet",hpacketEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet.device", hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("packet.device.project",hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project.user",hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_findAllRuleByPacketIdShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        Rule rule = createRule(project, packet);
        Assert.assertNotEquals(0, rule.getId());
        Rule rule2 = createRule(project, packet);
        Assert.assertNotEquals(0, rule2.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules").concat("/bypacket/").concat(String.valueOf(packet.getId()));
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
                .containExactProperties(ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("actions", ruleEntityActionsFieldWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet",hpacketEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("packet.device", hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("packet.device.project",hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .containExactInnerProperties("project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project.user",hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test009_findAllRuleActionShouldSerializeResponseCorrectly(){
        Rule rule = createRuleForTest();
        Assert.assertNotEquals(0, rule.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/rules").concat("/actions"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("type", "EVENT")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                //Assert that the RuleAction returned contain at least all fields declared in RuleAction(it.acsoftware.hyperiot.rule.model.actions) Abstract's class.
                .containProperties(ruleActionAbstractEntityWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    /*
    *
    *
    *
    *  Utility method for test.
    *
    *
    *
     */

    private List<String> ruleActionAbstractEntityWithJsonViewPublicExpectedProperties(){
        List<String> ruleActionAbstractEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        ruleActionAbstractEntityWithJsonViewPublicExpectedProperties.add("actionName");
        ruleActionAbstractEntityWithJsonViewPublicExpectedProperties.add("ruleId");
        ruleActionAbstractEntityWithJsonViewPublicExpectedProperties.add("ruleName");
        ruleActionAbstractEntityWithJsonViewPublicExpectedProperties.add("tags");
        ruleActionAbstractEntityWithJsonViewPublicExpectedProperties.add("active");
        ruleActionAbstractEntityWithJsonViewPublicExpectedProperties.add("bundleContext");
        return ruleActionAbstractEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> ruleEntityActionsFieldWithJsonViewPublicExpectedProperties(){
        List<String> ruleEntityActionsFieldWithJsonViewPublicExpectedProperties = new ArrayList<>();
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("actionName");
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("ruleId");
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("ruleName");
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("tags");
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("active");
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("bundleContext");
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("categoryIds");
        ruleEntityActionsFieldWithJsonViewPublicExpectedProperties.add("ruleType");
        return ruleEntityActionsFieldWithJsonViewPublicExpectedProperties;
    }

    private List<String> hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty(){
        List<String> hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty = new ArrayList<>();
        hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty.add("id");
        hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty.add("entityCreateDate");
        hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty.add("entityModifyDate");
        hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty.add("admin");
        hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty.add("imagePath");
        return hUserEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty;
    }

    private List<String> hpacketEntityWithJsonViewPublicExpectedProperties(){
        List<String> hpacketEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hpacketEntityWithJsonViewPublicExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        hpacketEntityWithJsonViewPublicExpectedProperties.add("name");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("type");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("format");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("serialization");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("device");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("version");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("fields");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("valid");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("timestampField");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("timestampFormat");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("unixTimestamp");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("unixTimestampFormatSeconds");
        hpacketEntityWithJsonViewPublicExpectedProperties.add("trafficPlan");
        return hpacketEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> hdeviceEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty(){
        List<String> hdeviceEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("id");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("entityCreateDate");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("entityModifyDate");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("deviceName");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("brand");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("model");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("firmwareVersion");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("softwareVersion");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("description");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("project");
        hdeviceEntityWithJsonViewPublicExpectedProperties.add("roles");
        return hdeviceEntityWithJsonViewPublicExpectedProperties;
    }


    private List<String> hprojectEntityWithJsonViewPublicExpectedProperties(){
        List<String> hprojectEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hprojectEntityWithJsonViewPublicExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        hprojectEntityWithJsonViewPublicExpectedProperties.add("name");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("description");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("user");
        return hprojectEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> hprojectEntitySerializedWithHyperIoTInnerEntityJSONSerializerExpectedProperty(){
        List<String> hprojectEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hprojectEntityWithJsonViewPublicExpectedProperties.add("id");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("entityCreateDate");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("entityModifyDate");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("name");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("description");
        return hprojectEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> ruleEntityWithJsonViewPublicExpectedProperties(){
        List<String> ruleEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        ruleEntityWithJsonViewPublicExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        ruleEntityWithJsonViewPublicExpectedProperties.add("name");
        ruleEntityWithJsonViewPublicExpectedProperties.add("description");
        ruleEntityWithJsonViewPublicExpectedProperties.add("ruleDefinition");
        ruleEntityWithJsonViewPublicExpectedProperties.add("project");
        ruleEntityWithJsonViewPublicExpectedProperties.add("jsonActions");
        ruleEntityWithJsonViewPublicExpectedProperties.add("actions");
        ruleEntityWithJsonViewPublicExpectedProperties.add("type");
        ruleEntityWithJsonViewPublicExpectedProperties.add("packet");
        ruleEntityWithJsonViewPublicExpectedProperties.add("parent");
        return ruleEntityWithJsonViewPublicExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
    }

    private String serializeRuleForRequest(Rule rule){
        //We must serialize the Rule entity in this manner to bypass jackson's serialization.
        //If we use the jackson's serialization , the framework look on entity's property and serialize according to them.
        //So this causes the inclusion of the field actions and rule in the serialization of Rule Entity.
        //This field actions cannot be deserialized by jackson because fields's type is abstract (so Jackson cannot instantiate it).
        //This field rule cannot be deserialized by jackson because fields's type is abstract (so Jackson cannot instantiate it).
        //The  field parent cannot be deserialized by jackson because the field doesn't respect the "property bean" convention
        //(jackson works with the suffix of the get/set method during serialization/deserialization property)
        try {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode ruleJsonNode = mapper.readTree(mapper.writeValueAsString(rule));
            Assert.assertTrue(ruleJsonNode.isObject());
            ObjectNode ruleObjectNode = (ObjectNode) ruleJsonNode;
            if(ruleObjectNode.has("actions")){
                ruleObjectNode.remove("actions");
            }
            if(ruleObjectNode.has("rule")){
                ruleObjectNode.remove("rule");
            }
            if(ruleObjectNode.has("parent")){
                ruleObjectNode.remove("parent");
            }
            Assert.assertFalse(ruleObjectNode.has("actions"));
            Assert.assertFalse(ruleObjectNode.has("rule"));
            Assert.assertFalse(ruleObjectNode.has("parent"));
            return mapper.writeValueAsString(ruleObjectNode);
        } catch (Exception e){
            //Serialization error.
            throw new RuntimeException();
        }
    }

    private String createJsonActionsForRule(List<RuleAction> actions, Rule rule){
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<String> stringActionsList = new ArrayList<>();
        actions.forEach(r -> {
            r.setRuleId(rule.getId());
            try {
                stringActionsList.add(objectMapper.writeValueAsString(r));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        try {
            return objectMapper.writeValueAsString(stringActionsList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Assert.fail();
            throw new HyperIoTRuntimeException();
        }
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

    private Rule createRule(HProject hproject, HPacket hpacket) {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Rule rule = new Rule();
        rule.setName("Add category rule 1" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + hproject.getUser().getUsername());
        rule.setType(RuleType.EVENT);
        rule.setRuleDefinition("temperature >= 23 AND humidity > 36");
        rule.setProject(hproject);
        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        rule.setActions(actions);
        this.impersonateUser(ruleEngineRestApi, adminUser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getId());
        Assert.assertEquals(rule.getName(), ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getName());
        Assert.assertEquals("Rule defined by huser: " + hproject.getUser().getUsername(), ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals(RuleType.EVENT, ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getType());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events", ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("temperature >= 23 AND humidity > 36", ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals(hproject.getId(), ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(adminUser.getId(), ((it.acsoftware.hyperiot.rule.model.Rule) restResponse.getEntity()).getProject().getUser().getId());
        return rule;
    }

    private Rule createRuleForTest(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField packetField = createHPacketField(packet);
        Assert.assertNotEquals(0, packetField.getId());
        Rule rule = createRule(project, packet);
        Assert.assertNotEquals(0, rule.getId());
        return rule;
    }

    @After
    public void afterTest() {
        RuleEngineSystemApi ruleEngineSystemApi = getOsgiService(RuleEngineSystemApi.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HPacketFieldSystemApi hPacketFieldSystemApi = getOsgiService(HPacketFieldSystemApi.class);
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        HyperIoTTestUtils.truncateTables(ruleEngineSystemApi, null);
        HyperIoTTestUtils.truncateTables(areaSystemApi, null);
        HyperIoTTestUtils.truncateTables(hPacketFieldSystemApi,null);
        HyperIoTTestUtils.truncateTables(hPacketSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
    }

}

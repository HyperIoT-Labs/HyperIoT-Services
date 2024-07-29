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

package it.acsoftware.hyperiot.hproject.algorithm.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmSystemApi;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.algorithm.service.rest.AlgorithmRestApi;
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
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmSystemApi;
import it.acsoftware.hyperiot.hproject.algorithm.job.HProjectAlgorithmJob;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmInputField;
import it.acsoftware.hyperiot.hproject.algorithm.model.MappedInput;
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
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

import static it.acsoftware.hyperiot.hproject.algorithm.test.HyperIoTHProjectAlgorithmConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHProjectAlgorithmRestInterfaceTest extends KarafTestSupport {

    /*
        TODO
            We must add a test on  getAlgorithmOutputs service on HProjectAlgorithmRestApi.
            To test this endpoint we must trigger the execution of an algorithm such that we can inspect the Algorithm's Outputs.
     */

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    @Before
    public void impersonateAsHyperIoTAdmin() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestApi = getOsgiService(HProjectAlgorithmRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin", "admin");
        this.impersonateUser(hProjectAlgorithmRestApi, admin);
    }

    private HyperIoTAction getHyperIoTAction(String resourceName,
                                             HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder
                .createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
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
    public void test001_saveHProjectAlgorithmShouldSerializeResponseCorrectly() {
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojectalgorithms"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(hProjectAlgorithmTemplate)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hProjectAlgorithmWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("algorithm", algorithmWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("jobParams", hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }


    @Test
    public void test002_findHProjectAlgorithmShouldSerializeResponseCorrectly() {
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojectalgorithms/").concat(String.valueOf(projectAlgorithm.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hProjectAlgorithmWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("algorithm", algorithmWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("jobParams", hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateHProjectAlgorithmShouldSerializeResponseCorrectly() {
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        projectAlgorithm.setName("Name".concat(UUID.randomUUID().toString().replaceAll("-", "")));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojectalgorithms"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(projectAlgorithm)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hProjectAlgorithmWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("algorithm", algorithmWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("jobParams", hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteHProjectAlgorithmShouldSerializeResponseCorrectly() {
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojectalgorithms/").concat(String.valueOf(projectAlgorithm.getId())))
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
    public void test005_findAllHProjectAlgorithmShouldSerializeResponseCorrecly() {
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        HProjectAlgorithm projectAlgorithm2 = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojectalgorithms/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hProjectAlgorithmWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("algorithm", algorithmWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("jobParams", hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllHProjectAlgorithmPaginatedPaginatedShouldSerializeResponseCorrecly() {
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        HProjectAlgorithm projectAlgorithm2 = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojectalgorithms"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("delta", String.valueOf(defaultDelta))
                .withParameter("page", String.valueOf(defaultPage))
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results", hProjectAlgorithmWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("results.project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("results.algorithm", algorithmWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("results.jobParams", hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findByHProjectIdShouldSerializeResponseCorrectly() {
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        Assert.assertNotNull(projectAlgorithm.getProject());
        Assert.assertNotEquals(0, projectAlgorithm.getProject().getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/hprojectalgorithms").
                concat("/projects/").concat(String.valueOf(projectAlgorithm.getProject().getId()));
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
                .containExactProperties(hProjectAlgorithmWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("algorithm", algorithmWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("jobParams", hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_updateBaseConfigShouldSerializeResponseCorrectly() throws JsonProcessingException {
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm();
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        Assert.assertNotNull(projectAlgorithm.getProject());
        Assert.assertNotEquals(0, projectAlgorithm.getProject().getId());
        HProjectAlgorithmConfig newConfig = createNewHProjectAlgorithmConfigAlgorithmConfig(projectAlgorithm.getProject());
        ObjectMapper mapper = new ObjectMapper();
        String serializedConfig = mapper.writeValueAsString(newConfig);
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/hprojectalgorithms/")
                .concat(String.valueOf(projectAlgorithm.getId()))
                .concat("/config");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedConfig)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(hProjectAlgorithmWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("project", hProjectWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("algorithm", algorithmWithHyperIoTInnerEntitySerializerExpectedProperties())
                .containExactInnerProperties("jobParams", hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }


    /*
     *
     *
     * Utility method for test .
     *
     *
     *
     */


    private List<String> hProjectAlgorithmWithJsonViewPublicExpectedProperties() {
        List<String> hProjectAlgorithmWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("name");
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("config");
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("className");
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("jobParams");
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("cronExpression");
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("active");
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("algorithm");
        hProjectAlgorithmWithJsonViewPublicExpectedProperties.add("project");
        return hProjectAlgorithmWithJsonViewPublicExpectedProperties;
    }

    private List<String> hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties() {
        List<String> hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("appResource");
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("cronExpression");
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("mainClass");
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("name");
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("projectId");
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("config");
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("spark.jars");
        hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties.add("algorithmId");
        return hprojectAlgorithmJobParamsWithJsonViewPublicExpectedProperties;
    }

    private List<String> algorithmWithHyperIoTInnerEntitySerializerExpectedProperties() {
        List<String> algorithmWithHyperIoTInnerEntitySerializerExpectedProperties = new ArrayList<>();
        algorithmWithHyperIoTInnerEntitySerializerExpectedProperties.add("id");
        algorithmWithHyperIoTInnerEntitySerializerExpectedProperties.add("entityModifyDate");
        algorithmWithHyperIoTInnerEntitySerializerExpectedProperties.add("entityCreateDate");
        return algorithmWithHyperIoTInnerEntitySerializerExpectedProperties;
    }

    private List<String> hProjectWithHyperIoTInnerEntitySerializerExpectedProperties() {
        List<String> hProjectWithHyperIoTInnerEntitySerializerExpectedProperties = new ArrayList<>();
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("id");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("entityCreateDate");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("entityModifyDate");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("name");
        hProjectWithHyperIoTInnerEntitySerializerExpectedProperties.add("description");
        return hProjectWithHyperIoTInnerEntitySerializerExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties() {
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
    }

    private HProjectAlgorithmConfig createNewHProjectAlgorithmConfigAlgorithmConfig(HProject project) {
        //Crea a new device with a new packet and a new field to create a new HProjectAlgorithConfig.
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField otherPacketField = packet.getFields().iterator().next();
        //Create a new config for the algorithm
        Algorithm newAlgorithm = createAlgorithmWithInputAndOutputField();
        AlgorithmConfig newAlgorithmConfig = getAlgorithmConfig(newAlgorithm);
        Assert.assertTrue(newAlgorithmConfig.getOutput() != null && newAlgorithmConfig.getOutput().size() == 1);
        AlgorithmIOField newOutputField = newAlgorithmConfig.getOutput().get(0);
        Assert.assertTrue(newAlgorithmConfig.getInput() != null && newAlgorithmConfig.getInput().size() == 1);
        AlgorithmIOField newInputField = newAlgorithmConfig.getInput().get(0);
        MappedInput mappedInput = new MappedInput();
        mappedInput.setPacketFieldId(otherPacketField.getId());
        mappedInput.setAlgorithmInput(newInputField);
        HProjectAlgorithmInputField hProjectAlgorithmInputField = new HProjectAlgorithmInputField();
        hProjectAlgorithmInputField.setPacketId(packet.getId());
        List<MappedInput> mappedInputList = new LinkedList<>();
        mappedInputList.add(mappedInput);
        hProjectAlgorithmInputField.setMappedInputList(mappedInputList);
        HProjectAlgorithmConfig newHProjectAlgorithmConfig = new HProjectAlgorithmConfig();
        newHProjectAlgorithmConfig.setInput(new ArrayList<>());
        newHProjectAlgorithmConfig.setOutput(new ArrayList<>());
        newHProjectAlgorithmConfig.getOutput().add(newOutputField);
        newHProjectAlgorithmConfig.getInput().add(hProjectAlgorithmInputField);
        return newHProjectAlgorithmConfig;
    }

    private void forceAlgorithmTableCreation(HProjectAlgorithm hProjectAlgorithm) {
        HBaseConnectorSystemApi hBaseConnectorSystemApi = getOsgiService(HBaseConnectorSystemApi.class);
        String algorithmTable = String.format("algorithm_%s", hProjectAlgorithm.getAlgorithm().getId());
        try {
            List<String> columnFamilies = new ArrayList<>();
            columnFamilies.add("value");
            hBaseConnectorSystemApi.createTable(algorithmTable, columnFamilies);
            hBaseConnectorSystemApi.tableExists(algorithmTable);
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean tableExist = false;
        try {
            tableExist = hBaseConnectorSystemApi.tableExists(algorithmTable);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(tableExist);
    }

    private AlgorithmConfig getAlgorithmConfig(Algorithm algorithm) {
        AlgorithmConfig baseConfig;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            baseConfig = objectMapper.readValue(algorithm.getBaseConfig(), AlgorithmConfig.class);
        } catch (Throwable t) {
            Assert.fail();
            throw new RuntimeException();
        }
        return baseConfig;
    }

    private Algorithm createAlgorithmWithInputAndOutputField() {
        Algorithm algorithm = createAlgorithm();
        createAlgorithmIOField(algorithm, "INPUT");
        createAlgorithmIOField(algorithm, "OUTPUT");
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAlgorithm(algorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Algorithm algorithmWithConfiguration = (Algorithm) restResponse.getEntity();
        return algorithmWithConfiguration;
    }

    private Algorithm createAlgorithm() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("Algorithm defined by huser: " + adminUser.getUsername());
        algorithm.setMainClassname(algorithmResourceName);
        algorithm.setType(AlgorithmType.STATISTICS);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Algorithm) restResponse.getEntity()).getId());
        Assert.assertEquals(algorithm.getName(), ((Algorithm) restResponse.getEntity()).getName());
        Assert.assertEquals("Algorithm defined by huser: " + adminUser.getUsername(), ((Algorithm) restResponse.getEntity()).getDescription());
        Assert.assertEquals(algorithmResourceName, ((Algorithm) restResponse.getEntity()).getMainClassname());
        Assert.assertEquals(HyperIoTHProjectAlgorithmRestTest.DEFAULT_ALG_VAL, ((Algorithm) restResponse.getEntity()).getBaseConfig());
        return algorithm;
    }


    private AlgorithmIOField createAlgorithmIOField(Algorithm algorithm, String algorithmFieldType) {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        if (algorithm == null) {
            algorithm = createAlgorithm();
        }

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOFieldName " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithmIOField.setDescription("IOField description " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        if (algorithmFieldType == "INPUT") {
            algorithmIOField.setType(AlgorithmFieldType.INPUT);
            this.impersonateUser(algorithmRestApi, adminUser);
            Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
            Assert.assertEquals(200, restResponse.getStatus());
            //checks if inputField has been added in baseConfig
            String jsonInputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
            if ((((Algorithm) restResponse.getEntity()).getEntityVersion() - 1) == algorithmIOField.getId()) {
                Assert.assertTrue(
                        jsonInputField.contains(
                                "{\"id\":" + algorithmIOField.getId() + "," +
                                        "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                        "\"description\":\"" + algorithmIOField.getDescription() + "\"," +
                                        "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                        "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                        "\"type\":\"" + algorithmIOField.getType() + "\"}"
                        )
                );
            }
        }
        if (algorithmFieldType == "OUTPUT") {
            algorithmIOField.setType(AlgorithmFieldType.OUTPUT);
            this.impersonateUser(algorithmRestApi, adminUser);
            Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
            Assert.assertEquals(200, restResponse.getStatus());
            //checks if outputField has been added in baseConfig
            String jsonOutputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
            if ((((Algorithm) restResponse.getEntity()).getEntityVersion() - 1) == algorithmIOField.getId()) {
                Assert.assertTrue(
                        jsonOutputField.contains(
                                "{\"id\":" + algorithmIOField.getId() + "," +
                                        "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                        "\"description\":\"" + algorithmIOField.getDescription() + "\"," +
                                        "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                        "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                        "\"type\":\"" + algorithmIOField.getType() + "\"}"
                        )
                );
            }
        }
        if (algorithmFieldType != "INPUT" && algorithmFieldType != "OUTPUT") {
            Assert.assertEquals(0, algorithmIOField.getId());
            System.out.println("algorithmIOField is null, field not created...");
            System.out.println("allowed values: INPUT, OUTPUT");
            return null;
        }
        return algorithmIOField;
    }

    private HProjectAlgorithm createHProjectAlgorithm() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        HProject project = createHProject();
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String cronExpression = hProjectAlgorithmTemplate.getCronExpression();
        String config = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(restResponse, project, algorithm, hprojectAlgorithmName, config, cronExpression);
        return (HProjectAlgorithm) restResponse.getEntity();
    }

    private HProjectAlgorithm createHProjectAlgorithmTemplate(HProject project, Algorithm algorithm, HPacket packet, HPacketField packetField) {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hProjectAlgorithmRestService, adminUser);
        HProjectAlgorithm hProjectAlgorithm = new HProjectAlgorithm();
        hProjectAlgorithm.setAlgorithm(algorithm);
        hProjectAlgorithm.setProject(project);
        hProjectAlgorithm.setConfig(createHProjectAlgorithmConfig(project, algorithm, packet, packetField));
        hProjectAlgorithm.setCronExpression(createCronExpression());
        hProjectAlgorithm.setName(createHProjectAlgorithmName());
        return hProjectAlgorithm;
    }


    private void assertSavedHProjectAlgorithm(Response restResponse, HProject project, Algorithm algorithm,
                                              String hprojectAlgorithmName, String config, String cronExpression) {
        //Assert response status.
        Assert.assertEquals(200, restResponse.getStatus());
        HProjectAlgorithm entityResponse = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertNotNull(entityResponse);
        //Assert the project related to hprojectAlgorithms
        Assert.assertNotNull(entityResponse.getProject());
        Assert.assertEquals(entityResponse.getProject().getId(), project.getId());
        //Assert the algorithm related to hprojectAlgorithms
        Assert.assertNotNull(entityResponse.getAlgorithm());
        Assert.assertEquals(entityResponse.getAlgorithm().getId(), algorithm.getId());
        //Assertion relative to hprojectAlgorithm
        Assert.assertNotEquals(0, entityResponse.getId());
        Assert.assertEquals(entityResponse.getClassName(), HProjectAlgorithmJob.class.getName());
        Assert.assertEquals(entityResponse.getName(), hprojectAlgorithmName);
        Assert.assertEquals(entityResponse.getConfig(), config);
        Assert.assertEquals(entityResponse.getCronExpression(), cronExpression);
        Assert.assertFalse(entityResponse.isActive());
        AlgorithmUtil algorithmUtil = getOsgiService(AlgorithmUtil.class);
        //Assertion relative to jobDetail
        JobDetail algorithmJobDetail = entityResponse.getJobDetail();
        Assert.assertNotNull(algorithmJobDetail);
        Assert.assertEquals(algorithmJobDetail.getJobClass(), HProjectAlgorithmJob.class);
        Assert.assertEquals(algorithmJobDetail.getKey().getName(), Long.toString(entityResponse.getId()));
        Assert.assertEquals(algorithmJobDetail.getKey().getGroup(), "DEFAULT");
        JobDataMap dataMap = algorithmJobDetail.getJobDataMap();
        Assert.assertNotNull(dataMap);
        Assert.assertEquals(dataMap.get("cronExpression"), cronExpression);
        Assert.assertEquals(dataMap.get("name"), entityResponse.getName());
        Assert.assertEquals(dataMap.get("projectId").toString(), Long.toString(project.getId()));
        Assert.assertEquals(dataMap.get("config"), config);
        Assert.assertEquals(dataMap.get("algorithmId").toString(), Long.toString(algorithm.getId()));
        Assert.assertEquals(dataMap.get("mainClass"), algorithm.getMainClassname());
        Assert.assertEquals(dataMap.get("appResource"), algorithmUtil.getJarFullPath(algorithm));
        Assert.assertEquals(dataMap.get("spark.jars"), algorithmUtil.getJarFullPath(algorithm));
        //Assertion relative to job params.
        Map<String, Object> jobParams = entityResponse.getJobParams();
        Assert.assertNotNull(jobParams);
        Assert.assertEquals(jobParams.get("name"), entityResponse.getName());
        Assert.assertEquals(jobParams.get("projectId").toString(), Long.toString(project.getId()));
        Assert.assertEquals(jobParams.get("config"), config);
        Assert.assertEquals(jobParams.get("algorithmId").toString(), Long.toString(algorithm.getId()));
        Assert.assertEquals(jobParams.get("mainClass"), algorithm.getMainClassname());
        Assert.assertEquals(jobParams.get("cronExpression"), cronExpression);
        Assert.assertEquals(jobParams.get("appResource"), algorithmUtil.getJarFullPath(algorithm));
        Assert.assertEquals(jobParams.get("spark.jars"), algorithmUtil.getJarFullPath(algorithm));
        JobKey jobKey = entityResponse.getJobKey();
        Assert.assertNotNull(jobKey);
        Assert.assertEquals(jobKey.getName(), Long.toString(entityResponse.getId()));
        Assert.assertEquals(jobKey.getGroup(), "DEFAULT");
    }


    private String createHProjectAlgorithmConfig(HProject project, Algorithm algorithm, HPacket packet, HPacketField hPacketField) {
        long packetId = packet.getId();
        long hpacketFieldId = hPacketField.getId();
        ObjectMapper objectMapper = new ObjectMapper();
        AlgorithmConfig baseConfig;
        try {
            baseConfig = objectMapper.readValue(algorithm.getBaseConfig(), AlgorithmConfig.class);
        } catch (Throwable t) {
            Assert.fail();
            throw new RuntimeException();
        }
        Assert.assertTrue(baseConfig.getInput() != null && baseConfig.getInput().size() == 1);
        AlgorithmIOField inputField = baseConfig.getInput().get(0);
        long algorithmInputFieldId = inputField.getId();
        String algorithInputFieldName = inputField.getName();
        String algorithmInputFieldDescription = inputField.getDescription();
        AlgorithmIOFieldType algorithmInputFieldType = inputField.getFieldType();
        AlgorithmIOFieldMultiplicity algorithmInputFieldMultiplicity = inputField.getMultiplicity();
        AlgorithmFieldType algorithmInputType = inputField.getType();
        Assert.assertTrue(baseConfig.getOutput() != null && baseConfig.getOutput().size() == 1);
        AlgorithmIOField outputField = baseConfig.getOutput().get(0);
        long algorithmOutputFieldId = outputField.getId();
        String algorithOutputFieldName = outputField.getName();
        String algorithmOutputFieldDescription = outputField.getDescription();
        AlgorithmIOFieldType algorithmOutputFieldType = outputField.getFieldType();
        AlgorithmIOFieldMultiplicity algorithmOutputFieldMultiplicity = outputField.getMultiplicity();
        AlgorithmFieldType algorithmOutputType = outputField.getType();
        return String.format("{\"input\":[{\"packetId\"" + ": %s ,\"" +
                        "mappedInputList\":[{\"packetFieldId\"" + ":  %s," +
                        "\"algorithmInput\":{\"id\":\" %s \",\"name\":\" %s \"," +
                        "\"description\":\" %s \",\"fieldType\":\" %s \"," +
                        "\"multiplicity\":\" %s \",\"type\":\" %s \"}}]}]," +
                        "\"output\":[{\"id\": %s,\"name\":\" %s \",\"description\": \" %s \"," +
                        "\"fieldType\":\" %s \",\"multiplicity\":\" %s \",\"type\":\" %s \"}]}\"",
                packetId, hpacketFieldId,//packet and packet field settings
                algorithmInputFieldId, algorithInputFieldName, algorithmInputFieldDescription, algorithmInputFieldType, algorithmInputFieldMultiplicity, algorithmInputType, //input field settings
                algorithmOutputFieldId, algorithOutputFieldName, algorithmOutputFieldDescription, algorithmOutputFieldType, algorithmOutputFieldMultiplicity, algorithmOutputType
        );
    }

    private String createCronExpression() {
        return "0 0 10 ? 1 MON#1 *";
    }

    private String createHProjectAlgorithmName() {
        return "hprojectAlgorithm" + java.util.UUID.randomUUID().toString().replaceAll("-", "");
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

    private HPacket createHPacketAndAddHPacketField(HDevice hdevice, boolean createField) {
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

        if (createField) {
            HPacketField field1 = new HPacketField();
            field1.setPacket(hpacket);
            field1.setName("temperature" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
            field1.setDescription("Temperature");
            field1.setType(HPacketFieldType.DOUBLE);
            field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
            field1.setValue(24.0);


            hpacket.setFields(new HashSet<>() {
                {
                    add(field1);
                }
            });

            // add field1
            this.impersonateUser(hPacketRestApi, adminUser);
            Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
            Assert.assertEquals(200, responseAddField1.getStatus());
            Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
            Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
            Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
            Assert.assertEquals(adminUser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());

            //check restResponse field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            List<HPacketField> fields = new ArrayList<>();
            fields.addAll(((HPacket) restResponse.getEntity()).getFields());
            Assert.assertEquals(fields.get(0).getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

        }
        return hpacket;
    }

    @After
    public void afterTest() {
        HProjectAlgorithmSystemApi hProjectAlgorithmSystemApi = getOsgiService(HProjectAlgorithmSystemApi.class);
        AlgorithmSystemApi algorithmSystemApi = getOsgiService(AlgorithmSystemApi.class);
        AreaDeviceRepository areaDeviceRepository = getOsgiService(AreaDeviceRepository.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HPacketFieldSystemApi hPacketFieldSystemApi = getOsgiService(HPacketFieldSystemApi.class);
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        HyperIoTTestUtils.truncateTables(hProjectAlgorithmSystemApi, null);
        HyperIoTTestUtils.truncateTables(algorithmSystemApi, null);
        HyperIoTTestUtils.truncateTables(areaDeviceRepository, null);
        HyperIoTTestUtils.truncateTables(areaSystemApi, null);
        HyperIoTTestUtils.truncateTables(hPacketFieldSystemApi, null);
        HyperIoTTestUtils.truncateTables(hPacketSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
    }
}
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

package it.acsoftware.hyperiot.algorithm.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.algorithm.service.rest.AlgorithmRestApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.http.*;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidationCriteria;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidator;
import it.acsoftware.hyperiot.base.test.http.matcher.HyperIoTHttpResponseValidatorBuilder;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.algorithm.test.HyperIoTAlgorithmConfiguration.*;

/**
 * @author Francesco Salerno
 *
 * This is a test class relative to Interface's test of AlgorithmRestApi
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAlgorithmRestInterfaceTest extends KarafTestSupport {

    /*
       TODO
            Add test to uploadJar service (relative to the serialization of the response).
            We can add test method to verify different cases of Algorithm's baseConfig field's serialization.
     */

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    @Before
    public void impersonateAsHyperIoTAdmin(){
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(algorithmRestApi, admin);
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    @Test
    public void test001_checksIfFileHadoopManagerCfgExists() {
        // checks if it.acsoftware.hyperiot.hadoopmanager.cfg exists.
        // If file not found HyperIoTHadoopManager-service bundle is in Waiting state
        String hyperIoTHadoopManagerService = executeCommand("bundle:list | grep HyperIoTHadoopManager-service");
        boolean fileCfgHadoopManagerFound = false;
        String fileConfigHadoopManager = executeCommand("config:list | grep it.acsoftware.hyperiot.hadoopmanager.cfg");
        System.out.println(hyperIoTHadoopManagerService);
        if (hyperIoTHadoopManagerService.contains("Active")) {
            Assert.assertTrue(hyperIoTHadoopManagerService.contains("Active"));
            if (fileConfigHadoopManager.contains("it.acsoftware.hyperiot.hadoopmanager.cfg")) {
                Assert.assertTrue(fileConfigHadoopManager.contains("it.acsoftware.hyperiot.hadoopmanager.cfg"));
                fileCfgHadoopManagerFound = true;
            }
        }
        if (hyperIoTHadoopManagerService.contains("Waiting")) {
            Assert.assertTrue(hyperIoTHadoopManagerService.contains("Waiting"));
            if (fileConfigHadoopManager.isEmpty()) {
                Assert.assertTrue(fileConfigHadoopManager.isEmpty());
                Assert.assertFalse(fileCfgHadoopManagerFound);
                System.out.println("file ect/it.acsoftware.hyperiot.hadoopmanager.cfg not found...");
            }
        }
        Assert.assertTrue(fileCfgHadoopManagerFound);
    }

    @Test
    public void test002_checksIfFileAlgorithmCfgExists() {
        // checks if it.acsoftware.hyperiot.algorithm.cfg exists.
        boolean fileCfgHadoopManagerFound = false;
        String fileConfigHadoopManager = executeCommand("config:list | grep it.acsoftware.hyperiot.algorithm.cfg");
        if (!fileConfigHadoopManager.isEmpty()) {
            if (fileConfigHadoopManager.contains("it.acsoftware.hyperiot.algorithm.cfg")) {
                Assert.assertTrue(fileConfigHadoopManager.contains("it.acsoftware.hyperiot.algorithm.cfg"));
                fileCfgHadoopManagerFound = true;
            }
        }
        Assert.assertTrue(fileCfgHadoopManagerFound);
    }

    @Test
    public void test003_saveAlgorithmShouldSerializeResponseCorrectly(){
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("Algorithm defined by huser: " + adminUser.getUsername());
        algorithm.setMainClassname(algorithmResourceName);
        algorithm.setType(AlgorithmType.STATISTICS);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/algorithms"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(algorithm)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(canSerializeBaseConfigInAlgorithmConfigObject())
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_findAlgorithmShouldSerializeResponseCorrectly(){
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/algorithms/").concat(String.valueOf(algorithm.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(canSerializeBaseConfigInAlgorithmConfigObject())
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test005_updateAlgorithmShouldSerializeResponseCorrectly(){
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setName("newName " + UUID.randomUUID().toString().replaceAll("-", ""));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/algorithms"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(algorithm)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(canSerializeBaseConfigInAlgorithmConfigObject())
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_deleteAlgorithmShouldSerializeResponseCorrectly(){
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/algorithms/").concat(String.valueOf(algorithm.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .withCustomCriteria(hyperIoTHttpResponse -> hyperIoTHttpResponse.getResponseBody().isEmpty())
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllAlgorithmShouldSerializeResponseCorrectly(){
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Algorithm algorithm2 = createAlgorithm();
        Assert.assertNotEquals(0, algorithm2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/algorithms/type/STATISTICS/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try{
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        if(! node.isArray()){
                            return false;
                        }
                        for (JsonNode curr : node) {
                            if (!curr.has("baseConfig") || !curr.get("baseConfig").isTextual()) {
                                return false;
                            }
                            TextNode baseConfig = (TextNode) curr.get("baseConfig");
                            if(! canSerializeBaseConfigInAlgorithmConfigObject(baseConfig)){
                                return false;
                            }
                        }
                    }catch ( Exception e){
                        return false;
                    }
                    return true;
                })
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_findAllAlgorithmPaginatedShouldSerializeResponseCorrectly(){
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Algorithm algorithm2 = createAlgorithm();
        Assert.assertNotEquals(0, algorithm2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/algorithms/type/STATISTICS"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withParameter("delta",String.valueOf(defaultDelta))
                .withParameter("page",String.valueOf(defaultPage))
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactHyperIoTPaginatedProperties()
                .containExactInnerProperties("results",expectedAlgorithmFields)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try{
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        if(! node.has("results") || ! node.get("results").isArray()){
                            return false;
                        }
                        JsonNode resultsNode = node.get("results");
                        for (JsonNode curr : resultsNode) {
                            if (!curr.has("baseConfig") || !curr.get("baseConfig").isTextual()) {
                                return false;
                            }
                            TextNode baseConfig = (TextNode) curr.get("baseConfig");
                            if(! canSerializeBaseConfigInAlgorithmConfigObject(baseConfig)){
                                return false;
                            }
                        }
                    }catch ( Exception e){
                        return false;
                    }
                    return true;
                })
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_addIOFieldShouldSerializeResponseCorrectly() throws JsonProcessingException {
        //hadmin create an algorithm
        //hadmin add algorithmIOField to algorithm (When user create algorithm algorithm has not AlgorithmIOField associated).
        //Test the response serialization based on this assumption.
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOFieldName " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithmIOField.setDescription("IOField description " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);
        algorithmIOField.setType(AlgorithmFieldType.INPUT);
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/algorithms/").concat(String.valueOf(algorithm.getId())).concat("/ioFields");
        ObjectMapper mapper = new ObjectMapper();
        String algorithmIOFieldSerialized = mapper.writeValueAsString(algorithmIOField);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(algorithmIOFieldSerialized)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("baseConfig"));
                        Assert.assertTrue(node.get("baseConfig").isTextual());
                        String baseConfig = node.get("baseConfig").textValue();
                        AlgorithmConfig config = objMapper.readValue(baseConfig,AlgorithmConfig.class);
                        Assert.assertEquals(config.getInput().size(),1);
                        long inputFieldId = config.getInput().get(0).getId();
                        String expectedAlgorithmBaseConfig =
                                "{\"input\":[{\"id\":" + inputFieldId + "," +
                                        "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                        "\"description\":\"" + algorithmIOField.getDescription() + "\"," +
                                        "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                        "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                        "\"type\":\"" + algorithmIOField.getType() + "\"}],\"output\":[],\"customConfig\":null}";
                        return baseConfig.equals(expectedAlgorithmBaseConfig);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test009_deleteIOFieldShouldSerializeResponseCorrectly(){
        //hadmin create an algorithm
        //hadmin add algorithmIOField to algorithm (When user create algorithm algorithm has not AlgorithmIOField associated).
        //hadmin delete algorithmIOField from algorithm
        //Test the response serialization based on this assumption.
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm,"INPUT");
        Assert.assertNotNull(algorithmIOField);
        Assert.assertNotNull(algorithmIOField.getType());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/algorithms/").concat(String.valueOf(algorithm.getId()))
                .concat("/ioFields").concat("/INPUT/").concat(String.valueOf(algorithmIOField.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("baseConfig"));
                        Assert.assertTrue(node.get("baseConfig").isTextual());
                        String baseConfig = node.get("baseConfig").textValue();
                        String expectedBaseConfig = "{\"input\":[],\"output\":[],\"customConfig\":null}";
                        return baseConfig.equals(expectedBaseConfig);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);

    }

    @Test
    public void test010_getBaseConfigShouldSerializeResponseCorrectly(){
        //hadmin create an algorithm without add algorithmIOField to algorithm
        //Test the response serialization based on this assumption.
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/algorithms/").concat(String.valueOf(algorithm.getId()))
                .concat("/baseConfig");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        String expectedBaseConfig = "{\"input\":[],\"output\":[],\"customConfig\":null}";
                        return hyperIoTHttpResponse.getResponseBody().equals(expectedBaseConfig);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test011_updateBaseConfigShouldSerializeResponseCorrectly() throws JsonProcessingException {
        //hadmin create an algorithm without
        //hadmin add algorithmIOField with Input Type to algorithm
        //hadmin add algorithmIOField with Output type to algorithm
        //hadmin update algorithm baseConfig with the following call  /hyperiot/algorithms/{algorithmId}/baseConfig
        //hadmin the response serialization based on this assumption.
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotNull(algorithmIOField1);
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        algorithmIOField1.setDescription("description edited in date: " + new Date());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm,"OUTPUT");
        Assert.assertNotNull(algorithmIOField2);
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        algorithmIOField2.setDescription("description edited in date: " + new Date());

        AlgorithmConfig algorithmConfig = new AlgorithmConfig();
        List<AlgorithmIOField> input = new ArrayList<>();
        input.add(algorithmIOField1);
        List<AlgorithmIOField> output = new ArrayList<>();
        output.add(algorithmIOField2);
        algorithmConfig.setInput(input);
        algorithmConfig.setOutput(output);
        ObjectMapper mapper = new ObjectMapper();
        String baseConfigSerialized = mapper.writeValueAsString(algorithmConfig);
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/algorithms/").concat(String.valueOf(algorithm.getId()))
                .concat("/baseConfig");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(baseConfigSerialized)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("baseConfig"));
                        Assert.assertTrue(node.get("baseConfig").isTextual());
                        String baseConfigResponse = node.get("baseConfig").textValue();
                        return baseConfigResponse.equals(baseConfigSerialized);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test012_updateIOFieldShouldSerializeResponseCorrectly() throws JsonProcessingException {
        //hadmin create an algorithm without
        //hadmin add algorithmIOField of Input type to algorithm
        //hadmin update algorithmIOField   /hyperiot/algorithms/{algorithmId}/ioFields
        //hadmin the response serialization based on this assumption.
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotNull(algorithmIOField1);
        algorithmIOField1.setDescription("description edited in date: " + new Date());
        ObjectMapper mapper = new ObjectMapper();
        String algorithmIOField1Serialized = mapper.writeValueAsString(algorithmIOField1);
        AlgorithmConfig config = new AlgorithmConfig();
        config.setInput(new ArrayList<>());
        config.setOutput(new ArrayList<>());
        config.getInput().add(algorithmIOField1);
        String expectedBaseConfigSerialized = mapper.writeValueAsString(config);
        String requestUri = HyperIoTHttpUtils.SERVICE_BASE_URL
                .concat("/algorithms/").concat(String.valueOf(algorithm.getId()))
                .concat("/ioFields");
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(algorithmIOField1Serialized)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        List<String> expectedAlgorithmFields = algorithmExpectedProperties();
        expectedAlgorithmFields.addAll(hyperIoTAbstractEntityProperties());
        HyperIoTHttpResponseValidator responseValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(expectedAlgorithmFields)
                .withCustomCriteria(hyperIoTHttpResponse -> {
                    try {
                        ObjectMapper objMapper = new ObjectMapper();
                        JsonNode node = objMapper.readTree(hyperIoTHttpResponse.getResponseBody());
                        Assert.assertTrue(node.has("baseConfig"));
                        Assert.assertTrue(node.get("baseConfig").isTextual());
                        String baseConfigResponse = node.get("baseConfig").textValue();
                        return baseConfigResponse.equals(expectedBaseConfigSerialized);
                    } catch (Exception e){
                        return false;
                    }
                })
                .build();
        boolean testSuccessful = responseValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    /*
    *
    *
    *   Utility Methods.
    *
    *
     */

    private boolean canSerializeBaseConfigInAlgorithmConfigObject(TextNode baseConfig){
        try {
            ObjectMapper mapper = new ObjectMapper();
            AlgorithmConfig config = mapper.readValue(baseConfig.textValue(), AlgorithmConfig.class);
        } catch (Exception e){
            return false;
        }
        return true;
    }

    private HyperIoTHttpResponseValidationCriteria canSerializeBaseConfigInAlgorithmConfigObject() {
        return (hyperIoTHttpResponse) -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(hyperIoTHttpResponse.getResponseBody()).get("baseConfig");
                Assert.assertTrue(node.isTextual());
                AlgorithmConfig config = mapper.readValue(node.textValue(), AlgorithmConfig.class);
            } catch (Exception e) {
                return false;
            }
            return true;
        };
    }

    private List<String> algorithmExpectedProperties(){
        List<String> algorithmExpectedProperties = new ArrayList<>();
        algorithmExpectedProperties.add("name");
        algorithmExpectedProperties.add("description");
        algorithmExpectedProperties.add("baseConfig");
        algorithmExpectedProperties.add("algorithmFileName");
        algorithmExpectedProperties.add("algorithmFilePath");
        algorithmExpectedProperties.add("type");
        algorithmExpectedProperties.add("mainClassname");
        return algorithmExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
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
        String defaultValue = "{\"input\":[],\"output\":[],\"customConfig\":null}";
        Assert.assertEquals(defaultValue, ((Algorithm) restResponse.getEntity()).getBaseConfig());
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

    @After
    public void afterTest() {
        // Remove all Algorithms created in every tests
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithm(AlgorithmType.STATISTICS);
        List<Algorithm> listAlgorithm = restResponse.readEntity(new GenericType<List<Algorithm>>() {
        });
        if (!listAlgorithm.isEmpty()) {
            for (Algorithm algorithm : listAlgorithm) {
                try {
                    // delete jar file inside /spark/jobs
                    HadoopManagerSystemApi hadoopManagerSystemApi = getOsgiService(HadoopManagerSystemApi.class);
                    String pathJarFile = "/spark/jobs/";
                    String jarName = algorithm.getName().replaceAll(" ", "_").toLowerCase() + ".jar";
                    hadoopManagerSystemApi.deleteFile(pathJarFile + jarName);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                this.impersonateUser(algorithmRestApi, adminUser);
                Response restResponse1 = algorithmRestApi.deleteAlgorithm(algorithm.getId());
                Assert.assertEquals(200, restResponse1.getStatus());
                Assert.assertNull(restResponse1.getEntity());
            }
        }
    }

}

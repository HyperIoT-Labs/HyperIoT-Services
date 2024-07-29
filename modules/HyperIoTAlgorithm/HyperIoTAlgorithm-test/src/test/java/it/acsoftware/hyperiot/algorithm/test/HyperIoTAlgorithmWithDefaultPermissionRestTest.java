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

import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.algorithm.service.rest.AlgorithmRestApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.model.Role;
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
import java.io.File;
import java.io.IOException;
import java.util.*;

import static it.acsoftware.hyperiot.algorithm.test.HyperIoTAlgorithmConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for Algorithm System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAlgorithmWithDefaultPermissionRestTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
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
    public void test003_algorithmModuleShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call checkModuleWorking checks if Algorithm module working
        // correctly
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Algorithm Module works!", restResponse.getEntity());
    }


    // Algorithm action save: 1 not assigned in default permission
    @Test
    public void test004_saveAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to save new Algorithm with the following call saveAlgorithm
        // huser to save a new Algorithm needs the "save algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("everybody wants to rule the world");
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");
        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update: 2 not assigned in default permission
    @Test
    public void test005_updateAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update Algorithm with the following call updateAlgorithm
        // huser to update Algorithm needs the "update algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setDescription("edit description failed...");

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action remove: 4 not assigned in default permission
    @Test
    public void test006_deleteAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to delete Algorithm with the following call deleteAlgorithm
        // huser to delete Algorithm needs the "remove algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.deleteAlgorithm(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action find: 8 not assigned in default permission
    @Test
    public void test007_findAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to find Algorithm with the following call findAlgorithm
        // huser to find Algorithm needs the "find algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.findAlgorithm(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action find-all: 16
    @Test
    public void test008_findAllAlgorithmWithDefaultPermissionShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, find all Algorithms with the following call findAllAlgorithm
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.findAllAlgorithm(AlgorithmType.STATISTICS);
        Assert.assertEquals(200, restResponse.getStatus());
        List<Algorithm> algorithmList = restResponse.readEntity(new GenericType<List<Algorithm>>() {
        });
        Assert.assertFalse(algorithmList.isEmpty());
        Assert.assertEquals(1, algorithmList.size());
        boolean algorithmFound = false;
        for (Algorithm a : algorithmList) {
            if (algorithm.getId() == a.getId()) {
                algorithmFound = true;
            }
        }
        Assert.assertTrue(algorithmFound);
    }


    // Algorithm action find-all: 16
    @Test
    public void test009_findAllAlgorithmPaginatedWithDefaultPermissionShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, huser, with default permission
        // find all Algorithm with pagination
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<Algorithm> algorithms = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        Assert.assertEquals(defaultDelta, algorithms.size());
        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,defaultDelta, defaultPage);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAlgorithms.getResults().size());
        Assert.assertEquals(defaultDelta, listAlgorithms.getDelta());
        Assert.assertEquals(defaultPage, listAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listAlgorithms.getNextPage());
        // default delta is 10, default page is 1: 10 entities stored in database
        Assert.assertEquals(1, listAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // Algorithm action add_io_field: 32 not assigned in default permission
    @Test
    public void test010_addInputFieldWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to add input field with the following call addIOField
        // huser to add ioField needs the "add_io_field algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOField of " + algorithm.getName());
        algorithmIOField.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmIOField.setType(AlgorithmFieldType.INPUT);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action delete_io_field: 64 not assigned in default permission
    @Test
    public void test011_deleteInputFieldWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to delete input field with the following call deleteIOField
        // huser to delete ioField needs the "delete_io_field algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action read_base_config: 128 not assigned in default permission
    @Test
    public void test012_getBaseConfigWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to find BaseConfig with the following call getBaseConfig
        // huser to find BaseConfig needs the "read_base_config algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("INPUT"), algorithmIOField1.getType());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("OUTPUT"), algorithmIOField2.getType());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.getBaseConfig(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update_base_config: 256 not assigned in default permission
    @Test
    public void test013_updateBaseConfigWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update BaseConfig with the following call updateBaseConfig
        // huser to update BaseConfig needs the "read_base_config algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Date date = new Date();
        algorithmIOField1.setDescription("description edited in date: " + date);

        AlgorithmConfig algorithmConfig = new AlgorithmConfig();
        List<AlgorithmIOField> input = new ArrayList<>();
        input.add(algorithmIOField1);
        algorithmConfig.setInput(input);

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateBaseConfig(algorithm.getId(), algorithmConfig);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update_jar: 512 not assigned in default permission
    @Test
    public void test014_updateAlgorithmFileWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update jar file field with the following call updateAlgorithmFile
        // huser to update jar file needs the "update_jar algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getAlgorithmFileName());
        File algorithmFile = new File(jarPath + jarName);

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateAlgorithmFile(algorithm.getId(), algorithmResourceName, algorithmFile);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update_io_field: 1024 not assigned in default permission
    @Test
    public void test015_updateInputFieldWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update input field with the following call updateIOField
        // huser to update ioField needs the "update_io_field algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());
        algorithmIOField.setDescription("edit failed...");

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


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


    // Algorithm isn't Owned Resource
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
            Assert.assertFalse(listAlgorithm.isEmpty());
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
//		String sqlAlgorithm = "select * from algorithm";
//		String resultAlgorithm = executeCommand("jdbc:query hyperiot " + sqlAlgorithm);
//		System.out.println(resultAlgorithm);
    }


    private HUser huserWithDefaultPermissionInHyperIoTFramework(boolean isActive) {
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
        huser.setEmail(huser.getUsername() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        huser.setActive(false);
        Assert.assertNull(huser.getActivateCode());
        Response restResponse = hUserRestApi.register(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
        Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
        Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
        Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
        Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
        Assert.assertFalse(huser.isAdmin());
        Assert.assertFalse(huser.isActive());
        Assert.assertTrue(roles.isEmpty());
        if (isActive) {
            //Activate huser and checks if default role has been assigned
            Role role = null;
            Assert.assertFalse(huser.isActive());
            String activationCode = huser.getActivateCode();
            Assert.assertNotNull(activationCode);
            Response restResponseActivateUser = hUserRestApi.activate(huser.getEmail(), activationCode);
            Assert.assertEquals(200, restResponseActivateUser.getStatus());
            huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
            roles = Arrays.asList(huser.getRoles().toArray());
            Assert.assertFalse(roles.isEmpty());
            Assert.assertTrue(huser.isActive());

            // checks: default role has been assigned to new huser
            Assert.assertEquals(1, huser.getRoles().size());
            Assert.assertEquals(roles.size(), huser.getRoles().size());
            Assert.assertFalse(roles.isEmpty());
            for (int i = 0; i < roles.size(); i++) {
                role = ((Role) roles.get(i));
            }
            Assert.assertNotNull(role);
            Assert.assertEquals("RegisteredUser", role.getName());
            Assert.assertEquals("Role associated with the registered user",
                    role.getDescription());
            PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
            Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
            Assert.assertFalse(listPermissions.isEmpty());
            boolean resourceNameFound = false;
            for (Permission permission : listPermissions) {
                // it.acsoftware.hyperiot.algorithm.model.Algorithm (16)
                // find_all                 16
                if (permission.getEntityResourceName().contains(permissionAlgorithm)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionAlgorithm, permission.getEntityResourceName());
                    Assert.assertEquals(permissionAlgorithm + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(16, permission.getActionIds());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameFound = true;
                }
            }
            Assert.assertTrue(resourceNameFound);
        }
        return huser;
    }

}

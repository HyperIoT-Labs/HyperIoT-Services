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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.algorithm.test.HyperIoTAlgorithmConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for Algorithm System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAlgorithmRestTest extends KarafTestSupport {

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
    public void test003_algorithmModuleShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call checkModuleWorking checks if Algorithm module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Algorithm Module works!", restResponse.getEntity());
    }


    @Test
    public void test004_saveAlgorithmShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin save Algorithm with the following call saveAlgorithm
        // response status code '200'
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
    }


    @Test
    public void test005_saveAlgorithmShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to save Algorithm with the following call saveAlgorithm,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("description");
        algorithm.setMainClassname(algorithmResourceName);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");
        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test006_updateAlgorithmShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update Algorithm with the following call updateAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        Date date = new Date();
        algorithm.setDescription("description edited in date: " + date);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("description edited in date: " + date,
                ((Algorithm) restResponse.getEntity()).getDescription());
        Assert.assertEquals(algorithm.getEntityVersion() + 1,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
    }


    @Test
    public void test007_updateAlgorithmShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to update Algorithm with the following call updateAlgorithm,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setDescription("edit description failed...");
        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test008_updateAlgorithmShouldFailIfEntityNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        // entity isn't stored in database
        Algorithm algorithm = new Algorithm();
        algorithm.setDescription("edit description failed...");
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test009_findAlgorithmShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin find Algorithm with the following call findAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAlgorithm(algorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getId(), ((Algorithm) restResponse.getEntity()).getId());
        Assert.assertEquals("Algorithm defined by huser: " + adminUser.getUsername(), ((Algorithm) restResponse.getEntity()).getDescription());
    }


    @Test
    public void test010_findAlgorithmShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to find Algorithm with the following call findAlgorithm,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.findAlgorithm(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test011_findAlgorithmShouldFailIfEntityNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to find Algorithm with the following call findAlgorithm,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAlgorithm(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test012_findAllAlgorithmShouldWorkIfAlgorithmListIsEmpty() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin find all Algorithm with the following call findAllAlgorithm,
        // there are still no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithm(AlgorithmType.STATISTICS);
        Assert.assertEquals(200, restResponse.getStatus());
        List<Algorithm> algorithmList = restResponse.readEntity(new GenericType<List<Algorithm>>() {
        });
        Assert.assertTrue(algorithmList.isEmpty());
        Assert.assertEquals(0, algorithmList.size());
    }


    @Test
    public void test013_findAllAlgorithmShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin find all Algorithm with the following call findAllAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithm(AlgorithmType.STATISTICS);
        Assert.assertEquals(200, restResponse.getStatus());
        List<Algorithm> algorithmList = restResponse.readEntity(new GenericType<List<Algorithm>>() {
        });
        Assert.assertFalse(algorithmList.isEmpty());
        Assert.assertEquals(1, algorithmList.size());
        boolean algorithmFound = false;
        for (Algorithm a : algorithmList) {
            if (algorithm.getId() == a.getId()) {
                Assert.assertEquals("Algorithm defined by huser: " + adminUser.getUsername(), a.getDescription());
                algorithmFound = true;
            }
        }
        Assert.assertTrue(algorithmFound);
    }


    @Test
    public void test014_findAllAlgorithmShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to find all Algorithm with the following call findAllAlgorithm,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.findAllAlgorithm(AlgorithmType.STATISTICS);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test015_deleteAlgorithmShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin delete Algorithm with the following call deleteAlgorithm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteAlgorithm(algorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test016_deleteAlgorithmShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to delete Algorithm with the following call deleteAlgorithm,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.deleteAlgorithm(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test017_deleteAlgorithmShouldFailIfEntityNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to delete Algorithm with the following call deleteAlgorithm,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteAlgorithm(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test018_saveAlgorithmShouldFailIfNameIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName(null);
        algorithm.setType(AlgorithmType.STATISTICS);
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test019_saveAlgorithmShouldFailIfNameIsEmpty() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("");
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setType(AlgorithmType.STATISTICS);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test020_saveAlgorithmShouldFailIfNameIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("javascript:");
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setType(AlgorithmType.STATISTICS);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test021_saveAlgorithmShouldFailIfDescriptionIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("vbscript:");
        algorithm.setType(AlgorithmType.STATISTICS);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test022_saveAlgorithmShouldFailIfDescriptionIsOver3000Chars() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription(testMaxLength(maxLengthDescription));
        algorithm.setType(AlgorithmType.STATISTICS);
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals(maxLengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }


    @Test
    public void test023_saveAlgorithmShouldFailIfBaseConfigIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but BaseConfig is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setBaseConfig(null);
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("algorithm-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test024_saveAlgorithmShouldFailIfBaseConfigIsEmpty() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but BaseConfig is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setBaseConfig("");
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getBaseConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test025_saveAlgorithmShouldFailIfBaseConfigIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but BaseConfig is malicious code
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setBaseConfig("vbscript:");
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test026_saveAlgorithmShouldFailIfJarNameIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but jarName is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setBaseConfig("{}");
        algorithm.setAlgorithmFileName("javascript:");
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-algorithmfilename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getAlgorithmFileName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test027_saveAlgorithmShouldFailIfMainClassnameIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but MainClassname is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setBaseConfig("{}");
        algorithm.setMainClassname("javascript:");
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-mainclassname", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("algorithm-mainclassname", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test028_saveAlgorithmShouldFailIfMainClassnameIsInvalid() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but MainClassname is invalid
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        algorithm.setBaseConfig("{}");
        algorithm.setMainClassname("invalid main class name");
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-mainclassname", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getMainClassname(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test029_updateAlgorithmShouldFailIfNameIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setName(null);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test030_updateAlgorithmShouldFailIfNameIsEmpty() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setName("");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test031_updateAlgorithmShouldFailIfNameIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setName("vbscript:");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test032_updateAlgorithmShouldFailIfDescriptionIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but description is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setDescription("vbscript:");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test033_updateAlgorithmShouldFailIfDescriptionIsOver3000Chars() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setDescription(testMaxLength(maxLengthDescription));

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
        Assert.assertEquals(maxLengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
    }


    @Test
    public void test034_updateAlgorithmShouldFailIfBaseConfigIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but BaseConfig is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setBaseConfig(null);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("algorithm-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test035_updateAlgorithmShouldFailIfBaseConfigIsEmpty() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but BaseConfig is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setBaseConfig("");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getBaseConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test036_updateAlgorithmShouldFailIfBaseConfigIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but BaseConfig is malicious code
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setBaseConfig("vbscript:");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test037_updateAlgorithmShouldFailIfJarNameIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but jarName is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setAlgorithmFileName("javascript:");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-algorithmfilename", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getAlgorithmFileName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test038_updateAlgorithmShouldFailIfMainClassnameIsMaliciousCode() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but MainClassname is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setMainClassname("vbscript:");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-mainclassname", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("algorithm-mainclassname", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }


    @Test
    public void test039_updateAlgorithmShouldFailIfMainClassnameIsInvalid() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but MainClassname is invalid
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setMainClassname("invalid main class name");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-mainclassname", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getMainClassname(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    /*
     *
     *
     * ADD IOFIELD
     *
     *
     */


    @Test
    public void test040_addInputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update Algorithm and add input field with the following call addIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOField of " + algorithm.getName());
        algorithmIOField.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmIOField.setType(AlgorithmFieldType.INPUT);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 1,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        //checks if inputField has been added in baseConfig
        String jsonInputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        Assert.assertTrue(
                jsonInputField.contains(
                        "\"input\":[{\"id\":" + algorithmIOField.getId() + "," +
                                "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                "\"description\":\"" + algorithmIOField.getDescription() + "\"," +
                                "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmIOField.getType() + "\"}" +
                                "],\"output\":[],\"customConfig\":null}")
        );
    }


    @Test
    public void test041_addOutputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin add output field with the following call addIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOField of " + algorithm.getName());
        algorithmIOField.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField.setType(AlgorithmFieldType.OUTPUT);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 1,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        //checks if outputField has been added in baseConfig
        String jsonOutputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        System.out.println(jsonOutputField);
        Assert.assertTrue(
                jsonOutputField.contains(
                        "\"output\":[{\"id\":" + algorithmIOField.getId() + "," +
                                "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                "\"description\":\"" + algorithmIOField.getDescription() + "\"," +
                                "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmIOField.getType() + "\"}" +
                                "],\"customConfig\":null}")
        );
    }


    @Test
    public void test042_addInputAndOutputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update Algorithm and add input and output field with the following call addIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        //add input field
        AlgorithmIOField algorithmInputField = new AlgorithmIOField();
        algorithmInputField.setName("InputField of " + algorithm.getName());
        algorithmInputField.setDescription("InputField of " + algorithm.getDescription());
        algorithmInputField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmInputField.setType(AlgorithmFieldType.INPUT);
        algorithmInputField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse1 = algorithmRestApi.addIOField(algorithm.getId(), algorithmInputField);
        Assert.assertEquals(200, restResponse1.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 1,
                ((Algorithm) restResponse1.getEntity()).getEntityVersion());
        Assert.assertNotEquals(0, algorithmInputField.getId());

        //add output field
        AlgorithmIOField algorithmOutputField = new AlgorithmIOField();
        algorithmOutputField.setName("OutputField of " + algorithm.getName());
        algorithmOutputField.setDescription("OutputField of " + algorithm.getDescription());
        algorithmOutputField.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmOutputField.setType(AlgorithmFieldType.OUTPUT);
        algorithmOutputField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse2 = algorithmRestApi.addIOField(algorithm.getId(), algorithmOutputField);
        Assert.assertEquals(200, restResponse2.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 2,
                ((Algorithm) restResponse2.getEntity()).getEntityVersion());
        Assert.assertNotEquals(0, algorithmOutputField.getId());
        //checks if IOField has been added in baseConfig
        String jsonIOField = ((Algorithm) restResponse2.getEntity()).getBaseConfig();
        Assert.assertTrue(
                jsonIOField.contains(
                        "\"input\":[{\"id\":" + algorithmInputField.getId() + "," +
                                "\"name\":\"" + algorithmInputField.getName() + "\"," +
                                "\"description\":\"" + algorithmInputField.getDescription() + "\"," +
                                "\"fieldType\":\"" + algorithmInputField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmInputField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmInputField.getType() + "\"}" +
                                "]," +
                                "\"output\":[{\"id\":" + algorithmOutputField.getId() + "," +
                                "\"name\":\"" + algorithmOutputField.getName() + "\"," +
                                "\"description\":\"" + algorithmOutputField.getDescription() + "\"," +
                                "\"fieldType\":\"" + algorithmOutputField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmOutputField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmOutputField.getType() + "\"}" +
                                "],\"customConfig\":null}"
                )
        );
    }


    @Test
    public void test043_addInputFieldShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to add inputField in Algorithm with the following call addIOField,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOField of " + algorithm.getName());
        algorithmIOField.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmIOField.setType(AlgorithmFieldType.INPUT);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test044_addOutputFieldShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to add outputField in Algorithm with the following call addIOField,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOField of " + algorithm.getName());
        algorithmIOField.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmIOField.setType(AlgorithmFieldType.OUTPUT);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test045_addInputFieldShouldFailIfInputFieldIsDuplicated() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm and add input field with the following call addIOField,
        // but input field is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        AlgorithmIOField algorithmIOFieldDuplicated = new AlgorithmIOField();
        algorithmIOFieldDuplicated.setName(algorithmIOField.getName());
        algorithmIOFieldDuplicated.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOFieldDuplicated.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOFieldDuplicated.setType(AlgorithmFieldType.INPUT);
        algorithmIOFieldDuplicated.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOFieldDuplicated);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }


    @Test
    public void test046_addOutputFieldShouldFailIfOutputFieldIsDuplicated() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm and add output field with the following call addIOField,
        // but output field is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        AlgorithmIOField algorithmIOFieldDuplicated = new AlgorithmIOField();
        algorithmIOFieldDuplicated.setName(algorithmIOField.getName());
        algorithmIOFieldDuplicated.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOFieldDuplicated.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOFieldDuplicated.setType(AlgorithmFieldType.OUTPUT);
        algorithmIOFieldDuplicated.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOFieldDuplicated);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }


    @Test
    public void test047_addIOFieldShouldFailIfTypeIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to add input field with the following call addIOField,
        // but type is null
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOField of " + algorithm.getName());
        algorithmIOField.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmIOField.setType(null);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test048_addInputFieldShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to add input field with the following call addIOField,
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(null, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.addIOField(0, algorithmIOField);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test049_addOutputFieldShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to add output field with the following call addIOField,
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(null, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.addIOField(0, algorithmIOField);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     *
     * UPDATE IOFIELD
     *
     *
     */


    @Test
    public void test050_updateInputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update input field with the following call updateIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        Date date = new Date();
        algorithmIOField.setDescription("description edited in date: " + date);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 2,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        //checks if outputField has been added in baseConfig
        String jsonOutputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        Assert.assertTrue(
                jsonOutputField.contains(
                        "\"input\":[{\"id\":" + algorithmIOField.getId() + "," +
                                "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                "\"description\":\"description edited in date: " + date + "\"," +
                                "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmIOField.getType() + "\"}" +
                                "],\"output\":[],\"customConfig\":null}"
                )
        );
    }


    @Test
    public void test051_updateOutputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update output field with the following call updateIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        Date date = new Date();
        algorithmIOField.setDescription("description edited in date: " + date);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 2,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        //checks if outputField has been added in baseConfig
        String jsonOutputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        Assert.assertTrue(
                jsonOutputField.contains(
                        "\"output\":[{\"id\":" + algorithmIOField.getId() + "," +
                                "\"name\":\"" + algorithmIOField.getName() + "\"," +
                                "\"description\":\"description edited in date: " + date + "\"," +
                                "\"fieldType\":\"" + algorithmIOField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmIOField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmIOField.getType() + "\"}" +
                                "],\"customConfig\":null}")
        );
    }


    @Test
    public void test052_updateInputAndOutputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update Algorithm update add input and output field with the following call updateIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        //update input field
        AlgorithmIOField algorithmInputField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmInputField.getId());

        Date date = new Date();
        algorithmInputField.setDescription("description edited in date: " + date);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmInputField);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 2,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());

        //update output field
        AlgorithmIOField algorithmOutputField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmOutputField.getId());

        algorithmOutputField.setDescription("description edited in date: " + date);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse2 = algorithmRestApi.updateIOField(algorithm.getId(), algorithmOutputField);
        Assert.assertEquals(200, restResponse2.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 4,
                ((Algorithm) restResponse2.getEntity()).getEntityVersion());

        //checks if IOField has been added in baseConfig
        String jsonIOField = ((Algorithm) restResponse2.getEntity()).getBaseConfig();
        Assert.assertTrue(
                jsonIOField.contains(
                        "\"input\":[{\"id\":" + algorithmInputField.getId() + "," +
                                "\"name\":\"" + algorithmInputField.getName() + "\"," +
                                "\"description\":\"description edited in date: " + date + "\"," +
                                "\"fieldType\":\"" + algorithmInputField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmInputField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmInputField.getType() + "\"}" +
                                "]," +
                                "\"output\":[{\"id\":" + algorithmOutputField.getId() + "," +
                                "\"name\":\"" + algorithmOutputField.getName() + "\"," +
                                "\"description\":\"description edited in date: " + date + "\"," +
                                "\"fieldType\":\"" + algorithmOutputField.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmOutputField.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmOutputField.getType() + "\"}" +
                                "],\"customConfig\":null}"
                )
        );
    }


    @Test
    public void test053_updateInputFieldShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to update inputField in Algorithm with the following call updateIOField,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());
        algorithmIOField.setDescription("edit failed...");

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test054_updateOutputFieldShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to update outputField in Algorithm with the following call updateIOField,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());
        algorithmIOField.setDescription("edit failed...");

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test055_updateInputFieldShouldFailIfIOFieldNotStoredInDatabase() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but ioField isn't stored in database
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        // algorithmIOField isn't stored in database
        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("InputField of " + algorithm.getName());
        algorithmIOField.setDescription("InputField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmIOField.setType(AlgorithmFieldType.INPUT);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test056_updateInputFieldShouldFailIfInputFieldIsDuplicated() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but input field is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());

        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        algorithmIOField2.setName(algorithmIOField1.getName());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }


    @Test
    public void test057_updateOutputFieldShouldFailIfOutputFieldIsDuplicated() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update output field with the following call updateIOField,
        // but output field is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());

        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        algorithmIOField2.setName(algorithmIOField1.getName());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }


    @Test
    public void test058_triesToUpdateOutputFieldInInputShouldFailItIsUnsupportedOperation() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update output field in input field with the following call updateIOField,
        // with this call hadmin tries to change field in input and add input field.
        // It's unsupported operation, hadmin can be add field with addIOField
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());

        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        algorithmIOField2.setName(algorithmIOField1.getName());
        algorithmIOField2.setType(AlgorithmFieldType.INPUT);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test059_triesToUpdateInputFieldInOutputShouldFailItIsUnsupportedOperation() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field in output field with the following call updateIOField,
        // with this call hadmin tries to change field in output and add output field.
        // It's unsupported operation, hadmin can be add field with addIOField
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());

        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        algorithmIOField2.setName(algorithmIOField1.getName());
        algorithmIOField2.setType(AlgorithmFieldType.OUTPUT);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test060_updateInputFieldShouldFailIfIOFieldIdNotExists() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but ioFieldId not exists
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        algorithmIOField2.setId(5000);
        algorithmIOField2.setName("test name......");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test061_updateInputFieldShouldWorkIfSetNewName() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but ioFieldId not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());

        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        algorithmIOField2.setName("test name......");
        algorithmIOField2.setDescription(algorithmIOField2.getDescription());
        algorithmIOField2.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField2.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);
        algorithmIOField2.setType(AlgorithmFieldType.INPUT);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(200, restResponse.getStatus());
        //checks if inputField has been updated in baseConfig
        String jsonInputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        jsonInputField.contains(
                "\"input\":[{\"id\":" + algorithmIOField1.getId() + "," +
                        "\"name\":\"" + algorithmIOField1.getName() + "\"," +
                        "\"description\":\"" + algorithmIOField1.getDescription() + "\"," +
                        "\"fieldType\":\"" + algorithmIOField1.getFieldType() + "\"," +
                        "\"multiplicity\":\"" + algorithmIOField1.getMultiplicity() + "\"," +
                        "\"type\":\"" + algorithmIOField1.getType() + "\"}," +
                        "{\"id\":" + algorithmIOField2.getId() + "," +
                        "\"name\":\"" + algorithmIOField2.getName() + "\"," +
                        "\"description\":\"" + algorithmIOField2.getDescription() + "\"," +
                        "\"fieldType\":\"" + algorithmIOField2.getFieldType() + "\"," +
                        "\"multiplicity\":\"" + algorithmIOField2.getMultiplicity() + "\"," +
                        "\"type\":\"" + algorithmIOField2.getType() + "\"}" +
                        "],\"output\":[]}"
        );
    }


    @Test
    public void test062_updateInputFieldShouldFailIfIOFieldNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but ioField not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        AlgorithmIOField algorithmIOField2 = new AlgorithmIOField();
        algorithmIOField2.setName("IOField not found");
        algorithmIOField2.setDescription("IOField not found");
        algorithmIOField2.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField2.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);
        algorithmIOField2.setType(AlgorithmFieldType.INPUT);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test063_updateInputFieldShouldFailIfIOFieldIdNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but ioFieldId not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        AlgorithmIOField algorithmIOField2 = new AlgorithmIOField();
        algorithmIOField2.setId(5000);
        algorithmIOField2.setName(algorithmIOField.getName());
        algorithmIOField2.setDescription("IOField not found");
        algorithmIOField2.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField2.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);
        algorithmIOField2.setType(AlgorithmFieldType.INPUT);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField2);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test064_updateInputFieldShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(null, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());
        algorithmIOField.setName("test name......");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(0, algorithmIOField);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test065_updateInputFieldShouldFailIfInputFieldIsDuplicated() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update input field with the following call updateIOField,
        // but input field is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        algorithmIOField.setName(algorithmIOField2.getName());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }


    /*
     *
     *
     * DELETE IOFIELD
     *
     *
     */


    @Test
    public void test066_deleteInputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin delete input field with the following call deleteIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 2,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        String jsonField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        Assert.assertEquals(jsonField, ((Algorithm) restResponse.getEntity()).getBaseConfig());
        Assert.assertTrue(jsonField.contains("{\"input\":[],\"output\":"));
    }


    @Test
    public void test067_deleteOutputFieldShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin delete output field with the following call deleteIOField
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 2,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        String jsonField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        Assert.assertEquals(jsonField, ((Algorithm) restResponse.getEntity()).getBaseConfig());
        Assert.assertTrue(jsonField.contains(",\"output\":[],\"customConfig\":null}"));
    }


    @Test
    public void test068_deleteInputFieldShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to delete input field with the following call deleteIOField,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test069_deleteOutputFieldShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to delete input field with the following call deleteIOField,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test070_deleteInputFieldShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to delete input field with the following call deleteIOField,
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AlgorithmIOField algorithmIOField = createAlgorithmIOField(null, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(0, algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test071_deleteOutputFieldShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to delete output field with the following call deleteIOField,
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AlgorithmIOField algorithmIOField = createAlgorithmIOField(null, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(0, algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test072_deleteInputFieldShouldFailIfFieldIdNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to delete input field with the following call deleteIOField,
        // but fieldId not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), 0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test073_deleteOutputFieldShouldFailIfFieldIdNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to delete output field with the following call deleteIOField,
        // but fieldId not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), 0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test074_deleteInputFieldShouldFailIfFieldTypeIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to delete input field with the following call deleteIOField,
        // but fieldType not found
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), null, algorithmIOField.getId());
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test075_deleteOutputFieldShouldFailIfFieldTypeIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to delete output field with the following call deleteIOField,
        // but fieldType not found
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), null, algorithmIOField.getId());
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test076_deleteAlgorithmIOField2ShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin delete input field (algorithmIOField2) with the following call deleteIOField;
        // if deleted has been successful jsonField contains only algorithmIOField1, algorithmIOField3
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        AlgorithmIOField algorithmIOField3 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField3.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField2.getType(), algorithmIOField2.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        Assert.assertEquals(jsonField, ((Algorithm) restResponse.getEntity()).getBaseConfig());
        // checks if algorithmIOField2 has been deleted
        // jsonField contains only algorithmIOField1, algorithmIOField3
        Assert.assertTrue(
                jsonField.contains(
                        "\"input\":[{\"id\":" + algorithmIOField1.getId() + "," +
                                "\"name\":\"" + algorithmIOField1.getName() + "\"," +
                                "\"description\":\"" + algorithmIOField1.getDescription() + "\"," +
                                "\"fieldType\":\"" + algorithmIOField1.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmIOField1.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmIOField1.getType() + "\"}," +
                                "{\"id\":" + algorithmIOField3.getId() + "," +
                                "\"name\":\"" + algorithmIOField3.getName() + "\"," +
                                "\"description\":\"" + algorithmIOField3.getDescription() + "\"," +
                                "\"fieldType\":\"" + algorithmIOField3.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmIOField3.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmIOField3.getType() + "\"}" +
                                "],\"output\":[],\"customConfig\":null}")
        );
    }


    @Test
    public void test077_deleteAlgorithmIOField2ShouldFailIfField2IsNotAnInputField() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin delete input field (algorithmIOField2) with the following call deleteIOField;
        // if deleted has been successful jsonField contains only algorithmIOField1, algorithmIOField3
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("INPUT"), algorithmIOField1.getType());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("OUTPUT"), algorithmIOField2.getType());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi
                .deleteIOField(algorithm.getId(), algorithmIOField1.getType(), algorithmIOField2.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test078_getBaseConfigShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin find BaseConfig with the following call getBaseConfig
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("INPUT"), algorithmIOField1.getType());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("OUTPUT"), algorithmIOField2.getType());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.getBaseConfig(algorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());

        Assert.assertEquals(1, ((AlgorithmConfig) restResponse.getEntity()).getInput().size());
        Assert.assertEquals(algorithmIOField1.getId(), ((AlgorithmConfig) restResponse.getEntity()).getInput().get(0).getId());
        Assert.assertEquals(algorithmIOField1.getName(), ((AlgorithmConfig) restResponse.getEntity()).getInput().get(0).getName());
        Assert.assertEquals(algorithmIOField1.getDescription(), ((AlgorithmConfig) restResponse.getEntity()).getInput().get(0).getDescription());
        Assert.assertEquals(algorithmIOField1.getFieldType(), ((AlgorithmConfig) restResponse.getEntity()).getInput().get(0).getFieldType());
        Assert.assertEquals(algorithmIOField1.getMultiplicity(), ((AlgorithmConfig) restResponse.getEntity()).getInput().get(0).getMultiplicity());
        Assert.assertEquals(algorithmIOField1.getType(), ((AlgorithmConfig) restResponse.getEntity()).getInput().get(0).getType());

        Assert.assertEquals(1, ((AlgorithmConfig) restResponse.getEntity()).getOutput().size());
        Assert.assertEquals(algorithmIOField2.getId(), ((AlgorithmConfig) restResponse.getEntity()).getOutput().get(0).getId());
        Assert.assertEquals(algorithmIOField2.getName(), ((AlgorithmConfig) restResponse.getEntity()).getOutput().get(0).getName());
        Assert.assertEquals(algorithmIOField2.getDescription(), ((AlgorithmConfig) restResponse.getEntity()).getOutput().get(0).getDescription());
        Assert.assertEquals(algorithmIOField2.getFieldType(), ((AlgorithmConfig) restResponse.getEntity()).getOutput().get(0).getFieldType());
        Assert.assertEquals(algorithmIOField2.getMultiplicity(), ((AlgorithmConfig) restResponse.getEntity()).getOutput().get(0).getMultiplicity());
        Assert.assertEquals(algorithmIOField2.getType(), ((AlgorithmConfig) restResponse.getEntity()).getOutput().get(0).getType());
    }


    @Test
    public void test079_getBaseConfigShouldWorkIfAlgorithmNotHasIOField() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin find BaseConfig with the following call getBaseConfig
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.getBaseConfig(algorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(0, ((AlgorithmConfig) restResponse.getEntity()).getInput().size());
        Assert.assertEquals(0, ((AlgorithmConfig) restResponse.getEntity()).getOutput().size());
    }


    @Test
    public void test080_getBaseConfigShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to find BaseConfig with the following call getBaseConfig,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("INPUT"), algorithmIOField1.getType());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm, "OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("OUTPUT"), algorithmIOField2.getType());

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.getBaseConfig(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test081_getBaseConfigShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to find BaseConfig with the following call getBaseConfig,
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.getBaseConfig(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test082_updateBaseConfigShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update baseConfig field with the following call updateBaseConfig
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

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

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateBaseConfig(algorithm.getId(), algorithmConfig);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 2,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        String jsonIOField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
        Assert.assertTrue(
                jsonIOField.contains(
                        "\"input\":[{\"id\":" + algorithmIOField1.getId() + "," +
                                "\"name\":\"" + algorithmIOField1.getName() + "\"," +
                                "\"description\":\"description edited in date: " + date + "\"," +
                                "\"fieldType\":\"" + algorithmIOField1.getFieldType() + "\"," +
                                "\"multiplicity\":\"" + algorithmIOField1.getMultiplicity() + "\"," +
                                "\"type\":\"" + algorithmIOField1.getType() + "\"}" +
                                "]," +
                                "\"output\":[],\"customConfig\":null}"
                )
        );
    }


    @Test
    public void test083_updateBaseConfigShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to update BaseConfig with the following call updateBaseConfig,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
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

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.updateBaseConfig(algorithm.getId(), algorithmConfig);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test084_updateBaseConfigShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update baseConfig field with the following call updateBaseConfig
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(null, "INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Date date = new Date();
        algorithmIOField1.setDescription("description edited in date: " + date);

        AlgorithmConfig algorithmConfig = new AlgorithmConfig();
        List<AlgorithmIOField> input = new ArrayList<>();
        input.add(algorithmIOField1);
        algorithmConfig.setInput(input);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateBaseConfig(0, algorithmConfig);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test085_updateBaseConfigShouldFailIfBaseConfigIsNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update baseConfig field with the following call updateBaseConfig,
        // but baseConfig is null
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateBaseConfig(algorithm.getId(), null);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test086_updateBaseConfigShouldWorkIfSetBaseConfigWithDefaultValue() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update baseConfig field with the following call updateBaseConfig,
        // and set baseConfig with default value
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setBaseConfig("{\"input\":[{\"name\":null,\"description\":null,\"fieldType\":null,\"multiplicity\":null}],\"output\":[]}");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 1,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());

        AlgorithmConfig algorithmConfig = new AlgorithmConfig();

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse1 = algorithmRestApi.updateBaseConfig(algorithm.getId(), algorithmConfig);
        Assert.assertEquals(200, restResponse1.getStatus());
        Assert.assertEquals(((Algorithm) restResponse.getEntity()).getEntityVersion() + 1,
                ((Algorithm) restResponse1.getEntity()).getEntityVersion());
        String defaultValue = "{\"input\":[],\"output\":[],\"customConfig\":null}";
        Assert.assertEquals(defaultValue, ((Algorithm) restResponse1.getEntity()).getBaseConfig());
    }


    @Test
    public void test087_updateAlgorithmFileShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin update jar file field with the following call updateAlgorithmFile
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getAlgorithmFileName());
        File algorithmFile = new File(jarPath + jarName);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi
                .updateAlgorithmFile(algorithm.getId(), algorithmResourceName, algorithmFile);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(algorithm.getEntityVersion() + 1,
                ((Algorithm) restResponse.getEntity()).getEntityVersion());
        String jarFullPath = "hdfs://namenode:8020/spark/jobs/";
        String expectedJarName = algorithm.getName().replaceAll(" ", "_").toLowerCase() + ".jar";
        Assert.assertEquals(expectedJarName,
                ((Algorithm) restResponse.getEntity()).getAlgorithmFileName());
    }


    @Test
    public void test088_updateAlgorithmFileShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to update jar file with the following call updateAlgorithmFile,
        // but huser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getAlgorithmFileName());
        File algorithmFile = new File(jarPath + jarName);

        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.updateAlgorithmFile(algorithm.getId(), algorithmResourceName, algorithmFile);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test089_updateAlgorithmFileShouldFailIfAlgorithmNotFound() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update jar file field with the following call updateAlgorithmFile,
        // but algorithm not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        File algorithmFile = new File(jarPath + jarName);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithmFile(0, algorithmResourceName, algorithmFile);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test090_updateAlgorithmFileShouldFailIfFileNotExists() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update jar file field with the following call updateAlgorithmFile,
        // but file not exists
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getAlgorithmFileName());
        File algorithmFile = new File(jarPath + "test.jar");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithmFile(algorithm.getId(), algorithmResourceName, algorithmFile);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test091_updateAlgorithmFileShouldFailIfFileOrDirectoryNotExists() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update jar file field with the following call updateAlgorithmFile,
        // but directory not exists
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getAlgorithmFileName());
        File algorithmFile = new File("/bad_path/" + jarName);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithmFile(algorithm.getId(), algorithmResourceName, algorithmFile);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test092_updateAlgorithmFileShouldFailIfDirectoryNotExists() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update jar file field with the following call updateAlgorithmFile,
        // but directory not exists
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getAlgorithmFileName());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithmFile(algorithm.getId(), algorithmResourceName, null);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test093_updateAlgorithmFileShouldFailIfFileNotSupported() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update jar file field with the following call updateAlgorithmFile,
        // but if file isn't supported
        // response status code '500' HyperIoTRuntimeException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getAlgorithmFileName());
        File algorithmFile = new File(jarPath + "karaf-keystore");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithmFile(algorithm.getId(), algorithmResourceName, algorithmFile);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0).isEmpty());
    }


    @Test
    public void test094_findAllAlgorithmPaginatedShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, hadmin find all Algorithm with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 7;
        int page = 2;
        List<Algorithm> algorithms = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        Assert.assertEquals(defaultDelta, algorithms.size());
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, page);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(defaultDelta - delta, listAlgorithms.getResults().size());
        Assert.assertEquals(delta, listAlgorithms.getDelta());
        Assert.assertEquals(page, listAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listAlgorithms.getNextPage());
        // delta is 7, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponsePage1 = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, 1);
        HyperIoTPaginableResult<Algorithm> listAlgorithmsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithmsPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listAlgorithmsPage1.getResults().size());
        Assert.assertEquals(delta, listAlgorithmsPage1.getDelta());
        Assert.assertEquals(defaultPage, listAlgorithmsPage1.getCurrentPage());
        Assert.assertEquals(page, listAlgorithmsPage1.getNextPage());
        // delta is 7, page is 1: 10 entities stored in database
        Assert.assertEquals(2, listAlgorithmsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test095_findAllAlgorithmPaginatedShouldWorkIfDeltaAndPageAreNull() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, hadmin find all Algorithm with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        List<Algorithm> algorithms = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        Assert.assertEquals(defaultDelta, algorithms.size());
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, page);
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


    @Test
    public void test096_findAllAlgorithmPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, hadmin find all Algorithm with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = -1;
        int page = 1;
        List<Algorithm> algorithms = new ArrayList<>();
        int numbEntities = 7;
        for (int i = 0; i < numbEntities; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        Assert.assertEquals(numbEntities, algorithms.size());
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, page);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listAlgorithms.getResults().size());
        Assert.assertEquals(defaultDelta, listAlgorithms.getDelta());
        Assert.assertEquals(defaultPage, listAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listAlgorithms.getNextPage());
        // default delta is 10, page is 1: 7 entities stored in database
        Assert.assertEquals(1, listAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test097_findAllAlgorithmPaginatedShouldWorkIfDeltaIsZero() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, hadmin find all Algorithm with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 0;
        int page = 2;
        List<Algorithm> algorithms = new ArrayList<>();
        int numbEntities = 16;
        for (int i = 0; i < numbEntities; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        Assert.assertEquals(numbEntities, algorithms.size());
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, page);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(numbEntities - defaultDelta, listAlgorithms.getResults().size());
        Assert.assertEquals(defaultDelta, listAlgorithms.getDelta());
        Assert.assertEquals(page, listAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listAlgorithms.getNextPage());
        // default delta is 10, page is 2: 16 entities stored in database
        Assert.assertEquals(2, listAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponsePage1 = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, 1);
        HyperIoTPaginableResult<Algorithm> listAlgorithmsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithmsPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAlgorithmsPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listAlgorithmsPage1.getDelta());
        Assert.assertEquals(defaultPage, listAlgorithmsPage1.getCurrentPage());
        Assert.assertEquals(page, listAlgorithmsPage1.getNextPage());
        // default delta is 10, page is 1: 16 entities stored in database
        Assert.assertEquals(2, listAlgorithmsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }


    @Test
    public void test098_findAllAlgorithmPaginatedShouldWorkIfPageIsLowerThanZero() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, hadmin find all Algorithm with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 4;
        int page = -1;
        List<Algorithm> algorithms = new ArrayList<>();
        int numbEntities = 6;
        for (int i = 0; i < numbEntities; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        Assert.assertEquals(numbEntities, algorithms.size());
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, page);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(delta, listAlgorithms.getResults().size());
        Assert.assertEquals(delta, listAlgorithms.getDelta());
        Assert.assertEquals(defaultPage, listAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listAlgorithms.getNextPage());
        // delta is 4, default page is 1: 6 entities stored in database
        Assert.assertEquals(2, listAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 2
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponsePage2 = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS, delta, 2);
        HyperIoTPaginableResult<Algorithm> listAlgorithmsPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithmsPage2.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listAlgorithmsPage2.getResults().size());
        Assert.assertEquals(delta, listAlgorithmsPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listAlgorithmsPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listAlgorithmsPage2.getNextPage());
        // delta is 4, page is 2: 6 entities stored in database
        Assert.assertEquals(2, listAlgorithmsPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }


    @Test
    public void test099_findAllAlgorithmPaginatedShouldWorkIfPageIsZero() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, hadmin find all Algorithm with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        int delta = 5;
        int page = 0;
        List<Algorithm> algorithms = new ArrayList<>();
        int numbEntities = 4;
        for (int i = 0; i < numbEntities; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        Assert.assertEquals(numbEntities, algorithms.size());
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,delta, page);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertFalse(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listAlgorithms.getResults().size());
        Assert.assertEquals(delta, listAlgorithms.getDelta());
        Assert.assertEquals(defaultPage, listAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listAlgorithms.getNextPage());
        // delta is 5, default page is 1: 4 entities stored in database
        Assert.assertEquals(1, listAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test100_findAllAlgorithmPaginatedShouldFailIfNotLogged() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // the following call tries to find all Algorithm with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(algorithmRestApi, null);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test101_findAllAlgorithmPaginatedShouldWorkIfListIsEmpty() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, hadmin find all Algorithm with pagination.
        // there are no entities saved in the database, this call return an empty list
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(AlgorithmType.STATISTICS,defaultDelta, defaultPage);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertTrue(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(0, listAlgorithms.getResults().size());
        Assert.assertEquals(defaultDelta, listAlgorithms.getDelta());
        Assert.assertEquals(defaultPage, listAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listAlgorithms.getNextPage());
        // default delta is 10, default page is 1: there are not entities stored in database
        Assert.assertEquals(0, listAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test102_saveAlgorithmShouldFailIfEntityIsDuplicated() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but algorithm is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm1 = createAlgorithm();
        Assert.assertNotEquals(0, algorithm1.getId());

        Algorithm algorithm2 = new Algorithm();
        algorithm2.setName(algorithm1.getName());
        algorithm2.setDescription("defined by huser " + adminUser.getUsername());
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm2.setBaseConfig("{}");
        algorithm2.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm2);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }


    @Test
    public void test103_updateAlgorithmShouldFailIfEntityIsDuplicated() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call saveAlgorithm,
        // but algorithm is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm1 = createAlgorithm();
        Assert.assertNotEquals(0, algorithm1.getId());

        Algorithm algorithm2 = createAlgorithm();
        Assert.assertNotEquals(0, algorithm2.getId());

        Assert.assertNotEquals(algorithm1.getId(), algorithm2.getId());
        Assert.assertNotEquals(algorithm1.getName(), algorithm2.getName());

        // set in algorithm2 equals name of algorithm1
        algorithm2.setName(algorithm1.getName());
        Assert.assertEquals(algorithm1.getName(), algorithm2.getName());

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm2);
        Assert.assertEquals(409, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
                nameIsDuplicated = true;
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }

    @Test
    public void test104_saveAlgorithmShouldFailIfNameIsGreaterThan255Chars() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // but name is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName(createStringFieldWithSpecifiedLenght(256));
        algorithm.setDescription("defined by huser " + adminUser.getUsername());
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test105_updateAlgorithmShouldFailIfNameIsGreaterThan255Chars() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // but name is greater than 255 chars
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setName(createStringFieldWithSpecifiedLenght(256));

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("algorithm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(algorithm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test106_saveAlgorithmShouldWorkWhenDescriptionIs2999Chars() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to save Algorithm with the following call saveAlgorithm,
        // description is 2999 chars
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription(createStringFieldWithSpecifiedLenght(2999));
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");
        algorithm.setType(AlgorithmType.STATISTICS);

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(((Algorithm) restResponse.getEntity()).getDescription().length(), 2999);
    }

    @Test
    public void test107_updateAlgorithmShouldWorkWhenDescriptionIs2999Chars() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // hadmin tries to update Algorithm with the following call updateAlgorithm,
        // description is 2999 chars
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Algorithm algorithm = createAlgorithm();
        algorithm.setDescription(createStringFieldWithSpecifiedLenght(2999));
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(((Algorithm) restResponse.getEntity()).getDescription().length(), 2999);
    }




    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    private String createStringFieldWithSpecifiedLenght(int length) {
        String symbol = "a";
        String field = String.format("%" + length + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(length, field.length());
        return field;
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


    private String testMaxLength(int maxLength) {
        String symbol = "a";
        String descriptionLength = String.format("%" + maxLength + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(maxLength, descriptionLength.length());
        return descriptionLength;
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


}

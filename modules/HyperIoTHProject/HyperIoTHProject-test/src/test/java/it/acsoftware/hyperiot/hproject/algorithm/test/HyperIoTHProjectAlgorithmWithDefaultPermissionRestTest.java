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


import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.algorithm.service.rest.AlgorithmRestApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmUtil;
import it.acsoftware.hyperiot.hproject.algorithm.job.HProjectAlgorithmJob;
import it.acsoftware.hyperiot.hproject.algorithm.model.*;
import it.acsoftware.hyperiot.hproject.algorithm.service.rest.HProjectAlgorithmRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntityRepository;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi;
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
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static it.acsoftware.hyperiot.hproject.algorithm.test.HyperIoTHProjectAlgorithmConfiguration.*;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHProjectAlgorithmWithDefaultPermissionRestTest extends KarafTestSupport {


    //force global config
    @Override
    public Option[] config() {
        return null;
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
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_hprojectAlgorithmModuleShouldWorkIfHUserIsNotActive() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call checkModuleWorking checks if HProjectAlgorithm module working
        // correctly, if HUser not active
        huser = huserWithDefaultPermissionInHyperIoTFramework(false);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertFalse(huser.isActive());
        this.impersonateUser(hProjectAlgorithmRestService, null);
        Response restResponse = hProjectAlgorithmRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("HProjectAlgorithm Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_saveHProjectAlgorithmWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser, with default permission, save HProjectAlgorithm with the following call saveHProjectAlgorithm
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hProjectAlgorithmRestService, huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HProject project = createHProject(huser);
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String cronExpression = hProjectAlgorithmTemplate.getCronExpression();
        String config = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(200, restResponse.getStatus());
        assertSavedHProjectAlgorithm(restResponse, project, algorithm, hprojectAlgorithmName, config, cronExpression);
    }

    @Test
    public void test003_saveHProjectAlgorithmWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2, with default permission, save HProjectAlgorithm with the following call saveHProjectAlgorithm
        //but hproject related to hproject algorithms is not own or shared with huser2
        // response status code 403 HyperIoTUnauthorizedException
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hProjectAlgorithmRestService, huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HProject project = createHProject(huser);
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertFalse(userIsInHProjectSharingUserList(project, huser2));
        Assert.assertNotEquals(project.getUserOwner().getUsername(), huser2.getUsername());
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(403, restResponse.getStatus());
    }

    @Test
    public void test004_saveHProjectAlgorithmWithDefaultPermissionShouldWorkIfUserNotOwnsProjectButProjectIsSharedWithHim() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2, with default permission, save HProjectAlgorithm with the following call saveHProjectAlgorithm
        // the hproject related to the hproject algorithm is shared with huser2.
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hProjectAlgorithmRestService, huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HProject project = createHProject(huser);
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        createSharedEntity(project, huser, huser2);
        Assert.assertTrue(userIsInHProjectSharingUserList(project, huser2));
        Assert.assertNotEquals(project.getUserOwner().getUsername(), huser2.getUsername());
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test005_findHProjectAlgorithmWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser find HProjectAlgorithm with the following call findHProjectAlgorithm
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HProject project = createHProject(huser);
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
        HProjectAlgorithm savedHProjectAlgorithm = (HProjectAlgorithm) restResponse.getEntity();

        Response findResponse = hProjectAlgorithmRestService.findHProjectAlgorithm(savedHProjectAlgorithm.getId());
        Assert.assertEquals(200, findResponse.getStatus());
        Assert.assertEquals(((HProjectAlgorithm) findResponse.getEntity()).getId(), savedHProjectAlgorithm.getId());
    }

    @Test
    public void test006_findHProjectAlgorithmWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 find HProjectAlgorithm with the following call findHProjectAlgorithm
        //but hproject related to hproject algorithms is not own or shared with huser2
        // response status code '404' HyperIoTEntityNotFound
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HProject project = createHProject(huser);
        HDevice device = createHDevice(project);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.findHProjectAlgorithm(hProjectAlgorithm.getId());
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test007_findHProjectAlgorithmWithDefaultPermissionShouldWorkIfUserNotOwnsProjectButProjectIsSharedWithHim() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 find HProjectAlgorithm with the following call findHProjectAlgorithm
        // the hproject related to the hproject algorithm is shared with huser2.
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HProject project = createHProject(huser);
        HDevice device = createHDevice(project);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        createSharedEntity(project, huser, huser2);
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.findHProjectAlgorithm(hProjectAlgorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hProjectAlgorithm.getId(), ((HProjectAlgorithm) restResponse.getEntity()).getId());
    }

    @Test
    public void test008_findAllHProjectWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hUser find all HProjectAlgorithm with the following call findAllHProjectAlgorithm
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
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
        HProjectAlgorithm savedHProjectAlgorithm = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertNotEquals(0, savedHProjectAlgorithm.getId());

        Response findAllHProjectAlgorithmResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithm();
        Assert.assertEquals(200, findAllHProjectAlgorithmResponse.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList = findAllHProjectAlgorithmResponse.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertFalse(hProjectAlgorithmsList.isEmpty());
        Assert.assertEquals(1, hProjectAlgorithmsList.size());
        boolean hprojectFound = false;
        for (HProjectAlgorithm projectAlgorithm : hProjectAlgorithmsList) {
            if (savedHProjectAlgorithm.getId() == projectAlgorithm.getId()) {
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
    }

    @Test
    public void test009_findAllHProjectWithDefaultPermissionShouldReturnOnlyOwnedOrSharedHProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 find all HProjectAlgorithm with the following call findAllHProjectAlgorithm
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser2);
        //huser2 not own/share project
        Response restResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithm();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList = restResponse.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertTrue(hProjectAlgorithmsList.isEmpty());

        //huser share project with huser2
        createSharedEntity(project, huser, huser2);
        Assert.assertTrue(userIsInHProjectSharingUserList(project, huser2));
        Response restResponse1 = hProjectAlgorithmRestService.findAllHProjectAlgorithm();
        Assert.assertEquals(200, restResponse1.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList1 = restResponse1.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertFalse(hProjectAlgorithmsList1.isEmpty());
        Assert.assertEquals(1, hProjectAlgorithmsList1.size());
        Assert.assertTrue(hProjectAlgorithmsList1.stream().map(HProjectAlgorithm::getId).collect(Collectors.toList()).contains(hProjectAlgorithm.getId()));

        //create hproject with huser2 and create an hproject algorithm related to this project.
        HProject project2 = createHProject(huser2);
        Assert.assertNotEquals(0, project2.getId());
        HProjectAlgorithm hProjectAlgorithm2 = createHProjectAlgorithm(project2);
        Assert.assertNotEquals(0, hProjectAlgorithm2.getId());

        //find all hprojectalgorithms and assert that project2 and project is present.
        Response restResponse2 = hProjectAlgorithmRestService.findAllHProjectAlgorithm();
        Assert.assertEquals(200, restResponse2.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList2 = restResponse2.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertFalse(hProjectAlgorithmsList2.isEmpty());
        Assert.assertEquals(2, hProjectAlgorithmsList2.size());
        Assert.assertTrue(hProjectAlgorithmsList2.stream().map(HProjectAlgorithm::getId).collect(Collectors.toList()).contains(hProjectAlgorithm.getId()));
        Assert.assertTrue(hProjectAlgorithmsList2.stream().map(HProjectAlgorithm::getId).collect(Collectors.toList()).contains(hProjectAlgorithm2.getId()));

        //Remove the project created by huser2(Because this is not included in the after test).
        HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
        impersonateUser(hProjectRestService, huser2);
        Response deleteProjectResponse = hProjectRestService.deleteHProject(project2.getId());
        Assert.assertEquals(200, deleteProjectResponse.getStatus());
    }

    @Test
    public void test010_updateHProjectAlgorithmWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hUser update HProjectAlgorithm with the following call updateHProjectAlgorithm
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        String projectAlgorithmNewName = projectAlgorithm.getName().concat("newName");
        projectAlgorithm.setName(projectAlgorithmNewName);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(projectAlgorithm.getEntityVersion() + 1,
                (((HProjectAlgorithm) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals(projectAlgorithmNewName,
                (((HProjectAlgorithm) restResponse.getEntity()).getName()));
        Assert.assertEquals(projectAlgorithm.getId(), ((HProjectAlgorithm) restResponse.getEntity()).getId());
    }

    @Test
    public void test011_updateHProjectAlgorithmWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 update HProjectAlgorithm with the following call updateHProjectAlgorithm
        //but hproject related to hproject algorithms is not own or shared with huser2
        // response status code '403' HyperIoTUnauthorized
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        String projectAlgorithmNewName = projectAlgorithm.getName().concat("newName");
        projectAlgorithm.setName(projectAlgorithmNewName);
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(403, restResponse.getStatus());
    }

    @Test
    public void test012_updateHProjectAlgorithmWithDefaultPermissionShouldWorkIfUserNotOwnsProjectButProjectIsSharedWithHim() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 update HProjectAlgorithm with the following call updateHProjectAlgorithm
        // the hproject related to the hproject algorithm is shared with huser2.
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        String projectAlgorithmNewName = projectAlgorithm.getName().concat("newName");
        projectAlgorithm.setName(projectAlgorithmNewName);
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        createSharedEntity(project, huser, huser2);
        Response restResponse = hProjectAlgorithmRestService.updateHProjectAlgorithm(projectAlgorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(projectAlgorithm.getEntityVersion() + 1,
                (((HProjectAlgorithm) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals(projectAlgorithmNewName,
                (((HProjectAlgorithm) restResponse.getEntity()).getName()));
        Assert.assertEquals(projectAlgorithm.getId(), ((HProjectAlgorithm) restResponse.getEntity()).getId());
    }

    @Test
    public void test013_deleteHProjectAlgorithmWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hUser delete HProjectAlgorithm with the following call deleteHProject
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Response restResponse = hProjectAlgorithmRestService.deleteHProjectAlgorithm(hProjectAlgorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test014_deleteHProjectAlgorithmWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hUser2 delete HProjectAlgorithm with the following call deleteHProject
        //but hproject related to hproject algorithms is not own or shared with hUser2
        // response status code '404' HyperIoTEntityNotFound, because user cannot retrieve the HProjectAlgorithms.
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        HUser hUser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, hUser2);
        Response restResponse = hProjectAlgorithmRestService.deleteHProjectAlgorithm(hProjectAlgorithm.getId());
        Assert.assertEquals(404, restResponse.getStatus());
    }

    @Test
    public void test015_deleteHProjectAlgorithmWithDefaultPermissionShouldWorkIfUserNotOwnsProjectButProjectIsSharedWithHim() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 delete HProjectAlgorithm with the following call deleteHProject
        // the hproject related to the hproject algorithm is shared with huser2.
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        createSharedEntity(project, huser, huser2);
        Assert.assertTrue(userIsInHProjectSharingUserList(project, huser2));
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.deleteHProjectAlgorithm(hProjectAlgorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test016_findAllHProjectAlgorithmPaginatedWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmPaginated, hUser finds all
        // HProjectsAlgorithms with pagination
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        int delta = 5;
        int page = 2;
        List<HProjectAlgorithm> hProjectAlgorithms = new ArrayList<>();
        int numbEntities = 9;
        for (int i = 0; i < numbEntities; i++) {
            HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
            Assert.assertNotEquals(0, hProjectAlgorithm.getId());
            hProjectAlgorithms.add(hProjectAlgorithm);
        }
        Assert.assertEquals(numbEntities, hProjectAlgorithms.size());
        Response restResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithms.getResults().isEmpty());
        Assert.assertEquals(numbEntities - delta, listHProjectAlgorithms.getResults().size());
        Assert.assertEquals(delta, listHProjectAlgorithms.getDelta());
        Assert.assertEquals(page, listHProjectAlgorithms.getCurrentPage());
        Assert.assertEquals(defaultPage, listHProjectAlgorithms.getNextPage());
        // delta is 5, page 2: 9 entities stored in database
        Assert.assertEquals(2, listHProjectAlgorithms.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        impersonateUser(hProjectAlgorithmRestService, huser);
        Response restResponsePage1 = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, 1);
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithmsPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertFalse(listHProjectAlgorithmsPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listHProjectAlgorithmsPage1.getResults().size());
        Assert.assertEquals(delta, listHProjectAlgorithmsPage1.getDelta());
        Assert.assertEquals(defaultPage, listHProjectAlgorithmsPage1.getCurrentPage());
        Assert.assertEquals(page, listHProjectAlgorithmsPage1.getNextPage());
        // delta is 5, page is 1: 9 entities stored in database
        Assert.assertEquals(2, listHProjectAlgorithmsPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    //Implement this test.
    @Test
    public void test017_findAllHProjectAlgorithmPaginatedWithDefaultPermissionReturnOnlyProjectAlgorithmRelatedToProjectSharedWithUser() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // In this following call findAllHProjectAlgorithmPaginated, huser finds all
        // HProjectsAlgorithms with pagination
        //Assert that only hproject algorithms relative to project that is shared with huser2 is returned.
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        Assert.assertNotEquals(0, project.getId());
        HProjectAlgorithm projectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, projectAlgorithm.getId());
        int delta = 10;
        int page = 1;
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, page);
        Assert.assertEquals(200, restResponse.getStatus());
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertEquals(listHProjectAlgorithms.getCurrentPage(), 1);
        Assert.assertEquals(listHProjectAlgorithms.getDelta(), 10);
        Assert.assertEquals(listHProjectAlgorithms.getNextPage(), 1);
        Assert.assertEquals(listHProjectAlgorithms.getNumPages(), 0);
        Assert.assertTrue(listHProjectAlgorithms.getResults().isEmpty());

        //Create another project and another hproject algorithm and share this project with huser2
        HProject project2 = createHProject(huser);
        Assert.assertNotEquals(0, project2.getId());
        HProjectAlgorithm hProjectAlgorithm2 = createHProjectAlgorithm(project2);
        Assert.assertNotEquals(0, hProjectAlgorithm2.getId());
        createSharedEntity(project2, huser, huser2);

        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse2 = hProjectAlgorithmRestService.findAllHProjectAlgorithmPaginated(delta, page);
        Assert.assertEquals(200, restResponse2.getStatus());
        HyperIoTPaginableResult<HProjectAlgorithm> listHProjectAlgorithms2 = restResponse2
                .readEntity(new GenericType<HyperIoTPaginableResult<HProjectAlgorithm>>() {
                });
        Assert.assertEquals(listHProjectAlgorithms2.getCurrentPage(), 1);
        Assert.assertEquals(listHProjectAlgorithms2.getDelta(), 10);
        Assert.assertEquals(listHProjectAlgorithms2.getNextPage(), 1);
        Assert.assertEquals(listHProjectAlgorithms2.getNumPages(), 1);
        Assert.assertFalse(listHProjectAlgorithms2.getResults().isEmpty());
        Assert.assertEquals(listHProjectAlgorithms2.getResults().size(), 1);
        Assert.assertTrue(listHProjectAlgorithms2.getResults().stream().map(HProjectAlgorithm::getId).collect(Collectors.toList()).contains(hProjectAlgorithm2.getId()));

    }

    @Test
    public void test018_findByHProjectIdWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // hUser find HProjectAlgorithm with the following call findByHProjectId
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        Response restResponse = hProjectAlgorithmRestService.findByHProjectId(hProjectAlgorithm.getProject().getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList = restResponse.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertFalse(hProjectAlgorithmsList.isEmpty());
        Assert.assertEquals(1, hProjectAlgorithmsList.size());
        boolean hprojectFound = false;
        for (HProjectAlgorithm projectAlgorithm : hProjectAlgorithmsList) {
            if (hProjectAlgorithm.getId() == projectAlgorithm.getId()) {
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
    }

    @Test
    public void test019_findByHProjectIdWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 find HProjectAlgorithm with the following call findByHProjectId
        // but huser2 not own and not is in project's shared user list.
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertFalse(userIsInHProjectSharingUserList(project, huser2));
        impersonateUser(hProjectAlgorithmRestService, huser2);
        //huser2 not own/share project
        Response restResponse = hProjectAlgorithmRestService.findByHProjectId(project.getId());
        Assert.assertEquals(403, restResponse.getStatus());
    }

    @Test
    public void test020_findByHProjectIdWithDefaultPermissionWithDefaultPermissionWhenProjectIsSharedWithUserReturnProjectAlgorithms() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 find HProjectAlgorithm with the following call findByHProjectId
        // assert that huser2 can find project'algorithms related to project shared with him.
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        //huser share project with huser2
        createSharedEntity(project, huser, huser2);
        Assert.assertTrue(userIsInHProjectSharingUserList(project, huser2));
        Response restResponse1 = hProjectAlgorithmRestService.findByHProjectId(project.getId());
        Assert.assertEquals(200, restResponse1.getStatus());
        List<HProjectAlgorithm> hProjectAlgorithmsList1 = restResponse1.readEntity(new GenericType<List<HProjectAlgorithm>>() {
        });
        Assert.assertFalse(hProjectAlgorithmsList1.isEmpty());
        Assert.assertEquals(1, hProjectAlgorithmsList1.size());
        Assert.assertTrue(hProjectAlgorithmsList1.stream().map(HProjectAlgorithm::getId).collect(Collectors.toList()).contains(hProjectAlgorithm.getId()));
    }


    @Test
    public void test021_updateBaseConfigWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser with the following call tries to update HProjectAlgorithm config field  with updateBaseConfig
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        String hprojectAlgorithmConfig = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmCronExpression = hProjectAlgorithmTemplate.getCronExpression();
        Response saveAlgorithmResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(saveAlgorithmResponse, project, algorithm, hprojectAlgorithmName, hprojectAlgorithmConfig, hprojectAlgorithmCronExpression);
        HProjectAlgorithm savedAlgorithm = (HProjectAlgorithm) saveAlgorithmResponse.getEntity();
        Assert.assertNotEquals(0, savedAlgorithm.getId());

        //Crea a new device with a new packet and a new field to create a new HProjectAlgorithConfig.
        HDevice anotherDevice = createHDevice(project);
        HPacket otherPacket = createHPacketAndAddHPacketField(anotherDevice, true);
        Assert.assertTrue(otherPacket.getFields() != null && otherPacket.getFields().size() == 1);
        HPacketField otherPacketField = otherPacket.getFields().iterator().next();

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
        HProjectAlgorithmConfig newConfig = new HProjectAlgorithmConfig();
        newConfig.setInput(new ArrayList<>());
        newConfig.setOutput(new ArrayList<>());
        newConfig.getOutput().add(newOutputField);
        newConfig.getInput().add(hProjectAlgorithmInputField);


        HProjectAlgorithmUtil hProjectAlgorithmUtil = getOsgiService(HProjectAlgorithmUtil.class);
        String expectedConfig = null;
        try {
            expectedConfig = hProjectAlgorithmUtil.getConfigString(newConfig);
        } catch (Throwable exc) {
            Assert.fail();
            throw new RuntimeException();
        }
        //Assert that we update the config.
        Assert.assertNotEquals(savedAlgorithm.getConfig(), expectedConfig);

        this.impersonateUser(hProjectAlgorithmRestService, huser);
        Response restResponse = hProjectAlgorithmRestService.updateBaseConfig(savedAlgorithm.getId(), newConfig);
        Assert.assertEquals(200, restResponse.getStatus());
        HProjectAlgorithm entityResponse = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertEquals(entityResponse.getId(), savedAlgorithm.getId());
        //Assert that the config is how we expected.
        Assert.assertEquals(expectedConfig, entityResponse.getConfig());
    }

    @Test
    public void test022_updateBaseConfigWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser 2 with the following call tries to update HProjectAlgorithm config field  with updateBaseConfig
        //but hproject related to the hprojectalgorithm is not shared with huser2
        // response status code 403 HyperIoTUnauthorizedException
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        String hprojectAlgorithmConfig = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmCronExpression = hProjectAlgorithmTemplate.getCronExpression();
        Response saveAlgorithmResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(saveAlgorithmResponse, project, algorithm, hprojectAlgorithmName, hprojectAlgorithmConfig, hprojectAlgorithmCronExpression);
        HProjectAlgorithm savedAlgorithm = (HProjectAlgorithm) saveAlgorithmResponse.getEntity();
        Assert.assertNotEquals(0, savedAlgorithm.getId());

        //Crea a new device with a new packet and a new field to create a new HProjectAlgorithConfig.
        HDevice anotherDevice = createHDevice(project);
        HPacket otherPacket = createHPacketAndAddHPacketField(anotherDevice, true);
        Assert.assertTrue(otherPacket.getFields() != null && otherPacket.getFields().size() == 1);
        HPacketField otherPacketField = otherPacket.getFields().iterator().next();

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
        HProjectAlgorithmConfig newConfig = new HProjectAlgorithmConfig();
        newConfig.setInput(new ArrayList<>());
        newConfig.setOutput(new ArrayList<>());
        newConfig.getOutput().add(newOutputField);
        newConfig.getInput().add(hProjectAlgorithmInputField);


        HProjectAlgorithmUtil hProjectAlgorithmUtil = getOsgiService(HProjectAlgorithmUtil.class);
        String expectedConfig = null;
        try {
            expectedConfig = hProjectAlgorithmUtil.getConfigString(newConfig);
        } catch (Throwable exc) {
            Assert.fail();
            throw new RuntimeException();
        }
        //Assert that we update the config.
        Assert.assertNotEquals(savedAlgorithm.getConfig(), expectedConfig);

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        this.impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.updateBaseConfig(savedAlgorithm.getId(), newConfig);
        Assert.assertEquals(403, restResponse.getStatus());
    }

    @Test
    public void test023_updateBaseConfigWithDefaultPermissionShouldWorkIfUserNotOwnsProjectButProjectIsSharedWithHim() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 with the following call tries to update HProjectAlgorithm config field  with updateBaseConfig
        //the project related to the hproject's algorithm is shared with huser2
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        impersonateUser(hProjectAlgorithmRestService, huser);
        HProject project = createHProject(huser);
        Algorithm algorithm = createAlgorithmWithInputAndOutputField();
        HDevice device = createHDevice(project);
        HPacket packet = createHPacketAndAddHPacketField(device, true);
        Assert.assertTrue(packet.getFields() != null && packet.getFields().size() == 1);
        HPacketField packetField = packet.getFields().iterator().next();
        HProjectAlgorithm hProjectAlgorithmTemplate = createHProjectAlgorithmTemplate(project, algorithm, packet, packetField);
        String hprojectAlgorithmName = hProjectAlgorithmTemplate.getName();
        String hprojectAlgorithmConfig = hProjectAlgorithmTemplate.getConfig();
        String hprojectAlgorithmCronExpression = hProjectAlgorithmTemplate.getCronExpression();
        Response saveAlgorithmResponse = hProjectAlgorithmRestService.saveHProjectAlgorithm(hProjectAlgorithmTemplate);
        assertSavedHProjectAlgorithm(saveAlgorithmResponse, project, algorithm, hprojectAlgorithmName, hprojectAlgorithmConfig, hprojectAlgorithmCronExpression);
        HProjectAlgorithm savedAlgorithm = (HProjectAlgorithm) saveAlgorithmResponse.getEntity();
        Assert.assertNotEquals(0, savedAlgorithm.getId());

        //Crea a new device with a new packet and a new field to create a new HProjectAlgorithConfig.
        HDevice anotherDevice = createHDevice(project);
        HPacket otherPacket = createHPacketAndAddHPacketField(anotherDevice, true);
        Assert.assertTrue(otherPacket.getFields() != null && otherPacket.getFields().size() == 1);
        HPacketField otherPacketField = otherPacket.getFields().iterator().next();

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
        HProjectAlgorithmConfig newConfig = new HProjectAlgorithmConfig();
        newConfig.setInput(new ArrayList<>());
        newConfig.setOutput(new ArrayList<>());
        newConfig.getOutput().add(newOutputField);
        newConfig.getInput().add(hProjectAlgorithmInputField);


        HProjectAlgorithmUtil hProjectAlgorithmUtil = getOsgiService(HProjectAlgorithmUtil.class);
        String expectedConfig = null;
        try {
            expectedConfig = hProjectAlgorithmUtil.getConfigString(newConfig);
        } catch (Throwable exc) {
            Assert.fail();
            throw new RuntimeException();
        }
        //Assert that we update the config.
        Assert.assertNotEquals(savedAlgorithm.getConfig(), expectedConfig);

        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        //huser share project with huser2
        createSharedEntity(project, huser, huser2);
        Assert.assertTrue(userIsInHProjectSharingUserList(project, huser2));
        this.impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.updateBaseConfig(savedAlgorithm.getId(), newConfig);
        Assert.assertEquals(200, restResponse.getStatus());
        HProjectAlgorithm entityResponse = (HProjectAlgorithm) restResponse.getEntity();
        Assert.assertEquals(entityResponse.getId(), savedAlgorithm.getId());
        //Assert that the config is how we expected.
        Assert.assertEquals(expectedConfig, entityResponse.getConfig());
    }

    @Test
    public void test024_getAlgorithmOutputsWithDefaultPermissionShouldWork() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // the following call tries to find HProjectAlgorithmHBaseResult with getAlgorithmOutput
        // response status code '200' HyperIoTRuntimeException
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        impersonateUser(hProjectAlgorithmRestService, huser);
        forceAlgorithmTableCreation(hProjectAlgorithm);
        Response restResponse = hProjectAlgorithmRestService.getAlgorithmOutputs(hProjectAlgorithm.getProject().getId(), hProjectAlgorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotNull(restResponse.getEntity());
        Assert.assertTrue(((HProjectAlgorithmHBaseResult) restResponse.getEntity()).getRows().isEmpty());
    }

    @Test
    public void test025_getAlgorithmOutputsWithDefaultPermissionShouldFailIfUserNotOwnsProjectAndUserNotShareProject() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 with the following call tries to find HProjectAlgorithmHBaseResult
        // but hproject related to the hprojectalgorithm is not shared with huser2
        // response status code '403' HyperIoTUnauthorizedException
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        forceAlgorithmTableCreation(hProjectAlgorithm);
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(project.getUser().getId(), huser2.getId());
        Assert.assertFalse(userIsInHProjectSharingUserList(project, huser2));
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.getAlgorithmOutputs(hProjectAlgorithm.getProject().getId(), hProjectAlgorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(((HyperIoTBaseError) restResponse.getEntity()).getType(), hyperIoTException + "HyperIoTUnauthorizedException");
    }

    @Test
    public void test026_getAlgorithmOutputsWithDefaultPermissionShouldWorkIfUserNotOwnsProjectButProjectIsSharedWithHim() {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        // huser2 with the following call tries to find HProjectAlgorithmHBaseResult
        // the project related to the hproject's algorithm is shared with huser2
        // response status code '200'
        huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        HProject project = createHProject(huser);
        HProjectAlgorithm hProjectAlgorithm = createHProjectAlgorithm(project);
        Assert.assertNotEquals(0, hProjectAlgorithm.getId());
        Assert.assertNotNull(hProjectAlgorithm.getProject());
        Assert.assertNotEquals(0, hProjectAlgorithm.getProject().getId());
        forceAlgorithmTableCreation(hProjectAlgorithm);
        HUser huser2 = huserWithDefaultPermissionInHyperIoTFramework(true);
        createSharedEntity(project, huser, huser2);
        Assert.assertNotEquals(project.getUser().getId(), huser2.getId());
        Assert.assertTrue(userIsInHProjectSharingUserList(project, huser2));
        impersonateUser(hProjectAlgorithmRestService, huser2);
        Response restResponse = hProjectAlgorithmRestService.getAlgorithmOutputs(hProjectAlgorithm.getProject().getId(), hProjectAlgorithm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotNull(restResponse.getEntity());
        Assert.assertTrue(((HProjectAlgorithmHBaseResult) restResponse.getEntity()).getRows().isEmpty());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

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
    }

    private boolean userIsInHProjectSharingUserList(HProject project, HUser sharingUser) {
        List<HyperIoTUser> projectSharingUser = getProjectSharingUser(project);
        if (projectSharingUser.isEmpty()) {
            return false;
        }
        return projectSharingUser.stream().map(HyperIoTUser::getUsername).collect(Collectors.toList()).contains(sharingUser.getUsername());
    }

    private List<HyperIoTUser> getProjectSharingUser(HProject project) {
        Assert.assertNotEquals(0, project.getId());
        SharedEntityRepository sharedEntityRepository = getOsgiService(SharedEntityRepository.class);
        return sharedEntityRepository.getSharingUsers(hProjectResourceName, project.getId());
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


    private HProjectAlgorithm createHProjectAlgorithm(HProject project) {
        HProjectAlgorithmRestApi hProjectAlgorithmRestService = getOsgiService(HProjectAlgorithmRestApi.class);
        this.impersonateUser(hProjectAlgorithmRestService, project.getUserOwner());
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
        huser.setPassword(defaultPasswordForUser);
        huser.setPasswordConfirm(defaultPasswordForUser);
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
            boolean resourceNameHProject = false;
            boolean resourceNameHProjectAlgorithm = false;
            for (Permission permission : listPermissions) {
                if (permission.getEntityResourceName().contains(permissionHProject)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionHProject, permission.getEntityResourceName());
                    Assert.assertEquals(permissionHProject + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameHProject = true;
                }
                if (permission.getEntityResourceName().contains(permissionHProjectAlgorithm)) {
                    Assert.assertNotEquals(0, permission.getId());
                    Assert.assertEquals(permissionHProjectAlgorithm, permission.getEntityResourceName());
                    Assert.assertEquals(permissionHProjectAlgorithm + nameRegisteredPermission, permission.getName());
                    Assert.assertEquals(role.getName(), permission.getRole().getName());
                    resourceNameHProjectAlgorithm = true;
                }
            }
            Assert.assertTrue(resourceNameHProject);
            Assert.assertTrue(resourceNameHProjectAlgorithm);
        }
        return huser;
    }

    private SharedEntity createSharedEntity(HyperIoTBaseEntity hyperIoTBaseEntity, HUser ownerUser, HUser huser) {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);

        SharedEntity sharedEntity = new SharedEntity();
        sharedEntity.setEntityId(hyperIoTBaseEntity.getId());
        sharedEntity.setEntityResourceName(hyperIoTBaseEntity.getResourceName()); // "it.acsoftware.hyperiot.shared.entity.example.HyperIoTSharedEntityExample"
        sharedEntity.setUserId(huser.getId());

        if (ownerUser == null) {
            AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
            ownerUser = (HUser) authService.login("hadmin", "admin");
            Assert.assertTrue(ownerUser.isAdmin());
        }
        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hyperIoTBaseEntity.getId(), ((SharedEntity) restResponse.getEntity()).getEntityId());
        Assert.assertEquals(huser.getId(), ((SharedEntity) restResponse.getEntity()).getUserId());
        Assert.assertEquals(hyperIoTBaseEntity.getResourceName(), ((SharedEntity) restResponse.getEntity()).getEntityResourceName());
        return sharedEntity;
    }


    private HProject createHProject(HUser huser) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HProject hproject = new HProject();
        hproject.setName("Project " + UUID.randomUUID());
        hproject.setDescription("Project of user: " + huser.getUsername());
        hproject.setUser(huser);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals(hproject.getDescription(), ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
    }

    private HDevice createHDevice(HProject hproject) {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HUser ownerHUser = hproject.getUser();
        HDevice hdevice = new HDevice();
        hdevice.setBrand("ACSoftware");
        hdevice.setDescription("Property of: " + hproject.getUser().getUsername());
        hdevice.setDeviceName("deviceName" + UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(hproject);
        this.impersonateUser(hDeviceRestApi, ownerHUser);
        Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals("ACSoftware",
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
        Assert.assertEquals(ownerHUser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
    }

    private HPacket createHPacketAndAddHPacketField(HDevice hdevice, boolean createField) {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);

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

        HUser ownerHUser = hdevice.getProject().getUser();

        this.impersonateUser(hPacketRestApi, hdevice.getProject().getUser());
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(), ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(ownerHUser.getId(), ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());

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
            this.impersonateUser(hPacketRestApi, ownerHUser);
            Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
            Assert.assertEquals(200, responseAddField1.getStatus());
            Assert.assertEquals(hpacket.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());
            Assert.assertEquals(hdevice.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getId());
            Assert.assertEquals(hdevice.getProject().getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getId());
            Assert.assertEquals(ownerHUser.getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getDevice().getProject().getUser().getId());

            //check restResponse field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getFields().iterator().next().getId(), ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getId(), ((HPacketField) responseAddField1.getEntity()).getPacket().getId());

            Assert.assertEquals(1, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        return hpacket;
    }

    // HProject is Owned Resource: only huser  is able to find/findAll his entities
    private HUser huser;

    @After
    public void afterTest() {

        // Remove projects in every test
        if ((huser != null) && (huser.isActive())) {
            HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
            this.impersonateUser(hprojectRestService, huser);
            Response restResponse = hprojectRestService.findAllHProject();
            List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
            });
            if (!listHProjects.isEmpty()) {
                Assert.assertFalse(listHProjects.isEmpty());
                for (HProject project : listHProjects) {
                    this.impersonateUser(hprojectRestService, huser);
                    Response restResponse1 = hprojectRestService.deleteHProject(project.getId());
                    Assert.assertEquals(200, restResponse1.getStatus());
                }
            }
        }
        // Remove all husers created in every test
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(huserRestService, adminUser);
        Response restResponseUsers = huserRestService.findAllHUser();
        List<HUser> listHUsers = restResponseUsers.readEntity(new GenericType<List<HUser>>() {
        });
        if (!listHUsers.isEmpty()) {
            Assert.assertFalse(listHUsers.isEmpty());
            for (HUser huser : listHUsers) {
                if (!huser.isAdmin()) {
                    this.impersonateUser(huserRestService, adminUser);
                    Response restResponse1 = huserRestService.deleteHUser(huser.getId());
                    Assert.assertEquals(200, restResponse1.getStatus());
                    Assert.assertNull(restResponse1.getEntity());
                }
            }
        }


        // Remove all Algorithms created in every tests
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponseAlgorithms = algorithmRestApi.findAllAlgorithm(AlgorithmType.STATISTICS);
        List<Algorithm> listAlgorithm = restResponseAlgorithms.readEntity(new GenericType<List<Algorithm>>() {
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
    }


}

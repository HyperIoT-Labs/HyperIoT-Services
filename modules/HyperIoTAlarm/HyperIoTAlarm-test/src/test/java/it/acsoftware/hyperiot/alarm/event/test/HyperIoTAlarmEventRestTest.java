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

package it.acsoftware.hyperiot.alarm.event.test;

import it.acsoftware.hyperiot.alarm.api.AlarmSystemApi;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.alarm.event.service.rest.AlarmEventRestApi;
import it.acsoftware.hyperiot.alarm.model.Alarm;
import it.acsoftware.hyperiot.alarm.service.rest.AlarmRestApi;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
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

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

import static it.acsoftware.hyperiot.alarm.event.test.HyperIoTAlarmEventTestConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAlarmEventRestTest extends KarafTestSupport {

    //forcing global config
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
        assertContains("HyperIoTAlarm-features ",features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_alarmEventModuleShouldWork(){
        // the following call checkModuleWorking checks if AlarmEvent module working
        // response statuc code '200'
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("AlarmEvent Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_saveAlarmEventShouldWork(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // response status code '200'
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        Alarm alarm = createAlarm();
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(200, restResponse.getStatus());
        AlarmEvent responseEvent = (AlarmEvent) restResponse.getEntity();
        Assert.assertNotNull(responseEvent);
        Assert.assertNotEquals(0, responseEvent.getId());
        Assert.assertEquals(responseEvent.getEvent().getId(), event.getEvent().getId());
        Assert.assertEquals(responseEvent.getAlarm().getId(), event.getAlarm().getId());
        Assert.assertEquals(responseEvent.getSeverity(), event.getSeverity());
    }

    @Test
    public void test003_saveAlarmEventShouldFailIfUserNotLogged(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        Alarm alarm = createAlarm();
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, null);
        Response restResponse =  alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError)restResponse.getEntity()).getType());
    }

    @Test
    public void test003_saveAlarmEventShouldFailIsAlarmIsNull(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event's alarm is null
        // response status code 500 ('NullPointerException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0 , project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(null);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(NullPointerException.class.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test004_saveAlarmEventShouldFailIsAlarmNotExist(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event's alarm not exist
        // response status code 404 HyperIoTEntityNotFound.
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        Alarm alarm = createAlarm();
        alarm.setId(0);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException +"HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test005_saveAlarmEventShouldFailIsRuleIsNull(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event's rule is null
        // response status code 500 (IllegalArgumentException).
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        AlarmEvent event = new AlarmEvent();
        event.setEvent(null);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(NullPointerException.class.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test006_saveAlarmEventShouldFailIfProjectIsNull(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but rule's project is null
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0 , alarm.getId());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        rule.setProject(null);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-project", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test007_saveAlarmEventShouldFailIfRuleNameIsNull(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event has a rule with null name
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        rule.setName(null);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(2, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(1).getField());
    }

    @Test
    public void test008_saveAlarmEventShouldFailIfRuleNameIsEmpty(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event has a rule with empty name
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        rule.setName("");
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test009_saveAlarmAndEventsShouldFailIfRuleNameIsMaliciousCode(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event has a rule with malicious name
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        rule.setName(maliciousCodeString);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test010_saveAlarmEventShouldFailIfRuleDescriptionIsMaliciousCode(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event has a rule with malicious description
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        rule.setDescription(maliciousCodeString);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-description", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test011_saveAlarmEventShouldFailIfRuleDefinitionIsMaliciousCode(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event has a rule with malicious ruleDefinition
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        rule.setRuleDefinition(maliciousCodeString);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
    }

    @Test
    public void test012_saveAlarmEventShouldFailIfRuleJsonActionIsNotSpecified(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but event has a rule with no specified jsonAction.
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = new Rule();
        rule.setName("AlarmEventRule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + project.getUser().getUsername());
        rule.setType(RuleType.ALARM_EVENT);
        long packetId = packet.getId();
        long packetFieldId = field.getId();
        rule.setRuleDefinition("\""+packetId+"."+packetFieldId+"\" > 30");
        rule.setProject(project);
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(0, errorResponse.getValidationErrors().size());
    }

    @Test
    public void test013_findAlarmEventShouldWork(){
        // hadmin find AlarmEvent with the following call findAlarmEvent
        // response status code 200;
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAlarmEvent(alarmEvent.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(alarmEvent.getId(), ((AlarmEvent) restResponse.getEntity()).getId());
    }

    @Test
    public void test014_findAlarmShouldFailIfNotLogged(){
        // the following call tries to find Alarm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        impersonateUser(alarmEventRestService, null);
        Response restResponse = alarmEventRestService.findAlarmEvent(alarmEvent.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test015_findAlarmShouldFailIfEntityNotExist(){
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        // the following call tries to find AlarmEvent,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAlarmEvent(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test016_updateAlarmEventShouldWork(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // response status code 200;
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        int newSeverity =  10;
        alarmEvent.setSeverity(newSeverity);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(200, restResponse.getStatus());
        AlarmEvent alarmEventFromResponse = ((AlarmEvent) restResponse.getEntity());
        Assert.assertEquals(alarmEvent.getId(), alarmEventFromResponse.getId());
        Assert.assertEquals(newSeverity, (alarmEventFromResponse.getSeverity()));
        Assert.assertEquals(alarmEvent.getEvent().getId(), alarmEventFromResponse.getEvent().getId());
        Assert.assertEquals(alarmEvent.getAlarm().getId(), alarmEventFromResponse.getAlarm().getId());
    }

    @Test
    public void test017_updateAlarmEventShouldFailIfUserNotLogged(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.setSeverity(10);
        impersonateUser(alarmEventRestService, null);
        Response restResponse = alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test018_updateAlarmEventShouldFailIfEntityNotExist(){
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        // the following call tries to find AlarmEvent,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.setId(0);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test019_updateAlarmEventShouldFailIsRuleIsNull(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // but event's rule is null.
        // response status code 404 'HyperIoTEntityNotFound'.
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.setEvent(null);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test020_updateAlarmEventShouldFailIfProjectIsNull(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but rule's project is null
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.getEvent().setProject(null);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-project", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test021_saveAlarmEventShouldFailIfRuleNameIsNull(){
        // hadmin save AlarmEvent with the following call saveAlarmEvent
        // but rule's name is null
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.getEvent().setName(null);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(2, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(1).getField());
    }

    @Test
    public void test022_updateAlarmEventShouldFailIfRuleNameIsEmpty(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // but rule's name is empty
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.getEvent().setName("");
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test023_saveAlarmEventShouldFailIfRuleNameIsMaliciousCode(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // but rule's name is malicious code
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.getEvent().setName(maliciousCodeString);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test024_saveAlarmEventShouldFailIfRuleDescriptionIsMaliciousCode(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // but rule's description is malicious code
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.getEvent().setDescription(maliciousCodeString);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-description", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test025_updateAlarmEventShouldFailIfRuleDefinitionIsMaliciousCode(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // but rule's definition is malicious code
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        alarmEvent.getEvent().setRuleDefinition(maliciousCodeString);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
    }

    @Test
    public void test026_updateAlarmEventShouldFailIfRuleJsonActionIsNotSpecified(){
        // hadmin update AlarmEvent with the following call updateAlarmEvent
        // but rule's definition is malicious code
        // response status code 422 ('HyperIoTValidationException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        Rule rule = new Rule();
        rule.setId(alarmEvent.getEvent().getId());
        rule.setName(alarmEvent.getEvent().getName());
        rule.setDescription(alarmEvent.getEvent().getDescription());
        rule.setType(alarmEvent.getEvent().getType());
        rule.setRuleDefinition(alarmEvent.getEvent().getRuleDefinition());
        rule.setProject(alarmEvent.getEvent().getProject());
        alarmEvent.setEvent(rule);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.updateAlarmEvent(alarmEvent);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException+ "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(0, errorResponse.getValidationErrors().size());
    }

    @Test
    public void test027_deleteAlarmEventShouldWork(){
        // hadmin delete AlarmEvent with the following call deleteAlarmEvent
        // response status code 200
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.deleteAlarmEvent(alarmEvent.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test028_deleteAlarmEventShouldFailIfEntityNotLogged(){
        // hadmin delete AlarmEvent with the following call deleteAlarmEvent
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        impersonateUser(alarmEventRestService, null);
        Response restResponse = alarmEventRestService.deleteAlarmEvent(alarmEvent.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test029_deleteAlarmEventShouldFailIfEntityNotExist(){
        // hadmin delete AlarmEvent with the following call deleteAlarmEvent
        // but HUser is not logged
        // response status code '404' ('HyperIoTEntityNotFound')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.deleteAlarmEvent(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test030_findAllAlarmEventShouldWork(){
        // hadmin find all AlarmEvent  with the following call findAllAlarmEvent
        // response status code 200
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        AlarmEvent alarmEvent2 = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent2.getId());
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAllAlarmEvent();
        Assert.assertEquals(200, restResponse.getStatus());
        List<AlarmEvent> alarmEventList = restResponse.readEntity(new GenericType<List<AlarmEvent>>() {
        });
        Assert.assertNotNull(alarmEventList);
        Assert.assertEquals(2, alarmEventList.size());
        Assert.assertTrue(alarmEventList.stream().map(AlarmEvent::getId).collect(Collectors.toList()).contains(alarmEvent.getId()));
        Assert.assertTrue(alarmEventList.stream().map(AlarmEvent::getId).collect(Collectors.toList()).contains(alarmEvent2.getId()));
    }

    @Test
    public void test031_findAllAlarmEventShouldWorkIfEventListIsEmpty(){
        // hadmin find all AlarmEvent  with the following call findAllAlarmEvent
        // // alarm event list is empty
        // response status code 200
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        AlarmEvent alarmEvent2 = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent2.getId());
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAllAlarmEvent();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(200, restResponse.getStatus());
        List<AlarmEvent> alarmEventList = restResponse.readEntity(new GenericType<List<AlarmEvent>>() {
        });
        Assert.assertNotNull(alarmEventList);
        Assert.assertEquals(2, alarmEventList.size());
        Assert.assertTrue(alarmEventList.stream().map(AlarmEvent::getId).collect(Collectors.toList()).contains(alarmEvent.getId()));
        Assert.assertTrue(alarmEventList.stream().map(AlarmEvent::getId).collect(Collectors.toList()).contains(alarmEvent2.getId()));
    }

    @Test
    public void test032_findAllAlarmEventShouldFailIfUserNotLogged(){
        // hadmin  find all alarm event paginated   with the following call findAllAlarmEventPaginated
        // but HUser is not logged
        // response status code '403' ('HyperIoTUnauthorizedException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(alarmEventRestService, null);
        Response restResponse = alarmEventRestService.findAllAlarmEvent();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test033_findAlarmEventPaginatedShouldWork(){
        // hadmin find all AlarmEvent paginated with the following call findAllAlarmEvent
        // response status code 200
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent.getId());
        AlarmEvent alarmEvent2 = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0, alarmEvent2.getId());
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAllAlarmEventPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(200, restResponse.getStatus());
        HyperIoTPaginableResult<AlarmEvent> alarmPageResult = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AlarmEvent>>() {
                });
        Assert.assertNotNull(alarmPageResult);
        Assert.assertEquals(defaultPage,alarmPageResult.getNumPages());
        Assert.assertEquals(defaultPage, alarmPageResult.getCurrentPage());
        Assert.assertEquals(defaultPage, alarmPageResult.getNextPage());
        Assert.assertEquals(defaultDelta, alarmPageResult.getDelta());
        Assert.assertNotNull(alarmPageResult.getResults());
        Assert.assertEquals(2, alarmPageResult.getResults().size());
        Assert.assertTrue(alarmPageResult.getResults().stream().map(AlarmEvent::getId).collect(Collectors.toList()).contains(alarmEvent.getId()));
        Assert.assertTrue(alarmPageResult.getResults().stream().map(AlarmEvent::getId).collect(Collectors.toList()).contains(alarmEvent2.getId()));
    }

    @Test
    public void test034_findAllAlarmEventPaginatedShouldWorkIfEventListIsEmpty(){
        // hadmin find all AlarmEvent paginated with the following call findAllAlarmEventPaginated
        // response status code 200
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAllAlarmEventPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(200, restResponse.getStatus());
        HyperIoTPaginableResult<AlarmEvent> alarmPageResult = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AlarmEvent>>() {
                });
        Assert.assertNotNull(alarmPageResult);
        Assert.assertEquals(0,alarmPageResult.getNumPages());
        Assert.assertEquals(defaultPage, alarmPageResult.getCurrentPage());
        Assert.assertEquals(defaultPage, alarmPageResult.getNextPage());
        Assert.assertEquals(defaultDelta, alarmPageResult.getDelta());
        Assert.assertNotNull(alarmPageResult.getResults());
        Assert.assertTrue(alarmPageResult.getResults().isEmpty());
    }

    @Test
    public void test035_findAllAlarmEventPaginatedShouldFailIfUserNotLogged(){
        // hadmin  find all alarm event paginated   with the following call findAllAlarmEventPaginated
        // but HUser is not logged
        // response status code '403' ('HyperIoTUnauthorizedException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(alarmEventRestService, null);
        Response restResponse = alarmEventRestService.findAllAlarmEventPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test036_findAllAlarmEventByAlarmIdShouldWork(){
        // hadmin find all AlarmEvent by alarm id   with the following call findAllAlarmEventByAlarmId
        //response status code '200'
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        AlarmEvent alarmEvent = createAlarmEvent(project, packet, field);
        Assert.assertNotEquals(0 , alarmEvent.getId());
        Assert.assertNotNull(alarmEvent.getAlarm());
        Assert.assertNotEquals(0, alarmEvent.getAlarm().getId());
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAllAlarmEventByAlarmId(alarmEvent.getAlarm().getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<AlarmEvent> alarmEventList = restResponse.readEntity(new GenericType<List<AlarmEvent>>() {
        });
        Assert.assertNotNull(alarmEventList);
        Assert.assertEquals(1, alarmEventList.size());
        AlarmEvent alarmEventResponse = alarmEventList.get(0);
        Assert.assertEquals(alarmEvent.getId(), alarmEventResponse.getId());
    }


    @Test
    public void test037_findAllAlarmEventByAlarmIdShouldWorkIfEventListIsEmpty(){
        // hadmin find all AlarmEvent by alarm id   with the following call findAllAlarmEventByAlarmId
        // alarm event list is empty
        //response status code '200'
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0 , alarm.getId());
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAllAlarmEventByAlarmId(alarm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<AlarmEvent> alarmEventList = restResponse.readEntity(new GenericType<List<AlarmEvent>>() {
        });
        Assert.assertNotNull(alarmEventList);
        Assert.assertTrue(alarmEventList.isEmpty());
    }

    @Test
    public void test038_findAllAlarmEventByAlarmIdShouldFailIfUserNotLogged(){
        // hadmin  find all alarm event by alarm id  with the following call findAllAlarmEventByAlarmId
        // but HUser is not logged
        // response status code '403' ('HyperIoTUnauthorizedException')
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0 , alarm.getId());
        impersonateUser(alarmEventRestService, null);
        Response restResponse = alarmEventRestService.findAllAlarmEventByAlarmId(alarm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test039_findAllAlarmEventByAlarmIdShouldFailIfAlarmNotFound(){
        // hadmin find all AlarmEvent by alarm id   with the following call findAllAlarmEventByAlarmId
        // but alarm not exist
        // response status code '404' (HyperIoTNoResultException).
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse = alarmEventRestService.findAllAlarmEventByAlarmId(-1);
        Assert.assertEquals(404 , restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTNoResultException", ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    /*
    *
    *
    *   UtilityMethod for test.
    *
    *
     */

    //Create an alarm template for test (An alarm template is an alarm entity build with valid fields).
    private Alarm createAlarmTemplate(){
        Alarm alarm = new Alarm();
        alarm.setName("AlarmName".concat(createRandomString()));
        alarm.setInhibited(true);
        return alarm;
    }

    private String createStringFieldWithSpecifiedLenght(int length) {
        String symbol = "a";
        String field = String.format("%" + length + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(length, field.length());
        return field;
    }

    private AlarmEvent createAlarmEventTemplate(Rule rule){
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setSeverity(1);
        return event;
    }

    private AlarmEvent createAlarmEvent(HProject project, HPacket packet, HPacketField field){
        AlarmEventRestApi alarmEventRestService = getOsgiService(AlarmEventRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Rule rule = createRuleTemplate(project, packet, field);
        Alarm alarm = createAlarm();
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setAlarm(alarm);
        event.setSeverity(1);
        impersonateUser(alarmEventRestService, adminUser);
        Response restResponse =  alarmEventRestService.saveAlarmEvent(event);
        Assert.assertEquals(200, restResponse.getStatus());
        AlarmEvent responseEvent = (AlarmEvent) restResponse.getEntity();
        Assert.assertNotNull(responseEvent);
        Assert.assertNotEquals(0, responseEvent.getId());
        Assert.assertEquals(responseEvent.getEvent().getId(), event.getEvent().getId());
        Assert.assertEquals(responseEvent.getAlarm().getId(), event.getAlarm().getId());
        Assert.assertEquals(responseEvent.getSeverity(), event.getSeverity());
        return responseEvent;
    }

    private Rule createRuleTemplate(HProject hproject, HPacket packet, HPacketField field){
        Rule rule = new Rule();
        rule.setName("AlarmEventRule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + hproject.getUser().getUsername());
        rule.setType(RuleType.ALARM_EVENT);
        long packetId = packet.getId();
        long packetFieldId = field.getId();
        rule.setRuleDefinition("\""+packetId+"."+packetFieldId+"\" > 30");
        rule.setProject(hproject);
        try {
            rule.setJsonActions("[\"{\\\"actionName\\\": \\\"it.acsoftware.hyperiot.alarm.service.actions.NoAlarmAction\\\", \\\"active\\\": true}\"]");
        } catch (Exception e){
            //fail to set jsonAction.
            throw new RuntimeException();
        }
        return rule;
    }

    private String createRandomString(){
        return java.util.UUID.randomUUID().toString().replaceAll("-", "");
    }

    private Alarm createAlarmWithEvents(HProject project, HPacket packet, HPacketField field){
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Rule rule = createRuleTemplate(project, packet, field);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(200, restResponse.getStatus());
        Alarm alarmWithEvents = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmWithEvents.getId());
        Assert.assertEquals(inhibited, alarmWithEvents.isInhibited());
        Assert.assertEquals(alarmName, alarmWithEvents.getName());
        List<AlarmEvent> alarmEventsResponseList = alarmWithEvents.getAlarmEventList();
        Assert.assertNotNull(alarmEventsResponseList);
        Assert.assertEquals(1, alarmEventsResponseList.size());
        AlarmEvent alarmEventResponse = alarmEventsResponseList.get(0);
        Assert.assertNotEquals(0, alarmEventResponse.getId());
        Assert.assertEquals(event.getSeverity(), alarmEventResponse.getSeverity());
        Assert.assertNotNull(alarmEventResponse.getEvent());
        it.acsoftware.hyperiot.rule.model.Rule ruleFromResponse = alarmEventResponse.getEvent();
        Assert.assertNotEquals(0, ruleFromResponse.getId());
        Assert.assertNotNull(ruleFromResponse.getProject());
        Assert.assertEquals(project.getId(), ruleFromResponse.getProject().getId());
        Assert.assertEquals(rule.getDescription(), ruleFromResponse.getDescription());
        Assert.assertEquals(rule.getName(), ruleFromResponse.getName());
        Assert.assertEquals(rule.getType(), ruleFromResponse.getType());
        Assert.assertEquals(rule.getJsonActions(), ruleFromResponse.getJsonActions());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events", ruleFromResponse.getType().getDroolsPackage());
        Assert.assertEquals("temperature >= 23 AND humidity > 36", ruleFromResponse.getRuleDefinition());
        return alarmWithEvents;
    }

    private Alarm createAlarm(){
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarmTemplate();
        String alarmName = alarm.getName();
        boolean alarmInhinbited = alarm.isInhibited();
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Assert.assertEquals(200, restResponse.getStatus());
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmFromResponse.getId());
        Assert.assertEquals(alarmName, alarmFromResponse.getName());
        Assert.assertEquals(alarmInhinbited, alarmFromResponse.isInhibited());
        return alarmFromResponse;
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
        AlarmEventSystemApi alarmEventSystemApi = getOsgiService(AlarmEventSystemApi.class);
        AlarmSystemApi alarmSystemApi = getOsgiService(AlarmSystemApi.class);
        RuleEngineSystemApi ruleEngineSystemApi = getOsgiService(RuleEngineSystemApi.class);
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        HPacketFieldSystemApi hPacketFieldSystemApi = getOsgiService(HPacketFieldSystemApi.class);
        HPacketSystemApi hPacketSystemApi = getOsgiService(HPacketSystemApi.class);
        HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
        HyperIoTTestUtils.truncateTables(alarmEventSystemApi, null);
        HyperIoTTestUtils.truncateTables(alarmSystemApi, null);
        HyperIoTTestUtils.truncateTables(ruleEngineSystemApi, null);
        HyperIoTTestUtils.truncateTables(areaSystemApi, null);
        HyperIoTTestUtils.truncateTables(hPacketFieldSystemApi,null);
        HyperIoTTestUtils.truncateTables(hPacketSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
    }

}

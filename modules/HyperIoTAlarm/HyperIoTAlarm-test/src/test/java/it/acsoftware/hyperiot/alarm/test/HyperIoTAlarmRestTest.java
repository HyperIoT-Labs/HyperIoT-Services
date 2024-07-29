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

package it.acsoftware.hyperiot.alarm.test;

import it.acsoftware.hyperiot.alarm.api.AlarmSystemApi;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
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

import static it.acsoftware.hyperiot.alarm.test.HyperIoTAlarmTestConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAlarmRestTest extends KarafTestSupport {

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
        assertContains("HyperIoTAlarm-features ", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_alarmModuleShouldWork() {
        // the following call checkModuleWorking checks if Alarm module working
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Alarm Module works!", restResponse.getEntity());
    }

    @Test
    public void test002_saveAlarmShouldWork() {
        // hadmin save Alarm with the following call saveAlarm
        // response status code '200'
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
    }

    @Test
    public void test003_saveAlarmShouldFailIfNotLogged() {
        // the following call saveAlarm tries to save Alarm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        Alarm alarm = createAlarmTemplate();
        impersonateUser(alarmRestService, null);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test004_saveAlarmShouldFailIfNameIsNull() {
        // hadmin tries to save Alarm with the following call saveAlarm,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarmTemplate();
        alarm.setName(null);
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test005_saveAlarmShouldFailIfNameIsEmpty() {
        // hadmin tries to save Alarm with the following call saveAlarm,
        // but name is empty string
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarmTemplate();
        alarm.setName("");
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test006_saveAlarmShouldFailIfNameIsMaliciousCode() {
        // hadmin tries to save Alarm with the following call saveAlarm,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarmTemplate();
        alarm.setName(maliciousCodeString);
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test007_saveAlarmShouldFailIfNameLenghtIsGreaterThan255Chars() {
        // hadmin tries to save Alarm with the following call saveAlarm,
        // but name's lenght is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarmTemplate();
        alarm.setName(createStringFieldWithSpecifiedLenght(256));
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test008_saveAlarmShouldWorkIfNameLenghtIsEqualTo255Chars() {
        // hadmin save Alarm with the following call saveAlarm
        // alarm's name field lenght is equal to 255 chars.
        // response status code '200'
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarmTemplate();
        alarm.setName(createStringFieldWithSpecifiedLenght(255));
        String alarmName = alarm.getName();
        boolean alarmInhinbited = alarm.isInhibited();
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmFromResponse.getId());
        Assert.assertEquals(alarmName, alarmFromResponse.getName());
        Assert.assertEquals(alarmInhinbited, alarmFromResponse.isInhibited());
    }

    @Test
    public void test010_findAlarmShouldWork() {
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Response restResponse = alarmRestService.findAlarm(alarm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertEquals(alarm.getId(), alarmFromResponse.getId());
        Assert.assertEquals(alarm.getName(), alarmFromResponse.getName());
        Assert.assertEquals(alarm.isInhibited(), alarmFromResponse.isInhibited());
    }

    @Test
    public void test011_findAlarmShouldFailIfNotLogged() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // the following call tries to find Alarm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        this.impersonateUser(alarmRestService, null);
        Response restResponse = alarmRestService.findAlarm(alarm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test012_findAlarmShouldFailIfEntityNotExist() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // the following call tries to find Alarm,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.findAlarm(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test013_updateAlarmShouldWork() {
        // hadmin update Alarm with the following call updateAlarm
        // response status code '200'
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        String newName = createRandomString();
        alarm.setName(newName);
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.updateAlarm(alarm);
        Assert.assertEquals(200, restResponse.getStatus());
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmFromResponse.getId());
        Assert.assertEquals(newName, alarmFromResponse.getName());
        Assert.assertEquals(alarm.isInhibited(), alarmFromResponse.isInhibited());
    }

    @Test
    public void test014_updateAlarmShouldFailIfNotLogged() {
        // the following call updateAlarm tries to update Alarm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        impersonateUser(alarmRestService, null);
        Response restResponse = alarmRestService.updateAlarm(alarm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test015_updateAlarmShouldFailIfNameIsNull() {
        // hadmin tries to update Alarm with the following call updateAlarm,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        alarm.setName(null);
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.updateAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test016_updateAlarmShouldFailIfNameIsEmpty() {
        // hadmin tries to update Alarm with the following call updateAlarm,
        // but name is empty string
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        alarm.setName("");
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.updateAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test017_updateAlarmShouldFailIfNameIsMaliciousCode() {
        // hadmin tries to update Alarm with the following call updateAlarm,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        alarm.setName(maliciousCodeString);
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.updateAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test018_updateAlarmShouldFailIfNameLenghtIsGreaterThan255Chars() {
        // hadmin tries to update Alarm with the following call updateAlarm,
        // but name's lenght is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        alarm.setName(createStringFieldWithSpecifiedLenght(256));
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.updateAlarm(alarm);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarm.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test019_updateAlarmShouldWorkIfNameLenghtIsEqualTo255Chars() {
        // hadmin save Alarm with the following call saveAlarm
        // alarm's name field lenght is equal to 255 chars.
        // response status code '200'
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Alarm alarm = createAlarmTemplate();
        alarm.setName(createStringFieldWithSpecifiedLenght(255));
        String alarmName = alarm.getName();
        boolean alarmInhinbited = alarm.isInhibited();
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarm(alarm);
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmFromResponse.getId());
        Assert.assertEquals(alarmName, alarmFromResponse.getName());
        Assert.assertEquals(alarmInhinbited, alarmFromResponse.isInhibited());
    }

    @Test
    public void test021_deleteAlarmShouldWork() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // hadmin delete Alarm with the following call deleteAlarm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.deleteAlarm(alarm.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test022_deleteAlarmShouldFailIfNotLogged() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // the following call tries to delete Alarm,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        this.impersonateUser(alarmRestService, null);
        Response restResponse = alarmRestService.deleteAlarm(alarm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test023_deleteAlarmShouldFailIfEntityNotFound() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // the following call deleteAlarm tries to delete Alarm,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.deleteAlarm(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test025_findAllAlarmShouldWork() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // hadmin find all Alarm with the following call findAllAlarm
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        Alarm alarm2 = createAlarm();
        Assert.assertNotEquals(0, alarm2.getId());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.findAllAlarm();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Alarm> alarmList = restResponse.readEntity(new GenericType<List<Alarm>>() {
        });
        Assert.assertNotNull(alarmList);
        Assert.assertEquals(2, alarmList.size());
        Assert.assertTrue(alarmList.stream().map(Alarm::getId).collect(Collectors.toList()).contains(alarm.getId()));
        Assert.assertTrue(alarmList.stream().map(Alarm::getId).collect(Collectors.toList()).contains(alarm2.getId()));
    }

    @Test
    public void test026_findAllAlarmShouldWorkWhenAlarmListIsEmpty() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // hadmin find all Alarm with the following call findAllAlarm
        // alarm list is empty
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.findAllAlarm();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Alarm> alarmList = restResponse.readEntity(new GenericType<List<Alarm>>() {
        });
        Assert.assertNotNull(alarmList);
        Assert.assertTrue(alarmList.isEmpty());
    }

    @Test
    public void test027_findAllAlarmPaginatedShouldWork() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // hadmin find all AlarmPaginated with the following call findAllAlarmPaginated
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        Alarm alarm2 = createAlarm();
        Assert.assertNotEquals(0, alarm2.getId());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.findAllAlarmPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(200, restResponse.getStatus());
        HyperIoTPaginableResult<Alarm> alarmPageResult = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Alarm>>() {
                });
        Assert.assertNotNull(alarmPageResult);
        Assert.assertEquals(defaultPage, alarmPageResult.getNumPages());
        Assert.assertEquals(defaultPage, alarmPageResult.getCurrentPage());
        Assert.assertEquals(defaultPage, alarmPageResult.getNextPage());
        Assert.assertEquals(defaultDelta, alarmPageResult.getDelta());
        Assert.assertNotNull(alarmPageResult.getResults());
        Assert.assertEquals(2, alarmPageResult.getResults().size());
        Assert.assertTrue(alarmPageResult.getResults().stream().map(Alarm::getId).collect(Collectors.toList()).contains(alarm.getId()));
        Assert.assertTrue(alarmPageResult.getResults().stream().map(Alarm::getId).collect(Collectors.toList()).contains(alarm2.getId()));
    }

    @Test
    public void test025_findAllAlarmPaginatedShouldWorkWhenAlarmListIsEmpty() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // hadmin find all AlarmPaginated with the following call findAllAlarmPaginated
        // alarm list is empty.
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.findAllAlarmPaginated(defaultDelta, defaultPage);
        Assert.assertEquals(200, restResponse.getStatus());
        HyperIoTPaginableResult<Alarm> alarmPageResult = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Alarm>>() {
                });
        Assert.assertNotNull(alarmPageResult);
        Assert.assertEquals(0, alarmPageResult.getNumPages());
        Assert.assertEquals(defaultPage, alarmPageResult.getCurrentPage());
        Assert.assertEquals(defaultPage, alarmPageResult.getNextPage());
        Assert.assertEquals(defaultDelta, alarmPageResult.getDelta());
        Assert.assertNotNull(alarmPageResult.getResults());
        Assert.assertTrue(alarmPageResult.getResults().isEmpty());
    }

    @Test
    public void test026_saveAlarmAndEventsShouldWork() {
        // hadmin save Alarm with events with the following call saveAlarmAndEvents
        // response status code '200'
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        Rule ruleFromResponse = alarmEventResponse.getEvent();
        Assert.assertNotEquals(0, ruleFromResponse.getId());
        Assert.assertNotNull(ruleFromResponse.getProject());
        Assert.assertEquals(project.getId(), ruleFromResponse.getProject().getId());
        Assert.assertEquals(rule.getDescription(), ruleFromResponse.getDescription());
        Assert.assertEquals(rule.getName(), ruleFromResponse.getName());
        Assert.assertEquals(RuleType.ALARM_EVENT, ruleFromResponse.getType());
        Assert.assertEquals(rule.getJsonActions(), ruleFromResponse.getJsonActions());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events", ruleFromResponse.getType().getDroolsPackage());
        Assert.assertEquals("\"" + packet.getId() + "." + field.getId() + "\" > 30", ruleFromResponse.getRuleDefinition());
    }

    @Test
    public void test028_saveAlarmAndEventsShouldFailIfNotLogged() {
        // the following call saveAlarmAndEvents tries to save Alarm with Events,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        Rule rule = createRuleTemplate(project, packet, field);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, null);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test029_saveAlarmAndEventsShouldFailIfNameIsNull() {
        // hadmin tries to saveAlarmAndEvents Alarm with the following call saveAlarmAndEvents,
        // but alarm name is null
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = null;
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
    }

    @Test
    public void test030_saveAlarmAndEventsShouldFailIfNameIsEmpty() {
        // hadmin tries to save Alarm with the following call saveAlarm,
        // but name is empty string
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = "";
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarmName, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test031_saveAlarmAndEventsShouldFailIfNameIsMaliciousCode() {
        // hadmin tries to saveAlarmAndEvents with the following call saveAlarmAndEvents,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = "";
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarmName, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test032_saveAlarmAndEventsShouldFailIfNameLenghtIsGreaterThan255Chars() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but name's lenght is greater than 255 chars.
        // response status code '422' HyperIoTValidationException
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createStringFieldWithSpecifiedLenght(256);
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
        Assert.assertEquals("alarm-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(alarmName, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test033_saveAlarmAndEventsShouldWorkIfNameLenghtIsEqualTo255Chars() {
        // hadmin save Alarm with events the following call saveAlarmAndEvents
        // alarm's name field lenght is equal to 255 chars.
        // response status code '200'
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createStringFieldWithSpecifiedLenght(255);
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(200, restResponse.getStatus());
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmFromResponse.getId());
        Assert.assertEquals(alarmName, alarmFromResponse.getName());
        Assert.assertEquals(inhibited, alarmFromResponse.isInhibited());
    }

    @Test
    public void test035_saveAlarmAndEventsShouldWorkIfEventListIsNull() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        //  event list is null
        // response status code 200
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(null, alarmName, inhibited);
        Assert.assertEquals(200, restResponse.getStatus());
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmFromResponse.getId());
        Assert.assertEquals(alarmName, alarmFromResponse.getName());
        Assert.assertEquals(inhibited, alarmFromResponse.isInhibited());
        Assert.assertTrue(alarmFromResponse.getAlarmEventList().isEmpty());
    }

    @Test
    public void test036_saveAlarmAndEventsShouldWorkIfEventListIsEmpty() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        //  event list is empty
        // response status code 200
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(200, restResponse.getStatus());
        Alarm alarmFromResponse = (Alarm) restResponse.getEntity();
        Assert.assertNotEquals(0, alarmFromResponse.getId());
        Assert.assertEquals(alarmName, alarmFromResponse.getName());
        Assert.assertEquals(inhibited, alarmFromResponse.isInhibited());
        Assert.assertTrue(alarmFromResponse.getAlarmEventList().isEmpty());
    }


    @Test
    public void test037_saveAlarmAndEventsShouldFailIfRuleIsNull() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event's rule is null
        // response status code 500
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AlarmEvent event = createAlarmEventTemplate(null);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(500, restResponse.getStatus());
        Assert.assertEquals(NullPointerException.class.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getType());

    }

    @Test
    public void test038_saveAlarmAndEventsShouldFailIfProjectIsNull() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event's rule is null
        // response status code 422
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        rule.setProject(null);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-project", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test039_saveAlarmAndEventsShouldFailIfRuleNameIsNull() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event has a rule with null name
        // response status code 422
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        rule.setName(null);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(2, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(1).getField());
    }

    @Test
    public void test040_saveAlarmAndEventsShouldFailIfRuleNameIsEmpty() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event has a rule with empty name
        // response status code 422
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        rule.setName("");
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test041_saveAlarmAndEventsShouldFailIfRuleNameIsMaliciousCode() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event has a rule with malicious name
        // response status code 422
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        rule.setName(maliciousCodeString);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-name", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test042_saveAlarmAndEventsShouldFailIfRuleDescriptionIsMaliciousCode() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event has a rule with malicious description
        // response status code 422
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        rule.setDescription(maliciousCodeString);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
        Assert.assertEquals("rule-description", errorResponse.getValidationErrors().get(0).getField());
    }

    @Test
    public void test043_saveAlarmAndEventsShouldFailIfRuleDefinitionIsMaliciousCode() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event has a rule with malicious definition
        // response status code 422
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        rule.setRuleDefinition(maliciousCodeString);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(1, errorResponse.getValidationErrors().size());
    }

    @Test
    public void test043_saveAlarmAndEventsShouldFailIfRuleJsonActionIsNotSpecified() {
        // hadmin tries to save Alarm with events with the following call saveAlarmAndEvents,
        // but event has a rule with no specified action.
        // response status code 422
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
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
        Rule rule = new Rule();
        rule.setName("Add category rule 1" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + project.getUser().getUsername());
        rule.setType(RuleType.ALARM_EVENT);
        rule.setRuleDefinition("\"" + packet.getId() + "." + field.getId() + "\" > 30");
        rule.setProject(project);
        AlarmEvent event = createAlarmEventTemplate(rule);
        Collection<AlarmEvent> alarmEvents = new LinkedList<>();
        alarmEvents.add(event);
        String alarmName = createRandomString();
        boolean inhibited = true;
        impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.saveAlarmAndEvents(alarmEvents, alarmName, inhibited);
        Assert.assertEquals(422, restResponse.getStatus());
        HyperIoTBaseError errorResponse = (HyperIoTBaseError) restResponse.getEntity();
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException", errorResponse.getType());
        Assert.assertEquals(0, errorResponse.getValidationErrors().size());
    }

    @Test
    public void test044_findAlarmByProjectIdShouldWork() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // hadmin tries to find Alarm by projectId with the following call findAlarmByProjectId,
        // response status code 200
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
        Alarm alarm = createAlarmWithEvents(project, packet, field);
        Assert.assertNotEquals(0, alarm.getId());
        Response restResponse = alarmRestService.findAllAlarmByProjectId(project.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Set<Alarm> alarmList = restResponse.readEntity(new GenericType<Set<Alarm>>() {
        });
        Assert.assertNotNull(alarmList);
        Assert.assertEquals(1, alarmList.size());
        Assert.assertTrue(alarmList.contains(alarm));
    }

    @Test
    public void test045_findAlarmShouldFailIfNotLogged() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // the following call tries to find Alarm by projectId findAlarmByProjectId,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        this.impersonateUser(alarmRestService, null);
        Response restResponse = alarmRestService.findAllAlarmByProjectId(project.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test046_findAlarmByProjectIdShouldFailIfProjectNotExist() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // the following call tries to find Alarm by projectId findAlarmByProjectId,
        // but project not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.findAllAlarmByProjectId(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test047_findAlarmByProjectIdShouldWorkIfProjectHasNotAlarm() {
        AlarmRestApi alarmRestService = getOsgiService(AlarmRestApi.class);
        // the following call tries to find Alarm by projectId findAlarmByProjectId,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(alarmRestService, adminUser);
        Response restResponse = alarmRestService.findAllAlarmByProjectId(project.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Set<Alarm> alarmList = restResponse.readEntity(new GenericType<Set<Alarm>>() {
        });
        Assert.assertNotNull(alarmList);
        Assert.assertEquals(0, alarmList.size());
    }





    /*
     *
     *
     *  Utility method for test.
     *
     *
     *
     */

    //Create an alarm template for test (An alarm template is an alarm entity build with valid fields).
    private Alarm createAlarmTemplate() {
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

    private AlarmEvent createAlarmEventTemplate(Rule rule) {
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setSeverity(1);
        return event;
    }

    private Rule createRuleTemplate(HProject hproject, HPacket packet, HPacketField field) {
        Rule rule = new Rule();
        rule.setName("AlarmEventRule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Rule defined by huser: " + hproject.getUser().getUsername());
        rule.setType(RuleType.ALARM_EVENT);
        long packetId = packet.getId();
        long packetFieldId = field.getId();
        rule.setRuleDefinition("\"" + packetId + "." + packetFieldId + "\" > 30");
        rule.setProject(hproject);
        try {
            rule.setJsonActions("[\"{\\\"actionName\\\": \\\"it.acsoftware.hyperiot.alarm.service.actions.NoAlarmAction\\\", \\\"active\\\": true}\"]");
        } catch (Exception e) {
            //fail to set jsonAction.
            throw new RuntimeException();
        }
        return rule;
    }

    private String createRandomString() {
        return java.util.UUID.randomUUID().toString().replaceAll("-", "");
    }

    private Alarm createAlarmWithEvents(HProject project, HPacket packet, HPacketField field) {
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
        Rule ruleFromResponse = alarmEventResponse.getEvent();
        Assert.assertNotEquals(0, ruleFromResponse.getId());
        Assert.assertNotNull(ruleFromResponse.getProject());
        Assert.assertEquals(project.getId(), ruleFromResponse.getProject().getId());
        Assert.assertEquals(rule.getDescription(), ruleFromResponse.getDescription());
        Assert.assertEquals(rule.getName(), ruleFromResponse.getName());
        Assert.assertEquals(rule.getType(), ruleFromResponse.getType());
        Assert.assertEquals(rule.getJsonActions(), ruleFromResponse.getJsonActions());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events", ruleFromResponse.getType().getDroolsPackage());
        Assert.assertEquals("\"" + packet.getId() + "." + field.getId() + "\" > 30", ruleFromResponse.getRuleDefinition());
        return alarmWithEvents;
    }

    private Alarm createAlarm() {
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

    public HPacketField createHPacketField(HPacket hpacket) {
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
        HyperIoTTestUtils.truncateTables(hPacketFieldSystemApi, null);
        HyperIoTTestUtils.truncateTables(hPacketSystemApi, null);
        HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
        HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
    }

}

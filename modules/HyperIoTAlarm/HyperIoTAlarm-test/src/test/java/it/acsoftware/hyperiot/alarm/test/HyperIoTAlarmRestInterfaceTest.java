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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.*;
import static it.acsoftware.hyperiot.alarm.test.HyperIoTAlarmTestConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAlarmRestInterfaceTest extends KarafTestSupport {

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

    @Before
    public void impersonateAsHyperIoTAdmin(){
        AlarmRestApi alarmRestApi = getOsgiService(AlarmRestApi.class);
        AuthenticationApi authenticationApi = getOsgiService(AuthenticationApi.class);
        HyperIoTUser admin = (HyperIoTUser) authenticationApi.login("hadmin","admin");
        this.impersonateUser(alarmRestApi, admin);
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
    public void test001_saveAlarmShouldSerializeResponseCorrectly(){
        Alarm alarm = new Alarm();
        alarm.setName(createRandomString());
        alarm.setInhibited(true);
        String serializedAlarm = serializeAlarmForRequest(alarm);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/alarms"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(serializedAlarm)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(alarmEntitySerializedWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test002_findAlarmShouldSerializeResponseCorrectly(){
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/alarms/").concat(String.valueOf(alarm.getId())))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(alarmEntitySerializedWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test003_updateAlarmShouldSerializeResponseCorrectly(){
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        alarm.setInhibited(! alarm.isInhibited());
        String alarmSerializedForRequest = serializeAlarmForRequest(alarm);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .put()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/alarms"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .withJsonBody(alarmSerializedForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(alarmEntitySerializedWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test004_deleteAlarmShouldSerializeResponseCorrectly(){
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .delete()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/alarms/").concat(String.valueOf(alarm.getId())))
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
    public void test005_findAllAlarmShouldSerializeResponseCorrectly(){
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        Alarm alarm2 = createAlarm();
        Assert.assertNotEquals(0, alarm2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/alarms/all"))
                .withAuthorizationAsHyperIoTAdmin()
                .withContentTypeHeader("application/json")
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(alarmEntitySerializedWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test006_findAllAlarmPagiantedShouldSerializeResponseCorrectly(){
        Alarm alarm = createAlarm();
        Assert.assertNotEquals(0, alarm.getId());
        Alarm alarm2 = createAlarm();
        Assert.assertNotEquals(0, alarm2.getId());
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/alarms"))
                .withAuthorizationAsHyperIoTAdmin()
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
                .containExactInnerProperties("results", alarmEntitySerializedWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test007_saveAlarmAndEventsShouldSerializeResponseCorrectly(){
        HProject project = createHProject();
        Assert.assertNotEquals(0, project.getId());
        HDevice device = createHDevice(project);
        Assert.assertNotEquals(0, device.getId());
        HPacket packet = createHPacket(device);
        Assert.assertNotEquals(0, packet.getId());
        HPacketField field = createHPacketField(packet);
        Assert.assertNotEquals(0, field.getId());
        String alarmEventForRequest = createAlarmEventsForRequest(project, packet, field);
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .post()
                .withUri(HyperIoTHttpUtils.SERVICE_BASE_URL.concat("/alarms/withEvents"))
                .withAuthorizationAsHyperIoTAdmin()
                .withParameter("alarmName", createRandomString())
                .withParameter("isInhibited", String.valueOf(true))
                .withJsonBody(alarmEventForRequest)
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(alarmEntitySerializedWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("alarmEventList", alarmEventEntitySerializedWithJsonViewPublicExpectedProperty())
                .containExactInnerProperties("alarmEventList.alarm", alarmEntitySerializedWithHyperIoTInnerEntityJsonSerializer())
                .containExactInnerProperties("alarmEventList.event", ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("alarmEventList.event.project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    @Test
    public void test008_findAllAlarmByProjectIdShouldSerializeResponseCorrectly(){
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
        String requestUri = HyperIoTHttpUtils
                .SERVICE_BASE_URL
                .concat("/alarms").concat("/all")
                .concat("/projects/").concat(String.valueOf(project.getId()));
        HyperIoTHttpRequest request = HyperIoTHttpRequestBuilder
                .get()
                .withUri(requestUri)
                .withAuthorizationAsHyperIoTAdmin()
                .build();
        HyperIoTHttpResponse response = HyperIoTHttpClient
                .hyperIoTHttpClient()
                .execute(request);
        HyperIoTHttpResponseValidator testValidator = HyperIoTHttpResponseValidatorBuilder
                .validatorBuilder()
                .withStatusEqual(200)
                .containExactProperties(alarmEntitySerializedWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("alarmEventList", alarmEventEntitySerializedWithJsonViewPublicExpectedProperty())
                .containExactInnerProperties("alarmEventList.alarm", alarmEntitySerializedWithHyperIoTInnerEntityJsonSerializer())
                .containExactInnerProperties("alarmEventList.event", ruleEntityWithJsonViewPublicExpectedProperties())
                .containExactInnerProperties("alarmEventList.event.project", hprojectEntityWithJsonViewPublicExpectedProperties())
                .build();
        boolean testSuccessful = testValidator.validateResponse(response);
        Assert.assertTrue(testSuccessful);
    }

    /*
    *
    *
    * Utility method for test.
    *
    *
     */

    private String createAlarmEventsForRequest(HProject project, HPacket packet, HPacketField field){
        try{
            int eventSeverity = 1;
            RuleType type = RuleType.ALARM_EVENT;
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode eventsNode = mapper.createArrayNode();
            ObjectNode eventNode = mapper.createObjectNode();
            eventNode.put("severity", String.valueOf(eventSeverity));
            eventNode.putObject("event");
            ObjectNode ruleNode =(ObjectNode) eventNode.get("event");
            ruleNode.put("name", createRandomString());
            ruleNode.put("description", createRandomString());
            ruleNode.put("ruleDefinition", "\""+packet.getId()+"."+field.getId()+"\" > 30");
            ruleNode.put("jsonActions", "[\"{\\\"actionName\\\": \\\"it.acsoftware.hyperiot.alarm.service.actions.NoAlarmAction\\\", \\\"active\\\": true}\"]");
            ruleNode.put("type", String.valueOf(type.ordinal()));
            ruleNode.putObject("project");
            ObjectNode projectNode = (ObjectNode) ruleNode.get("project");
            projectNode.put("id", project.getId());
            eventsNode.add(eventNode);
            return mapper.writeValueAsString(eventsNode);
        } catch ( Exception e ){
            //serialization errore;
            throw new RuntimeException();
        }
    }

    private List<String> hprojectEntityWithJsonViewPublicExpectedProperties(){
        List<String> hprojectEntityWithJsonViewPublicExpectedProperties = new ArrayList<>();
        hprojectEntityWithJsonViewPublicExpectedProperties.addAll(hyperIoTAbstractEntityProperties());
        hprojectEntityWithJsonViewPublicExpectedProperties.add("name");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("description");
        hprojectEntityWithJsonViewPublicExpectedProperties.add("user");
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

    private List<String> alarmEventEntitySerializedWithJsonViewPublicExpectedProperty(){
        List<String> alarmEventEntitySerializedWithJsonViewPublicExpectedProperty = new ArrayList<>(hyperIoTAbstractEntityProperties());
        alarmEventEntitySerializedWithJsonViewPublicExpectedProperty.add("alarm");
        alarmEventEntitySerializedWithJsonViewPublicExpectedProperty.add("event");
        alarmEventEntitySerializedWithJsonViewPublicExpectedProperty.add("severity");
        return alarmEventEntitySerializedWithJsonViewPublicExpectedProperty;

    }

    private List<String> alarmEntitySerializedWithHyperIoTInnerEntityJsonSerializer(){
        List<String> alarmSerializedWithHyperIoTInnerEntityJsonSerializer = new ArrayList<>();
        alarmSerializedWithHyperIoTInnerEntityJsonSerializer.add("id");
        alarmSerializedWithHyperIoTInnerEntityJsonSerializer.add("entityCreateDate");
        alarmSerializedWithHyperIoTInnerEntityJsonSerializer.add("entityModifyDate");
        alarmSerializedWithHyperIoTInnerEntityJsonSerializer.add("name");
        alarmSerializedWithHyperIoTInnerEntityJsonSerializer.add("inhibited");
        return alarmSerializedWithHyperIoTInnerEntityJsonSerializer;
    }

    private List<String> alarmEntitySerializedWithJsonViewPublicExpectedProperties(){
        List<String> alarmSerializedWithJsonViewPublicExpectedProperties = new ArrayList<>(hyperIoTAbstractEntityProperties());
        alarmSerializedWithJsonViewPublicExpectedProperties.add("name");
        alarmSerializedWithJsonViewPublicExpectedProperties.add("inhibited");
        alarmSerializedWithJsonViewPublicExpectedProperties.add("alarmEventList");
        return alarmSerializedWithJsonViewPublicExpectedProperties;
    }

    private List<String> hyperIoTAbstractEntityProperties(){
        List<String> hyperIoTAbstractEntityFields = new ArrayList<>();
        hyperIoTAbstractEntityFields.add("id");
        hyperIoTAbstractEntityFields.add("entityCreateDate");
        hyperIoTAbstractEntityFields.add("entityModifyDate");
        hyperIoTAbstractEntityFields.add("entityVersion");
        return hyperIoTAbstractEntityFields;
    }

    private String serializeAlarmForRequest(Alarm alarm){
        try{
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(alarm);
        } catch ( Exception e){
            //serialization error ;
            throw new RuntimeException();
        }
    }

    //Create an alarm template for test (An alarm template is an alarm entity build with valid fields).
    private Alarm createAlarmTemplate(){
        Alarm alarm = new Alarm();
        alarm.setName("AlarmName".concat(createRandomString()));
        alarm.setInhibited(true);
        return alarm;
    }

    private AlarmEvent createAlarmEventTemplate(it.acsoftware.hyperiot.rule.model.Rule rule){
        AlarmEvent event = new AlarmEvent();
        event.setEvent(rule);
        event.setSeverity(1);
        return event;
    }

    private Rule createRuleTemplate(HProject hproject, HPacket packet, HPacketField field)  {
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
        Assert.assertEquals(RuleType.ALARM_EVENT, ruleFromResponse.getType());
        Assert.assertEquals(rule.getJsonActions(), ruleFromResponse.getJsonActions());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events", ruleFromResponse.getType().getDroolsPackage());
        Assert.assertEquals("\""+packet.getId()+"."+field.getId()+"\" > 30", ruleFromResponse.getRuleDefinition());
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

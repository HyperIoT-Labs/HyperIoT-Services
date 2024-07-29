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

package it.acsoftware.hyperiot.stormmanager.test;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.service.actions.events.SendMailAction;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class StormManagerTestUtil {
    public static HProject createHProject(KarafTestSupport test,HUser user) {
        HPacketSystemApi packetSystemApi = test.getOsgiService(HPacketSystemApi.class);
        HDeviceSystemApi deviceSystemApi = test.getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi projectSystemApi = test.getOsgiService(HProjectSystemApi.class);
        HUserSystemApi userSystemApi = test.getOsgiService(HUserSystemApi.class);
        RuleEngineSystemApi ruleEngineSystemApi = test.getOsgiService(RuleEngineSystemApi.class);

        // create test HProject + HDevice + HPacket(s)
        if(user == null)
            user = createTestHUser(test, userSystemApi);
        HyperIoTContext context = null;
        HProject project = new HProject();
        project.setName(UUID.randomUUID().toString());
        project.setDescription("A test project");
        project.setUser(user);
        projectSystemApi.save(project, context);

        HDevice device = new HDevice();
        device.setProject(project);
        device.setDeviceName("TestDevice" + UUID.randomUUID().toString().replaceAll("-", ""));
        device.setDescription("A test device");
        device.setBrand("ACSoftware");
        device.setModel("MultiSensor");
        device.setFirmwareVersion("1.0");
        device.setSoftwareVersion("1.0");
        device.setPassword("passwordPass&01");
        device.setPasswordConfirm("passwordPass&01");
        deviceSystemApi.save(device, context);

        HPacket packet1 = new HPacket();
        packet1.setDevice(device);
        packet1.setVersion("1.0");
        packet1.setName("MultiSensor data");
        packet1.setType(HPacketType.OUTPUT);
        packet1.setFormat(HPacketFormat.JSON);
        packet1.setTrafficPlan(HPacketTrafficPlan.LOW);
        packet1.setUnixTimestamp(true);
        packet1.setTimestampFormat("GGMMYYYY");
        packet1.setTimestampField("timestamp");
        packet1.setSerialization(HPacketSerialization.AVRO);


        HPacketField field1 = new HPacketField();
        field1.setPacket(packet1);
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(23.52d);

        HPacketField field2 = new HPacketField();
        field2.setPacket(packet1);
        field2.setName("humidity");
        field2.setDescription("Humidity");
        field2.setType(HPacketFieldType.DOUBLE);
        field2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field2.setValue(42.75);

        packet1.setFields(new HashSet<>() {
            {
                add(field1);
                add(field2);
            }
        });

        HPacket packet2 = new HPacket();
        packet2.setDevice(device);
        packet2.setVersion("1.0");
        packet2.setName("GPS data");
        packet2.setType(HPacketType.OUTPUT);
        packet2.setFormat(HPacketFormat.JSON);
        packet2.setTrafficPlan(HPacketTrafficPlan.LOW);
        packet2.setUnixTimestamp(true);
        packet2.setTimestampFormat("GGMMYYYY");
        packet2.setTimestampField("timestamp");
        packet2.setSerialization(HPacketSerialization.AVRO);

        HPacketField field3 = new HPacketField();
        field3.setPacket(packet2);
        field3.setName("gps");
        field3.setDescription("GPS");
        field3.setType(HPacketFieldType.OBJECT);
        field3.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
        HPacketField field3_1 = new HPacketField();
        field3_1.setName("longitude");
        field3_1.setDescription("GPS Longitude");
        field3_1.setType(HPacketFieldType.DOUBLE);
        field3_1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field3_1.setParentField(field3);
        field3_1.setValue(48.243d);
        HPacketField field3_2 = new HPacketField();
        field3_2.setName("latitude");
        field3_2.setDescription("GPS Latitude");
        field3_2.setType(HPacketFieldType.DOUBLE);
        field3_2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field3_2.setParentField(field3);
        field3_2.setValue(38.123d);

        HashSet<HPacketField> gpsFields = new HashSet<>();
        gpsFields.add(field3_1);
        gpsFields.add(field3_2);
        field3.setInnerFields(gpsFields);

        packet2.setFields(new HashSet<>() {
            {
                add(field3);
            }
        });

        packetSystemApi.save(packet1, context);
        packetSystemApi.save(packet2, context);

        // KIE-Drools

        Rule rule1 = new Rule();
        rule1.setName("Add category rule 1");
        rule1.setDescription("Ambarabaccicicocò");
        rule1.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule1.setActions(actions);
        rule1.setProject(project);
        rule1.setPacket(packet1);
        rule1.setRuleDefinition("temperature >= 23 AND humidity > 36");
        ruleEngineSystemApi.save(rule1, context);
        //System.out.println(rule1.droolsDefinition());

        Rule rule2 = new Rule();
        rule2.setName("Add category rule 2");
        rule2.setDescription("Ambarabaccicicocò");
        rule2.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action2 = new AddCategoryRuleAction();
        action2.setCategoryIds(new long[]{123});
        List<RuleAction> actions2 = new ArrayList<>();
        actions2.add(action2);
        rule2.setActions(actions2);
        rule2.setProject(project);
        rule2.setPacket(packet2);
        rule2.setRuleDefinition("gps.latitude >= 3 AND temperature > 6");
        ruleEngineSystemApi.save(rule2, context);
        //System.out.println(rule2.droolsDefinition());

        Rule rule3 = new Rule();
        rule3.setName("Event action rule 1");
        rule3.setDescription("Send email");
        rule3.setType(RuleType.EVENT);
        SendMailAction action3 = new SendMailAction();
        action3.setRecipients("someone@somewhere.net");
        action3.setSubject("Sensor alert");
        action3.setBody("This is a test message.\nHello World!\n");
        List<RuleAction> actions3 = new ArrayList<>();
        actions3.add(action3);
        rule3.setActions(actions3);
        rule3.setProject(project);
        rule3.setPacket(packet1);
        rule3.setRuleDefinition("humidity >= 40 AND temperature > 21");
        ruleEngineSystemApi.save(rule3, context);
        return project;
    }

    // Utility: create new HUser
    private static HUser createTestHUser(KarafTestSupport test, HUserSystemApi userSystemApi) {
        AuthenticationApi authService = test.getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.isAdmin());
        HUser testUser = new HUser();
        testUser.setName("name" + java.util.UUID.randomUUID());
        testUser.setLastname("lastname" + java.util.UUID.randomUUID());
        testUser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        testUser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
        testUser.setPassword("passwordPass&01");
        testUser.setPasswordConfirm("passwordPass&01");
        testUser.setAdmin(false);
        userSystemApi.save(testUser, null);
        return testUser;
    }
}

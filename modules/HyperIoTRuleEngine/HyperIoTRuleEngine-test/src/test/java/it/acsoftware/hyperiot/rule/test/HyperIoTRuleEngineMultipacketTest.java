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

import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.service.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.service.RuleEngine;
import it.acsoftware.hyperiot.rule.service.rest.RuleEngineRestApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.Response;
import java.util.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRuleEngineMultipacketTest extends KarafTestSupport {

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


    @Test
    public void test01_checkMultiPacketRuleShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject project = new HProject();
        project.setName("HProject" + java.util.UUID.randomUUID());
        project.setDescription("A test project");
        project.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseProject = hprojectRestService.saveHProject(project);
        Assert.assertEquals(200, restResponseProject.getStatus());

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
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseDevice = hDeviceRestApi.saveHDevice(device);
        Assert.assertEquals(200, restResponseDevice.getStatus());

        this.impersonateUser(hPacketRestApi, adminUser);
        HPacket packet1 = new HPacket();
        packet1.setDevice(device);
        packet1.setVersion("1.0");
        packet1.setName("Temperature data");
        packet1.setType(HPacketType.OUTPUT);
        packet1.setFormat(HPacketFormat.JSON);
        packet1.setSerialization(HPacketSerialization.AVRO);

        packet1.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        packet1.setTimestampField(String.valueOf(timestamp));
        packet1.setTimestampFormat("String");

        HPacketField field1 = new HPacketField();
        field1.setPacket(packet1);
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(20.00);

        packet1.setFields(new HashSet<>() {
            {
                add(field1);
            }
        });

        HPacket packet2 = new HPacket();
        packet2.setDevice(device);
        packet2.setVersion("1.0");
        packet2.setName("Humidity data");
        packet2.setType(HPacketType.OUTPUT);
        packet2.setFormat(HPacketFormat.JSON);
        packet2.setSerialization(HPacketSerialization.AVRO);

        packet2.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp2 = new Date();
        packet2.setTimestampField(String.valueOf(timestamp2));
        packet2.setTimestampFormat("String");

        HPacketField field2 = new HPacketField();
        field2.setPacket(packet2);
        field2.setName("humidity");
        field2.setDescription("Humidity");
        field2.setType(HPacketFieldType.DOUBLE);
        field2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field2.setValue(37.00);

        packet2.setFields(new HashSet<>() {
            {
                add(field2);
            }
        });

        Response restResponsePacket1 = hPacketRestApi.saveHPacket(packet1);
        Assert.assertEquals(200, restResponsePacket1.getStatus());
        Response restResponsePacket2 = hPacketRestApi.saveHPacket(packet2);
        Assert.assertEquals(200, restResponsePacket2.getStatus());

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Rule rule1 = new Rule();
        rule1.setName("Add category rule 1");
        rule1.setDescription("Rule description");
        rule1.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule1.setActions(actions);
        rule1.setProject(project);
        rule1.setRuleDefinition("\"" + packet1.getId() + ".temperature\" > 35");
        Response restResponseRule = ruleEngineRestApi.saveRule(rule1);
        Assert.assertEquals(200, restResponseRule.getStatus());

        StringBuilder header = new StringBuilder();
        header.append("package ").append(rule1.getType().getDroolsPackage()).append(";\n\n")
                .append("import it.acsoftware.hyperiot.hpacket.model.HPacket;\n")
                .append("import it.acsoftware.hyperiot.rule.model.actions.RuleAction;\n")
                .append("import it.acsoftware.hyperiot.rule.service.RuleEngine;\n")
                .append("import java.util.ArrayList;\n");
        header.append("global java.util.ArrayList actions;\n");
        header.append("\n").append("dialect  \"mvel\"\n\n").append(rule1.droolsDefinition());
        System.out.println(header);

        Rule rule2 = new Rule();
        rule2.setName("Add category rule 2");
        rule2.setDescription("Rule description");
        rule2.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action2 = new AddCategoryRuleAction();
        action2.setCategoryIds(new long[]{123});
        List<RuleAction> actions2 = new ArrayList<>();
        actions2.add(action2);
        rule2.setActions(actions2);
        rule2.setProject(project);
        rule2.setRuleDefinition("\"" + packet1.getId() + ".temperature\" >= 23 AND \"" + packet2.getId() + ".humidity\" > 36");
        Response restResponseRule2 = ruleEngineRestApi.saveRule(rule2);
        Assert.assertEquals(200, restResponseRule2.getStatus());

        StringBuilder header2 = new StringBuilder();
        header2.append("package ").append(rule2.getType().getDroolsPackage()).append(";\n\n")
                .append("import it.acsoftware.hyperiot.hpacket.model.HPacket;\n")
                .append("import it.acsoftware.hyperiot.rule.model.actions.RuleAction;\n").append("\n")
                .append("import it.acsoftware.hyperiot.rule.service.RuleEngine;\n").append("\n")
                .append("import java.util.ArrayList;\n");
        header2.append("global java.util.ArrayList actions;\n");
        header2.append("dialect  \"mvel\"\n\n").append(rule2.droolsDefinition());
        System.out.println(header2);

		RuleEngine engine = new RuleEngine(Collections.singletonList(header2.toString()), project.getId());
        engine.check(packet1,System.currentTimeMillis());
		engine.check(packet2,System.currentTimeMillis());
        field1.setValue(24.00);
        engine.check(packet1,System.currentTimeMillis());  // here rule is satisfied and actions will be applied
		engine.disposeSession();
    }

    /**
     * This test checks if mail is sent once when rule is satisfied more than once sequentially
     */
    @Test
    public void test02_checkMultiPacketRuleSendMailShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HProject project = new HProject();
        project.setName("HProject" + java.util.UUID.randomUUID());
        project.setDescription("A test project");
        project.setUser((HUser) adminUser);
        this.impersonateUser(hprojectRestService, adminUser);
        Response restResponseProject = hprojectRestService.saveHProject(project);
        Assert.assertEquals(200, restResponseProject.getStatus());

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
        this.impersonateUser(hDeviceRestApi, adminUser);
        Response restResponseDevice = hDeviceRestApi.saveHDevice(device);
        Assert.assertEquals(200, restResponseDevice.getStatus());

        this.impersonateUser(hPacketRestApi, adminUser);
        HPacket packet1 = new HPacket();
        packet1.setDevice(device);
        packet1.setVersion("1.0");
        packet1.setName("Temperature data");
        packet1.setType(HPacketType.OUTPUT);
        packet1.setFormat(HPacketFormat.JSON);
        packet1.setSerialization(HPacketSerialization.AVRO);

        packet1.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        packet1.setTimestampField(String.valueOf(timestamp));
        packet1.setTimestampFormat("String");

        HPacketField field1 = new HPacketField();
        field1.setPacket(packet1);
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(20.00);

        packet1.setFields(new HashSet<>() {
            {
                add(field1);
            }
        });

        HPacket packet2 = new HPacket();
        packet2.setDevice(device);
        packet2.setVersion("1.0");
        packet2.setName("Humidity data");
        packet2.setType(HPacketType.OUTPUT);
        packet2.setFormat(HPacketFormat.JSON);
        packet2.setSerialization(HPacketSerialization.AVRO);

        packet2.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp2 = new Date();
        packet2.setTimestampField(String.valueOf(timestamp2));
        packet2.setTimestampFormat("String");

        HPacketField field2 = new HPacketField();
        field2.setPacket(packet2);
        field2.setName("humidity");
        field2.setDescription("Humidity");
        field2.setType(HPacketFieldType.DOUBLE);
        field2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field2.setValue(37.00);

        packet2.setFields(new HashSet<>() {
            {
                add(field2);
            }
        });

        Response restResponsePacket1 = hPacketRestApi.saveHPacket(packet1);
        Assert.assertEquals(200, restResponsePacket1.getStatus());
        Response restResponsePacket2 = hPacketRestApi.saveHPacket(packet2);
        Assert.assertEquals(200, restResponsePacket2.getStatus());

        this.impersonateUser(ruleEngineRestApi, adminUser);
        Rule rule1 = new Rule();
        rule1.setName("Add category rule 1");
        rule1.setDescription("Rule description");
        rule1.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule1.setActions(actions);
        rule1.setProject(project);
        rule1.setRuleDefinition("\"" + packet1.getId() + ".temperature\" >= 23 AND \"" + packet2.getId() + ".humidity\" > 36");
        Response restResponseRule = ruleEngineRestApi.saveRule(rule1);
        Assert.assertEquals(200, restResponseRule.getStatus());

        StringBuilder header = new StringBuilder();
        header.append("package ").append(rule1.getType().getDroolsPackage()).append(";\n\n")
                .append("import it.acsoftware.hyperiot.hpacket.model.HPacket;\n")
                .append("import it.acsoftware.hyperiot.rule.model.actions.RuleAction;\n")
                .append("import it.acsoftware.hyperiot.rule.service.RuleEngine;\n")
                .append("import java.util.ArrayList;\n");
        header.append("global java.util.ArrayList actions;\n");
        header.append("global Boolean fireRule;\n");
        header.append("\n").append("dialect  \"mvel\"\n\n").append(rule1.droolsDefinition());
        System.out.println(header);

        RuleEngine engine = new RuleEngine(Collections.singletonList(header.toString()), project.getId());
        engine.check(packet1,System.currentTimeMillis());
        engine.check(packet2,System.currentTimeMillis());
        field1.setValue(24.00);
        engine.check(packet1,System.currentTimeMillis());  // here rule is satisfied and actions will be applied
        engine.check(packet1,System.currentTimeMillis());  // rule is satisfied again, but not fired
        field1.setValue(20.00);
        engine.check(packet1,System.currentTimeMillis());  // rule is not satisfied anymore
        field1.setValue(24.00);
        engine.check(packet1,System.currentTimeMillis());  // rule is satisfied, fire it
        engine.disposeSession();
    }

}

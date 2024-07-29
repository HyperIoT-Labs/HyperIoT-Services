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

import it.acsoftware.hyperiot.rule.model.operations.RootRuleNode;
import it.acsoftware.hyperiot.rule.model.operations.RuleNode;
import it.acsoftware.hyperiot.rule.model.operations.RuleParser;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRuleParserTest extends KarafTestSupport {
    @Override
    public Option[] config() {
        return null;
    }

    @Test
    public void test01_SimpleRule() throws IOException, InstantiationException, IllegalAccessException {
        String rule = "\"123.23\" > 50 AND \"139.25\" < 2";
        RuleParser parser = new RuleParser(new StreamTokenizer(new StringReader(rule)));
        RootRuleNode rn = parser.parse();
        List<Long> packetIds = new ArrayList<>();
        packetIds.addAll(rn.getPacketIds());
        System.out.println(rn.getRuleNode().getDefinition());
        Assert.assertEquals(2,packetIds.size());
        Assert.assertEquals(123l,(long)packetIds.get(0));
        Assert.assertEquals(139l,(long)packetIds.get(1));
        Assert.assertNotNull(rn);
    }

    //TODO move parser unit test here

    @Test
    public void test02_fieldFunction() throws IOException, InstantiationException, IllegalAccessException {
        Assert.assertEquals("day(timestamp)>2.0", parse("\"123.day(timestamp)\" > 2").getDefinition());
    }

    @Test(expected = RuntimeException.class)
    public void test03_fieldFunctionNotFound() throws IOException, InstantiationException, IllegalAccessException, RuntimeException {
        parse("\"123.fakeFunction(temperature,2)\" > 3");
    }

    @Test(expected = RuntimeException.class)
    public void test03_fieldFunctionWrongParamNumber() throws IOException, InstantiationException, IllegalAccessException, RuntimeException {
        parse("\"123.day(timestamp,2)\" > 3");
    }

    @Test
    public void test04_checkDroolsRule() throws IOException, InstantiationException, IllegalAccessException {
        RuleNode rn = parse("\"123.day(12)\" > 4");
        Assert.assertNotNull(rn);
        Assert.assertEquals("eval(LocalDate.ofInstant(Instant.ofEpochMilli(packet_123.getFieldValue(\"12.0\")), ZoneId.systemDefault()).getDayOfMonth() > 4.0)", rn.droolsDefinition());
    }

    @Test(expected = RuntimeException.class)
    public void test05_wrongExpression() throws IOException, InstantiationException, IllegalAccessException {
        RuleNode rn = parse("\"123.day(12,\" > 4");
        Assert.assertNotNull(rn);
    }

    private RuleNode parse(String rule) throws IOException, InstantiationException, IllegalAccessException {
        RuleParser parser = new RuleParser(new StreamTokenizer(new StringReader(rule)));
        return parser.parse().getRuleNode();
    }
}

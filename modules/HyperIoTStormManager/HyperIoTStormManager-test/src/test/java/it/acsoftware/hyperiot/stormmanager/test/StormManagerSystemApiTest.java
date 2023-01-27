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

import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import org.apache.karaf.itests.KarafTestSupport;
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

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-11 Initial release
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StormManagerSystemApiTest extends KarafTestSupport {
    private static long projectId = 0;

    //force global configuration
    public Option[] config() {
        return null;
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @Test
    public void test0_buildTopology() throws IOException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        HProject project = StormManagerTestUtil.createHProject(this,null);
        projectId = project.getId();
        // Generate topology properties and YAML files
        TopologyInfo topologyConfig = stormManagerSystemApi.getTopologyStatus(project.getId());
        assertNotNull(topologyConfig);
    }

    @Test
    public void test1_submitTestTopology() throws InterruptedException, IOException {
        //forcing to wait in order to let hbase create table
        System.out.println("Waiting 10sec in order to let hbase table creation finish...");
        Thread.sleep(10000);
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.submitProjectTopology(projectId);
    }

    @Test
    public void test2_topologyListShouldContainTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        String topologyList = stormManagerSystemApi.getTopologyList();
        // should contain test topology
        assertTrue(topologyList.contains(stormManagerSystemApi.getTopologyName(projectId)));
    }

    @Test
    public void test3_activateTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.activateTopology(stormManagerSystemApi.getTopologyName(projectId));
    }

    @Test
    public void test4_testTopologyShouldBeActive() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        TopologyInfo topologyStatus = stormManagerSystemApi.getTopologyStatus(projectId);
        assertTrue(topologyStatus != null && topologyStatus.getStatus().equals(TopologyInfo.TOPOLOGY_STATUS_ACTIVE));
    }

    @Test
    public void test5_deactivateTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.deactivateTopology(stormManagerSystemApi.getTopologyName(projectId));
    }

    @Test
    public void test6_testTopologyShouldBeInactive() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        TopologyInfo topologyStatus = stormManagerSystemApi.getTopologyStatus(projectId);
        assertTrue(topologyStatus != null && topologyStatus.getStatus().equals("INACTIVE"));
    }

    @Test
    public void test8_killTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.killTopology(stormManagerSystemApi.getTopologyName(projectId));
    }

    @Test
    public void test9_topologyListShouldNotContainTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        String topologyList = stormManagerSystemApi.getTopologyList();
        System.out.println(topologyList);

    }


}

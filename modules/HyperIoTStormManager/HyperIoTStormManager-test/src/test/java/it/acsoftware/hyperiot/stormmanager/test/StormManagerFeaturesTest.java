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

import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.stormmanager.service.rest.StormManagerRestApi;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.Response;

/**
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-11 Initial release
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StormManagerFeaturesTest extends KarafTestSupport {

    //force global configuration
    public Option[] config() {
        return null;
    }

    @Before
    public void initPlatformContainers() {
        HyperIoTServicesTestUtil.initPlatformContainers();
    }

    @Test
    public void test00_hyperIoTFrameworkShouldBeInstalled() throws Exception {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class, 0);
        String features = executeCommand("feature:list -i");
        System.out.println(features);
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTStormManager-features ", features);
        String datasource = executeCommand("jdbc:ds-list");
        System.out.println(datasource);
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01_stormManagerRestModuleShouldWork() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        // Test StormManagerRestApi module
        Response response = stormManagerRestApi.checkModuleWorking();
        Assert.assertEquals(200, response.getStatus());
    }

    // TODO: implement test for ensuring that Storm folder and binaries are set

}

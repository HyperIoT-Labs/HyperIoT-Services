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

import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;

/**
 * 
 * @author Aristide Cittadino Interface component for AlarmEvent System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAlarmEventTest extends KarafTestSupport {

	//forcing global config
	public Option[] config() {
		return null;
	}

	public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi,HyperIoTUser user) {
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
	public void hyperIoTFrameworkShouldBeInstalled() throws Exception {
		// assert on an available service
		assertServiceAvailable(FeaturesService.class,0);
		String features = executeCommand("feature:list -i");
		assertContains("HyperIoTBase-features ", features);
		assertContains("HyperIoTPermission-features ", features);
		assertContains("HyperIoTHUser-features ", features);
		assertContains("HyperIoTAuthentication-features ", features);
		String datasource = executeCommand("jdbc:ds-list");
		assertContains("hyperiot", datasource);
	}

}
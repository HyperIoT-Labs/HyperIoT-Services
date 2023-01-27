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

package it.acsoftware.hyperiot.hproject.test;

import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTNoResultException;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.features.FeaturesService;
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

import static it.acsoftware.hyperiot.hproject.test.HyperIoTHProjectConfiguration.*;

/**
 *
 * @author Aristide Cittadino Interface component for HProject System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHProjectSystemServiceTest extends KarafTestSupport {

	//force global config
	@Override
	public Option[] config() {
		return null;
	}

	public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
		restApi.impersonate(user);
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
	public void test00s_hyperIoTFrameworkShouldBeInstalled() {
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
		String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
		assertContains("hyperiot", datasource);
	}

	@Test
	public void test01s_getHProjectAreaListShouldWork() {
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		// hadmin find all HProject Area list with the following call
		// getAreasList
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HyperIoTContext ctx = hprojectRestService.impersonate(adminUser);
		HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
		HProject hproject = createHProject();
		AreaSystemApi areaRestApi = getOsgiService(AreaSystemApi.class);
		Area area = new Area();
		area.setName("Area " + java.util.UUID.randomUUID());
		area.setDescription("Description");
		area.setProject(hproject);
		areaRestApi.save(area, ctx);
		Collection<Area> projectAreas = hProjectSystemApi.getAreasList(hproject.getId());
		Assert.assertFalse(projectAreas.isEmpty());
		boolean areaFound = false;
		for (Area listHProjectArea : projectAreas) {
			if (area.getId() == listHProjectArea.getId()) {
				areaFound = true;
			}
		}
		Assert.assertTrue(areaFound);
	}

	@Test
	public void test02s_getHProjectAreaListShouldWorkIfHProjectAreaListIsEmpty() {
		HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
		// hadmin find all HProject Area list with the following call
		// getAreasList, list of projectAreas is empty
		HProject hproject = createHProject();
		Collection<Area> projectAreas = hProjectSystemApi.getAreasList(hproject.getId());
		Assert.assertTrue(projectAreas.isEmpty());
	}

	@Test
	public void test03s_getHProjectAreaListShouldFailIfHProjectNotFound() throws HyperIoTEntityNotFound {
		HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
		// the following call getAreasList tries to find all HProject Area list but
		// HProject not found
		boolean entityFound = true;
		try {
			hProjectSystemApi.getAreasList(0);
		} catch (HyperIoTEntityNotFound e) {
			entityFound = false;
		}
		Assert.assertFalse(entityFound);
	}

	@Test
	public void test04s_removeHUserShouldRemoveHUserProjects(){
		HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
		//hUser create HProject.
		//hadmin delete hUser
		//Assert that hProject, related to hUser, will be deleted
		HUser hUser = createHUser();
		Assert.assertNotEquals(0, hUser.getId());
		HProject project = createHProject(hUser);
		Assert.assertNotEquals(0 , project.getId());
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		impersonateUser(hUserRestApi, adminUser);
		Response response = hUserRestApi.remove(hUser.getId());
		Assert.assertEquals(200, response.getStatus());
		boolean projectExist = true ;
		try{
			hProjectSystemApi.find(project.getId(), null);
		} catch (HyperIoTNoResultException e ){
			projectExist = false;
		}
		Assert.assertFalse(projectExist);
	}

	/*
	 *
	 *
	 * UTILITY METHODS
	 *
	 *
	 */

	private HProject createHProject() {
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HyperIoTContext ctx = hprojectRestService.impersonate(adminUser);
		HProject hproject = new HProject();
		hproject.setName("Project " + java.util.UUID.randomUUID());
		hproject.setDescription("Project of user: " + adminUser.getUsername());
		hproject.setUser((HUser) adminUser);
		hProjectSystemApi.save(hproject, ctx);
		return hproject;
	}

	private HProject createHProject(HUser user) {
		HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
		HProject hproject = new HProject();
		hproject.setName("Project " + java.util.UUID.randomUUID());
		hproject.setDescription("Project of user: " + java.util.UUID.randomUUID());
		hproject.setUser(user);
		hProjectSystemApi.save(hproject, null);
		return hproject;
	}

	private HUser createHUser() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		String username = "TestUser"+ UUID.randomUUID().toString().replaceAll("-", "");
		List<Object> roles = new ArrayList<>();
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername(username + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		huser.setAdmin(false);
		huser.setActive(true);
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
		Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
		Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
		Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
		Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
		Assert.assertFalse(huser.isAdmin());
		Assert.assertTrue(huser.isActive());
		Assert.assertTrue(roles.isEmpty());
		return huser;
	}

}

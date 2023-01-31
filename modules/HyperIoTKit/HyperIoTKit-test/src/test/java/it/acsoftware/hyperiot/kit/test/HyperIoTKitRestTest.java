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

package it.acsoftware.hyperiot.kit.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTNoResultException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.model.HyperIoTPaginatedResult;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.util.HyperIoTTestUtils;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.api.HUserRepository;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.huser.test.util.HyperIoTHUserTestUtils;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.kit.service.KitUtils;
import it.acsoftware.hyperiot.kit.service.rest.KitRestApi;
import it.acsoftware.hyperiot.kit.template.api.HDeviceTemplateRepository;
import it.acsoftware.hyperiot.kit.template.api.HPacketFieldTemplateRepository;
import it.acsoftware.hyperiot.kit.template.api.HPacketTemplateRepository;
import it.acsoftware.hyperiot.kit.template.model.HDeviceTemplate;
import it.acsoftware.hyperiot.kit.template.model.HPacketFieldTemplate;
import it.acsoftware.hyperiot.kit.template.model.HPacketTemplate;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.test.util.HyperIoTPermissionTestUtil;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * 
 * @author Aristide Cittadino Interface component for Kit System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTKitRestTest extends KarafTestSupport {

	//TODO Check if  user with RegisteredUser role is authorized to share a project owned by him.
	//TODO Add test to check field's validation when update a Kit
	//TODO Add test relative to kit's tag handling

	@Configuration
	public Option[] config() {
		return HyperIoTServicesTestConfigurationBuilder
				.createStandardConfiguration()
				.build();
	}

	@Before
	public void initPlatformContainers() {
		HyperIoTServicesTestUtil.initPlatformContainers();
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

	@Test
	public void test_000_hyperIoTFrameworkShouldBeInstalled() throws Exception {
		// assert on an available service
		assertServiceAvailable(FeaturesService.class,0);
		String features = executeCommand("feature:list -i");
		assertContains("HyperIoTBase-features ", features);
		assertContains("HyperIoTPermission-features ", features);
		assertContains("HyperIoTHUser-features ", features);
		assertContains("HyperIoTAuthentication-features ", features);
		assertContains("HyperIoTSharedEntity-features",features);
		assertContains("HyperIoTKit-features", features);
		assertContains("HyperIoTHProject-features", features);
		String datasource = executeCommand("jdbc:ds-list");
		assertContains("hyperiot", datasource);
	}

	@Test
	public void test_001_hyperiotPlatformShouldBeInstalled() throws Exception{
		assertServiceAvailable(FeaturesService.class,0);
		String features = executeCommand("feature:list -i");
		assertContains("HyperIoTHProject-features ", features);
		assertContains("HyperIoTKit-features ", features);
	}

	@Test
	public void test_002_should_checkModuleWorking_works(){
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(kitRestService, adminUser);
		Response restResponse = kitRestService.checkModuleWorking();
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals("Kit Module works!", restResponse.getEntity());
	}

	@Test
	public void test_003_should_saveKit_works_whenKitIsSavedByGenericUser() throws JsonProcessingException {
		//Create and register generic HUser (no admin user)
		//Create project
		//Create kit (with device/packet/packetField).
		//Save kit
		//Assert that operation is authorized
		//Assert the semantic of the operation.


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());


		//Create first device for kit
		HDeviceTemplate kitDevOne = createHDeviceTemplate("kitDevOne");
		HPacketTemplate devOnePacketOne = createHPacketTemplate("devOnePacketOne", "1.0");
		HPacketFieldTemplate devOnePacketOneFieldOne = createHPacketFieldTemplate("devOnePacketOneFieldOne");
		HPacketFieldTemplate devOnePacketOneFieldTwo = createHPacketFieldTemplate("devOnePacketOneFieldTwo");

		addPacketFieldTemplateToPacketTemplate(devOnePacketOne,devOnePacketOneFieldOne);
		addPacketFieldTemplateToPacketTemplate(devOnePacketOne,devOnePacketOneFieldTwo);
		addPacketTemplateToDeviceTemplate(kitDevOne,devOnePacketOne);


		HPacketTemplate devOnePacketTwo = createHPacketTemplate("devOnePacketTwo", "1.0");
		HPacketFieldTemplate devOnePacketTwoFieldOne = createHPacketFieldTemplate("devOnePacketTwoFieldOne");
		addPacketFieldTemplateToPacketTemplate(devOnePacketTwo,devOnePacketTwoFieldOne);
		addPacketTemplateToDeviceTemplate(kitDevOne,devOnePacketTwo);

		addDeviceTemplateToKit(kit,kitDevOne);


		//Create second device for kit
		HDeviceTemplate kitDevTwo = createHDeviceTemplate("kitDevTwo");
		HPacketTemplate devTwoPacketOne = createHPacketTemplate("devTwoPacketOne", "1.0");
		HPacketFieldTemplate devTwoPacketOneFieldOne = createHPacketFieldTemplate("devTwoPacketOneFieldOne");
		HPacketFieldTemplate devTwoPacketOneFieldTwo = createHPacketFieldTemplate("devTwoPacketOneFieldTwo");

		addPacketFieldTemplateToPacketTemplate(devTwoPacketOne,devTwoPacketOneFieldOne);
		addPacketFieldTemplateToPacketTemplate(devTwoPacketOne,devTwoPacketOneFieldTwo);
		addPacketTemplateToDeviceTemplate(kitDevTwo,devTwoPacketOne);

		addDeviceTemplateToKit(kit,kitDevTwo);

		//Clone kit .
		Kit expectedKit = cloneKit(kit);

		//Log as generic user.
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);

		//Save kit .
		Response saveKitResponse = kitRestService.saveKit(kit);
		//Assert that operation is authorized.
		assertEquals(200,saveKitResponse.getStatus());
		//Assert the coherency of the operation.
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);
		assertNotEquals(0,savedKit.getId());
		assertEquals(expectedKit.getLabel(),savedKit.getLabel());
		assertEquals(expectedKit.getKitVersion(),savedKit.getKitVersion());
		assertEquals(expectedKit.getProjectId(),savedKit.getProjectId());
		assertTrue(savedKit.getDevices() != null && expectedKit.getDevices().size() == savedKit.getDevices().size());
		for(HDeviceTemplate kitDevice : savedKit.getDevices()) {
			//Assert that saved kit contain expected device.
			List<HDeviceTemplate> expDevice = expectedKit.getDevices().stream().filter((deviceTemplate) -> deviceTemplate.getDeviceLabel().equals(kitDevice.getDeviceLabel())).collect(Collectors.toList());
			assertEquals(1, expDevice.size());
			HDeviceTemplate expectedDeviceTemplate = expDevice.get(0);
			assertTrue(expectedDeviceTemplate.getPackets() != null && expectedDeviceTemplate.getPackets().size() == kitDevice.getPackets().size());
			for (HPacketTemplate packetTemplate : kitDevice.getPackets()) {
				List<HPacketTemplate> expPacket = expectedDeviceTemplate.getPackets().stream().filter((pktTemplate) -> pktTemplate.getName().equals(packetTemplate.getName())).collect(Collectors.toList());
				assertEquals(1, expPacket.size());
				HPacketTemplate expectedPacketTemplate = expPacket.get(0);
				assertTrue(expectedPacketTemplate.getFields() != null && expectedPacketTemplate.getFields().size() == packetTemplate.getFields().size());
				for (HPacketFieldTemplate packetFieldTemplate : packetTemplate.getFields()) {
					List<HPacketFieldTemplate> expFields = expectedPacketTemplate.getFields().stream().filter((fldTemplate) -> fldTemplate.getName().equals(packetFieldTemplate.getName())).collect(Collectors.toList());
					assertEquals(1, expFields.size());
				}
			}
		}
	}

	@Test
	public void test_004_should_saveKit_isNotAuthorized_whenGenericUserSaveSystemKit(){
		//Create and register generic HUser (no admin user)
		//Create kit and set kit's project id with 0 (Project id is set to 0 when the project is a system kit).
		//Save kit
		//Assert that operation is not authorized.

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create SystemKit
		Kit systemKit = createKitTemplate("kitLabel", KitUtils.SYSTEM_KIT_PROJECT_ID);

		//Log as generic user.
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);

		//Save kit .
		Response saveKitResponse = kitRestService.saveKit(systemKit);
		assertEquals(403,saveKitResponse.getStatus());
	}

	@Test
	public void test_005_should_saveKit_isAuthorized_whenAdministratorUserSaveSystemKit(){
		//Log as administrator user
		//Create kit and set kit's project id with 0 (Project id is set to 0 when the project is a system kit).
		//Save kit
		//Assert that operation is  authorized.
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);
		//Create SystemKit
		Kit systemKit = createKitTemplate("kitLabel",KitUtils.SYSTEM_KIT_PROJECT_ID);
		addDeviceTemplateToKit(systemKit,createHDeviceTemplate("deviceLabel"));
		Response saveSystemKitResponse = kitRestService.saveKit(systemKit);
		//Assert that operation is authorized.
		assertEquals(200,saveSystemKitResponse.getStatus());

	}

	@Test
	public void test_006_should_saveKit_fail_whenKitLabelIsEmpty(){
		//Log as administrator user
		//Set building kit label with empty string (Other's kit field must be valid).
		//Save kit
		//Assert that operation fail and that fail's cause is relative to validation
		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		kit.setLabel("");
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		//Assert that operation fail
		//Assert that the fail's cause is validation.
		assertEquals(422,saveSystemKitResponse.getStatus());
		assertTrue(saveSystemKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveSystemKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTValidationExceptionClassName(),errorResponse.getType());
		assertEquals(1,errorResponse.getValidationErrors().size());
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-label");
	}

	@Test
	public void test_007_should_saveKit_fail_whenKitLabelIsNull(){
		//Create generic user
		//Create project.
		//Set building kit label with null string  (Other's kit field must be valid).
		//Save kit
		//Assert that operation fail and that fail's cause is relative to validation


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		//Set kit label to null string
		kit.setLabel(null);
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		//Assert that operation fail
		//Assert that the fail's cause is validation.
		assertEquals(422,saveSystemKitResponse.getStatus());
		assertTrue(saveSystemKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveSystemKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTValidationExceptionClassName(),errorResponse.getType());
		assertEquals(2,errorResponse.getValidationErrors().size());
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-label");
		assertEquals(errorResponse.getValidationErrors().get(1).getField(),"kit-label");
	}

	@Test
	public void test_008_should_saveKit_fail_whenKitLabelContainMaliciousCode(){
		//Create generic user
		//Create project.
		//Set building kit label with malicious value  (Other's kit field must be valid).
		//Save kit
		//Assert that operation fail and that fail's cause is relative to validation


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		kit.setLabel(getMaliciousValue());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit.
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		//Assert that operation fail
		//Assert that the fail's cause is validation.
		assertEquals(422,saveSystemKitResponse.getStatus());
		assertTrue(saveSystemKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveSystemKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTValidationExceptionClassName(),errorResponse.getType());
		assertEquals(1,errorResponse.getValidationErrors().size());
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-label");
	}

	@Test
	public void test_009_should_saveKit_fail_whenKitVersionIsEmptyString(){
		//Create generic user
		//Create project.
		//Set building kit kitversion with emptyString  (Other's kit field must be valid).
		//Save kit
		//Assert that operation fail and that fail's cause is relative to validation


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		kit.setKitVersion("");
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		//Assert that operation fail
		//Assert that the fail's cause is validation.
		assertEquals(422,saveSystemKitResponse.getStatus());
		assertTrue(saveSystemKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveSystemKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTValidationExceptionClassName(),errorResponse.getType());
		assertEquals(1,errorResponse.getValidationErrors().size());
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-kitversion");
	}

	@Test
	public void test_010_should_saveKit_fail_whenKitVersionContainMaliciousCode(){
		//Create generic user
		//Create project.
		//Set building kit kitversion with malicious value  (Other's kit field must be valid).
		//Save kit
		//Assert that operation fail and that fail's cause is relative to validation


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		//Set kit version with malicious value.
		kit.setKitVersion(getMaliciousValue());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		//Assert that operation fail
		//Assert that the fail's cause is validation.
		assertEquals(422,saveSystemKitResponse.getStatus());
		assertTrue(saveSystemKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveSystemKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTValidationExceptionClassName(),errorResponse.getType());
		assertEquals(1,errorResponse.getValidationErrors().size());
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-kitversion");
	}

	@Test
	public void test_011_should_saveKit_fail_whenKitVersionIsNull(){
		//Create generic user
		//Create project.
		//Set building kit kitversion with null value  (Other's kit field must be valid).
		//Save kit
		//Assert that operation fail and that fail's cause is relative to validation


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		//Set kit version to null
		kit.setKitVersion(null);
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		//Assert that operation fail
		//Assert that the fail's cause is validation.
		assertEquals(422,saveSystemKitResponse.getStatus());
		assertTrue(saveSystemKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveSystemKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTValidationExceptionClassName(),errorResponse.getType());
		assertEquals(2,errorResponse.getValidationErrors().size());
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-kitversion");
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-kitversion");
	}

	@Test
	public void test_012_should_saveKit_fail_whenKitVersionContainMaliciousCode(){
		//Create generic user
		//Create project.
		//Set building kit kitversion with emptyString  (Other's kit field must be valid).
		//Save kit
		//Assert that operation fail and that fail's cause is relative to validation


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		//Set kit version with malicious code
		kit.setKitVersion(getMaliciousValue());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		//Assert that operation fail
		//Assert that the fail's cause is validation.
		assertEquals(422,saveSystemKitResponse.getStatus());
		assertTrue(saveSystemKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveSystemKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTValidationExceptionClassName(),errorResponse.getType());
		assertEquals(1,errorResponse.getValidationErrors().size());
		assertEquals(errorResponse.getValidationErrors().get(0).getField(),"kit-kitversion");
	}

	@Test
	public void test_013_should_saveKit_fail_whenDuplicateEntityException(){
		//Create generic user
		//Create project.
		//Create kit
		//Save kit
		//Create second kit such that two kit has equal kitversion/label/projectId
		//Save the second kit
		//Assert that operation fail.


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveSystemKitResponse = kitRestService.saveKit(kit);
		assertEquals(200,saveSystemKitResponse.getStatus());

		//Create second kit.
		Kit duplicatedKit = new Kit();
		duplicatedKit.setLabel(kit.getLabel());
		duplicatedKit.setKitVersion(kit.getKitVersion());
		duplicatedKit.setProjectId(kit.getProjectId());
		duplicatedKit.setDevices(new LinkedList<>());
		addDeviceTemplateToKit(duplicatedKit,createHDeviceTemplate("deviceLabel"));

		Response saveDuplicatedKitResponse = kitRestService.save(duplicatedKit);
		assertEquals(409,saveDuplicatedKitResponse.getStatus());
		assertTrue(saveDuplicatedKitResponse.getEntity() instanceof HyperIoTBaseError);
		HyperIoTBaseError errorResponse = ((HyperIoTBaseError) saveDuplicatedKitResponse.getEntity());
		assertNotNull(errorResponse);
		assertEquals(getHyperIoTDuplicateEntityExceptionClassName(),errorResponse.getType());
		assertEquals(3,errorResponse.getErrorMessages().size());
		assertEquals(errorResponse.getErrorMessages().get(0),"kitVersion");
		assertEquals(errorResponse.getErrorMessages().get(1),"label");
		assertEquals(errorResponse.getErrorMessages().get(2),"projectId");
	}

	@Test
	public void test_014_should_saveKit_isNotAuthorized_whenUserTryToAddAKitRelatedToProjectWhichProjectOwnerIsNotTheCurrentUser(){
		//Create generic user
		//Add a HProject with this user.
		//Create second user
		//Create a kit such that the kit's projectId is equal to the project create by the other user
		//Save kit
		//Assert that operation is not authorized.

		//Create user
		HUser genericUser = registerHUserAndActivateAccount();
		//Creat HProject
		HProject project = saveNewHProject(genericUser,USER_PASSWORD);

		//Create second user
		HUser secondUser = createHUserTemplate();
		//Create a kit such that the kit's projectId is equal to the project create by the other user
		Kit kit = createKitTemplate("kitLabel", project.getId());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));
		//Log as second user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,secondUser);
		//Save kit.
		Response saveKitResponse = kitRestService.saveKit(kit);
		assertEquals(403,saveKitResponse.getStatus());
	}

	@Test
	public void test_015_should_findKit_works() throws JsonProcessingException {
		//Crete generic user
		//Create HProject
		//Create kit
		//Save kit
		//Find kit
		//Assert that operation is authorized
		//Assert the coherency of the operation.


		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());


		//Create first device for kit
		HDeviceTemplate kitDevOne = createHDeviceTemplate("kitDevOne");
		HPacketTemplate devOnePacketOne = createHPacketTemplate("devOnePacketOne", "1.0");
		HPacketFieldTemplate devOnePacketOneFieldOne = createHPacketFieldTemplate("devOnePacketOneFieldOne");
		HPacketFieldTemplate devOnePacketOneFieldTwo = createHPacketFieldTemplate("devOnePacketOneFieldTwo");

		addPacketFieldTemplateToPacketTemplate(devOnePacketOne,devOnePacketOneFieldOne);
		addPacketFieldTemplateToPacketTemplate(devOnePacketOne,devOnePacketOneFieldTwo);
		addPacketTemplateToDeviceTemplate(kitDevOne,devOnePacketOne);


		HPacketTemplate devOnePacketTwo = createHPacketTemplate("devOnePacketTwo", "1.0");
		HPacketFieldTemplate devOnePacketTwoFieldOne = createHPacketFieldTemplate("devOnePacketTwoFieldOne");
		addPacketFieldTemplateToPacketTemplate(devOnePacketTwo,devOnePacketTwoFieldOne);
		addPacketTemplateToDeviceTemplate(kitDevOne,devOnePacketTwo);

		addDeviceTemplateToKit(kit,kitDevOne);


		//Create second device for kit
		HDeviceTemplate kitDevTwo = createHDeviceTemplate("kitDevTwo");
		HPacketTemplate devTwoPacketOne = createHPacketTemplate("devTwoPacketOne", "1.0");
		HPacketFieldTemplate devTwoPacketOneFieldOne = createHPacketFieldTemplate("devTwoPacketOneFieldOne");
		HPacketFieldTemplate devTwoPacketOneFieldTwo = createHPacketFieldTemplate("devTwoPacketOneFieldTwo");

		addPacketFieldTemplateToPacketTemplate(devTwoPacketOne,devTwoPacketOneFieldOne);
		addPacketFieldTemplateToPacketTemplate(devTwoPacketOne,devTwoPacketOneFieldTwo);
		addPacketTemplateToDeviceTemplate(kitDevTwo,devTwoPacketOne);

		addDeviceTemplateToKit(kit,kitDevTwo);

		//Clone kit .
		Kit expectedKit = cloneKit(kit);

		//Log as generic user.
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);

		//Save kit .
		Response saveKitResponse = kitRestService.saveKit(kit);
		//Assert that operation is authorized.
		assertEquals(200,saveKitResponse.getStatus());
		//Assert the coherency of the operation.
		Kit kitSaved = (Kit) saveKitResponse.getEntity();
		assertNotNull(kitSaved);


		//Find kit.
		Response findKitResponse = kitRestService.findKit(kitSaved.getId());
		Kit findedKit = (Kit) findKitResponse.getEntity();
		assertEquals(200,findKitResponse.getStatus());
		assertNotNull(findedKit);
		assertNotEquals(0,findedKit.getId());
		assertEquals(expectedKit.getLabel(),findedKit.getLabel());
		assertEquals(expectedKit.getKitVersion(),findedKit.getKitVersion());
		assertEquals(expectedKit.getProjectId(),findedKit.getProjectId());
		assertTrue(findedKit.getDevices() != null && expectedKit.getDevices().size() == findedKit.getDevices().size());
		for(HDeviceTemplate kitDevice : findedKit.getDevices()) {
			//Assert that saved kit contain expected device.
			List<HDeviceTemplate> expDevice = expectedKit.getDevices().stream().filter((deviceTemplate) -> deviceTemplate.getDeviceLabel().equals(kitDevice.getDeviceLabel())).collect(Collectors.toList());
			assertEquals(1, expDevice.size());
			HDeviceTemplate expectedDeviceTemplate = expDevice.get(0);
			assertTrue(expectedDeviceTemplate.getPackets() != null && expectedDeviceTemplate.getPackets().size() == kitDevice.getPackets().size());
			for (HPacketTemplate packetTemplate : kitDevice.getPackets()) {
				List<HPacketTemplate> expPacket = expectedDeviceTemplate.getPackets().stream().filter((pktTemplate) -> pktTemplate.getName().equals(packetTemplate.getName())).collect(Collectors.toList());
				assertEquals(1, expPacket.size());
				HPacketTemplate expectedPacketTemplate = expPacket.get(0);
				assertTrue(expectedPacketTemplate.getFields() != null && expectedPacketTemplate.getFields().size() == packetTemplate.getFields().size());
				for (HPacketFieldTemplate packetFieldTemplate : packetTemplate.getFields()) {
					List<HPacketFieldTemplate> expFields = expectedPacketTemplate.getFields().stream().filter((fldTemplate) -> fldTemplate.getName().equals(packetFieldTemplate.getName())).collect(Collectors.toList());
					assertEquals(1, expFields.size());
				}
			}
		}
	}

	@Test
	public void test_016_should_findKit_isAuthorized_whenGenericUserFindSystemKit() throws JsonProcessingException {
		//Log as admin user
		//Create a system kit
		//Create generic user
		//Find system kit loaded by admin user
		//Assert that operation is authorized.

		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Kit systemKit = createKitTemplate("systemKit",KitUtils.SYSTEM_KIT_PROJECT_ID);
		addDeviceTemplateToKit(systemKit,createHDeviceTemplate("deviceLabel"));
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		//Save system kit
		Response saveSystemKitResponse = kitRestService.saveKit(systemKit);
		assertEquals(200,saveSystemKitResponse.getStatus());
		Kit savedSystemKit = (Kit) saveSystemKitResponse.getEntity();
		assertNotNull(savedSystemKit);

		//Create generic user and log with them .
		HUser genericUser = registerHUserAndActivateAccount();
		//Log
		impersonateUser(kitRestService,genericUser);
		//Find kit and assert that operation is authorized.
		Response findSystemKitResponse = kitRestService.findKit(savedSystemKit.getId());
		assertEquals(200,findSystemKitResponse.getStatus());
	}

	@Test
	public void test_017_should_findKit_isAuthorized_UserRetrieveAKitSuchThatKitProjectIsSharedWithUser(){
		//Create admin user
		//Save project
		//Creat kit related to this project
		//Save kit
		//Create second user
		//Share project with this user
		//Log as second user
		//Assert that second user is authorized to retrieve the kit related to this project.


		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser projectOwner = (HUser) authService.login("hadmin", "admin");

		//Save project
		HProject project = saveNewHProject(projectOwner,"admin");
		//Create kit related to this project
		Kit kit = createKitTemplate("kitLabel",project.getId());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		//Log and save kit.
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,projectOwner);

		//Save kit
		Response saveKitResponse = kitRestService.saveKit(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);

		//Create sharing user
		HUser sharingUser = registerHUserAndActivateAccount();

		//Share project with this user.
		shareHProjectWithUser(project,projectOwner,"admin",sharingUser);
		//Log as sharing user

		impersonateUser(kitRestService,sharingUser);
		//Assert that user is authorized to find kit.

		Response findKitResponse = kitRestService.findKit(savedKit.getId());
		assertEquals(200,findKitResponse.getStatus());

	}

	@Test
	public void test_018_should_findKit_isNotAuthorized_whenUserRetrieveAKitSuchThatKitProjectIsNotSharedWithUser(){
		//Create admin user
		//Save project
		//Creat kit related to this project
		//Save kit
		//Create second user
		//Log as second user
		//Assert that second user is not authorized to retrieve the kit related to this project.


		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser projectOwner = (HUser) authService.login("hadmin", "admin");

		//Save project
		HProject project = saveNewHProject(projectOwner,"admin");
		//Create kit related to this project
		Kit kit = createKitTemplate("kitLabel",project.getId());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("deviceLabel"));

		//Log and save kit.
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,projectOwner);

		//Save kit
		Response saveKitResponse = kitRestService.saveKit(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);

		//Create  user
		HUser genericUser = registerHUserAndActivateAccount();

		//Log as sharing user
		impersonateUser(kitRestService,genericUser);
		//Assert that user is not authorized to retrieve kit.

		Response findKitResponse = kitRestService.findKit(savedKit.getId());
		assertEquals(403,findKitResponse.getStatus());
	}

	@Test
	public void test_019_should_updateKit_works() throws JsonProcessingException {
		//Create generic user
		//Save project
		//Creat kit related to this project
		//Save kit
		//Update kit
		//Assert that operation is authorized
		//Assert coherency of the operation.

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		HDeviceTemplate kitDeviceTemplate = createHDeviceTemplate("deviceLabel");
		addDeviceTemplateToKit(kit,kitDeviceTemplate);

		//Clone kit .
		Kit expectedSavedKit = cloneKit(kit);

		//Log as user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveKitResponse = kitRestService.save(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);

		Kit updateKit = new Kit();
		updateKit.setId(savedKit.getId());
		//We set project id different of the project id of the kit ,and we assert that when we update kit the kit's projectId doesn't change.
		updateKit.setProjectId(savedProject.getId()+1);
		updateKit.setLabel("KitLabelUpdated");
		updateKit.setKitVersion("1.0.0.1");
		updateKit.setDevices(new LinkedList<>());
		HDeviceTemplate deviceTemplateForUpdateKit = createHDeviceTemplate("deviceLabelUpdated");
		addDeviceTemplateToKit(updateKit,deviceTemplateForUpdateKit);

		//Clone updatedKit
		Kit cloneUpdateKit = cloneKit(updateKit);

		Response updateKitResponse = kitRestService.updateKit(updateKit);
		assertEquals(200,updateKitResponse.getStatus());
		Kit updatedKit = (Kit) updateKitResponse.getEntity();
		assertNotNull(updatedKit);
		assertEquals(updatedKit.getId(),savedKit.getId());
		assertEquals(cloneUpdateKit.getLabel(),updatedKit.getLabel());
		assertEquals(cloneUpdateKit.getKitVersion(),updatedKit.getKitVersion());
		//Assert that project id doesn't change with update.
		assertEquals(updateKit.getProjectId(),savedKit.getProjectId());
		//Assert that kit's device list doesn't change with update.
		assertTrue(updatedKit.getDevices().containsAll(savedKit.getDevices()));
	}

	@Test
	public void test_020_should_updateKit_isNotAuthorized_whenKitIsNotASystemKitAndUserIsNotTheOwnerOfKitProject() throws JsonProcessingException {
		//Create generic user
		//Save project
		//Creat kit related to this project
		//Save kit
		//Create another user
		//Share project relative to kit saved before with this user.
		//Log such a user
		//Assert that operation is not authorized

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser adminUser = (HUser) authService.login("hadmin", "admin");
		//Create hProject.
		HProject savedProject = saveNewHProject(adminUser,"admin");
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		HDeviceTemplate kitDeviceTemplate = createHDeviceTemplate("deviceLabel");
		addDeviceTemplateToKit(kit,kitDeviceTemplate);

		//Clone kit .
		Kit expectedSavedKit = cloneKit(kit);

		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		//Save kit
		Response saveKitResponse = kitRestService.save(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);


		//Create another user
		HUser sharingUser = registerHUserAndActivateAccount();


		//Share project with this user.
		shareHProjectWithUser(savedProject,adminUser,"admin",sharingUser);

		//Log as sharing user
		impersonateUser(kitRestService,sharingUser);


		Kit updateKit = new Kit();
		updateKit.setId(savedKit.getId());
		updateKit.setProjectId(savedProject.getId()+1);
		updateKit.setLabel("KitLabelUpdated");
		updateKit.setKitVersion("1.0.0.1");
		updateKit.setDevices(new LinkedList<>());
		HDeviceTemplate deviceTemplateForUpdateKit = createHDeviceTemplate("deviceLabelUpdated");
		addDeviceTemplateToKit(updateKit,deviceTemplateForUpdateKit);

		//Update kit and assert that operation is not authorized.
		Response updateKitResponse = kitRestService.updateKit(updateKit);
		assertEquals(403,updateKitResponse.getStatus());
	}

	@Test
	public void test_021_should_updateKit_isNotAuthorized_whenNonAdminUserUpdateASystemKit(){
		//Log as admin user
		//Create a system kit
		//Create generic user
		//Log as generic user
		//Update kit as generic user
		//Assert that operation is not authorized.
		//Find system kit loaded by admin user
		//Assert that operation is authorized.

		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Kit systemKit = createKitTemplate("systemKit",KitUtils.SYSTEM_KIT_PROJECT_ID);
		addDeviceTemplateToKit(systemKit,createHDeviceTemplate("deviceLabel"));
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		//Save system kit
		Response saveSystemKitResponse = kitRestService.saveKit(systemKit);
		assertEquals(200,saveSystemKitResponse.getStatus());
		Kit savedSystemKit = (Kit) saveSystemKitResponse.getEntity();
		assertNotNull(savedSystemKit);


		//Create generic user and log with them .
		HUser genericUser = registerHUserAndActivateAccount();
		//Log
		impersonateUser(kitRestService,genericUser);

		Kit updateKit = new Kit();
		updateKit.setId(savedSystemKit.getId());
		//We set project id different of the project id of the kit ,and we assert that when we update kit the kit's projectId doesn't change.
		updateKit.setProjectId(savedSystemKit.getProjectId());
		updateKit.setLabel("KitLabelUpdated");
		updateKit.setKitVersion("1.0.0.1");
		updateKit.setDevices(new LinkedList<>());
		HDeviceTemplate deviceTemplateForUpdateKit = createHDeviceTemplate("deviceLabelUpdated");
		addDeviceTemplateToKit(updateKit,deviceTemplateForUpdateKit);

		//Update kit and assert that operation is not authorized.
		Response updateKitResponse = kitRestService.updateKit(updateKit);
		assertEquals(403,updateKitResponse.getStatus());
	}

	@Test
	public void test_022_should_updateKit_isNotAuthorized_whenAdminUserUpdateKitOwnedByAnotherUser() throws JsonProcessingException {
		//Create generic user
		//Save project
		//Create kit related to this project
		//Save kit
		//Log as admin user.
		//Assert that operation is not authorized

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		HDeviceTemplate kitDeviceTemplate = createHDeviceTemplate("deviceLabel");
		addDeviceTemplateToKit(kit,kitDeviceTemplate);

		//Clone kit .
		Kit expectedSavedKit = cloneKit(kit);

		//Log as user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveKitResponse = kitRestService.save(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser administratorUser = (HUser) authService.login("hadmin", "admin");
		impersonateUser(kitRestService,administratorUser);

		Kit updateKit = new Kit();
		updateKit.setId(savedKit.getId());
		//We set project id different of the project id of the kit ,and we assert that when we update kit the kit's projectId doesn't change.
		updateKit.setProjectId(savedKit.getProjectId());
		updateKit.setLabel("KitLabelUpdated");
		updateKit.setKitVersion("1.0.0.1");
		updateKit.setDevices(new LinkedList<>());
		HDeviceTemplate deviceTemplateForUpdateKit = createHDeviceTemplate("deviceLabelUpdated");
		addDeviceTemplateToKit(updateKit,deviceTemplateForUpdateKit);

		//Update kit and assert that operation is not authorized.
		Response updateKitResponse = kitRestService.updateKit(updateKit);
		assertEquals(403,updateKitResponse.getStatus());
	}

	@Test
	public void test_023_should_deleteKit_works() throws JsonProcessingException {
		//Create generic user
		//Save project
		//Create kit related to this project
		//Save kit
		//Delete kit
		//Assert that operation is authorized
		//Assert the coherency of the operation.

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		HDeviceTemplate kitDeviceTemplate = createHDeviceTemplate("deviceLabel");
		addDeviceTemplateToKit(kit,kitDeviceTemplate);


		//Log as user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveKitResponse = kitRestService.save(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);

		//Delete kit
		Response deleteKitResponse = kitRestService.deleteKit(savedKit.getId());
		assertEquals(200,deleteKitResponse.getStatus());

		//Assert that the kit is deleted
		Response findKitResponse = kitRestService.findKit(savedKit.getId());
		assertEquals(404,findKitResponse.getStatus());

	}

	@Test
	public void test_024_should_deleteKit_isAuthorized_whenAdminUserDeleteSystemKit() throws JsonProcessingException {
		//Log as admin user
		//Save system kit
		//Delete system kit
		//Assert that operation is authorized
		//Assert the coherency of the operation.

		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Kit systemKit = createKitTemplate("systemKit",KitUtils.SYSTEM_KIT_PROJECT_ID);
		addDeviceTemplateToKit(systemKit,createHDeviceTemplate("deviceLabel"));
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);


		//Save system kit
		Response saveSystemKitResponse = kitRestService.saveKit(systemKit);
		assertEquals(200,saveSystemKitResponse.getStatus());
		Kit savedSystemKit = (Kit) saveSystemKitResponse.getEntity();
		assertNotNull(savedSystemKit);

		//Delete kit
		Response deleteKitResponse = kitRestService.deleteKit(savedSystemKit.getId());
		assertEquals(200,deleteKitResponse.getStatus());

		//Assert that the kit is deleted
		Response findKitResponse = kitRestService.findKit(savedSystemKit.getId());
		assertEquals(404,findKitResponse.getStatus());

	}

	@Test
	public void test_025_should_deleteKit_isNotAuthorized_whenNonAdminUserDeleteSystemKit() throws JsonProcessingException {
		//Log as admin user
		//Save system kit
		//Create generic user
		//Log as generic user
		//Delete system kit
		//Assert that operation is not authorized
		//Assert the coherency of the operation.

		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Kit systemKit = createKitTemplate("systemKit",KitUtils.SYSTEM_KIT_PROJECT_ID);
		addDeviceTemplateToKit(systemKit,createHDeviceTemplate("deviceLabel"));
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		//Save system kit
		Response saveSystemKitResponse = kitRestService.saveKit(systemKit);
		assertEquals(200,saveSystemKitResponse.getStatus());
		Kit savedSystemKit = (Kit) saveSystemKitResponse.getEntity();
		assertNotNull(savedSystemKit);

		//Create generic user
		HUser genericUser = registerHUserAndActivateAccount();

		//Log  as generic user
		impersonateUser(kitRestService,genericUser);

		//Delete kit
		Response deleteKitResponse = kitRestService.deleteKit(savedSystemKit.getId());
		//Assert that operation is not authorized.
		assertEquals(403,deleteKitResponse.getStatus());

		//Assert that the kit is not deleted
		impersonateUser(kitRestService,adminUser);
		Response findKitResponse = kitRestService.findKit(savedSystemKit.getId());
		assertEquals(200,findKitResponse.getStatus());

	}

	@Test
	public void test_026_should_deleteKit_isNotAuthorized_whenAdminUserDeleteKitOwnedByOtherUser() throws JsonProcessingException {
		//Create generic user
		//Save project
		//Create kit related to this project
		//Save kit
		//Log as admin user
		//Delete kit
		//Assert that operation is not authorized .

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());
		HDeviceTemplate kitDeviceTemplate = createHDeviceTemplate("deviceLabel");
		addDeviceTemplateToKit(kit,kitDeviceTemplate);

		//Log as user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveKitResponse = kitRestService.save(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		impersonateUser(kitRestService,adminUser);

		//Delete kit
		Response deleteKitResponse = kitRestService.deleteKit(savedKit.getId());
		//Assert that operation is not authorized.
		assertEquals(403,deleteKitResponse.getStatus());

		//Assert that the kit is not deleted
		impersonateUser(kitRestService,registeredUser);
		Response findKitResponse = kitRestService.findKit(savedKit.getId());
		assertEquals(200,findKitResponse.getStatus());

	}

	@Test
	public void test_027_should_deleteKit_isNotAuthorized_whenUserThatShareButNotOwnProjectDeleteKitRelatedToThisProject() throws JsonProcessingException {
		//Log as admin user
		//Save project
		//Creat kit related to this project
		//Save kit
		//Create another user
		//Share project relative to kit saved before with this user.
		//Log such a user
		//Assert that operation is not authorized
		//Assert the coherency of the operation .

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser adminUser = (HUser) authService.login("hadmin", "admin");
		//Create hProject.
		HProject savedProject = saveNewHProject(adminUser, "admin");
		//Create kit
		Kit kit = createKitTemplate("kitLabel", savedProject.getId());
		HDeviceTemplate kitDeviceTemplate = createHDeviceTemplate("deviceLabel");
		addDeviceTemplateToKit(kit, kitDeviceTemplate);


		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService, adminUser);

		//Save kit
		Response saveKitResponse = kitRestService.save(kit);
		assertEquals(200, saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);


		//Create another user
		HUser sharingUser = registerHUserAndActivateAccount();


		//Share project with this user.
		shareHProjectWithUser(savedProject, adminUser, "admin", sharingUser);

		//Log as sharing user
		impersonateUser(kitRestService, sharingUser);


		//Delete kit and assert that operation is not authorized.
		Response deleteKitResponse = kitRestService.deleteKit(savedKit.getId());
		assertEquals(403, deleteKitResponse.getStatus());
		//Assert that the kit is not removed.
		impersonateUser(kitRestService, adminUser);
		Response findKitResponse = kitRestService.findKit(savedKit.getId());
		assertEquals(200, findKitResponse.getStatus());
	}

	@Test
	public void test_028_should_deleteKit_triggerTheDeletionOfKitDeviceTemplatePacketTemplatePacketFieldTemplateRelatedToKit() throws JsonProcessingException {
		//Create generic user
		//Save project
		//Create kit related to this project.
		//Save kit
		//Log as admin user
		//Delete kit
		//Assert that kit , kit's devices template , device's packets template , packet's fields template will be removed after the deletion.

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create kit
		Kit kit = createKitTemplate("kitLabel",savedProject.getId());


		//Add devices, packets, fields to kit.
		HDeviceTemplate kitDevOne = createHDeviceTemplate("kitDevOne");
		HPacketTemplate devOnePacketOne = createHPacketTemplate("devOnePacketOne", "1.0");
		HPacketFieldTemplate devOnePacketOneFieldOne = createHPacketFieldTemplate("devOnePacketOneFieldOne");
		HPacketFieldTemplate devOnePacketOneFieldTwo = createHPacketFieldTemplate("devOnePacketOneFieldTwo");

		addPacketFieldTemplateToPacketTemplate(devOnePacketOne,devOnePacketOneFieldOne);
		addPacketFieldTemplateToPacketTemplate(devOnePacketOne,devOnePacketOneFieldTwo);
		addPacketTemplateToDeviceTemplate(kitDevOne,devOnePacketOne);


		HPacketTemplate devOnePacketTwo = createHPacketTemplate("devOnePacketTwo", "1.0");
		HPacketFieldTemplate devOnePacketTwoFieldOne = createHPacketFieldTemplate("devOnePacketTwoFieldOne");
		addPacketFieldTemplateToPacketTemplate(devOnePacketTwo,devOnePacketTwoFieldOne);
		addPacketTemplateToDeviceTemplate(kitDevOne,devOnePacketTwo);

		addDeviceTemplateToKit(kit,kitDevOne);

		//Clone kit .
		Kit expectedSavedKit = cloneKit(kit);

		//Log as user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save kit
		Response saveKitResponse = kitRestService.save(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);

		List<Long> kitDevicesTemplateId = new LinkedList<>();
		List<Long> kitPacketsTemplateId = new LinkedList<>();
		List<Long> kitPacketsFieldTemplateId = new LinkedList<>();

		assertTrue(savedKit.getDevices() != null && (!savedKit.getDevices().isEmpty()));
		for(HDeviceTemplate deviceTemplate : savedKit.getDevices()){
			kitDevicesTemplateId.add(deviceTemplate.getId());
			assertTrue(deviceTemplate.getPackets() != null && (!deviceTemplate.getPackets().isEmpty()));
			for(HPacketTemplate packetTemplate : deviceTemplate.getPackets()){
				kitPacketsTemplateId.add(packetTemplate.getId());
				assertTrue(packetTemplate.getFields() != null && (!packetTemplate.getFields().isEmpty()));
				for(HPacketFieldTemplate packetFieldTemplate : packetTemplate.getFields()){
					kitPacketsFieldTemplateId.add(packetFieldTemplate.getId());
				}
			}
		}


		//Delete kit.
		Response deleteKitResponse = kitRestService.deleteKit(savedKit.getId());
		assertEquals(200,deleteKitResponse.getStatus());
		//Assert that the kit is removed.
		Response findKitResponse = kitRestService.findKit(savedKit.getId());
		assertEquals(404,findKitResponse.getStatus());

		//Assert that kit's devices template will be removed.
		HDeviceTemplateRepository deviceTemplateRepository = getOsgiService(HDeviceTemplateRepository.class);
		for(Long deviceTemplateId : kitDevicesTemplateId){
			boolean findDevice = true;
			try{
				deviceTemplateRepository.find(deviceTemplateId,null);
			}catch (HyperIoTNoResultException exc){
				findDevice=false;
			}
			assertFalse(findDevice);
		}

		//Assert that kit's packets template will be removed
		HPacketTemplateRepository hPacketTemplateRepository = getOsgiService(HPacketTemplateRepository.class);
		for(Long packetTemplatedId : kitPacketsTemplateId){
			boolean findPacket= true;
			try {
				hPacketTemplateRepository.find(packetTemplatedId, null);
			}catch (HyperIoTNoResultException exc){
				findPacket=false;
			}
			assertFalse(findPacket);
		}

		//Assert that kit's packet fields template will be removed
		HPacketFieldTemplateRepository hPacketFieldTemplateRepository = getOsgiService(HPacketFieldTemplateRepository.class);
		for(Long packetFieldTemplateId : kitPacketsFieldTemplateId){
			boolean findPacketField = true;
			try {
				hPacketFieldTemplateRepository.find(packetFieldTemplateId, null);
			}catch (HyperIoTNoResultException exc){
				findPacketField=false;
			}
			assertFalse(findPacketField);
		}

	}

	@Test
	public void test_029_should_findAllKit_works(){
		//Create generic user
		//Save project
		//Create two kit related to this project.
		//Save kits
		//Assert that findAllKit returned all kit.

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create first kit
		Kit firstKit = createKitTemplate("firstKitLabel",savedProject.getId());
		HDeviceTemplate firstKitDeviceTemplate = createHDeviceTemplate("firstKitDeviceLabel");
		addDeviceTemplateToKit(firstKit,firstKitDeviceTemplate);

		//Create secondKit
		Kit secondKit = createKitTemplate("secondKitLabel",savedProject.getId());
		HDeviceTemplate secondKitDeviceTemplate = createHDeviceTemplate("secondKitDeviceLabel");
		addDeviceTemplateToKit(secondKit,secondKitDeviceTemplate);

		//Save kits.
		//Log as user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save  first kit
		Response saveFirstKitResponse = kitRestService.saveKit(firstKit);
		assertEquals(200,saveFirstKitResponse.getStatus());
		Kit firstSavedKit = (Kit) saveFirstKitResponse.getEntity();
		assertNotNull(firstSavedKit);
		assertNotEquals(0,firstSavedKit.getId());

		//Save second kit
		Response saveSecondKitResponse = kitRestService.saveKit(secondKit);
		assertEquals(200,saveSecondKitResponse.getStatus());
		Kit secondSavedKit = (Kit) saveSecondKitResponse.getEntity();
		assertNotNull(secondSavedKit);
		assertNotEquals(0,secondSavedKit.getId());

		List<Long> expectedKitsIdList = new LinkedList<>();
		expectedKitsIdList.add(firstSavedKit.getId());
		expectedKitsIdList.add(secondSavedKit.getId());


		//Find all kit
		Response findAllKitResponse = kitRestService.findAllKit();
		assertEquals(200,findAllKitResponse.getStatus());
		Collection<Kit> findAllKitList = (Collection<Kit>) findAllKitResponse.getEntity();
		assertNotNull(findAllKitList);
		assertEquals(expectedKitsIdList.size(),findAllKitList.size());
		assertTrue(findAllKitList.stream().map(Kit::getId).collect(Collectors.toList()).containsAll(expectedKitsIdList));
	}

	@Test
	public void test_030_should_findAllKit_returnsSystemKit(){
		//Log as admin user.
		//Add system system kit
		//Create generic user
		//Log as generic user
		//Save project
		//Save kit
		//Assert that findAllKit return system kit created by admin and the kit created by the generic user.

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		Kit systemKit = createKitTemplate("systemKitLabel",KitUtils.SYSTEM_KIT_PROJECT_ID);
		addDeviceTemplateToKit(systemKit,createHDeviceTemplate("systemKitDeviceLabel"));

		//Save system kit
		Response saveSystemKitResponse = kitRestService.saveKit(systemKit);
		assertEquals(200,saveSystemKitResponse.getStatus());
		Kit savedSystemKit = (Kit) saveSystemKitResponse.getEntity();
		assertNotNull(savedSystemKit);
		assertNotEquals(0,savedSystemKit.getId());

		//Create generic user
		HUser genericUser = registerHUserAndActivateAccount();

		//Log as generic user
		impersonateUser(kitRestService,genericUser);

		//Save HProject
		HProject project = saveNewHProject(genericUser,USER_PASSWORD);

		//Create Kit
		Kit kit = createKitTemplate("kitLabel",project.getId());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("kitDeviceLabel"));

		//Save kit
		Response saveKitResponse = kitRestService.saveKit(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);
		assertNotEquals(0,savedKit.getId());

		List<Long> expectedKitsIdList = new LinkedList<>();
		expectedKitsIdList.add(savedSystemKit.getId());
		expectedKitsIdList.add(savedKit.getId());

		//Find all kit.
		Response findAllKitResponse = kitRestService.findAllKit();
		assertEquals(200,findAllKitResponse.getStatus());
		Collection<Kit> findAllKitList = (Collection<Kit>) findAllKitResponse.getEntity();
		assertNotNull(findAllKitList);
		assertEquals(expectedKitsIdList.size(),findAllKitList.size());
		assertTrue(findAllKitList.stream().map(Kit::getId).collect(Collectors.toList()).containsAll(expectedKitsIdList));

	}

	@Test
	public void test_031_should_findAllKit_returnOnlyKitsSuchThatKitProjectIdIsTheIdOfAProjectSharedWithUser(){
		//Log as admin user.
		//Create two project
		//For every project register a kit
		//Save kits
		//Create generic user
		//Share one project with generic user
		//Log as generic user
		//Assert that findAllKit return only the kit that is shared with the user.

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser adminUser = (HUser) authService.login("hadmin", "admin");
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		//Save two projects
		HProject firstProject = saveNewHProject(adminUser,"admin");
		HProject secondProject = saveNewHProject(adminUser,"admin");

		//Create kit and associate that to the first project
		Kit firstKit = createKitTemplate("firstKitLabel",firstProject.getId());
		addDeviceTemplateToKit(firstKit,createHDeviceTemplate("firstKitDeviceLabel"));
		//Create kit and associate that to the second project
		Kit secondKit = createKitTemplate("secondKitLabel",secondProject.getId());
		addDeviceTemplateToKit(secondKit,createHDeviceTemplate("secondKitDeviceLabel"));

		//Save kits
		Response saveFirstKitResponse = kitRestService.save(firstKit);
		assertEquals(200,saveFirstKitResponse.getStatus());
		Kit firstSavedKit = (Kit) saveFirstKitResponse.getEntity();
		assertNotNull(firstSavedKit);
		assertNotEquals(0,firstSavedKit.getId());

		Response saveSecondKitResponse = kitRestService.save(secondKit);
		assertEquals(200,saveSecondKitResponse.getStatus());
		Kit secondSavedKit = (Kit) saveSecondKitResponse.getEntity();
		assertNotNull(secondSavedKit);
		assertNotEquals(0,secondSavedKit.getId());

		//Create generic user
		HUser genericUser = registerHUserAndActivateAccount();
		//Share first project with generic user.
		shareHProjectWithUser(firstProject,adminUser,"admin",genericUser);
		//Log as generic user.
		impersonateUser(kitRestService,genericUser);
		//Find all kit and assert that find all return only the first kit(the kit related to the project shared with user).

		long expectedKitId = firstKit.getId();

		Response findAllKitResponse = kitRestService.findAllKit();
		assertEquals(200,findAllKitResponse.getStatus());
		Collection<Kit> findAllKitList = (Collection<Kit>) findAllKitResponse.getEntity();
		assertNotNull(findAllKitList);
		assertEquals(1,findAllKitList.size());
		assertTrue(findAllKitList.stream().map(Kit::getId).collect(Collectors.toList()).contains(expectedKitId));
	}

	@Test
	public void test_032_should_findAllKitPaginated_works(){
		//Create generic user
		//Save project
		//Create two kit related to this project.
		//Save kits
		//Assert that find all kit paginated return what we expected.

		//Create user.
		HUser registeredUser = registerHUserAndActivateAccount();
		//Create hProject.
		HProject savedProject = saveNewHProject(registeredUser,USER_PASSWORD);
		//Create first kit
		Kit firstKit = createKitTemplate("firstKitLabel",savedProject.getId());
		HDeviceTemplate firstKitDeviceTemplate = createHDeviceTemplate("firstKitDeviceLabel");
		addDeviceTemplateToKit(firstKit,firstKitDeviceTemplate);

		//Create secondKit
		Kit secondKit = createKitTemplate("secondKitLabel",savedProject.getId());
		HDeviceTemplate secondKitDeviceTemplate = createHDeviceTemplate("secondKitDeviceLabel");
		addDeviceTemplateToKit(secondKit,secondKitDeviceTemplate);

		//Save kits.
		//Log as user
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,registeredUser);
		//Save  first kit
		Response saveFirstKitResponse = kitRestService.saveKit(firstKit);
		assertEquals(200,saveFirstKitResponse.getStatus());
		Kit firstSavedKit = (Kit) saveFirstKitResponse.getEntity();
		assertNotNull(firstSavedKit);
		assertNotEquals(0,firstSavedKit.getId());

		//Save second kit
		Response saveSecondKitResponse = kitRestService.saveKit(secondKit);
		assertEquals(200,saveSecondKitResponse.getStatus());
		Kit secondSavedKit = (Kit) saveSecondKitResponse.getEntity();
		assertNotNull(secondSavedKit);
		assertNotEquals(0,secondSavedKit.getId());

		List<Long> expectedKitsIdList = new LinkedList<>();
		expectedKitsIdList.add(firstSavedKit.getId());
		expectedKitsIdList.add(secondSavedKit.getId());

		int delta = 0 ;
		int page = 0;

		//Find all paginated  kit
		Response findAllKitPaginatedResponse = kitRestService.findAllKitPaginated(delta,page);
		//Assert that only the expected kit will be returned.
		assertEquals(200,findAllKitPaginatedResponse.getStatus());
		HyperIoTPaginatedResult<Kit> findAllKitPaginatedList = (HyperIoTPaginatedResult<Kit>) findAllKitPaginatedResponse.getEntity();
		assertNotNull(findAllKitPaginatedList);
		assertEquals(10,findAllKitPaginatedList.getDelta());
		assertEquals(1,findAllKitPaginatedList.getCurrentPage());
		Collection<Kit> kitResultList = findAllKitPaginatedList.getResults();
		assertNotNull(kitResultList);
		assertEquals(expectedKitsIdList.size(),kitResultList.size());
		assertTrue(kitResultList.stream().map(Kit::getId).collect(Collectors.toList()).containsAll(expectedKitsIdList));
	}

	@Test
	public void test_033_should_findAllKitPaginated_returnsSystemKit(){
		//Log as admin user.
		//Add system system kit
		//Create generic user
		//Log as generic user
		//Save project
		//Save kit
		//Assert that find All paginated return system kit created by admin and the kit create by the generic user.

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		Kit systemKit = createKitTemplate("systemKitLabel",KitUtils.SYSTEM_KIT_PROJECT_ID);
		addDeviceTemplateToKit(systemKit,createHDeviceTemplate("systemKitDeviceLabel"));

		//Save system kit
		Response saveSystemKitResponse = kitRestService.saveKit(systemKit);
		assertEquals(200,saveSystemKitResponse.getStatus());
		Kit savedSystemKit = (Kit) saveSystemKitResponse.getEntity();
		assertNotNull(savedSystemKit);
		assertNotEquals(0,savedSystemKit.getId());

		//Create generic user
		HUser genericUser = registerHUserAndActivateAccount();

		//Log as generic user
		impersonateUser(kitRestService,genericUser);

		//Save HProject
		HProject project = saveNewHProject(genericUser,USER_PASSWORD);

		//Create Kit
		Kit kit = createKitTemplate("kitLabel",project.getId());
		addDeviceTemplateToKit(kit,createHDeviceTemplate("kitDeviceLabel"));

		//Save kit
		Response saveKitResponse = kitRestService.saveKit(kit);
		assertEquals(200,saveKitResponse.getStatus());
		Kit savedKit = (Kit) saveKitResponse.getEntity();
		assertNotNull(savedKit);
		assertNotEquals(0,savedKit.getId());

		List<Long> expectedKitsIdList = new LinkedList<>();
		expectedKitsIdList.add(savedSystemKit.getId());
		expectedKitsIdList.add(savedKit.getId());

		int delta = 0 ;
		int page = 0;

		//Find all paginated kit.
		Response findAllKitPaginatedResponse = kitRestService.findAllKitPaginated(delta,page);
		//Assert that only expected kit will be returned.
		assertEquals(200,findAllKitPaginatedResponse.getStatus());
		HyperIoTPaginatedResult<Kit> findAllKitPaginatedList = (HyperIoTPaginatedResult<Kit>) findAllKitPaginatedResponse.getEntity();
		assertNotNull(findAllKitPaginatedList);
		assertEquals(10,findAllKitPaginatedList.getDelta());
		assertEquals(1,findAllKitPaginatedList.getCurrentPage());
		Collection<Kit> kitResultList = findAllKitPaginatedList.getResults();
		assertNotNull(kitResultList);
		assertEquals(expectedKitsIdList.size(),kitResultList.size());
		assertTrue(kitResultList.stream().map(Kit::getId).collect(Collectors.toList()).containsAll(expectedKitsIdList));

	}

	@Test
	public void test_034_should_findAllKitPaginated_returnOnlyKitsSuchThatKitProjectIdIsTheIdOfAProjectSharedWithUser(){
		//Log as admin user.
		//Create two project
		//For every project register a kit
		//Save kits
		//Create generic user
		//Share one project with generic user
		//Log as generic user
		//Assert that find All Kit paginated return only the kit that is shared with the user.

		//Log as admin user
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser adminUser = (HUser) authService.login("hadmin", "admin");
		KitRestApi kitRestService = getOsgiService(KitRestApi.class);
		impersonateUser(kitRestService,adminUser);

		//Save two projects
		HProject firstProject = saveNewHProject(adminUser,"admin");
		HProject secondProject = saveNewHProject(adminUser,"admin");

		//Create kit and associate that to the first project
		Kit firstKit = createKitTemplate("firstKitLabel",firstProject.getId());
		addDeviceTemplateToKit(firstKit,createHDeviceTemplate("firstKitDeviceLabel"));
		//Create kit and associate that to the second project
		Kit secondKit = createKitTemplate("secondKitLabel",secondProject.getId());
		addDeviceTemplateToKit(secondKit,createHDeviceTemplate("secondKitDeviceLabel"));

		//Save kits
		Response saveFirstKitResponse = kitRestService.save(firstKit);
		assertEquals(200,saveFirstKitResponse.getStatus());
		Kit firstSavedKit = (Kit) saveFirstKitResponse.getEntity();
		assertNotNull(firstSavedKit);
		assertNotEquals(0,firstSavedKit.getId());

		Response saveSecondKitResponse = kitRestService.save(secondKit);
		assertEquals(200,saveSecondKitResponse.getStatus());
		Kit secondSavedKit = (Kit) saveSecondKitResponse.getEntity();
		assertNotNull(secondSavedKit);
		assertNotEquals(0,secondSavedKit.getId());

		//Create generic user
		HUser genericUser = registerHUserAndActivateAccount();
		//Share first project with generic user.
		shareHProjectWithUser(firstProject,adminUser,"admin",genericUser);
		//Log as generic user.
		impersonateUser(kitRestService,genericUser);
		//Find all paginated kit

		long expectedKitId = firstKit.getId();

		int delta = 0 ;
		int page = 0;

		//Find all paginated  kit
		Response findAllKitPaginatedResponse = kitRestService.findAllKitPaginated(delta,page);
		//Assert that find all return only the first kit(the kit related to the project shared with user).
		assertEquals(200,findAllKitPaginatedResponse.getStatus());
		HyperIoTPaginatedResult<Kit> findAllKitPaginatedList = (HyperIoTPaginatedResult<Kit>) findAllKitPaginatedResponse.getEntity();
		assertNotNull(findAllKitPaginatedList);
		assertEquals(10,findAllKitPaginatedList.getDelta());
		assertEquals(1,findAllKitPaginatedList.getCurrentPage());
		Collection<Kit> kitResultList = findAllKitPaginatedList.getResults();
		assertNotNull(kitResultList);
		assertEquals(1,kitResultList.size());
		assertTrue(kitResultList.stream().map(Kit::getId).collect(Collectors.toList()).contains(expectedKitId));
	}


	@After
	public void afterTest(){
		AreaDeviceRepository areaDeviceRepository = getOsgiService(AreaDeviceRepository.class);
		AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
		HDeviceSystemApi hDeviceSystemApi = getOsgiService(HDeviceSystemApi.class);
		HProjectSystemApi hProjectSystemApi = getOsgiService(HProjectSystemApi.class);
		KitSystemApi kitSystemApi = getOsgiService(KitSystemApi.class);
		SharedEntitySystemApi sharedEntitySystemApi = getOsgiService(SharedEntitySystemApi.class);
		PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
		RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
		HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);

		HyperIoTTestUtils.truncateTables(areaDeviceRepository, null);
		HyperIoTTestUtils.truncateTables(areaSystemApi, null);
		HyperIoTTestUtils.truncateTables(hDeviceSystemApi, null);
		HyperIoTTestUtils.truncateTables(hProjectSystemApi, null);
		HyperIoTTestUtils.truncateTables(kitSystemApi,null);
		HyperIoTTestUtils.truncateTables(sharedEntitySystemApi, null);

		HyperIoTPermissionTestUtil.dropPermissions(roleSystemApi,permissionSystemApi);
		HyperIoTHUserTestUtils.truncateRoles(roleSystemApi);
		HyperIoTHUserTestUtils.truncateHUsers(hUserSystemApi);
	}

	//Utility methods for test.

	private HUser registerHUserAndActivateAccount() {
		HUser registeredUser = registerNewHUserForTest();
		activateHUserAccount(registeredUser.getId());
		return registeredUser;
	}

	private HUser registerNewHUserForTest() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser userToRegister = createHUserTemplate();
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.register(userToRegister);
		Assert.assertEquals(200, restResponse.getStatus());
		HUser registeredUser = (HUser) restResponse.getEntity();
		Assert.assertNotNull(registeredUser);
		Assert.assertNotEquals(0, (registeredUser.getId()));
		Assert.assertFalse(registeredUser.isActive());
		return registeredUser;
	}

	private void activateHUserAccount(long userId) {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HUserRepository hUserRepository = getOsgiService(HUserRepository.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser userToActivate = hUserRepository.find(userId, null);
		Response restResponse = huserRestService.activate(userToActivate.getEmail(), userToActivate.getActivateCode());
		assertEquals(200, restResponse.getStatus());
	}

	private HUser createHUserTemplate(){
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword(USER_PASSWORD);
		huser.setPasswordConfirm(USER_PASSWORD);
		return huser;
	}

	private HProject saveNewHProject(HUser user, String userPassword) {
		HProjectRestApi hProjectRestService = getOsgiService(HProjectRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser userLogged = (HUser) authService.login(user.getUsername(), userPassword);
		this.impersonateUser(hProjectRestService, userLogged);
		HProject hproject = new HProject();
		hproject.setName("Project " + java.util.UUID.randomUUID());
		hproject.setDescription("Project of user: " + userLogged.getUsername());
		hproject.setUser((HUser) userLogged);
		Response restResponse = hProjectRestService.saveHProject(hproject);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
		Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
		Assert.assertEquals("Project of user: " + userLogged.getUsername(),
				((HProject) restResponse.getEntity()).getDescription());
		Assert.assertEquals(userLogged.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
		return hproject;
	}

	private void shareHProjectWithUser(HProject project, HUser projectOwner, String projectOwnerPassword, HUser sharingUser){
		SharedEntityRestApi sharedEntityRestService = getOsgiService(SharedEntityRestApi.class);
		//Log as project owener.
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser userLogged = (HUser) authService.login(projectOwner.getUsername(), projectOwnerPassword);
		this.impersonateUser(sharedEntityRestService,projectOwner);
		//Share project with sharing user.
		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(project.getId());
		sharedEntity.setEntityResourceName(HPROJECT_RESOURCE_NAME);
		sharedEntity.setUserId(sharingUser.getId());
		Response restResponse = sharedEntityRestService.saveSharedEntity(sharedEntity);
		assertEquals(200, restResponse.getStatus());
		assertEquals(project.getId(), ((SharedEntity) restResponse.getEntity()).getEntityId());
		assertEquals(sharingUser.getId(), ((SharedEntity) restResponse.getEntity()).getUserId());
		assertEquals(HPROJECT_RESOURCE_NAME, ((SharedEntity) restResponse.getEntity()).getEntityResourceName());
	}

	private Kit createKitTemplate(String kitLabelPrefix, long projectId){
		Kit kit = new Kit();
		String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
		kit.setLabel(kitLabelPrefix.concat(randomUUID));
		kit.setKitVersion(randomUUID);
		kit.setProjectId(projectId);
		kit.setDevices(new LinkedList<>());
		return kit;
	}

	private HDeviceTemplate createHDeviceTemplate(String hDeviceLabelPrefix){
		HDeviceTemplate hDeviceTemplate = new HDeviceTemplate();
		String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
		hDeviceTemplate.setDeviceLabel(hDeviceLabelPrefix.concat(randomUUID));
		hDeviceTemplate.setBrand("brand".concat(randomUUID));
		hDeviceTemplate.setModel("model".concat(randomUUID));
		hDeviceTemplate.setFirmwareVersion("firmwareversion".concat(randomUUID));
		hDeviceTemplate.setSoftwareVersion("softwareversion".concat(randomUUID));
		hDeviceTemplate.setDescription("description".concat(randomUUID));
		hDeviceTemplate.setPackets(new LinkedList<>());
		return hDeviceTemplate;
	}

	private HPacketTemplate createHPacketTemplate(String packetNamePrefix, String versionPrefix){
		HPacketTemplate packetTemplate = new HPacketTemplate();
		String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
		packetTemplate.setName(packetNamePrefix.concat(randomUUID));
		packetTemplate.setType(HPacketType.INPUT);
		packetTemplate.setFormat(HPacketFormat.JSON);
		packetTemplate.setSerialization(HPacketSerialization.NONE);
		packetTemplate.setVersion(versionPrefix.concat(randomUUID));
		packetTemplate.setTimestampField("timestamp");
		packetTemplate.setTimestampFormat("dd/MM/yyyy hh.mmZ");
		packetTemplate.setUnixTimestamp(true);
		packetTemplate.setUnixTimestamp(true);
		packetTemplate.setUnixTimestampFormatSeconds(false);
		packetTemplate.setTrafficPlan(HPacketTrafficPlan.LOW);
		packetTemplate.setFields(new LinkedList<>());
		return packetTemplate;
	}

	private HPacketFieldTemplate createHPacketFieldTemplate(String packetFieldNamePrefix){
		HPacketFieldTemplate packetFieldTemplate = new HPacketFieldTemplate();
		String randomUUID = UUID.randomUUID().toString().replaceAll("-", "");
		packetFieldTemplate.setName(packetFieldNamePrefix.concat(randomUUID));
		packetFieldTemplate.setType(HPacketFieldType.INTEGER);
		packetFieldTemplate.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
		packetFieldTemplate.setUnit("deg");
		return packetFieldTemplate;
	}

	private void addDeviceTemplateToKit(Kit kit, HDeviceTemplate device){
		kit.getDevices().add(device);
	}

	private void addPacketTemplateToDeviceTemplate(HDeviceTemplate deviceTemplate, HPacketTemplate packetTemplate){
		deviceTemplate.getPackets().add(packetTemplate);
	}

	private void addPacketFieldTemplateToPacketTemplate(HPacketTemplate packetTemplate , HPacketFieldTemplate hPacketFieldTemplate){
		packetTemplate.getFields().add(hPacketFieldTemplate);
	}

	private Kit cloneKit(Kit kit ) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String jsonKit = mapper.writeValueAsString(kit);
		return mapper.readValue(jsonKit,Kit.class);
	}

	private String getHyperIoTValidationExceptionClassName(){
		return HyperIoTValidationException.class.getName();
	}

	private String getHyperIoTDuplicateEntityExceptionClassName(){return HyperIoTDuplicateEntityException.class.getName();}

	private String getMaliciousValue(){
		return "<script>console.log('hello')</script>";
	}

	public static final String USER_PASSWORD= "passwordPass&01";

	public static final String HPROJECT_RESOURCE_NAME=HProject.class.getName();

}
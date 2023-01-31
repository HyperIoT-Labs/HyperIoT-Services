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

package it.acsoftware.hyperiot.widget.test;

import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import it.acsoftware.hyperiot.widget.model.Widget;
import it.acsoftware.hyperiot.widget.model.WidgetCategory;
import it.acsoftware.hyperiot.widget.model.WidgetRating;
import it.acsoftware.hyperiot.widget.service.rest.WidgetRestApi;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.*;

import static it.acsoftware.hyperiot.widget.test.HyperIoTWidgetConfiguration.*;

/**
 *
 * @author Vincenzo Longo Interface component for Widget System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTWidgetWithDefaultPermissionRestTest extends KarafTestSupport {

	//force global configuration
	public Option[] config() {
		return null;
	}

	public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
		restApi.impersonate(user);
	}

	@Before
	public void initPlatformContainers() {
		HyperIoTServicesTestUtil.initPlatformContainers();
	}

	@Test
	public void test00_hyperIoTFrameworkShouldBeInstalled() {
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
		//HyperIoTServices
		assertContains("HyperIoTWidget-features ", features);
		String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
		assertContains("hyperiot", datasource);
	}


	@Test
	public void test01_widgetModuleShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// the following call checkModuleWorking checks if Widget module working
		// correctly
		HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
		Assert.assertNotEquals(0, huser.getId());
		Assert.assertTrue(huser.isActive());
		this.impersonateUser(widgetRestApi, huser);
		Response restResponse = widgetRestApi.checkModuleWorking();
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals("Widget Module works!", restResponse.getEntity());
	}

	// Widget action save: 1 not assigned in default permission
    @Test
    public void test02_saveWidgetWithDefaultPermissionShouldFail() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, tries to save Widget with the following call saveWidget.
        // huser to save a new widget needs the "save widget" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);

        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.saveWidget(widget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Widget action update: 2 not assigned in default permission
    @Test
    public void test03_updateWidgetWithDefaultPermissionShouldFail() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, tries to update Widget with the following call updateWidget
        // huser to update a new widget needs the "update widget" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

        widget.setDescription("Edit description failed...");

        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.updateWidget(widget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Widget action remove: 4 not assigned in default permission
    @Test
    public void test04_deleteWidgetWithDefaultPermissionShouldFail() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, tries to delete Widget with the following call deleteWidget
        // huser to delete a new widget needs the "remove widget" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.deleteWidget(widget.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Widget action find: 8
    @Test
    public void test05_findWidgetWithDefaultPermissionShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, find Widget with the following call findWidget
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.findWidget(widget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(widget.getId(), ((Widget) restResponse.getEntity()).getId());
    }


    // Widget action find-all: 16
    @Test
    public void test06_findAllWidgetWithDefaultPermissionShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, find all Widget with the following call findAllWidget
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.findAllWidget();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Widget> listWidgets = restResponse.readEntity(new GenericType<List<Widget>>() {
        });
        Assert.assertFalse(listWidgets.isEmpty());
        Assert.assertEquals(1, listWidgets.size());
        boolean widgetFound = false;
        for (Widget w : listWidgets) {
            if (widget.getId() == w.getId()) {
                Assert.assertEquals(widget.getId(), w.getId());
                widgetFound = true;
            }
        }
        Assert.assertTrue(widgetFound);
    }


    // Widget action find-all: 16
    @Test
    public void test07_findAllWidgetPaginatedWithDefaultPermissionShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // In this following call findAllWidgetPaginated, huser, with default permission,
        // find all Widget with pagination
        // response status code '200'
		int delta = 5;
		int page = 1;
		HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<Widget> widgets = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
			Widget widget = createWidget(false);
			Assert.assertNotEquals(0, widget.getId());
			Assert.assertTrue(widget.isRealTime());
            widgets.add(widget);
        }
        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
        HyperIoTPaginableResult<Widget> listWidgets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
                });
		Assert.assertFalse(listWidgets.getResults().isEmpty());
		Assert.assertEquals(delta, listWidgets.getResults().size());
		Assert.assertEquals(delta, listWidgets.getDelta());
		Assert.assertEquals(page, listWidgets.getCurrentPage());
		Assert.assertEquals(page, listWidgets.getNextPage());
		// delta is 5, page is 1: 5 entities stored in database
		Assert.assertEquals(1, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());
    }


    // nothing action
    @Test
    public void test08_rateWidgetShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser rate Widget with the following call rateWidget
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		WidgetRating widgetRating = new WidgetRating();
		widgetRating.setWidget(widget);
		widgetRating.setRating(5);

		// userId inside WidgetRating table is set by loggedUsername (huser)
		this.impersonateUser(widgetRestApi, huser);
		Response restResponse = widgetRestApi.rateWidget(widgetRating.getRating(), widget);
		Assert.assertEquals(200, restResponse.getStatus());

        //checks if widgetId inside WidgetRating table is equals to widget.getId()
        String sqlWidgetId = "select wr.widget_id from widgetrating wr";
        String resultWidgetId = executeCommand("jdbc:query hyperiot " + sqlWidgetId);
        String[] wrId = resultWidgetId.split("\\n");

        this.impersonateUser(widgetRestApi, huser);
        Response restResponseFindWidget = widgetRestApi.findWidget(Long.parseLong(wrId[2]));
        Assert.assertEquals(200, restResponseFindWidget.getStatus());
        Assert.assertEquals(widget.getId(), ((Widget) restResponseFindWidget.getEntity()).getId());

        //checks if avgRating has been updated inside Widget table
        String sqlRating = "select wr.rating from widgetrating wr";
        String resultRating = executeCommand("jdbc:query hyperiot " + sqlRating);
        String[] wrRating = resultRating.split("\\n");
        Assert.assertEquals(((Float)Float.parseFloat(wrRating[2])), ((Widget) restResponseFindWidget.getEntity()).getAvgRating());

        //checks if huserId inside widgetRating table is equals to huser.getId()
        String sqlHuserId = "select wr.user_id from widgetrating wr";
        String resultHuserId = executeCommand("jdbc:query hyperiot " + sqlHuserId);
        String[] wrHuserId = resultHuserId.split("\\n");
        Assert.assertEquals(Integer.parseInt(wrHuserId[2]), huser.getId());
    }


	/*
	 *
	 *
	 * UTILITY METHODS
	 *
	 *
	 */


	private Widget createWidget(boolean isOffline) {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		if (isOffline) {
			widget.setRealTime(false);
			widget.setOffline(true);
		}

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());

		Assert.assertNotEquals(0, ((Widget) restResponse.getEntity()).getId());
		Assert.assertEquals(widget.getName(),
				((Widget) restResponse.getEntity()).getName());
		Assert.assertEquals(widgetDescription,
				((Widget) restResponse.getEntity()).getDescription());
		Assert.assertEquals(0,
				((Widget) restResponse.getEntity()).getWidgetCategory().getId());
		Assert.assertEquals("all",
				((Widget) restResponse.getEntity()).getWidgetCategory().getName());
		Assert.assertEquals("icon-hyt_layout",
				((Widget) restResponse.getEntity()).getWidgetCategory().getFontIcon());
		Assert.assertEquals("ALL",
				((Widget) restResponse.getEntity()).getWidgetCategory().name());
		Assert.assertEquals(0,
				((Widget) restResponse.getEntity()).getWidgetCategory().ordinal());

		Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getBaseConfig());
		Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((Widget) restResponse.getEntity()).getCols());
		Assert.assertEquals(3, ((Widget) restResponse.getEntity()).getRows());
		Assert.assertEquals(widgetImageData, ((Widget) restResponse.getEntity()).getImage());
		Assert.assertEquals(widgetImageDataPreview, ((Widget) restResponse.getEntity()).getPreView());
		if (isOffline) {
			Assert.assertFalse(((Widget) restResponse.getEntity()).isRealTime());
			Assert.assertTrue(((Widget) restResponse.getEntity()).isOffline());
		} else {
			Assert.assertTrue(((Widget) restResponse.getEntity()).isRealTime());
			Assert.assertFalse(((Widget) restResponse.getEntity()).isOffline());
		}
		return widget;
	}


	private HUser huserWithDefaultPermissionInHyperIoTFramework(boolean isActive) {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		String username = "TestUser";
		List<Object> roles = new ArrayList<>();
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername(username + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(huser.getUsername() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		huser.setAdmin(false);
		huser.setActive(false);
		Assert.assertNull(huser.getActivateCode());
		Response restResponse = hUserRestApi.register(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
		Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
		Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
		Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
		Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
		Assert.assertFalse(huser.isAdmin());
		Assert.assertFalse(huser.isActive());
		Assert.assertTrue(roles.isEmpty());
		if (isActive) {
			//Activate huser and checks if default role has been assigned
			Role role = null;
			Assert.assertFalse(huser.isActive());
			String activationCode = huser.getActivateCode();
			Assert.assertNotNull(activationCode);
			Response restResponseActivateUser = hUserRestApi.activate(huser.getEmail(), activationCode);
			Assert.assertEquals(200, restResponseActivateUser.getStatus());
			huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
			roles = Arrays.asList(huser.getRoles().toArray());
			Assert.assertFalse(roles.isEmpty());
			Assert.assertTrue(huser.isActive());

			// checks: default role has been assigned to new huser
			Assert.assertEquals(1, huser.getRoles().size());
			Assert.assertEquals(roles.size(), huser.getRoles().size());
			Assert.assertFalse(roles.isEmpty());
			for (int i = 0; i < roles.size(); i++){
				role = ((Role) roles.get(i));
			}
			Assert.assertNotNull(role);
			Assert.assertEquals("RegisteredUser", role.getName());
			Assert.assertEquals("Role associated with the registered user",
					role.getDescription());
			PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
			Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
			Assert.assertFalse(listPermissions.isEmpty());
			boolean resourceNameFound = false;
			for (Permission permission : listPermissions) {
				// it.acsoftware.hyperiot.widget.model.Widget (24)
				// find                     8
				// find_all                 16
				if (permission.getEntityResourceName().contains(permissionWidget)) {
					Assert.assertNotEquals(0, permission.getId());
					Assert.assertEquals(permissionWidget, permission.getEntityResourceName());
					Assert.assertEquals(permissionWidget + nameRegisteredPermission, permission.getName());
					Assert.assertEquals(24, permission.getActionIds());
					Assert.assertEquals(role.getName(), permission.getRole().getName());
					resourceNameFound = true;
				}
			}
			Assert.assertTrue(resourceNameFound);
		}
		return huser;
	}


	// Widget isn't Owned Resource
	@After
	public void afterTest() {
		// Remove all widgets in every tests
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidget();
		List<Widget> listWidgets = restResponse.readEntity(new GenericType<List<Widget>>() {
		});
		if (!listWidgets.isEmpty()) {
			Assert.assertFalse(listWidgets.isEmpty());
			for (Widget widget : listWidgets) {
				this.impersonateUser(widgetRestApi, adminUser);
				Response restResponse1 = widgetRestApi.deleteWidget(widget.getId());
				Assert.assertEquals(200, restResponse1.getStatus());
				Assert.assertNull(restResponse1.getEntity());
			}
		}
		// Remove all husers created in every test
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponseUsers = huserRestService.findAllHUser();
		List<HUser> listHUsers = restResponseUsers.readEntity(new GenericType<List<HUser>>() {
		});
		if (!listHUsers.isEmpty()) {
			Assert.assertFalse(listHUsers.isEmpty());
			for (HUser huser : listHUsers) {
				if (!huser.isAdmin()) {
					this.impersonateUser(huserRestService, adminUser);
					Response restResponse1 = huserRestService.deleteHUser(huser.getId());
					Assert.assertEquals(200, restResponse1.getStatus());
					Assert.assertNull(restResponse1.getEntity());
				}
			}
		}
//		String sqlWidget = "select w.id, w.widgetCategory from widget w";
//        String resultWidget = executeCommand("jdbc:query hyperiot " + sqlWidget);
//        System.out.println(resultWidget);
//
//        String sqlWidgetRating = "select wr.id, wr.widget_id, wr.user_id, wr.rating from widgetrating wr";
//        String resultWidgetRating = executeCommand("jdbc:query hyperiot " + sqlWidgetRating);
//        System.out.println(resultWidgetRating);
//
//        String sqlHUser = "select h.id, h.admin, h.username from huser h";
//        String resultHUser = executeCommand("jdbc:query hyperiot " + sqlHUser);
//        System.out.println(resultHUser);
	}

}

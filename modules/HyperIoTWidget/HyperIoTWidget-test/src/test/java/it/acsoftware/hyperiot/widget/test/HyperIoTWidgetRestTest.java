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
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
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

import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.util.*;

import static it.acsoftware.hyperiot.widget.test.HyperIoTWidgetConfiguration.*;

/**
 *
 * @author Aristide Cittadino Interface component for Widget System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTWidgetRestTest extends KarafTestSupport {

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
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.checkModuleWorking();
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals("Widget Module works!", restResponse.getEntity());
	}


	@Test
	public void test02_saveWidgetShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin save Widget with the following call saveWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

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

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());

		Assert.assertNotEquals(0, ((Widget) restResponse.getEntity()).getId());
		Assert.assertEquals(widget.getName(), ((Widget) restResponse.getEntity()).getName());
		Assert.assertEquals(widgetDescription, ((Widget) restResponse.getEntity()).getDescription());
		Assert.assertEquals(0, ((Widget) restResponse.getEntity()).getWidgetCategory().getId());
		Assert.assertEquals("all", ((Widget) restResponse.getEntity()).getWidgetCategory().getName());
		Assert.assertEquals("icon-hyt_layout", ((Widget) restResponse.getEntity()).getWidgetCategory().getFontIcon());
		Assert.assertEquals("ALL", ((Widget) restResponse.getEntity()).getWidgetCategory().name());
		Assert.assertEquals(0, ((Widget) restResponse.getEntity()).getWidgetCategory().ordinal());

		Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getBaseConfig());
		Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((Widget) restResponse.getEntity()).getCols());
		Assert.assertEquals(3, ((Widget) restResponse.getEntity()).getRows());
		Assert.assertEquals(widgetImageData, ((Widget) restResponse.getEntity()).getImage());
		Assert.assertEquals(widgetImageDataPreview, ((Widget) restResponse.getEntity()).getPreView());
		Assert.assertFalse(((Widget) restResponse.getEntity()).isOffline());
		Assert.assertTrue(((Widget) restResponse.getEntity()).isRealTime());
	}


	@Test
	public void test03_saveWidgetShouldFailIfNotLogged() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// the following call tries to save Widget, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException

		Widget widget = new Widget();
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setType("image-data");
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");

		this.impersonateUser(widgetRestApi, null);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test04_updateWidgetShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin update Widget with the following call updateWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		Date date = new Date();
		widget.setDescription("description edited in date: " + date);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponseUpdateWidget = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(200, restResponseUpdateWidget.getStatus());
		Assert.assertEquals("description edited in date: " + date,
				((Widget) restResponseUpdateWidget.getEntity()).getDescription());
		Assert.assertEquals(widget.getEntityVersion() + 1,
				((Widget) restResponseUpdateWidget.getEntity()).getEntityVersion());
	}


	@Test
	public void test05_updateWidgetShouldFailIfNotLogged() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// the following call tries to update Widget, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		Date date = new Date();
		widget.setDescription("description edited in date: " + date);

		this.impersonateUser(widgetRestApi, null);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test06_findWidgetShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin find Widget with the following call findWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findWidget(widget.getId());
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(widget.getId(), ((Widget) restResponse.getEntity()).getId());
	}


	@Test
	public void test07_findWidgetShouldFailIfNotLogged() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// the following call tries to find Widget, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		this.impersonateUser(widgetRestApi, null);
		Response restResponse = widgetRestApi.findWidget(widget.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test08_findWidgetShouldFailIfEntityNotFound() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to find Widget with the following call findWidget,
		// but entity not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findWidget(0);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test09_findAllWidgetShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin find all Widget with the following call findAllWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidget();
		Assert.assertEquals(200, restResponse.getStatus());
		List<Widget> listWidgets = restResponse.readEntity(new GenericType<List<Widget>>() {
		});
		Assert.assertFalse(listWidgets.isEmpty());
		Assert.assertEquals(1, listWidgets.size());
		boolean widgetFound = false;
		for (Widget widgets : listWidgets) {
			if (widget.getId() == widgets.getId()) {
				widgetFound = true;
			}
		}
		Assert.assertTrue(widgetFound);
	}


	@Test
	public void test10_findAllWidgetShouldFailIfNotLogged() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// the following call tries to find all Widget, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		this.impersonateUser(widgetRestApi, null);
		Response restResponse = widgetRestApi.findAllWidget();
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test11_deleteWidgetShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin delete Widget with the following call deleteWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.deleteWidget(widget.getId());
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNull(restResponse.getEntity());
	}


	@Test
	public void test12_deleteWidgetShouldFailIfNotLogged() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// the following call tries to delete all Widget, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		this.impersonateUser(widgetRestApi, null);
		Response restResponse = widgetRestApi.deleteWidget(widget.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test13_deleteWidgetShouldFailIfEntityNotFound() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to delete Widget with the following call deleteWidget,
		// but entity not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.deleteWidget(0);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test14_saveWidgetShouldFailIfNameIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but name is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName(null);
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
	}


	@Test
	public void test15_saveWidgetShouldFailIfNameIsEmpty() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but name is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("");
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test16_saveWidgetShouldFailIfNameIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but name is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("javascript:");
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test17_saveWidgetShouldFailIfNameIsOver500Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but name is over 500 chars
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName(testMaxLength(maxLengthName));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
		Assert.assertEquals(maxLengthName, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
	}


	@Test
	public void test18_saveWidgetShouldFailIfDescriptionIsOver3000Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but description is over 3000 chars
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(testMaxLength(maxLengthDescription));
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
		Assert.assertEquals(maxLengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
	}

	@Test
	public void test19_saveWidgetShouldFailIfDescriptionIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but description is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription("vbscript:");
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test20_saveWidgetShouldFailIfWidgetCategoryIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but WidgetCategory is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(null);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-widgetcategory", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}


	@Test
	public void test21_saveWidgetShouldFailIfBaseConfigIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but BaseConfig is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig(null);
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}


	@Test
	public void test22_saveWidgetShouldFailIfBaseConfigIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but BaseConfig is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("vbscript:");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getBaseConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test23_saveWidgetShouldFailIfTypeIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but type is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType(null);
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
	}


	@Test
	public void test24_saveWidgetShouldFailIfTypeIsEmpty() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but type is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getType(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test25_saveWidgetShouldFailIfTypeIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but type is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("image-data" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("javascript:");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getType(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test26_saveWidgetCheckDefaultValue() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin save Widget with the following call saveWidget,
		// this call checks:
		// - offline default value is false
		// - realtime default value is true
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

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

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertTrue(((Widget) restResponse.getEntity()).isRealTime());
		Assert.assertFalse(((Widget) restResponse.getEntity()).isOffline());
	}


	@Test
	public void test27_saveWidgetShouldFailIfEntityIsDuplicated() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but widget is duplicated
		// response status code '422' HyperIoTDuplicateEntityException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		Widget widgetDuplicated = new Widget();
		widgetDuplicated.setName(widget.getName());
		widgetDuplicated.setDescription(widgetDescription);
		widgetDuplicated.setWidgetCategory(WidgetCategory.ACTION);
		widgetDuplicated.setBaseConfig("image-data");
		widgetDuplicated.setType("image-data");
		widgetDuplicated.setCols(2);
		widgetDuplicated.setRows(3);
		widgetDuplicated.setImage(widgetImageData);
		widgetDuplicated.setPreView(widgetImageDataPreview);
		widgetDuplicated.setOffline(true);
		widgetDuplicated.setRealTime(false);
		widgetDuplicated.setAvgRating(100.27f);

		Assert.assertEquals(widget.getName(), widgetDuplicated.getName());

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widgetDuplicated);
		Assert.assertEquals(409, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test28_updateWidgetShouldFailIfEntityNotFound() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but entity not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		// Widget isn't stored in database
		Widget widget = new Widget();
		Assert.assertEquals(0, widget.getId());

		widget.setDescription("widget not found...");

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test29_updateWidgetShouldFailIfNameIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but name is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setName(null);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
	}


	@Test
	public void test30_updateWidgetShouldFailIfNameIsEmpty() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but name is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setName("");

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test31_updateWidgetShouldFailIfNameIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but name is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setName("javascript:");

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test32_updateWidgetShouldFailIfNameIsOver500Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but name is over 500 chars
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setName(testMaxLength(maxLengthName));

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-name", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getName(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
		Assert.assertEquals(maxLengthName, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
	}


	@Test
	public void test33_updateWidgetShouldFailIfDescriptionIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but description is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setDescription("javascript:");

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test34_updateWidgetShouldFailIfDescriptionIsOver3000Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but description is over 3000 chars
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setDescription(testMaxLength(maxLengthDescription));

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-description", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getDescription(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
		Assert.assertEquals(maxLengthDescription, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
	}


	@Test
	public void test35_updateWidgetShouldFailIfWidgetCategoryIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but WidgetCategory is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setWidgetCategory(null);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-widgetcategory", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}


	@Test
	public void test36_updateWidgetShouldFailIfBaseConfigIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but BaseConfig is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setBaseConfig(null);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}


	@Test
	public void test37_updateWidgetShouldFailIfBaseConfigIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but BaseConfig is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setBaseConfig("vbscript:");

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-baseconfig", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getBaseConfig(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test38_updateWidgetShouldFailIfTypeIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but type is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setType(null);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(1).getField());
	}


	@Test
	public void test39_updateWidgetShouldFailIfTypeIsEmpty() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but type is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setType("");

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getType(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test40_updateWidgetShouldFailIfTypeIsMaliciousCode() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but type is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());
		widget.setType("javascript:");

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-type", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(widget.getType(), ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test41_updateWidgetShouldFailIfEntityIsDuplicated() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but widget is duplicated
		// response status code '422' HyperIoTDuplicateEntityException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		Widget widgetDuplicated = createWidget(false);
		Assert.assertNotEquals(0, widgetDuplicated.getId());
		Assert.assertTrue(widgetDuplicated.isRealTime());
		Assert.assertNotEquals(widget.getId(), widgetDuplicated.getId());

		widgetDuplicated.setName(widget.getName());

		Assert.assertEquals(widget.getName(), widgetDuplicated.getName());

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widgetDuplicated);
		Assert.assertEquals(409, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("name", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test42_findAllWidgetPaginatedShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// In this following call findAllWidgetPaginated, hadmin find all Widget with pagination
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		int delta = 6;
		int page = 2;
		List<Widget> widgets = new ArrayList<>();
		for (int i = 0; i < defaultDelta; i++) {
			Widget widget = createWidget(false);
			Assert.assertNotEquals(0, widget.getId());
			Assert.assertTrue(widget.isRealTime());
			widgets.add(widget);
		}
		Assert.assertEquals(defaultDelta, widgets.size());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
		HyperIoTPaginableResult<Widget> listWidgets = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgets.getResults().isEmpty());
		Assert.assertEquals(defaultDelta - delta, listWidgets.getResults().size());
		Assert.assertEquals(delta, listWidgets.getDelta());
		Assert.assertEquals(page, listWidgets.getCurrentPage());
		Assert.assertEquals(defaultPage, listWidgets.getNextPage());
		// delta is 6, page is 2: 10 entities stored in database
		Assert.assertEquals(2, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());

		//checks with page = 1
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponsePage1 = widgetRestApi.findAllWidgetPaginated(delta, 1);
		HyperIoTPaginableResult<Widget> listWidgetsPage1 = restResponsePage1
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgetsPage1.getResults().isEmpty());
		Assert.assertEquals(delta, listWidgetsPage1.getResults().size());
		Assert.assertEquals(delta, listWidgetsPage1.getDelta());
		Assert.assertEquals(defaultPage, listWidgetsPage1.getCurrentPage());
		Assert.assertEquals(page, listWidgetsPage1.getNextPage());
		// delta is 6, page is 1: 10 entities stored in database
		Assert.assertEquals(2, listWidgetsPage1.getNumPages());
		Assert.assertEquals(200, restResponsePage1.getStatus());
	}


	@Test
	public void test43_findAllWidgetPaginatedShouldWorkIfDeltaAndPageAreNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// In this following call findAllWidgetPaginated, hadmin find all Widget with pagination
		// if delta and page are null
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Integer delta = null;
		Integer page = null;
		List<Widget> widgets = new ArrayList<>();
		for (int i = 0; i < defaultDelta; i++) {
			Widget widget = createWidget(false);
			Assert.assertNotEquals(0, widget.getId());
			Assert.assertTrue(widget.isRealTime());
			widgets.add(widget);
		}
		Assert.assertEquals(defaultDelta, widgets.size());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
		HyperIoTPaginableResult<Widget> listWidgets = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgets.getResults().isEmpty());
		Assert.assertEquals(defaultDelta, listWidgets.getResults().size());
		Assert.assertEquals(defaultDelta, listWidgets.getDelta());
		Assert.assertEquals(defaultPage, listWidgets.getCurrentPage());
		Assert.assertEquals(defaultPage, listWidgets.getNextPage());
		// default delta is 10, default page is 1: 10 entities stored in database
		Assert.assertEquals(1, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test44_findAllWidgetPaginatedShouldWorkIfDeltaIsLowerThanZero() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// In this following call findAllWidgetPaginated, hadmin find all Widget with pagination
		// if delta is lower than zero
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		int delta = -1;
		int page = 1;
		List<Widget> widgets = new ArrayList<>();
		int numbEntities = 6;
		for (int i = 0; i < numbEntities; i++) {
			Widget widget = createWidget(false);
			Assert.assertNotEquals(0, widget.getId());
			Assert.assertTrue(widget.isRealTime());
			widgets.add(widget);
		}
		Assert.assertEquals(numbEntities, widgets.size());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
		HyperIoTPaginableResult<Widget> listWidgets = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgets.getResults().isEmpty());
		Assert.assertEquals(numbEntities, listWidgets.getResults().size());
		Assert.assertEquals(defaultDelta, listWidgets.getDelta());
		Assert.assertEquals(page, listWidgets.getCurrentPage());
		Assert.assertEquals(page, listWidgets.getNextPage());
		// default delta is 10, page is 1: 6 entities stored in database
		Assert.assertEquals(1, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test45_findAllWidgetPaginatedShouldWorkIfDeltaIsZero() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// In this following call findAllWidgetPaginated, hadmin find all Widget with pagination
		// if delta is zero
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		int delta = 0;
		int page = 3;
		List<Widget> widgets = new ArrayList<>();
		int numbEntities = 22;
		for (int i = 0; i < numbEntities; i++) {
			Widget widget = createWidget(false);
			Assert.assertNotEquals(0, widget.getId());
			Assert.assertTrue(widget.isRealTime());
			widgets.add(widget);
		}
		Assert.assertEquals(numbEntities, widgets.size());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
		HyperIoTPaginableResult<Widget> listWidgets = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgets.getResults().isEmpty());
		Assert.assertEquals(numbEntities - (defaultDelta * 2), listWidgets.getResults().size());
		Assert.assertEquals(defaultDelta, listWidgets.getDelta());
		Assert.assertEquals(page, listWidgets.getCurrentPage());
		Assert.assertEquals(defaultPage, listWidgets.getNextPage());
		// default delta is 10, page is 3: 22 entities stored in database
		Assert.assertEquals(3, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());

		//checks with page = 1
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponsePage1 = widgetRestApi.findAllWidgetPaginated(delta, 1);
		HyperIoTPaginableResult<Widget> listWidgetsPage1 = restResponsePage1
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgetsPage1.getResults().isEmpty());
		Assert.assertEquals(defaultDelta, listWidgetsPage1.getResults().size());
		Assert.assertEquals(defaultDelta, listWidgetsPage1.getDelta());
		Assert.assertEquals(defaultPage, listWidgetsPage1.getCurrentPage());
		Assert.assertEquals(defaultPage + 1, listWidgetsPage1.getNextPage());
		// default delta is 10, page is 1: 22 entities stored in database
		Assert.assertEquals(3, listWidgetsPage1.getNumPages());
		Assert.assertEquals(200, restResponsePage1.getStatus());

		//checks with page = 2
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponsePage2 = widgetRestApi.findAllWidgetPaginated(delta, 2);
		HyperIoTPaginableResult<Widget> listWidgetsPage2 = restResponsePage2
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgetsPage2.getResults().isEmpty());
		Assert.assertEquals(defaultDelta, listWidgetsPage2.getResults().size());
		Assert.assertEquals(defaultDelta, listWidgetsPage2.getDelta());
		Assert.assertEquals(defaultPage + 1, listWidgetsPage2.getCurrentPage());
		Assert.assertEquals(page, listWidgetsPage2.getNextPage());
		// default delta is 10, page is 2: 22 entities stored in database
		Assert.assertEquals(3, listWidgetsPage2.getNumPages());
		Assert.assertEquals(200, restResponsePage2.getStatus());
	}


	@Test
	public void test46_findAllWidgetPaginatedShouldWorkIfPageIsLowerThanZero() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// In this following call findAllWidgetPaginated, hadmin find all Widget with pagination
		// if page is lower than zero
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		int delta = 4;
		int page = -1;
		List<Widget> widgets = new ArrayList<>();
		for (int i = 0; i < defaultDelta; i++) {
			Widget widget = createWidget(false);
			Assert.assertNotEquals(0, widget.getId());
			Assert.assertTrue(widget.isRealTime());
			widgets.add(widget);
		}
		Assert.assertEquals(defaultDelta, widgets.size());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
		HyperIoTPaginableResult<Widget> listWidgets = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgets.getResults().isEmpty());
		Assert.assertEquals(delta, listWidgets.getResults().size());
		Assert.assertEquals(delta, listWidgets.getDelta());
		Assert.assertEquals(defaultPage, listWidgets.getCurrentPage());
		Assert.assertEquals(defaultPage + 1, listWidgets.getNextPage());
		// delta is 4, default page is 1: 10 entities stored in database
		Assert.assertEquals(3, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());

		//checks with page = 2
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponsePage2 = widgetRestApi.findAllWidgetPaginated(delta, 2);
		HyperIoTPaginableResult<Widget> listWidgetsPage2 = restResponsePage2
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgetsPage2.getResults().isEmpty());
		Assert.assertEquals(delta, listWidgetsPage2.getResults().size());
		Assert.assertEquals(delta, listWidgetsPage2.getDelta());
		Assert.assertEquals(defaultPage + 1, listWidgetsPage2.getCurrentPage());
		Assert.assertEquals(defaultPage + 2, listWidgetsPage2.getNextPage());
		// delta is 10, page is 2: 10 entities stored in database
		Assert.assertEquals(3, listWidgetsPage2.getNumPages());
		Assert.assertEquals(200, restResponsePage2.getStatus());

		//checks with page = 3
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponsePage3 = widgetRestApi.findAllWidgetPaginated(delta, 3);
		HyperIoTPaginableResult<Widget> listWidgetsPage3 = restResponsePage3
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgetsPage3.getResults().isEmpty());
		Assert.assertEquals(defaultDelta - (delta * 2), listWidgetsPage3.getResults().size());
		Assert.assertEquals(delta, listWidgetsPage3.getDelta());
		Assert.assertEquals(defaultPage + 2, listWidgetsPage3.getCurrentPage());
		Assert.assertEquals(defaultPage, listWidgetsPage3.getNextPage());
		// delta is 10, page is 3: 10 entities stored in database
		Assert.assertEquals(3, listWidgetsPage3.getNumPages());
		Assert.assertEquals(200, restResponsePage3.getStatus());
	}


	@Test
	public void test47_findAllWidgetPaginatedShouldWorkIfPageIsZero() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// In this following call findAllWidgetPaginated, hadmin find all Widget with pagination
		// if page is zero
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		int delta = 7;
		int page = 0;
		List<Widget> widgets = new ArrayList<>();
		int numbEntities = 5;
		for (int i = 0; i < numbEntities; i++) {
			Widget widget = createWidget(false);
			Assert.assertNotEquals(0, widget.getId());
			Assert.assertTrue(widget.isRealTime());
			widgets.add(widget);
		}
		Assert.assertEquals(numbEntities, widgets.size());
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
		HyperIoTPaginableResult<Widget> listWidgets = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertFalse(listWidgets.getResults().isEmpty());
		Assert.assertEquals(numbEntities, listWidgets.getResults().size());
		Assert.assertEquals(delta, listWidgets.getDelta());
		Assert.assertEquals(defaultPage, listWidgets.getCurrentPage());
		Assert.assertEquals(defaultPage, listWidgets.getNextPage());
		// delta is 7, default page is 1: 5 entities stored in database
		Assert.assertEquals(1, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test48_findAllWidgetPaginatedShouldFailIfNotLogged() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// the following call tries to find all Widget with pagination,
		// but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		this.impersonateUser(widgetRestApi, null);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(defaultDelta, defaultPage);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test49_findAllWidgetInCategoriesShouldWorkIfTypeIsRealTime() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin find all widget in categories widget with the following
		// call findAllWidgetInCategories. This call checks if Widget RealTime
		// is actually stored in database
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetInCategories("realTime");
		Assert.assertEquals(200, restResponse.getStatus());

		// checks if Widget RealTime is actually stored in database
		HashMap<String, Object> test = ((HashMap)restResponse.getEntity());
		HashMap widgetMap = (HashMap) test.get("widgetMap");

		Set entrySet = widgetMap.entrySet();
		// Obtaining an iterator for the entry set
		Iterator it = entrySet.iterator();

		while(it.hasNext()){
			Map.Entry me = (Map.Entry)it.next();
			if (((ArrayList) me.getValue()).size() != 0) {
				for (int i = 0; i < ((ArrayList) me.getValue()).size(); i++) {
					Widget widgetRealtime = ((Widget) ((ArrayList) me.getValue()).get(i));
					Assert.assertNotEquals(0, widgetRealtime.getId());
					Assert.assertTrue(widgetRealtime.isRealTime());
					System.out.println("Name is: " + me.getKey() +
							" & \t" +
							" Widget is: " + widgetRealtime.getName() +
							", isRealtime? " + widgetRealtime.isRealTime());
				}
			}
		}
	}


	@Test
	public void test50_findAllWidgetInCategoriesShouldWorkIfTypeIsOffline() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin find all widget in categories widget with the following
		// call findAllWidgetInCategories. This call checks if Widget Offline
		// is actually stored in database
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(true);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isOffline());

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetInCategories("offline");
		Assert.assertEquals(200, restResponse.getStatus());

		// checks if Widget RealTime is actually stored in database
		HashMap<String, Object> test = ((HashMap)restResponse.getEntity());
		HashMap widgetMap = (HashMap) test.get("widgetMap");

		Set entrySet = widgetMap.entrySet();
		// Obtaining an iterator for the entry set
		Iterator it = entrySet.iterator();

		while(it.hasNext()){
			Map.Entry me = (Map.Entry)it.next();
			if (((ArrayList) me.getValue()).size() != 0) {
				for (int i = 0; i < ((ArrayList) me.getValue()).size(); i++) {
					Widget widgetOffline = ((Widget) ((ArrayList) me.getValue()).get(i));
					Assert.assertNotEquals(0, widgetOffline.getId());
					Assert.assertTrue(widgetOffline.isOffline());
					System.out.println("Name is: " + me.getKey() +
							" & \t" +
							" Widget is: " + widgetOffline.getName() +
							", isOffline? " + widgetOffline.isOffline());
				}
			}
		}
	}


	@Test
	public void test51_findAllWidgetShouldWorkIfListIsEmpty() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin find all Widget with the following call findAllWidget
		// there are no entities saved in the database, this call return an empty list
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidget();
		List<Widget> listWidgets = restResponse.readEntity(new GenericType<List<Widget>>() {
		});
		Assert.assertTrue(listWidgets.isEmpty());
		Assert.assertEquals(0, listWidgets.size());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test52_findAllWidgetPaginatedShouldWorkIfListIsEmpty() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// In this following call findAllWidgetPaginated, hadmin find all Widget with pagination.
		// there are no entities saved in the database, this call return an empty list
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.findAllWidgetPaginated(defaultDelta, defaultPage);
		HyperIoTPaginableResult<Widget> listWidgets = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
				});
		Assert.assertTrue(listWidgets.getResults().isEmpty());
		Assert.assertEquals(0, listWidgets.getResults().size());
		Assert.assertEquals(defaultDelta, listWidgets.getDelta());
		Assert.assertEquals(defaultPage, listWidgets.getCurrentPage());
		Assert.assertEquals(defaultPage, listWidgets.getNextPage());
		// default delta is 10, default page is 1: there are not entities stored in database
		Assert.assertEquals(0, listWidgets.getNumPages());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test53_rateWidgetShouldWork() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin rate Widget with the following call rateWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		WidgetRating widgetRating = new WidgetRating();
		widgetRating.setWidget(widget);
		widgetRating.setRating(4);

		// userId inside WidgetRating table is set by loggedUsername (hadmin)
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.rateWidget(widgetRating.getRating(), widget);
		Assert.assertEquals(200, restResponse.getStatus());

		//checks if widgetId inside WidgetRating table is equals to widget.getId()
		String sqlWidgetId = "select wr.widget_id from widgetrating wr";
		String resultWidgetId = executeCommand("jdbc:query hyperiot " + sqlWidgetId);
		String[] wrId = resultWidgetId.split("\\n");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponseFindWidget = widgetRestApi.findWidget(Long.parseLong(wrId[2]));
		Assert.assertEquals(200, restResponseFindWidget.getStatus());
		Assert.assertEquals(widget.getId(), ((Widget) restResponseFindWidget.getEntity()).getId());

		//checks if avgRating has been updated inside Widget table
		String sqlRating = "select wr.rating from widgetrating wr";
		String resultRating = executeCommand("jdbc:query hyperiot " + sqlRating);
		String[] wrRating = resultRating.split("\\n");
		Assert.assertEquals(((Float)Float.parseFloat(wrRating[2])),
				((Widget) restResponseFindWidget.getEntity()).getAvgRating());

		//checks if userId inside widgetRating table is equals to huser.getId()
		String sqlHuserId = "select wr.user_id from widgetrating wr";
		String resultHuserId = executeCommand("jdbc:query hyperiot " + sqlHuserId);
		String[] wrHuserId = resultHuserId.split("\\n");

		Assert.assertEquals(adminUser.getId(), Integer.parseInt(wrHuserId[2]));

		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponseFindHUser = huserRestService.findHUser(Long.parseLong(wrHuserId[2]));
		Assert.assertEquals(200, restResponseFindHUser.getStatus());
		Assert.assertEquals(adminUser.getId(), ((HUser) restResponseFindHUser.getEntity()).getId());
	}


	@Test
	public void test54_rateWidgetShouldWorkIfRatingIsLowerThan1() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin rate Widget with the following call rateWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		WidgetRating widgetRating = new WidgetRating();
		widgetRating.setWidget(widget);
		widgetRating.setUser((HUser) adminUser);
		widgetRating.setRating(-4);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.rateWidget(widgetRating.getRating(), widget);
		Assert.assertEquals(200, restResponse.getStatus());

		//checks if avgRating has been updated inside Widget table
		String sqlRating = "select wr.rating from widgetrating wr";
		String resultRating = executeCommand("jdbc:query hyperiot " + sqlRating);
		String[] wrRating = resultRating.split("\\n");
		Assert.assertEquals(1, Integer.parseInt(wrRating[2]));
	}


	@Test
	public void test55_rateWidgetShouldWorkIfRatingIsGreaterThan5() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin rate Widget with the following call rateWidget
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		WidgetRating widgetRating = new WidgetRating();
		widgetRating.setWidget(widget);
		widgetRating.setUser((HUser) adminUser);
		widgetRating.setRating(9);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.rateWidget(widgetRating.getRating(), widget);
		Assert.assertEquals(200, restResponse.getStatus());

		//checks if avgRating has been updated inside Widget table
		String sqlRating = "select wr.rating from widgetrating wr";
		String resultRating = executeCommand("jdbc:query hyperiot " + sqlRating);
		String[] wrRating = resultRating.split("\\n");
		Assert.assertEquals(5, Integer.parseInt(wrRating[2]));
	}


	@Test
	public void test56_rateWidgetShouldFailIfHUserIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// This call tries to rate Widget with the following call rateWidget,
		// but huser is not found
		// response status code '404' HyperIoTEntityNotFound
		Widget widget = createWidget(false);
		Assert.assertNotEquals(0, widget.getId());
		Assert.assertTrue(widget.isRealTime());

		this.impersonateUser(widgetRestApi, null);
		Response restResponse = widgetRestApi.rateWidget(3, widget);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test57_rateWidgetShouldFailIfWidgetIsNull() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to rate Widget with the following call rateWidget,
		// but widget is not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.rateWidget(3, null);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test58_saveWidgetShouldWorkIfNameIs500Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// name's lenght is 500 chars.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName(createStringFieldWithSpecifiedLenght(500));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(500, ((Widget) restResponse.getEntity()).getName().length());
		Assert.assertNotEquals(0 , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test59_saveWidgetShouldWorkIfDescriptionIs3000Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// description's lenght is 3000 chars.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("Widget"+UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(createStringFieldWithSpecifiedLenght(3000));
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(3000, ((Widget) restResponse.getEntity()).getDescription().length());
		Assert.assertNotEquals(0 , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test60_saveWidgetShouldWorkIfNameIfImageIs30000Byte() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// image's lenght is 30000 byte.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("Widget"+UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(createRandomByteArray(30000));
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(30000, ((Widget) restResponse.getEntity()).getImage().length);
		Assert.assertNotEquals(0 , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test61_saveWidgetShouldFailIfNameIfImageSizeIsExceeded() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but image's lenght is over 30000 byte.
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("Widget"+UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(createRandomByteArray(200001));
		widget.setPreView(widgetImageDataPreview);
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-image", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test62_saveWidgetShouldWorkIfPreviewSizeIsNotExceeded() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// preview's lenght is 30000 byte.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("Widget"+UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(createRandomByteArray(200000));
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(200000, ((Widget) restResponse.getEntity()).getPreView().length);
		Assert.assertNotEquals(0 , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test63_saveWidgetShouldFailIfNameIfImageSizeIsExceeded() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to save Widget with the following call saveWidget,
		// but image's lenght is over 30000 byte.
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = new Widget();
		widget.setName("Widget"+UUID.randomUUID().toString().replaceAll("-", ""));
		widget.setDescription(widgetDescription);
		widget.setWidgetCategory(WidgetCategory.ALL);
		widget.setBaseConfig("image-data");
		widget.setType("image-data");
		widget.setCols(2);
		widget.setRows(3);
		widget.setImage(widgetImageData);
		widget.setPreView(createRandomByteArray(200001));
		widget.setOffline(false);
		widget.setRealTime(true);

		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.saveWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-preview", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test64_updateWidgetShouldWorkIfNameIs500Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// name's lenght is 500 chars.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		Widget widget = createWidget(true);
		long widgetId = widget.getId();
		widget.setName(createStringFieldWithSpecifiedLenght(500));
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(500, ((Widget) restResponse.getEntity()).getName().length());
		Assert.assertEquals(widgetId , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test65_updateWidgetShouldWorkIfDescriptionIs3000Chars() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// description's lenght is 3000 chars.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(true);
		long widgetId = widget.getId();
		widget.setDescription(createStringFieldWithSpecifiedLenght(3000));
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(3000, ((Widget) restResponse.getEntity()).getDescription().length());
		Assert.assertEquals(widgetId , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test66_updateWidgetShouldWorkIfImageIs30000Byte() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// image's lenght is 30000 byte.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(true);
		long widgetId = widget.getId();
		widget.setImage(createRandomByteArray(30000));
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(30000, ((Widget) restResponse.getEntity()).getImage().length);
		Assert.assertEquals(widgetId , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test67_updateWidgetShouldFailIfImageSizeIsExceeded() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but image's lenght is over 30000 byte.
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(true);
		long widgetId = widget.getId();
		widget.setImage(createRandomByteArray(200001));
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-image", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test68_updateWidgetShouldWorkIfPreviewIs30000Byte() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// preview's lenght is 30000 byte.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(true);
		long widgetId = widget.getId();
		widget.setPreView(createRandomByteArray(30000));
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(30000, ((Widget) restResponse.getEntity()).getPreView().length);
		Assert.assertEquals(widgetId , ((Widget)restResponse.getEntity()).getId());
	}

	@Test
	public void test69_updateWidgetShouldFailIfPreviewSizeIsExceeded() {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		// hadmin tries to update Widget with the following call updateWidget,
		// but preview's lenght is over 30000 byte.
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Widget widget = createWidget(true);
		long widgetId = widget.getId();
		widget.setPreView(createRandomByteArray(200001));
		this.impersonateUser(widgetRestApi, adminUser);
		Response restResponse = widgetRestApi.updateWidget(widget);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertFalse(((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage().isEmpty());
		Assert.assertEquals("widget-preview", ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}


	/*
	 *
	 *
	 * UTILITY METHODS
	 *
	 *
	 */

	private byte[] createRandomByteArray(int length){
		byte[] b = new byte[length];
		new Random().nextBytes(b);
		return b;
	}

	private String createStringFieldWithSpecifiedLenght(int length) {
		String symbol = "a";
		String field = String.format("%" + length + "s", " ").replaceAll(" ", symbol);
		Assert.assertEquals(length, field.length());
		return field;
	}

	private Widget createWidget(boolean isOffline) {
		WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

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

	private String testMaxLength(int maxLength) {
		String symbol = "a";
		String descriptionLength = String.format("%" + maxLength + "s", " ").replaceAll(" ", symbol);
		Assert.assertEquals(maxLength, descriptionLength.length());
		return descriptionLength;
	}

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
//        String sqlWidget = "select w.id, w.widgetCategory from widget w";
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

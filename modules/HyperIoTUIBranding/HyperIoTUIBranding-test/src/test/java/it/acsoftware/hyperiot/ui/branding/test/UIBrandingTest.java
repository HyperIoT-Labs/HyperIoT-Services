package it.acsoftware.hyperiot.ui.branding.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.huser.test.util.HyperIoTHUserTestUtils;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.test.util.HyperIoTPermissionTestUtil;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.ui.branding.model.UIBranding;
import it.acsoftware.hyperiot.ui.branding.model.view.Isolated;
import it.acsoftware.hyperiot.ui.branding.service.rest.UIBrandingRestApi;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.checkerframework.checker.guieffect.qual.UI;
import org.junit.After;
import org.junit.Assert;
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
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Aristide Cittadino.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UIBrandingTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        UIBrandingTestConfiguration conf = new UIBrandingTestConfiguration();
        return conf.createConfiguration();
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
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
    public void test000_hyperIoTFrameworkShouldBeInstalled() throws Exception {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class, 0);
        String features = executeCommand("feature:list -i");
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test001_modelShouldWorkAsExpected() {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser u = (HUser) authService.login("hadmin", "admin");
        UIBrandingRestApi brandingRestApi = getOsgiService(UIBrandingRestApi.class);
        brandingRestApi.impersonate(u);
        UIBranding ui = createUIBrandingObject(u, "/provaPath", "/provaPath");
        Assert.assertNotNull(ui.getName());
        Assert.assertNotNull(ui.getColorScheme());
        Assert.assertNotNull(ui.getHuser());
        Assert.assertNotNull(ui.getLogoPath());
        Assert.assertNotNull(ui.getFaviconPath());
        Response response = brandingRestApi.save(ui);
        ui = (UIBranding) response.getEntity();
        Assert.assertTrue(ui.getId() > 0);
    }

    @Test
    public void test002_uiBrandingShouldBeSavedOrUpdated() {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser u = (HUser) authService.login("hadmin", "admin");
        UIBrandingRestApi brandingRestApi = getOsgiService(UIBrandingRestApi.class);
        brandingRestApi.impersonate(u);
        UIBranding ui = createUIBrandingObject(u, "/provaPath", "/provaPath");
        Response response = brandingRestApi.updateUIBranding("nuovoNome", "nuovoColor", null, null);
        ui = (UIBranding) response.getEntity();
        Assert.assertTrue(ui.getId() > 0);
        Assert.assertTrue(ui.getName().equals("nuovoNome"));
        Assert.assertTrue(ui.getColorScheme().equals("nuovoColor"));
    }

    @Test
    public void test003_useShouldAccessToTheirBrandingOnly() {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser u = (HUser) authService.login("hadmin", "admin");
        UIBrandingRestApi brandingRestApi = getOsgiService(UIBrandingRestApi.class);
        brandingRestApi.impersonate(u);
        Response r = brandingRestApi.getUIBranding();
        UIBranding ui = (UIBranding) r.getEntity();
        Assert.assertTrue(ui.getHuser().getId() == u.getId());
    }

    @Test
    public void test004_userShouldReceiveJustNeededData() throws JsonProcessingException {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUser u = (HUser) authService.login("hadmin", "admin");
        UIBrandingRestApi brandingRestApi = getOsgiService(UIBrandingRestApi.class);
        brandingRestApi.impersonate(u);
        Response r = brandingRestApi.getUIBranding();
        UIBranding ui = (UIBranding) r.getEntity();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithView(Isolated.class).writeValueAsString(ui);
        Map<String, Object> jsonValues = new HashMap<>();
        jsonValues = mapper.readValue(json, HashMap.class);
        Assert.assertTrue(jsonValues.containsKey("name"));
        Assert.assertTrue(jsonValues.containsKey("colorScheme"));
        Assert.assertTrue(!jsonValues.containsKey("id"));
        Assert.assertTrue(!jsonValues.containsKey("tagIds"));
        Assert.assertTrue(!jsonValues.containsKey("categoryIds"));
        Assert.assertTrue(!jsonValues.containsKey("entityVersion"));
    }

    @Test
    public void test005_assetsShouldBeErasedWhenUserIsDeleted() {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        HUser u = (HUser) authService.login("hadmin", "admin");
        hUserRestApi.impersonate(u);
        roleRestApi.impersonate(u);
        HUser newUser = HyperIoTHUserTestUtils.registerAndActivateNewUser(hUserRestApi);
        UIBrandingRestApi brandingRestApi = getOsgiService(UIBrandingRestApi.class);
        brandingRestApi.impersonate(newUser);
        brandingRestApi.updateUIBranding("nuovoBrand", "nuovoScheme", null, null);
        hUserRestApi.deleteHUser(newUser.getId());
    }

    @After
    public void afterTest() {
        HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        HyperIoTPermissionTestUtil.dropPermissions(roleSystemApi, permissionSystemApi);
        HyperIoTHUserTestUtils.truncateHUsers(hUserSystemApi);
    }

    private UIBranding createUIBrandingObject(HUser currentUser, String logoPath, String faviconPath) {
        UIBranding ui = new UIBranding();
        ui.setName("nome");
        ui.setColorScheme("red");
        ui.setHuser(currentUser);
        ui.setLogoPath(logoPath);
        ui.setFaviconPath(faviconPath);
        return ui;
    }
}
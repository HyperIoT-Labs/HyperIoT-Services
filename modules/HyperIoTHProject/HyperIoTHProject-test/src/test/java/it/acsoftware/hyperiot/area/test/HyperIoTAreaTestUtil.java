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

package it.acsoftware.hyperiot.area.test;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.authentication.AuthenticationApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class HyperIoTAreaTestUtil {

    private static HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    public static void eraseDatabase(KarafTestSupport testClass) {
        // Remove projects and delete in cascade all Area created in every test
        AuthenticationApi authService = testClass.getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        RoleRestApi roleRestService = testClass.getOsgiService(RoleRestApi.class);
        HProjectRestApi hprojectRestService = testClass.getOsgiService(HProjectRestApi.class);
        impersonateUser(hprojectRestService, adminUser);
        Response restResponse = hprojectRestService.findAll();
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        if (!listHProjects.isEmpty()) {
            Assert.assertFalse(listHProjects.isEmpty());
            for (HProject project : listHProjects) {

                Response restResponse1 = hprojectRestService.deleteHProject(project.getId());
                Assert.assertEquals(200, restResponse1.getStatus());
            }
        }
        impersonateUser(roleRestService, adminUser);
        // Remove all roles and permissions (in cascade mode) created in every test
        Response restResponseRole = roleRestService.findAllRoles();
        List<Role> listRoles = restResponseRole.readEntity(new GenericType<List<Role>>() {
        });
        if (!listRoles.isEmpty()) {
            Assert.assertFalse(listRoles.isEmpty());
            for (Role role : listRoles) {
                if (!role.getName().contains("RegisteredUser")) {
                    impersonateUser(roleRestService, adminUser);
                    Response restResponseRole1 = roleRestService.deleteRole(role.getId());
                    Assert.assertEquals(200, restResponseRole1.getStatus());
                    Assert.assertNull(restResponseRole1.getEntity());
                }
            }
        }

        // Remove all husers created in every test
        HUserRestApi huserRestService = testClass.getOsgiService(HUserRestApi.class);
        impersonateUser(huserRestService, adminUser);
        Response restResponseUsers = huserRestService.findAllHUser();
        List<HUser> listHUsers = restResponseUsers.readEntity(new GenericType<List<HUser>>() {
        });
        if (!listHUsers.isEmpty()) {
            for (HUser huser : listHUsers) {
                if (!huser.isAdmin()) {
                    impersonateUser(huserRestService, adminUser);
                    Response restResponse1 = huserRestService.deleteHUser(huser.getId());
                    Assert.assertEquals(200, restResponse1.getStatus());
                    Assert.assertNull(restResponse1.getEntity());
                }
            }
        }
    }

    public static FileInputStream getImageAttachment() {
        FileInputStream imageFile = null;
        try {
            File image = new File("resources/image.jpg");
            imageFile = new FileInputStream(image);
            return imageFile;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}

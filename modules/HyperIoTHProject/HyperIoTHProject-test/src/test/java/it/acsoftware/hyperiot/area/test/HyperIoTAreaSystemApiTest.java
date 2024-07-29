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

import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.exception.HyperIoTNoResultException;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestUtil;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import java.util.Collection;
import java.util.UUID;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAreaSystemApiTest extends KarafTestSupport {

    //force global config
    @Override
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
    public void test01_areaListShouldContainTestArea() {
        Area created = getTestArea();
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        Collection<Area> areaList = areaSystemApi.findAll(null, null);
        Area area = areaList.stream().filter(a -> a.getName().equals(created.getName())).findAny().orElse(null);
        Assert.assertNotNull(area);
    }

    @Test
    public void test02_getAndUpdateTestArea() {

        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        Area area = getTestArea();
        Assert.assertNotNull(area);
        area.setDescription(HyperIoTAreaConfiguration.testAreaUpdatedDescription);
        areaSystemApi.update(area, null);
    }

    @Test
    public void test03_areaListShouldContainTestAreaWithNewDescription() {
        Area created = getTestArea();
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        Collection<Area> areaList = areaSystemApi.findAll(null, null);
        Area area = areaList.stream().filter(a -> a.getId() == created.getId()).findAny().orElse(null);
        Assert.assertNotNull(area);
    }


    @Test
    public void test04_areaDeviceListShouldContainTestDevice() {
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        AreaDevice ad = getAreaDevice();
        Collection<AreaDevice> deviceList = areaSystemApi.getAreaDevicesList(ad.getArea());
        AreaDevice device = deviceList.stream().filter(d -> d.getDevice().getId() == ad.getDevice().getId()).findFirst()
                .orElse(null);
        Assert.assertNotNull(device);
    }

    @Test
    public void test05_removeAreaDevice() {
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        AreaDevice ad = getAreaDevice();
        Collection<AreaDevice> deviceList = areaSystemApi.getAreaDevicesList(ad.getArea());
        AreaDevice areaDeviceFound = deviceList.stream().filter(d -> d.getDevice().getId() == ad.getDevice().getId()).findFirst()
                .orElse(null);
        Assert.assertNotNull(areaDeviceFound);
        areaSystemApi.removeAreaDevice(areaDeviceFound.getArea(), ad.getId());
    }


    @Test(expected = HyperIoTNoResultException.class)
    public void test06_deleteTestArea() {
        Area a = getTestArea();
        long testAreaId = a.getId();
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        areaSystemApi.remove(a.getId(), null);
        Area area = areaSystemApi.find(testAreaId, null);
        Assert.assertNull(area);
    }


    // Utility functions

    private HDevice getTestDevice() {
        HDeviceSystemApi deviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        Collection<HDevice> allDevices = deviceSystemApi.findAll(null, null);
        HDevice hdevice = new HDevice();
        hdevice.setDeviceName(UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setDescription("Just a test device");
        hdevice.setBrand("Marvellous devices Inc.");
        hdevice.setFirmwareVersion("1.0");
        hdevice.setModel("Claudia Shiffer");
        hdevice.setSoftwareVersion("1.1-RollerCoaster");
        hdevice.setPassword("AmbaraBacciCiCocò_1");
        hdevice.setPasswordConfirm("AmbaraBacciCiCocò_1");
        hdevice.setProject(getTestProject());
        deviceSystemApi.save(hdevice, null);
        return hdevice;
    }

    private HProject getTestProject() {
        HProjectSystemApi projectSystemApi = getOsgiService(HProjectSystemApi.class);
        HProject testProject = new HProject();
        testProject.setName("Test Project");
        testProject.setDescription("Just a test project");
        testProject.setUser(getTestUser());
        projectSystemApi.save(testProject, null);
        return testProject;
    }

    private HUser getTestUser() {
        HUserSystemApi userSystemApi = getOsgiService(HUserSystemApi.class);
        String username = UUID.randomUUID().toString().replaceAll("-", "");
        HUser testUser = new HUser();
        testUser.setAdmin(false);
        testUser.setEmail(username + "@bar.com");
        testUser.setName("Foo");
        testUser.setLastname("Bar");
        testUser.setUsername(username);
        testUser.setPassword("testPassword&%123");
        testUser.setPasswordConfirm("testPassword&%123");
        userSystemApi.save(testUser, null);
        return testUser;
    }

    private Area getTestArea() {
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        Area area = new Area();
        area.setName(UUID.randomUUID().toString());
        area.setDescription("Just a test area");
        area.setProject(getTestProject());
        areaSystemApi.save(area, null);
        return area;
    }

    private AreaDevice getAreaDevice() {
        AreaSystemApi areaSystemApi = getOsgiService(AreaSystemApi.class);
        Area a = getTestArea();
        AreaDevice ad = new AreaDevice();
        ad.setArea(a);
        ad.setDevice(getTestDevice());
        areaSystemApi.
                saveAreaDevice(a, ad);
        return ad;
    }

    @After
    public void afterTest() {
        HyperIoTAreaTestUtil.eraseDatabase(this);
    }
}

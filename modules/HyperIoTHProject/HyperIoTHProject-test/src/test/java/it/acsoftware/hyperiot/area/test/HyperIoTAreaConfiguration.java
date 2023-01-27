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

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.model.HProject;

public class HyperIoTAreaConfiguration {

    static final String testAreaUpdatedDescription = "Updated test area description :)";
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String areaResourceName = Area.class.getName();
    static final String hProjectResourceName = HProject.class.getName();
    static final String hDeviceResourceName = HDevice.class.getName();
    static final String areaImageName = "image.jpg";

    static final String permissionArea = "it.acsoftware.hyperiot.area.model.Area";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

    static final int defaultDelta = 10;
    static final int defaultPage = 1;

}

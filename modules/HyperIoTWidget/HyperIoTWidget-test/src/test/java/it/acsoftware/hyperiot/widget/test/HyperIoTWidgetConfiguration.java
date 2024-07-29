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

import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import it.acsoftware.hyperiot.widget.model.Widget;
import org.ops4j.pax.exam.ConfigurationFactory;
import org.ops4j.pax.exam.Option;

public class HyperIoTWidgetConfiguration implements ConfigurationFactory {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String widgetResourceName = Widget.class.getName();

    static final int maxLengthName = 501;
    static final int maxLengthDescription = 3001;
    static final String widgetDescription = "Display image based on thermal camera data array";
    static final byte[] widgetImageData = new byte[1000];//"13.572";
    static final byte[] widgetImageDataPreview = new byte[1000];

    static final String permissionWidget = "it.acsoftware.hyperiot.widget.model.Widget";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

    static final int defaultDelta = 10;
    static final int defaultPage = 1;

    @Override
    public Option[] createConfiguration() {
        return HyperIoTServicesTestConfigurationBuilder.createStandardConfiguration()
                .withCodeCoverage("it.acsoftware.hyperiot.widget.*")
                .keepRuntime()
                .build();
    }
}

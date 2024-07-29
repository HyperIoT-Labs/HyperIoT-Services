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

package it.acsoftware.hyperiot.algorithm.test;

import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesTestConfigurationBuilder;
import org.ops4j.pax.exam.ConfigurationFactory;
import org.ops4j.pax.exam.Option;

public class HyperIoTAlgorithmConfiguration implements ConfigurationFactory {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String algorithmResourceName = Algorithm.class.getName();
    static final int maxLengthDescription = 3001;

    //jar file
    static final String jarName = "algorithm_test001.jar";
    static final String jarPath = "resources/";

    static final int defaultDelta = 10;
    static final int defaultPage = 1;

    static final String permissionAlgorithm = "it.acsoftware.hyperiot.algorithm.model.Algorithm";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

    @Override
    public Option[] createConfiguration() {
        return HyperIoTServicesTestConfigurationBuilder.createStandardConfiguration()
                .withCodeCoverage("it.acsoftware.hyperiot.algorithm.*")
                .withDebug("5005",false)
                .build();
    }
}

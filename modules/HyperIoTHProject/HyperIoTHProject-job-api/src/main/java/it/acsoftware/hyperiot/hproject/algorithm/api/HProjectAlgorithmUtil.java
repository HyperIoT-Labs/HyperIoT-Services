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

package it.acsoftware.hyperiot.hproject.algorithm.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;

public interface HProjectAlgorithmUtil {

    /**
     * This method serializes HProjectAlgorithmConfig through Jackson, providing default values if fields are not filled
     * @param config HProjectAlgorithmConfig
     * @return String
     */
    String getConfigString(HProjectAlgorithmConfig config) throws JsonProcessingException;

}

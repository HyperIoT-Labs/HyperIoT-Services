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

package it.acsoftware.hyperiot.hproject.algorithm.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmUtil;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;

@Component(service = HProjectAlgorithmUtil.class, immediate = true)
public class HProjectAlgorithmUtilImpl implements HProjectAlgorithmUtil {

    public String getConfigString(HProjectAlgorithmConfig config) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (config.getInput() == null) {
            config.setInput(new ArrayList<>());
        }
        if (config.getOutput() == null)
            config.setOutput(new ArrayList<>());
        return objectMapper.writeValueAsString(config);
    }

}

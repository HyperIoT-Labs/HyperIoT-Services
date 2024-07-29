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

package it.acsoftware.hyperiot.hproject.algorithm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HProjectAlgorithmResultEntry {
    private HashMap<String, String> grouping;
    private Double output;

    public HashMap<String, String> getGrouping() {
        return grouping;
    }

    public void setGrouping(HashMap<String, String> grouping) {
        this.grouping = grouping;
    }

    public Double getOutput() {
        return output;
    }

    public void setOutput(Double output) {
        this.output = output;
    }

    /**
     *
     * @return a flat map of fieldId: value and output
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        if (grouping != null) {
            grouping.keySet().iterator().forEachRemaining(key -> map.put(key, grouping.get(key)));
        }
        map.put("output", output);
        return map;
    }
}

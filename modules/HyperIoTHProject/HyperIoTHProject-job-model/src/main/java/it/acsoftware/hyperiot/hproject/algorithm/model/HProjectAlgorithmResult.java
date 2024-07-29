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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HProjectAlgorithmResult {
    private List<HProjectAlgorithmResultEntry> results;
    @JsonIgnore
    private long hProjectAlgorithmId;
    @JsonIgnore
    private long projectId;
    @JsonIgnore
    private long timestamp;

    public long gethProjectAlgorithmId() {
        return hProjectAlgorithmId;
    }

    public void sethProjectAlgorithmId(long hProjectAlgorithmId) {
        this.hProjectAlgorithmId = hProjectAlgorithmId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<HProjectAlgorithmResultEntry> getResults() {
        return results;
    }

    public void setResults(List<HProjectAlgorithmResultEntry> results) {
        this.results = results;
    }

    /**
     *
     * @return a flat map of fieldId: value and output
     */
    public List<Map<String, Object>> toMap() {
        List<Map<String, Object>> values = new ArrayList<>();
        results.forEach(result -> {
            values.add(result.toMap());
        });
        return values;
    }
}

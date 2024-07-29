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

package it.acsoftware.hyperiot.algorithm.model;


import java.util.List;
import java.util.Objects;

public class AlgorithmConfig {

    private List<AlgorithmIOField> input;
    private List<AlgorithmIOField> output;
    private String customConfig;

    public List<AlgorithmIOField> getInput() {
        return input;
    }

    public void setInput(List<AlgorithmIOField> input) {
        this.input = input;
    }

    public List<AlgorithmIOField> getOutput() {
        return output;
    }

    public void setOutput(List<AlgorithmIOField> output) {
        this.output = output;
    }

    public String getCustomConfig() {
        return customConfig;
    }

    public void setCustomConfig(String customConfig) {
        this.customConfig = customConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlgorithmConfig that = (AlgorithmConfig) o;
        return Objects.equals(input, that.input) &&
                Objects.equals(output, that.output);
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output);
    }

}

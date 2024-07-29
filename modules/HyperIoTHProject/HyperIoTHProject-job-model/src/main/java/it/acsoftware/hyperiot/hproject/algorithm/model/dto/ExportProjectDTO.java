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

package it.acsoftware.hyperiot.hproject.algorithm.model.dto;

import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.model.HProject;

import java.util.LinkedList;
import java.util.List;

public class ExportProjectDTO {

    private HProject project;

    private List<HProjectAlgorithm> algorithmsList;

    public ExportProjectDTO(){
        this.algorithmsList = new LinkedList<>();
    }

    public HProject getProject() {
        return project;
    }

    public void setProject(HProject project) {
        this.project = project;
    }

    public List<HProjectAlgorithm> getAlgorithmsList() {
        return algorithmsList;
    }

    public void setAlgorithmsList(List<HProjectAlgorithm> algorithmsList) {
        this.algorithmsList = algorithmsList;
    }
}

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

import it.acsoftware.hyperiot.algorithm.model.Algorithm;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;

import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;

import java.util.Collection;

/**
 *
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface HProjectAlgorithmSystemApi extends HyperIoTBaseEntitySystemApi<HProjectAlgorithm> {

    /**
     * It returns algorithms which have been defined for given HProject
     * @param hProjectId ID of HProject
     * @return Collection of Algorithm
     */
    Collection<HProjectAlgorithm> findByHProjectId(long hProjectId);

    /**
     *
     * @param hProjectId
     */
    void removeByHProjectId(long hProjectId);

    /**
     * It updates configuration of given HProjectAlgorithm
     * @param hProjectAlgorithmId HProjectAlgorithm ID
     * @param config configuration to be updated
     * @return Updated HProjectAlgorithm
     */
    HProjectAlgorithm updateConfig(long hProjectAlgorithmId, HProjectAlgorithmConfig config);


    Algorithm findAlgorithmByName(String algorithmName);


}

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

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;

import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;

import java.util.Collection;

/**
 *
 * @author Aristide Cittadino Interface component for HProjectAlgorithmApi. This interface
 *         defines methods for additional operations.
 *
 */
public interface HProjectAlgorithmApi extends HyperIoTBaseEntityApi<HProjectAlgorithm> {

    /**
     * It returns algorithms which have been defined for given HProject
     * @param hyperIoTContext hyperIoTContext
     * @param hProjectId ID of HProject
     * @return Collection of Algorithm
     */
    Collection<HProjectAlgorithm> findByHProjectId(HyperIoTContext hyperIoTContext, long hProjectId);


    /**
     * It updates configuration of HProjectAlgorithm with given ID
     * @param context hyperIoTContext
     * @param hProjectAlgorithmId ID of HProjectAlgorithm
     * @param config configuration to be updated
     * @return Updated HProjectAlgorithm
     */
    HProjectAlgorithm updateConfig(HyperIoTContext context, long hProjectAlgorithmId, HProjectAlgorithmConfig config);

}

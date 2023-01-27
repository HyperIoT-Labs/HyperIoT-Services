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

package it.acsoftware.hyperiot.algorithm.api;

import it.acsoftware.hyperiot.algorithm.model.AlgorithmConfig;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmFieldType;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmIOField;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;

import it.acsoftware.hyperiot.algorithm.model.Algorithm;

import java.io.File;

/**
 * 
 * @author Aristide Cittadino Interface component for AlgorithmApi. This interface
 *         defines methods for additional operations.
 *
 */
public interface AlgorithmApi extends HyperIoTBaseEntityApi<Algorithm> {

    /**
     * It add IO field to Algorithm with given ID
     * @param context HyperIoTContext
     * @param algorithmId Algorithm ID
     * @param ioField IO field
     * @return Updated algorithm
     */
    Algorithm addIOField(HyperIoTContext context, long algorithmId, AlgorithmIOField ioField);

    /**
     * It delete IO field of Algorithm with given ID
     * @param context HyperIoTContext
     * @param algorithmId Algorithm ID
     * @param fieldType Field type, i.e. input or output
     * @param ioFieldId IO field ID
     * @return Updated algorithm
     */
    Algorithm deleteIOField(HyperIoTContext context, long algorithmId, AlgorithmFieldType fieldType, long ioFieldId);

    /**
     * It returns base configuration of Algorithm with given ID
     * @param context HyperIoTContext
     * @param algorithmId Algorithm ID
     * @return Updated algorithm
     */
    String getBaseConfig(HyperIoTContext context, long algorithmId);

    /**
     * It updates base configuration of Algorithm with given ID
     * @param context HyperIoTContext
     * @param algorithmId Algorithm ID
     * @param baseConfig Base Configuration
     * @return Updated algorithm
     */
    Algorithm updateBaseConfig(HyperIoTContext context, long algorithmId, AlgorithmConfig baseConfig);

    /**
     * It updates algorithmFile of Algorithm with given ID
     * @param context HyperIoTContext
     * @param algorithmId Algorithm ID
     * @param mainClassname Class containing main method
     * @param algorithmFile Jar file
     * @return Updated algorithm
     */
    Algorithm updateAlgorithmFile(HyperIoTContext context, long algorithmId, String mainClassname, File algorithmFile);

    /**
     * It update IO field of Algorithm with given ID
     * @param context HyperIoTContext
     * @param algorithmId Algorithm ID
     * @param ioField IO field
     * @return Updated algorithm
     */
    Algorithm updateIOField(HyperIoTContext context, long algorithmId, AlgorithmIOField ioField);

}
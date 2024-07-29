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
package it.acsoftware.hyperiot.stormmanager.api;

import it.acsoftware.hyperiot.base.api.HyperIoTBaseApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;

import java.io.IOException;

/**
 * 
 * @author Aristide Cittadino Interface component for StormManagerApi. This interface
 *         defines methods for additional operations.
 *
 */
public interface StormManagerApi extends HyperIoTBaseApi {


    /**
     * Generates and submits a project topology

     * @param context
     * @param projectId
     * @throws IOException
     * @throws HyperIoTUnauthorizedException
     */
    void submitProjectTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException;


    /**
     * Gets status of a topology by project ID.
     *
     * @param context
     * @param projectId ID of the project.
     * @return The TopologyStatus object.
     * @throws IOException
     * @throws HyperIoTUnauthorizedException
     */
    TopologyInfo getTopologyStatus(HyperIoTContext context, long projectId)
            throws IOException, HyperIoTUnauthorizedException;

    /**
     * Activates a topology by project ID.
     *
     * @param context
     * @param projectId ID of the project.
     * @throws IOException
     * @throws HyperIoTUnauthorizedException
     */
    void activateTopology(HyperIoTContext context, long projectId)
            throws IOException, HyperIoTUnauthorizedException;

    /**
     * Deactivates a topology by project ID.
     *
     * @param context
     * @param projectId ID of the project.
     * @throws IOException
     * @throws HyperIoTUnauthorizedException
     */
    void deactivateTopology(HyperIoTContext context, long projectId)
            throws IOException, HyperIoTUnauthorizedException;

    /**
     * Kills a Storm topology by project ID.
     *
     * @param context
     * @param projectId ID of the project.
     * @throws IOException
     * @throws HyperIoTUnauthorizedException
     */
    void killTopology(HyperIoTContext context, long projectId)
            throws IOException, HyperIoTUnauthorizedException;


}
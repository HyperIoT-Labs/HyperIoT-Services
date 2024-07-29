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

import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;

import java.io.IOException;

/**
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 * interface defines methods for additional operations.
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-19 Initial release
 */
public interface StormManagerSystemApi extends HyperIoTBaseSystemApi {

    /**
     * Get the topology name of a device.
     *
     * @param projectId The HProject id
     * @return
     */
    String getTopologyName(long projectId);


    /**
     * Generate topology files of a given project and submit it to the Storm cluster.
     *
     * @param projectId The project id
     * @throws IOException
     */
    void submitProjectTopology(long projectId) throws IOException;


    /**
     * Gets the list of topologies on Storm cluster.
     *
     * @return Topology list.
     */
    String getTopologyList() throws IOException;

    /**
     * Get status of a topology.
     *
     * @param projectId project ID
     * @return The TopologyStatus object.
     * @throws IOException
     */
    TopologyInfo getTopologyStatus(long projectId)
            throws IOException;

    /**
     * Activate a topology.
     *
     * @param topologyName Name of the topology to activate.
     * @throws IOException
     */
    void activateTopology(String topologyName)
            throws IOException;

    /**
     * Deactivate a topology.
     *
     * @param topologyName Name of the topology to deactivate.
     * @throws IOException
     */
    void deactivateTopology(String topologyName)
            throws IOException;

    /**
     * Kills a Storm topology.
     *
     * @param topologyName Name of the topology to kill.
     * @throws IOException
     */
    void killTopology(String topologyName)
            throws IOException;

}

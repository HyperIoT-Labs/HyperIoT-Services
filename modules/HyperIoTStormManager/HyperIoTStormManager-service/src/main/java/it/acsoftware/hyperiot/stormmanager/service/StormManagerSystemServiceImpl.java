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
package it.acsoftware.hyperiot.stormmanager.service;

import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectHBaseConstants;
import it.acsoftware.hyperiot.storm.api.StormClient;
import it.acsoftware.hyperiot.storm.builder.HyperIoTTopologyConfigBuilder;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyConfig;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.generated.TopologySummary;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.util.List;

/**
 * @author Aristide Cittadino Implementation class of the StormManagerSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the storm cluster.
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-19 Initial release
 */
@Component(service = StormManagerSystemApi.class, immediate = true)
public final class StormManagerSystemServiceImpl extends HyperIoTBaseSystemServiceImpl
    implements StormManagerSystemApi {
    public static Logger log = LoggerFactory.getLogger(StormManagerSystemServiceImpl.class);
    public static final String TOPOLOGY_INFO_NOT_FOUND = "NOT FOUND";

    /**
     * Injecting Storm Client
     */
    private StormClient stormClient;

    /**
     *
     */
    private HProjectRepository hProjectRepository;

    private HBaseConnectorSystemApi hBaseConnectorSystemApi;

    /**
     * @param stormClient
     */
    @Reference
    public void setStormClient(StormClient stormClient) {
        this.stormClient = stormClient;
    }

    /**
     * @param hProjectRepository
     */
    @Reference
    protected void setHProjectRepository(HProjectRepository hProjectRepository) {
        this.hProjectRepository = hProjectRepository;
    }

    @Reference
    protected void setHBaseConnectorSystemApi(HBaseConnectorSystemApi hBaseConnectorSystemApi) {
        this.hBaseConnectorSystemApi = hBaseConnectorSystemApi;
    }

    /**
     * @param projectId The HProject id
     * @return
     */
    @Override
    public String getTopologyName(long projectId) {
        return "topology-" + projectId;
    }

    /**
     * @param projectId
     * @param currentTopologyConfigHashcode
     * @return
     * @throws IOException
     */
    public boolean mustResubmitTopology(long projectId, int currentTopologyConfigHashcode) throws IOException {
        TopologyConfig config = HyperIoTTopologyConfigBuilder.getTopologyConfig(projectId, getTopologyName(projectId));
        if (config.hashCode() == currentTopologyConfigHashcode) {
            return false;
        }
        return true;
    }

    /**
     * @param projectId The project id
     * @throws IOException
     */
    @Override
    public void submitProjectTopology(long projectId)
        throws IOException {
        try {
            hProjectRepository.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        TopologyConfig topologyConfig = HyperIoTTopologyConfigBuilder.getTopologyConfig(projectId, getTopologyName(projectId));
        getLog().debug("Submitting topology with props: \n {} and yaml: {}", new Object[]{topologyConfig.properties, topologyConfig.yaml});
        checkHBaseTables(projectId);
        topologyServiceSubmit(topologyConfig);
    }

    private void checkHBaseTables(long projectId) throws IOException {
        final boolean hprojectTableExists =
                hBaseConnectorSystemApi.tableExists(HProjectHBaseConstants.HPROJECT_TABLE_NAME_PREFIX + projectId);
        final boolean timelineTableExists =
                hBaseConnectorSystemApi.tableExists(HProjectHBaseConstants.TIMELINE_TABLE_NAME_PREFIX + projectId);
        final boolean errorTableExists =
                hBaseConnectorSystemApi.tableExists(HProjectHBaseConstants.ERROR_TABLE_NAME_PREFIX + projectId);
        final boolean eventTableExists =
                hBaseConnectorSystemApi.tableExists(HProjectHBaseConstants.EVENT_TABLE_NAME_PREFIX + projectId);
        final boolean alarmTableExists =
                hBaseConnectorSystemApi.tableExists(HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + projectId);
        final boolean alarmEventRuleStateTableExists =
                hBaseConnectorSystemApi.tableExists(HProjectHBaseConstants.EVENT_RULE_STATE_TABLE_NAME_PREFIX + projectId);
        if (!(hprojectTableExists && timelineTableExists && errorTableExists && eventTableExists && alarmTableExists
                 && alarmEventRuleStateTableExists)) {
            final String errorMessage = "One or more HBase tables do not exist";
            getLog().error(errorMessage);
            throw new HyperIoTRuntimeException(errorMessage);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    @Override
    public String getTopologyList() throws IOException {
        try {
            return this.stormClient.getTopologyList().toString();
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    /**
     * @param projectId project ID
     * @return
     * @throws IOException
     */
    @Override
    public TopologyInfo getTopologyStatus(long projectId)
        throws IOException {
        TopologyInfo info = new TopologyInfo();
        long currentTime = System.currentTimeMillis();
        try {
            hProjectRepository.find(projectId, null);
            String topologyName = this.getTopologyName(projectId);
            List<TopologySummary> topologies = this.stormClient.getTopologyList();
            if (topologies.size() > 0) {
                TopologySummary topologySummary = topologies.stream().filter(topology -> topology.get_name().equalsIgnoreCase(topologyName)).findFirst().get();
                if (topologyName != null) {
                    String topologyId = topologySummary.get_id();
                    info.setStatus(topologySummary.get_status());
                    info.setUptimeSecs(topologySummary.get_uptime_secs());
                    info.setMustResubmit(this.mustResubmitTopology(projectId, this.stormClient.getTopologyConfigHashCode(topologySummary)));
                    StormTopology topology = this.stormClient.getTopology(topologyId);
                    if (topology != null) {
                        info.setSpoutsCount(topology.get_spouts().size());
                        info.setBoltsCount(topology.get_bolts().size());
                    }
                }
            } else {
                info.setStatus(TOPOLOGY_INFO_NOT_FOUND);
            }
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            info.setStatus(TOPOLOGY_INFO_NOT_FOUND);
        }
        long now = System.currentTimeMillis();

        if(log.isDebugEnabled()){
            long timeDiff = now - currentTime;
            log.debug(" Storm topology status got in : {}",timeDiff);
        }

        return info;
    }

    /**
     * @param topologyName Name of the topology to activate.
     * @throws IOException
     */
    @Override
    public void activateTopology(String topologyName) throws IOException {
        try {
            this.stormClient.activate(topologyName);
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    /**
     * @param topologyName Name of the topology to deactivate.
     * @throws IOException
     */
    @Override
    public void deactivateTopology(String topologyName) throws IOException {
        try {
            this.stormClient.deactivate(topologyName);
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    /**
     * @param topologyName Name of the topology to kill.
     * @throws IOException
     */
    @Override
    public void killTopology(String topologyName)
        throws IOException {
        try {
            this.stormClient.killTopology(topologyName);
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    /**
     * @param topologyConfig
     */
    private void topologyServiceSubmit(TopologyConfig topologyConfig) {
        try {
            this.stormClient.submitTopology(topologyConfig.properties, topologyConfig.yaml, topologyConfig.hashCode());
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }
}

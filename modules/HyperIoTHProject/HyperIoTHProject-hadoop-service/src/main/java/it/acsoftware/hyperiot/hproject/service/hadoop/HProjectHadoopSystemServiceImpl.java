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

package it.acsoftware.hyperiot.hproject.service.hadoop;

import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;

import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.api.hadoop.HProjectHadoopSystemApi;
import it.acsoftware.hyperiot.hproject.util.hadoop.HProjectHDFSConstants;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectHBaseConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.io.IOException;

@Component(service = HProjectHadoopSystemApi.class,immediate = true)
public class HProjectHadoopSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements HProjectHadoopSystemApi {

    /**
     * Injecting HProject System Api
     */
    private HProjectSystemApi hprojectSystemApi;

    /**
     * Injecting the HProjectRepository to interact with persistence layer
     */
    private HProjectRepository repository;

    /**
     * Injecting HBaseConnectorSystemApi
     */
    private HBaseConnectorSystemApi hBaseConnectorSystemApi;

    /**
     * Injecting HadoopManagerSystemApi
     */
    private HadoopManagerSystemApi hadoopManagerSystemApi;

    /**
     * @param hProjectRepository The current value of HProjectRepository to interact
     *                           with persistence layer
     */
    @Reference
    protected void setRepository(HProjectRepository hProjectRepository) {
        getLog().debug("invoking setRepository, setting: {}", hProjectRepository);
        this.repository = hProjectRepository;
    }

    @Reference
    public void setHadoopManagerSystemApi(HadoopManagerSystemApi hadoopManagerSystemApi) {
        this.hadoopManagerSystemApi = hadoopManagerSystemApi;
    }

    @Reference
    public void setHBaseConnectorSystemApi(HBaseConnectorSystemApi hBaseConnectorSystemApi) {
        this.hBaseConnectorSystemApi = hBaseConnectorSystemApi;
    }

    @Reference
    public void setHprojectSystemApi(HProjectSystemApi hprojectSystemApi) {
        this.hprojectSystemApi = hprojectSystemApi;
    }

    @Override
    public void deleteHadoopData(long projectId) throws IOException {
        try {
            repository.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        // delete all folders on HDFS. Remember: there is one folder for each packet
        for (HPacket hPacket : this.hprojectSystemApi.getProjectTreeViewJson(projectId))
            hadoopManagerSystemApi.deleteFolder(HProjectHDFSConstants.HPACKET_FOLDER_BASE_URL + "/" + hPacket.getId());
        // delete all tables on HBase
        hBaseConnectorSystemApi.disableAndDropTable(HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + projectId);
        hBaseConnectorSystemApi.disableAndDropTable(HProjectHBaseConstants.EVENT_TABLE_NAME_PREFIX + projectId);
        hBaseConnectorSystemApi.disableAndDropTable(HProjectHBaseConstants.HPROJECT_TABLE_NAME_PREFIX + projectId);
        hBaseConnectorSystemApi.disableAndDropTable(HProjectHBaseConstants.ERROR_TABLE_NAME_PREFIX + projectId);
        hBaseConnectorSystemApi.disableAndDropTable(HProjectHBaseConstants.TIMELINE_TABLE_NAME_PREFIX + projectId);
    }
}

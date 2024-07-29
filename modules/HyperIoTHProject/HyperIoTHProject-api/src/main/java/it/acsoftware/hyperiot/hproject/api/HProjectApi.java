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

package it.acsoftware.hyperiot.hproject.api;

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExporter;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ExportProjectDTO;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ImportLogReport;
import it.acsoftware.hyperiot.hproject.model.AutoRegisterChallengeRequest;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.model.HyperIoTTopicType;

import javax.security.auth.x500.X500PrivateCredential;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * @author Aristide Cittadino Interface component for HProjectApi. This interface
 * defines methods for additional operations.
 */
public interface HProjectApi extends HyperIoTBaseEntityApi<HProject> {

    /**
     * Gets the list of project areas
     *
     * @param context   The HyperIoTContext instance
     * @param projectId The project id
     * @return List of areas
     */
    Collection<Area> getAreasList(HyperIoTContext context, long projectId);

    /**
     * @param type
     * @param projectId
     * @return
     */
    List<String> getUserProjectTopics(HyperIoTContext context, HyperIoTTopicType type, long projectId);

    /**
     *
     * @param context
     * @param type
     * @param projectId
     * @return
     */
    List<String> getUserProjectRealtimeTopics(HyperIoTContext context, HyperIoTTopicType type, long projectId);

    /**
     * @param context
     * @param projectId
     * @return
     */
    Collection<HPacket> getProjectTreeViewJson(HyperIoTContext context, long projectId);

    /**
     * Method for autoregister all project elements without being authenticated, but presenting a X509Certificate file
     *
     * @param cipherTextChallenge encrypted text message, same as plainText
     * @param projectId
     * @param packets
     * @return
     */
    boolean autoRegister(String cipherTextChallenge, long projectId, List<HPacket> packets);

    /**
     * Method for creating empty project that will be autoconfigured by a gateway device trough certificate
     *
     * @param project
     * @return
     */
    X500PrivateCredential createEmptyAutoRegisterProject(HProject project, HyperIoTContext ctx);

    /**
     * Creates auto register challenge request
     *
     * @param projectId
     * @return
     */
    public AutoRegisterChallengeRequest createAutoRegisterChallenge(long projectId);



    ExportProjectDTO loadHProjectForExport(HyperIoTContext ctx,long projectId);


    ImportLogReport importHProject (ExportProjectDTO dtoProject, HProject project , HyperIoTContext context);


    HProject updateHProjectOwner(HyperIoTContext ctx , long projectId, long userId);

    /**
     *
     * @param exportFormat
     * @param exportName
     * @param hProjectId
     * @param hPacketId
     * @param prettifyTimestamp
     * @param timestampPattern
     * @param context
     * @return
     */
    HPacketDataExporter exportHPacketData(HPacketFormat exportFormat, String exportName, long hProjectId, long hPacketId, boolean prettifyTimestamp, String timestampPattern, HyperIoTContext context);

    /**
     *
     * @param exportId
     * @param hPacketId
     * @param context
     * @return
     */
    String exportStatus(String exportId,long hPacketId,HyperIoTContext context);

    /**
     *
     * @param exportId
     * @param hPacketId
     * @param context
     */
    void stopExport(String exportId,long hPacketId,HyperIoTContext context);

    /**
     *
     * @param exportId
     * @param hProjectId
     * @param context
     * @return
     */
    InputStream getExportStream(String exportId, long hProjectId, HyperIoTContext context);

    /**
     *
     * @param exportId
     * @param hProjectId
     */
    void finalizeExportDownload(String exportId,long hProjectId,HyperIoTContext context);

    HProject load(long projectId,HyperIoTContext context);
}

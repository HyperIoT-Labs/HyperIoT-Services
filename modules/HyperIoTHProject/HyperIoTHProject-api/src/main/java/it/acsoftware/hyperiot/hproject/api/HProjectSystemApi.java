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
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ImportLogReport;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.ExportProjectDTO;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.model.HyperIoTTopicType;

import javax.security.auth.x500.X500PrivateCredential;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 * interface defines methods for additional operations.
 */
public interface HProjectSystemApi extends HyperIoTBaseEntitySystemApi<HProject> {


    /**
     * Gets the list of project areas
     *
     * @param projectId The project id
     * @return List of areas
     */
    Collection<Area> getAreasList(long projectId);

    /**
     * Gets the list of all streaming topics related to the project with id `projectId`
     *
     * @param type      Type filter
     * @param projectId The project id
     * @return List of topics names
     */
    List<String> getUserProjectTopics(HyperIoTTopicType type, long projectId);


    /**
     * @param type
     * @param projectId
     * @return
     */
    default List<String> getUserRealtimeProjectTopics(HyperIoTTopicType type, long projectId) {
        ArrayList<String> topics = new ArrayList<>();
        topics.addAll(this.getDeviceRealtimeTopics(type, projectId));
        return topics;
    }

    /**
     * Gets the list of streaming topics related to the given device
     *
     * @param type      Type filter
     * @param projectId The project id
     * @return List of topics names
     */

    default List<String> getDeviceTopics(HyperIoTTopicType type, long projectId, HDevice d) {
        ArrayList<String> topics = new ArrayList<>();
        //Adding realtime topics
        topics.addAll(this.getDeviceRealtimeTopics(type, projectId, d));
        return topics;
    }

    /**
     * @param type
     * @param projectId
     * @return
     */
    default List<String> getDeviceTopics(HyperIoTTopicType type, long projectId) {
        ArrayList<String> topics = new ArrayList<>();
        //Adding realtime topics
        topics.addAll(this.getDeviceRealtimeTopics(type, projectId));
        return topics;
    }

    /**
     * @param type
     * @param projectId
     * @return
     */
    default List<String> getDeviceRealtimeTopics(HyperIoTTopicType type, long projectId) {
        HProject hProject = this.find(projectId, null);
        return hProject.getDeviceRealtimeTopics(type, hProject.getDevices());
    }

    /**
     * @param type
     * @param projectId
     * @param d
     * @return
     */
    default List<String> getDeviceRealtimeTopics(HyperIoTTopicType type, long projectId, HDevice d) {
        HProject hProject = this.find(projectId, null);
        Collection<HPacket> packets = d.getPackets();
        return hProject.getDeviceRealtimeTopics(type, packets);
    }

    /**
     * Gets the list of streaming write-only topics related to the given device.
     * A device can publish messages only to devices of same project
     *
     * @param type      Type filter
     * @param projectId The project id
     * @param current    The device instance
     * @return List of topics names
     */
    default List<String> getWriteOnlyDeviceTopics(HyperIoTTopicType type, long projectId, HDevice current,
                                                  Collection<HDevice> projectDevices) {
        List<String> topics = new ArrayList<>();
        HProject hProject = this.find(projectId, null);
        return hProject.getWriteOnlyDeviceTopics(type, projectDevices, current);
    }

    /**
     * Get list of
     *
     * @param projectId
     * @return
     */
    Collection<HPacket> getProjectTreeViewJson(long projectId);

    /**
     * Method for autoregister all project elements
     *
     * @param p       new Project
     * @param packets List of packets
     */
    public boolean autoRegister(HProject p, List<HPacket> packets);

    /**
     * Method for creating empty project that will be autoconfigured by a gateway device trough certificate
     *
     * @param project
     * @return
     */
    X500PrivateCredential createEmptyAutoRegisterProject(HProject project, HyperIoTContext ctx);

    /**
     * Creates a challenge string for Gateway and save it to the database
     *
     * @param projectId
     * @return
     */
    public String createAutoRegisterChallenge(long projectId);

    ExportProjectDTO loadHProjectForExport(HProject projectToExport);

    ImportLogReport importHProject (ExportProjectDTO dtoProject , HyperIoTContext context);

    HProject updateHProjectOwner(HyperIoTContext ctx , long projectId, long userId);

    HProject load(long projectId);
    Collection<HProject> load(HyperIoTQuery filter);
}

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

package it.acsoftware.hyperiot.hpacket.api;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.hpacket.model.*;

import javax.ws.rs.core.StreamingOutput;
import java.util.Collection;
import java.util.List;

/**
 * @author Aristide Cittadino Interface component for HPacketApi. This interface
 * defines methods for additional operations.
 */
public interface HPacketApi extends HyperIoTBaseEntityApi<HPacket> {
    /**
     * Gets the list of packets defined for the device with id `deviceId`
     *
     * @param context  The HyperIoTContext instance
     * @param deviceId The device id
     * @return List of packets
     */
    Collection<HPacket> getPacketsList(HyperIoTContext context, long deviceId);

    /**
     * Gets the list of all packets defined for the project with id `projectId`
     *
     * @param context The HyperIoTContext instance
     * @param projectId The project id
     * @return List of packets
     */
    Collection<HPacket> getProjectPacketsList(HyperIoTContext context, long projectId);

    /**
     * Get the list of all project packets
     * @param context The HyperIoTContext instance
     * @param projectId
     * @return List of all packets
     */
    Collection<HPacket> getProjectPacketsTree(HyperIoTContext context, long projectId);

    /**
     * Return packets of a specific project filtered by type
     * @param projectId
     * @param types
     * @return
     */
    Collection<HPacket> getProjectPacketsListByType(HyperIoTContext context,long projectId, List<HPacketType> types);

    /**
     * @param contenxt
     * @param field
     */
    HPacketField updateHPacketField(HyperIoTContext contenxt, HPacketField field);

    /**
     * @param context
     * @param field
     */
    HPacketField addHPacketField(HyperIoTContext context, HPacketField field);

    /**
     * @param context
     * @param packetId
     * @return
     */
    Collection<HPacketField> getHPacketFieldsTree(HyperIoTContext context,long packetId);

    /**
     *
     * @param fieldId
     */
    void removeHPacketField(HyperIoTContext context, long fieldId,long packetId);

    /**
     *
     * @param context
     * @param hpacketFieldId
     * @return
     */
    HPacket findHPacketByHpacketFieldId(HyperIoTContext context, long hpacketFieldId);

}

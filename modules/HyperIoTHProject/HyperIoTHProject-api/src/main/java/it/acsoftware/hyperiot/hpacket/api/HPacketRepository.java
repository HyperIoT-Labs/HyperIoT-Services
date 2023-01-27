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

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseRepository;
import it.acsoftware.hyperiot.hpacket.model.HPacket;

import java.util.Collection;

/**
 * 
 * @author Aristide Cittadino Interface component for HPacket Repository.
 *         It is used for CRUD operations,
 *         and to interact with the persistence layer.
 *
 */
public interface HPacketRepository extends HyperIoTBaseRepository<HPacket> {

    Collection<HPacket> getProjectPacketsTree(long projectId) ;

    Collection<HPacket> getProjectPacketsList(long projectId);

    Collection<HPacket> getPacketsList(long deviceId);

    Collection<HPacket> findByDeviceId(long deviceId);

	
}

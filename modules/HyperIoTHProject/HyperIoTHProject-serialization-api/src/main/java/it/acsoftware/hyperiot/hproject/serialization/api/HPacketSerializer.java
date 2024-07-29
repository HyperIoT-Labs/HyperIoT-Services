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

package it.acsoftware.hyperiot.hproject.serialization.api;

import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketSerializationConfiguration;

import java.io.IOException;
import java.util.Set;

public interface HPacketSerializer {

    /**
     *
     */
    default void defineHPacketSchema(HPacket hPacketDefinition) {
        return;
    }

    /**
     *
     * @param configuration
     */
    default void defineConfiguration(HPacketSerializationConfiguration configuration){
        return;
    }

    /**
     * Serializes HPacket
     *
     * @param hPacket
     * @return
     * @throws IOException
     */
    byte[] serialize(HPacket hPacket) throws IOException;

    /**
     * Serializes only packet fields no meta information.
     * It can be used for output packets going outside HyperIoT World
     *
     * @param hPacket
     * @return
     * @throws IOException
     */
    byte[] serializeRaw(HPacket hPacket) throws IOException;

}

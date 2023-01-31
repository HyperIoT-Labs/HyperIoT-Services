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

package it.acsoftware.hyperiot.hproject.serialization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Author Aristide Cittadino
 * writing HPacket as XML
 */
public class XmlHPacketSerializer implements HPacketSerializer {
    private static XmlHPacketSerializer instance;
    private XmlMapper xmlMapper;
    private ObjectMapper objectMapper;

    private XmlHPacketSerializer() {
        xmlMapper = new XmlMapper();
        objectMapper = new ObjectMapper();
    }

    public static synchronized XmlHPacketSerializer getInstance() {
        if (instance == null)
            instance = new XmlHPacketSerializer();
        return instance;
    }

    @Override
    public byte[] serialize(HPacket hPacket) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        xmlMapper.writerFor(HPacket.class).writeValue(outputStream, hPacket);
        return outputStream.toByteArray();
    }

    @Override
    public byte[] serializeRaw(HPacket hPacket) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        xmlMapper.writerFor(HashMap.class).withRootName("Root").writeValue(outputStream, hPacket.getFlatFieldsMapWithValues());
        return outputStream.toByteArray();
    }

}

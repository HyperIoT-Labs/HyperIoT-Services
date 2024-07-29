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

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Author Aristide Cittadino
 * Class implementing JSON HPacket serializer
 */
public class JsonHPacketSerializer implements HPacketSerializer {
    private static JsonHPacketSerializer instance;
    private ObjectMapper mapper;

    private JsonHPacketSerializer() {

        this.mapper = new ObjectMapper();
    }

    public static synchronized JsonHPacketSerializer getInstance() {
        if (instance == null)
            instance = new JsonHPacketSerializer();
        return instance;
    }

    @Override
    public byte[] serialize(HPacket hPacket) throws IOException {
        return mapper.writeValueAsString(hPacket).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] serializeRaw(HPacket hPacket) throws IOException {
        String jsonResult = mapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(hPacket.getFlatFieldsMapWithValues());
        return jsonResult.getBytes(StandardCharsets.UTF_8);
    }
}

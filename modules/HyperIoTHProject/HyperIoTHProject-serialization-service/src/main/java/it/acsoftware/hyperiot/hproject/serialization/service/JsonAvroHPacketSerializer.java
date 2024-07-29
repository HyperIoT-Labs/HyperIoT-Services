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

import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class JsonAvroHPacketSerializer implements HPacketSerializer {

    private static JsonAvroHPacketSerializer instance;

    private static final Logger log = LoggerFactory.getLogger(JsonAvroHPacketSerializer.class);

    public static synchronized JsonAvroHPacketSerializer getInstance() {
        if (instance == null)
            instance = new JsonAvroHPacketSerializer();
        return instance;
    }

    @Override
    public byte[] serialize(HPacket hPacket) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(hPacket.getSchema(), byteArrayOutputStream);
            DatumWriter<HPacket> datumWriter = new SpecificDatumWriter<>(hPacket.getSchema());
            datumWriter.write(hPacket, jsonEncoder);
            jsonEncoder.flush();
            return byteArrayOutputStream.toByteArray();
        }
    }

    @Override
    public byte[] serializeRaw(HPacket hPacket) throws IOException {
        throw new UnsupportedOperationException();
    }

}

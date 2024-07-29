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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Author Giacomo Spanò
 * Class implementing Avro HPacket Serializer
 */
public final class AvroHPacketSerializer extends Serializer<HPacket> implements HPacketSerializer {

    private static AvroHPacketSerializer instance;

    private static final Logger log = LoggerFactory.getLogger(AvroHPacketSerializer.class);

    public static synchronized AvroHPacketSerializer getInstance() {
        if (instance == null)
            instance = new AvroHPacketSerializer();
        return instance;
    }

    @Override
    public HPacket read(Kryo kryo, Input input, Class type) {
        log.debug("Starting deserialization to HPacket instance");
        byte[] value = input.getBuffer();
        DatumReader<HPacket> reader = new SpecificDatumReader<>(new HPacket().getSchema());
        HPacket hPacket = null;
        try {
            hPacket = reader.read(null, DecoderFactory.get().binaryDecoder(value, null));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return hPacket;
    }

    @Override
    public byte[] serialize(HPacket hPacket) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            log.debug("Starting serialization to HPacket instance");
            DatumWriter<HPacket> datumWriter = new SpecificDatumWriter<>(hPacket.getSchema());
            Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            datumWriter.write(hPacket,  encoder);
            encoder.flush();
            return outputStream.toByteArray();
        }
    }

    @Override
    public byte[] serializeRaw(HPacket hPacket) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Kryo kryo, Output output, HPacket hPacket) {
        log.debug("Starting serialization of HPacket instance {}", hPacket);
        DatumWriter<HPacket> writer = new SpecificDatumWriter<>(hPacket.getSchema());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        try {
            writer.write(hPacket, encoder);
            encoder.flush();
            byte[] outBytes = out.toByteArray();
            output.writeInt(outBytes.length, true);
            output.write(outBytes);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

}

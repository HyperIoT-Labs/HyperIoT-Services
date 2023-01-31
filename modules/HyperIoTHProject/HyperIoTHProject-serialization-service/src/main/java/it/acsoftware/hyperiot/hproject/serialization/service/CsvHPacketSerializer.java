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
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Author Aristide Cittadino
 * Serilizes HPacket in CSV Format
 */
public class CsvHPacketSerializer implements HPacketSerializer {
    private static CsvHPacketSerializer instance;

    private ObjectMapper objectMapper;

    private CsvHPacketSerializer() {
        objectMapper = new ObjectMapper();
    }

    public static synchronized CsvHPacketSerializer getInstance() {
        if (instance == null)
            instance = new CsvHPacketSerializer();
        return instance;
    }

    @Override
    public byte[] serialize(HPacket hPacket) throws IOException {
        byte[] jsonData = JsonHPacketSerializer.getInstance().serialize(hPacket);
        return serializeFromJson(jsonData);
    }

    @Override
    public byte[] serializeRaw(HPacket hPacket) throws IOException {
        byte[] jsonData = JsonHPacketSerializer.getInstance().serializeRaw(hPacket);
        return serializeFromJson(jsonData);
    }

    private byte[] serializeFromJson(byte[] jsonData) throws IOException {
        JsonNode jsonTree = objectMapper.readTree(jsonData);
        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        jsonTree.fieldNames().forEachRemaining(fieldName -> {
            csvSchemaBuilder.addColumn(fieldName);
        });
        CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CsvMapper csvMapper = new CsvMapper();
        csvMapper.writerFor(JsonNode.class)
            .with(csvSchema)
            .writeValue(outputStream, jsonTree);
        return outputStream.toByteArray();
    }
}

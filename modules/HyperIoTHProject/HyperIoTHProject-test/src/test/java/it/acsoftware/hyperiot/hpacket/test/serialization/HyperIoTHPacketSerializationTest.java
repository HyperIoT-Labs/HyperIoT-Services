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

package it.acsoftware.hyperiot.hpacket.test.serialization;

import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketInfo;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketSchema;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketTimestamp;
import it.acsoftware.hyperiot.hproject.deserialization.service.JsonAvroHPacketDeserializer;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.serialization.service.AvroHPacketSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Aristide Cittadino Interface component for HPacket System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHPacketSerializationTest extends KarafTestSupport {

    //force global config
    @Override
    public Option[] config() {
        return null;
    }

    @Test
    public void test00_testSingleValueSerialization() throws IOException {
        HPacketField field = new HPacketField();
        field.setName("testValue");
        field.setCategoryIds(new long[]{12, 1});
        field.setTagIds(new long[]{14, 15});
        field.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field.setType(HPacketFieldType.INTEGER);
        field.setValue(new Integer(1));
        HPacket packet = createHPacket(Arrays.asList(field));
        Assert.assertNotNull(packet);
        AvroHPacketSerializer avroHPacketSerializer = new AvroHPacketSerializer();
        byte[] serializedPacket = avroHPacketSerializer.serialize(packet);
        String avroJson = convertToJson(serializedPacket, packet.getJsonSchema());
        Assert.assertNotNull(avroJson);
        HPacket deserialized = JsonAvroHPacketDeserializer.getInstance().deserialize(avroJson.getBytes(StandardCharsets.UTF_8), createHPacketInfo(packet));
        Assert.assertNotNull(deserialized);
        Assert.assertEquals(1, deserialized.getFieldsMap().get("testValue").getValue());
    }

    @Test
    public void test01_testArrayValueSerialization() throws IOException {
        HPacketField field = new HPacketField();
        field.setName("testArray");
        field.setCategoryIds(new long[]{12, 1});
        field.setTagIds(new long[]{14, 15});
        field.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
        field.setType(HPacketFieldType.INTEGER);
        List<Integer> array = new ArrayList<>();
        array.add(1);
        array.add(2);
        array.add(3);
        array.add(4);
        field.setValue(array);
        HPacket packet = createHPacket(Arrays.asList(field));
        Assert.assertNotNull(packet);
        AvroHPacketSerializer avroHPacketSerializer = new AvroHPacketSerializer();
        byte[] serializedPacket = avroHPacketSerializer.serialize(packet);
        String avroJson = convertToJson(serializedPacket, packet.getJsonSchema());
        Assert.assertNotNull(avroJson);
        HPacket deserialized = JsonAvroHPacketDeserializer.getInstance().deserialize(avroJson.getBytes(StandardCharsets.UTF_8), createHPacketInfo(packet));
        Assert.assertNotNull(deserialized);
        Assert.assertEquals(array, deserialized.getFieldsMap().get("testArray").getValue());
    }

    @Test
    public void test02_test2DimensionalMatrixValueSerialization() throws IOException {
        HPacketField field = new HPacketField();
        field.setName("testMatrix");
        field.setCategoryIds(new long[]{12, 1});
        field.setTagIds(new long[]{14, 15});
        field.setMultiplicity(HPacketFieldMultiplicity.MATRIX);
        field.setType(HPacketFieldType.INTEGER);
        List<List<Integer>> matrix = new ArrayList<>();
        matrix.add(Arrays.asList(1, 2, 3, 4));
        matrix.add(Arrays.asList(1, 2, 3, 4));
        matrix.add(Arrays.asList(1, 2, 3, 4));
        matrix.add(Arrays.asList(1, 2, 3, 4));
        field.setValue(matrix);
        HPacket packet = createHPacket(Arrays.asList(field));
        Assert.assertNotNull(packet);
        AvroHPacketSerializer avroHPacketSerializer = new AvroHPacketSerializer();
        byte[] serializedPacket = avroHPacketSerializer.serialize(packet);
        String avroJson = convertToJson(serializedPacket, packet.getJsonSchema());
        Assert.assertNotNull(avroJson);
        HPacket deserialized = JsonAvroHPacketDeserializer.getInstance().deserialize(avroJson.getBytes(StandardCharsets.UTF_8), createHPacketInfo(packet));
        Assert.assertNotNull(deserialized);
        Assert.assertEquals(matrix, deserialized.getFieldsMap().get("testMatrix").getValue());
    }

    @Test
    public void test03_test3DimensionalMatrixValueSerialization() throws IOException {
        HPacketField field = new HPacketField();
        field.setName("testMatrix");
        field.setCategoryIds(new long[]{12, 1});
        field.setTagIds(new long[]{14, 15});
        field.setMultiplicity(HPacketFieldMultiplicity.MATRIX);
        field.setType(HPacketFieldType.INTEGER);
        List<List<List<Integer>>> matrix = new ArrayList<>();
        List<List<Integer>> innerMatrix = new ArrayList<>();
        innerMatrix.add(Arrays.asList(1, 2, 3, 4));
        innerMatrix.add(Arrays.asList(1, 2, 3, 4));
        innerMatrix.add(Arrays.asList(1, 2, 3, 4));
        innerMatrix.add(Arrays.asList(1, 2, 3, 4));
        matrix.add(new ArrayList<>(innerMatrix));
        matrix.add(new ArrayList<>(innerMatrix));
        matrix.add(new ArrayList<>(innerMatrix));
        matrix.add(new ArrayList<>(innerMatrix));
        field.setValue(matrix);
        HPacket packet = createHPacket(Arrays.asList(field));
        Assert.assertNotNull(packet);
        AvroHPacketSerializer avroHPacketSerializer = new AvroHPacketSerializer();
        byte[] serializedPacket = avroHPacketSerializer.serialize(packet);
        String avroJson = convertToJson(serializedPacket, packet.getJsonSchema());
        Assert.assertNotNull(avroJson);
        HPacket deserialized = JsonAvroHPacketDeserializer.getInstance().deserialize(avroJson.getBytes(StandardCharsets.UTF_8), createHPacketInfo(packet));
        Assert.assertNotNull(deserialized);
        Assert.assertEquals(matrix, deserialized.getFieldsMap().get("testMatrix").getValue());
    }

    private HPacket createHPacket(List<HPacketField> fields) {
        HProject project = new HProject();
        project.setId(1);
        project.setName("project");
        HDevice device = new HDevice();
        device.setDeviceName("device");
        device.setId(1);
        device.setProject(project);
        project.setDevices(Arrays.asList(device));
        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setDevice(device);
        hpacket.setId(1);
        hpacket.setSerialization(HPacketSerialization.NONE);
        hpacket.setType(HPacketType.INPUT);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        hpacket.setCategoryIds(new long[]{12, 14});
        hpacket.setTagIds(new long[]{1, 2});
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        fields.forEach(field -> field.setPacket(hpacket));
        hpacket.defineFields(fields);
        device.setPackets(Arrays.asList(hpacket));
        return hpacket;
    }

    private HPacketInfo createHPacketInfo(HPacket packet) {
        HPacketInfo hPacketInfo = new HPacketInfo();
        hPacketInfo.setHPacketId(packet.getId());
        hPacketInfo.setName(packet.getName());
        hPacketInfo.setType(packet.getType().getName());
        hPacketInfo.setTrafficPlan(packet.getTrafficPlan().getName());
        hPacketInfo.setHDeviceId(packet.getDevice().getId());
        hPacketInfo.setHProjectId(packet.getDevice().getProject().getId());
        HPacketSchema schema = new HPacketSchema();
        schema.setFields(packet.getFlatFieldsMapWithValues());
        schema.setType(packet.getType().getName());
        hPacketInfo.setSchema(schema);
        HPacketTimestamp timestamp = new HPacketTimestamp();
        timestamp.setFormat("DDMMYYYY");
        timestamp.setField("timestmap");
        timestamp.setCreateDefaultIfNotExists(true);
        hPacketInfo.setTimestamp(timestamp);
        hPacketInfo.setUnixTimestamp(true);
        hPacketInfo.setUnixTimestampFormatSeconds(false);
        return hPacketInfo;
    }

    private String convertToJson(byte[] serializedData, String schemaStr) throws IOException {
        Schema schema = new Schema.Parser().parse(schemaStr);
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().binaryDecoder(new ByteArrayInputStream(serializedData), null);
        GenericRecord avroObject = reader.read(null, decoder);
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().jsonEncoder(schema, outputStream);
        writer.write(avroObject, encoder);
        encoder.flush();
        String jsonString = new String(outputStream.toByteArray());
        return jsonString;
    }
}

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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.hpacket.model.HPacketSerializationConfiguration;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Author Aristide Cittadino
 * Serilizes HPacket in CSV Format
 */
public class CsvHPacketSerializer implements HPacketSerializer {
    private static final String TIMESTAMP_DEFAULT_NAME = "timestamp-default";
    private ObjectMapper csvMapper;
    private HPacketSerializationConfiguration configuration;
    private ObjectWriter objectWriterWithHeader;
    private ObjectWriter objectWriterWithoutHeader;
    private CsvSchema.Builder csvSchemaBuilder;
    private Set<Long> fieldIds;
    private HPacket hPacketDefinition;
    private boolean writeHeader = true;

    private CsvHPacketSerializer() {
        csvMapper = new CsvMapper();
    }

    public static synchronized CsvHPacketSerializer getNewInstance() {
        return new CsvHPacketSerializer();
    }

    @Override
    public void defineHPacketSchema(HPacket hPacketDefinition) {
        this.fieldIds = new HashSet<>();
        this.hPacketDefinition = hPacketDefinition;
        this.csvSchemaBuilder = initCsvSchemaBuilder(hPacketDefinition);
        this.objectWriterWithHeader = csvMapper.writerFor(Map.class).with(this.csvSchemaBuilder.build().withHeader());
        this.objectWriterWithoutHeader = csvMapper.writerFor(Map.class).with(this.csvSchemaBuilder.build().withoutHeader());
    }

    @Override
    public void defineConfiguration(HPacketSerializationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public byte[] serialize(HPacket hPacket) throws IOException {
        byte[] serialized = serializeRaw(hPacket);
        writeHeader = false;
        return serialized;
    }

    @Override
    public byte[] serializeRaw(HPacket hPacket) throws IOException {
        ObjectWriter objectWriter = null;
        if(writeHeader)
            objectWriter = this.objectWriterWithHeader;
        else
            objectWriter = this.objectWriterWithoutHeader;
        Map<String, HPacketField> fieldsMap = hPacket.getFieldsMap();
        Map<String, Object> fieldValues = new HashMap<>();
        Map<String, Object> flatPacketFields = hPacket.getFlatFieldsMapWithValues();
        flatPacketFields.keySet().forEach(columnName -> {
            //Exporting field with defined columnIds which are the current defined ones
            if (columnName.equalsIgnoreCase(hPacketDefinition.getTimestampField()) || columnName.equalsIgnoreCase(TIMESTAMP_DEFAULT_NAME) || this.fieldIds.contains(fieldsMap.get(columnName).getId())) {
                Object value = flatPacketFields.get(columnName);
                //chekcks if the value should be converted in other formats
                value = convertValue(columnName, hPacket, fieldsMap, value);
                //loading inner fields
                fieldValues.put(columnName, value);
            }
        });
        byte[] serialized = objectWriter.writeValueAsBytes(fieldValues);
        return serialized;
    }

    private CsvSchema.Builder initCsvSchemaBuilder(HPacket hPacketDefinition) {
        CsvSchema.Builder csvSchemaBuilder = CsvSchema.builder();
        Map<String, HPacketField> fields = hPacketDefinition.getFieldsMap();
        //since field names can change in time this method preserve the logic to retrieve correct field even if
        //its name has changed in time
        fields.keySet().iterator().forEachRemaining(fieldName -> {
            HPacketField field = fields.get(fieldName);
            fieldIds.add(field.getId());
            csvSchemaBuilder.addColumn(field.getName(), getFieldColumType(field.getType()));
        });

        if (configuration.isExportTimestampField()) {
            csvSchemaBuilder.addColumn(hPacketDefinition.getTimestampField(), getFieldColumType(HPacketFieldType.TIMESTAMP));
            //adding also the timestamp-default column added by the system if no timestamp is present
            csvSchemaBuilder.addColumn(TIMESTAMP_DEFAULT_NAME, getFieldColumType(HPacketFieldType.TIMESTAMP));
        }

        return csvSchemaBuilder;
    }

    private CsvSchema.ColumnType getFieldColumType(HPacketFieldType type) {
        switch (type) {
            case BOOLEAN:
                return CsvSchema.ColumnType.BOOLEAN;
            default:
                return CsvSchema.ColumnType.NUMBER_OR_STRING;
        }
    }

    private Object convertValue(String columnName, HPacket hPacket, Map<String, HPacketField> fieldsMap, Object value) {
        //checking if field represents a date and convert it
        boolean isTimestampField = columnName.equals(hPacket.getTimestampField()) || fieldsMap.get(columnName).getType().equals(HPacketFieldType.TIMESTAMP);
        boolean isDateField = fieldsMap.get(columnName).getType().equals(HPacketFieldType.DATE);
        long dateTimeValue = -1;
        if (isTimestampField) {
            dateTimeValue = (Long) value;
        } else if (isDateField) {
            dateTimeValue = ((Date) value).getTime();
        }
        if (dateTimeValue > 0) {
            if (configuration.isPrettyTimestamp())
                value = configuration.getDateTimeFormatter().format(LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTimeValue), ZoneOffset.UTC));
            else
                value = dateTimeValue;
        } else if (fieldsMap.get(columnName).getType().equals(HPacketFieldType.TEXT)) {
            value = value.toString();
        }
        return value;
    }
}

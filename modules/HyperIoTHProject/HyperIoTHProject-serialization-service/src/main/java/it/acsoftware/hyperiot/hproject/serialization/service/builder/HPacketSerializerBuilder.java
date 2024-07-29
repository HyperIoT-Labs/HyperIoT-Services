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

package it.acsoftware.hyperiot.hproject.serialization.service.builder;

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hpacket.model.HPacketSerializationConfiguration;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.CsvHPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.JsonHPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.PlainTextHPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.XmlHPacketSerializer;

import java.time.format.DateTimeFormatter;

/**
 * Author Aristide Cittadino
 * returns correct serializer based on packet format
 */
public class HPacketSerializerBuilder {
    private HPacketSerializationConfiguration configuration;
    private HPacket hPacketDefinition;

    private HPacketSerializerBuilder() {
        configuration = new HPacketSerializationConfiguration();
    }

    public HPacketSerializerBuilder withFieldIds(boolean useFieldIds) {
        this.configuration.setUseFieldIds(useFieldIds);
        return this;
    }

    public HPacketSerializerBuilder withPrettyTimestamp(boolean prettyTimestamp) {
        this.configuration.setPrettyTimestamp(prettyTimestamp);
        return this;
    }

    public HPacketSerializerBuilder withDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        withPrettyTimestamp(true);
        this.configuration.setDateTimeFormatter(dateTimeFormatter);
        return this;
    }

    public HPacketSerializerBuilder withFormat(HPacketFormat format) {
        this.configuration.setFormat(format);
        return this;
    }

    public HPacketSerializerBuilder withTimestampField(boolean exportTimestampField) {
        this.configuration.setExportTimestampField(exportTimestampField);
        return this;
    }

    public HPacketSerializerBuilder withHPacketDefinition(HPacket hPacketDefinition) {
        this.hPacketDefinition = hPacketDefinition;
        return this;
    }

    public HPacketSerializer build() {
        if (this.configuration == null || this.configuration.getFormat() == null)
            throw new IllegalStateException("Format is required");
        if (this.hPacketDefinition == null)
            throw new IllegalStateException("Please set the hpacket definition in order to export data");

        HPacketSerializer serializer = getHPacketSerializer(this.configuration.getFormat());
        serializer.defineConfiguration(configuration);
        serializer.defineHPacketSchema(hPacketDefinition);
        return serializer;
    }

    public static HPacketSerializerBuilder newBuilder() {
        return new HPacketSerializerBuilder();
    }

    public static HPacketSerializer getHPacketSerializer(HPacket packet) {
        return getHPacketSerializer(packet.getFormat());
    }

    public static HPacketSerializer getHPacketSerializer(HPacketFormat format) {
        switch (format) {
            case CSV:
                //forcing new instance due to specific configuration on each serialization
                return CsvHPacketSerializer.getNewInstance();
            case JSON:
                return JsonHPacketSerializer.getInstance();
            case XML:
                XmlHPacketSerializer.getInstance();
            case TEXT:
                PlainTextHPacketSerializer.getInstance();
            default:
                throw new HyperIoTRuntimeException("HPacket Serializer not found");
        }

    }

}

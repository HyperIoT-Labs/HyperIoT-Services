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
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.CsvHPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.JsonHPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.PlainTextHPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.XmlHPacketSerializer;

import java.util.EnumMap;
import java.util.Map;

/**
 * Author Aristide Cittadino
 * returns correct serializer based on packet format
 */
public class HPacketSerializerBuilder {

    private static final Map<HPacketFormat, HPacketSerializer> serializerMap;

    static {
        serializerMap = new EnumMap<>(HPacketFormat.class);
        serializerMap.put(HPacketFormat.CSV, CsvHPacketSerializer.getInstance());
        serializerMap.put(HPacketFormat.JSON, JsonHPacketSerializer.getInstance());
        serializerMap.put(HPacketFormat.XML, XmlHPacketSerializer.getInstance());
        serializerMap.put(HPacketFormat.TEXT, PlainTextHPacketSerializer.getInstance());
    }

    public static HPacketSerializer getHPacketSerializer(HPacket packet) {
        return getHPacketSerializer(packet.getFormat());
    }

    public static HPacketSerializer getHPacketSerializer(HPacketFormat format) {
        if (!serializerMap.containsKey(format))
            throw new HyperIoTRuntimeException("HPacket Serializer not found");
        return serializerMap.get(format);
    }

}

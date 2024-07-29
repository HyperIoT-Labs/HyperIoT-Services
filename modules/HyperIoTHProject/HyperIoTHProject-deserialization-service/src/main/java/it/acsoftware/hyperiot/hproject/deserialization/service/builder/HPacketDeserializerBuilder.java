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

package it.acsoftware.hyperiot.hproject.deserialization.service.builder;

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.service.CsvHPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.service.JsonHPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.service.XmlHPacketDeserializer;

import java.util.EnumMap;
import java.util.Map;

public final class HPacketDeserializerBuilder {

    private static final Map<HPacketFormat, HPacketDeserializer> deserializerMap;

    static {
        deserializerMap = new EnumMap<>(HPacketFormat.class);
        deserializerMap.put(HPacketFormat.CSV, CsvHPacketDeserializer.getInstance());
        deserializerMap.put(HPacketFormat.JSON, JsonHPacketDeserializer.getInstance());
        deserializerMap.put(HPacketFormat.XML, XmlHPacketDeserializer.getInstance());
    }

    private HPacketDeserializerBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static HPacketDeserializer getDeserializer(HPacketFormat format) {
        if (!deserializerMap.containsKey(format))
            throw new HyperIoTRuntimeException("Deserializer not found");
        return deserializerMap.get(format);
    }

}

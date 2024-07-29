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

package it.acsoftware.hyperiot.hproject.deserialization.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketInfo;
import it.acsoftware.hyperiot.hproject.deserialization.service.util.HPacketDeserializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

public class JsonHPacketDeserializer implements HPacketDeserializer {

    private static JsonHPacketDeserializer instance;

    private static final Logger log = LoggerFactory.getLogger(JsonHPacketDeserializer.class);

    private final ObjectMapper objectMapper;

    private JsonHPacketDeserializer() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static synchronized JsonHPacketDeserializer getInstance() {
        if (instance == null)
            instance = new JsonHPacketDeserializer();
        return instance;
    }

    @Override
    public HPacket deserialize(byte[] rawHPacket, HPacketInfo hPacketInfo) throws IOException {
        HashMap<String, Object> message;
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        // example input JSON packet
        /*
        {
          "id": "Multisensor 3", // this field is not used
          "humidity": 33.75,
          "gps.longitude": 122.04,
          "gps.latitude": 85.55,
          "temperature": 30.28
        }
        NOTE: also nested fields are allowed eg:
        {
          …
          "gps": {
            "longitude": 122.04,
            "latitude": 85.55
          }
          …
        }
        */
        message = objectMapper.readValue(rawHPacket, typeRef);
        log.debug("JSON Data : {}", message);
        // create and return the HPacket
        return HPacketDeserializerUtil.createHPacket(hPacketInfo, message);
    }

}

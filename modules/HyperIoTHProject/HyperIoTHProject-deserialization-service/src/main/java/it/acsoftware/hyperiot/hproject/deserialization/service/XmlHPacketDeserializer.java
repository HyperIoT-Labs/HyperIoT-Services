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
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketInfo;
import it.acsoftware.hyperiot.hproject.deserialization.service.util.HPacketDeserializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

public class XmlHPacketDeserializer implements HPacketDeserializer {

    private static XmlHPacketDeserializer instance;

    private static final Logger log = LoggerFactory.getLogger(XmlHPacketDeserializer.class);

    private XmlHPacketDeserializer() {
    }

    public static synchronized XmlHPacketDeserializer getInstance() {
        if (instance == null)
            instance = new XmlHPacketDeserializer();
        return instance;
    }

    @Override
    public HPacket deserialize(byte[] rawHPacket, HPacketInfo hPacketInfo) throws IOException {
        HashMap<String, Object> message;
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        // example input XML packet
        /*
        <?xml version="1.0" encoding="UTF-8"?>
        <fields>
           <id>Multisensor 2</id> <!-- this field is not used -->
           <gps>
               <latitude>29.42</latitude>
               <longitude>32.15</longitude>
           </gps>
           <temperature>19.25</temperature>
           <humidity>43.11</humidity>
        </fields>
        NOTE: also dotted fields notation is allowed eg:
        <fields>
          …
          <gps.latitude>29.42</gps.latitude>
          <gps.longitude>32.15</gps.longitude>
          …
        </fields>
        */
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.setDefaultUseWrapper(false);
        message = xmlMapper.readValue(rawHPacket, typeRef);
        log.debug("XML Data : {}", message);
        // create and return the HPacket
        return HPacketDeserializerUtil.createHPacket(hPacketInfo, message);
    }

}

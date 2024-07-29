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

import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketInfo;
import it.acsoftware.hyperiot.hproject.deserialization.service.util.HPacketDeserializerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsvHPacketDeserializer implements HPacketDeserializer {

    private static CsvHPacketDeserializer instance;

    private static final Logger log = LoggerFactory.getLogger(CsvHPacketDeserializer.class);

    private CsvHPacketDeserializer() {
    }

    public static synchronized CsvHPacketDeserializer getInstance() {
        if (instance == null)
            instance = new CsvHPacketDeserializer();
        return instance;
    }

    @Override
    public HPacket deserialize(byte[] rawHPacket, HPacketInfo hPacketInfo) throws IOException {
        HashMap<String, Object> message;
        // example CSV input packet
        /*
        "id","Multisensor 2","temperature",22.34,humidity,56.44,"gps.latitude",45.23,"gps.longitude",87.23
         */
        message = new HashMap<>();
        Pattern pattern = Pattern.compile("(\"(?:[^\"]|\"\")*\"|[^,\"\\n\\r]*)(,|\\r?\\n|\\r|$)");
        Matcher matcher = pattern.matcher(new String(rawHPacket));
        String match;
        String currentField = null;
        while (matcher.find()) {
            match = matcher.group(1);
            if (match != null) {
                match = match.trim().replaceAll("^\"|\"$", "").trim();
                if (currentField != null) {
                    message.put(currentField, match);
                    currentField = null;
                } else if (hPacketInfo.getSchema().getFields().containsKey(match)) {
                    currentField = match;
                }
            }
        }
        log.debug("CSV Data : {}", message);
        return HPacketDeserializerUtil.createHPacket(hPacketInfo, message);
    }

}

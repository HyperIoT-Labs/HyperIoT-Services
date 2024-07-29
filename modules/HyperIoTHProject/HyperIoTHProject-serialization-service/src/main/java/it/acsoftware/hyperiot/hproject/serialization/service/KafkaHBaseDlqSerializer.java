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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class KafkaHBaseDlqSerializer implements Serializer<HashMap<String,String>> {

    private static final Logger log = LoggerFactory.getLogger(KafkaHBaseDlqSerializer.class);

    private static final ObjectMapper jsonMapper= new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        log.debug("configure {} {}", configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, HashMap<String, String> data) {
        try {
            return jsonMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            log.debug("**********KafkaHBaseDlqSerializer Exception cannot serialize data");
            throw new SerializationException(String.format("Cannot serialize data '%s' for topic '%s'", data, topic), e);
        }
    }

    @Override
    public void close() {
        log.debug("closed");
    }
}

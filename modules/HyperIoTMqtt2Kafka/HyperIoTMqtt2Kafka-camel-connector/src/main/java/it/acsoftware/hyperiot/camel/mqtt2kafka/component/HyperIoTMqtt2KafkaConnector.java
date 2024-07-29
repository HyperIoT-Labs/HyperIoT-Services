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

package it.acsoftware.hyperiot.camel.mqtt2kafka.component;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

import java.util.Map;

/**
 * @Author Aristide Cittadino
 * Camel2KafkaConnector
 */
public class HyperIoTMqtt2KafkaConnector extends DefaultComponent {
    public static final String HYPERIOT_CAMEL_MQTT_2_KAFKA_COMPONENT_NAME = "HyperIoTMqtt2Kafka";

    protected Endpoint createEndpoint(String uri, String remaining,
                                      Map<String, Object> parameters) throws Exception {
        HyperIoTMqtt2KafkaEndpoint endpoint = new HyperIoTMqtt2KafkaEndpoint(uri, remaining,
            this);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}

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

package it.acsoftware.hyperiot.camel.mqtt2kafka;

import it.acsoftware.hyperiot.camel.mqtt2kafka.component.HyperIoTMqtt2KafkaConnector;
import it.acsoftware.hyperiot.camel.mqtt2kafka.component.util.HyperIoTMqtt2KafkaConstants;
import it.acsoftware.hyperiot.camel.mqtt2kafka.component.util.HyperIoTMqtt2KafkaUtil;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Aristide Cittadino
 * This class implements camel routing from mqtt topics to kafka.
 */
public class HyperIoTMqttJMS2KafkaRouteBuilder extends RouteBuilder {
    private Logger logger = LoggerFactory.getLogger(HyperIoTMqttJMS2KafkaRouteBuilder.class);
    public static final String HYPERIOT_MQTT_TO_JMS_ID = "HyperIoTMQTT2JMS";

    @Override
    public void configure() throws Exception {
        log.info("HyperIoT Camel :Configuring ActiveMQ 2 Kafka Route");
        StringBuilder options = new StringBuilder();
        options.append("?")
            .append("concurrentConsumers=").append(HyperIoTMqtt2KafkaUtil.getConcurrentConsumers())
            .append("&")
            .append("maxConcurrentConsumers=").append(HyperIoTMqtt2KafkaUtil.getMaxConcurrentConsumers())
            .append("&")
            .append("clientId=").append(HyperIoTMqtt2KafkaConstants.HYPERIOT_MQTT_2_KAFKA_CLIENT_ID);
        String topicPattern = "activemq:topic:"+ HyperIoTMqtt2KafkaConstants.JMS_TOPIC_MQTT_TOPIC_PATTERN+options.toString();
        logger.info("Configuring Mqtt JMS 2 Kafka Route Builder with pattern {} and options: {}",topicPattern,options.toString());
        from(topicPattern)
            .autoStartup(false)
            .routeId(HYPERIOT_MQTT_TO_JMS_ID)
            .to(HyperIoTMqtt2KafkaConnector.HYPERIOT_CAMEL_MQTT_2_KAFKA_COMPONENT_NAME);
    }
}

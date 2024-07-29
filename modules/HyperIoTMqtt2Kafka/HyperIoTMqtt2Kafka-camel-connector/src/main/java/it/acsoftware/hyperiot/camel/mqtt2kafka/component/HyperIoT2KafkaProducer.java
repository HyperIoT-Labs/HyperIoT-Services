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

import it.acsoftware.hyperiot.camel.mqtt2kafka.component.util.HyperIoTMqtt2KafkaConstants;
import it.acsoftware.hyperiot.camel.mqtt2kafka.component.util.HyperIoTMqtt2KafkaUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * @Author Aristide Cittadino
 * HyperIoT2KafkaProducer Producer
 */
public class HyperIoT2KafkaProducer extends DefaultProducer {
    private static final transient Logger logger = LoggerFactory
        .getLogger(HyperIoT2KafkaProducer.class);

    private HyperIoTMqtt2KafkaEndpoint endpoint;
    private KafkaProducer<byte[], byte[]> producer;

    public HyperIoT2KafkaProducer(HyperIoTMqtt2KafkaEndpoint endpoint, KafkaProducer<byte[], byte[]> producer) {
        super(endpoint);
        this.endpoint = endpoint;
        this.producer = producer;
    }

    /**
     * This method converts inbound topic to kafka message
     *
     * @param exchange
     * @throws Exception
     */
    public void process(Exchange exchange) throws Exception {
        KafkaConnectorSystemApi connectorSystemApi = HyperIoTMqtt2KafkaUtil.getKafkaConnectorSystemApi();
        if (connectorSystemApi != null) {
            Map<String, Object> properties = exchange.getIn().getHeaders();
            printHeaderParams(properties);
            ActiveMQTopic activeMQTopic = (ActiveMQTopic) properties.get(HyperIoTMqtt2KafkaConstants.PARAMS_JMS_DESTINATION);
            String topic = activeMQTopic.getPhysicalName().replace(HyperIoTMqtt2KafkaConstants.JMS_TOPIC_PREFIX, "");
            //here is mqtt topic with dotted notation instead of "/"
            // now from messageIn we convert data from Mqtt To Kafka Standard
            Message messageIn = (Message) exchange.getIn();
            HyperIoTKafkaMessage message = HyperIoTMqtt2KafkaUtil
                .createKafkaKeyValueFromMqttTopic(topic,
                    messageIn.getBody(byte[].class));
            logger.debug("Converting MQTT Message from topic {}, to kafka topic {} and key {}", topic, message.getTopic(), new String(message.getKey()));
            if (message != null) {
                if (topic != null) {
                    try {
                        connectorSystemApi.produceMessage(message, producer, null);
                    } catch (Throwable t) {
                        logger.error(t.getMessage(), t);
                    }
                }
            }
        }
    }

    private void printHeaderParams(Map<String, Object> properties) {
        if (logger.isDebugEnabled()) {
            logger.debug("HEADER PARAMS");
            Iterator<String> it = properties.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                logger.debug("KEY: " + key + " = " + properties.get(key));
            }
        }
    }
}

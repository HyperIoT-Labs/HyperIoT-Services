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

package it.acsoftware.hyperiot.camel.mqtt2kafka.component.util;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Author Aristide Cittadino
 * Used to convert mqtt messange and topic to kafka topic
 */
public class HyperIoTMqtt2KafkaUtil {
    private static final transient Logger log = LoggerFactory
        .getLogger(HyperIoTMqtt2KafkaUtil.class);

    private static Properties props;

    private static Properties loadMqtt2KafkaConfiguration() {
        BundleContext context = HyperIoTUtil.getBundleContext(HyperIoTMqtt2KafkaUtil.class);
        log.debug("Mqtt 2 Kafka Properties not cached, reading from .cfg file...");
        if (props == null) {
            ServiceReference<?> configurationAdminReference = context
                .getServiceReference(ConfigurationAdmin.class.getName());

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                    .getService(configurationAdminReference);
                try {
                    Configuration configuration = confAdmin.getConfiguration(
                        HyperIoTMqtt2KafkaConstants.HYPERIOT_MQTT_2_KAFKA_PROP_FILENAME);
                    if (configuration != null && configuration.getProperties() != null) {
                        log.debug("Reading properties for Mqtt Client....");
                        Dictionary<String, Object> dict = configuration.getProperties();
                        List<String> keys = Collections.list(dict.keys());
                        Map<String, Object> dictCopy = keys.stream()
                            .collect(Collectors.toMap(Function.identity(), dict::get));
                        props = new Properties();
                        props.putAll(dictCopy);

                    } else {
                        log.error(
                            "Impossible to find Configuration admin reference, mqtt client won't start!");
                    }
                } catch (IOException e) {
                    log.error(
                        "Impossible to find it.acsoftware.hyperiot.mqtt.client.cfg, please create it!", e);
                }
            }
        }
        return props;
    }

    /**
     * @return
     */
    public static String getMqttBrokerUsername() {
        return loadMqtt2KafkaConfiguration().getProperty(HyperIoTMqtt2KafkaConstants.HYPERIOT_MQTT_2_KAFKA_PROP_USERNAME);
    }

    /**
     * @return
     */
    public static String getMqttBrokerPassword() {
        return loadMqtt2KafkaConfiguration().getProperty(HyperIoTMqtt2KafkaConstants.HYPERIOT_MQTT_2_KAFKA_PROP_PASSWORD);
    }

    /**
     *
     * @return
     */
    public static int getConcurrentConsumers() {
        return Integer.parseInt(loadMqtt2KafkaConfiguration().getProperty(HyperIoTMqtt2KafkaConstants.HYPERIOT_MQTT_2_KAFKA_PROP_DEFAULT_CONCURRENT_CONSUMERS, "1"));
    }

    /**
     *
     * @return
     */
    public static int getMaxConcurrentConsumers() {
        return Integer.parseInt(loadMqtt2KafkaConfiguration().getProperty(HyperIoTMqtt2KafkaConstants.HYPERIOT_MQTT_2_KAFKA_PROP_MAX_CONCURRENT_CONSUMERS, "1"));
    }

    /**
     * topic is in the form <mqttSubscribeTopic>/<projectId>/<deviceId>/<packetId> (eg. "/streaming/10/8/9" where "/streaming/#" is the subscribed topic)
     *
     * @param mqttTopic
     * @param messageIn
     * @return
     */
    public static HyperIoTKafkaMessage createKafkaKeyValueFromMqttTopic(
        String mqttTopic, byte[] messageIn) {
        //removing "VirtualTopic.streaming."
        String topic = mqttTopic.replace(HyperIoTMqtt2KafkaConstants.JMS_VIRTUAL_TOPIC_NAME, "");
        topic = topic.replace(HyperIoTMqtt2KafkaConstants.JMS_HYPERIOT_TOPIC_PREFIX, "");
        String[] path = topic.split("\\.");
        String projectId = path[0];
        String deviceId = path[1];
        String packetId = path[2];
        String kafkaKey = deviceId.concat(".").concat(packetId);
        String kafkaTopic = HyperIoTMqtt2KafkaConstants.KAFKA_TOPIC_PREFIX + projectId;
        byte key[] = kafkaKey.getBytes(StandardCharsets.UTF_8);
        return new HyperIoTKafkaMessage(key, kafkaTopic, Base64.getEncoder().encode(messageIn));
    }

    /**
     * Retrieving Kafka Connector from OSGI Context
     *
     * @return
     */
    public static KafkaConnectorSystemApi getKafkaConnectorSystemApi() {
        try {
            return (KafkaConnectorSystemApi) HyperIoTUtil.getService(KafkaConnectorSystemApi.class);
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        }
        return null;
    }

}

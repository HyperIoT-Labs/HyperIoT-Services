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

/**
 * @Author Aristide Cittadino
 */
public class HyperIoTMqtt2KafkaConstants {
    public static final String HYPERIOT_MQTT_2_KAFKA_PROP_USERNAME = "it.acsoftware.hyperiot.activemq.mqtt.username";
    public static final String HYPERIOT_MQTT_2_KAFKA_PROP_PASSWORD = "it.acsoftware.hyperiot.activemq.mqtt.password";
    public static final String HYPERIOT_MQTT_2_KAFKA_PROP_FILENAME = "it.acsoftware.hyperiot.mqtt2kafka";
    public static final String HYPERIOT_MQTT_2_KAFKA_PROP_DEFAULT_CONCURRENT_CONSUMERS = "it.acsoftware.hyperiot.mqtt2kafka.concurrent.consumers";
    public static final String HYPERIOT_MQTT_2_KAFKA_PROP_MAX_CONCURRENT_CONSUMERS = "it.acsoftware.hyperiot.mqtt2kafka.max.concurrent.consumers";
    public static final String HYPERIOT_MQTT_2_KAFKA_CLIENT_ID = "HyperIoTMqtt2Kafka-Client-"+ HyperIoTUtil.getLayer()+"-"+HyperIoTUtil.getNodeId();


    public static final String PARAMS_JMS_DESTINATION = "JMSDestination";
    public static final String JMS_TOPIC_PROTOCOL = "topic://";
    public static final String JMS_VIRTUAL_TOPIC_NAME = "VirtualTopic.";
    public static final String KAFKA_TOPIC_PREFIX = "streaming.";
    public static final String JMS_HYPERIOT_TOPIC_PREFIX = "streaming.";
    public static final String JMS_TOPIC_PREFIX = JMS_TOPIC_PROTOCOL+JMS_VIRTUAL_TOPIC_NAME;
    //topic pattern is in the form of streaming.<priojectId>.<deviceId>.<packetId>
    public static final String JMS_TOPIC_MQTT_TOPIC_PATTERN = JMS_VIRTUAL_TOPIC_NAME+KAFKA_TOPIC_PREFIX+"*.*.*";
}

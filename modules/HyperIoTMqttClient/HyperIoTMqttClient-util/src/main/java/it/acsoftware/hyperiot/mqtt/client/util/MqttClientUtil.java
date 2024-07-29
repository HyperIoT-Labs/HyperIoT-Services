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

package it.acsoftware.hyperiot.mqtt.client.util;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MqttClientUtil {
    private static Logger log = LoggerFactory.getLogger(MqttClientUtil.class.getName());
    private static Properties props;

    private static Properties loadMqttClientConfiguration() {
        BundleContext context = HyperIoTUtil.getBundleContext(MqttClientUtil.class);
        log.debug( "Mqtt Client Properties not cached, reading from .cfg file...");
        if (props == null) {
            ServiceReference<?> configurationAdminReference = context
                .getServiceReference(ConfigurationAdmin.class.getName());

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                    .getService(configurationAdminReference);
                try {
                    Configuration configuration = confAdmin.getConfiguration(
                        MqttClientConstants.MQTT_CLIENT_CONFIG_FILE_NAME);
                    if (configuration != null && configuration.getProperties() != null) {
                        log.debug( "Reading properties for Mqtt Client....");
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

    public static String getBrokerHost() {
        return loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_BROKER_HOST);
    }

    public static String getBrokerPort() {
        return loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_BROKER_PORT);
    }

    public static boolean getAutomaticReconnect() {
        return Boolean.parseBoolean(loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_AUTOMATIC_RECONNECT));
    }

    public static boolean getCleanSession() {
        return Boolean.parseBoolean(loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_CLEAN_SESSION));
    }

    public static int getConnectionTimeout() {
        return Integer.parseInt(loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_CONNECTION_TIMEOUT));
    }

    public static int getQos() {
        return Integer.parseInt(loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_QOS));
    }

    public static int getKeepAlive() {
        return Integer.parseInt(loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_KEEP_ALIVE));
    }

    public static String getMqttBrokerCompleteAddress() {
        return getBrokerHost() + ":" + getBrokerPort();
    }

    public static String getMqttAdminUsername() {
        return loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_ADMIN_USERNAME);
    }

    public static String getMqttAdminPassword() {
        return loadMqttClientConfiguration().getProperty(MqttClientConstants.MQTT_CLIENT_PROPERTY_ADMIN_PASSWORD);
    }


}

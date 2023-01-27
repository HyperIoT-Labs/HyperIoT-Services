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

package it.acsoftware.hyperiot.mqtt.client.service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.mqtt.client.api.MqttClient;
import it.acsoftware.hyperiot.mqtt.client.api.MqttClientApi;
import it.acsoftware.hyperiot.mqtt.client.model.HyperIoTMqttMessage;
import it.acsoftware.hyperiot.mqtt.client.util.MqttClientConstants;
import it.acsoftware.hyperiot.mqtt.client.util.MqttClientUtil;
import it.acsoftware.hyperiot.websocket.session.HyperIoTWebSocketAbstractSession;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.paho.client.mqttv3.*;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class MqttWebSocketSession extends HyperIoTWebSocketAbstractSession implements IMqttMessageListener, IMqttActionListener {
    private static Logger logger = LoggerFactory.getLogger(MqttWebSocketSession.class.getName());
    private MqttClient client;
    private MqttClientApi mqttClientService;
    private String[] topics;
    private ObjectMapper jsonMapper;

    public MqttWebSocketSession(Session session) {
        super(session, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            this.client.disconnect();
        } catch (MqttException e) {
            logger.error( e.getMessage(), e);
        }
    }

    @Override
    public void close(String s) {
        /*
            TODO implement logic
         */
    }

    @Override
    public void initialize() {
        // HProject System API
        ServiceReference serviceReference = getBundleContext()
            .getServiceReference(MqttClientApi.class);
        mqttClientService = (MqttClientApi) getBundleContext()
            .getService(serviceReference);
        Map<String, List<String>> params = getSession().getUpgradeRequest().getParameterMap();
        String mqttUsername = (params.get(MqttClientConstants.MQTT_CLIENT_USERNAME_PARAM) != null && params.get(MqttClientConstants.MQTT_CLIENT_USERNAME_PARAM).size() == 1) ? params.get(MqttClientConstants.MQTT_CLIENT_USERNAME_PARAM).get(0) : "";
        String mqttPassword = (params.get(MqttClientConstants.MQTT_CLIENT_PASSWORD_PARAM) != null && params.get(MqttClientConstants.MQTT_CLIENT_PASSWORD_PARAM).size() == 1) ? params.get(MqttClientConstants.MQTT_CLIENT_PASSWORD_PARAM).get(0) : "";
        String mqttTopics = (params.get(MqttClientConstants.MQTT_CLIENT_TOPICS_PARAMS) != null && params.get(MqttClientConstants.MQTT_CLIENT_TOPICS_PARAMS).size() == 1) ? params.get(MqttClientConstants.MQTT_CLIENT_TOPICS_PARAMS).get(0) : "";
        //retrieving from osgi context the default JSON Mapper through declarative services
        this.jsonMapper = HyperIoTBaseRestApi.getHyperIoTJsonMapper();
        try {
            this.topics = mqttTopics.split(",");
            //login to mqtt broker with credentials passed via reuqest
            this.client = this.mqttClientService.createMqttClient(MqttClientUtil.getMqttBrokerCompleteAddress(), mqttUsername, mqttPassword, this);
            this.client.connect(this);
        } catch (Exception e) {
            logger.error( e.getMessage(), e);
            try {
                getSession().getRemote().sendString(e.getMessage());
            } catch (Exception e1) {
                logger.error( e1.getMessage(), e);
            }
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            logger.debug( message.getPayload().toString());
            HyperIoTMqttMessage wsMessage = new HyperIoTMqttMessage(topic, new String(message.getPayload()));
            getSession().getRemote().sendString(jsonMapper.writeValueAsString(wsMessage));
        } catch (Exception e) {
            logger.error( e.getMessage(), e);
            getSession().getRemote().sendString(e.getMessage());
        }
    }

    @Override
    public void onMessage(String s) {
        try {
            // TO DO: read topic to publish on from message and check if user can send it
            if (this.client.isConnected()) {
                HyperIoTMqttMessage wsMessage = jsonMapper.readValue(s.getBytes(), HyperIoTMqttMessage.class);
                this.client.publish(wsMessage.getTopic(), 1, false, wsMessage.hashCode(), wsMessage.getMessage().getBytes());
            } else {
                getSession().getRemote().sendString("not connected");
            }
        } catch (Exception e) {
            logger.error( e.getMessage(), e);
            try {
                getSession().getRemote().sendString(e.getMessage());
            } catch (Exception e1) {
                logger.error( e1.getMessage(), e1);
            }
        }
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        try {
            for (String topic : topics) {
                this.client.subscribe(topic, MqttClientUtil.getQos());
                getSession().getRemote().sendString("MQTT Client Connected!");
            }
        } catch (Exception e) {
            try {
                getSession().getRemote().sendString(e.getMessage());
            } catch (Exception e1) {
                logger.error( e1.getMessage(), e1);
            }
        }
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        try {
            getSession().getRemote().sendString(exception.getMessage());
        } catch (Exception e) {
            try {
                getSession().getRemote().sendString(e.getMessage());
            } catch (Exception e1) {
                logger.error( e1.getMessage(), e1);
            }
            logger.error( e.getMessage(), e);
        }
    }
}

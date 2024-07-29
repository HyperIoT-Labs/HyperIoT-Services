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
import it.acsoftware.hyperiot.mqtt.client.util.MqttClientConstants;
import it.acsoftware.hyperiot.mqtt.client.util.MqttClientUtil;
import it.acsoftware.hyperiot.websocket.session.HyperIoTWebSocketAbstractSession;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
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
    private String topic;
    private ObjectMapper jsonMapper;

    public MqttWebSocketSession(Session session) {
        super(session, false);
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            this.client.disconnect();
        } catch (MqttException e) {
            logger.error(e.getMessage(), e);
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
        String mqttUsername = getParameterFromUpgradeRequest(MqttClientConstants.MQTT_CLIENT_USERNAME_PARAM, getSession().getUpgradeRequest());
        String mqttPassword = getParameterFromUpgradeRequest(MqttClientConstants.MQTT_CLIENT_PASSWORD_PARAM, getSession().getUpgradeRequest());
        String mqttTopic = getParameterFromUpgradeRequest(MqttClientConstants.MQTT_CLIENT_TOPIC_PARAM, getSession().getUpgradeRequest());
        //retrieving from osgi context the default JSON Mapper through declarative services
        this.jsonMapper = HyperIoTBaseRestApi.getHyperIoTJsonMapper();
        try {
            this.topic = mqttTopic;
            //login to mqtt broker with credentials passed via reuqest
            this.client = this.mqttClientService.createMqttClient(MqttClientUtil.getMqttBrokerCompleteAddress(), mqttUsername, mqttPassword, this);
            this.client.connect(this);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            try {
                getSession().getRemote().sendString(e.getMessage());
            } catch (Exception e1) {
                logger.error(e1.getMessage(), e);
            }
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            logger.debug(message.getPayload().toString());
            getSession().getRemote().sendString(new String(message.getPayload()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            getSession().getRemote().sendString(e.getMessage());
        }
    }

    @Override
    public void onMessage(String s) {
        try {
            // TODO: read topic to publish on from message and check if user can send it
            if (this.client.isConnected()) {
                //allow client to keep the websocket open in order to refresh timeout with simple message "ping"
                if (s != null && !s.equalsIgnoreCase("ping") && !s.equalsIgnoreCase("\"ping\"")) {
                    this.client.publish(topic, 1, false, s.hashCode(), s.getBytes());
                } else {
                    logger.debug(" Message is {}, skipping from forwarding message to kakfa", s);
                }
            } else {
                getSession().getRemote().sendString("not connected");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            try {
                getSession().getRemote().sendString(e.getMessage());
            } catch (Exception e1) {
                logger.error(e1.getMessage(), e1);
            }
        }
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        try {
            this.client.subscribe(topic, MqttClientUtil.getQos());
            getSession().getRemote().sendString("MQTT Client Connected!");
        } catch (Exception e) {
            try {
                getSession().getRemote().sendString(e.getMessage());
            } catch (Exception e1) {
                logger.error(e1.getMessage(), e1);
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
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * This method tries to get parameter from header or from request parameters
     *
     * @param webSocketRequest
     * @return param value or empty string
     */
    private String getParameterFromUpgradeRequest(String paramName, UpgradeRequest webSocketRequest) {
        Map<String, List<String>> params = webSocketRequest.getHeaders();
        String paramValue = (params != null && params.get(paramName) != null && params.get(paramName).size() == 1) ? params.get(paramName).get(0) : null;
        if (paramValue == null) {
            params = webSocketRequest.getParameterMap();
            paramValue = (params != null && params.get(paramName) != null && params.get(paramName).size() == 1) ? params.get(paramName).get(0) : "";
        }
        return paramValue;
    }
}

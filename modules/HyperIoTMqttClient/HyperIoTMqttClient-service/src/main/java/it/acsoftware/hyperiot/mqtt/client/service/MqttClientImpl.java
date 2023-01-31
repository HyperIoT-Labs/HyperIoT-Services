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

package it.acsoftware.hyperiot.mqtt.client.service;

import it.acsoftware.hyperiot.mqtt.client.api.MqttClient;
import it.acsoftware.hyperiot.mqtt.client.util.MqttClientUtil;
import org.eclipse.paho.client.mqttv3.*;

import java.util.UUID;

public class MqttClientImpl implements MqttClient {

    private MqttConnectOptions options;
    private IMqttAsyncClient client;
    private IMqttMessageListener listener;

    public MqttClientImpl(String mqttBrokerAddress, String username, String password, IMqttMessageListener listener) throws MqttException {
        this.listener = listener;
        this.options = new MqttConnectOptions();
        this.client = new MqttAsyncClient(mqttBrokerAddress, username+ UUID.randomUUID().toString());
        options.setAutomaticReconnect(MqttClientUtil.getAutomaticReconnect());
        options.setCleanSession(MqttClientUtil.getCleanSession());
        options.setConnectionTimeout(MqttClientUtil.getConnectionTimeout());
        options.setKeepAliveInterval(MqttClientUtil.getKeepAlive());
        options.setUserName(username);
        options.setPassword(password.toCharArray());
    }

    public MqttClientImpl(String mqttBrokerAddress, String username, String password, IMqttMessageListener listener,MqttCallback callback) throws MqttException {
        this(mqttBrokerAddress,username,password,listener);
        client.setCallback(callback);
    }

    public void connect(IMqttActionListener callback) throws MqttSecurityException, MqttException {
        client.connect(options,null,callback);
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void disconnect() throws MqttException {
        this.client.disconnect();
    }

    public void publish(String topic, int qos, boolean retained, int id, byte[] payload) throws MqttException, MqttPersistenceException {
        MqttMessage m = new MqttMessage();
        m.setPayload(payload);
        m.setQos(qos);
        m.setId(id);
        m.setRetained(retained);
        this.client.publish(topic, m);
    }

    public void subscribe(String topic, int qos) throws MqttException {
        this.client.subscribe(topic, qos, listener);
    }

}
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

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.mqtt.client.api.MqttClient;
import it.acsoftware.hyperiot.mqtt.client.api.MqttClientSystemApi;
import it.acsoftware.hyperiot.mqtt.client.util.MqttClientUtil;

import org.eclipse.paho.client.mqttv3.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Aristide Cittadino Implementation class of the MqttClientSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = MqttClientSystemApi.class, immediate = true)
public final class MqttClientSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements MqttClientSystemApi, IMqttActionListener, IMqttMessageListener {
    private MqttClient adminClient;

    @Activate
    public void activate() {
        try {
            adminClient = this.createMqttClient(MqttClientUtil.getMqttBrokerCompleteAddress(), MqttClientUtil.getMqttAdminUsername(), MqttClientUtil.getMqttAdminPassword(), this);
            this.adminClient.connect(this);
        } catch (MqttException e) {
            getLog().error(e.getMessage(), e);
        }
    }

    @Override
    public void onSuccess(IMqttToken asyncActionToken) {
        getLog().info(" MQTT Success: {}",asyncActionToken.getResponse());
    }

    @Override
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
        getLog().error(exception.getMessage(), exception);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        getLog().debug("Message arrived on topic {}, message {}", topic, message);
    }

    @Deactivate
    public void deactivate() {
        try {
            if (this.adminClient != null && this.adminClient.isConnected())
                this.adminClient.disconnect();
        } catch (MqttException e) {
            getLog().error(e.getMessage(), e);
        }
    }

    @Override
    public MqttClient createMqttClient(String mqttBrokerAddress, String username, String password, IMqttMessageListener listener) throws MqttException {
        return new MqttClientImpl(mqttBrokerAddress, username, password, listener);
    }

    /**
     * Send an mqtt message as admin
     *
     * @param message
     * @param topic
     */
    public void publishAsAdmin(MqttMessage message, String topic) {
        if (this.adminClient != null) {
            try {
                if (!this.adminClient.isConnected()) {
                    throw new HyperIoTRuntimeException("Mqtt Admin client is no connected!");
                }
                this.adminClient.publish(topic, message.getQos(), message.isRetained(), message.getId(), message.getPayload());
                return;
            } catch (Exception e) {
                getLog().error(e.getMessage(), e);
            }
        }
        throw new HyperIoTRuntimeException("Mqtt Admin Client is NULL");
    }

}
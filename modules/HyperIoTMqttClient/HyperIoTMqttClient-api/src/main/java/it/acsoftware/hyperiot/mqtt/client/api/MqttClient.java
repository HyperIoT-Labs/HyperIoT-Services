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

package it.acsoftware.hyperiot.mqtt.client.api;

import it.acsoftware.hyperiot.base.api.HyperIoTBaseApi;
import org.eclipse.paho.client.mqttv3.*;

/**
 * @author Aristide Cittadino Interface component for MqttClientApi. This interface
 * defines methods for additional operations.
 */
public interface MqttClient extends HyperIoTBaseApi {
    void connect(IMqttActionListener callback) throws MqttSecurityException, MqttException;

    boolean isConnected();

    void disconnect() throws MqttException;

    void publish(String topic, int qos, boolean retained, int id, byte[] payload) throws MqttException, MqttPersistenceException;

    void subscribe(String topic, int qos) throws MqttException;
}

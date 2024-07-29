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

import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 * interface defines methods for additional operations.
 */
public interface MqttClientSystemApi extends HyperIoTBaseSystemApi {
    MqttClient createMqttClient(String mqttBrokerAddress, String username, String password, IMqttMessageListener listener) throws MqttException;

    void publishAsAdmin(MqttMessage message, String topic);
}

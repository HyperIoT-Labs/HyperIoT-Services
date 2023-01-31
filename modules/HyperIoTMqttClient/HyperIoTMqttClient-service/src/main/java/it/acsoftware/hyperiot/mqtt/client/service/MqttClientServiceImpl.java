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

import it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;
import it.acsoftware.hyperiot.mqtt.client.api.MqttClient;
import it.acsoftware.hyperiot.mqtt.client.api.MqttClientApi;
import it.acsoftware.hyperiot.mqtt.client.api.MqttClientSystemApi;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Aristide Cittadino Implementation class of MqttClientApi interface.
 * It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = MqttClientApi.class, immediate = true)
public final class MqttClientServiceImpl extends HyperIoTBaseServiceImpl implements MqttClientApi {
    /**
     * Injecting the MqttClientSystemApi
     */
    private MqttClientSystemApi systemService;

    /**
     * @return The current MqttClientSystemApi
     */
    protected MqttClientSystemApi getSystemService() {
        getLog().debug( "invoking getSystemService, returning: {}" , this.systemService);
        return systemService;
    }

    /**
     * @param mqttClientSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(MqttClientSystemApi mqttClientSystemService) {
        getLog().debug( "invoking setSystemService, setting: {}" , systemService);
        this.systemService = mqttClientSystemService;
    }

    @Override
    public MqttClient createMqttClient(String mqttBrokerAddress, String username, String password, IMqttMessageListener listener) throws MqttException {
        return this.systemService.createMqttClient(mqttBrokerAddress, username, password, listener);
    }
}

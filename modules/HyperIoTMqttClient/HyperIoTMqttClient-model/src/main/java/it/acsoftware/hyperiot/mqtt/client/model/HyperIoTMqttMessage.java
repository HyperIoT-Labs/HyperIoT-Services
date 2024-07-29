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

package it.acsoftware.hyperiot.mqtt.client.model;

/**
 * Author Aristide Cittadino
 * Class used to map MQTT Message outside the MQTT Broker
 */
public class HyperIoTMqttMessage {
    private String topic;
    private String message;

    public HyperIoTMqttMessage(String topic, String message) {
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int hashCode() {
        int result = 1;
        int prime = 31;
        result = prime * result + topic.hashCode();
        result = prime * result +  message.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HyperIoTMqttMessage))
            return false;
        HyperIoTMqttMessage message = (HyperIoTMqttMessage) obj;
        return message.topic.equals(this.topic) && message.message.equals(this.message);
    }
}

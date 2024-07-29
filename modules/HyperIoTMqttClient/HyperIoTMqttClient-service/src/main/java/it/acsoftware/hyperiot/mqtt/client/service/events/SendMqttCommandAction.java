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

package it.acsoftware.hyperiot.mqtt.client.service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.model.HPacketInfo;
import it.acsoftware.hyperiot.hproject.deserialization.service.builder.HPacketDeserializerBuilder;
import it.acsoftware.hyperiot.hproject.deserialization.service.util.HPacketDeserializerUtil;
import it.acsoftware.hyperiot.hproject.serialization.api.HPacketSerializer;
import it.acsoftware.hyperiot.hproject.serialization.service.builder.HPacketSerializerBuilder;

import it.acsoftware.hyperiot.mqtt.client.api.MqttClientSystemApi;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.EventRuleAction;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.DiscriminatorValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Component(service = RuleAction.class, immediate = true, property = {"it.acsoftware.hyperiot.rule.action.type=EVENT"})
@DiscriminatorValue("rule.action.name.sendMqttCommand")
public class SendMqttCommandAction extends EventRuleAction implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(SendMqttCommandAction.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private long packetId;
    private String packetFormat;
    private String message;
    private String topic;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getPacketId() {
        return packetId;
    }

    public void setPacketId(long packetId) {
        this.packetId = packetId;
    }

    public String getPacketFormat() {
        return packetFormat;
    }

    public void setPacketFormat(String packetFormat) {
        this.packetFormat = packetFormat;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String droolsDefinition() {
        return this.droolsAsJson();
    }

    @Override
    public void run() {
        if (this.isActive()) {
            try {
                logger.debug("Starting Send MQTT Command Action ....");
                HPacket packetDefinition = getHPacketSystemApi().find(packetId, null);
                HPacketInfo info = createHPacketInfo(packetDefinition);
                info.getTimestamp().setCreateDefaultIfNotExists(false);
                //front end sends a json rapresentation of the action and data
                HPacket packetToSend = deserializeFromJSon(info);
                packetToSend.setFormat(packetDefinition.getFormat());
                //Serializer is used to send in the format specified by packet configuration
                HPacketSerializer serializer = HPacketSerializerBuilder.getHPacketSerializer(packetToSend);
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setRetained(true);
                //we send only HPacket fields outside
                mqttMessage.setPayload(serializer.serializeRaw(packetToSend));
                //exactly once
                mqttMessage.setQos(2);
                logger.debug("Sending message: {0}", mqttMessage);
                this.getMqttClientSystemApi().publishAsAdmin(mqttMessage, topic);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        } else {
            logger.debug("Event fired but not active, skipping sending message to MQTT server");
        }
    }

    /**
     * Deserializes json inside the message field rapresenting an HPacket data
     *
     * @param info
     * @return
     * @throws IOException
     */
    private HPacket deserializeFromJSon(HPacketInfo info) throws IOException {
        HPacketDeserializer deserializer = HPacketDeserializerBuilder.getDeserializer(HPacketFormat.JSON);
        return deserializer.deserialize(message.getBytes(StandardCharsets.UTF_8), info);
    }

    /**
     * Creates HPacketInfo object based on HPacket saved on database
     *
     * @return
     */
    private HPacketInfo createHPacketInfo(HPacket packetDefinition) {
        return HPacketDeserializerUtil.createPacketInfoFromHPacket(packetDefinition);
    }

    /**
     *
     * @return
     */
    private HPacketSystemApi getHPacketSystemApi() {
        try {
            ServiceReference<HPacketSystemApi> serviceRef = this.getBundleContext().getServiceReference(HPacketSystemApi.class);
            return this.getBundleContext().getService(serviceRef);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     *
     * @return
     */
    private MqttClientSystemApi getMqttClientSystemApi() {
        try {
            ServiceReference<MqttClientSystemApi> serviceRef = this.getBundleContext().getServiceReference(MqttClientSystemApi.class);
            return this.getBundleContext().getService(serviceRef);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}

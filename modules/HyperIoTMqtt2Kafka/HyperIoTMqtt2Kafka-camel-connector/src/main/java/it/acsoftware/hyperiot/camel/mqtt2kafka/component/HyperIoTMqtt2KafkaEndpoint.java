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
package it.acsoftware.hyperiot.camel.mqtt2kafka.component;

import it.acsoftware.hyperiot.camel.mqtt2kafka.component.util.HyperIoTMqtt2KafkaUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

/**
 * @author Aristide Cittadino
 * Represents a HyperIoTMqtt2Kafka endpoint.
 */
public class HyperIoTMqtt2KafkaEndpoint extends DefaultEndpoint {
    private static Logger log = LoggerFactory.getLogger(HyperIoTMqtt2KafkaEndpoint.class);

    public HyperIoTMqtt2KafkaEndpoint(String endpointUri, String remaining,
                                      HyperIoTMqtt2KafkaConnector component) throws URISyntaxException {
        super(endpointUri, component);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Producer createProducer() throws Exception {
        log.debug("Creating Kafka Producer for Camel Context...");
        KafkaConnectorSystemApi kafkaConnectorSystemApi = HyperIoTMqtt2KafkaUtil.getKafkaConnectorSystemApi();
        if(kafkaConnectorSystemApi != null)
            return new HyperIoT2KafkaProducer(this,kafkaConnectorSystemApi.getNewProducer());
        throw new RuntimeException("Impossible to create kafka producer!");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }


}

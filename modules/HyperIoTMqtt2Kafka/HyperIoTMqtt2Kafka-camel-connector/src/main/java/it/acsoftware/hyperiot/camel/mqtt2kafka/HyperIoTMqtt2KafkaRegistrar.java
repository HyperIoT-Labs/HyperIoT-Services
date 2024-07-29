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

package it.acsoftware.hyperiot.camel.mqtt2kafka;

import it.acsoftware.hyperiot.camel.mqtt2kafka.component.HyperIoTMqtt2KafkaConnector;
import it.acsoftware.hyperiot.camel.mqtt2kafka.component.util.HyperIoTMqtt2KafkaUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Aristide Cittadino
 * This component creates a camel context and initializes mqtt 2 kafka routing via camel
 */
@Component(immediate = true)
public class HyperIoTMqtt2KafkaRegistrar {
    private Logger logger = LoggerFactory.getLogger(HyperIoTMqtt2KafkaRegistrar.class);
    private CamelContext mqtt2KafkaContext;

    @Activate
    public void start(BundleContext context) {
        mqtt2KafkaContext = new DefaultCamelContext();
        mqtt2KafkaContext.addComponent(HyperIoTMqtt2KafkaConnector.HYPERIOT_CAMEL_MQTT_2_KAFKA_COMPONENT_NAME, new HyperIoTMqtt2KafkaConnector());
        ActiveMQComponent activeMQComponent = new ActiveMQComponent(this.mqtt2KafkaContext);
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setUserName(HyperIoTMqtt2KafkaUtil.getMqttBrokerUsername());
        factory.setPassword(HyperIoTMqtt2KafkaUtil.getMqttBrokerPassword());
        activeMQComponent.setConnectionFactory(factory);
        mqtt2KafkaContext.addComponent("activemq", activeMQComponent);
        try {
            context.registerService(CamelContext.class, mqtt2KafkaContext, null);
            this.mqtt2KafkaContext.start();
            Thread startRouteThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    HyperIoTMqtt2KafkaRegistrar.this.startMqtt2KafkaRoute();
                }
            });
            startRouteThread.start();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Deactivate
    public void stop(BundleContext context) throws Exception {
        this.mqtt2KafkaContext.stop();
    }

    private void startMqtt2KafkaRoute() {
        long waitTime = 2000;
        boolean routeStarted = false;
        while (!routeStarted) {
            routeStarted = attempRouteStart();
            if (!routeStarted) {
                waitTime = waitBeforeRestart(waitTime);
            }
        }
    }

    private boolean attempRouteStart() {
        try {
            this.mqtt2KafkaContext.addRoutes(new HyperIoTMqttJMS2KafkaRouteBuilder());
            this.mqtt2KafkaContext.startRoute(HyperIoTMqttJMS2KafkaRouteBuilder.HYPERIOT_MQTT_TO_JMS_ID);
            return true;
        } catch (Throwable e) {
            try {
                this.mqtt2KafkaContext.stopRoute(HyperIoTMqttJMS2KafkaRouteBuilder.HYPERIOT_MQTT_TO_JMS_ID);
                this.mqtt2KafkaContext.removeRoute(HyperIoTMqttJMS2KafkaRouteBuilder.HYPERIOT_MQTT_TO_JMS_ID);
            } catch (Exception e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    private long waitBeforeRestart(long currentWaitTime) {
        try {
            Thread.sleep((long) currentWaitTime);
            return (long) Math.pow(currentWaitTime, 2);
        } catch (InterruptedException e) {
        }
        return currentWaitTime;
    }

}

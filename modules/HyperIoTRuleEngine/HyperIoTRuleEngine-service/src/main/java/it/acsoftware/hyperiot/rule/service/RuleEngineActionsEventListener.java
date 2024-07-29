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

package it.acsoftware.hyperiot.rule.service;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaMessageReceiver;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import it.acsoftware.hyperiot.kafka.connector.model.messages.types.SystemMessageType;
import it.acsoftware.hyperiot.kafka.connector.util.HyperIoTKafkaConnectorConstants;
import it.acsoftware.hyperiot.rule.model.RuleEngineConstants;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Generoso Martello Class to receive and process messages on
 * Kafka-HyperIoT events topic
 */
@Component(service = KafkaMessageReceiver.class, property = {
        HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_TOPIC_FILTER
                + "=" + HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_MICROSERVICES_TOPIC,
        HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_KEY_FILTER
                + "=" + HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_SYSTEM_MESSAGE_TYPE_PROCESS_EVENT},
        immediate = true)
public class RuleEngineActionsEventListener implements KafkaMessageReceiver {
    private Logger logger = LoggerFactory.getLogger(RuleEngineActionsEventListener.class.getName());

    private KafkaConnectorSystemApi kafkaConnectorSystemApi;
    private ThreadPoolExecutor actionsPool;
    private BlockingQueue<Runnable> workQueue;

    public RuleEngineActionsEventListener() {
        super();
        workQueue = new LinkedBlockingDeque<>();
        String coreEventsThread = (String) HyperIoTUtil
                .getHyperIoTProperty(RuleEngineConstants.RULE_ENGINE_EVENTS_THREAD_CORE);
        String maxEventsThread = (String) HyperIoTUtil
                .getHyperIoTProperty(RuleEngineConstants.RULE_ENGINE_EVENTS_THREAD_MAXIMUM);
        String idleKeepAlive = (String) HyperIoTUtil
                .getHyperIoTProperty(RuleEngineConstants.RULE_ENGINE_EVENTS_THREAD_IDLE_KEEP_ALIVE);

        // Default values
        int coreThreads = 1;
        int maxThreads = 10;
        int keepAlive = 60;

        if (coreEventsThread != null && coreEventsThread.length() > 0) {
            try {
                coreThreads = Integer.parseInt(coreEventsThread);
            } catch (Exception e) {
                logger.error( e.getMessage(), e);
            }
        }

        if (maxEventsThread != null && maxEventsThread.length() > 0) {
            try {
                maxThreads = Integer.parseInt(maxEventsThread);
            } catch (Exception e) {
                logger.error( e.getMessage(), e);
            }
        }

        if (idleKeepAlive != null && idleKeepAlive.length() > 0) {
            try {
                keepAlive = Integer.parseInt(idleKeepAlive);
            } catch (Exception e) {
                logger.error( e.getMessage(), e);
            }
        }

        actionsPool = new ThreadPoolExecutor(coreThreads, maxThreads, keepAlive, TimeUnit.MINUTES,
                workQueue);
    }

    @Override
    public synchronized void receive(HyperIoTKafkaMessage message) {
        logger.debug( "Receiving message: {}", message);
        String actionKey = new String(message.getKey());
        SystemMessageType messageType = SystemMessageType.valueOf(actionKey);
        if (messageType == SystemMessageType.PROCESS_EVENT) {
            String payload = new String(message.getPayload());
            RuleAction ruleAction = RuleAction.deserializeFromJson(payload);
            if (ruleAction != null) {
                //setting the bundle context of the rule action so it can use osgi components
                ruleAction.setBundleContext(HyperIoTUtil.getBundleContext(this));

                if (ruleAction instanceof Runnable) {
                    try {
                        logger.debug( "Scheduling execution for action on message: {} action: {}", message, ruleAction);
                        actionsPool.submit((Runnable) ruleAction);
                    } catch (Exception e) {
                        logger.error( e.getMessage(), e);
                    }
                }
            } else {
                logger.error( "No actions for message: {}", message);
            }
        }
    }

    protected KafkaConnectorSystemApi getKafkaConnectorSystemApi() {
        return kafkaConnectorSystemApi;
    }

    @Reference
    protected void setKafkaConnectorSystemApi(KafkaConnectorSystemApi kafkaConnectorSystemApi) {
        this.kafkaConnectorSystemApi = kafkaConnectorSystemApi;
    }
}

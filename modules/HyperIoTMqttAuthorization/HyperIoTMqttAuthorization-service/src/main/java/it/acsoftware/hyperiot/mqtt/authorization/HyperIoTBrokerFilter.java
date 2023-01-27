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

package it.acsoftware.hyperiot.mqtt.authorization;

import it.acsoftware.hyperiot.base.model.authentication.principal.HyperIoTPrincipal;
import it.acsoftware.hyperiot.base.model.authentication.principal.HyperIoTTopicPrincipal;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaMessageReceiver;
import it.acsoftware.hyperiot.kafka.connector.model.HyperIoTKafkaMessage;
import it.acsoftware.hyperiot.kafka.connector.model.messages.types.MqttMessageType;
import it.acsoftware.hyperiot.kafka.connector.util.HyperIoTKafkaConnectorConstants;
import org.apache.activemq.broker.*;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.DestinationInfo;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.security.SecurityContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.*;

/**
 * @author Aristide Cittadino Class which verifies users or devices can
 * create,remove,send or receive messages from topics. There's one basic
 * rule: Topics are associated with logged user/device, if Principal
 * (which contains topic list) contains the requested topic the
 * user/device can do anything on it.
 * <p>
 * Future versions may consider to give more granular access to topics.
 */
public class HyperIoTBrokerFilter extends BrokerFilter
        implements KafkaMessageReceiver, ServiceListener {
    private static Logger log = LoggerFactory.getLogger(HyperIoTBrokerFilter.class.getName());

    private ServiceRegistration<KafkaMessageReceiver> registration;

    public HyperIoTBrokerFilter(Broker next) {
        super(next);
    }

    /**
     * Method invoked on broker startup. This method instantiates a thread and
     * register a ServiceListener for KafkaConnectorSystemApi receive data.
     */
    private void init() {
        KafkaConnectorSystemApiListener kcsal = new KafkaConnectorSystemApiListener(this);
        Thread t = new Thread(kcsal);
        t.start();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<?> reference = event.getServiceReference();
        Object o = HyperIoTUtil.getBundleContext(this.getClass()).getService(reference);
        if (o instanceof KafkaConnectorSystemApi) {
            KafkaConnectorSystemApi kafkaConnectorSystemApi = (KafkaConnectorSystemApi) o;
            if (event.getType() == ServiceEvent.REGISTERED) {
                this.subscribeToKafka(kafkaConnectorSystemApi);
            } else if (event.getType() == ServiceEvent.MODIFIED) {
                this.unsubscribeToKafka();
                this.subscribeToKafka(kafkaConnectorSystemApi);
            } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                this.unsubscribeToKafka();
            }

        }

    }

    /**
     * Invoked by the Plugin in order to register this instance as a Kafka Message
     * Receiver.
     */
    public synchronized void subscribeToKafka(KafkaConnectorSystemApi kafkaConnectorSystemApi) {
        log.debug( "Setting up Authorization Filter...");
        if (kafkaConnectorSystemApi != null) {
            log.debug( "Kafka Connector System API found, registering to node topics...");
            List<String> topics = new ArrayList<>();
            topics.add(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_BASIC_TOPIC + "_"
                    + HyperIoTUtil.getLayer());
            topics.add(HyperIoTKafkaConnectorConstants.HYPERIOT_KAFKA_OSGI_BASIC_TOPIC + "_"
                    + HyperIoTUtil.getLayer() + "_" + HyperIoTUtil.getNodeId());
            Dictionary<String, Object> properties = new Hashtable<>();
            this.registration = kafkaConnectorSystemApi.registerKafkaMessageReceiver(this, topics,
                    properties);
        }
    }

    /**
     * Invoked by the Plugin in order to register this instance as a Kafka Message
     * Receiver.
     */
    public synchronized void unsubscribeToKafka() {
        if (this.registration != null) {
            log.debug( "Setting up Authorization Filter...");
            this.registration.unregister();
        }
    }

    protected SecurityContext checkSecurityContext(ConnectionContext context)
            throws SecurityException {
        final SecurityContext securityContext = context.getSecurityContext();
        if (securityContext == null) {
            throw new SecurityException("User is not authenticated.");
        }
        log.debug("Invoking checkSecurityContext: {}", context.getSecurityContext().toString());
        return securityContext;
    }

    protected boolean checkDestinationAccess(SecurityContext securityContext,
                                             ActiveMQDestination destination, boolean checkCanSubscribeOnly, boolean checkCanPublishOnly) {
        String message = (((destination != null) ? destination.toString() : " null destination"));
        log.debug( "Invoking checkDestinationAccess for destination: {}", message);

        if (destination == null) {
            return true;
        }

        if (!securityContext.isBrokerContext() && destination.isTopic()) {
            Iterator<Principal> it = securityContext.getPrincipals().iterator();
            while (it.hasNext()) {
                Principal p = it.next();
                log.debug( "Principal: {}", p.getName());
                if (p instanceof HyperIoTTopicPrincipal) {

                    String destinationTopic = extractTopic(destination.getPhysicalName());
                    log.debug(
                            p.getName() + " is a topic, checking equals to: {}", destinationTopic);
                    String topic = p.getName();
                    if (destinationTopic.equalsIgnoreCase(topic)) {
                        if (!checkCanPublishOnly && !checkCanSubscribeOnly) {
                            log.debug( "Access to topic granted...");
                            return true;
                        } else if (checkCanPublishOnly) {
                            log.debug( "Access to topic granted for publish only...");
                            HyperIoTTopicPrincipal topicPrincipal = (HyperIoTTopicPrincipal) p;
                            if (topicPrincipal.isCanPublish()) {
                                return true;
                            }
                            return false;
                        } else if (checkCanSubscribeOnly) {
                            log.debug( "Access to topic granted for consuming only...");
                            HyperIoTTopicPrincipal topicPrincipal = (HyperIoTTopicPrincipal) p;
                            if (topicPrincipal.isCanRead()) {
                                return true;
                            }
                            return false;
                        }
                        log.debug( "Topic found, but no rule match!");
                    }
                } else if (p instanceof HyperIoTPrincipal) {
                    HyperIoTPrincipal userPrincipal = (HyperIoTPrincipal) p;
                    if (userPrincipal.isAdmin())
                        return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public void addDestinationInfo(ConnectionContext context, DestinationInfo info)
            throws Exception {
        log.debug(
                "Invoking addDestinationInfo for topic: {}", info.getDestination().toString());
        final SecurityContext securityContext = checkSecurityContext(context);

        if (!checkDestinationAccess(securityContext, info.getDestination(), false, false)) {
            throw new SecurityException("User " + securityContext.getUserName()
                    + " is not authorized to create: " + info.getDestination());
        }

        getNext().addDestinationInfo(context, info);
    }

    @Override
    public Destination addDestination(ConnectionContext context, ActiveMQDestination destination,
                                      boolean create) throws Exception {
        log.debug( "Invoking addDestination for topic: {}", destination.toString());
        final SecurityContext securityContext = checkSecurityContext(context);

        if (!checkDestinationAccess(securityContext, destination, false, false)) {
            throw new SecurityException("User " + securityContext.getUserName()
                    + " is not authorized to create: " + destination);
        }

        return getNext().addDestination(context, destination, create);
    }

    @Override
    public void removeDestination(ConnectionContext context, ActiveMQDestination destination,
                                  long timeout) throws Exception {
        log.debug( "Invoking removeDestination for topic: {}", destination.toString());
        final SecurityContext securityContext = checkSecurityContext(context);

        if (!checkDestinationAccess(securityContext, destination, false, false)) {
            throw new SecurityException("User " + securityContext.getUserName()
                    + " is not authorized to remove: " + destination);
        }

        getNext().removeDestination(context, destination, timeout);
    }

    @Override
    public void removeDestinationInfo(ConnectionContext context, DestinationInfo info)
            throws Exception {
        log.debug(
                "Invoking removeDestinationInfo for topic: {}", info.getDestination().toString());
        final SecurityContext securityContext = checkSecurityContext(context);

        if (!checkDestinationAccess(securityContext, info.getDestination(), false, false)) {
            throw new SecurityException("User " + securityContext.getUserName()
                    + " is not authorized to remove: " + info.getDestination());
        }

        getNext().removeDestinationInfo(context, info);
    }

    @Override
    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        log.debug( "Invoking addConsumer for topic: {}", info.getDestination().toString());
        final SecurityContext securityContext = checkSecurityContext(context);

        if (!checkDestinationAccess(securityContext, info.getDestination(), true, false)) {
            throw new SecurityException("User " + securityContext.getUserName()
                    + " is not authorized to read from: " + info.getDestination());
        }

        return getNext().addConsumer(context, info);
    }

    @Override
    public void addProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        final SecurityContext securityContext = checkSecurityContext(context);

        if (!checkDestinationAccess(securityContext, info.getDestination(), false, true)) {
            throw new SecurityException("User " + securityContext.getUserName()
                    + " is not authorized to write to: " + info.getDestination());
        }

        getNext().addProducer(context, info);
    }

    @Override
    public void send(ProducerBrokerExchange producerExchange,
                     org.apache.activemq.command.Message messageSend) throws Exception {
        final SecurityContext securityContext = checkSecurityContext(
                producerExchange.getConnectionContext());

        if (!checkDestinationAccess(securityContext, messageSend.getDestination(), false, true)) {
            throw new SecurityException("User " + securityContext.getUserName()
                    + " is not authorized to write to: " + messageSend.getDestination());
        }

        getNext().send(producerExchange, messageSend);
    }

    private String extractTopic(String physicalName) {
        String convertedTopic = physicalName.replaceAll("\\.", "/");
        convertedTopic = convertedTopic.replaceAll(">", "");
        if (convertedTopic.startsWith("VirtualTopic/"))
            convertedTopic = convertedTopic.replace("VirtualTopic/", "");
        else if (convertedTopic.startsWith("/"))
            convertedTopic = convertedTopic.substring(1);
        log.debug( "Extracting topic from {}", new Object[]{physicalName, convertedTopic});
        return convertedTopic;
    }

    private void stopConnection(String clientId) {
        try {
            log.debug( "Searching for client id: {}", clientId);
            Connection[] connections = this.getClients();
            for (int i = 0; i < connections.length; i++) {
                Connection c = connections[i];
                log.debug( "Iterating over connection id: {}", c.getConnectionId()
                        + " searching for:" + clientId);
                if (c.getConnectionId().equalsIgnoreCase(clientId)) {
                    log.debug( "Stopping connection: {}", c.getConnectionId());
                    c.stop();
                    return;
                }
            }
        } catch (Exception e) {
            log.error( e.getMessage(), e);
        }
    }

    @Override
    public void receive(HyperIoTKafkaMessage message) {
        String key = new String(message.getKey());
        try {
            MqttMessageType keyMessageType = MqttMessageType.valueOf(key);
            if (keyMessageType == MqttMessageType.DROP_CONNECTION) {
                String clientId = new String(message.getPayload());
                this.stopConnection(clientId);
            }
        } catch (Exception e) {
            log.warn("Error on receive message from ActiveMQ Broker not valid Message Type: {}", e.getMessage(), e);
        }
    }

    @Override
    public void start() throws Exception {
        this.init();
        getNext().start();
    }

    @Override
    public void stop() throws Exception {
        this.unsubscribeToKafka();
        getNext().stop();
    }

}

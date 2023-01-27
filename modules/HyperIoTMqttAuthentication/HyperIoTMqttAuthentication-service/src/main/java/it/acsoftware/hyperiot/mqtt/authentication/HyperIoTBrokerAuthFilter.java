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

package it.acsoftware.hyperiot.mqtt.authentication;

import org.apache.activemq.broker.*;
import org.apache.activemq.broker.jmx.ManagedTransportConnection;
import org.apache.activemq.broker.jmx.ManagedTransportConnector;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.activemq.security.AuthenticationBroker;
import org.apache.activemq.security.JaasAuthenticationBroker;
import org.apache.activemq.security.JaasCertificateAuthenticationBroker;
import org.apache.activemq.security.SecurityContext;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Aristide Cittadino Class which verifies users or devices can
 * create,remove,send or receive messages from topics. There's one basic
 * rule: Topics are associated with logged user/device, if Principal
 * (which contains topic list) contains the requested topic the
 * user/device can do anything on it.
 * <p>
 * Future versions may consider to give more granular access to topics.
 */
public class HyperIoTBrokerAuthFilter extends BrokerFilter implements AuthenticationBroker {

    private final JaasCertificateAuthenticationBroker certificateAuthBroker;
    private final JaasAuthenticationBroker nonSslBroker;

    public HyperIoTBrokerAuthFilter(Broker next) {
        super(next);
        //get comma separated list from plugin configuration
        this.nonSslBroker = new JaasAuthenticationBroker(new EmptyBroker(), "HyperIoTMqttRealm");
        this.certificateAuthBroker = new JaasCertificateAuthenticationBroker(new EmptyBroker(), "HyperIoTMqttRealm");

    }

    /**
     * Overridden to allow for authentication using different Jaas
     * configurations depending on if the connection is SSL or not.
     *
     * @param context The context for the incoming Connection.
     * @param info    The ConnectionInfo Command representing the incoming
     *                connection.
     */
    @Override
    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {
        if (context.getSecurityContext() == null) {
            if (isSSL(context, info)) {
                if (info.getUserName() != null && info.getPassword() != null && !info.getUserName().isEmpty() && !info.getPassword().isEmpty())
                    this.nonSslBroker.addConnection(context, info);
                else
                    this.certificateAuthBroker.addConnection(context, info);
            } else {
                this.nonSslBroker.addConnection(context, info);
            }
        }
    }

    /**
     * Overriding removeConnection to make sure the security context is cleaned.
     */
    @Override
    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error) throws Exception {
        super.removeConnection(context, info, error);
        if (isSSL(context, info)) {
            if (info.getUserName() != null && info.getPassword() != null && !info.getUserName().isEmpty() && !info.getPassword().isEmpty())
                this.nonSslBroker.removeConnection(context,info,error);
            this.certificateAuthBroker.removeConnection(context, info, error);
        } else {
            this.nonSslBroker.removeConnection(context, info, error);
        }
    }

    private boolean isSSL(ConnectionContext context, ConnectionInfo info) throws Exception {
        boolean sslCapable = false;
        Connector connector = context.getConnector();
        if (connector instanceof TransportConnector) {
            TransportConnector transportConnector = (TransportConnector) connector;
            sslCapable = transportConnector.getServer().isSslServer();
        }
        // AMQ-5943, also check if transport context carries X509 cert
        if (!sslCapable && info.getTransportContext() instanceof X509Certificate[]) {
            sslCapable = true;
        }
        return sslCapable;
    }

    @Override
    public void removeDestination(ConnectionContext context, ActiveMQDestination destination, long timeout) throws Exception {
        // Give both a chance to clear out their contexts
        this.certificateAuthBroker.removeDestination(context, destination, timeout);
        this.nonSslBroker.removeDestination(context, destination, timeout);

        super.removeDestination(context, destination, timeout);
    }

    @Override
    public SecurityContext authenticate(String username, String password, X509Certificate[] peerCertificates) throws SecurityException {
        if (peerCertificates != null) {
            return this.certificateAuthBroker.authenticate(username, password, peerCertificates);
        } else {
            return this.nonSslBroker.authenticate(username, password, peerCertificates);
        }
    }
}

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

package it.acsoftware.hyperiot.mqtt.authentication.service.jaas;

import it.acsoftware.hyperiot.base.model.authentication.principal.HyperIoTTopicPrincipal;
import it.acsoftware.hyperiot.authentication.service.jaas.HyperIoTJaaSAuthenticationModule;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HyperIoTTopicType;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import org.apache.activemq.jaas.CertificateCallback;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;

public class HyperIoTJaaSMqttAuthenticationModule extends HyperIoTJaaSAuthenticationModule
        implements LoginModule {

    private static Logger log = LoggerFactory.getLogger(HyperIoTJaaSMqttAuthenticationModule.class.getName());

    protected List<String> topics;
    protected List<String> writeOnlyTopics;

    @Override
    public boolean login() throws LoginException {
        //login with certs
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new CertificateCallback();
        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioe) {
            throw new LoginException(ioe.getMessage());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getMessage() + " Unable to obtain client certificates.");
        }
        X509Certificate[] certificates = ((CertificateCallback) callbacks[0]).getCertificates();

        if (certificates == null || certificates.length == 0) {
            //tries to login with username and password
            return super.login();
        }

        this.loggedUser = getUserNameForCertificates(certificates);
        if (this.loggedUser == null) {
            throw new FailedLoginException("No user for client certificate: " + getDistinguishedName(certificates));
        }
        this.user = this.loggedUser.getScreenName();
        this.loginSucceeded = true;
        this.postAuthentication(loggedUser);
        return true;
    }

    protected HDevice getUserNameForCertificates(X509Certificate[] certs) throws LoginException {
        //selecting device name from 4th characted since string is CN=<devicename>
        String deviceName = certs[0].getSubjectDN().getName().substring(3);
        try {
            HDevice device = this.getHDeviceSystemApi().findByDeviceName(deviceName);
            int n = certs.length;
            for (int i = 0; i < n - 1; i++) {
                X509Certificate cert = (X509Certificate) certs[i];
                X509Certificate issuer = (X509Certificate) certs[i + 1];
                if (cert.getIssuerX500Principal().equals(issuer.getSubjectX500Principal()) == false) {
                    throw new LoginException("Login failed, cert auth not valid!");
                }
                cert.verify(issuer.getPublicKey());
                log.debug( "Verified: {}" , cert.getSubjectX500Principal());
            }

            X509Certificate last = (X509Certificate) certs[n - 1];
            // if self-signed, verify the final cert
            if (last.getIssuerX500Principal().equals(last.getSubjectX500Principal())) {
                last.verify(last.getPublicKey());
                log.debug( "Verified: {}" , last.getSubjectX500Principal());
                //only if it is validated
                return device;
            }
            return null;
        } catch (Exception e) {
            log.error( e.getMessage(), e);
            throw new LoginException("Login failed, cert auth not valid!");
        }
    }

    protected String getDistinguishedName(final X509Certificate[] certs) {
        if (certs != null && certs.length > 0 && certs[0] != null) {
            return certs[0].getSubjectDN().getName();
        } else {
            return null;
        }
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        log.debug( "Initializing HyperIoTJaaSMqttAuthenticationModule...");
        super.initialize(subject, callbackHandler, sharedState, options);
        this.topics = new ArrayList<>();
        this.writeOnlyTopics = new ArrayList<>();
    }

    @Override
    protected boolean doCommit() throws LoginException {
        boolean result = super.doCommit();

        for (String entry : topics) {
            log.debug( "Adding new TOPIC Principal {}" , entry);
            principals.add(new HyperIoTTopicPrincipal(entry));
        }

        for (String entry : writeOnlyTopics) {
            log.debug( "Adding new WRITE ONLY TOPIC Principal {}" , entry);
            principals.add(new HyperIoTTopicPrincipal(entry, false, true));
        }

        return result;
    }

    @Override
    protected void postAuthentication(HyperIoTAuthenticable authenticated) {
        topics = this.getTopics();
        writeOnlyTopics = this.getWriteOnlyTopics();
    }

    /**
     * Calculates topics which the device can connect to
     */
    protected List<String> getTopics() {
        if (this.loggedUser != null && this.loggedUser instanceof HDevice) {
            HDevice device = (HDevice) this.loggedUser;
            if (device.getProject() != null) {
                List<String> topics = getHProjectSystemService().getDeviceTopics(HyperIoTTopicType.MQTT,
                        device.getProject().getId(), device);
                List<String> topicsReadOnly = Collections.unmodifiableList(topics);
                return topicsReadOnly;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Calculates write-only topics which the device can connect to
     */
    protected List<String> getWriteOnlyTopics() {
        if (this.loggedUser != null && this.loggedUser instanceof HDevice) {
            HDevice device = (HDevice) this.loggedUser;
            if (device.getProject() != null) {
                long projectId = device.getProject().getId();
                Collection<HDevice> projectDevices = getHDeviceSystemApi().getProjectDevicesList(projectId);
                List<String> topics = getHProjectSystemService().getWriteOnlyDeviceTopics(HyperIoTTopicType.MQTT,
                        projectId, device, projectDevices);
                List<String> topicsReadOnly = Collections.unmodifiableList(topics);
                return topicsReadOnly;
            }
        }
        return new ArrayList<>();
    }

    /**
     * return the current HProjectSystemApi
     */
    protected HProjectSystemApi getHProjectSystemService() {
        log.debug( "Invoking getHyperIoTAuthApi for searching for HPacketSystemApi");
        try {
            Collection<ServiceReference<HProjectSystemApi>> references = HyperIoTUtil
                    .getBundleContext(this).getServiceReferences(HProjectSystemApi.class, null);
            if (references != null && !references.isEmpty()) {
                return HyperIoTUtil.getBundleContext(this).getService(references.iterator().next());
            }
        } catch (InvalidSyntaxException e) {
            log.warn( e.getMessage(), e);
        }
        return null;
    }

    /**
     * return the current HDeviceSystemApi
     */
    protected HDeviceSystemApi getHDeviceSystemApi() {
        log.debug( "Invoking getHyperIoTAuthApi for searching for HDeviceSystemApi");
        try {
            Collection<ServiceReference<HDeviceSystemApi>> references = HyperIoTUtil
                    .getBundleContext(this).getServiceReferences(HDeviceSystemApi.class, null);
            if (references != null && !references.isEmpty()) {
                return HyperIoTUtil.getBundleContext(this).getService(references.iterator().next());
            }
        } catch (InvalidSyntaxException e) {
            log.warn( e.getMessage(), e);
        }
        return null;
    }


    protected String getAuthenticationProviderFilter() {
        String osgiFilter = OSGiFilterBuilder.createFilter(HyperIoTConstants.OSGI_AUTH_PROVIDER_RESOURCE, HDevice.class.getName()).getFilter();
        return osgiFilter;
    }


}

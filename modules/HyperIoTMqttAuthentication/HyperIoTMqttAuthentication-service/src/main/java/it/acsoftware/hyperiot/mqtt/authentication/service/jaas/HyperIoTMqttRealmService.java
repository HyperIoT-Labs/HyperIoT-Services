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

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
public class HyperIoTMqttRealmService implements JaasRealm {
    public static final String REALM_NAME = "HyperIoTMqttRealm";

    private AppConfigurationEntry[] configEntries;

    @Activate
    public void activate(BundleContext bc) {
        // create the configuration entry field using ProxyLoginModule class

        Map<String, Object> options = new HashMap<>();
        configEntries = new AppConfigurationEntry[1];
        configEntries[0] = new AppConfigurationEntry(ProxyLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, options);

        // actual LoginModule class name will be passed using the options object

        options.put(ProxyLoginModule.PROPERTY_MODULE,
                HyperIoTJaaSMqttAuthenticationModule.class.getName());

        // put bundle id of the LoginModule and bundlecontext of it
        // (in this case, it is the same bundle)
        // This is a neat trick to adapt to OSGI classloader

        long bundleId = bc.getBundle().getBundleId();
        options.put(ProxyLoginModule.PROPERTY_BUNDLE, String.valueOf(bundleId));
        options.put(BundleContext.class.getName(), bc);


        // add extra options if needed; for example, karaf encryption
        // ....
    }

    @Override
    public AppConfigurationEntry[] getEntries() {
        return configEntries;
    }

    // return the name and the rank of the realm

    @Override
    public String getName() {
        return REALM_NAME;
    }

    @Override
    public int getRank() {
        return 0;
    }




}

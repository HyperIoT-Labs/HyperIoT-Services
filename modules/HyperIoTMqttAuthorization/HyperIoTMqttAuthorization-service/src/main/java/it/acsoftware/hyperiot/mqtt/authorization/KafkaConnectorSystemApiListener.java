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

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.kafka.connector.api.KafkaConnectorSystemApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConnectorSystemApiListener implements Runnable {
	private static Logger log = LoggerFactory.getLogger(KafkaConnectorSystemApiListener.class.getName());
	private HyperIoTBrokerFilter brokerFilter;

	public KafkaConnectorSystemApiListener(HyperIoTBrokerFilter brokerFilter) {
		this.brokerFilter = brokerFilter;
	}

	@Override
	public void run() {
		try {
			log.debug( "Initializing broker MQTT...");
			String filter = OSGiFilterBuilder
					.createFilter(Constants.OBJECTCLASS, KafkaConnectorSystemApi.class.getName())
					.getFilter();
			// Waiting for bundle context available...
			BundleContext bundleContext = null;
			while (bundleContext == null) {
				log.info( "Waiting for BundleContext in HyperIoTBrokerFilter....");
				bundleContext = HyperIoTUtil.getBundleContext(this.getClass());
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {

				}
			}
			log.info(
					"BundleContext found in HyperIoTBrokerFilter, start consuming from Kafka");
			bundleContext.addServiceListener(brokerFilter, filter);
			ServiceReference<KafkaConnectorSystemApi> serviceReference = bundleContext
					.getServiceReference(KafkaConnectorSystemApi.class);
			if (serviceReference != null) {
				log.debug( "Registering to KafkaConnectorSystem API");
				KafkaConnectorSystemApi service = bundleContext.getService(serviceReference);
				brokerFilter.subscribeToKafka(service);
				bundleContext.ungetService(serviceReference);
			}

		} catch (Exception e) {
			log.error( e.getMessage(), e);
		}
	}

}

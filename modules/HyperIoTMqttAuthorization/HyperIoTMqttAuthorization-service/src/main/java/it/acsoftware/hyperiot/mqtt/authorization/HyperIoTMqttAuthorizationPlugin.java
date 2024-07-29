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

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HyperIoTMqttAuthorizationPlugin implements BrokerPlugin {
	private static Logger log = LoggerFactory.getLogger(HyperIoTMqttAuthorizationPlugin.class.getName());

	@Override
	public Broker installPlugin(Broker broker) throws Exception {
		log.info( "Installing and initializing HyperIoTMqttAuthorizationPlugin...");
		return new HyperIoTBrokerFilter(broker);
	}

}

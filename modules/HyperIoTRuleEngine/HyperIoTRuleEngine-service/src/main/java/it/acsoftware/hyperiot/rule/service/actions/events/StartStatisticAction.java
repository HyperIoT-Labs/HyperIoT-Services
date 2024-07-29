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

package it.acsoftware.hyperiot.rule.service.actions.events;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;

/**
 *
 * @author Aristide Cittadino
 * Class which start statistics on HyperIoT Platform
 */
public class StartStatisticAction extends RuleAction implements Runnable {
	private static Logger log = LoggerFactory.getLogger(StartStatisticAction.class.getName());

	@Override
	public String droolsDefinition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RuleType getRuleType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		// TODO: not implemented yet
		log.debug( "Starting StartStatisticAction Action ....");
	}

}

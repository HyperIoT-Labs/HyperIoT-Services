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

package it.acsoftware.hyperiot.rule.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate RuleEngine Actions
 *
 */
public enum HyperIoTRuleEngineAction implements HyperIoTActionName {
	
	//TO DO: add enumerations here
	ACTION_RUN("action_run");

	private String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the RuleEngine  action
	 */
	private HyperIoTRuleEngineAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of RuleEngine action
	 */
	public String getName() {
		return name;
	}

}

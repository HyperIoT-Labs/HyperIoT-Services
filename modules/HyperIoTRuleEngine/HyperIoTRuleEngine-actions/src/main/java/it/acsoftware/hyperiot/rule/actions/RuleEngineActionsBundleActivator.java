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

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;
import it.acsoftware.hyperiot.rule.model.Rule;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 *         register actions for RuleEngine
 *
 */
public class RuleEngineActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public List<HyperIoTActionList> getActions() {
		getLog().info("Registering Rule actions...");
		List<HyperIoTActionList> actionList = new ArrayList<>();
		HyperIoTActionList ruleActionList = HyperIoTActionFactory
				.createBaseCrudActionList(Rule.class.getName(), Rule.class.getName());
		ruleActionList.addAction(
				HyperIoTActionFactory.createAction(Rule.class.getName(),
						Rule.class.getName(), HyperIoTRuleEngineAction.ACTION_RUN)
		);
		actionList.add(ruleActionList);
		return actionList;
	}

}

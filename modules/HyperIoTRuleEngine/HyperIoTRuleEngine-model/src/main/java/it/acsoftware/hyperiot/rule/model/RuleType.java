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

package it.acsoftware.hyperiot.rule.model;

/**
 * 
 * @author Aristide Cittadino
 * Enumeration which identifies the rule type
 */
public enum RuleType {
	ENRICHMENT("it.acsoftware.hyperiot.rules.enrichments"),
	EVENT("it.acsoftware.hyperiot.rules.events"),
	ALARM_EVENT("it.acsoftware.hyperiot.rules.events");
	private String droolsPackage;

	private RuleType(String droolsPackage) {
		this.droolsPackage = droolsPackage;
	}

	public String getDroolsPackage() {
		return droolsPackage;
	}

}

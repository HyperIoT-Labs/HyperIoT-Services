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
package it.acsoftware.hyperiot.stormmanager.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate StormManager Actions
 *
 */
public enum HyperIoTStormManagerAction implements HyperIoTActionName {
	
	UPLOAD_TOPOLOGY_JAR("upload_topology_jar");

	private String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the StormManager  action
	 */
	HyperIoTStormManagerAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of StormManager action
	 */
	public String getName() {
		return name;
	}

}

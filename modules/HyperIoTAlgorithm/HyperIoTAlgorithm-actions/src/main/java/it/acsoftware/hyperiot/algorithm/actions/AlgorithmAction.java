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

package it.acsoftware.hyperiot.algorithm.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate Algorithm Actions
 *
 */
public enum AlgorithmAction implements HyperIoTActionName {
	
	ADD_IO_FIELD(Names.ADD_IO_FIELD),
	DELETE_IO_FIELD(Names.DELETE_IO_FIELD),
	READ_BASE_CONFIG(Names.READ_BASE_CONFIG),
	UPDATE_BASE_CONFIG(Names.UPDATE_BASE_CONFIG),
	UPDATE_JAR(Names.UPDATE_JAR),
	UPDATE_IO_FIELD(Names.UPDATE_IO_FIELD),
	CONTROL_PANEL(Names.CONTROL_PANEL);

	private final String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the Algorithm  action
	 */
	AlgorithmAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of Algorithm action
	 */
	public String getName() {
		return name;
	}

	public static class Names {

		public static final String ADD_IO_FIELD = "add_io_field";
		public static final String DELETE_IO_FIELD = "delete_io_field";
		public static final String READ_BASE_CONFIG = "read_base_config";
		public static final String UPDATE_BASE_CONFIG = "update_base_config-all";
		public static final String UPDATE_JAR = "update_jar";
		public static final String UPDATE_IO_FIELD = "update_io_field";
		public static final String CONTROL_PANEL = "control_panel";

		private Names() {
			throw new IllegalStateException("Utility class");
		}

	}

}

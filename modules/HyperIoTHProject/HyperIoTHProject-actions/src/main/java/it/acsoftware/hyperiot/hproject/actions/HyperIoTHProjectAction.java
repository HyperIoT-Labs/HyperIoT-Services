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

package it.acsoftware.hyperiot.hproject.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate HProject Actions
 *
 */
public enum HyperIoTHProjectAction implements HyperIoTActionName {
	
	//TO DO: add enumerations here
	ALGORITHMS_MANAGEMENT(Names.ALGORITHMS_MANAGEMENT),
	AREAS_MANAGEMENT(Names.AREAS_MANAGEMENT),
	DEVICE_LIST(Names.DEVICE_LIST),
	MANAGE_RULES(Names.MANAGE_RULES),
	GET_TOPOLOGY_LIST(Names.GET_TOPOLOGY_LIST),
	GET_TOPOLOGY(Names.GET_TOPOLOGY),
	ACTIVATE_TOPOLOGY(Names.ACTIVATE_TOPOLOGY),
	DEACTIVATE_TOPOLOGY(Names.DEACTIVATE_TOPOLOGY),
	KILL_TOPOLOGY(Names.KILL_TOPOLOGY),
	ADD_TOPOLOGY(Names.ADD_TOPOLOGY),
	SCAN_HBASE_DATA(Names.SCAN_HBASE_DATA),
	DELETE_HADOOP_DATA(Names.DELETE_HADOOP_DATA);
	private final String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the HProject  action
	 */
	HyperIoTHProjectAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of HProject action
	 */
	public String getName() {
		return name;
	}

	public static class Names {

		public static final String ALGORITHMS_MANAGEMENT = "algorithms_management";
		public static final String AREAS_MANAGEMENT = "areas_management";
		public static final String DEVICE_LIST = "device_list";
		public static final String MANAGE_RULES = "manage_rules";
		public static final String GET_TOPOLOGY_LIST = "get_topology_list";
		public static final String GET_TOPOLOGY = "get_topology";
		public static final String ACTIVATE_TOPOLOGY = "activate_topology";
		public static final String DEACTIVATE_TOPOLOGY = "deactivate_topology";
		public static final String KILL_TOPOLOGY = "kill_topology";
		public static final String ADD_TOPOLOGY = "add_topology";
		public static final String SCAN_HBASE_DATA = "scan_hbase-data";
		public static final String DELETE_HADOOP_DATA = "delete_hadoop_data";

		private Names() {
			throw new IllegalStateException("Utility class");
		}

	}

}

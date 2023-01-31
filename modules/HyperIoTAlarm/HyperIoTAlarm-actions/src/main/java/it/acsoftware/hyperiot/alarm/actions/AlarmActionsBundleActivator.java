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

package it.acsoftware.hyperiot.alarm.actions;

import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.alarm.model.Alarm;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for Alarm
 *
 */
public class AlarmActionsBundleActivator extends HyperIoTPermissionActivator {
	
	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public List<HyperIoTActionList> getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		getLog().info("Registering base CRUD actions...");
		List<HyperIoTActionList> actionList = new ArrayList<>();
		HyperIoTActionList alarmCrudActionList = HyperIoTActionFactory.createBaseCrudActionList(Alarm.class.getName(),
				Alarm.class.getName());
		HyperIoTActionList alarmEventCrudActionList =
				HyperIoTActionFactory.createBaseCrudActionList(AlarmEvent.class.getName(), AlarmEvent.class.getName());
		actionList.add(alarmCrudActionList);
		actionList.add(alarmEventCrudActionList);

		//TO DO: add more actions to actionList here...
		return actionList;
	}

}

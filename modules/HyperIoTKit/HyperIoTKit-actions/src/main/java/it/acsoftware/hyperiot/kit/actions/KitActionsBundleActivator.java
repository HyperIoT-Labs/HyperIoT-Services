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

package it.acsoftware.hyperiot.kit.actions;

import java.util.ArrayList;
import java.util.List;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.kit.category.actions.DiagnoseTypeAction;
import it.acsoftware.hyperiot.kit.category.model.DiagnoseType;
import it.acsoftware.hyperiot.kit.model.Kit;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for Kit
 *
 */
public class KitActionsBundleActivator extends HyperIoTPermissionActivator {
	
	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public List<HyperIoTActionList> getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		ArrayList<HyperIoTActionList> actionsLists = new ArrayList<>();
		
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(Kit.class.getName(),
				Kit.class.getName());
		
		actionsLists.add(actionList);
		//TO DO: add more actions to actionList here...
		HyperIoTActionList diagnoseTypeActionList = HyperIoTActionFactory.createBaseCrudActionList(DiagnoseType.class.getName(),
				DiagnoseType.class.getName());
		actionsLists.add(diagnoseTypeActionList);
		return actionsLists;
	}

}

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

package it.acsoftware.hyperiot.dashboard.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for Dashboard
 *
 */
public class DashboardActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public List<HyperIoTActionList> getActions() {
		getLog().info("Registering Dashboard actions...");
		List<HyperIoTActionList> actionList = new ArrayList<>();
		HyperIoTActionList dashboardActionList = HyperIoTActionFactory.createBaseCrudActionList(Dashboard.class.getName(),
				Dashboard.class.getName());
		dashboardActionList.addAction(HyperIoTActionFactory.createAction(Dashboard.class.getName(),
				Dashboard.class.getName(), HyperIoTDashboardAction.FIND_WIDGETS));
		HyperIoTActionList dashboardWidgetActionList = HyperIoTActionFactory.createBaseCrudActionList(DashboardWidget.class.getName(),
				DashboardWidget.class.getName());
		actionList.add(dashboardActionList);
		actionList.add(dashboardWidgetActionList);
		return actionList;
	}

}

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

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;
import it.acsoftware.hyperiot.base.action.util.HyperIoTShareAction;
import it.acsoftware.hyperiot.area.actions.HyperIoTAreaAction;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.hdevice.actions.HyperIoTHDeviceAction;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.actions.HyperIoTHPacketAction;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.model.HProject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for HProject
 */
public class HProjectActionsBundleActivator extends HyperIoTPermissionActivator {

    /**
     * Return a list actions that have to be registerd as OSGi components
     */
    @Override
    public List<HyperIoTActionList> getActions() {
        // creates base Actions save,update,remove,find,findAll for the specified entity
        log.info("Registering actions...");
        List<HyperIoTActionList> actionList = new ArrayList<>();
        HyperIoTActionList hProjectActionList = HyperIoTActionFactory
                .createBaseCrudActionList(HProject.class.getName(), HProject.class.getName());
        hProjectActionList.addAction(HyperIoTActionFactory.createAction(HProject.class.getName(),
                HProject.class.getName(), HyperIoTHProjectAction.ALGORITHMS_MANAGEMENT));
        hProjectActionList.addAction(HyperIoTActionFactory.createAction(HProject.class.getName(),
                HProject.class.getName(), HyperIoTHProjectAction.AREAS_MANAGEMENT));
        hProjectActionList.addAction(HyperIoTActionFactory.createAction(HProject.class.getName(),
                HProject.class.getName(), HyperIoTHProjectAction.DEVICE_LIST));
        hProjectActionList.addAction(HyperIoTActionFactory.createAction(HProject.class.getName(),
                HProject.class.getName(), HyperIoTHProjectAction.MANAGE_RULES));
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.GET_TOPOLOGY_LIST)
        );
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.GET_TOPOLOGY)
        );
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.ACTIVATE_TOPOLOGY)
        );
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.DEACTIVATE_TOPOLOGY)
        );

        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.KILL_TOPOLOGY)
        );
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.ADD_TOPOLOGY)
        );
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTShareAction.SHARE)
        );
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.SCAN_HBASE_DATA)
        );
        hProjectActionList.addAction(
                HyperIoTActionFactory.createAction(HProject.class.getName(),
                        HProject.class.getName(), HyperIoTHProjectAction.DELETE_HADOOP_DATA)
        );
        actionList.add(hProjectActionList);
        // Area actions
        HyperIoTActionList areaActionList = HyperIoTActionFactory.createBaseCrudActionList(Area.class.getName(),
                Area.class.getName());
        areaActionList.addAction(
                HyperIoTActionFactory.createAction(Area.class.getName(),
                        Area.class.getName(), HyperIoTAreaAction.AREA_DEVICE_MANAGER)
        );
        actionList.add(areaActionList);
        // HDevice actions
        HyperIoTActionList deviceActionList = HyperIoTActionFactory
                .createBaseCrudActionList(HDevice.class.getName(), HDevice.class.getName());
        deviceActionList.addAction(HyperIoTActionFactory.createAction(HDevice.class.getName(),
                HDevice.class.getName(), HyperIoTHDeviceAction.PACKETS_MANAGEMENT));
        actionList.add(deviceActionList);
        // HPacket actions
        HyperIoTActionList hPacketActionList = HyperIoTActionFactory.createBaseCrudActionList(HPacket.class.getName(),
                HPacket.class.getName());
        hPacketActionList.addAction(HyperIoTActionFactory.createAction(HPacket.class.getName(),
                HPacket.class.getName(), HyperIoTHPacketAction.FIELDS_MANAGEMENT));
        actionList.add(hPacketActionList);
        return actionList;
    }

}

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

package it.acsoftware.hyperiot.alarm.service;

import it.acsoftware.hyperiot.alarm.api.AlarmApi;
import it.acsoftware.hyperiot.alarm.api.AlarmSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.alarm.event.model.ProjectsAlarmsStatus;
import it.acsoftware.hyperiot.alarm.model.Alarm;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.security.annotations.AllowGenericPermissions;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collection;
import java.util.Set;


/**
 * @author Aristide Cittadino Implementation class of AlarmApi interface.
 * It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = AlarmApi.class, immediate = true)
public final class AlarmServiceImpl extends HyperIoTBaseEntityServiceImpl<Alarm> implements AlarmApi {

    public static final String ALARM_RESOURCE_NAME = "it.acsoftware.hyperiot.alarm.model.Alarm";

    /**
     * Injecting the AlarmSystemApi
     */
    private AlarmSystemApi systemService;

    /**
     * Constructor for a AlarmServiceImpl
     */
    public AlarmServiceImpl() {
        super(Alarm.class);
    }

    /**
     * @return The current AlarmSystemApi
     */
    protected AlarmSystemApi getSystemService() {
        getLog().debug("invoking getSystemService, returning: {}", this.systemService);
        return systemService;
    }

    /**
     * @param alarmSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(AlarmSystemApi alarmSystemService) {
        getLog().debug("invoking setSystemService, setting: {}", systemService);
        this.systemService = alarmSystemService;
    }


    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.SAVE)
    public Alarm saveAlarmAndEvents(Alarm alarm, Collection<AlarmEvent> alarmEvents, HyperIoTContext ctx) {
        return systemService.saveAlarmAndEvents(alarm, alarmEvents, ctx);
    }


    /*
        TODO
            This annotation must be fix.
            The permission must be checked on the project.
     */
    @Override
    @AllowGenericPermissions(actions = HyperIoTCrudAction.Names.FINDALL, resourceName = ALARM_RESOURCE_NAME)
    public Set<Alarm> findAlarmByProjectId(HyperIoTContext hyperIoTContext, long projectId) {
        return systemService.findAlarmByProjectId(hyperIoTContext, projectId);
    }

    @Override
    @AllowGenericPermissions(actions = HyperIoTCrudAction.Names.FINDALL, resourceName = ALARM_RESOURCE_NAME)
    public ProjectsAlarmsStatus getProjectsAlarmStatuses(HyperIoTContext hyperIoTContext, long[] projectIds) {
        return systemService.getProjectsAlarmStatuses(projectIds);
    }
}

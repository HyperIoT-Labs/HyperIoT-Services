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

package it.acsoftware.hyperiot.alarm.api;

import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.alarm.event.model.ProjectAlarmsStatus;
import it.acsoftware.hyperiot.alarm.event.model.ProjectsAlarmsStatus;
import it.acsoftware.hyperiot.alarm.model.Alarm;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 * interface defines methods for additional operations.
 */
public interface AlarmSystemApi extends HyperIoTBaseEntitySystemApi<Alarm> {

    Alarm saveAlarmAndEvents(Alarm alarm, Collection<AlarmEvent> alarmEvents, HyperIoTContext ctx);

    Set<Alarm> findAlarmByProjectId(HyperIoTContext hyperIoTContext, long projectId);

    ProjectsAlarmsStatus getProjectsAlarmStatuses(long[] projectIds);
}
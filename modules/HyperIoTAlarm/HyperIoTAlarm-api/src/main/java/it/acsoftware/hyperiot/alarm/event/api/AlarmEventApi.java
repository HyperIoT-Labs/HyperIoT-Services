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

package it.acsoftware.hyperiot.alarm.event.api;

import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;

import java.util.Collection;

/**
 * 
 * @author Aristide Cittadino Interface component for AlarmEventApi. This interface
 *         defines methods for additional operations.
 *
 */
public interface AlarmEventApi extends HyperIoTBaseEntityApi<AlarmEvent> {

    Collection<AlarmEvent> findAllEventByAlarmId(HyperIoTContext hyperIoTContext , long alarmId);

}
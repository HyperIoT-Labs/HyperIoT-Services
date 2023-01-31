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

package it.acsoftware.hyperiot.alarm.service.preaction;

import it.acsoftware.hyperiot.alarm.event.api.AlarmEventSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Component(service = HyperIoTPreRemoveAction.class, property = {"type=it.acsoftware.hyperiot.alarm.model.Alarm"},immediate = true)
public class AlarmPreRemoveAction<T extends HyperIoTBaseEntity> implements HyperIoTPreRemoveAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmPreRemoveAction.class.getName());

    private AlarmEventSystemApi alarmEventSystemApi;

    @Override
    public void execute(T entity) {
        long alarmId = entity.getId();
        LOGGER.debug("Delete events related to Alarm with id {}", alarmId);
        HyperIoTQuery byAlarmId = HyperIoTQueryBuilder.newQuery().equals("alarm.id", alarmId);
        Collection<AlarmEvent> alarmEvents =  alarmEventSystemApi.findAll(byAlarmId, null);
        alarmEvents.stream().sequential().forEach(alarmEvent -> {
            alarmEventSystemApi.remove(alarmEvent.getId(),null);
        });
    }

    @Reference
    public void setAlarmEventSystemApi(AlarmEventSystemApi alarmEventSystemApi) {
        this.alarmEventSystemApi = alarmEventSystemApi;
    }

}

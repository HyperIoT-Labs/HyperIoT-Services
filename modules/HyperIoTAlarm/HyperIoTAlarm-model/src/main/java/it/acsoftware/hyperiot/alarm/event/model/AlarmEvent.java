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

package it.acsoftware.hyperiot.alarm.event.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.acsoftware.hyperiot.alarm.model.Alarm;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTInnerEntityJSONSerializer;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import it.acsoftware.hyperiot.rule.model.Rule;

import javax.persistence.*;

/**
 * 
 * @author Aristide Cittadino Model class for AlarmEvent of HyperIoT platform. This
 *         class is used to map AlarmEvent with the database.
 *
 */

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"alarm_id", "event_id"})})
public class AlarmEvent extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity {

    @JsonView({HyperIoTJSONView.Public.class})
    @JsonSerialize(using = HyperIoTInnerEntityJSONSerializer.class)
    private Alarm alarm;

    @JsonView(HyperIoTJSONView.Public.class)
    private Rule event;

    @JsonView(HyperIoTJSONView.Public.class)
    private int severity;

    @NotNullOnPersist
    @ManyToOne(targetEntity = Alarm.class)
    public Alarm getAlarm() {
        return alarm;
    }

    public void setAlarm(Alarm alarm) {
        this.alarm = alarm;
    }

    @NotNullOnPersist
    @ManyToOne(targetEntity = Rule.class)
    public Rule getEvent() {
        return event;
    }

    public void setEvent(Rule event) {
        this.event = event;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }
}
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

import java.util.Date;

public class AlarmEventStatus {
    private long alarmEventId;
    private int severity;
    private String alarmEventName;
    private String ruleDefinition;
    private String description;
    private boolean fired;
    private Date lastFiredTimestamp;

    public AlarmEventStatus(AlarmEvent alarmEvent, Date lastFiredTimestamp, boolean fired) {
        this.alarmEventId = alarmEvent.getId();
        this.alarmEventName = alarmEvent.getEvent().getName();
        this.severity = alarmEvent.getSeverity();
        this.ruleDefinition = alarmEvent.getEvent().getRulePrettyDefinition();
        this.description = alarmEvent.getEvent().getDescription();
        this.fired = fired;
        this.lastFiredTimestamp = lastFiredTimestamp;
    }

    public long getAlarmEventId() {
        return alarmEventId;
    }

    public String getAlarmEventName() {
        return alarmEventName;
    }

    public boolean isFired() {
        return fired;
    }

    public int getSeverity() {
        return severity;
    }

    public String getRuleDefinition() {
        return ruleDefinition;
    }

    public String getDescription() {
        return description;
    }

    public Date getLastFiredTimestamp() {
        return lastFiredTimestamp;
    }
}

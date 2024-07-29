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

import java.util.List;

public class AlarmStatus {
    private long alarmId;
    private String alarmName;
    private List<AlarmEventStatus> alarmEvents;

    public AlarmStatus(long alarmId, String alarmName, List<AlarmEventStatus> alarmEvents) {
        this.alarmId = alarmId;
        this.alarmName = alarmName;
        this.alarmEvents = alarmEvents;
    }

    public long getAlarmId() {
        return alarmId;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public List<AlarmEventStatus> getAlarmEvents() {
        return alarmEvents;
    }
}

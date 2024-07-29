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

package it.acsoftware.hyperiot.storm.alarm;

/**
 * This class map a hbase cell
 */
public class HBaseAlarmCell {

    private String alarmName;
    private long timestamp;
    private boolean inhibited;
    private int severity;
    private AlarmState state;

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isInhibited() {
        return inhibited;
    }

    public void setInhibited(boolean inhibited) {
        this.inhibited = inhibited;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public AlarmState getState() {
        return state;
    }

    public void setState(AlarmState state) {
        this.state = state;
    }

}

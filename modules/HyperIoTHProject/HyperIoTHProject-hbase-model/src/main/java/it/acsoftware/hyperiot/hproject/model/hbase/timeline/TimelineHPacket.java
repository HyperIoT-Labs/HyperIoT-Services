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

package it.acsoftware.hyperiot.hproject.model.hbase.timeline;

import java.util.List;

@SuppressWarnings("unused")
public class TimelineHPacket {

    private List<TimelineHPacketField> fields;
    private String timestampField;

    public List<TimelineHPacketField> getFields() {
        return fields;
    }

    public void setFields(List<TimelineHPacketField> fields) {
        this.fields = fields;
    }

    public String getTimestampField() {
        return timestampField;
    }

    public void setTimestampField(String timestampField) {
        this.timestampField = timestampField;
    }

}

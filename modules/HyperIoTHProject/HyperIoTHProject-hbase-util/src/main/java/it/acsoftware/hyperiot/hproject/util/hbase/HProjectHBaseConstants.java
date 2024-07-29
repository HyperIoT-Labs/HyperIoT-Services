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

package it.acsoftware.hyperiot.hproject.util.hbase;

import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;

public final class HProjectHBaseConstants {

    private HProjectHBaseConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String ALARM_TABLE_NAME_PREFIX = "alarm_";

    public static final String ALARM_TABLE_DOWN_COLUMN_FAMILY = "DOWN";

    public static final String ALARM_TABLE_UP_COLUMN_FAMILY = "UP";

    public static final String ALARM_TABLE_HANDLED_COLUMN_FAMILY = "HANDLED";

    public static final String ALARM_EVENT_ID_IDENTIFIER = "0";
    public static final String EVENT_TABLE_NAME_PREFIX = "event_";
    public static final String HPROJECT_TABLE_NAME_PREFIX = "hproject_";
    public static final String EVENT_RULE_STATE_TABLE_NAME_PREFIX = "hproject_event_rule_state_";
    public static final String DAY_COLUMN_FAMILY = TimelineColumnFamily.DAY.getName();
    public static final String ERROR_COLUMN_FAMILY = "error";
    public static final String ERROR_MESSAGE_COLUMM = "message";
    public static final String ERROR_TABLE_NAME_PREFIX = "hproject_error_";
    public static final String EVENT_IDENTIFIER = "-1";
    public static final String ERROR_IDENTIFIER = "-2";
    public static final String EVENT_COLUMN = "eventColumn";
    public static final String EVENT_COLUMN_FAMILY = "event";
    public static final String HOUR_COLUMN_FAMILY = TimelineColumnFamily.HOUR.getName();
    public static final String HPACKET_COLUMN_FAMILY = "hpacket";
    public static final String HPACKET_ATTACHMENTS_COLUMN_FAMILY = "attachments";
    public static final String HDEVICE_COLUMN_FAMILY = "hdevice";
    public static final String RULE_COLUMN_FAMILY = "rule";
    public static final String MILLISECOND_COLUMN_FAMILY = TimelineColumnFamily.MILLISECOND.getName();
    public static final String MINUTE_COLUMN_FAMILY = TimelineColumnFamily.MINUTE.getName();
    public static final String MODEL_IDENTIFIER_SEPARATOR = ".";
    public static final String MONTH_COLUMN_FAMILY = TimelineColumnFamily.MONTH.getName();
    public static final String SECOND_COLUMN_FAMILY = TimelineColumnFamily.SECOND.getName();
    public static final String TIMELINE_TABLE_NAME_PREFIX = "timeline_hproject_";
    public static final String YEAR_COLUMN_FAMILY = TimelineColumnFamily.YEAR.getName();

}

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

package it.acsoftware.hyperiot.hproject.api.hbase.timeline;

import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.TreeMap;

public interface HProjectTimelineUtil extends Serializable {

    /**
     * Build JSON output for timeline
     * @param events Map containing timestamp and event number
     * @return JSON like this: [{"timestamp": 1579705026419, "value": 8}, ...]
     */
    String buildJsonOutput(TreeMap<Long, Long> events);

    /**
     * Get column value inside Day column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getDayColumnFamily(Instant instant);

    /**
     * Get column value inside hour column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getHourColumnFamily(Instant instant);

    /**
     * Retrieve step from HBase row key.
     * Step is value which will be inserted in output, while a retrieved HBase row key can contain
     * one or more granularity deeper levels.
     * @param key HBase row key
     * @param step Step value
     * @return HBase row key first part, depending on step
     * @throws ParseException ParseException
     */
    String getKeyFromStep(String key, TimelineColumnFamily step) throws ParseException;

    /**
     * Get column value inside millisecond column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getMillisecondColumnFamily(Instant instant);

    /**
     * Get column value inside minute column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getMinuteColumnFamily(Instant instant);

    /**
     * Get column value inside month column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getMonthColumnFamily(Instant instant);

    /**
     * This method build a String, which is a HBase row key prefix
     * @param modelIdentifier Identifier of model for which retrieves realizations (i.e. HPacket.<id>, Event etc)
     * @param step Timeline step
     * @param timestamp Timestamp which retrieves event number from
     * @return HBase row key prefix
     * @throws IOException IOException
     */
    String getRowKeyPrefix(String modelIdentifier, TimelineColumnFamily step, long timestamp) throws IOException;

    /**
     * Get column value inside second column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getSecondColumnFamily(Instant instant);

    /**
     * Return column bound in String format
     * @param instant Date representing bound
     * @param step Step
     * @return String representing column bound, depending on step value
     * @throws IOException IOException
     */
    String getStringColumnBound(Instant instant, TimelineColumnFamily step) throws IOException;

    /**
     *
     * @param stringTimestamp stringTimestamp
     * @param column column
     * @param step step
     * @return long
     * @throws ParseException ParseException
     */
    long getTimestamp(String stringTimestamp, byte[] column, TimelineColumnFamily step, SimpleDateFormat format) throws ParseException ;

    /**
     * Get column value inside year column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getYearColumnFamily(Instant instant);

    /**
     * It initializes output of timeline queries
     * @param events Output, containing key-value pairs: key are timestamps, values are HPacket events
     * @param step Scanning step
     * @param startTime Scanning start time
     * @param endTime Scanning end time
     * @param timezone Timezone of client which has invoked the method, i.e. Europe/Rome
     * @throws IOException IOException
     */
    void initializeEventMap(TreeMap<Long, Long> events, TimelineColumnFamily step, long startTime, long endTime,
                            String timezone) throws IOException;

}

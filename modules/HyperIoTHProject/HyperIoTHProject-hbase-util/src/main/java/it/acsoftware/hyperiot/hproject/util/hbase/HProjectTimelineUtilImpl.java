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

import it.acsoftware.hyperiot.hproject.api.hbase.timeline.HProjectTimelineUtil;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.TreeMap;

@Component(service = HProjectTimelineUtil.class, immediate = true)
public class HProjectTimelineUtilImpl implements HProjectTimelineUtil {

    private static final String DELIMITER = "_";

    @Override
    public String buildJsonOutput(TreeMap<Long, Long> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Iterator<Long> iterator = events.keySet().iterator();
        while (iterator.hasNext()) {
            long timestamp = iterator.next();
            sb.append("{\"timestamp\": ").append(timestamp).append(", \"value\": ").append(events.get(timestamp)).append("}");
            if (iterator.hasNext())
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Given a scanning step, it returns next scanning value
     *
     * @param localDateTime LocalDateTime which calculate next step from
     * @param step          Scanning step
     * @return Next LocalDateTime, depending on step
     * @throws IOException IOException
     */
    private LocalDateTime getCurrentTime(LocalDateTime localDateTime, TimelineColumnFamily step) throws IOException {
        switch (step) {
            case YEAR:
                return localDateTime.plusYears(1L);
            case MONTH:
                return localDateTime.plusDays(localDateTime.getMonth().length(localDateTime.toLocalDate().isLeapYear()));
            case DAY:
                return localDateTime.plusDays(1L);
            case HOUR:
                return localDateTime.plusHours(1L);
            case MINUTE:
                return localDateTime.plusMinutes(1L);
            case SECOND:
                return localDateTime.plusSeconds(1L);
            case MILLISECOND:
                return localDateTime.plusSeconds(1000L);
            default:
                throw new IOException("Unknown step");
        }
    }

    @Override
    public String getDayColumnFamily(Instant instant) {
        int day = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getDayOfMonth();
        return day >= 10 ? String.valueOf(day) : "0" + day;
    }

    @Override
    public String getHourColumnFamily(Instant instant) {
        int hour = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getHour();
        return hour >= 10 ? String.valueOf(hour) : "0" + hour;
    }

    @Override
    public String getKeyFromStep(String key, TimelineColumnFamily step) throws ParseException {
        switch (step) {
            case YEAR:
                return key.substring(0, 4);
            case MONTH:
                return key.substring(0, 7);
            case DAY:
                return key.substring(0, 10);
            case HOUR:
                return key.substring(0, 13);
            case MINUTE:
                return key.substring(0, 16);
            case SECOND:
                return key.substring(0, 19);
            case MILLISECOND:
                return key;
            default:
                throw new ParseException("Wrong key", 0);
        }
    }

    @Override
    public String getMillisecondColumnFamily(Instant instant) {
        int milli = (LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getNano()) / 1_000_000;
        if (milli >= 100) {
            return String.valueOf(milli);
        } else {
            if (milli >= 10)
                return "0" + milli;
            return "00" + milli;
        }
    }

    @Override
    public String getMinuteColumnFamily(Instant instant) {
        int minute = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getMinute();
        return minute >= 10 ? String.valueOf(minute) : "0" + minute;
    }

    @Override
    public String getMonthColumnFamily(Instant instant) {
        int month = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getMonthValue();
        return month >= 10 ? String.valueOf(month) : "0" + month;
    }

    @Override
    public String getRowKeyPrefix(String modelIdentifier, TimelineColumnFamily granularity, long timestamp)
        throws IOException {
        Instant instant = Instant.ofEpochMilli(timestamp);
        switch (granularity) {
            case YEAR:
                return modelIdentifier;
            case MONTH:
                return String.join(DELIMITER, modelIdentifier, getYearColumnFamily(instant));
            case DAY:
                return String.join(DELIMITER, modelIdentifier, getYearColumnFamily(instant), getMonthColumnFamily(instant));
            case HOUR:
                return String.join(DELIMITER, modelIdentifier, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                    getDayColumnFamily(instant));
            case MINUTE:
                return String.join(DELIMITER, modelIdentifier, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                    getDayColumnFamily(instant), getHourColumnFamily(instant));
            case SECOND:
                return String.join(DELIMITER, modelIdentifier, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                    getDayColumnFamily(instant), getHourColumnFamily(instant), getMinuteColumnFamily(instant));
            case MILLISECOND:
                return String.join(DELIMITER, modelIdentifier, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                    getDayColumnFamily(instant), getHourColumnFamily(instant), getMinuteColumnFamily(instant),
                    getSecondColumnFamily(instant));
            default:
                throw new IOException("Unexpected step received");
        }
    }

    @Override
    public String getSecondColumnFamily(Instant instant) {
        int second = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getSecond();
        return second >= 10 ? String.valueOf(second) : "0" + second;
    }

    @Override
    public String getStringColumnBound(Instant instant, TimelineColumnFamily step) throws IOException {
        switch (step) {
            case YEAR:
                return getYearColumnFamily(instant);
            case MONTH:
                return getMonthColumnFamily(instant);
            case DAY:
                return getDayColumnFamily(instant);
            case HOUR:
                return getHourColumnFamily(instant);
            case MINUTE:
                return getMinuteColumnFamily(instant);
            case SECOND:
                return getSecondColumnFamily(instant);
            case MILLISECOND:
                return getMillisecondColumnFamily(instant);
            default:
                throw new IOException("Unexpected step received");
        }
    }

    @Override
    public long getTimestamp(String stringTimestamp, byte[] column, TimelineColumnFamily step, SimpleDateFormat format) throws ParseException {
        stringTimestamp = stringTimestamp.substring(stringTimestamp.indexOf(DELIMITER) + 1);     // remove '<packetId>_', for sum up events on the same timestamp
        stringTimestamp = getKeyFromStep(stringTimestamp, step);
        return format.parse(stringTimestamp).getTime();   // millis in UnixEpochTime
    }

    @Override
    public String getYearColumnFamily(Instant instant) {
        return String.valueOf(LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getYear());
    }

    @Override
    public void initializeEventMap(TreeMap<Long, Long> events, TimelineColumnFamily step,
                                   long startTime, long endTime, String timezone) throws IOException {
        long currentTime = startTime;
        while (currentTime < endTime) {
            events.put(currentTime, 0L);
            // get current time in LocalDateTime format
            Instant instant = Instant.ofEpochMilli(currentTime);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(timezone));
            // get next LocalDateTime, depending on step value
            LocalDateTime nextLocalDateTime = getCurrentTime(localDateTime, step);
            // get time in millis
            currentTime = nextLocalDateTime
                .toInstant(    // get Instant object to obtain millis
                    // nextLocalDateTime is current time with given GMT,
                    // retrieve offset in order to get back time in UTC
                    ZoneId.of(timezone).getRules().getOffset(nextLocalDateTime))
                .toEpochMilli();
        }
    }

}

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

package it.acsoftware.hyperiot.storm.hbase.mapper;

import it.acsoftware.hyperiot.hproject.api.hbase.timeline.HProjectTimelineUtil;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;

import it.acsoftware.hyperiot.hproject.util.hbase.HProjectTimelineUtilImpl;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.storm.tuple.Tuple;

import java.time.Instant;

import static it.acsoftware.hyperiot.storm.util.StormConstants.STEP_FIELD;
import static it.acsoftware.hyperiot.storm.util.StormConstants.TIMESTAMP_FIELD;
import static org.apache.storm.hbase.common.Utils.toBytes;

@SuppressWarnings("unused")
public class TimelineHBaseMapper implements HBaseMapper {

    // HBase timeline bolt has to receive a tuple with three fields:
    // - rowKeyField, which is set in topology.yaml config method, refers to HBase row key
    // - timestamp value is HPacket timestamp
    // - step, HBase column family on which register increment
    private String rowKeyField;
    private final HProjectTimelineUtil hBaseConnectorTimelineUtil;

    public TimelineHBaseMapper() {
        hBaseConnectorTimelineUtil = new HProjectTimelineUtilImpl();
    }

    @Override
    public byte[] rowKey(Tuple tuple) {
        Object objVal = tuple.getValueByField(this.rowKeyField);
        return toBytes(objVal);
    }

    @Override
    public ColumnList columns(Tuple tuple) {
        long timestamp = (long) tuple.getValueByField(TIMESTAMP_FIELD);
        TimelineColumnFamily step = (TimelineColumnFamily) tuple.getValueByField(STEP_FIELD);
        ColumnList cols = new ColumnList();
        Instant instant = Instant.ofEpochMilli(timestamp);
        switch (step) {
            // we need increment, so add value 1 to HBase cell
            case YEAR:
                cols.addCounter(TimelineColumnFamily.YEAR.getName().getBytes(),
                    hBaseConnectorTimelineUtil.getYearColumnFamily(instant).getBytes(), 1);
                break;
            case MONTH:
                cols.addCounter(TimelineColumnFamily.MONTH.getName().getBytes(),
                    hBaseConnectorTimelineUtil.getMonthColumnFamily(instant).getBytes(), 1);
                break;
            case DAY:
                cols.addCounter(TimelineColumnFamily.DAY.getName().getBytes(),
                    hBaseConnectorTimelineUtil.getDayColumnFamily(instant).getBytes(), 1);
                break;
            case HOUR:
                cols.addCounter(TimelineColumnFamily.HOUR.getName().getBytes(),
                    hBaseConnectorTimelineUtil.getHourColumnFamily(instant).getBytes(), 1);
                break;
            case MINUTE:
                cols.addCounter(TimelineColumnFamily.MINUTE.getName().getBytes(),
                    hBaseConnectorTimelineUtil.getMinuteColumnFamily(instant).getBytes(), 1);
                break;
            case SECOND:
                cols.addCounter(TimelineColumnFamily.SECOND.getName().getBytes(),
                    hBaseConnectorTimelineUtil.getSecondColumnFamily(instant).getBytes(), 1);
                break;
            case MILLISECOND:
                cols.addCounter(TimelineColumnFamily.MILLISECOND.getName().getBytes(),
                    hBaseConnectorTimelineUtil.getMillisecondColumnFamily(instant).getBytes(), 1);
                break;
            default:
                break;
        }
        return cols;
    }

    /**
     * This is a config method, it is called in topology.yaml file and set tuple field containing
     * HBase row key
     * @param rowKeyField Tuple field containing HBase row key
     * @return TimelineHBaseMapper
     */
    public TimelineHBaseMapper withRowKeyField(String rowKeyField) {
        this.rowKeyField = rowKeyField;
        return this;
    }

}

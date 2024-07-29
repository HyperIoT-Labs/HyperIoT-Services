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

package it.acsoftware.hyperiot.storm.runtime.bolt.util;

import it.acsoftware.hyperiot.hproject.api.hbase.timeline.HProjectTimelineUtil;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectTimelineUtilImpl;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.tuple.Values;

import java.io.IOException;

import static it.acsoftware.hyperiot.storm.util.StormConstants.TIMELINE_HPROJECT_STREAM_ID_PREFIX;

public final class TimelineUtil {

    private static final HProjectTimelineUtil hProjectTimelineUtil;

    static {
        hProjectTimelineUtil = new HProjectTimelineUtilImpl();
    }

    private TimelineUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Send tuples to HBase, supporting timeline queries.
     * Timeline table contains counting about how many times Storm receives an HPacket, on the basis of its timestamp.
     * We send one tuple for each step (see it.acsoftware.hyperiot.hbase.connector.model.TimelineColumnFamily enum for its values).
     * For every emitted tuple, there is a composed row key.
     * For example, on January 2020, the 22th, Storm receive tuple about HPacket with id 156 or a generic event.
     * One of the emitted tuples contains the following row key: HPacket.156_2020_1 / Event_2020_1.
     * On HBase, we increment cell value with column day:22
     *
     * @param collector OutputCollector
     * @param rowKeyBeginning How row key begins (i.e. HPacket.<id>, Event or Error)
     * @param hProjectId ID of HProject
     * @param timestamp Timestamp
     */
    public static void emitTuplesToTimelineTable(OutputCollector collector, String rowKeyBeginning, long hProjectId, long timestamp)
            throws IOException {
        // emit to timeline table, which counts model (i.e. HPacket or Event) instances
        String timelineTableStreamId = String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, hProjectId);
        collector.emit(timelineTableStreamId, new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning,
                TimelineColumnFamily.YEAR, timestamp), timestamp, TimelineColumnFamily.YEAR));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.MONTH,
                        timestamp), timestamp, TimelineColumnFamily.MONTH));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.DAY,
                        timestamp), timestamp, TimelineColumnFamily.DAY));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.HOUR,
                        timestamp), timestamp, TimelineColumnFamily.HOUR));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.MINUTE,
                        timestamp), timestamp, TimelineColumnFamily.MINUTE));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.SECOND,
                        timestamp), timestamp, TimelineColumnFamily.SECOND));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.MILLISECOND,
                        timestamp), timestamp, TimelineColumnFamily.MILLISECOND));
    }

    public static void emitTuplesToTimelineTable(BasicOutputCollector collector, String rowKeyBeginning, long hProjectId, long timestamp)
            throws IOException {
        // emit to timeline table, which counts model (i.e. HPacket or Event) instances
        String timelineTableStreamId = String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, hProjectId);
        collector.emit(timelineTableStreamId, new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning,
                TimelineColumnFamily.YEAR, timestamp), timestamp, TimelineColumnFamily.YEAR));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.MONTH,
                        timestamp), timestamp, TimelineColumnFamily.MONTH));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.DAY,
                        timestamp), timestamp, TimelineColumnFamily.DAY));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.HOUR,
                        timestamp), timestamp, TimelineColumnFamily.HOUR));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.MINUTE,
                        timestamp), timestamp, TimelineColumnFamily.MINUTE));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.SECOND,
                        timestamp), timestamp, TimelineColumnFamily.SECOND));
        collector.emit(timelineTableStreamId,
                new Values(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, TimelineColumnFamily.MILLISECOND,
                        timestamp), timestamp, TimelineColumnFamily.MILLISECOND));
    }

}

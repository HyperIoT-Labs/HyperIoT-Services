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

package it.acsoftware.hyperiot.hproject.api.hbase;

import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmHBaseResult;
import it.acsoftware.hyperiot.hproject.model.hbase.HPacketCount;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineElement;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.List;
import java.util.function.Consumer;

public interface HProjectHBaseSystemApi extends HyperIoTBaseSystemApi {

    /**
     * Given an HProject ID and a list of HPacket IDs inside it, this method scan Avro HPackets between a start time
     * and an end time, stored in an HBase table.
     * @param hProjectId HProject ID
     * @param hPacketIds HPacket list
     * @param hDeviceIds HDevice list
     * @param alarmState Alarm State
     * @param rowKeyLowerBound Scanning start time (i.e. an HBase row key)
     * @param rowKeyUpperBound Scanning end time (i.e. an HBase row key)
     * @param limit limit to results
     * @param outputStream Stream on which it sends data
     * @throws IOException IOException
     */
    void scanHProject(long hProjectId, List<String> hPacketIds, List<String> hDeviceIds, long rowKeyLowerBound, long rowKeyUpperBound, int limit,
                      String alarmState, OutputStream outputStream)
            throws IOException;

    /**
     *
     * @param hProjectId
     * @param packetId
     * @param rowKeyLowerBound
     * @param rowKeyUpperBound
     * @param function
     */
    void scanHProject(long hProjectId, long packetId, long rowKeyLowerBound, long rowKeyUpperBound, Consumer<HPacket> function);

    /**
     * It counts HPacket event number between start time and end time, depending on pagination delta,
     * which must be lower or equal than HBase max scan page size
     * @param projectId HProject ID
     * @param packetIds List of HPacket IDs, which count event number for
     * @param deviceIds List of HDevice IDs, which count event number for
     * @param startTime Scanning start time
     * @param endTime Scanning end time
     * @return A list of HPacketCount
     * @throws IOException IOException
     * @throws ParseException ParseException
     */
    List<HPacketCount> timelineEventCount(long projectId, List<String> packetIds, List<String> deviceIds, long startTime, long endTime)
            throws Throwable;

    /**
     * Service scans and returns data from timeline table
     * @param tableName Table name
     * @param packetIds Packet IDs
     * @param deviceIds Device IDs
     * @param step Step
     * @param startTime Timeline start time
     * @param endTime Timeline end time
     * @param timezone Timezone of client which has invoked the method, i.e. Europe/Rome
     * @return TimelineElement list
     * @throws Exception Exception
     */
    List<TimelineElement> timelineScan(String tableName, List<String> packetIds, List<String> deviceIds, TimelineColumnFamily step,
                                       long startTime, long endTime, String timezone)
            throws Exception;

    /**
     * It returns output of algorithm which has been defined for a project
     * @param projectId Project ID
     * @param hProjectAlgorithmId HProjectAlgorithm ID
     * @return HProjectAlgorithmHBaseResult
     */
    HProjectAlgorithmHBaseResult getAlgorithmOutputs(long projectId, long hProjectAlgorithmId) throws IOException;

    /**
     * Returns specific hpacket attachment
     * @param hProjectId
     * @param packetId
     * @param fieldId
     * @param rowKeyLowerBound
     * @param rowKeyUpperBound
     * @return
     * @throws IOException
     */
    byte[] getHPacketAttachment(long hProjectId,long packetId, long fieldId,  long rowKeyLowerBound, long rowKeyUpperBound) throws IOException;


}

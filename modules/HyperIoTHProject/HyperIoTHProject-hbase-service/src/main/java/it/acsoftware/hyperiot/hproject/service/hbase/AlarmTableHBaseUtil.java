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

package it.acsoftware.hyperiot.hproject.service.hbase;

import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.model.hbase.HProjectScan;
import it.acsoftware.hyperiot.hproject.util.hbase.AlarmState;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectHBaseConstants;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author Francesco Salerno.
 * This is a utility class that define how to query  HProject's alarm HBase table.
 */
public class AlarmTableHBaseUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlarmTableHBaseUtil.class);


    private AlarmTableHBaseUtil(){

    }


    private static  String getColumnFamilyByAlarmState( AlarmState alarmState){
        if(alarmState.equals(AlarmState.UP)){
            return HProjectHBaseConstants.ALARM_TABLE_UP_COLUMN_FAMILY ;
        } else if (alarmState.equals(AlarmState.DOWN)){
            return HProjectHBaseConstants.ALARM_TABLE_DOWN_COLUMN_FAMILY ;
        } else if (alarmState.equals(AlarmState.HANDLED)){
            return HProjectHBaseConstants.ALARM_TABLE_HANDLED_COLUMN_FAMILY;
        }
        throw new HyperIoTRuntimeException("Error. Not supported Alarm State");
    }


    static HProjectScan retrieveAllAlarmByAlarmState(HBaseConnectorSystemApi hBaseConnectorSystemApi, HPacketDeserializer hPacketDeserializer, List<Long> devicesId, String tableName,
                                                     byte[] rowKeyLowerBound, byte[] rowKeyUpperBound, AlarmState alarmState) throws IOException {
        HProjectScan hProjectScan = new HProjectScan(Long.parseLong(HProjectHBaseConstants.ALARM_EVENT_ID_IDENTIFIER));
        // specify column families and columns on which perform scan
        byte[] alarmColumnFamily = Bytes.toBytes(getColumnFamilyByAlarmState(alarmState));
        List<byte[]> columnQualifierList = new ArrayList<>();
        //Retrieve columns qualifier from deviceId.
        for( Long deviceId : devicesId){
            columnQualifierList.add(Bytes.toBytes(deviceId));
        }
        Map<byte[], List<byte[]>> targetColumns = new HashMap<>();
        targetColumns.put(alarmColumnFamily, columnQualifierList);
        List<Result> hBaseResults = hBaseConnectorSystemApi.scanWithCompleteResult(tableName, targetColumns,
                rowKeyLowerBound, rowKeyUpperBound,0);
        // construct frontend output
        addAlarmValueToScanResults(hBaseResults, alarmColumnFamily, targetColumns.get(alarmColumnFamily), hProjectScan, hPacketDeserializer);
        if (!hBaseResults.isEmpty())
            hProjectScan.setRowKeyUpperBound(Bytes.toLong(hBaseResults.get(hBaseResults.size() - 1).getRow()));
        return hProjectScan;
    }

    static HProjectScan retrieveAllAlarmByDeviceIdAndAlarmState(HBaseConnectorSystemApi hBaseConnectorSystemApi, HPacketDeserializer hPacketDeserializer,
            String deviceId, String tableName, byte[] rowKeyLowerBound, byte[] rowKeyUpperBound, String alarmState )
            throws IOException {
        long hDeviceId = Long.parseLong(deviceId);
        //Packet id for alarm event is set to 0 .
        HProjectScan hProjectScan = new HProjectScan(Long.parseLong(HProjectHBaseConstants.ALARM_EVENT_ID_IDENTIFIER));
        AlarmState alarmStateFilter = (alarmState == null || alarmState.isBlank() ) ? null : AlarmState.fromString(alarmState);
        byte[] columnQualifier = Bytes.toBytes(hDeviceId);
        Map<byte[], List<byte[]>> targetColumns = getScannerColumnsForAlarmsTable(columnQualifier, alarmStateFilter);
        List<Result> hBaseResults = hBaseConnectorSystemApi.scanWithCompleteResult(tableName, targetColumns,
                rowKeyLowerBound, rowKeyUpperBound,0);
        // construct frontend output
        addAlarmValueToScanResults(hBaseResults, targetColumns.keySet(), columnQualifier, hProjectScan, hPacketDeserializer);
        if (!hBaseResults.isEmpty())
            hProjectScan.setRowKeyUpperBound(Bytes.toLong(hBaseResults.get(hBaseResults.size() - 1).getRow()));
        return hProjectScan;
    }

    static Map<byte[], List <byte[]>> getScannerColumnsForAlarmsTable( byte[] columnQualifier, AlarmState alarmState) {
        Map<byte[], List<byte[]>> targetColumns = new HashMap<>();
        if(alarmState == null ) {
            byte[] columnFamilyUp = Bytes.toBytes(HProjectHBaseConstants.ALARM_TABLE_UP_COLUMN_FAMILY);
            targetColumns.put(columnFamilyUp, new ArrayList<>());
            targetColumns.get(columnFamilyUp).add(columnQualifier);
            byte[] columnFamilyDown = Bytes.toBytes(HProjectHBaseConstants.ALARM_TABLE_DOWN_COLUMN_FAMILY);
            targetColumns.put(columnFamilyDown, new ArrayList<>());
            targetColumns.get(columnFamilyDown).add(columnQualifier);
            byte[] columnFamilyHandled = Bytes.toBytes(HProjectHBaseConstants.ALARM_TABLE_HANDLED_COLUMN_FAMILY);
            targetColumns.put(columnFamilyHandled, new ArrayList<>());
            targetColumns.get(columnFamilyHandled).add(columnQualifier);
        } else {
            byte[] columnFamily = Bytes.toBytes(getColumnFamilyByAlarmState(alarmState));
            targetColumns.put(columnFamily, new ArrayList<>());
            targetColumns.get(columnFamily).add(columnQualifier);
        }
        return targetColumns;
    }


    static void addAlarmValueToScanResults(List<Result> hBaseResults, Set<byte[]> columnFamilies, byte[] alarmColumnQualifier, HProjectScan hProjectScan, HPacketDeserializer hPacketDeserializer)
            throws IOException {
        for (Result result : hBaseResults) {
            for(byte[] columnFamily : columnFamilies) {
                if(result.containsNonEmptyColumn(columnFamily, alarmColumnQualifier)) {
                    hProjectScan.addValue(decodeAvroHPacket(hPacketDeserializer, result.getValue(columnFamily, alarmColumnQualifier)));
                    //pass to the next result.
                    break;
                }
            }
        }
    }

    static void addAlarmValueToScanResults(List<Result> hbaseResults , byte[] columnFamily, List<byte[]> columnQualifierList
            , HProjectScan hProjectScan, HPacketDeserializer hPacketDeserializer) throws IOException {
        for (Result result : hbaseResults) {
            for (byte[] columnQualifier : columnQualifierList){
                if(result.containsNonEmptyColumn(columnFamily, columnQualifier)) {
                    hProjectScan.addValue(decodeAvroHPacket(hPacketDeserializer, result.getValue(columnFamily, columnQualifier)));
                    //pass to the next result.
                    break;
                }
            }
        }
    }

    private static HPacket decodeAvroHPacket(HPacketDeserializer hPacketDeserializer, byte[] avroHPacket) throws IOException {
        return hPacketDeserializer.deserialize(avroHPacket, null);
    }

}

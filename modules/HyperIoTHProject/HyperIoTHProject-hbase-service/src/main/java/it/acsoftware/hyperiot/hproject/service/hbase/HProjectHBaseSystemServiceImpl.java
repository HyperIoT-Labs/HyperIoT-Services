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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.algorithm.model.AlgorithmIOField;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketDataExportInterruptedException;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmRepository;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmHBaseResult;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.hproject.api.hbase.HProjectHBaseSystemApi;
import it.acsoftware.hyperiot.hproject.api.hbase.timeline.HProjectTimelineUtil;
import it.acsoftware.hyperiot.hproject.deserialization.api.HPacketDeserializer;
import it.acsoftware.hyperiot.hproject.deserialization.service.JsonAvroHPacketDeserializer;
import it.acsoftware.hyperiot.hproject.model.ModelType;
import it.acsoftware.hyperiot.hproject.model.hbase.HPacketCount;
import it.acsoftware.hyperiot.hproject.model.hbase.HProjectScan;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineElement;
import it.acsoftware.hyperiot.hproject.util.hbase.AlarmState;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectHBaseConstants;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static it.acsoftware.hyperiot.hproject.model.hbase.HProjectScan.PACKET_COLUMN;
import static it.acsoftware.hyperiot.hproject.model.hbase.HProjectScan.RECEIVED_PACKET_COLUMN;
import static java.time.temporal.TemporalAdjusters.firstDayOfYear;

@Component(service = HProjectHBaseSystemApi.class, immediate = true)
public class HProjectHBaseSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements HProjectHBaseSystemApi {
    private Logger log = LoggerFactory.getLogger(HProjectHBaseSystemServiceImpl.class);
    private HPacketDeserializer hPacketDeserializer;

    private HDeviceRepository hDeviceRepository;

    /**
     * Injectinh AlgorithmRepository
     */
    private HProjectAlgorithmRepository algorithmRepository;
    /**
     * Injecting HProjectRepository
     */
    private HProjectRepository hProjectRepository;

    /**
     * Injecting HBaseConnectorSystemApi
     */
    private HBaseConnectorSystemApi hBaseConnectorSystemApi;
    /**
     * Injecting HBaseConnectorSystemApi
     */
    private HProjectTimelineUtil hProjectTimelineUtil;
    /**
     * This map will contain one date formatter for each timeline step (year, month, day, hour, minute, second, millisecond).
     * Formatters are created everytime they are requested. We cannot instantiate them on class preparation before SimpleDateFormat
     * is not thread safe
     * See HBase row keys for further information
     */
    private Map<TimelineColumnFamily, Callable<SimpleDateFormat>> formats;
    /**
     * This map supports timeline query, giving prefix of rows to be searched
     */
    private Map<String, String> rowKeyPrefixes;

    private Map<String, String> tablePrefixes;
    private Map<String, String> columnFamilies;
    private Map<String, String> columns;

    private ObjectMapper objectMapper;

    /**
     * On Bundle activated
     */
    @Activate
    public void onActivate() {
        //set formats for timeline timestamp
        setFormats();
        setRowKeyPrefixes();
        setTablePrefixes();
        setColumnFamilies();
        setColumns();
        objectMapper = new ObjectMapper();
        hPacketDeserializer = JsonAvroHPacketDeserializer.getInstance();
    }

    private void addValueToScanResults(String packetId, List<Result> hBaseResults, byte[] columnFamily, byte[] column, HProjectScan hProjectScan) throws IOException {
        if (packetId.equals(HProjectHBaseConstants.ERROR_IDENTIFIER)) {
            for (Result result : hBaseResults) {
                // error
                HashMap<String, Object> error = objectMapper.readValue(new String(result.getValue(columnFamily, column), StandardCharsets.UTF_8), HashMap.class);
                String packetAsString = new String(result.getValue(columnFamily, Bytes.toBytes(RECEIVED_PACKET_COLUMN)), StandardCharsets.UTF_8);
                try {
                    Object packet = objectMapper.readValue(packetAsString, Object.class);
                    error.put(PACKET_COLUMN, objectMapper.writeValueAsString(packet));
                } catch (Exception e) {
                    log.warn("Error while trying to read data as JSON, returning a string", e.getMessage());
                    error.put(PACKET_COLUMN, packetAsString);
                }
                hProjectScan.addValue(error, result.rawCells()[0].getTimestamp());
            }
        } else {
            for (Result result : hBaseResults) {
                // packet or event
                hProjectScan.addValue(decodeAvroHPacket(result.getValue(columnFamily, column)));
            }
        }
    }

    private void countOnHBase(List<HPacketCount> countList, String tableName, String hPacketId, long startTime, long endTime) throws Throwable {
        HPacketCount hBaseConnectorHPacketCount = new HPacketCount();
        hBaseConnectorHPacketCount.setHPacketId(Long.parseLong(hPacketId));
        byte[] rowKeyLowerBound;
        byte[] rowKeyUpperBound;
        if (hPacketId.equals(HProjectHBaseConstants.EVENT_IDENTIFIER)) {
            rowKeyLowerBound = serializeTimeStampFieldAsString(startTime, false);
            rowKeyUpperBound = serializeTimeStampFieldAsString(endTime, true);
        } else {
            rowKeyLowerBound = Bytes.toBytes(startTime);
            rowKeyUpperBound = Bytes.toBytes(endTime);
        }
        byte[] columnFamily = getColumnFamily(hPacketId);
        byte[] column = getColumn(hPacketId);

        long totalCount = hBaseConnectorSystemApi.rowCount(tableName, columnFamily, column, rowKeyLowerBound, rowKeyUpperBound);
        hBaseConnectorHPacketCount.setTotalCount(totalCount);
        countList.add(hBaseConnectorHPacketCount);
    }

    private void countAlarmOnHBase(List<HPacketCount> countList, String tableName, String hDeviceId, long startTime, long endTime) throws Throwable {
        //0 is the id of the packet related to the event defined on alarm.
        HPacketCount hbaseConnectorHPacketCount = new HPacketCount();
        hbaseConnectorHPacketCount.sethDeviceId(Long.parseLong(hDeviceId));
        long deviceId = Long.parseLong(hDeviceId);
        byte[] rowKeyLowerBound = Bytes.toBytes(startTime);
        byte[] rowKeyUpperBound = Bytes.toBytes(endTime);
        //Count alarm related to the device and not include alarm state in the query.
        byte[] alarmUpColumnFamily = Bytes.toBytes(HProjectHBaseConstants.ALARM_TABLE_UP_COLUMN_FAMILY);
        byte[] alarmDownColumnFamily = Bytes.toBytes(HProjectHBaseConstants.ALARM_TABLE_DOWN_COLUMN_FAMILY);
        byte[] alarmHandledColumnFamily = Bytes.toBytes(HProjectHBaseConstants.ALARM_TABLE_HANDLED_COLUMN_FAMILY);
        byte[] alarmTableQualifier = Bytes.toBytes(deviceId);
        Map<byte[], List<byte[]>> targetColumns = new HashMap<>();
        targetColumns.put(alarmUpColumnFamily, new ArrayList<>());
        targetColumns.get(alarmUpColumnFamily).add(alarmTableQualifier);
        targetColumns.put(alarmDownColumnFamily, new ArrayList<>());
        targetColumns.get(alarmDownColumnFamily).add(alarmTableQualifier);
        targetColumns.put(alarmHandledColumnFamily, new ArrayList<>());
        targetColumns.get(alarmHandledColumnFamily).add(alarmTableQualifier);
        long totalCount = hBaseConnectorSystemApi.rowCount(tableName, targetColumns, rowKeyLowerBound, rowKeyUpperBound);
        hbaseConnectorHPacketCount.setTotalCount(totalCount);
        countList.add(hbaseConnectorHPacketCount);
    }

    private HPacket decodeAvroHPacket(byte[] avroHPacket) throws IOException {
        return hPacketDeserializer.deserialize(avroHPacket, null);
    }

    /**
     * Extract rows from HBase
     *
     * @param events
     * @param result
     * @param hasMoreElements
     * @param startRowKeyPrefix
     * @param endRowKeyPrefix
     * @param step
     * @param startBoundColumn
     * @param endBoundColumn
     * @throws Exception
     */
    private void extractRowFromHBase(TreeMap<Long, Long> events, Result result, boolean hasMoreElements, byte[] startRowKeyPrefix, byte[] endRowKeyPrefix, TimelineColumnFamily step, byte[] startBoundColumn, byte[] endBoundColumn) throws Exception {
        Map<byte[], byte[]> columnMap = getColumnMap(result, startRowKeyPrefix, endRowKeyPrefix, step, startBoundColumn, endBoundColumn, hasMoreElements);
        for (byte[] column : columnMap.keySet())
            putInsideMap(events, result, column, columnMap, step);
    }

    private Map<byte[], byte[]> getColumnMap(Result result, byte[] startRowKeyPrefix, byte[] endRowKeyPrefix, TimelineColumnFamily granularity, byte[] startBoundColumn, byte[] endBoundColumn, boolean hasMoreElements) {
        Map<byte[], byte[]> columnMap;
        String rowString = Bytes.toString(result.getRow());
        byte[] columnFamily = Bytes.toBytes(granularity.getName());
        if (rowString.equals(Bytes.toString(startRowKeyPrefix))) {
            // Check on the first value: if its row key is equal to received start time
            // and some of its columns are before that, exclude them
            columnMap = Bytes.toString(startRowKeyPrefix).equals(Bytes.toString(endRowKeyPrefix)) ? result.getFamilyMap(columnFamily).subMap(startBoundColumn, true, endBoundColumn, false) : result.getFamilyMap(columnFamily).tailMap(startBoundColumn);
        } else {
            // Check on the last value and received end time:
            //  - it is not the last value => get all columns;
            //  - it is the last value and its row key is not equal to end time => get all columns
            //  - it is the last value and its row key is equal to end time => some of is columns
            //    can be after end time, exclude them
            //  - it is not the last value and its row key is equal to end time => it can't be exist,
            //    because row key contains all value between start and end times (both of them included)
            columnMap = hasMoreElements || !rowString.equals(Bytes.toString(endRowKeyPrefix)) ? result.getFamilyMap(columnFamily) : result.getFamilyMap(columnFamily).headMap(endBoundColumn, false);
        }
        return columnMap;
    }

    private String getRowKeyPrefix(String hPacketId) {
        if (rowKeyPrefixes.containsKey(hPacketId)) return rowKeyPrefixes.get(hPacketId);
        return ModelType.HPACKET.getSimpleName() + HProjectHBaseConstants.MODEL_IDENTIFIER_SEPARATOR + hPacketId;
    }

    private String getRowKeyPrefixForAlarmEntryInTimelineTable(String deviceId) {
        //TODO Alarm prefix can be defined as a constant in HProjectHBaseConstants class.
        return "Alarm" + HProjectHBaseConstants.MODEL_IDENTIFIER_SEPARATOR + deviceId;
    }

    private String getTableNamePrefix(String hPacketId) {
        if (tablePrefixes.containsKey(hPacketId)) return tablePrefixes.get(hPacketId);
        return HProjectHBaseConstants.HPROJECT_TABLE_NAME_PREFIX;
    }

    private byte[] getColumnFamily(String hPacketId) {
        if (columnFamilies.containsKey(hPacketId)) return Bytes.toBytes(columnFamilies.get(hPacketId));
        return Bytes.toBytes(HProjectHBaseConstants.HPACKET_COLUMN_FAMILY);
    }

    private byte[] getColumn(String hPacketId) {
        if (columns.containsKey(hPacketId)) return Bytes.toBytes(columns.get(hPacketId));
        return Bytes.toBytes(Long.parseLong(hPacketId));
    }

    private Map<byte[], List<byte[]>> getScannerColumns(String packetId, byte[] columnFamily, byte[] column) {
        Map<byte[], List<byte[]>> targetColumns = new HashMap<>();
        targetColumns.put(columnFamily, new ArrayList<>());
        targetColumns.get(columnFamily).add(column);
        if (packetId.equals(HProjectHBaseConstants.ERROR_IDENTIFIER)) {
            // in case of error, add packet alongside message error
            targetColumns.get(columnFamily).add(Bytes.toBytes(RECEIVED_PACKET_COLUMN));
        }
        return targetColumns;
    }

    private void putInsideMap(TreeMap<Long, Long> events, Result result, byte[] column, Map<byte[], byte[]> columnMap, TimelineColumnFamily step) throws Exception {
        SimpleDateFormat dateFormat = formats.get(step).call();
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // set UTC timezone
        String stringTimestamp = Bytes.toString(result.getRow()) + "_" + Bytes.toString(column);
        long key = hProjectTimelineUtil.getTimestamp(stringTimestamp, column, step, dateFormat);
        key = events.floorEntry(key).getKey();           // bind to entry of event map
        events.put(key, events.get(key) + Bytes.toLong(columnMap.get(column)));
    }

    private void hBaseTraversing(String tableName, TreeMap<Long, Long> events, String rowKeyBeginning, TimelineColumnFamily step, long startTime, long endTime) throws Exception {

        // Get HBase scanner and its iterator

        // get first possible row key
        byte[] startRowKeyPrefix = Bytes.toBytes(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, step, startTime));
        // get last possible row key
        byte[] endRowKeyPrefix = Bytes.toBytes(hProjectTimelineUtil.getRowKeyPrefix(rowKeyBeginning, step, endTime));
        // specify column families and columns on which perform scan
        Map<byte[], List<byte[]>> cols = new HashMap<>();
        byte[] columnFamily = Bytes.toBytes(step.getName());
        cols.put(columnFamily, null);
        // get lower bound from startTime
        Instant instant = Instant.ofEpochMilli(startTime);
        // get first possible column
        byte[] startBoundColumn = Bytes.toBytes(hProjectTimelineUtil.getStringColumnBound(instant, step));
        // Remember on the previous variable: if we receive a request with step second,
        // a permitted startBoundColumn value can be 15: it is the first second which we retrieve elements from

        // get upper bound from endTime
        instant = Instant.ofEpochMilli(endTime);
        // get last possible column
        byte[] endBoundColumn = Bytes.toBytes(hProjectTimelineUtil.getStringColumnBound(instant, step));
        hBaseConnectorSystemApi.iterateOverResults(tableName, cols, startRowKeyPrefix, endRowKeyPrefix, -1, (result, hasMoreElements) -> {
            try {
                extractRowFromHBase(events, result, hasMoreElements, startRowKeyPrefix, endRowKeyPrefix, step, startBoundColumn, endBoundColumn);
            } catch (Exception e) {
                getLog().error(e.getMessage(), e);
            }
        });
    }

    @Override
    public void scanHProject(long hProjectId, List<String> hPacketIds, List<String> hDeviceIds, long rowKeyLowerBound, long rowKeyUpperBound, int limit, String alarmState, OutputStream outputStream) throws IOException {
        try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(outputStream)) {
            jsonGenerator.writeStartArray();
            for (String packetId : hPacketIds) {
                String tableName = getTableNamePrefix(packetId) + hProjectId;
                byte[] rowKeyLowBound;
                byte[] rowKeyUppBound;
                if (packetId.equals(HProjectHBaseConstants.EVENT_IDENTIFIER)) {
                    rowKeyLowBound = serializeTimeStampFieldAsString(rowKeyLowerBound, false);
                    rowKeyUppBound = serializeTimeStampFieldAsString(rowKeyUpperBound, true);
                } else {
                    rowKeyLowBound = Bytes.toBytes(rowKeyLowerBound);
                    rowKeyUppBound = Bytes.toBytes(rowKeyUpperBound);
                }
                writeJsonObject(packetId, tableName, rowKeyLowBound, rowKeyUppBound, limit, objectMapper, jsonGenerator);
            }
            if (hDeviceIds.isEmpty()) {
                if (alarmState != null && !alarmState.isEmpty()) {
                    AlarmState alarmStateFilter = AlarmState.fromString(alarmState);
                    String tableName = HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + hProjectId;
                    byte[] rowKeyLowBound = Bytes.toBytes(rowKeyLowerBound);
                    byte[] rowKeyUppBound = Bytes.toBytes(rowKeyUpperBound);
                    Collection<HDevice> projectDevices = this.hDeviceRepository.getProjectDevicesList(hProjectId);
                    if (projectDevices != null && !projectDevices.isEmpty()) {
                        List<Long> deviceIds = new ArrayList<>();
                        for (HDevice device : projectDevices) {
                            deviceIds.add(device.getId());
                        }
                        HProjectScan hProjectScan = AlarmTableHBaseUtil.retrieveAllAlarmByAlarmState(hBaseConnectorSystemApi, hPacketDeserializer, deviceIds, tableName, rowKeyLowBound, rowKeyUppBound, alarmStateFilter);
                        objectMapper.writeValue(jsonGenerator, hProjectScan);
                    }
                }
            } else {
                for (String deviceId : hDeviceIds) {
                    String tableName = HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + hProjectId;
                    byte[] rowKeyLowBound = Bytes.toBytes(rowKeyLowerBound);
                    byte[] rowKeyUppBound = Bytes.toBytes(rowKeyUpperBound);
                    HProjectScan hProjectScan = AlarmTableHBaseUtil.retrieveAllAlarmByDeviceIdAndAlarmState(hBaseConnectorSystemApi, hPacketDeserializer, deviceId, tableName, rowKeyLowBound, rowKeyUppBound, alarmState);
                    objectMapper.writeValue(jsonGenerator, hProjectScan);
                }
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.flush();
        }
    }

    public void scanHProject(long hProjectId, long packetId, long rowKeyLowerBound, long rowKeyUpperBound, Consumer<HPacket> function) {
        String packetIdsStr = String.valueOf(packetId);
        String tableName = getTableNamePrefix(packetIdsStr) + hProjectId;
        // specify column families and columns on which perform scan
        byte[] columnFamily = getColumnFamily(packetIdsStr);
        byte[] column = getColumn(packetIdsStr);
        byte[] rowKeyLowBound = Bytes.toBytes(rowKeyLowerBound);
        byte[] rowKeyUppBound = Bytes.toBytes(rowKeyUpperBound);
        Map<byte[], List<byte[]>> targetColumns = getScannerColumns(String.valueOf(packetId), columnFamily, column);
        try {
            ResultScanner rs = hBaseConnectorSystemApi.getScanner(tableName, targetColumns, rowKeyLowBound, rowKeyUppBound, -1);
            Iterator<Result> it = rs.iterator();
            while (it.hasNext()) {
                Result res = it.next();
                try {
                    HPacket packet = decodeAvroHPacket(res.getValue(columnFamily, column));
                    function.accept(packet);
                } catch (HPacketDataExportInterruptedException hEx) {
                    log.warn("Export has been interrupted, exiting...");
                    return;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void writeJsonObject(String packetId, String tableName, byte[] rowKeyLowerBound, byte[] rowKeyUpperBound, int limit, ObjectMapper objectMapper, JsonGenerator jsonGenerator) throws IOException {
        long hPacketId = Long.parseLong(packetId);
        HProjectScan hProjectScan = new HProjectScan(hPacketId);
        // specify column families and columns on which perform scan
        byte[] columnFamily = getColumnFamily(packetId);
        byte[] column = getColumn(packetId);
        Map<byte[], List<byte[]>> targetColumns = getScannerColumns(packetId, columnFamily, column);
        long startingTime = System.currentTimeMillis();
        List<Result> hBaseResults = hBaseConnectorSystemApi.scanWithCompleteResult(tableName, targetColumns, rowKeyLowerBound, rowKeyUpperBound, limit);
        long deserializationTime = System.currentTimeMillis();
        // construct frontend output
        addValueToScanResults(packetId, hBaseResults, columnFamily, column, hProjectScan);
        if (!hBaseResults.isEmpty())
            hProjectScan.setRowKeyUpperBound(Bytes.toLong(hBaseResults.get(hBaseResults.size() - 1).getRow()));
        objectMapper.writeValue(jsonGenerator, hProjectScan);
        long finalTime = System.currentTimeMillis();
        log.debug("Duration for hbase scan with packetId {} , total Duration: {}, hbase scan Duration: {}, response construction time:{}", packetId, (finalTime - startingTime), (deserializationTime - startingTime), (finalTime - deserializationTime));
    }

    public byte[] getHPacketAttachment(long hProjectId, long packetId, long fieldId, long rowKeyLowerBound, long rowKeyUpperBound) throws IOException {
        // specify column families and columns on which perform scan
        String tableName = getTableNamePrefix(String.valueOf(packetId)) + hProjectId;
        byte[] columnFamily = Bytes.toBytes(HProjectHBaseConstants.HPACKET_ATTACHMENTS_COLUMN_FAMILY);
        byte[] column = Bytes.toBytes(fieldId);
        Map<byte[], List<byte[]>> targetColumns = getScannerColumns(String.valueOf(packetId), columnFamily, column);
        List<Result> hBaseResult = hBaseConnectorSystemApi.scanWithCompleteResult(tableName, targetColumns, Bytes.toBytes(rowKeyLowerBound), Bytes.toBytes(rowKeyUpperBound), 1);
        if (!hBaseResult.isEmpty()) {
            return hBaseResult.get(0).getValue(columnFamily, column);
        }
        return new byte[]{};
    }

    private byte[] serializeTimeStampFieldAsString(long timestamp, boolean upperBound) {
        if (upperBound) {
            //To include upper bound result.
            timestamp = timestamp + 1;
            return Bytes.toBytes(String.valueOf(timestamp));
        }
        return Bytes.toBytes(String.valueOf(timestamp));
    }

    /**
     * Initialization method: for each step, set one date format.
     * As a matter of fact, caller will receive timestamp in millis unix epoch time,
     * so we need conversion between HBase row keys and millis
     */
    private void setFormats() {
        formats = new EnumMap<>(TimelineColumnFamily.class);
        formats.put(TimelineColumnFamily.YEAR, () -> new SimpleDateFormat("yyyy"));
        formats.put(TimelineColumnFamily.MONTH, () -> new SimpleDateFormat("yyyy_MM"));
        formats.put(TimelineColumnFamily.DAY, () -> new SimpleDateFormat("yyyy_MM_dd"));
        formats.put(TimelineColumnFamily.HOUR, () -> new SimpleDateFormat("yyyy_MM_dd_HH"));
        formats.put(TimelineColumnFamily.MINUTE, () -> new SimpleDateFormat("yyyy_MM_dd_HH_mm"));
        formats.put(TimelineColumnFamily.SECOND, () -> new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss"));
        formats.put(TimelineColumnFamily.MILLISECOND, () -> new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS"));
    }

    private void setRowKeyPrefixes() {
        rowKeyPrefixes = new HashMap<>();
        rowKeyPrefixes.put(HProjectHBaseConstants.EVENT_IDENTIFIER, ModelType.EVENT.getSimpleName());
        rowKeyPrefixes.put(HProjectHBaseConstants.ERROR_IDENTIFIER, ModelType.ERROR.getSimpleName());
    }

    private void setTablePrefixes() {
        tablePrefixes = new HashMap<>();
        tablePrefixes.put(HProjectHBaseConstants.EVENT_IDENTIFIER, HProjectHBaseConstants.EVENT_TABLE_NAME_PREFIX);
        tablePrefixes.put(HProjectHBaseConstants.ERROR_IDENTIFIER, HProjectHBaseConstants.ERROR_TABLE_NAME_PREFIX);
    }

    private void setColumnFamilies() {
        columnFamilies = new HashMap<>();
        columnFamilies.put(HProjectHBaseConstants.EVENT_IDENTIFIER, HProjectHBaseConstants.EVENT_COLUMN_FAMILY);
        columnFamilies.put(HProjectHBaseConstants.ERROR_IDENTIFIER, HProjectHBaseConstants.ERROR_COLUMN_FAMILY);
    }

    private void setColumns() {
        columns = new HashMap<>();
        columns.put(HProjectHBaseConstants.EVENT_IDENTIFIER, HProjectHBaseConstants.EVENT_COLUMN);
        columns.put(HProjectHBaseConstants.ERROR_IDENTIFIER, HProjectHBaseConstants.ERROR_MESSAGE_COLUMM);
    }

    @Override
    public List<HPacketCount> timelineEventCount(long projectId, List<String> packetIds, List<String> deviceIds, long startTime, long endTime) throws Throwable {
        List<HPacketCount> countList = new ArrayList<>();
        for (String hPacketId : packetIds) {
            String tableName = getTableNamePrefix(hPacketId) + projectId;
            countOnHBase(countList, tableName, hPacketId, startTime, endTime);
        }
        for (String hDeviceId : deviceIds) {
            String projectAlarmTableName = HProjectHBaseConstants.ALARM_TABLE_NAME_PREFIX + projectId;
            countAlarmOnHBase(countList, projectAlarmTableName, hDeviceId, startTime, endTime);
        }
        return countList;
    }

    @Override
    public List<TimelineElement> timelineScan(String tableName, List<String> packetIds, List<String> deviceIds, TimelineColumnFamily step, long startTime, long endTime, String timezone) throws Exception {
        TreeMap<Long, Long> events = new TreeMap<>();
        hProjectTimelineUtil.initializeEventMap(events, step, startTime, endTime, timezone);
        step = step.compareTo(TimelineColumnFamily.HOUR) < 0 ? TimelineColumnFamily.HOUR : step;   // to handle timezone in the right way, collapse step to hour if it is of type year, month or day

        List<TimelineElement> timelineElementList = new ArrayList<>();
        for (String hPacketId : packetIds)
            hBaseTraversing(tableName, events, getRowKeyPrefix(hPacketId), step, startTime, endTime);
        for (String hDeviceId : deviceIds) {
            hBaseTraversing(tableName, events, getRowKeyPrefixForAlarmEntryInTimelineTable(hDeviceId), step, startTime, endTime);
        }
        for (Long timestamp : events.keySet())
            timelineElementList.add(new TimelineElement(timestamp, events.get(timestamp)));

        return timelineElementList;
    }

    @Override
    public HProjectAlgorithmHBaseResult getAlgorithmOutputs(long projectId, long hProjectAlgorithmId) throws IOException {
        final String HBASE_TABLE_PREFIX = "algorithm";
        final String HBASE_OUTPUT_COLUMN_FAMILY = "value";
        final String SEPARATOR = "_";

        try {
            hProjectRepository.find(projectId, null);
            algorithmRepository.find(hProjectAlgorithmId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }

        HProjectAlgorithm hProjectAlgorithm = algorithmRepository.find(hProjectAlgorithmId, null);
        String tableName = HBASE_TABLE_PREFIX + SEPARATOR + hProjectAlgorithm.getAlgorithm().getId();
        Map<byte[], List<byte[]>> columnMap = new HashMap<>();
        List<byte[]> columnList = new ArrayList<>();

        // get output of algorithm
        HProjectAlgorithmConfig config = objectMapper.readValue(hProjectAlgorithm.getConfig(), HProjectAlgorithmConfig.class);
        List<AlgorithmIOField> outputList = config.getOutput();

        for (AlgorithmIOField output : outputList)
            columnList.add(output.getName().getBytes());
        columnMap.put(HBASE_OUTPUT_COLUMN_FAMILY.getBytes(), columnList);
        // Algorithm HBase tables contains row which are the concatenation between project id, hProjectAlgorithm name
        // and reversed timestamp. HProjectAlgorithm name distinguish between two algorithms defined on the same project,
        // but on different hPackets. Using HProjectAlgorithm name (and not its ID), we are sure that different configurations7
        // write and load date with different row keys. If we use HProjectAlgorithm ID, we could read data based on old configurations.

        // Get reversed timestamp lower bound from now ...
        byte[] rowKeyLowerBound = (projectId + SEPARATOR + hProjectAlgorithm.getName() + SEPARATOR + (Long.MAX_VALUE - Instant.now().getEpochSecond())).getBytes();
        // ... and get reversed timestamp upper bound from the first day of the current year
        byte[] rowKeyUpperBound = (projectId + SEPARATOR + hProjectAlgorithm.getName() + SEPARATOR + (Long.MAX_VALUE - LocalDateTime.now().with(firstDayOfYear()).toInstant(ZoneOffset.UTC).getEpochSecond())).getBytes();
        HProjectAlgorithmHBaseResult result = new HProjectAlgorithmHBaseResult();
        result.setRowsFromOriginalMap(projectId, hProjectAlgorithmId, hBaseConnectorSystemApi.scan(tableName, columnMap, rowKeyLowerBound, rowKeyUpperBound, 1));
        return result;
    }

    @Reference
    public void setHBaseConnectorSystemApi(HBaseConnectorSystemApi hBaseConnectorSystemApi) {
        this.hBaseConnectorSystemApi = hBaseConnectorSystemApi;
    }

    @Reference
    public void setHProjectTimelineUtil(HProjectTimelineUtil hProjectTimelineUtil) {
        this.hProjectTimelineUtil = hProjectTimelineUtil;
    }

    @Reference
    public void setAlgorithmRepository(HProjectAlgorithmRepository algorithmRepository) {
        this.algorithmRepository = algorithmRepository;
    }

    @Reference
    public void setHProjectRepository(HProjectRepository hProjectRepository) {
        this.hProjectRepository = hProjectRepository;
    }

    @Reference
    public void sethDeviceRepository(HDeviceRepository hDeviceRepository) {
        this.hDeviceRepository = hDeviceRepository;
    }

}

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

import it.acsoftware.hyperiot.storm.util.StormConstants;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.storm.tuple.Tuple;

import java.util.HashMap;
import java.util.Map;

import static org.apache.storm.hbase.common.Utils.toBytes;

public class AlarmHBaseMapper implements HBaseMapper {


    private String rowKeyField;

    private Map<String, byte[]> columnFamilyMap;

    @Override
    public byte[] rowKey(Tuple tuple) {
        Object objVal = tuple.getValueByField(this.rowKeyField);
        return toBytes(objVal);
    }

    @Override
    public ColumnList columns(Tuple tuple) {
        long hDeviceId = (long) tuple.getValueByField(StormConstants.HDEVICE_ID_FIELD);
        String alarmState = (String) tuple.getValueByField(StormConstants.ALARM_STATE_FIELD);
        byte[] columnFamily = this.columnFamilyMap.get(alarmState);
        String cellValue = (String) tuple.getValueByField(StormConstants.EVENT_COLUMN_FIELD);
        ColumnList cols = new ColumnList();
        cols.addColumn(columnFamily, Bytes.toBytes(hDeviceId), toBytes(cellValue));
        return cols;
    }


    public AlarmHBaseMapper withColumnFamilies() {
        this.columnFamilyMap = new HashMap<>();
        columnFamilyMap.put(StormConstants.HBASE_ALARM_TABLE_COLUMN_FAMILY_UP, StormConstants.HBASE_ALARM_TABLE_COLUMN_FAMILY_UP.getBytes());
        columnFamilyMap.put(StormConstants.HBASE_ALARM_TABLE_COLUMN_FAMILY_DOWN, StormConstants.HBASE_ALARM_TABLE_COLUMN_FAMILY_DOWN.getBytes());
        return this;
    }

    /**
     * This is a config method, it is called in topology.yaml file and set tuple field containing
     * HBase row key
     * @param rowKeyField Tuple field containing HBase row key
     * @return AlarmHBaseMapper
     */
    public AlarmHBaseMapper withRowKeyField(String rowKeyField) {
        this.rowKeyField = rowKeyField;
        return this;
    }
}

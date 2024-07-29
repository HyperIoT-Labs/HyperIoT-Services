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

import it.acsoftware.hyperiot.storm.runtime.bolt.SelectionBolt;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static it.acsoftware.hyperiot.storm.util.StormConstants.*;
import static org.apache.storm.hbase.common.Utils.toBytes;

@SuppressWarnings("unused")
public class AvroHBaseMapper implements HBaseMapper {
    private static final Logger log =
            LoggerFactory.getILoggerFactory().getLogger(AvroHBaseMapper.class.getName());
    // HBase Avro bolt had to receive a tuple with two fields:
    // - rowKeyField, which is set in topology.yaml config method, refers to HBase row key
    // - packet value is HPacket instance in Avro format
    private String rowKeyField;
    private byte[] columnFamily;
    private byte[] attachmentsColumnFamily;

    @Override
    public byte[] rowKey(Tuple tuple) {
        Object objVal = tuple.getValueByField(this.rowKeyField);
        return toBytes(objVal);
    }

    @Override
    public ColumnList columns(Tuple tuple) {
        long hPacketId = (long) tuple.getValueByField(HPACKET_ID_FIELD);
        String avroHPacket = (String) tuple.getValueByField(AVRO_HPACKET_FIELD);
        ColumnList cols = new ColumnList();
        cols.addColumn(this.columnFamily, Bytes.toBytes(hPacketId), toBytes(avroHPacket));
        try {
            Map<Long, byte[]> attachments = (Map) tuple.getValueByField(AVRO_HPACKET_ATTACHMENTS);
            attachments.keySet().forEach(fieldId -> {
                log.debug("Adding ATTACHMENT to HPacket");
                cols.addColumn(this.attachmentsColumnFamily, Bytes.toBytes(fieldId.longValue()), attachments.get(fieldId));
            });
        } catch (IllegalArgumentException e){
            log.warn("Field avroHPacketAttachment field does not exists");
        }
        return cols;
    }

    /**
     * This is a config method, it is called in topology.yaml file and set HBase column family
     *
     * @param columnFamily HBase column family
     * @return AvroHBaseMapper
     */
    public AvroHBaseMapper withColumnFamily(String columnFamily) {
        this.columnFamily = columnFamily.getBytes();
        return this;
    }

    /**
     * This is a config method, it is called in topology.yaml file and set HBase attachments column family
     *
     * @param attachmentsColumnFamily HBase column family
     * @return AvroHBaseMapper
     */
    public AvroHBaseMapper withAttachmentsColumnFamily(String attachmentsColumnFamily) {
        this.attachmentsColumnFamily = attachmentsColumnFamily.getBytes();
        return this;
    }

    /**
     * This is a config method, it is called in topology.yaml file and set tuple field containing
     * HBase row key
     *
     * @param rowKeyField Tuple field containing HBase row key
     * @return AvroHBaseMapper
     */
    public AvroHBaseMapper withRowKeyField(String rowKeyField) {
        this.rowKeyField = rowKeyField;
        return this;
    }

}

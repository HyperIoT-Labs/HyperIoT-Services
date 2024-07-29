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

package it.acsoftware.hyperiot.storm.runtime.bolt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.deserialization.service.JsonAvroHPacketDeserializer;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import it.acsoftware.hyperiot.stormmanager.model.MessageConversionStrategy;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static it.acsoftware.hyperiot.storm.util.StormConstants.*;

/**
 * Author Francesco Salerno
 *
 * This is a Bolt that read data from appropriate dlq relative to hbase's failure.
 * The purpose of this bolt is to reconstruct the original message and send them to appropriate
 * hbase bolt, to retry write's operation after hbase failure
 *
 */
public class HyperIoTDlqBolt extends BaseRichBolt {


    private static final Logger log = LoggerFactory.getILoggerFactory().getLogger(HyperIoTDlqBolt.class.getName());

    private OutputCollector outputCollector;

    private static final ObjectMapper jsonMapper= new ObjectMapper();

    private static final JsonAvroHPacketDeserializer hdfsHPacketDeserializer = JsonAvroHPacketDeserializer.getInstance();


    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.outputCollector=collector;
    }

    @Override
    public void execute(Tuple input) {
        try {
            //Deserialize value and resend to CustomHBaseBolt value like the original bolt.
            log.debug("HyperIoTHBaseDlqBolt before deserialize method");
            Map<String, String> tupleValue = deserializeFromPacketDlq(input);
            log.debug("HyperIoTHBaseDlqBolt after deserialize method");
            //Print map content to help in debug. (Remember this code is execute on the host in which run storm)
            log.debug("HyperIoTHBaseDlqBolt Show value in map");
            for(String key: tupleValue.keySet()){
                log.debug("key : {} , value : {} ",key,tupleValue.get(key));
            }
            log.debug("HyperIoTHBaseDlqBolt end of map");
            //It's important that the tuple is anchored because this bolt send tuple to the bolt which write on HBase.
            //If the tuple is anchored and the hbase's writer fail the operation, dlq logic is apply.
            //To understand the reason of why we don't use a strategy object to avoid switch case, ask to the class's author.
            String boltSenderStrategy = tupleValue.get(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL);
            if(boltSenderStrategy == null){
                log.debug("In HyperIoTDlqBolt , boldSenderStrategy is null");
                throw new RuntimeException("In HyperIoTDlqBolt deserialization failed, no sender label is present");
            }
            log.debug("In HyperIoTDlqBolt , boldSenderStrategy is {} ",boltSenderStrategy);
            MessageConversionStrategy messageConversionStrategy = MessageConversionStrategy.valueOf(boltSenderStrategy);
            switch (messageConversionStrategy){
                case HBASE_DLQ_EVENT_TABLE_MESSAGE_STRATEGY :{
                    emitMessageToHBaseEventTableBolt(tupleValue,input);
                    break;
                }
                case HBASE_DLQ_AVRO_TABLE_MESSAGE_STRATEGY : {
                    emitMessageToHBaseAvroTableBolt(tupleValue,input);
                    break;
                }
                case HBASE_DLQ_TIMELINE_TABLE_MESSAGE_STRATEGY : {
                    emitMessageToHbaseTimelineTableBolt(tupleValue,input);
                    break;
                }
                case HBASE_DLQ_ALARM_TABLE_MESSAGE_STRATEGY : {
                    emitMessageToHBaseAlarmTableBolt(tupleValue,input);
                    break;
                }
                case HBASE_DLQ_EVENT_RULE_STATE_MESSAGE_STRATEGY : {
                    emitMessageToHBaseAlarmEventRuleStateTableBolt(tupleValue, input);
                    break;
                }
                case HBASE_DLQ_ERROR_TABLE_MESSAGE_STRATEGY :{
                    emitMessageToHBaseErrorTableBolt(tupleValue,input);
                    break;
                }
                case HDFS_DLQ_DAY_MESSAGE_STRATEGY :{
                    emitMessageToHdfsBolt(tupleValue,input,StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_DAY);
                    break;
                }
                case HDFS_DLQ_HOUR_MESSAGE_STRATEGY:{
                    emitMessageToHdfsBolt(tupleValue,input, KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_HOUR);
                    break;
                }
                case HDFS_DLQ_MONTH_MESSAGE_STRATEGY:{
                    emitMessageToHdfsBolt(tupleValue,input, KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_MONTH);
                    break;
                }
                case HDFS_DLQ_YEAR_MESSAGE_STRATEGY:{
                    emitMessageToHdfsBolt(tupleValue,input, KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_YEAR);
                    break;
                }
                case HDFS_DLQ_QUARTER_MESSAGE_STRATEGY:{
                    emitMessageToHdfsBolt(tupleValue,input, KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_QUARTER);
                    break;
                }
                case HDFS_DLQ_SEMESTER_MESSAGE_STRATEGY:{
                    emitMessageToHdfsBolt(tupleValue,input, KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_SEMESTER);
                    break;
                }
                default :{
                    log.debug("HyperIoTHBaseDlqBolt Deserialization strategy not supported");
                    throw new RuntimeException("HyperIoTHBaseDlqBolt Deserialization strategy not supported");
                }

            }
        }catch (Throwable exc){
                log.debug("In HyperIoTDlqBolt , exception during tuple deserialization , exception class {} , exception message {}"
                        ,exc.getClass().getName()
                        ,exc.getMessage());
                // Ack tuple because we can't recover from a serialization exception.
                outputCollector.ack(input);
                return;
        }
        log.debug("HyperIoTHBaseDlqBolt HBaseDlqDeserializer success method");
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT,
                new Fields(StormConstants.TIMESTAMP_FIELD, StormConstants.EVENT_COLUMN_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_AVRO,
                new Fields(StormConstants.ROWKEY_FIELD, StormConstants.HPACKET_ID_FIELD, StormConstants.AVRO_HPACKET_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_TIMELINE,
                new Fields(StormConstants.ROWKEY_FIELD, StormConstants.TIMESTAMP_FIELD, StormConstants.STEP_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ALARM,
                new Fields(StormConstants.ROWKEY_FIELD, StormConstants.HDEVICE_ID_FIELD, StormConstants.ALARM_STATE_FIELD, StormConstants.EVENT_COLUMN_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT_RULE_STATE,
                new Fields(StormConstants.EVENT_RULE_ID, StormConstants.EVENT_RULE_STATE_INFO_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ERROR,
                new Fields(StormConstants.TIMESTAMP_FIELD, StormConstants.MESSAGE_FIELD, StormConstants.RECEIVED_PACKET_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_DAY, new Fields(StormConstants.PACKET_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_HOUR, new Fields(StormConstants.PACKET_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_YEAR, new Fields(StormConstants.PACKET_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_MONTH, new Fields(StormConstants.PACKET_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_QUARTER, new Fields(StormConstants.PACKET_FIELD));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HDFS_SEMESTER, new Fields(StormConstants.PACKET_FIELD));

    }

    /**
     *
     * @param input Raw tuple read from Kafka Spout
     * @return Map that container the original message.
     * Map's key represent the field of the original message.
     * Map's value represente the value of the field of the original message.
     */
    private Map<String,String> deserializeFromPacketDlq(Tuple input) {
        try {
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {};
            Map<String,String> mappa = jsonMapper.readValue(input.getBinary(4), typeRef);
            log.debug("HyperIoTHBaseDlqBolt HBaseDlqDeserializer method  succeed");
            return mappa;
        }catch(Throwable exc){
            log.debug("HyperIoTHBaseDlqBolt HBaseDlqDeserializer method  fail");
        }
        throw new RuntimeException("HyperIoTHBaseDlqBolt Deserialization fail");
    }

    private void emitMessageToHBaseEventTableBolt(Map<String,String> tupleValue, Tuple input){
        String rowKeyTimeStamp =  tupleValue.get(StormConstants.TIMESTAMP_FIELD);
        String avroPacket = tupleValue.get(StormConstants.EVENT_COLUMN_FIELD);
        //Emit anchored tuple
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT,
                input,
                new Values(rowKeyTimeStamp,avroPacket));
    }

    private void emitMessageToHBaseAvroTableBolt(Map<String,String> tupleValue, Tuple input){
        long timestamp = Long.parseLong(tupleValue.get(StormConstants.ROWKEY_FIELD));
        long packetId = Long.parseLong(tupleValue.get(StormConstants.HPACKET_ID_FIELD));
        String avroPacket = tupleValue.get(StormConstants.AVRO_HPACKET_FIELD);

        //Emit anchored tuple
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_AVRO,
                input,
                new Values(timestamp,packetId,avroPacket));
    }
    private void emitMessageToHbaseTimelineTableBolt(Map<String,String> tupleValue, Tuple input){
        String rowKeyField = (String)tupleValue.get(StormConstants.ROWKEY_FIELD);
        long timestamp = Long.parseLong(tupleValue.get(StormConstants.TIMESTAMP_FIELD));
        String stepString = ((String)tupleValue.get(StormConstants.STEP_FIELD)).toUpperCase(Locale.ROOT);
        TimelineColumnFamily step=  TimelineColumnFamily.valueOf(stepString);

        //Emit anchored tuple
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_TIMELINE,
                input,
                new Values(rowKeyField,timestamp,step));
    }

    private void emitMessageToHBaseAlarmTableBolt(Map<String,String> tupleValue, Tuple input){
        long rowKeyField = Long.parseLong(tupleValue.get(StormConstants.ROWKEY_FIELD));
        long hDeviceId = Long.parseLong(tupleValue.get(StormConstants.HDEVICE_ID_FIELD));
        String alarmState = (String) tupleValue.get(StormConstants.ALARM_STATE_FIELD);
        String avroHPacket = (String)tupleValue.get(StormConstants.EVENT_COLUMN_FIELD);
        //Emit anchored tuple
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ALARM,
                input,
                new Values(rowKeyField, hDeviceId, alarmState, avroHPacket));
    }

    private void emitMessageToHBaseAlarmEventRuleStateTableBolt(Map<String,String> tupleValue, Tuple input){
        long alarmEventRuleStateTableRowKey = Long.parseLong((String)tupleValue.get(StormConstants.EVENT_RULE_ID));
        String eventRuleStateField = (String)tupleValue.get(StormConstants.EVENT_RULE_STATE_INFO_FIELD);
        //Emit anchored tuple
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_EVENT_RULE_STATE,
                input,
                new Values(alarmEventRuleStateTableRowKey,eventRuleStateField));
    }

    private void emitMessageToHBaseErrorTableBolt(Map<String,String> tupleValue, Tuple input){
        long timestamp=Long.parseLong(tupleValue.get(StormConstants.TIMESTAMP_FIELD));
        String message=(String)tupleValue.get(StormConstants.MESSAGE_FIELD);
        String reiceved_packet=(String)tupleValue.get(StormConstants.RECEIVED_PACKET_FIELD);
        //Emit anchored tuple
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_DESERIALIZATION_HBASE_ERROR,
                input,
                new Values(timestamp,message,reiceved_packet));
    }


    private void emitMessageToHdfsBolt(Map<String,String> tupleValue, Tuple input, String streamId) throws IOException {
        String packetSerialization = tupleValue.get(StormConstants.BOLT_HPACKET_DESERIALIZATION_FIELD_LABEL);
        HPacket packet = hdfsHPacketDeserializer.deserialize(packetSerialization.getBytes(StandardCharsets.UTF_8),null);
        //Emit anchored tuple
        outputCollector.emit(streamId,input,new Values(packet));
    }









}

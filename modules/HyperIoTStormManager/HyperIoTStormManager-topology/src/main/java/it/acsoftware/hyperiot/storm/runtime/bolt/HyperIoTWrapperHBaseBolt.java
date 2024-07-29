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

import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import it.acsoftware.hyperiot.stormmanager.model.MessageConversionStrategy;
import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.HyperIoTHBaseBolt;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Author Francesco Salerno
 *
 * This class is a wrapper of an HyperIoTHBaseBolt.
 * To understand why this class is not a wrapper of HBaseBolt ask to the author of the class.
 *
 */
public class HyperIoTWrapperHBaseBolt extends BaseRichBolt {

    private static final Logger log = LoggerFactory.getILoggerFactory().getLogger(HyperIoTWrapperHBaseBolt.class.getName());

    private String deserializationDlqBoltHBaseId;

    private MessageConversionStrategy serializationStrategy;

    private OutputCollector outputCollector;


    private HBaseBolt hBaseBoltDependency;

    private static final ObjectMapper jsonMapper= new ObjectMapper();


    //It's important(to handling hbase failure in a correct way), to pass at this class not a generic
    //HBaseBolt , but an HyperIoTHBaseBolt (See the org.apache.storm.hbase.bolt package in this module).
    public HyperIoTWrapperHBaseBolt(HBaseBolt hBaseBoltDependency, MessageConversionStrategy serializationStrategy, String deserializationDlqBoltHBaseId){
        if(! (hBaseBoltDependency instanceof HyperIoTHBaseBolt)){
            log.debug("Instantiation of HyperIoTWrapperHBaseBolt failed , you must pass an HyperIoTHBaseBolt class");
            throw new RuntimeException("Instantiation of HyperIoTWrapperHBaseBolt failed , you must pass an HyperIoTHBaseBolt class");
        }
        this.hBaseBoltDependency=hBaseBoltDependency;
        this.serializationStrategy=serializationStrategy;
        this.deserializationDlqBoltHBaseId = deserializationDlqBoltHBaseId;
    }


    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
            hBaseBoltDependency.prepare(stormConf,context,collector);
            this.outputCollector=collector;
    }

    @Override
    public void execute(Tuple input) {
            try{
                log.debug("In HyperIoTWrapperHBaseBolt in method execute");
                //this method execute tuple's ack/fail.
                hBaseBoltDependency.execute(input);
                log.debug("In HyperIoTWrapperHBaseBolt ExecuteSuccessful");
            }catch (Throwable exc){
                log.debug("In HyperIoTWrapperHBaseBolt , exc type : {} ",exc.getClass().getName());
                //If input source is deserializationDlq bolt means that this tuple come from dlq, so we don't need
                // to send again to the dlq.
                // (There is a fail in execute method, so the tuple will be resend after a certain period of time .)

                if(! (input.getSourceStreamId().equals(this.deserializationDlqBoltHBaseId) ||
                        input.getSourceComponent().equals(this.deserializationDlqBoltHBaseId))) {
                    log.debug("In HyperIoTWrapperHBaseBolt Emit tuple on kafka dlq bolt for hbase");
                    handleHBaseErrorFailure(input);
                } else{
                    log.debug("In HyperIoTWrapperHBaseBolt tuple is sent by deserialization hbase bolt");
                }
            }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HBASE_AVRO,
                new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HBASE_EVENT,
                new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HBASE_TIMELINE,
                new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HBASE_ALARM,
                new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HBASE_EVENT_RULE_STATE,
                new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));

        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HBASE_ERROR,
                new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));

    }
    //todo change messageMap.put(..,this.serializationStrategy.name()
    private void handleHBaseErrorFailure(Tuple input){
        log.debug("In HyperIoTWrapperHBaseBolt HandleHBaseFailureError");
        switch (serializationStrategy){
            case HBASE_DLQ_EVENT_TABLE_MESSAGE_STRATEGY : {
                emitSerializeMessageForEventStream(input);
                break;
            }
            case HBASE_DLQ_AVRO_TABLE_MESSAGE_STRATEGY : {
                emitSerializeMessageForAvroStream(input);
                break;
            }
            case HBASE_DLQ_TIMELINE_TABLE_MESSAGE_STRATEGY : {
                emitSerializeMessageForTimelineStream(input);
                break;
            }
            case  HBASE_DLQ_ALARM_TABLE_MESSAGE_STRATEGY : {
                emitSerializeMessageForAlarmStream(input);
                break;
            }
            case HBASE_DLQ_EVENT_RULE_STATE_MESSAGE_STRATEGY : {
                emitSerializeMessageForAlarmEventRuleStateStream(input);
                break;
            }
            case HBASE_DLQ_ERROR_TABLE_MESSAGE_STRATEGY : {
                emitSerializeMessageForErrorStream(input);
                break;
            }
            default : {
                //DEFAULT CASE HAPPEN IF DEVELOPER INSERT AN ERROR SERIALIZATION STRATEGY
                //THIS CASE CAN VERIFY IN THE FUTURE ONLY IF A DEVELOPER ADD A NEW VALUE ON ENUM
                //AND FORGIVE TO ADD HANDLING OF THIS NEW VALUE IN THIS METHOD.
                throw new RuntimeException("In HyperIoTWrapperHBaseBolt Exception no strategy to serialize defined");
            }
        }
    }


    private void emitSerializeMessageForAvroStream( Tuple input ){
        String timeStampKey = String.valueOf(Instant.now().toEpochMilli());
        HashMap<String,String> messageMap = new HashMap<>();
        messageMap.put(StormConstants.ROWKEY_FIELD, String.valueOf((Long)input.getValueByField(StormConstants.ROWKEY_FIELD)));
        messageMap.put(StormConstants.HPACKET_ID_FIELD, String.valueOf((Long)input.getValueByField(StormConstants.HPACKET_ID_FIELD)));
        messageMap.put(StormConstants.AVRO_HPACKET_FIELD, (String)input.getValueByField(StormConstants.AVRO_HPACKET_FIELD));
        messageMap.put(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL, this.serializationStrategy.name());

        log.debug("HyperIoTWrapperHBaseBolt HandleHBaseFailure emit bolt avro table error stream");
        String jsonResult =null;
        try {
            jsonResult = jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(messageMap);
        }catch (Throwable exc){
            log.debug("HyperIoTWrapperHBaseBolt JsonSerializationStringFailed in event table");
        }
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_HBASE_AVRO,
                new Values(timeStampKey,jsonResult));
    }
    private void emitSerializeMessageForEventStream(Tuple input ){
        String timeStampKey = String.valueOf(Instant.now().toEpochMilli());
        HashMap<String,String> messageMap = new HashMap<>();
        messageMap.put(StormConstants.TIMESTAMP_FIELD, (String) input.getValueByField(StormConstants.TIMESTAMP_FIELD));
        messageMap.put(StormConstants.EVENT_COLUMN_FIELD, (String)input.getValueByField(StormConstants.EVENT_COLUMN_FIELD));
        messageMap.put(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL, this.serializationStrategy.name());
        log.debug("HyperIoTWrapperHBaseBolt HandleHBaseFailure emit bolt event table error stream");
        String jsonResult =null;
        try {
            jsonResult = jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(messageMap);
        }catch (Throwable exc){
            log.debug("HyperIoTWrapperHBaseBolt JsonSerializationStringFailed in avro table");
        }
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_HBASE_EVENT,
                new Values(timeStampKey,jsonResult));

    }
    private void emitSerializeMessageForTimelineStream( Tuple input){
        String timeStampKey = String.valueOf(Instant.now().toEpochMilli());
        HashMap<String,String> messageMap = new HashMap<>();
        messageMap.put(StormConstants.ROWKEY_FIELD,(String)input.getValueByField(StormConstants.ROWKEY_FIELD));
        messageMap.put(StormConstants.TIMESTAMP_FIELD,String.valueOf((Long)input.getValueByField(StormConstants.TIMESTAMP_FIELD)));
        messageMap.put(StormConstants.STEP_FIELD,((TimelineColumnFamily)input.getValueByField(StormConstants.STEP_FIELD)).getName());
        messageMap.put(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL, this.serializationStrategy.name());

        log.debug("HyperIoTWrapperHBaseBolt HandleHBaseFailure emit bolt timeline table error stream");

        String jsonResult =null;
        try {
            jsonResult = jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(messageMap);
        }catch (Throwable exc){
            log.debug("HyperIoTWrapperHBaseBolt JsonSerializationStringFailed in timeline table");
        }
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_HBASE_TIMELINE,
                new Values(timeStampKey,jsonResult));
    }
    private void emitSerializeMessageForAlarmStream( Tuple input ){
        String timeStampKey = String.valueOf(Instant.now().toEpochMilli());
        HashMap<String,String> messageMap = new HashMap<>();
        messageMap.put(StormConstants.ROWKEY_FIELD, String.valueOf((Long) input.getValueByField(StormConstants.ROWKEY_FIELD)));
        messageMap.put(StormConstants.HDEVICE_ID_FIELD, String.valueOf((Long) input.getValueByField(StormConstants.HDEVICE_ID_FIELD)));
        messageMap.put(StormConstants.ALARM_STATE_FIELD, (String) input.getValueByField(StormConstants.ALARM_STATE_FIELD));
        messageMap.put(StormConstants.EVENT_COLUMN_FIELD, (String) input.getValueByField(StormConstants.EVENT_COLUMN_FIELD));
        messageMap.put(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL, this.serializationStrategy.name());

        String jsonResult =null;
        try {
            jsonResult = jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(messageMap);
        }catch (Throwable exc){
            log.debug("HyperIoTWrapperHBaseBolt JsonSerializationStringFailed in Alarm table");
        }
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_HBASE_ALARM,
                new Values(timeStampKey,jsonResult));
    }

    private void emitSerializeMessageForAlarmEventRuleStateStream( Tuple input ){
        String timeStampKey = String.valueOf(Instant.now().toEpochMilli());
        HashMap<String,String> messageMap = new HashMap<>();
        messageMap.put(StormConstants.EVENT_RULE_ID,String.valueOf((Long) input.getValueByField(StormConstants.EVENT_RULE_ID)));
        messageMap.put(StormConstants.EVENT_RULE_STATE_INFO_FIELD,(String)input.getValueByField(StormConstants.EVENT_RULE_STATE_INFO_FIELD));
        messageMap.put(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL, this.serializationStrategy.name());

        String jsonResult =null;
        try {
            jsonResult = jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(messageMap);
        }catch (Throwable exc){
            log.debug("HyperIoTWrapperHBaseBolt JsonSerializationStringFailed in Event Rule State table");
        }
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_HBASE_EVENT_RULE_STATE,
                new Values(timeStampKey,jsonResult));
    }

    private void emitSerializeMessageForErrorStream( Tuple input ){
        String timeStampKey = String.valueOf(Instant.now().toEpochMilli());
        HashMap<String,String> messageMap = new HashMap<>();
        messageMap.put(StormConstants.TIMESTAMP_FIELD,String.valueOf((Long)input.getValueByField(StormConstants.TIMESTAMP_FIELD)));
        messageMap.put(StormConstants.MESSAGE_FIELD,(String)input.getValueByField(StormConstants.MESSAGE_FIELD));
        messageMap.put(StormConstants.RECEIVED_PACKET_FIELD,(String)input.getValueByField(StormConstants.RECEIVED_PACKET_FIELD));
        messageMap.put(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL, this.serializationStrategy.name());

        String jsonResult =null;
        try {
            jsonResult = jsonMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(messageMap);
        }catch (Throwable exc){
            log.debug("HyperIoTWrapperHBaseBolt JsonSerializationStringFailed in ERROR table");
        }
        outputCollector.emit(StormConstants.KAKFA_DLQ_STREAM_HBASE_ERROR,
                new Values(timeStampKey,jsonResult));
    }

}

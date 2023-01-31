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
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.serialization.service.JsonAvroHPacketSerializer;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import it.acsoftware.hyperiot.stormmanager.model.MessageConversionStrategy;
import org.apache.storm.hdfs.bolt.AvroGenericRecordBolt;
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
 * This class is an extension of a AvroGenericRecordBolt ( Bolt that implement funcionality to write on HDFS directory)
 *
 * We need to wrap this AvroGenericRecordBolt to catch Exception relative to an HDFS's failure , such that we can
 * implement a recovery strategy.
 * For the moment the strategy to recover from an HDFS failure is to use a DLQ, and retry the operation after a certain period of time.
 */
public class HyperIoTWrapperAvroGenericBolt extends BaseRichBolt {

    private static final Logger log =LoggerFactory.getILoggerFactory().getLogger(HyperIoTWrapperAvroGenericBolt.class.getName());

    private AvroGenericRecordBolt avroBoltDependency;

    private MessageConversionStrategy serializationStrategy;

    private String deserializationDlqBoltId;

    private String errorStreamId;

    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static final JsonAvroHPacketSerializer hPacketSerializer = JsonAvroHPacketSerializer.getInstance();

    private OutputCollector outputCollector;

    public HyperIoTWrapperAvroGenericBolt(AvroGenericRecordBolt avroBoltDependency,String errorStreamId,MessageConversionStrategy serializationStrategy, String deserializationDlqBoltId){
        log.debug("In HyperIoTWrapperAvroGenericBolt Constructor , class name wrapped : {}",avroBoltDependency.getClass().getName());
        this.avroBoltDependency=avroBoltDependency;
        this.serializationStrategy=serializationStrategy;
        this.errorStreamId=errorStreamId;
        this.deserializationDlqBoltId = deserializationDlqBoltId;

    }

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        log.debug("In HyperIoTWrapperAvroGenericBolt prepare method ");
        avroBoltDependency.prepare(stormConf,context,collector);
        outputCollector=collector;

    }

    @Override
    public void execute(Tuple input) {
                try{
                    log.debug("In HyperIoTWrapperAvroGenericBolt before execute");
                    //Execute method of the wrapped bolt ack/fail tuple.
                    avroBoltDependency.execute(input);
                    log.debug("In HyperIoTWrapperAvroGenericBolt execute succeed");
            }catch (Throwable exc){
                    log.debug("In HyperIoTWrapperAvroGenericBolt ExceptionCustomAvro in execute method,Write on hdfs failed, sourceComponent is : {} , sourceStreamId is {}",
                            input.getSourceComponent(),
                            input.getSourceStreamId());
                    //If data not arrive from dlq, send to the kafka bolt that write on dlq .
                if(! (input.getSourceStreamId().equals(deserializationDlqBoltId) || input.getSourceComponent().equals(deserializationDlqBoltId))  ) {
                    log.debug("In HyperIoTWrapperAvroGenericBolt StreamNotCorrect Emit tuple on kafka dlq bolt ");
                    handleHdfsFailure(input);
                }else{
                    log.debug("In HyperIoTWrapperAvroGenericBolt StreamNotCorrect tuple is send by deserialization bolt");
                }
            }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HDFS_HOUR,new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HDFS_DAY,new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HDFS_MONTH,new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HDFS_YEAR,new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HDFS_SEMESTER,new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));
        declarer.declareStream(StormConstants.KAKFA_DLQ_STREAM_HDFS_QUARTER,new Fields(StormConstants.TIMESTAMP_FIELD,StormConstants.ERROR_PACKET_FIELD_RETRANSMIT));
    }

    private void handleHdfsFailure(Tuple input){
        log.debug("In HyperIoTWrapperAvroGenericBolt handleHdfsFailure");
        Map<String,String> messageMap= new HashMap<>();
        String timeStampKey = String.valueOf(Instant.now().toEpochMilli());
        String boltSenderLabel = this.serializationStrategy.name();
        String packetSerialization ;
        try {
            packetSerialization = new String(hPacketSerializer.serialize((HPacket) input.getValueByField(StormConstants.PACKET_FIELD)));

        }catch (Exception exc){
            //If this serialization fail ,throw a RuntimeException.
            throw new RuntimeException("Error during packet deserialization in HyperIoTWrapperAvroGenericBolt");
        }
        messageMap.put(StormConstants.KAFKA_DLQ_PACKET_SENDER_LABEL,boltSenderLabel);
        messageMap.put(StormConstants.BOLT_HPACKET_DESERIALIZATION_FIELD_LABEL,packetSerialization);
        String jsonResult =null;
        try {
            jsonResult = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messageMap);
        }catch (Throwable exc){
            log.debug("In HyperIoTWrapperAvroGenericBolt JsonSerializationStringFailed in ERROR table");
        }
        outputCollector.emit(errorStreamId,new Values(timeStampKey,jsonResult));

    }
}

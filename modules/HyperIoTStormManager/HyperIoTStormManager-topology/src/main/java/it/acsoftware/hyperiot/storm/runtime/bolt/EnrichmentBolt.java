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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.rule.service.RuleEngine;
import it.acsoftware.hyperiot.storm.runtime.bolt.util.BoltUtil;
import it.acsoftware.hyperiot.storm.topology.TopologyConfigKeys;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import it.acsoftware.hyperiot.stormmanager.model.HyperIoTTopologyError;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.FailedException;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.mariuszgromada.math.mxparser.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static it.acsoftware.hyperiot.storm.util.StormConstants.HPROJECT_ERROR_STREAM_ID;
import static it.acsoftware.hyperiot.storm.util.StormConstants.TIMELINE_HPROJECT_STREAM_ID_PREFIX;

public class EnrichmentBolt extends BaseRichBolt {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentBolt.class);
    private OutputCollector collector;
    @SuppressWarnings({"FieldCanBeLocal", "rawtypes"})
    private Map config;
    private long hProjectId;
    private ObjectMapper objectMapper;
    private RuleEngine ruleEngine;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        log.debug("prepare with config {} context {}", stormConf, context);
        this.collector = collector;
        config = stormConf;
        objectMapper = new ObjectMapper();
        Map<String, String> rules = (Map<String, String>) (config.get("rule-engine.drools"));
        ruleEngine = new RuleEngine(new ArrayList<>(rules.values()),
                Long.parseLong(config.get(TopologyConfigKeys.CONFIG_PROJECT_ID).toString()));
        ruleEngine.start();
    }

    @Override
    public void cleanup() {
        this.ruleEngine.stop();
    }

    @Override
    public void execute(Tuple input) {
        HPacket packet;
        log.trace("execute on tuple {} with collector {}", input, collector);
        packet = (HPacket) input.getValueByField("hpacket");
        if (packet != null) {
            try {
                log.trace("Input HPacket with id {} and name {}", packet.getId(), packet.getName());
                if (log.isDebugEnabled()) {
                    packet.getFields().forEach(pf ->
                            log.trace("   - field {} with id {} and value {}", pf.getName(), pf.getId(), pf.getValue())
                    );
                }
                ruleEngineRun(packet);
                log.trace("Output HPacket with id {} and name {}", packet.getId(), packet.getName());
                if (log.isDebugEnabled()) {
                    packet.getFields().forEach(pf ->
                            log.trace("   - field {} with id {} and value {}", pf.getName(), pf.getId(), pf.getValue())
                    );
                }
                long deviceId = packet.getDevice().getId();
                long packetId = packet.getId();
                log.trace("emitting packet {} with id {} belonging to device with id {}", packet, packetId, deviceId);
                collector.emit(new Values(deviceId + "." + packetId, packet));
                collector.ack(input);   // ack tuple
                log.debug("Tuple sent to SelectionBolt");
            } catch (Throwable e) {
                log.error("Error sending tuple to SelectionBolt: ", e);
                HyperIoTTopologyError hyperIoTTopologyError = HyperIoTTopologyError.builder()
                        .errorMessage(e.getMessage())
                        .errorType(e.getClass().getSimpleName())
                        .build();
                try {
                    BoltUtil.handleHyperIoTError(input, collector, hyperIoTTopologyError, hProjectId,
                            objectMapper.writeValueAsString(packet), (long) packet.getFieldValue(packet.getTimestampField()),
                            String.format(HPROJECT_ERROR_STREAM_ID, hProjectId));
                } catch (JsonProcessingException jsonProcessingException) {
                    log.error("Error writing json from hpacket: ", jsonProcessingException);
                    collector.ack(input);
                }
            }
        } else {
            log.error("Could not send tuple to SelectionBolt: packet is null");
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        // FieldNameBasedTupleToKafkaMapper used by KafkaBolt must specify "packet" in the constructor as field name and "key" for the key name
        declarer.declare(new Fields(StormConstants.DEVICEID_PACKETID_FIELD, StormConstants.PACKET_FIELD));
        declarer.declareStream(String.format(HPROJECT_ERROR_STREAM_ID, hProjectId),
                new Fields(StormConstants.TIMESTAMP_FIELD, StormConstants.MESSAGE_FIELD, StormConstants.RECEIVED_PACKET_FIELD));
        declarer.declareStream(String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, hProjectId),
                new Fields(StormConstants.ROWKEY_FIELD, StormConstants.TIMESTAMP_FIELD, StormConstants.STEP_FIELD));
    }

    private void ruleEngineRun(it.acsoftware.hyperiot.hpacket.model.HPacket packet) {
        log.trace("executing RuleEngine on {}", packet);
        try {
            ruleEngine.check(packet, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("Throwing FailedException: RuleEngine error caused by {}", e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
            throw new FailedException("RuleEngine error in EnrichmentBolt");
        }
    }

    /**
     * This is a config method, it is called in topology.yaml file
     */
    @SuppressWarnings("unused")
    public EnrichmentBolt withHProject(String hProjectId) {
        this.hProjectId = Long.parseLong(hProjectId);
        return this;
    }

}

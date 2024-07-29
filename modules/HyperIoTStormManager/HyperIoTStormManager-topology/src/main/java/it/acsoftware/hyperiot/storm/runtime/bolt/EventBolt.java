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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.alarm.service.actions.AlarmAction;
import it.acsoftware.hyperiot.alarm.service.actions.NoAlarmAction;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.rule.model.RuleEngineAsynchronousObserver;
import it.acsoftware.hyperiot.rule.model.facts.FiredRule;
import it.acsoftware.hyperiot.rule.service.RuleEngine;
import it.acsoftware.hyperiot.storm.alarm.AlarmState;
import it.acsoftware.hyperiot.storm.alarm.AlarmStateTransitionManager;
import it.acsoftware.hyperiot.storm.alarm.HyperIoTHBaseRuleStateTableUtils;
import it.acsoftware.hyperiot.storm.runtime.bolt.util.BoltUtil;
import it.acsoftware.hyperiot.storm.topology.TopologyConfigKeys;
import it.acsoftware.hyperiot.storm.util.StormConstants;
import it.acsoftware.hyperiot.stormmanager.model.HyperIoTTopologyError;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.drools.core.common.DefaultFactHandle;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static it.acsoftware.hyperiot.storm.util.StormConstants.HPROJECT_ERROR_STREAM_ID;
import static it.acsoftware.hyperiot.storm.util.StormConstants.TIMELINE_HPROJECT_STREAM_ID_PREFIX;

public class EventBolt extends BaseRichBolt implements RuleEngineAsynchronousObserver {

    private static final Logger log = LoggerFactory.getLogger(EventBolt.class);
    private Map config;
    private OutputCollector collector;
    private long hProjectId;
    private RuleEngine ruleEngine;
    private ObjectMapper objectMapper;
    private static final String KAFKA_REALTIME_TOPIC_STREAM_ID = "kafkaRealtimeTopicEvent";
    private static final String EVENT_RULE_IDS_PARSING_ERROR_MESSAGE = "Event rule ids do not match regex";
    private static final String ALARM_EVENT_RULE_IDS_PARSING_ERROR_MESSAGE = "Alarm Event rule ids do not match regex";
    private static final String KAFKA_MESSAGE_KEY = "0.0";

    private static final String ALARM_EVENT_RULE_TYPE = "ALARM_EVENT";

    private AlarmStateTransitionManager alarmStateTransitionManager;


    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        log.debug("prepare with config {} context {}", stormConf, context);
        config = stormConf;
        this.collector = collector;
        Map<String, String> rules = (Map<String, String>) (config.get("rule-engine.drools"));
        for (Map.Entry<String, String> entry : rules.entrySet()) {
            log.debug("In Event Bolt Prepare : Key is {} /n  Value is : {} ", entry.getKey(), entry.getValue());
        }
        ruleEngine = new RuleEngine(new ArrayList<>(rules.values()),
                Long.parseLong(config.get(TopologyConfigKeys.CONFIG_PROJECT_ID).toString()), true);
        long[] eventRuleIds = extractRuleIdFromStormConfiguration(TopologyConfigKeys.CONFIG_EVENT_RULE_IDS, EVENT_RULE_IDS_PARSING_ERROR_MESSAGE);
        log.info("In Event Bolt, Event rule ids is  : {} ", Arrays.toString(eventRuleIds));
        long[] alarmEventRuleIds = extractRuleIdFromStormConfiguration(TopologyConfigKeys.CONFIG_ALARM_EVENT_RULE_IDS, ALARM_EVENT_RULE_IDS_PARSING_ERROR_MESSAGE);
        log.info("In Event Bolt, Alarm Event Rule Ids is : {} ", Arrays.toString(alarmEventRuleIds));
        //Initialize ObjectMapper.
        objectMapper = new ObjectMapper();

        String ruleStateTableName = String.format(StormConstants.EVENT_RULE_STATE_TABLE_NAME_PREFIX, hProjectId);
        String hbaseClientConfigKey = StormConstants.HBASE_BOLT_CONF;
        log.info("In Event Bolt, read rule state from hbase table {} , configKey is {}", ruleStateTableName, hbaseClientConfigKey);
        //Load last rule state (if rule is registered) from HBase hproject_event_rule_state_<projectId> table.
        Map<Long, FiredRule> ruleStateMap = HyperIoTHBaseRuleStateTableUtils.retrieveRuleStateFromHBase(stormConf, ruleStateTableName, hbaseClientConfigKey);
        //Retrieve from storm config alarm.event.rule.map property and serialize them as an HashMap.
        //This property is used to keep track of the relationship between alarm and rule related to them.
        //(Alarm is related to a list of AlarmEvent, and every AlarmEvent is related to one Rule)
        HashMap<Long, Set<Long>> alarmEventRuleMap = deserializeAlarmRuleMapFromStormConfig();
        configureAlarmStateTransitionManager(ruleStateMap, alarmEventRuleMap);
        configureKieSessionWithRulesState(ruleStateMap, alarmEventRuleIds, eventRuleIds);
        ruleEngine.addAsyncObserver(this);
        ruleEngine.start();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(StormConstants.MESSAGE_TYPE_FIELD, StormConstants.EVENT_JSON_FIELD));
        declarer.declareStream(KAFKA_REALTIME_TOPIC_STREAM_ID, new Fields(StormConstants.DEVICEID_PACKETID_FIELD, StormConstants.PACKET_FIELD));
        declarer.declareStream(String.format(StormConstants.EVENT_HPROJECT_STREAM_ID_PREFIX, hProjectId), new Fields(StormConstants.PACKET_FIELD, StormConstants.EVENT_RULE_ID));
        declarer.declareStream(String.format(StormConstants.ALARM_EVENT_HPROJECT_STREAM_ID_PREFIX, hProjectId), new Fields(StormConstants.PACKET_FIELD, StormConstants.ALARM_EVENT_DEVICE_ID, StormConstants.ALARM_STATE_FIELD));
        declarer.declareStream(String.format(StormConstants.EVENT_RULE_STATE_HBASE_STREAM_ID, hProjectId), new Fields(StormConstants.EVENT_RULE_ID, StormConstants.EVENT_RULE_STATE_INFO_FIELD));
        declarer.declareStream(String.format(HPROJECT_ERROR_STREAM_ID, hProjectId),
                new Fields(StormConstants.TIMESTAMP_FIELD, StormConstants.MESSAGE_FIELD, StormConstants.RECEIVED_PACKET_FIELD));
        declarer.declareStream(String.format(TIMELINE_HPROJECT_STREAM_ID_PREFIX, hProjectId),
                new Fields(StormConstants.ROWKEY_FIELD, StormConstants.TIMESTAMP_FIELD, StormConstants.STEP_FIELD));
    }

    @Override
    public void cleanup() {
        this.ruleEngine.stop();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Tuple input) {
        log.trace("execute on tuple {} with collector {}", input, collector);
        HPacket packet = (HPacket) input.getValueByField("hpacket");
        log.debug("Device Id : {}, Packet Id in Event Bolt : {}", packet.getDevice().getId(), packet.getId());
        try {
            this.ruleEngine.check(packet, System.currentTimeMillis());
            processRuleEngineActions(packet);
            collector.ack(input);
        } catch (Throwable e) {
            log.error("Error sending tuple from EventBolt: ", e);
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
    }

    @Override
    public void processData(HPacket packet) {
        try {
            processRuleEngineActions(packet);
        } catch (Throwable e) {
            log.error("Error processing async event from rule engine:", e);
        }
    }

    private void processRuleEngineActions(HPacket packet) throws ClassNotFoundException, IOException {
        KieSession session = ruleEngine.getSession();
        ArrayList<String> actions = session == null ? null : (ArrayList<String>) session.getGlobals().get("actions");
        if (actions != null) {
            for (String base64JsonInstance : actions) {
                String jsonInstance = new String(Base64.getDecoder().decode(base64JsonInstance));
                log.debug("In Event Bolt jsonInstance is : {} ", jsonInstance);
                TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
                };
                //using temp mapper in order to convert every number to long not integer
                HashMap<String, Object> jsonActionMap = objectMapper.readValue(jsonInstance, typeRef);
                //enrich the action object with timestamp of the event and current payload.
                long packetTimestamp = (Long) packet.getFieldValue(packet.getTimestampField());
                //convert to millis
                if (packet.isUnixTimestampFormatSeconds())
                    packetTimestamp *= 1000;
                //forcing convertion to arrat of integer to array
                //todo convert directly to long
                List<Integer> packetIdsAsInt = new ArrayList<>((List<Integer>) jsonActionMap.get("packetIds"));
                List<Long> packetIds = new ArrayList<>();
                packetIdsAsInt.forEach(packetId -> packetIds.add(packetId.longValue()));
                String firePayload = ruleEngine.getPacketsContentFromSession(packetIds);
                jsonActionMap.put("firePayload", firePayload);
                jsonActionMap.put("fireTimestamp", packetTimestamp);
                jsonInstance = objectMapper.writeValueAsString(jsonActionMap);
                log.debug("In Event Bolt, after deserialize jsonInstance in map");
                String actionRuleType = jsonActionMap.get("ruleType").toString();
                log.debug("In Event Bolt, actionRuleType is {} ", actionRuleType);
                if (actionRuleType.equals(ALARM_EVENT_RULE_TYPE)) {
                    log.info("In Event Bolt, Action {} is an alarm", jsonInstance);
                    String actionClassName = jsonActionMap.get("actionName").toString();
                    log.debug("In Event Bolt, actionClassName is {} ", actionClassName);
                    Class<?> ruleActionClass = Class.forName(actionClassName);
                    AlarmAction action = (AlarmAction) objectMapper.readValue(jsonInstance, ruleActionClass);
                    handleAlarm(ruleEngine.getSession(), action, packet, jsonInstance);
                } else {
                    log.info("In Event Bolt, Action {} is a regular event", jsonInstance);
                    // TODO: use HyperIoTKafkaConnector model 'SystemMessageType' enumeration
                    log.trace("emitting event signal 'PROCESS_EVENT' with JSON data {}", jsonInstance);
                    handleEvent(packet, jsonInstance);
                }
            }
            actions.clear();
        } else {
            log.debug("In EventBolt, no actions register in KieSession. Kie session is {} ", session != null ? "not null" : "null");
        }
    }

    private void handleAlarm(KieSession session, AlarmAction action, HPacket packet, String jsonInstance) {
        log.debug("In EventBolt handleAlarm");
        FiredRule rule = retrieveFiredRuleFromSession(session, action.getRuleId());
        if (rule != null) {
            emitRuleStateToHBaseBolt(rule);
            if (alarmStateTransitionManager.ruleTriggerAlarmStateTransition(rule, action)) {
                try {
                    log.debug("FiredRule is ruleId {} , ruleHasBeenFired {}", rule.getRuleId(), rule.isFired());
                    boolean ruleHasBeenFired = rule.isFired();
                    AlarmState alarmState;
                    if (ruleHasBeenFired) {
                        log.info("Alarm must go up");
                        alarmState = AlarmState.UP;
                    } else {
                        log.info("Alarm must go down");
                        alarmState = AlarmState.DOWN;
                    }
                    long deviceId = packet.getDevice().getId();
                    Map<String, Object> eventActionRule = objectMapper.readValue(jsonInstance, HashMap.class);
                    HPacket alarmEventPacket = BoltUtil.createHPacketForEventRelatedToAlarm(packet, eventActionRule, jsonInstance, alarmState);
                    log.debug("In Event Bolt after create HPacketEvent for alarm");
                    //(emit alarm event packet on kafka)
                    collector.emit(KAFKA_REALTIME_TOPIC_STREAM_ID, new Values(KAFKA_MESSAGE_KEY, alarmEventPacket));
                    //If action isn't a NoAlarmAction (An action related to alarm but with no code execution associated), send to topic on which read HyperIoTRuleEventActionListener.
                    if (!(action.getClass().isAssignableFrom(NoAlarmAction.class))) {
                        log.debug("In Event Bolt, Emit to default stream to process alarm event");
                        collector.emit(new Values("PROCESS_EVENT", jsonInstance));
                    }
                    String alarmStreamId = String.format(StormConstants.ALARM_EVENT_HPROJECT_STREAM_ID_PREFIX, hProjectId);
                    collector.emit(alarmStreamId, new Values(alarmEventPacket, deviceId, alarmState.toString()));
                } catch (IOException e) {
                    log.error("Could not write alarm on HBase", e);
                }
            } else {
                log.info("FiredRule is ruleId {} , ruleHasBeenFired {} . No Alarm state transition", rule.getRuleId(), rule.isFired());
            }
        } else {
            log.error("Could not find fact about alarm rule with ID {}: do not handle it!", action.getRuleId());
        }
    }

    private void handleEvent(HPacket packet, String jsonInstance) {
        log.debug("Emit to default stream");
        collector.emit(new Values("PROCESS_EVENT", jsonInstance));
        String eventStreamId = String.format(StormConstants.EVENT_HPROJECT_STREAM_ID_PREFIX, hProjectId);
        log.debug("Emit to " + KAFKA_REALTIME_TOPIC_STREAM_ID + " stream and to "
                + eventStreamId + " stream");
        log.info("Creating HPacket containing the event");
        try {
            Map<String, Object> rule = objectMapper.readValue(jsonInstance, HashMap.class);
            HPacket eventPacket = BoltUtil.createHPacketEvent(packet, rule, jsonInstance);
            // emit to Kafka
            collector.emit(KAFKA_REALTIME_TOPIC_STREAM_ID, new Values(KAFKA_MESSAGE_KEY, eventPacket));
            // emit to Selection bolt
            int ruleId = (int) rule.get("ruleId");
            collector.emit(eventStreamId, new Values(eventPacket, ruleId));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private FiredRule retrieveFiredRuleFromSession(KieSession session, long ruleId) {
        for (FactHandle fact : session.getFactHandles()) {
            DefaultFactHandle defaultFactHandle = (DefaultFactHandle) fact;
            if (defaultFactHandle.getObject() instanceof FiredRule) {
                FiredRule fr = (FiredRule) defaultFactHandle.getObject();
                if (fr.getRuleId() == ruleId) {
                    return fr;
                }
            }
        }
        return null;
    }

    /**
     * This is a config method, it is called in topology.yaml file
     */
    @SuppressWarnings("unused")
    public EventBolt withHProject(String hProjectId) {
        this.hProjectId = Long.parseLong(hProjectId);
        return this;
    }

    private long[] extractRuleIdFromStormConfiguration(String topologyConfigKey, String errorMessage) {
        log.debug("In Event Bolt  extractRuleIdFromStormConfiguration, topologyConfigKey key is : {} ", topologyConfigKey);
        String ruleIds = config.get(topologyConfigKey).toString();
        log.debug("In Event Bolt rule id for topologyConfigKey  {} , is : {}", topologyConfigKey, ruleIds);
        if (!Pattern.matches("\\[(\\d+)*(, \\d+)*\\]", ruleIds)) {
            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        return ruleIds.equals("[]") ? new long[0] :
                Arrays.stream(ruleIds.substring(1, ruleIds.length() - 1)    // remove "[" and "]"
                        .split(", ")).mapToLong(Long::parseLong).toArray();
    }

    private void configureAlarmStateTransitionManager(Map<Long, FiredRule> rulesStateMap, HashMap<Long, Set<Long>> alarmEventRuleMap) {
        log.info("In Event Bolt  configureAlarmStateTransitionManager");
        try {
            alarmStateTransitionManager = new AlarmStateTransitionManager(rulesStateMap, alarmEventRuleMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException("Error during configuration of AlarmStateTransitionManager");
        }
    }

    private HashMap<Long, Set<Long>> deserializeAlarmRuleMapFromStormConfig() {
        log.info("In Event Bolt  deserializeAlarmRuleMapFromStormConfig");
        //Get config property from yaml file
        String alarmEventRuleMapConfig = config.get(TopologyConfigKeys.CONFIG_ALARM_EVENT_RULE_MAP).toString();
        log.info("In EventBolt deserializeAlarmRuleMapFromStormConfig, alarmEventRuleMapConfig is : {} ", alarmEventRuleMapConfig);
        try {
            TypeReference<HashMap<String, Set<String>>> typeRef = new TypeReference<HashMap<String, Set<String>>>() {
            };
            Map<String, Set<String>> alarmEventRuleMapSerialized = objectMapper.readValue(alarmEventRuleMapConfig, typeRef);
            HashMap<Long, Set<Long>> alarmEventRuleMap = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : alarmEventRuleMapSerialized.entrySet()) {
                long alarmId = Long.parseLong(entry.getKey());
                HashSet<Long> alarmEventRuleIds = new HashSet<>();
                for (String ruleId : entry.getValue()) {
                    alarmEventRuleIds.add(Long.parseLong(ruleId));
                }
                alarmEventRuleMap.put(alarmId, alarmEventRuleIds);
            }
            return alarmEventRuleMap;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IllegalArgumentException("In Event Bolt deserializeAlarmRuleMapFromStormConfig, error during deserialization of alarmEventRuleMap");
        }
    }

    private void configureKieSessionWithRulesState(Map<Long, FiredRule> ruleStateMap, long[] alarmEventRuleIds, long[] eventRuleIds) {
        log.info("In EventBolt configureKieSessionWithRulesState");
        Set<Long> allEvents = new HashSet<>();

        allEvents.addAll(Arrays.stream(alarmEventRuleIds)
                .boxed()
                .collect(Collectors.toList()));
        allEvents.addAll(Arrays.stream(eventRuleIds)
                .boxed()
                .collect(Collectors.toList()));

        for (long ruleId : allEvents) {
            //If the map does not contain the rule id it means that the rule state has never been saved on hbase table hproject_rule_state_<projectId>
            boolean isFired = ruleStateMap.containsKey(ruleId) ? ruleStateMap.get(ruleId).isFired() : false;
            Date lastFiredTimestamp = ruleStateMap.containsKey(ruleId) ? ruleStateMap.get(ruleId).getLastFiredTimestamp() : null;
            log.info("In EventBolt configureKieSessionWithRulesState add alarm event rule to session, ruleId is {} , fired is {} ", ruleId, isFired);
            ruleEngine.insertFiredRuleFact(ruleId, isFired, lastFiredTimestamp);
        }
    }

    private void emitRuleStateToHBaseBolt(FiredRule firedRule) {
        try {
            log.info("In EventBolt emitRuleStateToHBaseBolt, rule id is : {} , rule is {} ", firedRule.getRuleId(), firedRule.isFired() ? "ACTIVE" : "INACTIVE");
            String ruleStateSerialized = objectMapper.writeValueAsString(firedRule);
            log.debug("In EventBolt emitRuleStateToHBaseBolt, rule state serialized is {} ", ruleStateSerialized);
            long rowKey = firedRule.getRuleId();
            log.debug("In EventBolt emitRuleStateToHBaseBolt, Emitting record to hbase....");
            collector.emit(String.format(StormConstants.EVENT_RULE_STATE_HBASE_STREAM_ID, hProjectId), new Values(rowKey, ruleStateSerialized));
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            log.error("In EventBolt emitRuleStateToHBaseBolt, Failed to emit rule state");
        }
    }

}

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

package it.acsoftware.hyperiot.storm.builder;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.stormmanager.model.HyperIoTTopologyConfig;
import it.acsoftware.hyperiot.stormmanager.model.StormManager;
import it.acsoftware.hyperiot.stormmanager.model.TopologyConfig;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Aristide Cittadino
 * Class which implements HyperIoT Topology configuration creation
 */
public class HyperIoTTopologyConfigBuilder {
    private static Logger logger = LoggerFactory.getLogger(HyperIoTTopologyConfigBuilder.class);

    /**
     * @param projectId
     * @return
     * @throws IOException
     */
    public static TopologyConfig getTopologyConfig(long projectId, String topologyName) throws IOException {
        HProjectSystemApi hProjectSystemApi = (HProjectSystemApi) HyperIoTUtil.getService(HProjectSystemApi.class);
        HProject hProject = hProjectSystemApi.find(projectId, null);
        TopologyConfig topologyConfig = new TopologyConfig();
        topologyConfig.name = topologyName;
        HyperIoTTopologyConfig topologyConfigParts = new HyperIoTTopologyConfig();
        //creting configuration from hdevices and packets
        defineTopologyConfigParts(topologyConfigParts, hProject);
        //creating yaml configuration
        String topologyYaml = createTopologyYaml(topologyConfig.name, hProject, topologyConfigParts.packetConfig.toString());
        //creating properties
        String topologyProperties = createTopologyProperties(projectId, topologyConfigParts.properties.toString());

        // add generated configs
        topologyConfig.properties = topologyProperties;
        topologyConfig.yaml = topologyYaml;
        return topologyConfig;
    }

    private static String getTopologyOnHeapMemory(HProject hProject) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(hProject).getResourcesOnHeapMemory();
    }

    /**
     * @param hProject
     * @return
     */
    private static String getTopologyMaxHeapSize(HProject hProject) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(hProject).getTopologyWorkerMaxHeapSize();
    }

    /**
     * @param hProject
     * @return
     */
    private static String getTopologyLogWriterXmx(HProject hProject) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(hProject).getLogWriterXmx();
    }

    /**
     * @param hProject
     * @return
     */
    private static String getTopologyLogWriterXms(HProject hProject) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(hProject).getLogWriterXms();
    }

    /**
     * @param hProject
     * @return
     */
    private static int getTopologyWorkers(HProject hProject) {
        return HyperIoTTopologyPerformanceConfig.fromHProject(hProject).getTopologyWorkers();
    }

    /**
     * @param projectId
     * @param topologyConfigProperties
     * @return
     * @throws IOException
     */
    private static String createTopologyProperties(long projectId, String topologyConfigProperties) throws IOException {
        // generate topology.properties
        String topologyProperties = readBundleResource("topology.properties");
        topologyProperties = topologyProperties.replace("%packets%", topologyConfigProperties);
        topologyProperties = topologyProperties.replace("%drools-enrichment%",
                getDroolsCode(projectId, RuleType.ENRICHMENT));
        topologyProperties = topologyProperties.replace("%drools-event%",
                getDroolsCode(projectId, RuleType.EVENT));
        topologyProperties = topologyProperties.replace("%drools-alarm-event%",
                getDroolsCode(projectId, RuleType.ALARM_EVENT));
        topologyProperties = topologyProperties.replace("%kafka-bootstrap-servers%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.kafka.bootstrap.servers"));
        topologyProperties = topologyProperties.replace("%hdfs-namenode-hosts%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hdfs.namenode.hosts"));
        topologyProperties = topologyProperties.replace("%hfds-write-dir%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.hproject.hdfs.write.dir"));
        topologyProperties = topologyProperties.replace("%hbase-root-dir%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hbase.root.dir"));
        topologyProperties = topologyProperties.replace("%zookeeper-hosts%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hbase.zookeeper.quorum"));
        topologyProperties = topologyProperties.replace("%zookeeper-client-port%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hbase.zookeeper.client.port"));

        //topologyProperty that add configuration needed to handle hbase/hdfs failure
        topologyProperties = topologyProperties.replace("%it.acsoftware.hyperiot.storm.hbase.client.batch.size%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.batch.size"));

        topologyProperties = topologyProperties.replace("%it.acsoftware.hyperiot.storm.hbase.client.flush.interval.seconds%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.flush.interval.seconds"));

        topologyProperties = topologyProperties.replace("%hbase.client.operation.timeout%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.operation.timeout.millisecond"));

        topologyProperties = topologyProperties.replace("%hbase.client.retries.number%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.retries.number"));

        topologyProperties = topologyProperties.replace("%it.acsoftware.hyperiot.storm.initialDelayKafkaSpoutDLQ.seconds%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.initialDelayKafkaSpoutDLQ.seconds"));

        topologyProperties = topologyProperties.replace("%it.acsoftware.hyperiot.storm.progressiveDelayFactorKafkaSpoutDLQ.seconds%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.progressiveDelayFactorKafkaSpoutDLQ.seconds"));

        topologyProperties = topologyProperties.replace("%it.acsoftware.hyperiot.storm.maxRetryKafkaSpoutDLQ%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.maxRetryKafkaSpoutDLQ"));

        topologyProperties = topologyProperties.replace("%it.acsoftware.hyperiot.storm.maxDelayKafkaSpoutDLQ.seconds%",
                (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.maxDelayKafkaSpoutDLQ.seconds"));


        return topologyProperties;
    }

    /**
     * @param topologyName
     * @param hProject
     * @param packetsConfig
     * @return
     * @throws IOException
     */
    private static String createTopologyYaml(String topologyName, HProject hProject, String packetsConfig) throws IOException {
        long projectId = hProject.getId();
        int numWorkers = getTopologyWorkers(hProject);
        String logWriterXms = getTopologyLogWriterXms(hProject);
        String logWriterXmx = getTopologyLogWriterXmx(hProject);
        String topologyMaxHeapSize = getTopologyMaxHeapSize(hProject);
        String topologyOnHeapMemory = getTopologyOnHeapMemory(hProject);

        // generate topology.yaml
        String topologyYaml = readBundleResource("topology.yaml");
        topologyYaml = topologyYaml.replace("%topology-name%", topologyName);
        topologyYaml = topologyYaml.replace("%topology_workers%", String.valueOf(numWorkers));
        topologyYaml = topologyYaml.replace("%worker_max_heap_size%", topologyMaxHeapSize);
        topologyYaml = topologyYaml.replace("%topology_onheap_memory%", topologyOnHeapMemory);
        topologyYaml = topologyYaml.replace("%topology_logw_xmx%", logWriterXmx);
        topologyYaml = topologyYaml.replace("%topology_logw_xms%", logWriterXms);
        topologyYaml = topologyYaml.replace(
                "%hproject-id%", "\"" + projectId + "\"");
        List<Long> eventRuleIds = getEventRuleIds(projectId);
        topologyYaml = topologyYaml.replace(
                "%event-rule-ids%", "\"" + Arrays.toString(eventRuleIds.toArray()) + "\"");
        List<Long> alarmEventRuleIds = getAlarmEventRuleIds(projectId);
        topologyYaml = topologyYaml.replace(
                "%alarm-event-rule-ids%", "\"" + Arrays.toString(alarmEventRuleIds.toArray()) + "\"");
        topologyYaml = topologyYaml.replace("%alarm-event-rule-map%", getAlarmEventRuleMapConfig(alarmEventRuleIds));
        // DeserializationBolt and SelectionBolt needs projectId to send to HBase tables
        topologyYaml = topologyYaml.replace("%hproject-id%", String.valueOf(projectId));
        topologyYaml = topologyYaml.replace("%packets-config%", packetsConfig);
        //topologyProperty that add configuration needed to handle hbase/hdfs failure
        topologyYaml = topologyYaml.replace("%{hbase.client.operation.timeout}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.operation.timeout.millisecond"));
        topologyYaml = topologyYaml.replace("%{hbase.client.retries.number}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.retries.number"));
        topologyYaml = topologyYaml.replace("%{it.acsoftware.hyperiot.storm.hbase.client.batch.size}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.batch.size"));
        topologyYaml = topologyYaml.replace("%{it.acsoftware.hyperiot.storm.hbase.client.flush.interval.seconds}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.hbase.client.flush.interval.seconds"));
        topologyYaml = topologyYaml.replace("%{it.acsoftware.hyperiot.storm.initialDelayKafkaSpoutDLQ.seconds}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.initialDelayKafkaSpoutDLQ.seconds"));
        topologyYaml = topologyYaml.replace("%{it.acsoftware.hyperiot.storm.progressiveDelayFactorKafkaSpoutDLQ.seconds}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.progressiveDelayFactorKafkaSpoutDLQ.seconds"));
        topologyYaml = topologyYaml.replace("%{it.acsoftware.hyperiot.storm.maxRetryKafkaSpoutDLQ}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.maxRetryKafkaSpoutDLQ"));
        topologyYaml = topologyYaml.replace("%{it.acsoftware.hyperiot.storm.maxDelayKafkaSpoutDLQ.seconds}%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.storm.maxDelayKafkaSpoutDLQ.seconds"));


        return topologyYaml;
    }

    /**
     * @param topologyConfigParts
     * @param hProject
     */
    private static void defineTopologyConfigParts(HyperIoTTopologyConfig topologyConfigParts, HProject hProject) {
        HDeviceSystemApi deviceSystemService = (HDeviceSystemApi) HyperIoTUtil.getService(HDeviceSystemApi.class);
        Collection<HDevice> devices = deviceSystemService.getProjectDevicesList(hProject.getId());
        devices.forEach((d) -> {
            try {
                HPacketSystemApi hPacketSystemApi = (HPacketSystemApi) HyperIoTUtil.getService(HPacketSystemApi.class);
                Collection<HPacket> packets = hPacketSystemApi.getPacketsList(d.getId());
                HyperIoTTopologyConfig topologyDeviceConfig = HyperIoTTopologyConfigBuilder.createHyperIoTTopologyConfig(d, packets);
                topologyConfigParts.packetConfig.append(topologyDeviceConfig.packetConfig);
                topologyConfigParts.properties.append(topologyDeviceConfig.properties);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    /**
     * @param device
     * @return
     * @throws IOException
     */
    private static HyperIoTTopologyConfig createHyperIoTTopologyConfig(HDevice device, Collection<HPacket> packets) throws IOException {
        HProject project = device.getProject();
        // load properties file template
        String packetConfigTemplate = readBundleResource("packet-config.yaml");
        // topology config parts
        HyperIoTTopologyConfig topologyConfig = new HyperIoTTopologyConfig();
        topologyConfig.project = project;
        topologyConfig.device = device;
        topologyConfig.packets = packets;
        packets.forEach((p) -> {
            // build JSON schema for deserialization bolt
            HashMap<String, Object> packetData = new HashMap<>();
            packetData.put("name", p.getName());
            packetData.put("type", p.getType().getName());
            packetData.put("unixTimestamp", p.isUnixTimestamp());
            packetData.put("unixTimestampFormatSeconds", p.isUnixTimestampFormatSeconds());
            HashMap<String, Object> schema = new HashMap<>();
            packetData.put("schema", schema);
            schema.put("type", p.getFormat().getName());
            schema.put("fields", p.getFlatFieldsMap());
            //add timestamp information
            HashMap<String, Object> timestampInformation = new HashMap<>();
            timestampInformation.put("format", p.getTimestampFormat());
            timestampInformation.put("field", p.getTimestampField());
            packetData.put("timestamp", timestampInformation);
            //add traffic plan information
            packetData.put("trafficPlan", p.getTrafficPlan().getName().toLowerCase());
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String jsonSchema = objectMapper.writeValueAsString(packetData);
                String tp = "packet." + p.getId() + "='" + jsonSchema + "'" + "\n";
                topologyConfig.properties.append(tp);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            // apply templates
            topologyConfig.packetConfig
                    .append(packetConfigTemplate.replaceAll("%pid%", String.valueOf(p.getId())));
        });
        return topologyConfig;
    }

    /**
     * @param projectId
     * @return
     */
    private static List<Long> getEventRuleIds(long projectId) {
        RuleEngineSystemApi ruleEngineSystemApi = (RuleEngineSystemApi) HyperIoTUtil.getService(RuleEngineSystemApi.class);
        if (ruleEngineSystemApi != null) {
            Collection<Rule> rules = ruleEngineSystemApi.findAllRuleByProjectIdAndRuleType(projectId, RuleType.EVENT);
            return rules.stream().map(rule -> rule.getId()).collect(Collectors.toList());
        }
        throw new HyperIoTRuntimeException("No HyperIoT Rule Engine Api Found");
    }

    /**
     * @param projectId HProject's id
     * @return the id of the alarm event's entity related to the project.
     * (AlarmEvent is related to a Rule entity, and Rule entity is related to the HProject.
     * Rule's entity relative to AlarmEvent is characterized by the ALARM_EVENT RuleType attribute)
     */
    private static List<Long> getAlarmEventRuleIds(long projectId) {
        RuleEngineSystemApi ruleEngineSystemApi = (RuleEngineSystemApi) HyperIoTUtil.getService(RuleEngineSystemApi.class);
        if (ruleEngineSystemApi != null) {
            Collection<Rule> rules = ruleEngineSystemApi.findAllRuleByProjectIdAndRuleType(projectId, RuleType.ALARM_EVENT);
            return rules.stream().map(rule -> rule.getId()).collect(Collectors.toList());
        }
        throw new HyperIoTRuntimeException("No HyperIoT Rule Engine Api Found");
    }

    /**
     * Utility method to valorize the alarm.event.rule.map property of topology.yaml file.
     *
     * @param alarmEventRuleIds alarmEvents id related to rule related to the project for which we submit the topology.
     * @return a String representation of a Map where :
     * 1)Map key : is the id of the alarm.
     * 2)Map value : is the list of rule's id that characterized AlarmEvent related to Alarm.
     */
    private static String getAlarmEventRuleMapConfig(List<Long> alarmEventRuleIds) {
        Collection<AlarmEvent> alarmEventList = getAlarmEventListByRulesId(alarmEventRuleIds);
        Map<String, Set<String>> alarmRulesMap = new HashMap<>();
        for (AlarmEvent alarmEvent : alarmEventList) {
            String alarmId = String.valueOf(alarmEvent.getAlarm().getId());
            String ruleId = String.valueOf(alarmEvent.getEvent().getId());
            if (alarmRulesMap.containsKey(alarmId)) {
                Set<String> alarmRulesList = alarmRulesMap.get(alarmId);
                alarmRulesList.add(ruleId);
            } else {
                Set<String> alarmRulesList = new HashSet<>();
                alarmRulesList.add(ruleId);
                alarmRulesMap.put(alarmId, alarmRulesList);
            }
        }
        try {
            //Serialize data for storm configuration.
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(alarmRulesMap);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new HyperIoTRuntimeException("Alarm Event Rule State Map serialization fail ");
        }
    }

    private static Collection<AlarmEvent> getAlarmEventListByRulesId(List<Long> alarmEventRuleIds) {
        AlarmEventSystemApi alarmEventSystemApi = (AlarmEventSystemApi) HyperIoTUtil.getService(AlarmEventSystemApi.class);
        if (alarmEventSystemApi != null) {
            if (alarmEventRuleIds != null && !alarmEventRuleIds.isEmpty()) {
                HyperIoTQuery byRuleIds = HyperIoTQueryBuilder.newQuery();
                boolean isFirstCondition = true;
                for (long ruleId : alarmEventRuleIds) {
                    if (isFirstCondition) {
                        byRuleIds = byRuleIds.equals("event.id", ruleId);
                        isFirstCondition = false;
                    } else {
                        byRuleIds = byRuleIds.or(HyperIoTQueryBuilder.newQuery().equals("event.id", ruleId));
                    }
                }
                return alarmEventSystemApi.findAll(byRuleIds, null);
            } else {
                return new ArrayList<>();
            }
        }
        throw new HyperIoTRuntimeException("No HyperIoT AlarmEventSystemApi Found");
    }

    /**
     * @param path
     * @return
     * @throws IOException
     */
    private static String readBundleResource(String path) throws IOException {
        URL url = FrameworkUtil.getBundle(StormManager.class).getResource(path);
        BufferedReader br = new BufferedReader(
                new InputStreamReader(url.openConnection().getInputStream()));
        StringBuilder buffer = new StringBuilder();
        while (br.ready()) {
            buffer.append(br.readLine()).append("\n");
        }
        br.close();
        return buffer.toString();
    }

    /**
     * @param projectId
     * @param ruleType
     * @return
     */
    private static String getDroolsCode(long projectId, RuleType ruleType) {
        StringBuilder droolsCode = new StringBuilder();
        RuleEngineSystemApi ruleEngineSystemApi = (RuleEngineSystemApi) HyperIoTUtil.getService(RuleEngineSystemApi.class);
        if (ruleEngineSystemApi != null) {
            String[] droolsLines = ruleEngineSystemApi.getDroolsForProject(projectId, ruleType)
                    .split("\n");
            int lines = droolsLines.length;
            for (String l : droolsLines) {
                droolsCode.append(l);
                if (--lines == 0) {
                    droolsCode.append("\n");
                } else if (!l.trim().isEmpty()) {
                    droolsCode.append(" \\\n    ");
                }
            }
            return droolsCode.toString();
        }
        throw new HyperIoTRuntimeException("No HyperIoT Rule Engine Api Found");
    }
}

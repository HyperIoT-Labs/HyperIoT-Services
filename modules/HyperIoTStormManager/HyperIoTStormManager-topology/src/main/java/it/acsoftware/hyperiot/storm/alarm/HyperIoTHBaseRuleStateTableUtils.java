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

package it.acsoftware.hyperiot.storm.alarm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.hproject.util.hbase.HProjectHBaseConstants;
import it.acsoftware.hyperiot.rule.model.facts.FiredRule;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.storm.Config;
import org.apache.storm.hbase.common.HBaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Francesco Salerno
 * <p>
 * Utility class used to load last state of HProject rules.
 * It uses internally the same HBaseClient used by org.apache.storm.hbase.bolt.HBaseBolt
 */
public class HyperIoTHBaseRuleStateTableUtils {

    private static final Logger log = LoggerFactory.getLogger(HyperIoTHBaseRuleStateTableUtils.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private HyperIoTHBaseRuleStateTableUtils() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Map<Long, FiredRule> retrieveRuleStateFromHBase(Map<String, Object> topologyConf, String tableName, String configKey) {
        log.info("In HyperIoTHBaseRuleStateTableUtils, retrieveRuleStateFromHBase, tableName is {}, configKey is {} ", tableName, configKey);
        Map<Long, FiredRule> firedRules = new HashMap<>();
        final Configuration hbConfig = HBaseConfiguration.create();
        //Configure hbase client. (The hbase client configuration process is equivalent to that of the org.apache.storm.hbase.bolt.HBaseBolt).
        Map<String, Object> conf = (Map<String, Object>) topologyConf.get(configKey);
        if (conf == null) {
            log.warn("HBase configuration not found using key {}", configKey);
            conf = Collections.emptyMap();
        }

        if (conf.get("hbase.rootdir") == null) {
            log.warn("No 'hbase.rootdir' value found in configuration! Using HBase defaults.");
        }
        for (Map.Entry<String, Object> entry : conf.entrySet()) {
            hbConfig.set(entry.getKey(), String.valueOf(entry.getValue()));
        }
        //heck for backward compatibility, we need to pass TOPOLOGY_AUTO_CREDENTIALS to hbase conf
        //the conf instance is instance of persistentMap so making a copy.
        Map<String, Object> hbaseConfMap = new HashMap<>(conf);
        hbaseConfMap.put(Config.TOPOLOGY_AUTO_CREDENTIALS, topologyConf.get(Config.TOPOLOGY_AUTO_CREDENTIALS));
        //Read hbase table hproject_event_rule_state_<projectId>  to retrieve initial rule state.
        try (HBaseClient hBaseClient = new HBaseClient(hbaseConfMap, hbConfig, tableName)) {
            byte[] rowKeyLowerBound = Bytes.toBytes(0L);
            byte[] rowKeyUpperBound = Bytes.toBytes(Long.MAX_VALUE);
            //LowerBound and UpperBound is set such that client retrieve all row
            byte[] columnFamily = Bytes.toBytes(HProjectHBaseConstants.RULE_COLUMN_FAMILY);
            ResultScanner scanner = hBaseClient.scan(rowKeyLowerBound, rowKeyUpperBound);
            HashMap<Long, Boolean> projectEventRuleStateMap = new HashMap<>();
            log.info("In HyperIoTHBaseRuleStateTableUtils, before serialize rule state in Java Map");
            for (Result result : scanner) {
                Map<byte[], byte[]> resultFamilyMap = result.getFamilyMap(columnFamily);
                for (Map.Entry<byte[], byte[]> entry : resultFamilyMap.entrySet()) {
                    String cellValue = Bytes.toString(entry.getValue());
                    FiredRule firedRule = mapper.readValue(cellValue, FiredRule.class);
                    log.info("In HyperIoTHBaseRuleStateTableUtils :  rule Id : {} , rule isFired :  {}", firedRule.getRuleId(), firedRule.isFired());
                    firedRules.put(firedRule.getRuleId(), firedRule);
                }
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            //If HBase is down, initialize an empty map.
            //TODO Formalize the concept HBase is down ( Identify the specific exception throws when HBaseClient can't reach HBase service)
        }
        return firedRules;
    }


}

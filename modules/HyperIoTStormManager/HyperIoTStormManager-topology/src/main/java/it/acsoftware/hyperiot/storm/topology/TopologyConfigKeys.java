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
package it.acsoftware.hyperiot.storm.topology;

import java.io.Serializable;

public class TopologyConfigKeys implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String CONFIG_PROJECT_ID = "project.id";
    public static final String CONFIG_EVENT_RULE_IDS = "event.rule.ids";
    public static final String CONFIG_ALARM_EVENT_RULE_MAP = "alarm.event.rule.map";
    public static final String CONFIG_ALARM_EVENT_RULE_IDS = "alarm.event.rule.ids";
    public static final String CONFIG_SPOUT_KAFKA_SERVERS = "spout.kafka_servers";
    public static final String CONFIG_SPOUT_KAFKA_TOPIC = "spout.kafka_topic";
    public static final String CONFIG_BOLT_KAFKA_SERVERS = "bolt.kafka_servers";
    public static final String CONFIG_BOLT_ENRICHMENT_KAFKA_TOPIC = "bolt.enrichment.kafka_topic";
    public static final String CONFIG_BOLT_ENRICHMENT_DROOLS = "bolt.enrichment.drools";
    public static final String CONFIG_BOLT_EVENT_KAFKA_TOPIC = "bolt.event.kafka_topic";
    public static final String CONFIG_BOLT_EVENT_DROOLS = "bolt.event.drools";
    public static final String CONFIG_BOLT_ALARM_EVENT_DROOLS = "bolt.alarm.event.drools";
    public static final String CONFIG_RULE_ENGINE_DROOLS = "rule-engine.drools";
}

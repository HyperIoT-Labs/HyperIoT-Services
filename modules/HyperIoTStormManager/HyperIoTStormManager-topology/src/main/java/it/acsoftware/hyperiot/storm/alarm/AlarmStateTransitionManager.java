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

import it.acsoftware.hyperiot.alarm.service.actions.AlarmAction;
import it.acsoftware.hyperiot.rule.model.facts.FiredRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Francesco Salerno
 */
public class AlarmStateTransitionManager {

    private static final Logger log = LoggerFactory.getLogger(AlarmStateTransitionManager.class);

    /**
     * Map used to keep track of alarm's rule state. (Key : alarmId, Value : a list of rule state (where rule state is a aggregator of ruleId and rule state).
     */
    private HashMap<Long, Set<AlarmRuleState>> alarmRulesStateMap;

    /**
     * Map used to keep track of alarm state in a running topology. (Key : alarmId , Value : AlarmState)
     */
    private HashMap<Long, AlarmState> alarmStateMap;

    private AlarmStateTransitionManager() {
    }

    /**
     * Constructor to initialize AlarmStateTransitionManager
     *
     * @param rulesStateMap     Map containing "last" rule's state.
     *                          (Key : ruleId, Value : a Boolean value representing whether the rule condition is met or not.
     * @param alarmEventRuleMap Map represent the relationship between alarm and his rules.
     *                          (Key : alarmId, Value : A Set of rule id related to alarm (Rule related to event related to alarm).
     */
    public AlarmStateTransitionManager(Map<Long, FiredRule> rulesStateMap, HashMap<Long, Set<Long>> alarmEventRuleMap) {
        this.alarmRulesStateMap = initializeAlarmRuleStateMap(rulesStateMap, alarmEventRuleMap);
        alarmStateMap = new HashMap<>();
        for (Long alarmId : alarmRulesStateMap.keySet()) {
            AlarmState alarmState = existEnableAlarmRule(alarmId) ? AlarmState.UP : AlarmState.DOWN;
            log.info("In AlarmStateTransitionManager, alarmId : {} , alarmState : {}", alarmId, alarmState.name());
            alarmStateMap.put(alarmId, alarmState);
        }


    }

    /**
     * @param rule        the rule that can potentially cause an alarm's state transition .
     * @param alarmAction the action related to the fired rule . (In this case this is used to retrieve the alarmId related to the rule).
     * @return true when rule's evaluation cause alarm's state transition.
     * There is an alarm state transition when :
     * 1) Rule's condition is satisfied ( FiredRule.isFired() == true)
     * and not exist another rule related to alarm such that his its condition is satisfied
     * and alarm's state is OFF
     * 2)Rule's condition is not satisfied (FiredRule.isFired() == false)
     * and not exist another rule related to alarm such that its condition is satisfied
     * and alarm's state is UP.
     */
    public synchronized boolean ruleTriggerAlarmStateTransition(FiredRule rule, AlarmAction alarmAction) {
        boolean ruleIsFired = rule.isFired();
        long ruleId = rule.getRuleId();
        long alarmId = alarmAction.getAlarmId();
        if (ruleIsFired) {
            if (!existEnableAlarmRule(alarmId) && !alarmStateIsUp(alarmId)) {
                updateAlarmRuleState(alarmId, ruleId, true);
                alarmStateMap.replace(alarmId, AlarmState.UP);
                log.info("In AlarmStateTransitionManager : rule with id {} is Active,  alarm with id : {}  go UP", ruleId, alarmId);
                return true;
            } else {
                updateAlarmRuleState(alarmId, ruleId, true);
                log.info("In AlarmStateTransitionManager : rule with id {} is Active", ruleId);
            }
        } else {
            updateAlarmRuleState(alarmId, ruleId, false);
            log.debug("In AlarmStateTransitionManager : rule with id {} is Inactive", ruleId);
            if (!existEnableAlarmRule(alarmId) && (alarmStateIsUp(alarmId))) {
                alarmStateMap.replace(alarmId, AlarmState.DOWN);
                log.info("In AlarmStateTransitionManager : alarm with id : {}  go DOWN", alarmId);
                return true;
            }
        }
        return false;
    }

    protected final boolean alarmStateIsUp(long alarmId) {
        return this.alarmStateMap.get(alarmId) == AlarmState.UP;
    }

    protected final boolean existEnableAlarmRule(long alarmId) {
        for (AlarmRuleState ruleState : this.alarmRulesStateMap.get(alarmId)) {
            if (ruleState.isActive()) {
                return true;
            }
        }
        return false;
    }

    protected final void updateAlarmRuleState(long alarmId, long ruleId, boolean active) {
        for (AlarmRuleState ruleState : alarmRulesStateMap.get(alarmId)) {
            if (ruleState.getRuleId() == ruleId) {
                ruleState.setActive(active);
            }
        }
    }

    /**
     * Utility method used for initizialition purpose. (Useful to keep track of the state of the rule related to alarm).
     *
     * @param rulesStateMap     Map containing "last" rule's state.
     *                          (Key : ruleId, Value : a Boolean value representing whether the rule condition is met or not.
     * @param alarmEventRuleMap Map represent the relationship between alarm and his rules.
     *                          (Key : alarmId, Value : A Set of rule id related to alarm (Rule related to event related to alarm).
     */
    private HashMap<Long, Set<AlarmRuleState>> initializeAlarmRuleStateMap(Map<Long, FiredRule> rulesStateMap, HashMap<Long, Set<Long>> alarmEventRuleMap) {
        HashMap<Long, Set<AlarmRuleState>> alarmRulesMap = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entryMap : alarmEventRuleMap.entrySet()) {
            long alarmId = entryMap.getKey();
            Set<AlarmRuleState> alarmRuleStates = new HashSet<>();
            for (Long ruleId : entryMap.getValue()) {
                AlarmRuleState alarmRuleState = new AlarmRuleState(ruleId);
                //If the map does not contain the rule id it means that the rule state has never been saved on hbase table hproject_rule_state_<projectId>
                boolean isActive = rulesStateMap.containsKey(ruleId) ? rulesStateMap.get(ruleId).isFired() : false;
                alarmRuleState.setActive(isActive);
                log.info("In AlarmStateTransitionManager initializeAlarmRuleStateMap, alarmId : {} , alarmRuleState : {}", alarmId, alarmRuleState);
                alarmRuleStates.add(alarmRuleState);
            }
            alarmRulesMap.put(alarmId, alarmRuleStates);
        }
        return alarmRulesMap;
    }

    /**
     * AlarmRuleState class is an aggregator of rule id and rule condition's state (active -> rule's condition is satisfied).
     * This class is used only for internal purpose.
     */
    private class AlarmRuleState {
        private long ruleId;

        /**
         * True when condition related to rule is satisfied.
         */
        private boolean isActive;

        AlarmRuleState(long ruleId) {
            this.ruleId = ruleId;
            this.isActive = false;
        }

        public long getRuleId() {
            return ruleId;
        }

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
        }

        @Override
        public String toString() {
            return "AlarmRuleState{" +
                    "ruleId=" + ruleId +
                    ", isActive=" + isActive +
                    '}';
        }
    }

}

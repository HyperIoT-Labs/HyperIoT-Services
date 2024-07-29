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

package it.acsoftware.hyperiot.rule.model;

import it.acsoftware.hyperiot.asset.tag.api.AssetTagSystemApi;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.base.api.HyperIoTAssetTagManager;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.model.operations.RootRuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class centralize the concept of transforming an HyperIoT Rule into a Drool
 */
public class RuleDroolsBuilder {

    private static Logger logger = LoggerFactory.getLogger(RuleDroolsBuilder.class);

    /**
     * Builds all project rules into drools
     *
     * @param rules
     * @param ruleType
     * @return
     */
    public static String buildHProjectDrools(Collection<Rule> rules, RuleType ruleType) {
        StringBuilder header = new StringBuilder();
        StringBuilder body = new StringBuilder();
        // creating drool header enclosing it in a string representation
        header.append("'");
        header.append("package ").append(ruleType.getDroolsPackage()).append(";\n\n")
                .append("import it.acsoftware.hyperiot.hpacket.model.HPacket;\n")
                .append("import it.acsoftware.hyperiot.rule.model.actions.RuleAction;\n")
                .append("import it.acsoftware.hyperiot.rule.model.facts.LastReceivedPacket;\n")
                .append("import it.acsoftware.hyperiot.rule.model.facts.FiredRule;\n")
                .append("import it.acsoftware.hyperiot.rule.service.actions.FourierTransformRuleAction;\n")
                .append("import java.time.Instant;\n")
                .append("import java.time.LocalDate;\n")
                .append("import java.time.LocalDateTime;\n")
                .append("import java.time.ZonedDateTime;\n")
                .append("import java.time.ZoneId;\n")
                .append("import java.time.format.DateTimeFormatter;\n")
                .append("import java.util.ArrayList;\n")
                .append("import java.util.Date;\n")
                .append("\n");
        Set<String> imports = new HashSet<>();
        for (Rule r : rules) {
            List<RuleAction> ruleActions = r.getActions();
            for (int i = 0; ruleActions != null && i < ruleActions.size(); i++) {
                RuleAction ruleAction = ruleActions.get(i);
                if (ruleAction.isActive()) {
                    String importStr = ruleAction.getClass().getName();
                    if (!imports.contains(importStr)) {
                        header.append("import ").append(importStr).append(";\n");
                        imports.add(importStr);
                    }
                    body.append(r.droolsDefinition());
                }
            }
        }
        //globals and declarations
        header.append("\nglobal java.util.ArrayList<String> actions;\n");
        header.append("\n").append("dialect  \"mvel\"\n\n");
        return header.append(body.toString()).append("'").toString();
    }

    /**
     * Build a drool from an HyperIoT Rule
     *
     * @param r
     * @return
     */
    public static String buildDroolsFromRule(Rule r) {
        StringBuilder sb = new StringBuilder();
        // get rule
        RootRuleNode rootRuleNode = r.getRule();
        boolean isEventRule = r.getType().equals(RuleType.EVENT);
        boolean isAlarmEventRule = r.getType().equals(RuleType.ALARM_EVENT);
        try {
            sb.append("\nrule \"").append(r.getName()).append("\"\n");
            sb.append("    when\n").append("        ");
            declareFacts(sb, rootRuleNode.getPacketIds());
            if (isEventRule || isAlarmEventRule)
                createEventRule(sb, r, rootRuleNode);
            else
                createEnrichmentRule(sb, r, rootRuleNode);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return sb.toString();
    }

    /**
     * @param sb
     * @param r
     */
    private static void createEnrichmentRule(StringBuilder sb, Rule r, RootRuleNode rootRuleNode) {
        createBasicDroolsActions(sb, r, rootRuleNode);
        // until roo condition is valid and it is a event roo, it must not be fired
        sb.append("end\n");
    }

    /**
     * @param sb
     * @param r
     */
    private static void createEventRule(StringBuilder sb, Rule r, RootRuleNode rootRuleNode) {
        String fireRuleName = "fireRule_" + r.getId();
        declareFireRuleVariable(sb, r.getId(), false);
        createBasicDroolsActions(sb, r, rootRuleNode);
        // until rootRuleNode condition is valid and it is a event rootRuleNode, it must not be fired
        sb.append("        ").append(fireRuleName).append(".setFired(true);\n");
        sb.append("        ").append("update(").append(fireRuleName).append(");\n");
        sb.append("        ").append(fireRuleName).append(".setLastFiredTimestamp(Date.from(ZonedDateTime.now(ZoneId.of(\"UTC\")).toInstant()));\n");
        sb.append("end\n");
        addAlreadyFiredEventRule(sb, r, rootRuleNode);

    }

    /**
     * Declares a second drools rule if it is a event rule.
     * This drools rule is fired if RuleNode condition is not valid and rule itself has already been fired
     *
     * @param sb
     * @param r
     */
    private static void addAlreadyFiredEventRule(StringBuilder sb, Rule r, RootRuleNode rootRuleNode) {
        String fireRuleName = "fireRule_" + r.getId();
        sb.append("\nrule \"").append("NOT ").append(r.getName()).append("\"\n").append("    when\n").append("        ");
        declareFacts(sb, rootRuleNode.getPacketIds());
        declareFireRuleVariable(sb, r.getId(), true);
        sb.append("not(").append(rootRuleNode.getRuleNode().droolsDefinition()).append(")\n").append("    then\n");
        for (int i = 0; r.getActions() != null && i < r.getActions().size(); i++) {
            // check for alarms
            RuleAction action = r.getActions().get(i);
            if (action.executeAsSoonAsUnmetCondition()) {
                // TODO add comment on alarm handling
                addRuleAction(sb, action, r, rootRuleNode);
            }
        }
        // rule must be fired next time, because its condition was not valid anymore
        sb.append("        ").append(fireRuleName).append(".setFired(false);\n");
        sb.append("        ").append("update(").append(fireRuleName).append(");\n");
        sb.append("end\n");
    }


    /**
     * Declare all HPacket on which the rule acts
     *
     * @param sb         StringBuilder
     * @param hPacketIds Id of hPackets
     */
    private static void declareFacts(StringBuilder sb, Set<Long> hPacketIds) {
        for (Long hPacketId : hPacketIds) {
            sb.append("packet_").append(hPacketId).append(":HPacket( id == ")
                    .append(hPacketId).append(" )\n").append("        ");
        }
    }


    /**
     * Adding local variables related to already fired rules
     *
     * @param sb
     */
    private static void declareFireRuleVariable(StringBuilder sb, long ruleId, boolean shouldBeFired) {
        sb.append("fireRule_").append(ruleId).append(":FiredRule( ruleId == ")
                .append(ruleId).append(", fired == " + ((shouldBeFired) ? "true" : "false")).append(") \n").append("       ");
    }

    /**
     * @param sb
     * @param action
     */
    private static void addRuleAction(StringBuilder sb, RuleAction action, Rule r, RootRuleNode rootRuleNode) {
        // add rule id
        action.setRuleId(r.getId());
        // add rule name to action
        action.setRuleName(r.getName());
        // add tags
        action.setTags(retrieveTags(r));
        //adding packets ids related to this action
        action.setPacketIds(rootRuleNode.getPacketIds());
        // define all rule actions
        sb.append("        ").append(action.droolsDefinition()).append(";").append("\n");
    }

    /**
     * @return
     */
    private static List<AssetTag> retrieveTags(Rule r) {
        List<AssetTag> tags = new ArrayList<>();
        AssetTagSystemApi assetTagSystemApi = (AssetTagSystemApi) HyperIoTUtil.getService(AssetTagSystemApi.class);
        HyperIoTAssetTagManager assetTagManager =
                (HyperIoTAssetTagManager) HyperIoTUtil.getService(HyperIoTAssetTagManager.class);
        long[] tagIds = assetTagManager.findAssetTags(r.getClass().getName(), r.getId());
        for (long tagId : tagIds) {
            AssetTag assetTag = assetTagSystemApi.find(tagId, null);
            tags.add(assetTag);
        }
        return tags;
    }


    /**
     * Creates the basic drools definition for an HyperIoT Rule
     *
     * @param sb
     * @param r
     */
    private static void createBasicDroolsActions(StringBuilder sb, Rule r, RootRuleNode rootRuleNode) {
        sb.append(rootRuleNode.getRuleNode().droolsDefinition()).append("\n").append("    then\n");
        for (int i = 0; r.getActions() != null && i < r.getActions().size(); i++) {
            RuleAction action = r.getActions().get(i);
            addRuleAction(sb, action, r, rootRuleNode);
        }
    }
}

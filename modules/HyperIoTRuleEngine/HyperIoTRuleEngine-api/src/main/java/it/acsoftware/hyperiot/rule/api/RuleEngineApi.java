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

package it.acsoftware.hyperiot.rule.api;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;

import java.util.Collection;
import java.util.List;

/**
 * @author Aristide Cittadino Interface component for RuleEngineApi. This interface
 * defines methods for additional operations.
 */
public interface RuleEngineApi extends HyperIoTBaseEntityApi<Rule> {
    String getDroolsForProject(HyperIoTContext context, long projectId, RuleType ruleType);
    List<RuleAction> findRuleActions(String type);
    /**
     * Finds all rules defined in a given project.
     *
     * @param context The HyperIoT context instance
     * @param packetId The packet id
     * @return List of rules
     */
    Collection<Rule> findAllRuleByPacketId(HyperIoTContext context, long packetId);
    Collection<Rule> findAllRuleByProjectId(HyperIoTContext context, long projectId);
}
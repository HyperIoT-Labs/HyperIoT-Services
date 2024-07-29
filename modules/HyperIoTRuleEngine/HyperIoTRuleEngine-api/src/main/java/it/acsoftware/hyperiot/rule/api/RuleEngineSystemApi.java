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

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;

import java.util.Collection;
import java.util.List;

/**
 * 
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface RuleEngineSystemApi extends HyperIoTBaseEntitySystemApi<Rule> {
	String getDroolsForProject(long projectId, RuleType ruleType);
	/**
	 * Finds all rules defined in a given project.
	 *
	 * @param packetId The packet id
	 * @return List of rules
	 */
	Collection<Rule> findAllRuleByPacketId(long packetId);
	Collection<Rule> findAllRuleByProjectId(long projectId);

	void removeByHPacketId(long hPacketId);

	void removeByHProjectId(long hProjectId);

	Collection<Rule> findAllRuleByProjectIdAndRuleType(long projectId , RuleType ruleType );


	Collection<Rule> findAllRuleByHPacketId(long hPacketId );

	Collection<Rule> findAllRuleByHPacketFieldId(long hPacketFieldId );
}
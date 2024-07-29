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

package it.acsoftware.hyperiot.rule.service.actions;

import it.acsoftware.hyperiot.rule.model.actions.EnrichmentRuleAction;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.DiscriminatorValue;

/**
 * @author Aristide Cittadino Rule for validating HPacket
 */
@Component(service = RuleAction.class, immediate = true, property = {"it.acsoftware.hyperiot.rule.action.type=ENRICHMENT"})
@DiscriminatorValue("rule.action.name.validate")
public class ValidateHPacketRuleAction extends EnrichmentRuleAction {
    private static Logger log = LoggerFactory.getLogger(ValidateHPacketRuleAction.class.getName());

    public ValidateHPacketRuleAction() {
        super();
    }

    @Override
    public String droolsDefinition() {
        log.debug("In ValidateHPacketRuleAction.droolsDefinition");
        return this.getDroolsPacketNameVariable() + ".setValid(true);";
    }

}

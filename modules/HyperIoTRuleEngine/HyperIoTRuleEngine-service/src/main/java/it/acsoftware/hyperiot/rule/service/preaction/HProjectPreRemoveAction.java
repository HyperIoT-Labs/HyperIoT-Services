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

package it.acsoftware.hyperiot.rule.service.preaction;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Component(service = HyperIoTPreRemoveAction.class, property = {"type=it.acsoftware.hyperiot.hproject.model.HProject"})
public class HProjectPreRemoveAction<T extends HyperIoTBaseEntity> implements HyperIoTPreRemoveAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HProjectPreRemoveAction.class.getName());
    private RuleEngineSystemApi ruleEngineSystemApi;

    @Override
    public void execute(T entity) {
        long hProjectId = entity.getId();
        LOGGER.debug("Delete rules related to HProject with id {}", hProjectId);
        Collection<Rule> projectRules = ruleEngineSystemApi.findAllRuleByProjectId(hProjectId);
        if(projectRules != null && ! projectRules.isEmpty()){
            for(Rule rule : projectRules){
                try {
                    ruleEngineSystemApi.remove(rule.getId(),null);
                } catch (Exception e){
                    LOGGER.debug("Failed to remove rule with id : {} , and type : {} ", rule.getId(), rule.getType());
                }
            }
        }
    }

    @Reference
    private void setRuleEngineSystemApi(RuleEngineSystemApi ruleEngineSystemApi) {
        this.ruleEngineSystemApi = ruleEngineSystemApi;
    }

}

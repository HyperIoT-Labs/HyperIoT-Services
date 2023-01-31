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

package it.acsoftware.hyperiot.rule.service;

import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.rule.model.facts.FiredRule;
import org.kie.api.KieBase;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.*;

public class RuleEngine {
    private Logger logger = LoggerFactory.getLogger(RuleEngine.class);

	private long projectId;
	private KieBase kbase;
	private KieSession session;

	/**
	 * Map for updating facts, when they are encountered more than once
	 */
	private Map<Long, FactHandle> facts;

	public RuleEngine(List<String> droolsRules, long projectId) {
		super();
		logger.info("Creating RuleEngine for {}",projectId);
		this.projectId = projectId;
		List<Resource> resources = new ArrayList<>();
		int i = 0;
		for (String rule : droolsRules) {
			// create one resource for each rule
			Resource ruleResource = ResourceFactory.newReaderResource(new StringReader(rule));
			ruleResource.setResourceType(ResourceType.DRL);
			ruleResource.setSourcePath(String.format("drools/%d/%d", projectId, i++));
			resources.add(ruleResource);
		}
		this.session = getKieSession(resources);
        logger.info("Current kie session instance is {} and hashcode : {} ",this.session, this.session.hashCode());
		this.session.setGlobal("actions", new ArrayList<String>());
        facts = new HashMap<>();
	}

	public void disposeSession() {
		session.dispose();
	}

	public KieSession getSession() {
		return this.session;
	}

	public void check(HPacket packet) {
	    if (facts.containsKey(packet.getId()))
			session.update(facts.get(packet.getId()), packet);
		else
			facts.put(packet.getId(), session.insert(packet));
        logger.debug("KieSession instance is: {}",session);
        logger.debug("KieSession Globals: {}",session.getGlobals());
		session.fireAllRules();
	}

	public KieSession getKieSession(List<Resource> ruleResources) {
		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		try {
			return RuleEngineKieBase.getInstance(ruleResources).getKieBase().newKieSession();
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	/**
	 * Events have to be fired if its condition is not met twice or more times sequentially. Check it via facts
	 * @param eventRuleIds Ids of rules representing events
	 */
	public void insertFiredRuleFacts(long[] eventRuleIds) {
		for (long ruleId : eventRuleIds) {
			insertFiredRuleFact(ruleId, false);
		}
	}

	public void insertFiredRuleFact(long ruleId, boolean fired){
		logger.debug("Adding FiredRule with Id : {} ", ruleId);
		session.insert(new FiredRule(ruleId, fired));
	}

}

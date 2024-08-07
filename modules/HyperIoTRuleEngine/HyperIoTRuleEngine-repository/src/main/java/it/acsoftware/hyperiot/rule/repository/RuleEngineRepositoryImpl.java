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

package it.acsoftware.hyperiot.rule.repository;

import it.acsoftware.hyperiot.rule.model.RuleType;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.rule.api.RuleEngineRepository;
import it.acsoftware.hyperiot.rule.model.Rule;

import java.util.Collection;
import java.util.HashMap;

/**
 *
 * @author Aristide Cittadino Implementation class of the RuleEngine. This class
 *         is used to interact with the persistence layer.
 *
 */
@Component(service = RuleEngineRepository.class, immediate = true)
public class RuleEngineRepositoryImpl extends HyperIoTBaseRepositoryImpl<Rule> implements RuleEngineRepository {
	/**
	 * Injecting the JpaTemplate to interact with database
	 */
	private JpaTemplate jpa;

	/**
	 * Constructor for a RuleEngineRepositoryImpl
	 */
	public RuleEngineRepositoryImpl() {
		super(Rule.class);
	}

	/**
	 *
	 * @return The current jpaTemplate
	 */
	@Override
	protected JpaTemplate getJpa() {
		getLog().debug( "invoking getJpa, returning: {}" , jpa);
		return jpa;
	}
	/**
	 * @param jpa Injection of JpaTemplate
	 */
	@Override
	@Reference(target = "(osgi.unit.name=hyperiot-ruleEngine-persistence-unit)")
	protected void setJpa(JpaTemplate jpa) {
		getLog().debug( "invoking setJpa, setting: {}" , jpa);
		this.jpa = jpa;
	}

	@Override
	public Collection<Rule> getDroolsForProject(long projectId, RuleType ruleType) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select rule from Rule rule where rule.project.id = :projectId and rule.type = :ruleType";
			return entityManager.createQuery(query).
					setParameter("projectId",projectId).
					setParameter("ruleType",ruleType).
					getResultList();
		});
	}

	@Override
	public Collection<Rule> findAllRuleByPacketId(long packetId) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			//forcing finding rule with just one packet associated, for example enrichments
			String query = "Select r from Rule r where r.packetIds LIKE :packetId ";
			return entityManager.createQuery(query).
					setParameter("packetId",String.valueOf(packetId)).
					getResultList();
		});
	}

	@Override
	public Collection<Rule> findAllRuleByProjectId(long projectId) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select r from Rule r where r.project.id=:projectId";
			return entityManager.createQuery(query).
					setParameter("projectId",projectId).
					getResultList();
		});
	}
	//   "from Rule rule where rule.project.id = :projectId and rule.type = :ruleType", params);
	@Override
	public Collection<Rule> findAllRuleByProjectIdAndRuleType(long projectId, RuleType ruleType) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select rule from Rule rule where rule.project.id = :projectId and rule.type = :ruleType";
			return entityManager.createQuery(query).
					setParameter("projectId",projectId).
					setParameter("ruleType",ruleType).
					getResultList();
		});
	}

	@Override
	public Collection<Rule> findAllRuleByHPacketId(long hPacketId) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = String.format("select r from Rule r where r.ruleDefinition like '%%%s.%%'", hPacketId);
			return entityManager.createQuery(query).getResultList();
		});
	}

	@Override
	public Collection<Rule> findAllRuleByHPacketFieldId(long hPacketFieldId) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = String.format("select r from Rule r where r.ruleDefinition like '%%.%s%%'", hPacketFieldId);
			return entityManager.createQuery(query).getResultList();
		});
	}
}

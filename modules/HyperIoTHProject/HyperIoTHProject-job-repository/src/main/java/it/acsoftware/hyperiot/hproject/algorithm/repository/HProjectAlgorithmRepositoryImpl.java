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

package it.acsoftware.hyperiot.hproject.algorithm.repository;

import org.apache.aries.jpa.template.JpaTemplate;

import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;

import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmRepository ;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;

import java.util.Collection;

/**
 *
 * @author Aristide Cittadino Implementation class of the HProjectAlgorithm. This
 *         class is used to interact with the persistence layer.
 *
 */
@Component(service=HProjectAlgorithmRepository.class,immediate=true)
public class HProjectAlgorithmRepositoryImpl extends HyperIoTBaseRepositoryImpl<HProjectAlgorithm> implements HProjectAlgorithmRepository {
	/**
	 * Injecting the JpaTemplate to interact with database
	 */
	private JpaTemplate jpa;

	/**
	 * Constructor for a HProjectAlgorithmRepositoryImpl
	 */
	public HProjectAlgorithmRepositoryImpl() {
		super(HProjectAlgorithm.class);
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
	@Reference(target = "(osgi.unit.name=hyperiot-hProject-job-persistence-unit)")
	protected void setJpa(JpaTemplate jpa) {
		getLog().debug( "invoking setJpa, setting: {}", jpa);
		this.jpa = jpa;
	}

	@Override
	public Collection<HProjectAlgorithm> findAllHProjectAlgorithmByHProjectId(long hProjectId) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select hProjectAlgorithm FROM HProjectAlgorithm hProjectAlgorithm WHERE hProjectAlgorithm.project.id=:hProjectId";
			return entityManager.createQuery(query).setParameter("hProjectId",hProjectId).getResultList();
		});
	}
}

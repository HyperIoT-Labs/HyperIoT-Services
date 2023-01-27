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

package it.acsoftware.hyperiot.alarm.event.repository;

import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import org.apache.aries.jpa.template.JpaTemplate;

import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;

import it.acsoftware.hyperiot.alarm.event.api.AlarmEventRepository;

import java.util.Collection;

/**
 * 
 * @author Aristide Cittadino Implementation class of the AlarmEvent. This
 *         class is used to interact with the persistence layer.
 *
 */
@Component(service=AlarmEventRepository.class,immediate=true)
public class AlarmEventRepositoryImpl extends HyperIoTBaseRepositoryImpl<AlarmEvent> implements AlarmEventRepository {
	/**
	 * Injecting the JpaTemplate to interact with database
	 */
	private JpaTemplate jpa;

	/**
	 * Constructor for a AlarmEventRepositoryImpl
	 */
	public AlarmEventRepositoryImpl() {
		super(AlarmEvent.class);
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
	@Reference(target = "(osgi.unit.name=hyperiot-alarm-persistence-unit)")
	protected void setJpa(JpaTemplate jpa) {
		getLog().debug("invoking setJpa, setting: {}", jpa);
		this.jpa = jpa;
	}

	@Override
	public AlarmEvent findByAlarmIdAndEventid(long alarmId, long eventId) {
		return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
			String query = "SELECT ae FROM AlarmEvent ae WHERE ae.alarm.id = :alarmId AND ae.event.id = :eventId";
			return (AlarmEvent) entityManager.createQuery(query)
					.setParameter("alarmId", alarmId)
					.setParameter("eventId", eventId)
					.getSingleResult();
		});
	}

	@Override
	public Collection<AlarmEvent> findByEventId(long eventId) {
		return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
			String query = "SELECT ae FROM AlarmEvent ae WHERE ae.event.id = :eventId";
			return entityManager.createQuery(query)
					.setParameter("eventId", eventId)
					.getResultList();
		});
	}
}

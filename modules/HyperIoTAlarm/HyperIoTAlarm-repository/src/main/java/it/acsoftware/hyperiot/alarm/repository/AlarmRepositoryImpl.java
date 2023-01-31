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

package it.acsoftware.hyperiot.alarm.repository;

import org.apache.aries.jpa.template.JpaTemplate;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;

import it.acsoftware.hyperiot.alarm.api.AlarmRepository ;
import it.acsoftware.hyperiot.alarm.model.Alarm;

/**
 * 
 * @author Aristide Cittadino Implementation class of the Alarm. This
 *         class is used to interact with the persistence layer.
 *
 */
@Component(service=AlarmRepository.class,immediate=true)
public class AlarmRepositoryImpl extends HyperIoTBaseRepositoryImpl<Alarm> implements AlarmRepository {
	/**
	 * Injecting the JpaTemplate to interact with database
	 */
	private JpaTemplate jpa;

	/**
	 * Constructor for a AlarmRepositoryImpl
	 */
	public AlarmRepositoryImpl() {
		super(Alarm.class);
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
}

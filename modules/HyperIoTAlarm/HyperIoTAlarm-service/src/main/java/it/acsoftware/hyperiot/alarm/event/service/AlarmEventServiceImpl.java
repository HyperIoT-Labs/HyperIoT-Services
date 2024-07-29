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

package it.acsoftware.hyperiot.alarm.event.service;

import it.acsoftware.hyperiot.alarm.event.api.AlarmEventApi;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventSystemApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collection;


/**
 * 
 * @author Aristide Cittadino Implementation class of AlarmEventApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = AlarmEventApi.class, immediate = true)
public final class AlarmEventServiceImpl extends HyperIoTBaseEntityServiceImpl<AlarmEvent>  implements AlarmEventApi {
	/**
	 * Injecting the AlarmEventSystemApi
	 */
	private AlarmEventSystemApi systemService;
	
	/**
	 * Constructor for a AlarmEventServiceImpl
	 */
	public AlarmEventServiceImpl() {
		super(AlarmEvent.class);
	}
	
	/**
	 * 
	 * @return The current AlarmEventSystemApi
	 */
	protected AlarmEventSystemApi getSystemService() {
		getLog().debug( "invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 * 
	 * @param alarmEventSystemService Injecting via OSGi DS current systemService 
	 */
	@Reference
	protected void setSystemService(AlarmEventSystemApi alarmEventSystemService) {
		getLog().debug( "invoking setSystemService, setting: {}" , systemService);
		this.systemService = alarmEventSystemService ;
	}

	@Override
	@AllowPermissions(actions = HyperIoTCrudAction.Names.FIND, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.alarm.api.AlarmSystemApi")
	public Collection<AlarmEvent> findAllEventByAlarmId(HyperIoTContext hyperIoTContext, long alarmId) {
		return this.systemService.findAllEventsByAlarmId(alarmId);
	}
}

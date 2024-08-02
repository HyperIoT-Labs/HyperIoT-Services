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

package it.acsoftware.hyperiot.dashboard.repository;

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.dashboard.api.DashboardRepository;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author Aristide Cittadino Implementation class of the Dashboard. This
 *         class is used to interact with the persistence layer.
 *
 */
@Component(service=DashboardRepository.class,immediate=true)
public class DashboardRepositoryImpl extends HyperIoTBaseRepositoryImpl<Dashboard> implements DashboardRepository {
	/**
	 * Injecting the JpaTemplate to interact with database
	 */
	private JpaTemplate jpa;

	/**
	 * Constructor for a DashboardRepositoryImpl
	 */
	public DashboardRepositoryImpl() {
		super(Dashboard.class);
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
	@Reference(target = "(osgi.unit.name=hyperiot-dashboard-persistence-unit)")
	protected void setJpa(JpaTemplate jpa) {
		getLog().debug( "invoking setJpa, setting: {}" , jpa);
		this.jpa = jpa;
	}

	@Override
	public void createHProjectDashboard(HProject project) {
		this.jpa.tx(TransactionType.Required, entityManager -> {
			Dashboard offlineD = new Dashboard();
			offlineD.setDashboardType(DashboardType.OFFLINE);
			offlineD.setHProject(project);
			offlineD.setDeviceId(0);
			offlineD.setName(project.getName()+" Offline Dashboard");
			Dashboard onlineD = new Dashboard();
			onlineD.setDashboardType(DashboardType.REALTIME);
			onlineD.setHProject(project);
			onlineD.setDeviceId(0);
			onlineD.setName(project.getName()+" Online Dashboard");
			entityManager.persist(offlineD);
			entityManager.persist(onlineD);
		});
	}

	@Override
	public void createAreaDashboard(Area area) {
		this.jpa.tx(TransactionType.Required, entityManager -> {
			Dashboard offlineD = new Dashboard();
			offlineD.setDashboardType(DashboardType.OFFLINE);
			offlineD.setHProject(area.getProject());
			offlineD.setDeviceId(0);
			offlineD.setArea(area);
			offlineD.setName(area.getName()+" Offline Dashboard");
			Dashboard onlineD = new Dashboard();
			onlineD.setDashboardType(DashboardType.REALTIME);
			onlineD.setHProject(area.getProject());
			onlineD.setArea(area);
			onlineD.setDeviceId(0);
			onlineD.setName(area.getName()+" Online Dashboard");
			entityManager.persist(offlineD);
			entityManager.persist(onlineD);
		});
	}

	@Override
	public void createDeviceDashboard(HDevice device) {
		this.jpa.tx(TransactionType.Required, entityManager -> {
			Dashboard offlineD = new Dashboard();
			offlineD.setDashboardType(DashboardType.OFFLINE);
			offlineD.setHProject(device.getProject());
			offlineD.setDeviceId(device.getId());
			offlineD.setArea(null);
			offlineD.setName(device.getDeviceName()+" Offline Dashboard");
			entityManager.persist(offlineD);
			Dashboard onlineD = new Dashboard();
			onlineD.setDashboardType(DashboardType.REALTIME);
			onlineD.setHProject(device.getProject());
			onlineD.setDeviceId(device.getId());
			onlineD.setArea(null);
			onlineD.setName(device.getDeviceName()+" Online Dashboard");
			entityManager.persist(onlineD);
		});
	}
}

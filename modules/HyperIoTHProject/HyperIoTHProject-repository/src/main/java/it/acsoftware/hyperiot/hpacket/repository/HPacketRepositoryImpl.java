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

package it.acsoftware.hyperiot.hpacket.repository;

import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketRepository;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Aristide Cittadino Implementation class of the HPacket. This
 *         class is used to interact with the persistence layer.
 *
 */
@Component(service=HPacketRepository.class,immediate=true)
public class HPacketRepositoryImpl extends HyperIoTBaseRepositoryImpl<HPacket> implements HPacketRepository {


	private HDeviceRepository hDeviceRepository;


	@Reference
	public void sethDeviceRepository(HDeviceRepository hDeviceRepository){
		getLog().debug( "invoking sethDeviceRepository, setting: {}" , hDeviceRepository);
		this.hDeviceRepository=hDeviceRepository;
	}

	/**
	 * Injecting the JpaTemplate to interact with database
	 */
	private JpaTemplate jpa;

	/**
	 * Constructor for a HPacketRepositoryImpl
	 */
	public HPacketRepositoryImpl() {
		super(HPacket.class);
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
	@Reference(target = "(osgi.unit.name=hyperiot-hProject-persistence-unit)")
	protected void setJpa(JpaTemplate jpa) {
		getLog().debug( "invoking setJpa, setting: {}" , jpa);
		this.jpa = jpa;
	}

	@Override
	public void remove (long id){
		this.getJpa().tx(TransactionType.Required, (entityManager) -> {
			HPacket packet = this.find(id, null);
			HDevice device = packet.getDevice();
			device.getPackets().removeIf(pack -> pack.equals(packet));
			hDeviceRepository.update(device);
			super.remove(id);
		});
	}


	@Override
	public Collection<HPacket> getProjectPacketsTree(long projectId) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select p from HPacket p inner join p.device d inner join d.project hp where hp.id=:projectId";
			return entityManager.createQuery(query).setParameter("projectId",projectId).getResultList();
		});
	}

	@Override
	public Collection<HPacket> getProjectPacketsList(long projectId) {
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select pk from HPacket pk WHERE pk.device.project.id=:projectId";
			return entityManager.createQuery(query).setParameter("projectId",projectId).getResultList();
		});
	}

	@Override
	public Collection<HPacket> getPacketsList(long deviceId) {
		try {
			hDeviceRepository.find(deviceId, null);
		} catch (NoResultException e) {
			throw new HyperIoTEntityNotFound();
		}
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select pk from HPacket pk WHERE pk.device.id=:deviceId";
			return entityManager.createQuery(query).setParameter("deviceId",deviceId).getResultList();
		});
	}

	@Override
	public Collection<HPacket> findByDeviceId(long deviceId) {
		try {
			hDeviceRepository.find(deviceId, null);
		} catch (NoResultException e) {
			throw new HyperIoTEntityNotFound();
		}
		return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
			String query = "Select hpacket from HPacket hpacket where hpacket.device.id = :deviceId";
			return entityManager.createQuery(query).setParameter("deviceId",deviceId).getResultList();
		});
	}
}

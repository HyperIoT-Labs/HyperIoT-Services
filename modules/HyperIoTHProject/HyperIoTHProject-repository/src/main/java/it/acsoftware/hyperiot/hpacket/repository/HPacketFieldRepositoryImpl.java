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

import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldRepository;
import it.acsoftware.hyperiot.hpacket.api.HPacketRepository;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.List;

/**
 * @author Aristide Cittadino Implementation class of the HPacket. This
 * class is used to interact with the persistence layer.
 */
@Component(service = HPacketFieldRepository.class, immediate = true)
public class HPacketFieldRepositoryImpl extends HyperIoTBaseRepositoryImpl<HPacketField> implements HPacketFieldRepository {


    private HPacketRepository hPacketRepository;

    @Reference
    public void sethPacketRepository(HPacketRepository hPacketRepository){
        getLog().debug( "invoking sethDeviceRepository, setting: {}" , hPacketRepository);
        this.hPacketRepository=hPacketRepository;
    }


    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Constructor for a HPacketRepositoryImpl
     */
    public HPacketFieldRepositoryImpl() {
        super(HPacketField.class);
    }

    /**
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
    public List<HPacketField> getHPacketRootField(long packetId) {
        return jpa.txExpr( entityManager -> entityManager.createNativeQuery
                ("SELECT * FROM hpacketfield hpf \n" +
                        "WHERE hpf.packet_id = :packetId and hpf.parentfield_id is NULL \n"
                        ,HPacketField.class).
                setParameter("packetId",packetId).
                getResultList());
    }

    @Override
    public Collection<HPacketField> getHPacketFieldsTree(long packetId) {
        return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
            String query = "Select pf from HPacketField pf where pf.parentField is null and pf.packet.id = :packetId";
            return entityManager.createQuery(query).setParameter("packetId",packetId).getResultList();
        });
    }

    @Override
    public void remove(long id){
        this.getJpa().tx(TransactionType.Required, (entityManager) -> {
            /*
            We need to invoke invokePreAction, invokePostAction and manageAssets in this
            method because we can't call remove method of parent's class.
            On the relationship between HPacket and HPacketField there is orphan removal attribute.
            If we delete packetfield's reference in packet entity the remove operation is implicity
            processed by hibernate so if we call parent's remove method system tell us that the entity
            doesn't exists. So to be coherent with parent's remove operation we call the above mentioned
            method in this class.
             */
            HPacketField packetField = this.find(id, null);
            HyperIoTUtil.invokePreActions(packetField, HyperIoTPreRemoveAction.class);
            HPacket packet = packetField.getPacket();
            packet.getFields().removeIf(field -> field.equals(packetField));
            hPacketRepository.update(packet);
            //Make this enum protected in the framework module AssetManagementOperation
            this.manageAssets(packetField, HyperIoTBaseRepositoryImpl.AssetManagementOperation.DELETE);
            HyperIoTUtil.invokePostActions(packetField, HyperIoTPostRemoveAction.class);
        });
    }
}

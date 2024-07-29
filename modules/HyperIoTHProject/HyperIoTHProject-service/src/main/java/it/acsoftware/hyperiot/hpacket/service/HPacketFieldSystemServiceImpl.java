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

package it.acsoftware.hyperiot.hpacket.service;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldRepository;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldSystemApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketRepository;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Aristide Cittadino Implementation class of the HPacketSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = HPacketFieldSystemApi.class, immediate = true)
public final class HPacketFieldSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<HPacketField>
    implements HPacketFieldSystemApi {

    /**
     * Injecting the HPacketRepository to interact with persistence layer
     */
    private HPacketFieldRepository repository;

    /**
     * Injecting HPacket repository
     */
    private HPacketRepository hPacketRepository;


    /**
     * Constructor for a HPacketSystemServiceImpl
     */
    public HPacketFieldSystemServiceImpl() {
        super(HPacketField.class);
    }

    /**
     * Return the current repository
     */
    protected HPacketFieldRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param hPacketRepository The current value of HPacketRepository to interact
     *                          with persistence layer
     */
    @Reference
    protected void setRepository(HPacketFieldRepository hPacketRepository) {
        getLog().debug("invoking setRepository, setting: {}", hPacketRepository);
        this.repository = hPacketRepository;
    }

    /**
     * @param hPacketRepository
     */
    @Reference
    public void sethPacketRepository(HPacketRepository hPacketRepository) {
        this.hPacketRepository = hPacketRepository;
    }

    @Override
    public Collection<HPacketField> getHPacketFieldsTree(long packetId) {

        try {
            hPacketRepository.find(packetId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        return repository.getHPacketFieldsTree(packetId);
    }

    @Override
    public List<HPacketField> getHPacketRootField(long packetId) {
        return repository.getHPacketRootField(packetId);
    }

}

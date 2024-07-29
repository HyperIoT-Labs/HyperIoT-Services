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

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.hdevice.api.HDeviceRepository;
import it.acsoftware.hyperiot.hpacket.actions.HyperIoTHPacketAction;
import it.acsoftware.hyperiot.hpacket.api.HPacketRepository;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketType;
import it.acsoftware.hyperiot.hproject.api.HProjectRepository;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.osgi.service.component.annotations.Activate;
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
@Component(service = HPacketSystemApi.class, immediate = true)
public final class HPacketSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<HPacket>
    implements HPacketSystemApi {

    /**
     * Injecting the HPacketRepository to interact with persistence layer
     */
    private HPacketRepository repository;

    private HDeviceRepository hDeviceRepository;

    private HProjectRepository hProjectRepository;

    /**
     * Injecting the PermissionSystemApi to interact with persistence layer
     */
    private PermissionSystemApi permissionSystemApi;

    /**
     * Constructor for a HPacketSystemServiceImpl
     */
    public HPacketSystemServiceImpl() {
        super(HPacket.class);
    }

    /**
     * Return the current repository
     */
    protected HPacketRepository getRepository() {
        getLog().debug( "invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param hPacketRepository The current value of HPacketRepository to interact
     *                          with persistence layer
     */
    @Reference
    protected void setRepository(HPacketRepository hPacketRepository) {
        getLog().debug( "invoking setRepository, setting: {}", hPacketRepository);
        this.repository = hPacketRepository;
    }

    @Reference
    protected void setHDeviceRepository(HDeviceRepository hDeviceRepository) {
        this.hDeviceRepository = hDeviceRepository;
    }

    @Reference
    protected void setHProjectRepository(HProjectRepository hProjectRepository) {
        this.hProjectRepository = hProjectRepository;
    }

    /**
     * @param permissionSystemApi Injecting via OSGi DS current PermissionSystemApi
     */
    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    /**
     * @return list of packet belongs to a specific device
     */
    @Override
    public Collection<HPacket> findByDeviceId(long deviceId) {
        return repository.findByDeviceId(deviceId);
    }

    /**
     * On Bundle activated
     */
    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExists();
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        String resourceName = HPacket.class.getName();
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTHPacketAction.FIELDS_MANAGEMENT));
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    /**
     * @param deviceId
     * @return
     */
    @SuppressWarnings("serial")
    @Override
    public Collection<HPacket> getPacketsList(long deviceId) {
        return repository.getPacketsList(deviceId);
    }


    @Override
    public Collection<HPacket> getProjectPacketsTree(long projectId) {
        try {
            hProjectRepository.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        return repository.getProjectPacketsTree(projectId);
    }


}

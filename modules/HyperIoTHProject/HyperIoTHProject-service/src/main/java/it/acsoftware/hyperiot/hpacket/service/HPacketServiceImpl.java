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
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.api.HyperIoTPermissionManager;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.annotations.AllowGenericPermissions;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTOwnedChildBaseEntityServiceImpl;
import it.acsoftware.hyperiot.hdevice.actions.HyperIoTHDeviceAction;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hpacket.api.*;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hpacket.model.HPacketType;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import javax.ws.rs.core.StreamingOutput;
import java.util.Collection;
import java.util.List;

/**
 * @author Aristide Cittadino Implementation class of HPacketApi interface.
 * It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = HPacketApi.class, immediate = true)
public final class HPacketServiceImpl extends HyperIoTOwnedChildBaseEntityServiceImpl<HPacket> implements HPacketApi, HyperIoTOwnershipResourceService {
    /**
     * Injecting the HPacketSystemApi
     */
    private HPacketSystemApi systemService;

    /**
     * Injecting the HPacketFieldSystemApi
     */
    private HPacketFieldSystemApi hpacketFieldSystemService;

    /**
     * Injecting HDeviceSystemApi
     */
    private HDeviceSystemApi hdeviceSystemApi;

    /**
     * Injecting the HProjectSystemApi
     */
    private HProjectSystemApi hprojectService;


    /**
     * Constructor for a HPacketServiceImpl
     */
    public HPacketServiceImpl() {
        super(HPacket.class);
    }

    /**
     * @return The current HPacketSystemApi
     */
    protected HPacketSystemApi getSystemService() {
        getLog().debug("invoking getSystemService, returning: {}", this.systemService);
        return systemService;
    }

    /**
     * @param hPacketSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(HPacketSystemApi hPacketSystemService) {
        getLog().debug("invoking setSystemService, setting: {}", systemService);
        this.systemService = hPacketSystemService;
    }

    /**
     * @param hProjectSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setHProjectSystemService(HProjectSystemApi hProjectSystemService) {
        this.hprojectService = hProjectSystemService;
    }

    /**
     * @param hpacketFieldSystemService
     */
    @Reference
    public void setHpacketFieldSystemService(HPacketFieldSystemApi hpacketFieldSystemService) {
        this.hpacketFieldSystemService = hpacketFieldSystemService;
    }

    /**
     * @return
     */
    public HDeviceSystemApi getHdeviceSystemApi() {
        return hdeviceSystemApi;
    }

    /**
     * @param hdeviceSystemApi
     */
    @Reference
    public void setHdeviceSystemApi(HDeviceSystemApi hdeviceSystemApi) {
        this.hdeviceSystemApi = hdeviceSystemApi;
    }

    /**
     * @param context  The HyperIoTContext instance
     * @param deviceId The device id
     * @return
     */
    @Override
    @AllowPermissions(actions = HyperIoTHDeviceAction.Names.PACKETS_MANAGEMENT, checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi")
    public Collection<HPacket> getPacketsList(HyperIoTContext context, long deviceId) {
        getLog().debug("invoking getPacketsList, on device: {}", deviceId);
        return systemService.getPacketsList(deviceId);
    }

    @Override
    @AllowGenericPermissions(actions = HyperIoTHDeviceAction.Names.PACKETS_MANAGEMENT, resourceName = "it.acsoftware.hyperiot.hdevice.model.HDevice")
    public Collection<HPacket> getProjectPacketsList(HyperIoTContext context, long projectId) {
        getLog().debug("invoking getProjectPacketsList, on project: {}", projectId);
        try {
            hprojectService.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        HyperIoTQuery byProjectIdFilter = HyperIoTQueryBuilder.newQuery().equals("device.project.id", projectId);
        return super.findAll(byProjectIdFilter, context);
    }

    @Override
    @AllowGenericPermissions(actions = HyperIoTHDeviceAction.Names.PACKETS_MANAGEMENT, resourceName = "it.acsoftware.hyperiot.hdevice.model.HDevice")
    public Collection<HPacket> getProjectPacketsListByType(HyperIoTContext context, long projectId, List<HPacketType> types) {
        getLog().debug("invoking getProjectPacketsList, on project: {}", projectId);
        try {
            hprojectService.find(projectId, null);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        HyperIoTQuery filter = HyperIoTQueryBuilder.newQuery().equals("device.project.id", projectId);
        if (types != null && types.size() > 0) {
            HyperIoTQuery orTypesFilter = null;
            for (HPacketType type : types) {
                HyperIoTQuery typesFilterCondition = HyperIoTQueryBuilder.newQuery().equals("type", type);
                if (orTypesFilter == null)
                    orTypesFilter = typesFilterCondition;
                else
                    orTypesFilter = orTypesFilter.or(typesFilterCondition);
            }
            filter = filter.and(orTypesFilter);
        }
        return super.findAll(filter, context);
    }

    @Override
    public Collection<HPacket> getProjectPacketsTree(HyperIoTContext context, long projectId) {
        getLog().debug("invoking getProjectPacketsTree, on project: {}", projectId);
        HProject project = hprojectService.find(projectId, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        if (HyperIoTSecurityUtil.checkPermission(context, project,
                HyperIoTActionsUtil.getHyperIoTAction(HPacket.class.getName(), HyperIoTCrudAction.FINDALL))) {
            return this.systemService.getProjectPacketsTree(projectId);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param context
     * @param field
     */
    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.UPDATE)
    public HPacketField updateHPacketField(HyperIoTContext context, HPacketField field) {
        return this.hpacketFieldSystemService.update(field, context);
    }

    /**
     * @param context
     * @param field
     */
    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.SAVE)
    public HPacketField addHPacketField(HyperIoTContext context, HPacketField field) {
        return this.hpacketFieldSystemService.save(field, context);
    }


    /**
     * o     * @return
     */
    @Override
    public String getOwnerFieldPath() {
        return "device.project.user.id";
    }

    /**
     * @param packetId
     * @return
     */
    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.FINDALL, checkById = true, idParamIndex = 1)
    public Collection<HPacketField> getHPacketFieldsTree(HyperIoTContext context, long packetId) {
        getLog().debug("invoking getHPacketFieldsTree, on packet: {}", packetId);
        return this.hpacketFieldSystemService.getHPacketFieldsTree(packetId);
    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.REMOVE, checkById = true, idParamIndex = 2, systemApiRef = "it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi")
    public void removeHPacketField(HyperIoTContext context, long fieldId, long packetId) {
        getLog().debug("invoking removeHPacketField, on field: {}", fieldId);
        //removing field from its hierarchy
        HPacket packet = this.systemService.find(packetId, context);
        HPacketField field = this.hpacketFieldSystemService.find(fieldId, context);
        packet.removeField(field);
        this.systemService.update(packet, context);
    }

    @Override
    @AllowGenericPermissions(actions = HyperIoTCrudAction.Names.FIND)
    public HPacket findHPacketByHpacketFieldId(HyperIoTContext context, long hpacketFieldId) {
        try {
            HPacketField packetField = hpacketFieldSystemService.find(hpacketFieldId, context);
            return packetField.getPacket();
        } catch (NoResultException exception) {
            throw new HyperIoTEntityNotFound();
        }
    }

    @Override
    protected String getRootParentFieldPath() {
        return "device.project.id";
    }

    @Override
    protected Class<? extends HyperIoTOwnedResource> getParentResourceClass() {
        return HProject.class;
    }

}

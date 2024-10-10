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

package it.acsoftware.hyperiot.area.service;

import it.acsoftware.hyperiot.area.actions.HyperIoTAreaAction;
import it.acsoftware.hyperiot.area.api.AreaApi;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTOwnedChildBaseEntityServiceImpl;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;

/**
 * @author Aristide Cittadino Implementation class of AreaApi interface.
 * It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = AreaApi.class, immediate = true)
public final class AreaServiceImpl extends HyperIoTOwnedChildBaseEntityServiceImpl<Area> implements AreaApi, HyperIoTOwnershipResourceService {
    private static final String RESOURCE_NAME = Area.class.getName();
    /**
     * Injecting the AreaSystemApi
     */
    private AreaSystemApi systemService;

    /**
     * Constructor for a AreaServiceImpl
     */
    public AreaServiceImpl() {
        super(Area.class);
    }

    /**
     * @return The current AreaSystemApi
     */
    protected AreaSystemApi getSystemService() {
        getLog().debug("invoking getSystemService, returning: {}", this.systemService);
        return systemService;
    }

    /**
     * @param areaSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(AreaSystemApi areaSystemService) {
        getLog().debug("invoking setSystemService, setting: {}", systemService);
        this.systemService = areaSystemService;
    }

    /**
     * Get nested area tree
     */
    @Override
    public Area getAll(HyperIoTContext context, long areaId) {
        getLog().debug("invoking getAll, on area: {}", areaId);
        Area a;
        try {
            a = systemService.find(areaId, context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (HyperIoTSecurityUtil.checkPermissionAndOwnership(context, a,
                HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER), a)) {
            this.systemService.getAll(a);
            return a;
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Remove area along with its nested areas.
     */
    @Override
    public void removeAll(HyperIoTContext context, long areaId) {
        Area a;
        try {
            a = systemService.find(areaId, context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (HyperIoTSecurityUtil.checkPermissionAndOwnership(context, a, HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER))) {
            this.systemService.removeAll(a);
            return;
        }
        throw new HyperIoTUnauthorizedException();
    }

    @Override
    @AllowPermissions(
            actions = {"update"}
    )
    public Area updateAndPreserveImageData(Area entity, HyperIoTContext ctx) {
        return this.getSystemService().updateAndPreserveImageData(entity);
    }

    @Override
    public Collection<Area> getAreaPath(HyperIoTContext context, long areaId) {
        getLog().debug("invoking getAreaPath, on area: {}", areaId);
        Area a;
        try {
            a = systemService.find(areaId, context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (HyperIoTSecurityUtil.checkPermissionAndOwnership(context, a,
                HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER), a)) {
            return this.systemService.getAreaPath(a);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Add a device inside an area
     */
    @Override
    public void saveAreaDevice(HyperIoTContext context, long areaId, AreaDevice areaDevice) {
        Area a;
        try {
            a = systemService.find(areaId, context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (HyperIoTSecurityUtil
                .checkPermissionAndOwnership(context, a, HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER), a)) {
            HDevice device = areaDevice.getDevice();
            if (device == null)
                throw new HyperIoTEntityNotFound();

            if (HyperIoTSecurityUtil
                    .checkPermissionAndOwnership(context, a, HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER), areaDevice.getDevice())) {
                //checking User owns resources
                systemService.saveAreaDevice(a, areaDevice);
                return;
            }
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Gets an area device
     *
     * @param context      The HyperIoTContext instance
     * @param areaDeviceId The area device id
     * @return The AreaDevice
     */
    public AreaDevice getAreaDevice(HyperIoTContext context, long areaDeviceId) {
        getLog().debug("invoking getAreaDevice, on areaDevice: {}", areaDeviceId);
        AreaDevice ad;
        Area a;
        try {
            ad = systemService.getAreaDevice(areaDeviceId);
            a = systemService.find(ad.getArea().getId(), context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (HyperIoTSecurityUtil.checkPermissionAndOwnership(context, a,
                HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER), ad.getArea())) {
            return ad;
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Remove device from an area
     */
    @Override
    @AllowPermissions(actions = HyperIoTAreaAction.Names.AREA_DEVICE_MANAGER, checkById = true, idParamIndex = 1)
    public void removeAreaDevice(HyperIoTContext context, long areaId, long areaDeviceId) {
        getLog().debug("invoking removeAreaDevice, on areaDevice: {}", areaDeviceId);
        AreaDevice ad;
        try {
            ad = systemService.getAreaDevice(areaDeviceId);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        Area a = systemService.find(ad.getArea().getId(), context);
        if (a == null || a.getId() != areaId) {
            throw new HyperIoTEntityNotFound();
        }
        systemService.removeAreaDevice(a, ad.getId());
    }

    /**
     * Retrieve device list inside the area
     */
    @Override
    public Collection<AreaDevice> getAreaDevicesList(HyperIoTContext context, long areaId) {
        getLog().debug("invoking getAreaDevicesList, on area: {}", areaId);
        Area a;
        try {
            a = systemService.find(areaId, context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (HyperIoTSecurityUtil.checkPermissionAndOwnership(context, a, HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER))) {
            return systemService.getAreaDevicesList(a);
        }
        throw new HyperIoTUnauthorizedException();
    }

    @Override
    public Collection<AreaDevice> getAreaDevicesDeepList(HyperIoTContext context, long areaId, boolean seekRoot) {
        Area rootArea = systemService.find(areaId, context);
        if (rootArea == null) {
            throw new HyperIoTEntityNotFound();
        }
        while (seekRoot && rootArea.getParentArea() != null) {
            rootArea = systemService.find(rootArea.getParentArea().getId(), context);
        }
        if (HyperIoTSecurityUtil.checkPermissionAndOwnership(context, rootArea, HyperIoTActionsUtil.getHyperIoTAction(RESOURCE_NAME, HyperIoTAreaAction.AREA_DEVICE_MANAGER))) {
            return systemService.getAreaDevicesDeepList(rootArea);
        }
        throw new HyperIoTUnauthorizedException();
    }

    @Override
    @AllowPermissions(actions = HyperIoTAreaAction.Names.AREA_DEVICE_MANAGER, checkById = true, idParamIndex = 1)
    public void resetAreaType(HyperIoTContext context,long areaId) {
        this.systemService.resetAreaType(areaId);
    }

    @Override
    public String getOwnerFieldPath() {
        return "project.user.id";
    }


    @Override
    protected String getRootParentFieldPath() {
        return "project.id";
    }

    @Override
    protected Class<? extends HyperIoTOwnedResource> getParentResourceClass() {
        return HProject.class;
    }
}

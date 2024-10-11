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
import it.acsoftware.hyperiot.area.api.AreaDeviceRepository;
import it.acsoftware.hyperiot.area.api.AreaRepository;
import it.acsoftware.hyperiot.area.api.AreaSystemApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.model.AreaViewType;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.File;
import java.util.*;

/**
 * @author Aristide Cittadino Implementation class of the AreaSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = AreaSystemApi.class, immediate = true)
public final class AreaSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Area> implements AreaSystemApi {

    /**
     * Injecting the HDeviceSystemApi
     */
    private HDeviceSystemApi deviceSystemService;
    /**
     * Injecting the AreaRepository to interact with persistence layer
     */
    private AreaRepository repository;
    /**
     * Injecting the AreaRepository to interact with persistence layer
     */
    private AreaDeviceRepository areaDeviceRepository;
    /**
     * Injecting the PermissionSystemApi to interact with persistence layer
     */
    private PermissionSystemApi permissionSystemApi;

    /**
     * Constructor for a AreaSystemServiceImpl
     */
    public AreaSystemServiceImpl() {
        super(Area.class);
    }

    /**
     * Return the current repository
     */
    protected AreaRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param areaRepository The current value of AreaRepository to interact with
     *                       persistence layer
     */
    @Reference
    protected void setRepository(AreaRepository areaRepository) {
        getLog().debug("invoking setRepository, setting: {}", areaRepository);
        this.repository = areaRepository;
    }

    /**
     * @param deviceSystemService Injecting via OSGi DS current HDeviceSystemService
     */
    @Reference
    protected void setDeviceSystemService(HDeviceSystemApi deviceSystemService) {
        getLog().debug("invoking setDeviceSystemService, setting: {}", deviceSystemService);
        this.deviceSystemService = deviceSystemService;
    }

    /**
     * @param permissionSystemApi Injecting via OSGi DS current PermissionSystemApi
     */
    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    @Reference
    protected void setAreaDeviceRepository(AreaDeviceRepository areaDeviceRepository) {
        getLog().debug("invoking setDeviceRepository, setting: {}", areaDeviceRepository);
        this.areaDeviceRepository = areaDeviceRepository;
    }

    /**
     * On Bundle activated
     */
    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExists();
    }

    @Override
    public Area save(Area entity, HyperIoTContext ctx) {
        return super.save(entity, ctx);
    }

    @Override
    public Area update(Area entity, HyperIoTContext ctx) {
        return super.update(entity, ctx);
    }

    public Area updateAndPreserveImageData(Area entity) {
        //forcing previous set image since update and save method would override the image path with null
        Area areaFromDb = find(entity.getId(), null);
        if (areaFromDb.getImagePath() != null && !areaFromDb.getImagePath().isBlank())
            entity.setImagePath(areaFromDb.getImagePath());
        return super.update(entity, null);
    }

    @Override
    public void getAll(Area area) {
        Collection<Area> innerAreas = repository.getInnerArea(area.getId());
        area.setInnerArea(new HashSet<>(innerAreas));
        innerAreas.forEach(this::getAll);
    }

    @Override
    public void removeAll(Area area) {
        HyperIoTQuery filter = HyperIoTQueryBuilder.newQuery().equals("parentArea.id", area.getId());
        Collection<Area> children = this.repository.findAll(filter);
        children.forEach(this::removeAll);
        this.repository.remove(area.getId());
    }

    @Override
    public void removeByHProjectId(long hProjectId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("hProjectId", hProjectId);
        repository.executeUpdateQuery("delete from Area dashboard where area.project.id = :hProjectId", params);
    }

    @Override
    public Collection<Area> getAreaPath(Area area) {
        ArrayList<Area> areaList = new ArrayList<>();
        do {
            areaList.add(0, area);
            area = area.getParentArea();
        } while (area != null);
        return areaList;
    }

    @Override
    public AreaDevice getAreaDevice(long areaDeviceId) {
        return areaDeviceRepository.find(areaDeviceId, null);
    }

    /**
     * Add device inside an area
     */
    @Override
    public void saveAreaDevice(Area area, AreaDevice areaDevice) {
        // check if HDevice exists
        HDevice device;
        try {
            device = deviceSystemService.find(areaDevice.getDevice().getId(), null);
        } catch (NullPointerException e) {
            throw new HyperIoTEntityNotFound();
        }
        String[] duplicateEntityMessage = {"Device already mapped"};
        HyperIoTQuery areaFilter = HyperIoTQueryBuilder.newQuery().equals("area.id", area.getId());
        HyperIoTQuery deviceFilter = HyperIoTQueryBuilder.newQuery().equals("device.id", device.getId());
        if (areaDevice.getId() <= 0) {
            AreaDevice ad = areaDeviceRepository.findAll(areaFilter.and(deviceFilter)).stream().findFirst().orElse(null);
            if (ad != null) {
                throw new HyperIoTDuplicateEntityException(duplicateEntityMessage);
            }
            areaDevice.setArea(area);
            areaDeviceRepository.save(areaDevice);
        } else {
            // UPDATING
            Area rootArea = area;
            while (rootArea.getParentArea() != null) {
                rootArea = this.find(rootArea.getParentArea().getId(), null);
            }
            AreaDevice ad = areaDeviceRepository.findAll(areaFilter.and(deviceFilter)).stream().findFirst().orElse(null);
            if (ad != null) {
                throw new HyperIoTDuplicateEntityException(duplicateEntityMessage);
            }
            areaDevice.setArea(area);
            areaDeviceRepository.update(areaDevice);
        }
    }

    /**
     * Remove device from an area
     */
    @Override
    public void removeAreaDevice(Area area, long areaDeviceId) {
        areaDeviceRepository.remove(areaDeviceId);
    }

    /**
     * Retrieve device from area
     */
    @Override
    public Collection<AreaDevice> getAreaDevicesList(Area area) {
        if (area == null) {
            throw new HyperIoTEntityNotFound();
        }
        HyperIoTQuery areaFilter = HyperIoTQueryBuilder.newQuery().equals("area.id", area.getId());
        return areaDeviceRepository.findAll(areaFilter);
    }

    @Override
    public Collection<AreaDevice> getAreaDevicesDeepList(Area area) {
        return this.repository.getAreaDevicesDeepList(area.getId());
    }

    @Override
    public List<Area> getRootProjectArea(long projectId) {
        return repository.getRootProjectArea(projectId);
    }

    @Override
    public Collection<Area> getAreaListByProjectId(long projectId) {
        return repository.getAreaListByProjectId(projectId);
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        String resourceName = Area.class.getName();
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTAreaAction.AREA_DEVICE_MANAGER));
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    @Override
    public void resetAreaType(long areaId, AreaViewType newViewType) {
        Area a = repository.load(areaId);
        a.setAreaConfiguration("");
        //resetting all area devices, dropping cascade
        a.getAreaDevices().clear();
        a.setAreaViewType(newViewType);
        a.getInnerArea().forEach(innerArea -> innerArea.setMapInfo(null));
        if(a.getImagePath() != null && !a.getImagePath().isBlank()) {
            String imagePath = a.getImagePath();
            File f = new File(imagePath);
            if(f.exists())
                f.delete();
            a.setImagePath("");
        }
        this.update(a,null);
    }
}

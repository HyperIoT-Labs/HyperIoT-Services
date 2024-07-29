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

package it.acsoftware.hyperiot.kit.category.service;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.hproject.api.HProjectApi;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeApi;
import it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeSystemApi;
import it.acsoftware.hyperiot.kit.category.model.DiagnoseType;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.kit.service.KitPermissionUtils;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.Collection;

@Component(service = DiagnoseTypeApi.class, immediate = true)
public class DiagnoseTypeServiceImpl extends HyperIoTBaseEntityServiceImpl<DiagnoseType> implements DiagnoseTypeApi, HyperIoTOwnershipResourceService {

    private DiagnoseTypeSystemApi systemService;

    private KitSystemApi kitSystemApi;

    private HProjectApi hProjectApi;

    private SharedEntitySystemApi sharedEntitySystemApi;

    public DiagnoseTypeServiceImpl() {
        super(DiagnoseType.class);
    }

    @Override
    protected DiagnoseTypeSystemApi getSystemService() {
        getLog().debug("invoking getSystemService, returning: {}" , this.systemService);
        return systemService;
    }

    @Reference
    protected void setSystemService(DiagnoseTypeSystemApi systemService){
        getLog().debug("invoking setSystemService, setting: {}" , systemService);
        this.systemService = systemService;
    }

    protected KitSystemApi getKitSystemApi() {
        getLog().debug("invoking getKitSystemApi, returning: {}" , this.kitSystemApi);
        return kitSystemApi;
    }

    @Reference
    protected void setKitSystemApi(KitSystemApi kitSystemApi){
        getLog().debug("invoking setKitSystemApi, setting: {}" , kitSystemApi);
        this.kitSystemApi = kitSystemApi;
    }

    protected HProjectApi gethProjectApi() {
        getLog().debug("invoking gethProjectApi, returning: {}" , this.hProjectApi);
        return hProjectApi;
    }

    @Reference
    protected void sethProjectApi(HProjectApi hProjectApi){
        getLog().debug("invoking sethProjectApi, setting: {}" , hProjectApi);
        this.hProjectApi = hProjectApi;
    }

    protected SharedEntitySystemApi getSharedEntitySystemApi() {
        getLog().debug("invoking getSharedEntitySystemApi, returning: {}" , this.sharedEntitySystemApi);
        return sharedEntitySystemApi;
    }

    @Reference
    protected void setSharedEntitySystemApi(SharedEntitySystemApi sharedEntitySystemApi){
        getLog().debug("invoking setSharedEntitySystemApi, setting: {}" , sharedEntitySystemApi);
        this.sharedEntitySystemApi = sharedEntitySystemApi;
    }

    @Override
    @AllowPermissions(actions = "find", checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeSystemApi")
    public DiagnoseType addKitToDiagnoseTypeCategory(HyperIoTContext ctx, long diagnoseTypeId, long kitId) {
        KitPermissionUtils.checkUserHasPermissionToHandleKitCategory(kitSystemApi,hProjectApi,kitId,ctx);
        return systemService.addKitToDiagnoseTypeCategory(ctx,diagnoseTypeId,kitId);
    }

    @Override
    @AllowPermissions(actions = "find", checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeSystemApi")
    public void removeKitFromDiagnoseTypeCategory(HyperIoTContext ctx, long diagnoseTypeId, long kitId) {
        KitPermissionUtils.checkUserHasPermissionToHandleKitCategory(kitSystemApi,hProjectApi,kitId,ctx);
        systemService.removeKitFromDiagnoseTypeCategory(ctx,diagnoseTypeId,kitId);
    }

    @Override
    @AllowPermissions(actions = "find", checkById = true, idParamIndex = 1, systemApiRef = "it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeSystemApi")
    public Collection<Kit> getKitByDiagnoseTypeCategory(HyperIoTContext ctx, long diagnoseTypeId) {
        return systemService.getKitByDiagnoseTypeCategory(ctx,diagnoseTypeId);
    }

    @Override
    public Collection<DiagnoseType> getDiagnoseTypeByKit(HyperIoTContext ctx, long kitId) {
        //Calling this find method of KitSystemApi, we implicitily check if the user has permission to find the  kit.
        this.kitSystemApi.find(kitId,ctx);
        return systemService.getDiagnoseTypeByKit(ctx,kitId);
    }

    @Override
    public String getOwnerFieldPath() {
        return "user.id";
    }
}

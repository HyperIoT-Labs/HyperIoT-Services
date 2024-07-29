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

import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.asset.category.model.AssetCategoryResource;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.model.HyperIoTAssetOwnerImpl;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.hproject.api.HProjectApi;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeRepository;
import it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeSystemApi;
import it.acsoftware.hyperiot.kit.category.model.DiagnoseType;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.kit.service.KitPermissionUtils;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.*;
import java.util.stream.Collectors;

@Component(service = DiagnoseTypeSystemApi.class, immediate = true)
public class DiagnoseTypeSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<DiagnoseType> implements DiagnoseTypeSystemApi {

    private DiagnoseTypeRepository repository;

    private KitSystemApi kitSystemApi ;

    private PermissionSystemApi permissionSystemApi;

    private HUserSystemApi hUserSystemApi;

    private HProjectApi hProjectApi;

    public DiagnoseTypeSystemServiceImpl() {
        super(DiagnoseType.class);
    }

    protected DiagnoseTypeRepository getRepository() {
        getLog().debug("invoking getRepository, returning: {}" , this.repository);
        return repository;
    }

    @Reference
    protected void setRepository(DiagnoseTypeRepository repository) {
        getLog().debug("invoking setRepository, setting: {}" , repository);
        this.repository = repository;
    }

    protected KitSystemApi getKitSystemApi() {
        getLog().debug("invoking getKitSystemApi, returning: {}" , this.kitSystemApi);
        return kitSystemApi;
    }

    @Reference
    protected void setKitSystemApi(KitSystemApi kitSystemApi) {
        getLog().debug("invoking setKitSystemApi, setting: {}" , kitSystemApi);
        this.kitSystemApi = kitSystemApi;
    }

    @Reference
    protected void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        getLog().debug("invoking setPermissionSystemApi, setting: {}" , permissionSystemApi);
        this.permissionSystemApi = permissionSystemApi;
    }

    protected PermissionSystemApi getPermissionSystemApi() {
        getLog().debug("invoking getPermissionSystemApi, returning: {}" , this.permissionSystemApi);
        return permissionSystemApi;
    }



    @Reference
    protected void sethUserSystemApi(HUserSystemApi hUserSystemApi) {
        getLog().debug("invoking sethUserSystemApi, setting: {}" , hUserSystemApi);
        this.hUserSystemApi = hUserSystemApi;
    }

    protected HUserSystemApi gethUserSystemApi() {
        getLog().debug("invoking gethUserSystemApi, returning: {}" , this.hUserSystemApi);
        return hUserSystemApi;
    }

    @Reference
    protected void sethProjectApi(HProjectApi hProjectApi) {
        getLog().debug("invoking sethProjectApi, setting: {}" , hProjectApi);
        this.hProjectApi = hProjectApi;
    }

    protected HProjectApi gethProjectApi() {
        getLog().debug("invoking gethProjectApi, returning: {}" , this.hProjectApi);
        return hProjectApi;
    }

    @Override
    public DiagnoseType save(DiagnoseType entity, HyperIoTContext ctx) {
        HUser loggedUser ;
        try{
            loggedUser = hUserSystemApi.find(ctx.getLoggedEntityId(),ctx);
        }catch (NoResultException exc){
            throw new HyperIoTUnauthorizedException();
        }
        AssetCategory category = new AssetCategory();
        category.setName(String.format("%s_%s", entity.getLabel(), ctx.getLoggedEntityId()));
        HyperIoTAssetOwnerImpl assetOwner = new HyperIoTAssetOwnerImpl();
        assetOwner.setOwnerResourceName(DiagnoseType.class.getName());
        assetOwner.setUserId(ctx.getLoggedEntityId());
        category.setOwner(assetOwner);
        entity.setCategory(category);
        entity.setUser(loggedUser);
        entity= super.save(entity, ctx);
        //Save entity such that id field is valorize correctly, such that we can use them to assign to Category's resource owner.
        entity.getCategory().getOwner().setOwnerResourceId(entity.getId());
        return super.update(entity, ctx);
    }

    @Override
    public DiagnoseType addKitToDiagnoseTypeCategory(HyperIoTContext ctx, long diagnoseTypeId, long kitId) {
        return this.executeTransactionWithReturn(TransactionType.Required, entityManager -> {
            try{
                kitSystemApi.find(kitId,ctx);
            }catch (NoResultException exc){
                throw new HyperIoTEntityNotFound();
            }
            DiagnoseType diagnoseType;
            try{
                diagnoseType= this.repository.find(diagnoseTypeId,ctx);
            }catch (NoResultException exc){
                throw new HyperIoTEntityNotFound();
            }
            AssetCategoryResource resource = new AssetCategoryResource();
            resource.setCategory(diagnoseType.getCategory());
            resource.setResourceId(kitId);
            resource.setResourceName(Kit.class.getName());
            //Explicit reference to AssetCategoryResource to trigger lazy loading.
            Set<AssetCategoryResource> resources = diagnoseType.getCategory().getResources();
            //Cascading Persist
            resources.add(resource);
            return diagnoseType;
        });
    }

    @Override
    public void removeKitFromDiagnoseTypeCategory(HyperIoTContext ctx, long diagnoseTypeId, long kitId) {
        this.executeTransaction(TransactionType.Required, entityManager -> {
            try{
                kitSystemApi.find(kitId,ctx);
            }catch (NoResultException exc){
                throw new HyperIoTEntityNotFound();
            }
            DiagnoseType diagnoseType;
            try{
                diagnoseType= this.repository.find(diagnoseTypeId,ctx);
            }catch (NoResultException exc){
                throw new HyperIoTEntityNotFound();
            }
            //Explicit reference to AssetCategoryResource to trigger lazy loading.
            Set<AssetCategoryResource> resources = diagnoseType.getCategory().getResources();
            //Cascading remove
            resources.removeIf(resource ->
                    resource.getResourceId() == kitId &&
                            resource.getResourceName().equals(Kit.class.getName()));
        });
    }

    @Override
    public Collection<Kit> getKitByDiagnoseTypeCategory(HyperIoTContext ctx, long diagnoseTypeId) {
        return this.executeTransactionWithReturn(TransactionType.Required, entityManager -> {
            DiagnoseType diagnoseType ;
            try{
                diagnoseType=this.repository.find(diagnoseTypeId,ctx);
            }catch (NoResultException exc){
                throw new HyperIoTEntityNotFound();
            }
            if(diagnoseType.getCategory() == null){
                //This is an error , because when we save a DiagnoseType we register an AssetCategory relative to him.
                throw new HyperIoTRuntimeException();
            }
            Set<Long> kitsIdRelatedToDiagnoseTypeCategory= diagnoseType.
                    getCategory().getResources().stream().filter(assetCategoryResource -> assetCategoryResource.getResourceName().equals(Kit.class.getName())).
                    map(AssetCategoryResource::getResourceId).collect(Collectors.toSet());
            if(kitsIdRelatedToDiagnoseTypeCategory.isEmpty()){
                return new LinkedList<>();
            }
            HyperIoTQuery queryForRetrievePermittedKit = KitPermissionUtils.getQueryForFindOnlyPermittedKit(this.hProjectApi, ctx);
            Collection<Kit> userPermittedKit = this.kitSystemApi.findAll(queryForRetrievePermittedKit,ctx);
            List<Kit> kitRelativeToDiagnoseTypeSuchThatUserHasPermission = new LinkedList<>();
            for(Kit kit : userPermittedKit) {
                if (kitsIdRelatedToDiagnoseTypeCategory.contains(kit.getId())) {
                    kitRelativeToDiagnoseTypeSuchThatUserHasPermission.add(kit);
                }
            }
            return kitRelativeToDiagnoseTypeSuchThatUserHasPermission;
        });
    }

    @Override
    public Collection<DiagnoseType> getDiagnoseTypeByKit(HyperIoTContext ctx, long kitId) {
        long[] kitCategoriesId = this.kitSystemApi.getKitCategories(kitId);
        if(kitCategoriesId.length == 0){
            return new ArrayList<>();
        }
        HyperIoTQuery diagnoseTypeByCategoryId = HyperIoTQueryBuilder.newQuery();
        boolean first = true;
        for(long l : kitCategoriesId){
            if(first){
                diagnoseTypeByCategoryId.equals("category.id",l);
                first=false;
            }else{
                diagnoseTypeByCategoryId.or(HyperIoTQueryBuilder.newQuery().equals("category.id",l));
            }
        }
        //Add next line if you want this behaviour:
        //When user retrieve diagnose type relative to a kit, he retrieve only the DiagnoseType that he associated to a Kit.
        //diagnoseTypeByCategoryId=diagnoseTypeByCategoryId.and(HyperIoTQueryBuilder.newQuery().equals("user.id",ctx.getLoggedEntityId()));
        return this.findAll(diagnoseTypeByCategoryId,null);
    }

    @Activate
    public void onActivate(){
        this.checkRegisteredUserRoleExists();
    }

    private void checkRegisteredUserRoleExists() {
        String diagnoseTypeResourceName = DiagnoseType.class.getName();
        List<HyperIoTAction> diagnoseTypeCrudAction = HyperIoTActionsUtil.getHyperIoTCrudActions(diagnoseTypeResourceName);
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, diagnoseTypeCrudAction);
    }

}

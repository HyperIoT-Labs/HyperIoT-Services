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

package it.acsoftware.hyperiot.kit.service;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.hproject.api.HProjectApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.kit.api.KitSystemApi;
import it.acsoftware.hyperiot.kit.model.Kit;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class KitPermissionUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(KitPermissionUtils.class.getName());

    public static void checkUserHasPermissionToHandleKitCategory(KitSystemApi kitSystemApi, HProjectApi hProjectApi, long kitId, HyperIoTContext ctx){
        Kit kit = kitSystemApi.find(kitId,ctx);
        checkUserHasPermissionOnKit(hProjectApi,kit,ctx);
    }

    public static HyperIoTQuery getQueryForFindOnlyPermittedKit(HProjectApi hProjectApi, HyperIoTContext ctx){
        //Load all project that user can find.
        Collection<HProject> projectsVisibleByLoggedUser = hProjectApi.findAll((HyperIoTQuery) null, ctx);
        //Add a filter such that user can retrieve system kit.
        HyperIoTQuery findOnlyPermittedKit = HyperIoTQueryBuilder.newQuery().equals("projectId",KitUtils.SYSTEM_KIT_PROJECT_ID);
        if(projectsVisibleByLoggedUser == null || projectsVisibleByLoggedUser.isEmpty()){
            return findOnlyPermittedKit;
        }
        for(HProject project : projectsVisibleByLoggedUser){
            findOnlyPermittedKit= HyperIoTQueryBuilder.newQuery().equals("projectId",project.getId()).or(findOnlyPermittedKit);
        }
        return findOnlyPermittedKit;
    }

    public static void checkUserHasPermissionOnKit(HProjectApi hProjectApi, Kit kit, HyperIoTContext ctx){
        if(kit == null){
            throw new HyperIoTRuntimeException();
        }
        if(kit.getProjectId()==KitUtils.SYSTEM_KIT_PROJECT_ID  && !ctx.isAdmin()){
            //Only admin user can save/remove/update System Kit
            throw new HyperIoTUnauthorizedException();
        }
        if(kit.getProjectId() > 0){
            //Only user that own the project can save/remove/update a kit on his project.
            HProject project ;
            try {
                project=hProjectApi.find(kit.getProjectId(), ctx);
            }catch (NoResultException exc){
                throw new HyperIoTRuntimeException();
            }
            if(! project.getUserOwner().getUsername().equals(ctx.getLoggedUsername())){
                throw new HyperIoTUnauthorizedException();
            }
        }
    }

    public static void checkUserCanFindKit(HProjectApi hProjectApi, SharedEntitySystemApi sharedEntitySystemApi, Kit kit, HyperIoTContext ctx){
        //System kit will be visible for all user.
        if(KitUtils.isSystemKit(kit)){
            return;
        }
        //If the user has the permission to find the project he is authorized to retrieve project's kit.
        try{
            hProjectApi.find(kit.getProjectId(),ctx);
            return;
        }catch (Throwable exception){
            LOGGER.debug(exception.getMessage());
        }
        //If the loggedUser is a sharing user of the project related to kit he's authorize to find the kit.
        try{
            List<HyperIoTUser> sharingUserList = sharedEntitySystemApi.getSharingUsers(HProject.class.getName(),kit.getProjectId(),ctx);
            if(sharingUserList !=  null &&
                    (! sharingUserList.isEmpty()) &&
                    sharingUserList.stream().map(HyperIoTUser::getUsername).collect(Collectors.toList()).contains(ctx.getLoggedUsername())) {
                return;}
        }catch (Throwable exception){
            LOGGER.debug(exception.getMessage());
        }
        throw new HyperIoTUnauthorizedException();
    }

}

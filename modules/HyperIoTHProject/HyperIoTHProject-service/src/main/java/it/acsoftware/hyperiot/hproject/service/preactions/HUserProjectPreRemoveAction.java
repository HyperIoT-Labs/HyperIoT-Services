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

package it.acsoftware.hyperiot.hproject.service.preactions;


import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Component(service = HyperIoTPreRemoveAction.class, property = {"type=it.acsoftware.hyperiot.huser.model.HUser"})
public class HUserProjectPreRemoveAction <T extends HyperIoTBaseEntity> implements HyperIoTPreRemoveAction<T> {

    private static final Logger log = LoggerFactory.getLogger(HUserProjectPreRemoveAction.class);

    private HProjectSystemApi hProjectSystemApi;

    @Override
    public void execute(T t) {
        HUser user = (HUser) t ;
        log.info("In HUserProjectPreRemoveAction , Delete HProject related to HUser with id {}", user.getId());
        HyperIoTQuery byUserId = HyperIoTQueryBuilder.newQuery().equals("user.id", user.getId());
        Collection<HProject> userProjects = hProjectSystemApi.findAll(byUserId, null) ;
        for (HProject project : userProjects) {
            log.info("In HUserProjectPreRemoveAction, delete hproject with id : {} , user with id : {} ", project.getId(), user.getId());
            this.hProjectSystemApi.remove(project.getId(), null);
        }
    }

    @Reference
    public void sethProjectSystemApi(HProjectSystemApi hProjectSystemApi){
        log.debug( "invoking sethProjectSystemApi, returning: {}", this.hProjectSystemApi);
        this.hProjectSystemApi =  hProjectSystemApi;
    }


}

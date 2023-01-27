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

package it.acsoftware.hyperiot.hproject.service.hbase.postactions;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * This class deletes HBase table after removing HProject.
 * @param <T>
 */
@Component(service = HyperIoTPostRemoveAction.class, property = {"type=it.acsoftware.hyperiot.hproject.model.HProject"})
public class HProjectPostRemoveAction <T extends HyperIoTBaseEntity> implements HyperIoTPostRemoveAction<T> {

    private static Logger log = LoggerFactory.getLogger(HProjectPostRemoveAction.class.getName());
    /**
     * Injecting the HProjectPostActionThreadPool
     */
    private HProjectPostActionThreadPool hProjectPostActionThreadPool;

    @Override
    public void execute(T entity) {
        HProject hProject = (HProject) entity;
        log.debug( "Drop HBase tables after deleting HProject with id {}", hProject.getId());
        hProjectPostActionThreadPool.runPostRemoveAction(hProject.getId());
    }

    /**
     *
     * @param hProjectPostActionThreadPool HProjectPostActionThreadPool service
     */
    @Reference
    public void sethProjectPostActionThreadPool(HProjectPostActionThreadPool hProjectPostActionThreadPool) {
        this.hProjectPostActionThreadPool = hProjectPostActionThreadPool;
    }

}

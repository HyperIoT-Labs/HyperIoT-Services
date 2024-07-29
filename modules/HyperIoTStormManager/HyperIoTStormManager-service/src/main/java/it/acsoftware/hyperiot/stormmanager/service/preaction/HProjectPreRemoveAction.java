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

package it.acsoftware.hyperiot.stormmanager.service.preaction;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author Aristide Cittadino
 * Pre-Remove Action which stops (if it is started) storm topology related to that project
 */
@Component(service = HyperIoTPreRemoveAction.class, property = {"type=it.acsoftware.hyperiot.hproject.model.HProject"},immediate = true)
public class HProjectPreRemoveAction implements HyperIoTPreRemoveAction<HProject> {
    private static Logger log = LoggerFactory.getLogger(HProjectPreRemoveAction.class);

    private StormManagerSystemApi stormManagerSystemApi;

    @Reference
    public void setStormManagerSystemApi(StormManagerSystemApi stormManagerSystemApi) {
        this.stormManagerSystemApi = stormManagerSystemApi;
    }

    @Override
    public void execute(HProject project) {
        try {
            //avoiding to make call in test mode just to speed up tests
            if(!HyperIoTUtil.isInTestMode() && this.stormManagerSystemApi.getTopologyStatus(project.getId()).getStatus().equalsIgnoreCase(TopologyInfo.TOPOLOGY_STATUS_ACTIVE)) {
                this.stormManagerSystemApi.killTopology(this.stormManagerSystemApi.getTopologyName(project.getId()));
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        }
    }
}

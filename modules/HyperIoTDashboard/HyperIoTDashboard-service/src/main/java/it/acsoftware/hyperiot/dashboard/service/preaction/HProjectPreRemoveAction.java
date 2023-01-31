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

package it.acsoftware.hyperiot.dashboard.service.preaction;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPreRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.widget.api.DashboardWidgetSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Component(service = HyperIoTPreRemoveAction.class, property = {"type=it.acsoftware.hyperiot.hproject.model.HProject"},immediate = true)
public class HProjectPreRemoveAction<T extends HyperIoTBaseEntity> implements HyperIoTPreRemoveAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HProjectPreRemoveAction.class.getName());
    private DashboardSystemApi dashboardSystemApi;
    private DashboardWidgetSystemApi dashboardWidgetSystemApi;

    @Override
    public void execute(T entity) {
        long hProjectId = entity.getId();
        LOGGER.debug("Delete dashboards related to HProject with id {}", hProjectId);
        //finding manually in order to preserve hibernate cascading
        HyperIoTQuery byProjectId = HyperIoTQueryBuilder.newQuery().equals("HProject.id",hProjectId);
        Collection<Dashboard> dashboards =  dashboardSystemApi.findAll(byProjectId,null);
        dashboards.stream().parallel().forEach(dashboard -> {
            dashboardSystemApi.remove(dashboard.getId(),null);
        });
    }

    @Reference
    private void setDashboardSystemApi(DashboardSystemApi dashboardSystemApi) {
        this.dashboardSystemApi = dashboardSystemApi;
    }

    @Reference
    public void setDashboardWidgetSystemApi(DashboardWidgetSystemApi dashboardWidgetSystemApi) {
        this.dashboardWidgetSystemApi = dashboardWidgetSystemApi;
    }
}

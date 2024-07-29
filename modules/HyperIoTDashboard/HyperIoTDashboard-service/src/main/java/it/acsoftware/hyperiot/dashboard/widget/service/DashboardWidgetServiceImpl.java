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

package it.acsoftware.hyperiot.dashboard.widget.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.annotations.AllowPermissions;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTOwnedChildBaseEntityServiceImpl;
import it.acsoftware.hyperiot.dashboard.actions.HyperIoTDashboardAction;
import it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.widget.api.DashboardWidgetApi;
import it.acsoftware.hyperiot.dashboard.widget.api.DashboardWidgetSystemApi;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * @author Aristide Cittadino Implementation class of DashboardWidgetApi
 * interface. It is used to implement all additional methods in order to
 * interact with the system layer.
 */
@Component(service = DashboardWidgetApi.class, immediate = true)
public final class DashboardWidgetServiceImpl extends HyperIoTOwnedChildBaseEntityServiceImpl<DashboardWidget>
        implements DashboardWidgetApi, HyperIoTOwnershipResourceService {

    private DashboardSystemApi dashboardServiceApi;

    /**
     * Injecting the DashboardWidgetSystemApi
     */
    private DashboardWidgetSystemApi systemService;

    /**
     * @param dashboardServiceApi Injecting DashboardWidgetSystemApi
     */
    @Reference(service = DashboardSystemApi.class)
    protected void setDashboardServiceApi(DashboardSystemApi dashboardServiceApi) {
        getLog().debug( "invoking setDashboarServiceApi, setting: {}" , this.dashboardServiceApi);
        this.dashboardServiceApi = dashboardServiceApi;
    }

    /**
     * Constructor for a DashboardWidgetServiceImpl
     */
    public DashboardWidgetServiceImpl() {
        super(DashboardWidget.class);
    }

    /**
     * @return The current DashboardWidgetSystemApi
     */
    protected DashboardWidgetSystemApi getSystemService() {
        getLog().debug( "invoking getSystemService, returning: {}" , this.systemService);
        return systemService;
    }

    /**
     * @param dashboardWidgetSystemService Injecting via OSGi DS current
     *                                     systemService
     */
    @Reference
    protected void setSystemService(DashboardWidgetSystemApi dashboardWidgetSystemService) {
        getLog().debug( "invoking setSystemService, setting: {}" , systemService);
        this.systemService = dashboardWidgetSystemService;
    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.FIND, checkById = true)
    public String getDashboardWidgetConf(long dashboardWidgetId, HyperIoTContext ctx) {
        getLog().debug( "invoking getDashboardWidgetConf, on DashboardWidget : {}" , dashboardWidgetId);
        try {
            this.systemService.find(dashboardWidgetId, ctx);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        try {
            return this.getSystemService().find(dashboardWidgetId,null).getWidgetConf();
        } catch (NoResultException e) {
            getLog().debug( "Entity Not Found ");
            throw new HyperIoTEntityNotFound();
        }
    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.UPDATE, checkById = true)
    public DashboardWidget setDashboardWidgetConf(long dashboardWidgetId, String widgetConf, HyperIoTContext ctx) {
        getLog().debug( "invoking setDashboardWidgetConf, update dashboard widget conf: {}" , dashboardWidgetId);
        DashboardWidget dw;
        try {
            dw = this.systemService.find(dashboardWidgetId, ctx);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (dw != null) {
            dw.setWidgetConf(widgetConf);
            this.systemService.update(dw, ctx);
            return dw;
        } else {
            getLog().debug( "Entity Not Found ");
            throw new HyperIoTEntityNotFound();
        }
    }

    @Override
    @AllowPermissions(actions = HyperIoTDashboardAction.Names.FIND_WIDGETS, checkById = true,systemApiRef = "it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi")
    public Collection<DashboardWidget> getAllDashboardWidget(long dashboardId, HyperIoTContext ctx) {
        getLog().debug( "invoking getAllDashboardWidget, on dashboard: {}" , dashboardId);
        try {
            this.dashboardServiceApi.find(dashboardId, ctx);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        Collection<DashboardWidget> dashboardWidgets = this.getSystemService().getAllDashboardWidget(dashboardId);
        if (!dashboardWidgets.isEmpty()) {
            return dashboardWidgets;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    @AllowPermissions(actions = HyperIoTCrudAction.Names.UPDATE, checkById = true,systemApiRef = "it.acsoftware.hyperiot.dashboard.api.DashboardSystemApi")
    public void updateDashboardWidget(long dashboardId, DashboardWidget[] widgetConfiguration, HyperIoTContext ctx) {
        // this should verify that dashboard exists
        // and user has permission on it
        Dashboard dashboard = null;
        try {
            dashboard = dashboardServiceApi.find(dashboardId, ctx);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        if (HyperIoTSecurityUtil.checkPermissionAndOwnership(ctx, dashboard,
                   HyperIoTActionsUtil.getHyperIoTAction(DashboardWidget.class.getName(), HyperIoTCrudAction.UPDATE), widgetConfiguration)) {
                this.systemService.updateDashboardWidget(widgetConfiguration, ctx);
        } else {
            throw new HyperIoTUnauthorizedException();
        }
    }

    @Override
    public String getOwnerFieldPath() {
        return "dashboard.HProject.user.id";
    }

    @Override
    protected String getRootParentFieldPath() {
        return "dashboard.HProject.id";
    }

    @Override
    protected Class<? extends HyperIoTOwnedResource> getParentResourceClass() {
        return HProject.class;
    }

}

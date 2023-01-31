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

package it.acsoftware.hyperiot.widget.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import it.acsoftware.hyperiot.widget.api.WidgetRatingRepository;
import it.acsoftware.hyperiot.widget.api.WidgetRepository;
import it.acsoftware.hyperiot.widget.api.WidgetSystemApi;
import it.acsoftware.hyperiot.widget.model.Widget;
import it.acsoftware.hyperiot.widget.model.WidgetCategory;
import it.acsoftware.hyperiot.widget.model.WidgetDomain;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.*;

/**
 * @author Aristide Cittadino Implementation class of the WidgetSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = WidgetSystemApi.class, immediate = true)
public final class WidgetSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Widget> implements WidgetSystemApi {

    /**
     * Injecting the WidgetRepository to interact with persistence layer
     */
    private WidgetRepository repository;
    private WidgetRatingRepository widgetRatingRepository;

    /**
     * Injecting PermissionSystemApi
     */
    private PermissionSystemApi permissionSystemApi;

    /**
     * Constructor for a WidgetSystemServiceImpl
     */
    public WidgetSystemServiceImpl() {
        super(Widget.class);
    }

    /**
     * Return the current repository
     */
    protected WidgetRepository getRepository() {
        getLog().debug( "invoking getRepository, returning: {}", this.repository);
        return repository;
    }

    /**
     * @param permissionSystem Injecting via OSGi DS current PermissionSystemService
     */
    @Reference
    public void setPermissionSystem(PermissionSystemApi permissionSystem) {
        this.permissionSystemApi = permissionSystem;
    }

    /**
     * @param widgetRepository The current value of WidgetRepository to interact with persistence layer
     */
    @Reference
    protected void setRepository(WidgetRepository widgetRepository) {
        getLog().debug( "invoking setRepository, setting: {}", widgetRepository);
        this.repository = widgetRepository;
    }

    @Reference
    public void setWidgetRatingRepository(WidgetRatingRepository widgetRatingRepository) {
        this.widgetRatingRepository = widgetRatingRepository;
    }

    @Override
    public void rateWidget(int rating, Widget w, HyperIoTContext ctx) {
        if (ctx.getLoggedUsername() != null) {
            try {
                w = this.find(w.getId(), ctx);
            } catch (NullPointerException e) {
                throw new HyperIoTEntityNotFound();
            }
            this.widgetRatingRepository.rateWidget(rating, w, ctx.getLoggedUsername());
        }
    }

    public Widget findByName(String name) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("name", name);
        try {
            HyperIoTQuery findByName = HyperIoTQueryBuilder.newQuery().equals("name",name);
            return this.find(findByName,null);
        } catch (NoResultException e) {
            return null;
        }
    }

    private void createWidgetIfNotExists(String name, String description, byte[] image, byte[] preView, int cols, int rows, String type, String config, WidgetCategory cat, Set<WidgetDomain> domains, boolean offline, boolean realTime) {
        Widget w = this.findByName(name);
        if (w == null) {
            w = new Widget();
            w.setCols(cols);
            w.setDomains(domains);
            w.setImage(image);
            w.setPreView(preView);
            w.setName(name);
            w.setDescription(description);
            w.setRows(rows);
            w.setType(type);
            w.setWidgetCategory(cat);
            w.setOffline(offline);
            w.setRealTime(realTime);
            this.repository.save(w);
        }
    }

    /**
     * On Bundle activated
     */
    @Activate
    public void onActivate() {
        this.createBasicWidgets();
        this.checkRegisteredUserRoleExists();
    }

    /**
     * Register permissions for new users
     */
    private void checkRegisteredUserRoleExists() {
        String resourceName = Widget.class.getName();
        List<HyperIoTAction> actions = new ArrayList<>();
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTCrudAction.FIND));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTCrudAction.FINDALL));
        this.permissionSystemApi.checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    private void createBasicWidgets() {
        this.createWidgetIfNotExists("Realtime Events log LIST", "This widget shows real time event stream data.", WidgetUtils.eventsLogImg, WidgetUtils.eventsLogPreView, 2, 2, "events-log", "{}", WidgetCategory.TABLES, null, false, true);
        //this.createWidgetIfNotExists("Classic pie chart", "Widget to show statistics data with pie chart representation.", WidgetUtils.classicPieChartImg, WidgetUtils.classicPieChartPreView, 2, 3, "stats-chart", "{\"data\":[]}", WidgetCategory.PIE, null, false, true);
        this.createWidgetIfNotExists("Realtime line chart", "Widget to show single or combined realtime statistics with a line chart representation.", WidgetUtils.realtimeLineChartImg, WidgetUtils.realtimeLineChartPreView, 2, 3, "time-chart", "{\"data\":[]}", WidgetCategory.LINE, null, false, true);
        this.createWidgetIfNotExists("Realtime FFT Sine Waves", "Widget to show Fast Fourier Transform sine waves.", WidgetUtils.realtimeFftChartImg, WidgetUtils.realtimeFftChartPreView, 2, 3, "fourier-chart", "{\"data\":[]}", WidgetCategory.LINE, null, false, true);
        //this.createWidgetIfNotExists("Classic line chart", "Widget to show single or combined statistics with a line chart representation.", WidgetUtils.classicLineChartImg, WidgetUtils.classicLineChartPreView, 2, 3, "stats-chart", "{\"data\":[]}", WidgetCategory.LINE, null, false, true);
        this.createWidgetIfNotExists("Realtime Sensor value", "Widget to display sensor values.", WidgetUtils.sensorValueImg, WidgetUtils.sensorValuePreView, 1, 2, "sensor-value", "{\"data\":[]}", WidgetCategory.GAUGES, null, false, true);
        this.createWidgetIfNotExists("Realtime data table", "This widget displays realtime values of fields in tabular format.", WidgetUtils.realtimeDataTableImg, WidgetUtils.realtimeDataTablePreView, 3, 3, "realtime-table", "{}", WidgetCategory.TABLES, null, false, true);
        this.createWidgetIfNotExists("Data table", "This widget displays values of fields in tabular format.", WidgetUtils.dataTableImg, WidgetUtils.dataTablePreView, 3, 3, "offline-table", "{}", WidgetCategory.TABLES, null, true, false);
        this.createWidgetIfNotExists("Image data", "Display image based on thermal camera data array.", WidgetUtils.imageData, WidgetUtils.imageDataPreview, 2, 3, "image-data", "{}", WidgetCategory.MAP, null, false, true);
        this.createWidgetIfNotExists("Algorithm data table", "This widget displays output values of algorithms in tabular format.", WidgetUtils.algorithmDataTableImg, WidgetUtils.algorithmDataTablePreView, 3, 3, "algorithm-offline-table", "{}", WidgetCategory.TABLES, null, true, false);
        this.createWidgetIfNotExists("Event data table", "This widget displays events of project in tabular format.", WidgetUtils.eventDataTableImg, WidgetUtils.eventDataTablePreView, 3, 3, "event-offline-table", "{}", WidgetCategory.TABLES, null, true, false);
        this.createWidgetIfNotExists("Error data table", "This widget displays errors of project in tabular format.", WidgetUtils.errorDataTableImg, WidgetUtils.errorDataTablePreView, 3, 3, "error-table", "{}", WidgetCategory.TABLES, null, true, false);
        this.createWidgetIfNotExists("ecg-trace", "Show ECG trace chart (3 channel).", WidgetUtils.ecgImg, WidgetUtils.ecgPreView, 6, 6, "ecg", "{}", WidgetCategory.LINE, null, true, true);
        this.createWidgetIfNotExists("Bodymap", "Bodymap widget.", WidgetUtils.bodyMapImg, WidgetUtils.bodyMapPreView, 2, 6, "bodymap", "{}", WidgetCategory.LINE, null, false, true);
        this.createWidgetIfNotExists("Time chart multistate", "Shows a time chart multistate.", WidgetUtils.timeChartMultiStateImg, WidgetUtils.timeCharMultiStatePreView, 3, 3, "time-multistate-chart", "{}", WidgetCategory.LINE, null, true, true);
    }

    @Override
    public Collection<Widget> getWidgetsByCategory(WidgetCategory widgetCategory, String type) {
        return repository.getWidgetsByCategory(widgetCategory,type);
    }

}

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

package it.acsoftware.hyperiot.dashboard.widget.service.rest;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTNoResultException;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.dashboard.api.DashboardApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.widget.api.DashboardWidgetApi;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Aristide Cittadino DashboardWidget rest service class. Registered
 * with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/dashboardwidgets", info = @Info(description = "HyperIoT DashboardWidget API", version = "2.0.0", title = "HyperIoT DashboardWidget", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/dashboardwidgets", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = DashboardWidgetRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.dashboard.widget.service.rest.DashboardWidgetRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/dashboardwidgets",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"}, immediate = true)
@Path("")
public class DashboardWidgetRestApi extends HyperIoTBaseEntityRestApi<DashboardWidget> {
    private DashboardWidgetApi entityService;

    private DashboardApi dashboardApi;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT DashboardWidget Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug( "In Rest Service GET /hyperiot/dashboardwidget/module/status");
        return Response.ok("DashboardWidget Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<DashboardWidget> getEntityService() {
        getLog().debug( "invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = DashboardWidgetApi.class)
    protected void setEntityService(DashboardWidgetApi entityService) {
        getLog().debug( "invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * @param dashboardApi: InjectingDashboardApi
     */
    @Reference(service= DashboardApi.class)
    protected void setDashboardApi(DashboardApi dashboardApi) {
        getLog().debug( "invoking setDashboardApi, setting: {}", this.dashboardApi);
        this.dashboardApi = dashboardApi;
    }

    /**
     * @Return the current DashboardApi
     */
    protected DashboardApi getDashboardApi() {
        getLog().debug( "invoking getDashboardApi, returning: {}", this.dashboardApi);
        return dashboardApi;
    }

    /**
     * Service finds an existing DashboardWidget
     *
     * @param id id from which DashboardWidget object will retrieved
     * @return DashboardWidget if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets/{id}", notes = "Service for finding dashboardwidget", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response findDashboardWidget(
            @ApiParam(value = "id from which DashboardWidget object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug( "In Rest Service GET /hyperiot/dashboardwidgets/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new DashboardWidget
     *
     * @param entity DashboardWidget object to store in database
     * @return the DashboardWidget saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets", notes = "Service for adding a new dashboardwidget entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response saveDashboardWidget(
            @ApiParam(value = "DashboardWidget entity which must be saved ", required = true) DashboardWidget entity) {
        getLog().debug( "In Rest Service POST /hyperiot/dashboardwidgets \n Body: {}", entity);
        Dashboard dashboard ;
        try {
            if(entity.getDashboard() == null){
                throw new HyperIoTEntityNotFound();
            }
            dashboard = dashboardApi.find(entity.getDashboard().getId(),getHyperIoTContext());
        } catch (Throwable exc){
            return this.handleException(exc);
        }
        entity.setDashboard(dashboard);
        return this.save(entity);
    }

    /**
     * Service updates a DashboardWidget
     *
     * @param entity DashboardWidget object to update in database
     * @return the DashboardWidget updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets", notes = "Service for updating a dashboardwidget entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    public Response updateDashboardWidget(
            @ApiParam(value = "DashboardWidget entity which must be updated ", required = true) DashboardWidget entity) {
        getLog().debug( "In Rest Service PUT /hyperiot/dashboardwidgets \n Body: {}", entity);
        Dashboard dashboard ;
        try {
            if(entity.getDashboard() == null){
                throw new HyperIoTEntityNotFound();
            }
            dashboard = dashboardApi.find(entity.getDashboard().getId(),getHyperIoTContext());
        } catch (Throwable exc){
            return this.handleException(exc);
        }
        entity.setDashboard(dashboard);
        return this.update(entity);
    }

    /**
     * Service deletes a DashboardWidget
     *
     * @param id id from which DashboardWidget object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets/{id}", notes = "Service for deleting a dashboardwidget entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    public Response deleteDashboardWidget(
            @ApiParam(value = "The dashboardwidget id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug( "In Rest Service DELETE /hyperiot/dashboardwidgets/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available dashboardwidget
     *
     * @return list of all available dashboardwidget
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets/all", notes = "Service for finding all dashboardwidget entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllDashboardWidget() {
        getLog().debug( "In Rest Service GET /hyperiot/dashboardwidgets/all");
        return this.findAll();
    }

    /**
     * Service finds all available dashboardwidget
     *
     * @return list of all available dashboardwidget
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets", notes = "Service for finding all dashboardwidget entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllDashboardWidgetPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug( "In Rest Service GET /hyperiot/dashboardwidgets/");
        return this.findAll(delta, page);
    }

    /**
     * Service gets dashboard widget configuration
     *
     * @param dashboardWidgetId id of the Dashboard
     * @return list of all widgets in the dashboard
     */
    @GET
    @Path("/configuration/{dashboardWidgetId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets/configuration/{dashboardWidgetId}", notes = "Get dashboard widget configuration", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response findDashboardWidgetConf(
            @ApiParam(value = "dashboard widget id from which configuration will retrieve", required = true) @PathParam("dashboardWidgetId") long dashboardWidgetId) {
        getLog().debug( "In Rest Service GET hyperiot/dashboardwidgets/configuration/{}", dashboardWidgetId);
        try {
            return Response.ok().entity(entityService.getDashboardWidgetConf(dashboardWidgetId, getHyperIoTContext()))
                    .build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service updates a dashboard widget configuration
     *
     * @param dashboardWidgetId Dashboard widget id
     * @param widgetConf        Widget configuration as JSON string
     * @return the updated DashboardWidget
     */
    @PUT
    @Path("/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets/configuration", notes = "Service for updating a dashboard widget configuration", httpMethod = "PUT", consumes = "application/json", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    public Response setDashboardWidgetConf(
            @ApiParam(value = "dashboard widget id from which configuration will saved", required = true) @QueryParam("dashboardWidgetId") long dashboardWidgetId,
            @ApiParam(value = "new dashboard widget configuration in JSON format", required = true) String widgetConf) {
        getLog().debug( "In Rest Service PUT /hyperiot/dashboardwidgets/configuration \n Dashboard widget ID: {} \n new dashboard widget configuration: {}", new Object[]{dashboardWidgetId, widgetConf});
        try {
            return Response.ok()
                    .entity(entityService.setDashboardWidgetConf(dashboardWidgetId, widgetConf, getHyperIoTContext()))
                    .build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service finds all available dashboard widgets inside a particular dashboard
     *
     * @return list of all available dashboard widgets inside a dashboard
     */
    @GET
    @Path("/configuration/all/{dashboardId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboardwidgets/configuration/all/{dashboardId}", notes = "Service for finding all dashboard widget inside a dashboard", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllDashboardWidgetInDashboard(
            @ApiParam(value = "dashboard id from which dashboard widgets will retrieve", required = true) @PathParam("dashboardId") long dashboardId) {
        getLog().debug( "In Rest Service GET /hyperiot/dashboardwidgets/configuration/all/{}", dashboardId);
        try {
            return Response.ok().entity(entityService.getAllDashboardWidget(dashboardId, getHyperIoTContext())).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Updates all widgets configuration of the dashboard with the given id
     *
     * @param dashboardId The dashboard id
     * @return Response status OK or ERROR
     */
    @PUT
    @Path("/configuration/all/{dashboardId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(
            value = "/hyperiot/dashboardwidgets/configuration/all/{dashboardId}",
            notes = "Updates all widgets configuration of the dashboard with the given id",
            httpMethod = "PUT",
            consumes = MediaType.APPLICATION_JSON,
            authorizations = @Authorization("jwt-auth")
    )
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response saveAllDashboardWidget(
            @ApiParam(value = "dashboard id", required = true) @PathParam("dashboardId") long dashboardId,
            @ApiParam(value = "dashboard configuration", required = true) DashboardWidget[] configuration
    ) {
        getLog().debug( "In Rest Service POST /hyperiot/dashboardwidgets/configuration/all/{}", dashboardId);
        try {
            entityService.updateDashboardWidget(
                    dashboardId,
                    configuration,
                    getHyperIoTContext()
            );
            return Response.ok().build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

}

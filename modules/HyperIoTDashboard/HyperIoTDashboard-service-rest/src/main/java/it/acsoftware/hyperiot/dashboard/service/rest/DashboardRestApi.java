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

package it.acsoftware.hyperiot.dashboard.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.dashboard.api.DashboardApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;

/**
 * @author Aristide Cittadino Dashboard rest service class. Registered with
 * DOSGi CXF
 */
@SwaggerDefinition(basePath = "/dashboards", info = @Info(description = "HyperIoT Dashboard API", version = "2.0.0", title = "HyperIoT Dashboard", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/dashboards", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = DashboardRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.dashboard.service.rest.DashboardRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/dashboards",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"}, immediate = true)
@Path("")
public class DashboardRestApi extends HyperIoTBaseEntityRestApi<Dashboard> {
    private DashboardApi entityService;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Dashboard Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/dashboard/module/status");
        return Response.ok("Dashboard Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<Dashboard> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = DashboardApi.class)
    protected void setEntityService(DashboardApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing Dashboard
     *
     * @param id id from which Dashboard object will retrieved
     * @return Dashboard if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/{id}", notes = "Service for finding dashboard", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findDashboard(
            @ApiParam(value = "id from which dashboard object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new Dashboard
     *
     * @param entity Dashboard object to store in database
     * @return the Dashboard saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards", notes = "Service for adding a new dashboard entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response saveDashboard(
            @ApiParam(value = "Dashboard entity which must be saved ", required = true) Dashboard entity) {
        getLog().debug("In Rest Service POST /hyperiot/dashboards \n Body: {}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a Dashboard
     *
     * @param entity Dashboard object to update in database
     * @return the Dashboard updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards", notes = "Service for updating a dashboard entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response updateDashboard(
            @ApiParam(value = "Dashboard entity which must be updated ", required = true) Dashboard entity) {
        getLog().debug("In Rest Service PUT /hyperiot/dashboards \n Body: {}", entity);
        return this.update(entity);
    }

    /**
     * Service deletes a Dashboard
     *
     * @param id id from which Dashboard object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/{id}", notes = "Service for deleting a dashboard entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response deleteDashboard(
            @ApiParam(value = "The dashboard id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/dashboards/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available Dashboard
     *
     * @return list of all available Dashboard
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/all", notes = "Service for finding all dashboard entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findAllDashboard() {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/all");
        return this.findAll();
    }

    /**
     * Service finds all available Dashboard
     *
     * @return list of all available Dashboard
     */
    @GET
    @Path("/project/{projectId}/realtime")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/project/{projectId}/realtime", notes = "Service for finding realtime dashboard related to a specific project", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findHProjectRealtimeDashboard(@ApiParam(value = "The project id ", required = true) @PathParam("projectId") Long projectId) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/project/{projectId}/realtime");
        try {
            HashMap<String, Object> filter = new HashMap<String, Object>();
            filter.put("dashboardType", DashboardType.REALTIME);
            filter.put("HProject.id", projectId);
            filter.put("deviceId", 0);
            return Response.ok(this.entityService.findAll(filter, this.getHyperIoTContext())).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Service finds offline Dashboard
     *
     * @return list Offline Dashboard
     */
    @GET
    @Path("/project/{projectId}/offline")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/project/{projectId}/offline", notes = "Service for finding offline dashboard related to a specific project", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findHProjectOfflineDashboard(@ApiParam(value = "The project id ", required = true) @PathParam("projectId") Long projectId) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/project/{}", projectId);
        try {
            HashMap<String, Object> filter = new HashMap<String, Object>();
            filter.put("dashboardType", DashboardType.OFFLINE);
            filter.put("HProject.id", projectId);
            filter.put("deviceId", 0);
            return Response.ok(this.entityService.findAll(filter, this.getHyperIoTContext())).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Get the realtime Dashboard associated to the Area with the given id
     *
     * @return the Area Dashboard
     */
    @GET
    @Path("/area/{areaId}/realtime")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/area/{areaId}/realtime", notes = "Service for finding realtime dashboard related to a specific area", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findAreaRealtimeDashboard(@ApiParam(value = "The area id ", required = true) @PathParam("areaId") Long areaId) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/area/{}/realtime", areaId);
        try {
            HashMap<String, Object> filter = new HashMap<>();
            filter.put("dashboardType", DashboardType.REALTIME);
            filter.put("area.id", areaId);
            return Response.ok(this.entityService.findAll(filter, this.getHyperIoTContext())).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Get the offline Dashboard associated to the Area with the given id
     *
     * @return the Area Dashboard
     */
    @GET
    @Path("/area/{areaId}/offline")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/area/{areaId}/offline", notes = "Service for finding offline dashboard related to a specific area", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findAreaOfflineDashboard(@ApiParam(value = "The area id ", required = true) @PathParam("areaId") Long areaId) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/area/{}/offline", areaId);
        try {
            HashMap<String, Object> filter = new HashMap<>();
            filter.put("dashboardType", DashboardType.OFFLINE);
            filter.put("area.id", areaId);
            return Response.ok(this.entityService.findAll(filter, this.getHyperIoTContext())).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Get the realtime Dashboard associated to the Area with the given id
     *
     * @return the Area Dashboard
     */
    @GET
    @Path("/hdevice/{hdeviceId}/realtime")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/hdevice/{deviceId}/realtime", notes = "Service for finding realtime dashboard related to a specific device", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findDeviceRealtimeDashboard(@ApiParam(value = "The device id ", required = true) @PathParam("hdeviceId") Long deviceId) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/hdevice/{}/realtime", deviceId);
        try {
            HashMap<String, Object> filter = new HashMap<>();
            filter.put("dashboardType", DashboardType.REALTIME);
            filter.put("deviceId", deviceId);
            return Response.ok(this.entityService.findAll(filter, this.getHyperIoTContext())).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Get the realtime Dashboard associated to the Area with the given id
     *
     * @return the Area Dashboard
     */
    @GET
    @Path("/hdevice/{hdeviceId}/offline")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards/hdevice/{deviceId}/offline", notes = "Service for finding offline dashboard related to a specific device", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findDeviceOfflineDashboard(@ApiParam(value = "The device id ", required = true) @PathParam("deviceId") Long deviceId) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/hdevice/{}/offline", deviceId);
        try {
            HashMap<String, Object> filter = new HashMap<>();
            filter.put("dashboardType", DashboardType.OFFLINE);
            filter.put("deviceId", deviceId);
            return Response.ok(this.entityService.findAll(filter, this.getHyperIoTContext())).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Service finds all available Dashboard
     *
     * @return list of all available Dashboard
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/dashboards", notes = "Service for finding all dashboard entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView({HyperIoTJSONView.Public.class})
    public Response findAllDashboardPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/dashboards/");
        return this.findAll(delta, page);
    }

}

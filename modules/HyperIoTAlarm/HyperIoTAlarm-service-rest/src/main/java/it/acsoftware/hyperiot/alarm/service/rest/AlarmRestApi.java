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

package it.acsoftware.hyperiot.alarm.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.alarm.api.AlarmApi;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.alarm.model.Alarm;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import org.apache.commons.lang3.ArrayUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;


/**
 * @author Aristide Cittadino Alarm rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/alarms", info = @Info(description = "HyperIoT Alarm API", version = "2.0.0", title = "hyperiot Alarm", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(tags = "Alarms", value = "/alarms", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HyperIoTRestService.class,immediate = true)
@Path("/alarms")
public class AlarmRestApi extends HyperIoTBaseEntityRestApi<Alarm> implements HyperIoTRestService {
    private AlarmApi entityService;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Role Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/alarm/module/status");
        return Response.ok("Alarm Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<Alarm> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = AlarmApi.class)
    protected void setEntityService(AlarmApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * @param entityService: Unsetting current entityService
     */
    protected void unsetEntityService(AlarmApi entityService) {
        getLog().debug("invoking unsetEntityService, setting: {}", entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing %- projectSuffixUC
     *
     * @param id id from which %- projectSuffixUC  object will retrieved
     * @return Alarm if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms/{id}", notes = "Service for finding alarm", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAlarm(@PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/alarms/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new Alarm
     *
     * @param entity Alarm object to store in database
     * @return the Alarm saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms", notes = "Service for adding a new alarm entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveAlarm(
            @ApiParam(value = "Alarm entity which must be saved ", required = true) Alarm entity) {
        getLog().debug("In Rest Service POST /hyperiot/alarms \n Body: {}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a Alarm
     *
     * @param entity Alarm object to update in database
     * @return the Alarm updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms", notes = "Service for updating a alarm entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateAlarm(
            @ApiParam(value = "Alarm entity which must be updated ", required = true) Alarm entity) {
        getLog().debug("In Rest Service PUT /hyperiot/alarms \n Body: {}", entity);
        return this.update(entity);
    }

    /**
     * Service deletes a Alarm
     *
     * @param id id from which Alarm object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms/{id}", notes = "Service for deleting a alarm entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteAlarm(
            @ApiParam(value = "The alarm id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/alarms/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available alarm
     *
     * @return list of all available alarm
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms/all", notes = "Service for finding all alarm entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllAlarm() {
        getLog().debug("In Rest Service GET /hyperiot/alarms/");
        return this.findAll();
    }

    /**
     * Service finds all available alarm
     *
     * @return list of all available alarm
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms", notes = "Service for finding all alarm entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllAlarmPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/alarms/");
        return this.findAll(delta, page);
    }

    /**
     * Service save a new Alarms with AlarmEvent list.
     *
     * @param alarmEvents the AlarmEvents of the Alarm
     * @param alarmName   the name of the Alarm
     * @param isInhibited the value that specify is Alarm is inhibited
     * @return the Alarm saved with his events.
     */
    @POST
    @Path("/withEvents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms/withEvents", notes = "Service for adding a new alarm entity alongside with its events",
            httpMethod = "POST", produces = "application/json", consumes = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveAlarmAndEvents(
            Collection<AlarmEvent> alarmEvents,
            @QueryParam("alarmName") String alarmName,
            @QueryParam("isInhibited") boolean isInhibited) {
        try {
            getLog().debug("In Rest Service POST /hyperiot/alarmInformation");
            Alarm alarm = new Alarm();
            alarm.setName(alarmName);
            alarm.setInhibited(isInhibited);
            Alarm response = entityService.saveAlarmAndEvents(alarm, alarmEvents, this.getHyperIoTContext());
            //AlarmInformation response = entityService.saveAlarmAndEvents(getHyperIoTContext(), alarmInformation);
            return Response.ok(response).build();
        } catch (Throwable exc) {
            return this.handleException(exc);
        }
    }

    /**
     * Service finds all available alarm for the given project id
     *
     * @param projectId the id of the HProject from which the Alarm will be retrieved.
     * @return list of all available alarm for the given project id
     */
    @GET
    @Path("/all/projects/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms/all/projects/{projectId}", notes = "Service for finding all alarm entities given a projectId", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllAlarmByProjectId(
            @ApiParam(value = "The project id to get list of alarm from", required = true) @PathParam("projectId") long projectId) {
        getLog().debug("In Rest Service GET /hyperiot/alarms/all/projects/{}", projectId);
        try {
            return Response
                    .ok(this.entityService.findAlarmByProjectId(getHyperIoTContext(), projectId))
                    .build();
        } catch (Throwable exc) {
            return this.handleException(exc);
        }
    }

    /**
     * Service finds all alarm status for the given projects id
     *
     * @param projectIds
     * @return list of all available alarm with the related status
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/alarms/status", notes = "Service for finding all alarm entities given a projectId", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAlarmStatusByProjectId(
            @ApiParam(value = "The project id to get list of alarm from", required = true) @QueryParam("projectId") List<Long> projectIds) {
        getLog().debug("In Rest Service GET /hyperiot/alarms/all/projects/{}", projectIds.toString());
        try {
            long[] projectIdsArr = ArrayUtils.toPrimitive(projectIds.toArray(new Long[0]));
            return Response
                    .ok(this.entityService.getProjectsAlarmStatuses(getHyperIoTContext(), projectIdsArr))
                    .build();
        } catch (Throwable exc) {
            return this.handleException(exc);
        }
    }

}

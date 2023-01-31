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

package  it.acsoftware.hyperiot.alarm.event.service.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.alarm.event.model.AlarmEvent;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi ;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi ;
import it.acsoftware.hyperiot.alarm.event.api.AlarmEventApi;

/**
 * 
 * @author Aristide Cittadino AlarmEvent rest service class. Registered with DOSGi CXF
 * 
 */
@SwaggerDefinition(basePath = "/alarmevents", info = @Info(description = "HyperIoT AlarmEvent API", version = "2.0.0", title = "hyperiot AlarmEvent", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")),securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
		@ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/alarmevents", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = AlarmEventRestApi.class, property = { 
	    "service.exported.interfaces=it.acsoftware.hyperiot.alarm.event.service.rest.AlarmEventRestApi",
		"service.exported.configs=org.apache.cxf.rs","org.apache.cxf.rs.address=/alarmevents",
		"service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
		"service.exported.intents=swagger","service.exported.intents=exceptionmapper"
		 }, immediate = true)
@Path("")
public class AlarmEventRestApi extends HyperIoTBaseEntityRestApi<AlarmEvent>  {
	private AlarmEventApi entityService ;

	/**
	 * Simple service for checking module status
	 * 
	 * @return HyperIoT Role Module work!
	 */
	@GET
	@Path("/module/status")
	@ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
	public Response checkModuleWorking() {
		getLog().debug( "In Rest Service GET /hyperiot/alarmevent/module/status");
		return Response.ok("AlarmEvent Module works!").build();
	}

	/**
	 * @Return the current entityService
	 */
	@Override
	protected HyperIoTBaseEntityApi<AlarmEvent> getEntityService() {
		getLog().debug( "invoking getEntityService, returning: {}" , this.entityService);
		return entityService;
	}

	/**
	 * 
	 * @param entityService: Injecting entityService 
	 */
	@Reference(service = AlarmEventApi.class)
	protected void setEntityService(AlarmEventApi entityService) {
		getLog().debug( "invoking setEntityService, setting: {}" , this.entityService);
		this.entityService = entityService;
	}

	/**
	 * 
	 * @param entityService: Unsetting current entityService
	 */
	protected void unsetEntityService(AlarmEventApi entityService) {
		getLog().debug( "invoking unsetEntityService, setting: {}" , entityService);
		this.entityService = entityService;
	}

	/**
	 * Service finds an existing %- projectSuffixUC 
	 * 
	 * @param id id from which %- projectSuffixUC  object will retrieved
	 * @return  AlarmEvent if found
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/alarmevents/{id}", notes = "Service for finding alarmevent", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAlarmEvent(@PathParam("id") long id) {
		getLog().debug( "In Rest Service GET /hyperiot/alarmevents/{}" , id);
		return this.find(id);
	}

	/**
	 * Service saves a new AlarmEvent
	 * 
	 * @param entity AlarmEvent object to store in database
	 * @return the AlarmEvent saved
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/alarmevents", notes = "Service for adding a new alarmevent entity", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response saveAlarmEvent(
		@ApiParam(value = "AlarmEvent entity which must be saved ", required = true) AlarmEvent entity) {
		getLog().debug( "In Rest Service POST /hyperiot/alarmevents \n Body: {}" , entity);
		return this.save(entity);
	}

	/**
	 * Service updates a AlarmEvent
	 * 
	 * @param entity AlarmEvent object to update in database
	 * @return the AlarmEvent updated
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/alarmevents", notes = "Service for updating a alarmevent entity", httpMethod = "PUT", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Invalid ID supplied") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response updateAlarmEvent(
		@ApiParam(value = "AlarmEvent entity which must be updated ", required = true)AlarmEvent entity) {
		getLog().debug( "In Rest Service PUT /hyperiot/alarmevents \n Body: {}" , entity);
		return this.update(entity);
	}

	/**
	 * Service deletes a AlarmEvent
	 * 
	 * @param id id from which AlarmEvent object will deleted
	 * @return 200 OK if it has been deleted
	 */
	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/alarmevents/{id}", notes = "Service for deleting a alarmevent entity", httpMethod = "DELETE", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response deleteAlarmEvent(
		@ApiParam(value = "The alarmevent id which must be deleted", required = true) @PathParam("id") long id) {
		getLog().debug( "In Rest Service DELETE /hyperiot/alarmevents/{}" , id);
		return this.remove(id);
	}

	/**
	 * Service finds all available alarmevent
	 * 
	 * @return list of all available alarmevent
	 */
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/alarmevents/all", notes = "Service for finding all alarmevent entities", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllAlarmEvent() {
		getLog().debug( "In Rest Service GET /hyperiot/alarmevents/");
		return this.findAll();
	}

	/**
	 * Service finds all available alarmevent
	 * 
	 * @return list of all available alarmevent
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/alarmevents", notes = "Service for finding all alarmevent entities", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllAlarmEventPaginated(@QueryParam("delta") Integer delta,@QueryParam("page") Integer page) {
		getLog().debug( "In Rest Service GET /hyperiot/alarmevents/");
		return this.findAll(delta,page);
	}

	/**
	 * Service finds all available alarm event for the given alarm id
	 * @param alarmId  the id of the HProject from which the Alarm will be retrieved.
	 * @return list of all available alarm event for the given alarm id
	 */
	@GET
	@Path("/all/alarms/{alarmId}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/alarmevents/all/alarms/{alarmId}", notes = "Service for finding all alarm event entities given an alarmId", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllAlarmEventByAlarmId(
			@ApiParam(value = "The alarm id to get list of alarm event from", required = true) @PathParam("alarmId") long alarmId) {
		getLog().debug( "In Rest Service GET /hyperiot/alarmevents/all/alarms/{}",alarmId);
		try {
			return Response
					.ok(this.entityService.findAllEventByAlarmId(getHyperIoTContext(), alarmId))
					.build();
		} catch (Throwable exc){
			return this.handleException(exc);
		}
	}
	
}

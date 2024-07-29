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

package it.acsoftware.hyperiot.widget.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.widget.api.WidgetApi;
import it.acsoftware.hyperiot.widget.model.Widget;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Aristide Cittadino Widget rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/widgets", info = @Info(description = "HyperIoT Widget API", version = "2.0.0", title = "hyperiot Widget", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/widgets", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = WidgetRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.widget.service.rest.WidgetRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/widgets",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"
}, immediate = true)
@Path("")
public class WidgetRestApi extends HyperIoTBaseEntityRestApi<Widget> {
    private WidgetApi entityService;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Role Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug( "In Rest Service GET /hyperiot/widget/module/status");
        return Response.ok("Widget Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<Widget> getEntityService() {
        getLog().debug( "invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = WidgetApi.class)
    protected void setEntityService(WidgetApi entityService) {
        getLog().debug( "invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * @param entityService: Unsetting current entityService
     */
    protected void unsetEntityService(WidgetApi entityService) {
        getLog().debug( "invoking unsetEntityService, setting: {}", entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing %- projectSuffixUC
     *
     * @param id id from which %- projectSuffixUC  object will retrieved
     * @return Widget if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/widgets/{id}", notes = "Service for finding widget", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findWidget(@PathParam("id") long id) {
        getLog().debug( "In Rest Service GET /hyperiot/widgets/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new Widget
     *
     * @param entity Widget object to store in database
     * @return the Widget saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/widgets", notes = "Service for adding a new widget entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveWidget(
            @ApiParam(value = "Widget entity which must be saved ", required = true) Widget entity) {
        getLog().debug( "In Rest Service POST /hyperiot/widgets \n Body: {}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a Widget
     *
     * @param entity Widget object to update in database
     * @return the Widget updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/widgets", notes = "Service for updating a widget entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateWidget(
            @ApiParam(value = "Widget entity which must be updated ", required = true) Widget entity) {
        getLog().debug( "In Rest Service PUT /hyperiot/widgets \n Body: {}", entity);
        return this.update(entity);
    }

    /**
     * Service deletes a Widget
     *
     * @param id id from which Widget object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/widgets/{id}", notes = "Service for deleting a widget entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteWidget(
            @ApiParam(value = "The widget id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug( "In Rest Service DELETE /hyperiot/widgets/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available widget
     *
     * @return list of all available widget
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/widgets/all", notes = "Service for finding all widget entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllWidget() {
        getLog().debug( "In Rest Service GET /hyperiot/widgets/");
        return this.findAll();
    }

    /**
     * Service finds all available widget
     *
     * @return list of all available widget
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/widgets", notes = "Service for finding all widget entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllWidgetPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug( "In Rest Service GET /hyperiot/widgets/");
        return this.findAll(delta, page);
    }

    /**
	 * Service returns widgets divided into categories
	 *
     * @param type typology of the widgets
	 * @return widgets divided into categories
	 */
	@GET
	@Path("/listed/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/widgets/listed/{type}", notes = "Service for finding widgets divided into categories", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllWidgetInCategories(@PathParam("type") String type) {
		getLog().debug( "In Rest Service GET /hyperiot/widgets/listed/{}", type);
		return  Response.ok(this.entityService.getWidgetsInCategories(type)).build();
	}

    @POST
	@Path("/rate/{rate}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/widgets/rate/{rate}", notes = "Service for adding a new widgetRating", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response rateWidget(
			@ApiParam(value = "rating value", required = true) @PathParam("rate") Integer rate,
			@ApiParam(value = "Updated user object", required = true) Widget widget) {
        getLog().debug( "In Rest Service POST /hyperiot/widgets/rate \n:{} {}", rate,  widget);
        try {
            this.entityService.rateWidget(rate,widget,getHyperIoTContext());
            return Response.ok().build();
        } catch (Throwable e){
            return handleException(e);
        }
    }

}

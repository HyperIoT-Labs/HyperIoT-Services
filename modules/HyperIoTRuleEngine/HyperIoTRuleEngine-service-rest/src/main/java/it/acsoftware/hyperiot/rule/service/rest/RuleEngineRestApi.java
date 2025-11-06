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

package it.acsoftware.hyperiot.rule.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.rule.api.RuleEngineApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.operations.RuleOperationDefinition;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Aristide Cittadino Rule rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/rules", info = @Info(description = "HyperIoT Rule API", version = "2.0.0", title = "HyperIoT Rule", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(tags = "Rule Engine", value = "/rules", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HyperIoTRestService.class, immediate = true)
@Path("/rules")
public class RuleEngineRestApi extends HyperIoTBaseEntityRestApi<Rule> implements HyperIoTRestService {
    private RuleEngineApi entityService;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Rule Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    @JsonView(HyperIoTJSONView.Public.class)
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/rules/module/status");
        return Response.ok("RuleEngine Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<Rule> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = RuleEngineApi.class)
    protected void setEntityService(RuleEngineApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing RuleEngine
     *
     * @param id id from which RuleEngine object will retrieved
     * @return Rule if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules/{id}", notes = "Service for finding rules", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findRule(
            @ApiParam(value = "id from which rule object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/rules/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new Rule
     *
     * @param entity Rule object to store in database
     * @return the Rule saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules", notes = "Service for adding a new rule entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveRule(@ApiParam(value = "Rule entity which must be saved ", required = true) Rule entity) {
        getLog().debug("In Rest Service POST /hyperiot/rules \n Body: {}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a Rule
     *
     * @param entity Rule object to update in database
     * @return the Rule updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules", notes = "Service for updating a rule entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateRule(@ApiParam(value = "Rule entity which must be updated ", required = true) Rule entity) {
        getLog().debug("In Rest Service PUT /hyperiot/rules \n Body: {}", entity);
        return this.update(entity);
    }

    /**
     * Service deletes a Rule
     *
     * @param id id from which Rule object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules/{id}", notes = "Service for deleting a rule entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteRule(
            @ApiParam(value = "The rule id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/rules/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available rules
     *
     * @return list of all available rules
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules/all", notes = "Service for finding all rule entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllRule() {
        getLog().debug("In Rest Service GET /hyperiot/rules/all");
        return this.findAll();
    }

    /**
     * Service finds all available rules
     *
     * @return list of all available rules
     */
    @GET
    @Path("/bypacket/{packetId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules/bypacket/{packetId}", notes = "Service for finding all rule entities for a given packet", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllRuleByPacketId(
            @ApiParam(value = "The packet id", required = true) @PathParam("packetId") long packetId
    ) {
        getLog().debug("In Rest Service GET /hyperiot/rules/bypacket/{}", packetId);
        try {
            return Response.ok(this.entityService.findAllRuleByPacketId(getHyperIoTContext(), packetId)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service finds all available rules by project
     *
     * @return list of all available rules by project
     */
    @GET
    @Path("/byproject/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules/byproject/{projectId}", notes = "Service for finding all rule entities for a given project", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllRuleByProjectId(
            @ApiParam(value = "The project id", required = true) @PathParam("projectId") long projectId
    ) {
        getLog().debug("In Rest Service GET /hyperiot/rules/byproject/{}", projectId);
        try {
            return Response.ok(this.entityService.findAllRuleByProjectId(getHyperIoTContext(), projectId)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service finds all available rules
     *
     * @return list of all available rules
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules", notes = "Service for finding all rule entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllRulePaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/rules/");
        return this.findAll(delta, page);
    }

    /**
     * Service finds all available rules
     *
     * @return list of all available rules
     */
    @GET()
    @Path("/actions")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/rules/actions", notes = "Service for finding all rule actions", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllRuleActions(@QueryParam("type") String type) {
        getLog().debug("In Rest Service GET /hyperiot/rules/");
        return Response.ok(this.entityService.findRuleActions(type)).build();
    }

    @GET
    @Path("/operations")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/rules/operations", notes = "Service for finding all rule operations", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllAvailableOperations() {
        return Response.ok(RuleOperationDefinition.getDefinedOperationsDefinitions()).build();
    }
}

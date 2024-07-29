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

package it.acsoftware.hyperiot.hproject.algorithm.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmApi;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmConfig;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithmHBaseResult;
import it.acsoftware.hyperiot.hproject.api.hbase.HProjectHBaseApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/**
 * @author Aristide Cittadino HProjectAlgorithm rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/hprojectalgorithms", info = @Info(description = "HyperIoT HProjectAlgorithm API", version = "2.0.0", title = "hyperiot HProjectAlgorithm", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/hprojectalgorithms", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HProjectAlgorithmRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.hproject.algorithm.service.rest.HProjectAlgorithmRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/hprojectalgorithms",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"
}, immediate = true)
@Path("")
public class HProjectAlgorithmRestApi extends HyperIoTBaseEntityRestApi<HProjectAlgorithm> {
    private HProjectAlgorithmApi entityService;
    private HProjectHBaseApi hbaseApi;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Role Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/hprojectalgorithm/module/status");
        return Response.ok("HProjectAlgorithm Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<HProjectAlgorithm> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = HProjectAlgorithmApi.class)
    protected void setEntityService(HProjectAlgorithmApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    @Reference
    protected void setHbaseApi(HProjectHBaseApi hbaseApi) {
        this.hbaseApi = hbaseApi;
    }

    /**
     * Service finds an existing %- projectSuffixUC
     *
     * @param id id from which %- projectSuffixUC  object will retrieved
     * @return HProjectAlgorithm if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms/{id}", notes = "Service for finding hprojectalgorithm", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findHProjectAlgorithm(@PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hprojectalgorithms/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new HProjectAlgorithm
     *
     * @param entity HProjectAlgorithm object to store in database
     * @return the HProjectAlgorithm saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms", notes = "Service for adding a new hprojectalgorithm entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveHProjectAlgorithm(
            @ApiParam(value = "HProjectAlgorithm entity which must be saved ", required = true) HProjectAlgorithm entity) {
        getLog().debug("In Rest Service POST /hyperiot/hprojectalgorithms \n Body: {}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a HProjectAlgorithm
     *
     * @param entity HProjectAlgorithm object to update in database
     * @return the HProjectAlgorithm updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms", notes = "Service for updating a hprojectalgorithm entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHProjectAlgorithm(
            @ApiParam(value = "HProjectAlgorithm entity which must be updated ", required = true) HProjectAlgorithm entity) {
        getLog().debug("In Rest Service PUT /hyperiot/hprojectalgorithms \n Body: {}", entity);
        return this.update(entity);
    }

    /**
     * Service deletes a HProjectAlgorithm
     *
     * @param id id from which HProjectAlgorithm object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms/{id}", notes = "Service for deleting a hprojectalgorithm entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteHProjectAlgorithm(
            @ApiParam(value = "The hprojectalgorithm id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/hprojectalgorithms/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available hprojectalgorithm
     *
     * @return list of all available hprojectalgorithm
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms/all", notes = "Service for finding all hprojectalgorithm entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHProjectAlgorithm() {
        getLog().debug("In Rest Service GET /hyperiot/hprojectalgorithms/");
        return this.findAll();
    }

    /**
     * Service finds all available hprojectalgorithm
     *
     * @return list of all available hprojectalgorithm
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms", notes = "Service for finding all hprojectalgorithm entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHProjectAlgorithmPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/hprojectalgorithms/");
        return this.findAll(delta, page);
    }

    /**
     * Gets list algorithms which have been defined for a project
     *
     * @param id id of the project which gets the list of algorithms from
     * @return List of algorithms
     */
    @GET
    @Path("/projects/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms/projects/{id}", notes = "Return the list of algorithms which " +
            "have been defined for project with given ID", httpMethod = "GET", produces = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findByHProjectId(
            @ApiParam(value = "ID of the project", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hprojectalgorithms/projects/{}", id);
        try {
            return Response.ok().entity(entityService.findByHProjectId(getHyperIoTContext(), id)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Gets outputs of algorithm which have been defined for a project
     *
     * @param projectId           ID of project
     * @param hProjectAlgorithmId ID of hProjectAlgorithmId
     * @return Response
     */
    @GET
    @Path("/projects/{projectId}/algorithms/{hProjectAlgorithmId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms/projects/{projectId}/algorithms/{hProjectAlgorithmId}",
            notes = "Return outputs of algorithm which have been defined for a project", httpMethod = "GET",
            produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getAlgorithmOutputs(
            @ApiParam(value = "ID of project", required = true) @PathParam("projectId") long projectId,
            @ApiParam(value = "ID of HProjectAlgorithm", required = true) @PathParam("hProjectAlgorithmId") long hProjectAlgorithmId,
            @QueryParam(value = "asRowResult") Boolean asRowResult) {
        getLog().debug("In Rest Service GET /hyperiot/hprojectalgorithms/projects/{}/algorithms/{}",
                projectId, hProjectAlgorithmId);
        try {
            HProjectAlgorithmHBaseResult result = hbaseApi.getAlgorithmOutputs(getHyperIoTContext(), projectId, hProjectAlgorithmId);
            if (asRowResult == null || !asRowResult) {
                return Response.ok().entity(result).build();
            } else {
                //return a simplified and flat version of the result, used for integration purpose
                return Response.ok().entity(result.toRowResultsMap()).build();
            }

        } catch (Throwable e) {
            return handleException(e);
        }
    }


    @POST
    @Path("/{hProjectAlgorithmId}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojectalgorithms/{hProjectAlgorithmId}/config", notes = "Service to update " +
            "configuration of HProjectAlgorithm", httpMethod = "POST", produces = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateBaseConfig(
            @ApiParam(value = "HProjectAlgorithm ID whose update configuration", required = true)
            @PathParam("hProjectAlgorithmId") long hProjectAlgorithmId,
            @ApiParam(value = "Base config", required = true) HProjectAlgorithmConfig config) {
        getLog().debug("In Rest Service GET /hyperiot/hprojectalgorithms/{}/config", hProjectAlgorithmId);
        try {
            HProjectAlgorithm response = entityService.updateConfig(getHyperIoTContext(), hProjectAlgorithmId, config);
            return Response.ok(response).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

}

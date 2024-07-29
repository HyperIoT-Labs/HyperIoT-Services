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

package it.acsoftware.hyperiot.algorithm.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmApi;
import it.acsoftware.hyperiot.algorithm.api.AlgorithmUtil;
import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;


/**
 * @author Aristide Cittadino Algorithm rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/algorithms", info = @Info(description = "HyperIoT Algorithm API", version = "2.0.0", title = "hyperiot Algorithm", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/algorithms", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = AlgorithmRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.algorithm.service.rest.AlgorithmRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/algorithms",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"
}, immediate = true)
@Path("")
public class AlgorithmRestApi extends HyperIoTBaseEntityRestApi<Algorithm> {
    private AlgorithmApi entityService;

    private AlgorithmUtil algorithmUtil;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Role Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/algorithm/module/status");
        return Response.ok("Algorithm Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<Algorithm> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = AlgorithmApi.class)
    protected void setEntityService(AlgorithmApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * @param entityService: Unsetting current entityService
     */
    protected void unsetEntityService(AlgorithmApi entityService) {
        getLog().debug("invoking unsetEntityService, setting: {}", entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing %- projectSuffixUC
     *
     * @param id id from which %- projectSuffixUC  object will retrieved
     * @return Algorithm if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{id}", notes = "Service for finding algorithm", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAlgorithm(@PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/algorithms/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new Algorithm
     *
     * @param entity Algorithm object to store in database
     * @return the Algorithm saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms", notes = "Service for adding a new algorithm entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveAlgorithm(
            @ApiParam(value = "Algorithm entity which must be saved ", required = true) Algorithm entity) {
        getLog().debug("In Rest Service POST /hyperiot/algorithms \n Body: {}", entity);
        String config = entity.getBaseConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        if ((config != null) && (!config.equals(""))) {
            try {
                AlgorithmConfig baseConfig = objectMapper.readValue(config, AlgorithmConfig.class);
                config = algorithmUtil.getBaseConfigString(baseConfig);    // set default values if there are not
                entity.setBaseConfig(config);
            } catch (IOException e) {
                return this.handleException(new HyperIoTRuntimeException(e));
            }
        }
        return this.save(entity);
    }

    /**
     * Service updates a Algorithm
     *
     * @param entity Algorithm object to update in database
     * @return the Algorithm updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms", notes = "Service for updating a algorithm entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateAlgorithm(
            @ApiParam(value = "Algorithm entity which must be updated ", required = true) Algorithm entity) {
        getLog().debug("In Rest Service PUT /hyperiot/algorithms \n Body: {}", entity);
        String config = entity.getBaseConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        if ((config != null) && (!config.equals(""))) {
            try {
                AlgorithmConfig baseConfig = objectMapper.readValue(config, AlgorithmConfig.class);
                config = algorithmUtil.getBaseConfigString(baseConfig);    // set default values if there are not
                entity.setBaseConfig(config);
            } catch (IOException e) {
                return this.handleException(new HyperIoTRuntimeException(e));
            }
        }
        return this.update(entity);
    }

    /**
     * Service deletes a Algorithm
     *
     * @param id id from which Algorithm object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{id}", notes = "Service for deleting a algorithm entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteAlgorithm(
            @ApiParam(value = "The algorithm id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/algorithms/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available algorithm
     *
     * @return list of all available algorithm
     */
    @GET
    @Path("/type/{algorithmType}/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{algorithmType}/all", notes = "Service for finding all algorithm entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllAlgorithm(@ApiParam(value = "The algorithm type ", required = true) @PathParam(value = "algorithmType") AlgorithmType algorithmType) {
        getLog().debug("In Rest Service GET /hyperiot/algorithms/algorithmType/all");
        HashMap<String, Object> filter = new HashMap<>();
        filter.put("type", algorithmType);
        return this.findAll(filter);
    }

    /**
     * Service finds all available algorithm
     *
     * @return list of all available algorithm
     */
    @GET
    @Path("/type/{algorithmType}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/{type}/algorithms", notes = "Service for finding all algorithm entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllAlgorithmPaginated(@ApiParam(value = "The algorithm type ", required = true) @PathParam(value = "algorithmType") AlgorithmType algorithmType, @QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/algorithms/{algorithmType}");
        HashMap<String, Object> filter = new HashMap<>();
        filter.put("type", algorithmType);
        return this.findAll(delta, page, filter);
    }

    @POST
    @Path("/{id}/ioFields")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{id}/ioFields", notes = "Service for adding IO field", httpMethod = "POST", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response addIOField(
            @ApiParam(value = "The algorithm which must be updated", required = true) @PathParam("id") long id,
            @ApiParam(value = "The IO field which must be added", required = true) AlgorithmIOField ioField) {
        getLog().debug("In Rest Service POST /hyperiot/algorithms/{}/ioFields", id);
        try {
            Algorithm algorithm = this.entityService.addIOField(this.getHyperIoTContext(), id, ioField);
            return Response.ok(algorithm).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    @DELETE
    @Path("/{id}/ioFields/{fieldType}/{ioFieldId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{id}/ioFields/{fieldType}/{ioFieldId}",
            notes = "Service for deleting IO field", httpMethod = "DELETE", produces = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteIOField(
            @ApiParam(value = "The algorithm which must be updated", required = true) @PathParam("id") long id,
            @ApiParam(value = "IO field type", required = true) @PathParam("fieldType") AlgorithmFieldType fieldType,
            @ApiParam(value = "The IO field which must be deleted", required = true) @PathParam("ioFieldId") long ioFieldId) {
        getLog().debug("In Rest Service DELETE /hyperiot/algorithms/{}/ioFields/{}/{}", id, fieldType, ioFieldId);
        try {
            Algorithm algorithm = this.entityService.deleteIOField(this.getHyperIoTContext(), id, fieldType, ioFieldId);
            return Response.ok(algorithm).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    @GET
    @Path("/{algorithmId}/baseConfig")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{algorithmId}/baseConfig", notes = "Service for getting base " +
            "configuration of algorithm", httpMethod = "GET", produces = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getBaseConfig(
            @ApiParam(value = "Algorithm ID", required = true) @PathParam("algorithmId") long algorithmId) {
        getLog().debug("In Rest Service GET /hyperiot/algorithms/{}/baseConfig", algorithmId);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return Response.ok(objectMapper.readValue(entityService.getBaseConfig(getHyperIoTContext(), algorithmId),
                    AlgorithmConfig.class)).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    @POST
    @Path("/{algorithmId}/baseConfig")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{algorithmId}/baseConfig", notes = "Service for updating base " +
            "configuration of algorithm", httpMethod = "POST", produces = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateBaseConfig(
            @ApiParam(value = "Algorithm ID which updates base configuration for", required = true) @PathParam("algorithmId") long algorithmId,
            @ApiParam(value = "Base config", required = true) AlgorithmConfig baseConfig) {
        getLog().debug("In Rest Service GET /hyperiot/algorithms/{}/baseConfig", algorithmId);
        try {
            Algorithm response = entityService.updateBaseConfig(getHyperIoTContext(), algorithmId, baseConfig);
            return Response.ok(response).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    @PUT
    @Path("/{algorithmId}/file/{mainClassname}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{algorithmId}/file/{mainClassName}", notes = "Service for updating algorithm source file",
            httpMethod = "PUT", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateAlgorithmFile(
            @ApiParam(value = "Algorithm ID which updates source file for", required = true) @PathParam("algorithmId") long algorithmId,
            @ApiParam(value = "Classname containing main method", required = true) @PathParam("mainClassname") String mainClassname,
            @ApiParam(value = "Algorithm source file ", required = true) File algorithmFile) {
        getLog().debug("In Rest Service PUT /hyperiot/algorithms/{}/file", algorithmId);
        try {
            Algorithm response = entityService.updateAlgorithmFile(getHyperIoTContext(), algorithmId, mainClassname, algorithmFile);
            return Response.ok(response).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    @PUT
    @Path("/{id}/ioFields")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/algorithms/{id}/ioFields", notes = "Service for updating IO field", httpMethod = "PUT", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateIOField(
            @ApiParam(value = "The algorithm which must be updated", required = true) @PathParam("id") long id,
            @ApiParam(value = "The IO field which must be updated", required = true) AlgorithmIOField ioField) {
        getLog().debug("In Rest Service PUT /hyperiot/algorithms/{}/ioFields", id);
        try {
            Algorithm algorithm = this.entityService.updateIOField(this.getHyperIoTContext(), id, ioField);
            return Response.ok(algorithm).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    @Reference
    protected void setAlgorithmUtil(AlgorithmUtil algorithmUtil) {
        this.algorithmUtil = algorithmUtil;
    }

}

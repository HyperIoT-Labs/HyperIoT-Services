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
package it.acsoftware.hyperiot.stormmanager.service.rest;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@SwaggerDefinition(basePath = "/storm", info = @Info(description = "HyperIoT StormManager API", version = "1.0.0", title = "HyperIoT Storm Manager", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(tags = "Storm", value = "/storm", produces = "application/json")
@Component(service = HyperIoTRestService.class, immediate = true)
@Path("/storm")
public class StormManagerRestApi extends HyperIoTBaseRestApi implements HyperIoTRestService {
    private StormManagerApi stormServiceApi;

    /**
     * @return the current stormServiceApi
     */
    public StormManagerApi getStormServiceApi() {
        getLog().debug( "invoking getStormServiceApi, returning: {}", this.stormServiceApi);
        return stormServiceApi;
    }

    /**
     * @param stormServiceApi Injecting stormServiceApi
     */
    @Reference(service = StormManagerApi.class)
    protected void setStormServiceApi(StormManagerApi stormServiceApi) {
        getLog().debug( "invoking setStormServiceApi, setting: {}", this.stormServiceApi);
        this.stormServiceApi = stormServiceApi;
    }

    @GET
    @Path("/module/status")
    @ApiOperation(value = "/hyperiot/storm/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug( "In Rest Service GET /hyperiot/storm/module/status: ");
        return Response.ok("HyperIoT StormManager Module works!").build();
    }

    /**
     * Gets topology status by a given project ID.
     *
     * @param projectId ID of the project.
     * @return Response object wrapping TopologyStatus if provided topology exists
     */
    @GET
    @Path("/topology/{projectId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/storm/topology/{projectId}/status", notes = "Gets topology status", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Topology does not exists")
    })
    public Response getTopology(
            @ApiParam(value = "ID of the project", required = true)
            @PathParam("projectId")
                    long projectId
    ) {
        getLog().debug( "In REST Service GET /hyperiot/storm/topology/{}", projectId);
        try {
            TopologyInfo info = stormServiceApi.getTopologyStatus(this.getHyperIoTContext(), projectId);
            return Response.ok(info).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Activates a topology by project ID.
     *
     * @param projectId ID of the project.
     * @return Response object
     */
    @GET
    @Path("/topology/{projectId}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/storm/topology/{projectId}/activate", notes = "Activates a topology", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Topology does not exists")
    })
    public Response activateTopology(
            @ApiParam(value = "ID of the project", required = true)
            @PathParam("projectId")
                    long projectId
    ) {
        getLog().debug( "In REST Service GET /hyperiot/storm/topology/{}", projectId);
        try {
            stormServiceApi.activateTopology(this.getHyperIoTContext(), projectId);
            return Response.ok().build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Deactivates a topology by project ID.
     *
     * @param projectId ID of the project.
     * @return Response object
     */
    @GET
    @Path("/topology/{projectId}/deactivate")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/storm/topology/{projectId}/deactivate", notes = "Deactivates a topology", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Topology does not exists")
    })
    public Response deactivateTopology(
            @ApiParam(value = "ID of the project", required = true)
            @PathParam("projectId")
                    long projectId
    ) {
        getLog().debug( "In REST Service GET /hyperiot/storm/topology/{}", projectId);
        try {
            stormServiceApi.deactivateTopology(this.getHyperIoTContext(), projectId);
            return Response.ok().build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Kills a topology by project ID.
     *
     * @param projectId ID of the project.
     * @return Response object
     */
    @GET
    @Path("/topology/{projectId}/kill")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/storm/topology/{topology_name}/kill", notes = "Kills a topology", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Topology does not exists")
    })
    public Response killTopology(
            @ApiParam(value = "ID of the project", required = true)
            @PathParam("projectId")
                    long projectId
    ) {
        getLog().debug( "In REST Service GET /hyperiot/storm/topology/{}", projectId);
        try {
            stormServiceApi.killTopology(this.getHyperIoTContext(), projectId);
            return Response.ok().build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }


    /**
     * Generates and submits a project topology.
     *
     * @param projectId The project id.
     * @return
     */
    @GET
    @Path("/topology/{project_id}/submit")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/storm/topology/{project_id}/submit", notes = "Generates and submit a project topology", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Project does not exists")
    })
    public Response submitProjectTopology(
            @ApiParam(value = "Project id", required = true)
            @PathParam("project_id")
                    long projectId
    ) {
        getLog().debug( "In REST Service GET /hyperiot/storm/topology/submit/{}", projectId);
        try {
            stormServiceApi.submitProjectTopology(getHyperIoTContext(), projectId);
            return Response.ok().build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

}

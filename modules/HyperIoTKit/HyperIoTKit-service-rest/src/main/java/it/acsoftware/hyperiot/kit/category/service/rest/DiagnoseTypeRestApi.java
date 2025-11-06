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

package it.acsoftware.hyperiot.kit.category.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.kit.category.api.DiagnoseTypeApi;
import it.acsoftware.hyperiot.kit.category.model.DiagnoseType;
import it.acsoftware.hyperiot.kit.model.Kit;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@SwaggerDefinition(basePath = "/diagnose/type", info = @Info(description = "HyperIoT DiagnoseType API", version = "2.0.0", title = "HyperIoT DiagnoseType", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")),securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER)}))
@Api(tags = "Diagnose Type", value = "/diagnose/type", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HyperIoTRestService.class, immediate = true)
@Path("")
public class DiagnoseTypeRestApi extends HyperIoTBaseEntityRestApi<DiagnoseType> implements HyperIoTRestService {

    private DiagnoseTypeApi entityService ;

    @Override
    protected HyperIoTBaseEntityApi<DiagnoseType> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}" , this.entityService);
        return entityService;
    }

    @Reference(service = DiagnoseTypeApi.class)
    protected void setEntityService(DiagnoseTypeApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}" , this.entityService);
        this.entityService = entityService;
    }

    /**
     *
     * @param entityService: Unsetting current entityService
     */
    protected void unsetEntityService(DiagnoseTypeApi entityService) {
        getLog().debug("invoking unsetEntityService, setting: {}" , entityService);
        this.entityService = entityService;
    }

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT DiagnoseType module works
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/diagnosetypes/module/status");
        return Response.ok("HyperIoT DiagnoseType module works").build();
    }

    /**
     * Service finds an existing DiagnoseType
     *
     * @param id id from which DiagnoseType object will retrieved
     * @return  DiagnoseType if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/diagnose/type/{id}", notes = "Service for finding DiagnoseType", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findDiagnoseType(@PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/diagnose/type/{}" , id);
        return this.find(id);
    }

    /**
     * Service saves a new DiagnoseType
     *
     * @param entity DiagnoseType object to store in database
     * @return the DiagnoseType saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/diagnose/type", notes = "Service for adding a new DiagnoseType entity", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveDiagnoseType(
            @ApiParam(value = "DiagnoseType entity which must be saved ", required = true) DiagnoseType entity) {
        getLog().debug("In Rest Service POST /hyperiot/diagnose/type \n Body: {}" , entity);
        return this.save(entity);
    }

    /**
     * Service deletes an DiagnoseType
     *
     * @param id id from which DiagnoseType object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/diagnose/type/{id}", notes = "Service for deleting an DiagnoseType entity", httpMethod = "DELETE", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Entity not found") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteDiagnoseType(
            @ApiParam(value = "The DiagnoseType id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/diagnose/type/{}" , id);
        return this.remove(id);
    }

    /**
     * Service add a kit to DiagnoseType's category
     *
     * @param diagnoseTypeId from which DiagnoseType object will retrieved
     * @param kitId from which Kit object will retrieved
     * @return the DiagnoseType updated
     */
    @POST
    @Path("/{diagnoseTypeId}/kits/{kitId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/diagnose/type/{diagnoseTypeId}/kits/{kitId}", notes = "Service for adding a kit on DiagnoseType's category", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response addKitToDiagnoseTypeCategory(
            @PathParam("diagnoseTypeId") long diagnoseTypeId ,
            @PathParam("kitId") long kitId) {
        getLog().debug("In Rest Service POST /hyperiot/diagnose/type/{}/kits/{} " , diagnoseTypeId,kitId);
        try {
            DiagnoseType diagnoseType = entityService.addKitToDiagnoseTypeCategory(getHyperIoTContext(),diagnoseTypeId,kitId);
            return Response.ok(diagnoseType).build();
        }catch (Throwable exc){
            return handleException(exc);
        }
    }

    /**
     * Service delete a kit from DiagnoseType
     *
     * @param diagnoseTypeId from which DiagnoseType object will retrieved
     * @param kitId from which Kit object will be deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{diagnoseTypeId}/kits/{kitId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/diagnose/type/{diagnoseTypeId}/kits/{kitId}", notes = "Service for deleting a building from ALE's category", httpMethod = "DELETE", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteKitFromDiagnoseTypeCategory(
            @PathParam("diagnoseTypeId") long diagnoseTypeId ,
            @PathParam("kitId") long kitId) {
        getLog().debug("In Rest Service DELETE /hyperiot/diagnose/type/{}/kits/{} " , diagnoseTypeId,kitId);
        try {
            entityService.removeKitFromDiagnoseTypeCategory(getHyperIoTContext(),diagnoseTypeId,kitId);
            return Response.ok().build();
        }catch (Throwable exc){
            return handleException(exc);
        }
    }

    /**
     * Service find Kits relative to a DiagnoseType
     *
     * @param id of DiagnoseType from which Kits will retrieved
     * @return DiagnoseType's Kit list
     */
    @GET
    @Path("/{id}/kits")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/diagnose/types/{id}/kits", notes = "Service for find Kits relative to a DiagnoseType ", httpMethod = "GET", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getKitsFromDiagnoseType(
            @PathParam("id") long id ) {
        getLog().debug("In Rest Service GET /hyperiot/diagnose/type/{}/kits " , id);
        try {
            Collection<Kit> diagnoseTypeKits = entityService.getKitByDiagnoseTypeCategory(getHyperIoTContext(),id);
            return Response.ok(diagnoseTypeKits).build();
        }catch (Throwable exc){
            return handleException(exc);
        }
    }

    /**
     * Service find DiagnoseType associated to a Kit
     *
     * @param kitId of the Kit from which DiagnoseType will be retrieved.
     * @return The DiagnoseTypes associated to the kit
     */
    @GET
    @Path("/kits/{kitId}/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/diagnose/type/kits/{kitId}/all", notes = "Service find DiagnoseType associated to a Kit", httpMethod = "GET", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error") })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getDiagnoseTypeFromKit(
            @PathParam("kitId") long kitId ) {
        getLog().debug("In Rest Service GET /hyperiot/diagnose/type/kits/{}/all " , kitId);
        try {
            Collection<DiagnoseType> kitDiagnoseTypes = entityService.getDiagnoseTypeByKit(getHyperIoTContext(),kitId);
            return Response.ok(kitDiagnoseTypes).build();
        }catch (Throwable exc){
            return handleException(exc);
        }
    }

}

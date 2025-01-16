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

package it.acsoftware.hyperiot.hdevice.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Aristide Cittadino HDevice rest service class. Registered with DOSGi
 * CXF
 */
@SwaggerDefinition(basePath = "/hdevices", info = @Info(description = "HyperIoT HDevice API", version = "2.0.0", title = "HyperIoT HDevice", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(tags = "HDevices", value = "/hdevices", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HyperIoTRestService.class, immediate = true)
@Path("/hdevices")
public class HDeviceRestApi extends HyperIoTBaseEntityRestApi<HDevice> implements HyperIoTRestService {
    private HDeviceApi entityService;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT HDevice Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/hdevice/module/status");
        return Response.ok("HDevice Module works!").build();
    }

    /**
     * @return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<HDevice> getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = HDeviceApi.class)
    protected void setEntityService(HDeviceApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing HDevice
     *
     * @param id id from which HDevice object will retrieved
     * @return HDevice if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices/{id}", notes = "Service for finding hdevice", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findHDevice(
            @ApiParam(value = "id from which hdevice object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hdevices/{}", id);
        return this.find(id);
    }

    /**
     * Service finds an existing HDevice with extended view details (packet list added to the response)
     *
     * @param id id from which HDevice object will retrieved
     * @return HDevice if found
     */
    @GET
    @Path("/{id}/extended")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices/{id}/extended", notes = "Service for finding hdevice", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Extended.class)
    public Response findHDeviceExtended(
            @ApiParam(value = "id from which hdevice object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hdevices/{}/extended", id);
        return this.find(id);
    }

    /**
     * Service saves a new HDevice
     *
     * @param entity HDevice object to store in database
     * @return the HDevice saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices", notes = "Service for adding a new hdevice entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveHDevice(
            @ApiParam(value = "HDevice entity which must be saved ", required = true) HDevice entity) {
        getLog().debug("In Rest Service POST /hyperiot/hdevices \n Body: {}", entity);
        entity.setAdmin(false);
        return this.save(entity);
    }

    /**
     * Service updates a HDevice
     *
     * @param entity HDevice object to update in database
     * @return the HDevice updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices", notes = "Service for updating a hdevice entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHDevice(
            @ApiParam(value = "HDevice entity which must be updated ", required = true) HDevice entity) {
        getLog().debug("In Rest Service PUT /hyperiot/hdevices \n Body: {}", entity);
        entity.setAdmin(false);
        return this.update(entity);
    }

    /**
     * @param deviceId        device Id
     * @param oldPassword     old password
     * @param newPassword     new password
     * @param passwordConfirm password confirm
     * @return Response
     */
    @PUT
    @Path("/password")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/x-www-form-urlencoded")
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices/password", notes = "Service for updating a hdevice entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHDevicePassword(
            @ApiParam(value = "HDevice id which must be updated ", required = true) @FormParam("deviceId") long deviceId,
            @ApiParam(value = "Old HDevice Password", required = true) @FormParam("oldPassword") String oldPassword,
            @ApiParam(value = "New HDevice Password", required = true) @FormParam("newPassword") String newPassword,
            @ApiParam(value = "New HDevice Password confirm", required = true) @FormParam("passwordConfirm") String passwordConfirm) {
        getLog().debug("In Rest Service PUT /hyperiot/hdevices/password \n Body: {}", deviceId);
        try {
            return Response.ok(this.entityService.changePassword(this.getHyperIoTContext(), deviceId, oldPassword,
                    newPassword, passwordConfirm)).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Service deletes a HDevice
     *
     * @param id id from which HDevice object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices/{id}", notes = "Service for deleting a hdevice entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteHDevice(
            @ApiParam(value = "The hdevice id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/hdevices/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available HDevice
     *
     * @return list of all available HDevice for the given project id
     */
    @GET
    @Path("/all/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices/all", notes = "Service for finding all hdevice entities for a given project id", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHDeviceByProjectId(@ApiParam(value = "The project id to get list of devices from", required = true) @PathParam("projectId") long projectId) {
        getLog().debug("In Rest Service GET /hyperiot/hdevices/");
        try {
            return Response.ok(this.entityService.getProjectDevicesList(getHyperIoTContext(), projectId)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service finds all available HDevice
     *
     * @return list of all available HDevice for the given project id
     */
    @GET
    @Path("/all/{projectId}/extended")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/all/{projectId}/extended", notes = "Service for finding all hdevice entities for a given project id", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Extended.class)
    public Response findAllHDeviceByProjectIdExtended(@ApiParam(value = "The project id to get list of devices from", required = true) @PathParam("projectId") long projectId) {
        getLog().debug("In Rest Service GET /hyperiot/hdevices/");
        return this.findAllHDeviceByProjectId(projectId);
    }

    /**
     * Service finds all available HDevice
     *
     * @return list of all available HDevice
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices/all", notes = "Service for finding all hdevice entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHDevice() {
        getLog().debug("In Rest Service GET /hyperiot/hdevices/");
        return this.findAll();
    }

    /**
     * Service finds all available HDevice
     *
     * @return list of all available HDevice
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hdevices", notes = "Service for finding all hdevice entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHDevicePaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/hdevices/");
        return this.findAll(delta, page);
    }


    /**
     * Reset HDevice password request
     *
     * @param hDeviceId
     * @return User saved
     */
    @PUT
    @Path("/{hdeviceId}/resetPasswordRequest")
    @LoggedIn
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/hdevices/{hdeviceId}/resetPasswordRequest", notes = "Reset HDevice Password", httpMethod = "PUT", produces = "application/json", consumes = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 404, message = "Entity not found"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response resetPasswordRequest(
            @ApiParam(value = "Id of the device", required = true) @PathParam("hdeviceId") long hDeviceId) {
        getLog().debug("In Rest Service PUT /hyperiot/hdevice/{}/resetPasswordRequest", hDeviceId);
        try {
            this.entityService.requestDevicePasswordReset(this.getHyperIoTContext(), hDeviceId);
            return Response.ok().build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Reset HDevicePassword password
     *
     * @param hDeviceId         device
     * @param passwordResetCode
     * @param newPassword
     * @param passwordConfirm
     * @return 200 ok if operation is succesful
     */
    @PUT
    @Path("/resetPassword")
    @LoggedIn
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/hdevices/resetPassword", notes = "Change User Password", httpMethod = "PUT", produces = "application/x-www-form-urlencoded", consumes = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 404, message = "Entity not found"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response resetPassword(
            @FormParam("hdeviceId") long hDeviceId,
            @FormParam("passwordResetCode") String passwordResetCode,
            @FormParam("newPassword") String newPassword,
            @FormParam("passwordConfirm") String passwordConfirm) {
        getLog().debug("In Rest Service PUT /hyperiot/hdevices/resetPassword  . HDeviceId : {} ", hDeviceId);
        try {
            this.entityService.resetHDevicePassword(this.getHyperIoTContext(), hDeviceId, passwordResetCode, newPassword, passwordConfirm);
            return Response.ok().build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }


}

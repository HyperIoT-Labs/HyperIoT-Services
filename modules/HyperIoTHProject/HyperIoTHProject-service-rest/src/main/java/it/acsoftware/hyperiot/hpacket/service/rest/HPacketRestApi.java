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

package it.acsoftware.hyperiot.hpacket.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQuery;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.hdevice.api.HDeviceApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketFieldSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketType;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * @author Aristide Cittadino HPacket rest service class. Registered with DOSGi
 * CXF
 */
@SwaggerDefinition(basePath = "/hpackets", info = @Info(description = "HyperIoT HPacket API", version = "2.0.0", title = "HyperIoT HPacket", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/hpackets", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HPacketRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/hpackets",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"}, immediate = true)
@Path("")
public class HPacketRestApi extends HyperIoTBaseEntityRestApi<HPacket> {
    private HPacketApi entityService;

    private HDeviceApi hDeviceApi;

    private HPacketFieldSystemApi hPacketFieldSystemApi;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT HPacket Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    @JsonView(HyperIoTJSONView.Public.class)
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/hpacket/module/status");
        return Response.ok("HPacket Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HPacketApi getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = HPacketApi.class)
    protected void setEntityService(HPacketApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    @Reference(service = HPacketFieldSystemApi.class)
    public void sethPacketFieldSystemApi(HPacketFieldSystemApi hPacketFieldSystemApi) {
        this.hPacketFieldSystemApi = hPacketFieldSystemApi;
    }

    protected HDeviceApi gethDeviceApi() {
        getLog().debug("invoking gethDeviceApi, returning: {}", this.hDeviceApi);
        return hDeviceApi;
    }

    /**
     * @param hDeviceApi: Injecting HDeviceApi
     */
    @Reference(service = HDeviceApi.class)
    protected void sethDeviceApi(HDeviceApi hDeviceApi) {
        getLog().debug("invoking sethDeviceApi, setting: {}", this.hDeviceApi);
        this.hDeviceApi = hDeviceApi;
    }

    /**
     * Service finds an existing HPacket
     *
     * @param id id from which HPacket object will retrieved
     * @return HPacket if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/{id}", notes = "Service for finding hpacket", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findHPacket(
            @ApiParam(value = "id from which hpacket object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/{}", id);
        return this.find(id);
    }

    /**
     * Service saves a new HPacket
     *
     * @param entity HPacket object to store in database
     * @return the HPacket saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets", notes = "Service for adding a new hpacket entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveHPacket(
            @ApiParam(value = "HPacket entity which must be saved ", required = true) HPacket entity) {
        getLog().debug("In Rest Service POST /hyperiot/hpackets \n Body: {}", entity);
        HDevice device;
        try {
            if (entity.getDevice() == null) {
                throw new HyperIoTEntityNotFound();
            }
            device = this.hDeviceApi.find(entity.getDevice().getId(), this.getHyperIoTContext());
        } catch (Throwable t) {
            return this.handleException(t);
        }
        entity.setDevice(device);
        return this.save(entity);
    }

    /**
     * Service updates a HPacket
     *
     * @param entity HPacket object to update in database
     * @return the HPacket updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets", notes = "Service for updating a hpacket entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHPacket(
            @ApiParam(value = "HPacket entity which must be updated ", required = true) HPacket entity) {
        getLog().debug("In Rest Service PUT /hyperiot/hpackets \n Body: {}", entity);
        HDevice device;
        try {
            if (entity.getDevice() == null) {
                throw new HyperIoTEntityNotFound();
            }
            device = this.hDeviceApi.find(entity.getDevice().getId(), this.getHyperIoTContext());
            entity.setDevice(device);
            HPacket dbPacket = this.entityService.find(entity.getId(), this.getHyperIoTContext());
            entity.setFields(dbPacket.getFields());
        } catch (Throwable t) {
            return this.handleException(t);
        }
        return this.update(entity);
    }

    /**
     * Service deletes a HPacket
     *
     * @param id id from which HPacket object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/{id}", notes = "Service for deleting a hpacket entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteHPacket(
            @ApiParam(value = "The hpacket id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/hpackets/" + id);
        return this.remove(id);
    }

    /**
     * Service finds all available hpacket
     *
     * @return list of all available hpacket
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/all", notes = "Service for finding all hpacket entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHPacket() {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/");
        return this.findAll();
    }

    /**
     * Service finds all available hpacket
     *
     * @return list of all available hpacket
     */
    @GET
    @Path("/all/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/all/{id}", notes = "Service for finding all hpacket entities of a project", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHPacketByProjectId(@ApiParam(value = "The project id", required = true) @PathParam("id") long projectId) {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/all/{}", projectId);
        try {
            return Response.ok()
                    .entity(this.entityService.getProjectPacketsList(getHyperIoTContext(), projectId))
                    .build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Service finds all available hpacket
     *
     * @return list of all available hpacket
     */
    @GET
    @Path("/all/{id}/types")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/all/{id}/types", notes = "Service for finding all hpacket entities of a project with specific packet types passed as comma separated list", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHPacketByProjectIdAndType(@ApiParam(value = "The project id", required = true) @PathParam("id") long projectId,
                                                     @ApiParam(value = "Packet type", required = false) @QueryParam("types") String commaSeparatedListTypes) {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/all/{}/{}", new Object[]{projectId, commaSeparatedListTypes});
        try {
            List<HPacketType> types = new ArrayList<>();
            if (commaSeparatedListTypes != null && commaSeparatedListTypes.length() > 0) {
                String[] typesStrs = commaSeparatedListTypes.split(",");
                Arrays.stream(typesStrs).forEach(elem -> {
                    try {
                        HPacketType type = HPacketType.valueOf(elem);
                        types.add(type);
                    } catch (Exception e) {
                        getLog().debug("Invalid packet types in {}", typesStrs);
                    }
                });
            }
            return Response.ok()
                    .entity(this.entityService.getProjectPacketsListByType(getHyperIoTContext(), projectId, types))
                    .build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Service finds all available hpacket
     *
     * @return list of all available hpacket
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets", notes = "Service for finding all hpacket entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHPacketPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/");
        return this.findAll(delta, page);
    }


    /**
     * Gets the list of a device packets
     *
     * @param id id of the device from which to get the list of packets
     * @return List of device packets
     */
    @GET
    @Path("/devices/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/devices/{id}", notes = "Return the list of device packets", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getHDevicePacketList(
            @ApiParam(value = "id of the device", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/devices/{id}/{}", id);
        try {
            return Response.ok()
                    .entity(entityService.getPacketsList(getHyperIoTContext(), id))
                    .build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service deletes a HPacket
     *
     * @param packetId id from which HPacket object will deleted
     * @param field    to update
     * @return 200 OK if the field has been added
     */
    @POST
    @Path("/{id}/fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/{id}/fields", notes = "Service for deleting a hpacket entity", httpMethod = "POST", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response addHPacketField(
            @ApiParam(value = "The hpacket which must be updated", required = true) @PathParam("id") long packetId,
            @ApiParam(value = "The hpacket field which must be added", required = true) HPacketField field) {
        getLog().debug("In Rest Service POST /hyperiot/hpackets/{}/fields", packetId);
        HPacket packet;
        try {
            packet = this.getEntityService().find(packetId, this.getHyperIoTContext());
            field.setPacket(packet);
            field = this.entityService.addHPacketField(this.getHyperIoTContext(), field);
            return Response.ok(field).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }


    /**
     * Service returns field details
     *
     * @param fieldIds field ids to return
     * @return 200 OK if it has been found
     */
    @GET
    @Path("/fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/fields", notes = "Service for retrieving hpacket fields entity", httpMethod = "GET", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getHPacketField(
            @ApiParam(value = "list of packet fields the user wants to receive", required = true) @QueryParam("fieldId") List<Long> fieldIds) {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/fields/", fieldIds);
        try {
            if (fieldIds.size() > 0) {
                Iterator<Long> idsIt = fieldIds.iterator();
                HyperIoTQuery findById = HyperIoTQueryBuilder.newQuery().equals("id", idsIt.next());
                while (idsIt.hasNext()) {
                    findById = findById.or(HyperIoTQueryBuilder.newQuery().equals("id", idsIt.next()));
                }
                Collection<HPacketField> fields = hPacketFieldSystemApi.findAll(findById, this.getHyperIoTContext());
                return Response.ok(fields).build();
            }
            return Response.ok().build();
        } catch (Throwable t) {
            return this.handleException(t);
        }

    }

    /**
     * Service deletes a HPacket
     *
     * @param fieldId field id to remove
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/fields/{fieldId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/fields/{fieldId}", notes = "Service for deleting a hpacket field entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteHPacketField(
            @ApiParam(value = "The hpacket field id which must be deleted", required = true) @PathParam("fieldId") long fieldId) {
        getLog().debug("In Rest Service DELETE /hyperiot/hpackets/fields/{}", fieldId);
        try {
            HPacket packet = this.entityService.findHPacketByHpacketFieldId(getHyperIoTContext(), fieldId);
            this.getEntityService().removeHPacketField(this.getHyperIoTContext(), fieldId, packet.getId());
            return Response.ok().build();
        } catch (Throwable t) {
            return this.handleException(t);
        }

    }

    /**
     * Service deletes a HPacket
     *
     * @param packetId id from which HPacket object will deleted
     * @param field    HPacketField to update
     * @return 200 OK if it has been deleted
     */
    @PUT
    @Path("/{id}/fields")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/{id}/fields", notes = "Service for updating a hpacket field entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHPacketField(
            @ApiParam(value = "The hpacket id which must be updated", required = true) @PathParam("id") long packetId,
            @ApiParam(value = "The hpacket field which must be updated", required = true) HPacketField field) {
        getLog().debug("In Rest Service PUT /hyperiot/hpackets/{}/fields", packetId);
        HPacket packet;
        try {
            packet = this.getEntityService().find(packetId, this.getHyperIoTContext());
        } catch (Throwable t) {
            return this.handleException(t);
        }
        Iterator<HPacketField> fieldsIterator = packet.getFields().iterator();
        boolean found = false;
        field.setPacket(packet);
        while (fieldsIterator.hasNext() && !found) {
            HPacketField f = fieldsIterator.next();
            if (f.getId() == field.getId()) {
                field.setParentField(f.getParentField());
                field.setInnerFields(f.getInnerFields());
                found = true;
            }
        }
        if (found) {
            try {
                field = this.entityService.updateHPacketField(this.getHyperIoTContext(), field);
                return Response.ok(field).build();
            } catch (Throwable t) {
                return this.handleException(t);
            }
        }
        return this.handleException(new HyperIoTEntityNotFound());
    }

    /**
     * Gets the list of a fields with no parent
     *
     * @param id id of the packet
     * @return List of packet fields
     */
    @GET
    @Path("/treefields/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hpackets/treefields/{id}", notes = "Return the list of packet fields with no parent", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findTreeFields(
            @ApiParam(value = "id of the packet", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hpackets/treefields/{}", id);
        try {
            Collection<HPacketField> filteredList = this.getEntityService().getHPacketFieldsTree(this.getHyperIoTContext(), id);
            return Response.ok(filteredList).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }
}

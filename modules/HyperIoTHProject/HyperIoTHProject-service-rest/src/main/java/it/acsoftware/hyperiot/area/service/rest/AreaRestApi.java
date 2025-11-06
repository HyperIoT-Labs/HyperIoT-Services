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

package it.acsoftware.hyperiot.area.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.area.api.AreaApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.model.AreaViewType;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.base.util.HyperIoTErrorConstants;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Aristide Cittadino Area rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/areas", info = @Info(description = "HyperIoT Area API", version = "2.0.0", title = "HyperIoT Area", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(tags = "Area ", value = "/areas", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HyperIoTRestService.class, immediate = true)
@Path("/areas")
public class AreaRestApi extends HyperIoTBaseEntityRestApi<Area> implements HyperIoTRestService {
    private static Logger log = LoggerFactory.getLogger(AreaRestApi.class);
    private AreaApi entityService;

    // Area Config keys
    public static final String HYPERIOT_AREA_UPLOAD_FOLDER_PATH = "it.acsoftware.hyperiot.area.uploadFolder.path";
    public static final String HYPERIOT_AREA_UPLOAD_FOLDER_MAX_FILE_SIZE = "it.acsoftware.hyperiot.area.uploadFolder.maxFileSize";

    private String assetsFolderPath = "./data/assets/";
    private long assetsFileMaxLength = 1000000;

    public AreaRestApi() {
        super();
        // read config
        Object uploadFolderPath = HyperIoTUtil.getHyperIoTProperty(HYPERIOT_AREA_UPLOAD_FOLDER_PATH);
        if (uploadFolderPath != null && !uploadFolderPath.toString().isEmpty()) {
            assetsFolderPath = uploadFolderPath.toString();
        }
        Object uploadMaxFileSize = HyperIoTUtil.getHyperIoTProperty(HYPERIOT_AREA_UPLOAD_FOLDER_MAX_FILE_SIZE);
        if (uploadMaxFileSize != null && !uploadMaxFileSize.toString().isEmpty()) {
            assetsFileMaxLength = Long.parseLong(uploadMaxFileSize.toString());
        }
    }

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Area Module works!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/area/module/status");
        return Response.ok("Area Module works!").build();
    }

    @GET
    @Path("/config")
    @ApiOperation(value = "/config", notes = "Get Area service config (eg. max allowed file size for uploads).", httpMethod = "GET")
    public Response getConfig() {
        getLog().debug("In Rest Service GET /hyperiot/area/config");
        return Response.ok(String.format("{ \"maxFileSize\": %d }", assetsFileMaxLength)).build();
    }

    /**
     * @return the current entityService
     */
    @Override
    protected AreaApi getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = AreaApi.class)
    protected void setEntityService(AreaApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing Area
     *
     * @param id id from which Area object will retrieved
     * @return Area if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}", notes = "Service for finding area", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findArea(
            @ApiParam(value = "id from which area object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/areas/{}", id);
        return this.find(id);
    }

    /**
     * Gets inner areas.
     *
     * @param id Parent area id
     * @return List of inner areas or error
     */
    @GET
    @Path("/{id}/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/tree", notes = "Service for finding inner areas", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Extended.class)
    public Response findInnerAreas(
            @ApiParam(value = "id of parent area", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/areas/{}/tree", id);
        try {
            Area area = this.entityService.getAll(this.getHyperIoTContext(), id);
            ObjectMapper mapper = new ObjectMapper();
            String jsonAreaTree = mapper.writeValueAsString(area);
            return Response.ok(jsonAreaTree).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    @GET
    @Path("/{id}/tree/devices")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/tree/devices", notes = "Service for finding devices of an area and its inner areas", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getAreaDeviceDeepList(@ApiParam(value = "id of the area", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/areas/{}/tree/devices", id);
        try {
            return Response.ok().entity(entityService.getAreaDevicesDeepList(getHyperIoTContext(), id, false)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    @GET
    @Path("/{id}/tree/devices/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/tree/devices/all", notes = "Service for finding devices of an area and its inner areas starting from root node", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Compact.class)
    public Response getAreaDeviceDeepListFromRoot(@ApiParam(value = "id of the area", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/areas/{}/tree/devices/all", id);
        try {
            return Response.ok().entity(entityService.getAreaDevicesDeepList(getHyperIoTContext(), id, true)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Gets area path.
     *
     * @param id area id
     * @return List of areas in the area path
     */
    @GET
    @Path("/{id}/path")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/path", notes = "Service for getting area path", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Compact.class)
    public Response getAreaPath(
            @ApiParam(value = "id of area", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/areas/{}/path", id);
        try {
            return Response.ok(this.entityService.getAreaPath(this.getHyperIoTContext(), id)).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Service saves a new Area
     *
     * @param entity Area object to store in database
     * @return the Area saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas", notes = "Service for adding a new area entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveArea(@ApiParam(value = "Area entity which must be saved ", required = true) Area entity) {
        getLog().debug("In Rest Service POST /hyperiot/areas \n Body:{}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a Area
     *
     * @param entity Area object to update in database
     * @return the Area updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas", notes = "Service for updating a area entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateArea(@ApiParam(value = "Area entity which must be updated ", required = true) Area entity) {
        getLog().debug("In Rest Service PUT /hyperiot/areas \n Body: {}", entity);
        try {
            return Response.ok().entity(this.getEntityService().updateAndPreserveImageData(entity, getHyperIoTContext())).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Service updates a Area
     *
     * @param areaId area to update inside the database
     */
    @PUT
    @Path("/{id}/resetType/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/resetType/{type}", notes = "Service for updating a area entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response resetAreaType(@ApiParam(value = "Area entity which must be updated ", required = true) @PathParam("id") Long areaId, @ApiParam(value = "New view type ", required = true) @PathParam("type") String type) {
        getLog().debug("In Rest Service PUT /hyperiot/areas/{}/restType/{} ", areaId, type);
        try {
            AreaViewType viewType = AreaViewType.valueOf(type);
            this.getEntityService().resetAreaType(getHyperIoTContext(), areaId, viewType);
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return handleException(new HyperIoTRuntimeException("Illegal view type"));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Service deletes a Area
     *
     * @param id id from which Area object will deleted. Inner areas will be deleted as well.
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}", notes = "Service for deleting a area entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response deleteArea(
            @ApiParam(value = "The area id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/areas/{}", id);
        try {
            this.entityService.removeAll(this.getHyperIoTContext(), id);
            return Response.ok().build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Set area background image
     *
     * @param id        Area id
     * @param imageFile Image file
     * @return success or error response
     */
    @POST
    @Path("/{id}/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/image", notes = "Service for setting the area background image", httpMethod = "POST", consumes = "multipart/form-data", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")
    })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response setAreaImage(@ApiParam(value = "The area id", required = true) @PathParam("id") long id,
                                 @Multipart(value = "image_file") Attachment imageFile) {
        getLog().debug("In Rest Service POST /hyperiot/areas/{}/image", id);
        Area area = null;
        try {
            area = this.entityService.find(id, this.getHyperIoTContext());
        } catch (Throwable t) {
            return this.handleException(t);
        }
        if (imageFile == null) {
            HyperIoTBaseError response = HyperIoTBaseError.generateHyperIoTError(new IOException(), Arrays.asList("Missing image file"),
                    HyperIoTErrorConstants.VALIDATION_ERROR);
            return Response.status(response.getStatusCode()).entity(response).build();
        } else {
            try {
                String fileName = "";
                String[] contentDispositionHeader = imageFile.getHeader("Content-Disposition").split(";");
                for (String name : contentDispositionHeader) {
                    if ((name.trim().startsWith("filename"))) {
                        String[] tmp = name.split("=");
                        fileName = tmp[1].trim().replaceAll("\"", "");
                    }
                }
                String fileExtension = "";
                if (fileName.indexOf(".") > 0) {
                    fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                }
                if (area.getAreaViewType() != null && area.getAreaViewType().getSupportedFileExentsions().contains(fileExtension)) {
                    // copy file to assets folder
                    File assetsFolder = new File(assetsFolderPath);
                    if (!assetsFolder.exists()) {
                        assetsFolder.mkdirs();
                    }
                    String oldImagePath = null;
                    if (area.getImagePath() != null && !area.getImagePath().isEmpty()) {
                        oldImagePath = area.getImagePath();
                    }
                    File assetsAreaFile = new File(assetsFolder.getAbsolutePath(), String.valueOf(id).concat("_img.").concat(fileExtension));
                    imageFile.transferTo(assetsAreaFile);
                    if (assetsAreaFile.length() > assetsFileMaxLength) {
                        assetsAreaFile.delete();
                        HyperIoTBaseError response = HyperIoTBaseError.generateHyperIoTError(new IOException(), Arrays.asList("File length must be <= " + assetsFileMaxLength),
                                HyperIoTErrorConstants.INTERNAL_ERROR);
                        return Response.status(response.getStatusCode()).entity(response).build();
                    }
                    // update area image path
                    area.setImagePath(assetsAreaFile.getPath());
                    if (!area.getImagePath().equals(oldImagePath)) {
                        //removing old Image
                        try {
                            File f = new File(oldImagePath);
                            if (f.exists())
                                f.delete();
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    HyperIoTBaseError response = HyperIoTBaseError.generateHyperIoTError(new IOException(), Arrays.asList("File type not supported."),
                            HyperIoTErrorConstants.VALIDATION_ERROR);
                    return Response.status(response.getStatusCode()).entity(response).build();
                }
            } catch (IOException e) {
                HyperIoTBaseError response = HyperIoTBaseError.generateHyperIoTError(e, Arrays.asList("Error while creating output file (JAR)"),
                        HyperIoTErrorConstants.INTERNAL_ERROR);
                return Response.status(response.getStatusCode()).entity(response).build();
            }
        }

        return this.update(area);
    }

    /**
     * Gets the area background image file
     *
     * @param id The area id
     * @return The image file or error
     */
    @GET
    @Path("/{id}/image")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/image", notes = "Service to get the area background image", httpMethod = "GET", produces = "application/octet-stream", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")
    })
    public Response getAreaImage(@ApiParam(value = "The area id", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/areas/{}/image", id);
        Area area = null;
        try {
            area = this.entityService.find(id, this.getHyperIoTContext());
        } catch (Throwable t) {
            return this.handleException(t);
        }
        File assetsAreaFile = new File(area.getImagePath());
        if (!assetsAreaFile.exists()) {
            HyperIoTBaseError response = HyperIoTBaseError.generateHyperIoTError(new IOException(), Arrays.asList("Image file not found"),
                    HyperIoTErrorConstants.INTERNAL_ERROR);
            return Response.status(response.getStatusCode()).entity(response).build();
        }
        return Response.ok(assetsAreaFile, MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + assetsAreaFile.getName() + "\"")
                .build();
    }

    /**
     * Unset the area background image
     *
     * @param id The area id
     * @return
     */
    @DELETE
    @Path("/{id}/image")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/image", notes = "Service to unset the area background image", httpMethod = "DELETE", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")
    })
    @JsonView(HyperIoTJSONView.Public.class)
    public Response unsetAreaImage(@ApiParam(value = "The area id", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/areas/{}/image", id);
        Area area = null;
        try {
            area = this.entityService.find(id, this.getHyperIoTContext());
        } catch (Throwable t) {
            return this.handleException(t);
        }
        File assetsAreaFile = new File(area.getImagePath());
        if (assetsAreaFile.exists()) {
            assetsAreaFile.delete();
        }
        area.setImagePath(null);
        return this.update(area);
    }

    /**
     * Service finds all available area
     *
     * @return list of all available area
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/all", notes = "Service for finding all area entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllArea() {
        getLog().debug("In Rest Service GET /hyperiot/areas/");
        return this.findAll();
    }

    /**
     * Service finds all available area
     *
     * @return list of all available area
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas", notes = "Service for finding all area entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Compact.class)
    public Response findAllAreaPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/areas/");
        return this.findAll(delta, page);
    }

    /**
     * Gets the list of area devices
     *
     * @param id Area id
     * @return List of HDevice
     */
    @GET
    @Path("/{id}/devices")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/devices", notes = "Return the list of devices in an area", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Compact.class)
    public Response getAreaDeviceList(@ApiParam(value = "id of the area", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/areas/{}/devices", id);
        try {
            return Response.ok().entity(entityService.getAreaDevicesList(getHyperIoTContext(), id)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Adds or updates an area device
     *
     * @param id         Area id
     * @param areaDevice The area device
     * @return
     */
    @PUT
    @Path("/{id}/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/devices/{areaDevice}", notes = "Adds or updates an area device", httpMethod = "PUT", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response addAreaDevice(@ApiParam(value = "id of the area", required = true) @PathParam("id") long id,
                                  @ApiParam(value = "the area device", required = true) AreaDevice areaDevice) {
        getLog().debug("In Rest Service PUT /hyperiot/areas/{}/devices {}", new Object[]{id, areaDevice});
        try {
            entityService.saveAreaDevice(getHyperIoTContext(), id, areaDevice);
            return Response.ok().entity(areaDevice).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Gets an area device
     *
     * @param areaDeviceId the AreaDevice id
     * @return
     */
    @GET
    @Path("/devices/{areaDeviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/devices/{areaDeviceId}", notes = "Gets an area device", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Compact.class)
    public Response getAreaDevice(@ApiParam(value = "id of the area device", required = true) @PathParam("areaDeviceId") long areaDeviceId) {
        getLog().debug("In Rest Service GET /hyperiot/areas/devices/{}", areaDeviceId);
        try {
            return Response.ok().entity(entityService.getAreaDevice(getHyperIoTContext(), areaDeviceId)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Deletes an area device
     *
     * @param id           Area id
     * @param areaDeviceId the AreaDevice id
     * @return
     */
    @DELETE
    @Path("/{id}/devices/{areaDeviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/areas/{id}/devices/{areaDeviceId}", notes = "Deletes an area device", httpMethod = "DELETE", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Compact.class)
    public Response removeAreaDevice(@ApiParam(value = "id of the area", required = true) @PathParam("id") long id,
                                     @ApiParam(value = "id of the area device", required = true) @PathParam("areaDeviceId") long areaDeviceId) {
        getLog().debug("In Rest Service DELETE /hyperiot/areas/{}/devices/{}", new Object[]{id, areaDeviceId});
        try {
            entityService.removeAreaDevice(getHyperIoTContext(), id, areaDeviceId);
            return Response.ok().build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }
}

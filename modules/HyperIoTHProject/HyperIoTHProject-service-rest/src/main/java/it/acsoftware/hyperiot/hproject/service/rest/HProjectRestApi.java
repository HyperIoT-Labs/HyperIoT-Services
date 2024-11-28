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

package it.acsoftware.hyperiot.hproject.service.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.model.HProjectExportAreaDeviceMixin;
import it.acsoftware.hyperiot.area.model.HProjectExportAreaMixin;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.model.HyperIoTValidationError;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExportManager;
import it.acsoftware.hyperiot.hpacket.api.HPacketDataExporter;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.hproject.algorithm.model.dto.*;
import it.acsoftware.hyperiot.hproject.api.HProjectApi;
import it.acsoftware.hyperiot.hproject.api.hadoop.HProjectHadoopApi;
import it.acsoftware.hyperiot.hproject.api.hbase.HProjectHBaseApi;
import it.acsoftware.hyperiot.hproject.model.*;
import it.acsoftware.hyperiot.hproject.model.hbase.timeline.TimelineColumnFamily;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.security.auth.x500.X500PrivateCredential;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Aristide Cittadino HProject rest service class. Registered with DOSGi
 * CXF
 */
@SwaggerDefinition(basePath = "/hprojects", info = @Info(description = "HyperIoT HProject API", version = "2.0.0", title = "HyperIoT HProject", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/hprojects", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HProjectRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/hprojects",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"}, immediate = true)
@Path("")
public class HProjectRestApi extends HyperIoTBaseEntityRestApi<HProject> {
    private HProjectApi entityService;
    private HProjectHBaseApi hProjectHBaseApi;
    private HProjectHadoopApi hProjectHadoopApi;
    private HPacketDataExportManager hPacketDataExportManager;
    private static final String PACKET_IDS_REGEX = "-?\\d+(,-?\\d+)*";
    private static final String DEVICE_IDS_REGEX = "-?\\d+(,-?\\d+)*";

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT HProject Module works!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/hyperiot/hprojects/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    @JsonView(HyperIoTJSONView.Public.class)
    public Response checkModuleWorking() {
        getLog().debug("In Rest Service GET /hyperiot/hproject/module/status");
        return Response.ok("HProject Module works!").build();
    }

    /**
     * @return the current entityService
     */
    @Override
    protected HProjectApi getEntityService() {
        getLog().debug("invoking getEntityService, returning: {}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = HProjectApi.class)
    protected void setEntityService(HProjectApi entityService) {
        getLog().debug("invoking setEntityService, setting: {}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * @param hProjectHBaseApi
     */
    @Reference
    protected void setHProjectHBaseApi(HProjectHBaseApi hProjectHBaseApi) {
        this.hProjectHBaseApi = hProjectHBaseApi;
    }

    /**
     * @param hProjectHadoopApi
     */
    @Reference
    public void sethProjectHadoopApi(HProjectHadoopApi hProjectHadoopApi) {
        this.hProjectHadoopApi = hProjectHadoopApi;
    }

    /**
     * @param hPacketDataExportManager
     */
    @Reference
    public void sethPacketDataExportManager(HPacketDataExportManager hPacketDataExportManager) {
        this.hPacketDataExportManager = hPacketDataExportManager;
    }

    /**
     * Service finds an existing HProject
     *
     * @param id id from which HProject object will retrieved
     * @return HProject if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{id}", notes = "Service for finding hproject", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findHProject(
            @ApiParam(value = "id from which project object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/{}", id);
        return this.find(id);
    }

    /**
     * Service finds an existing HProject
     *
     * @param id id from which HProject object will retrieved
     * @return HProject if found
     */
    @GET
    @Path("/{id}/details")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{id}/details", notes = "Service for finding hproject with deeper details", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Extended.class)
    public Response findHProjectDetails(
            @ApiParam(value = "id from which project object will retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/{}", id);
        try {
            HProject project = this.getEntityService().load(id, getHyperIoTContext());
            return Response.ok(project).build();
        } catch (Throwable var4) {
            return this.handleException(var4);
        }
    }

    /**
     * Service finds all user projects with deeper details
     *
     * @return HProjects list if found
     */
    @GET
    @Path("/details")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/details", notes = "Service for finding all user projects with deeper details", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Extended.class)
    public Response findHProjectsDetails() {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/details");
        try {
            Collection<HProject> project = this.getEntityService().load(getHyperIoTContext());
            return Response.ok(project).build();
        } catch (Throwable var4) {
            return this.handleException(var4);
        }
    }

    /**
     * Service saves a new HProject
     *
     * @param entity HProject object to store in database
     * @return the HProject saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects", notes = "Service for adding a new hproject entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveHProject(
            @ApiParam(value = "HProject entity which must be saved ", required = true) HProject entity) {
        getLog().debug("In Rest Service POST /hyperiot/hprojects \n Body: {}", entity);
        return this.save(entity);
    }

    /**
     * Creates a challenge for the given project Id
     *
     * @param projectId
     * @return
     */
    @POST
    @Path("/auto-register-project/challenge/{projectId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/hprojects/auto-register-project/challenge/{projectId}", notes = "Service for adding a new empty hproject entity for autoregister devices with gateway", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response createChallengeForAutoRegister(@ApiParam(value = "HProject id which must be used forgenerating the challenge ", required = true) @PathParam("projectId") long projectId) {
        getLog().debug("In Rest Service POST /hyperiot/hprojects/auto-register-project/challenge/{}", projectId);
        try {
            AutoRegisterChallengeRequest arcr = this.getEntityService().createAutoRegisterChallenge(projectId);
            return Response.ok(arcr).build();
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    /**
     * Service saves a new HProject
     *
     * @param entity HProject object to store in database
     * @return the HProject saved
     */
    @POST
    @Path("/auto-register-project")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/auto-register-project", notes = "Service for adding a new empty hproject entity for autoregister devices with gateway", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveAutoRegisteredHProject(
            @ApiParam(value = "HProject entity which must be saved ", required = true) HProject entity) {
        getLog().debug("In Rest Service POST /hyperiot/hprojects/auto-register-project \n Body: {}", entity);
        try {
            X500PrivateCredential cred = this.getEntityService().createEmptyAutoRegisterProject(entity, this.getHyperIoTContext());
            AutoRegisterProjectCredentials projectCred = new AutoRegisterProjectCredentials();
            projectCred.setProject(entity);
            projectCred.setPrivateCert(Base64.getEncoder().encodeToString(cred.getCertificate().getEncoded()));
            projectCred.setPrivateKey(Base64.getEncoder().encodeToString(cred.getPrivateKey().getEncoded()));
            return Response.ok(projectCred).build();
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    /**
     * Service saves a new HProject
     *
     * @param registerRequest HProject object to store in database
     * @return the HProject saved
     */
    @POST
    @Path("/register/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/hprojects/register", notes = "Service for adding a new empty hproject entity for register devices with gateway", httpMethod = "POST", produces = "application/json", consumes = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response autoRegisterHProject(
            @ApiParam(value = "HProject entity which must be saved ", required = true) AutoRegisterProjectRequest registerRequest) {
        getLog().debug("In Rest Service POST /hyperiot/hprojects/register \n Body: {}", registerRequest);
        try {
            return Response.ok(this.getEntityService().autoRegister(registerRequest.getCipherTextChallenge(), registerRequest.getProjectId(), registerRequest.getPackets())).build();
        } catch (Throwable t) {
            return handleException(t);
        }
    }


    /**
     * Service updates a HProject
     *
     * @param entity HProject object to update in database
     * @return the HProject updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects", notes = "Service for updating a hproject entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHProject(
            @ApiParam(value = "HProject entity which must be updated ", required = true) HProject entity) {
        getLog().debug("In Rest Service PUT /hyperiot/hprojects \n Body: {}", entity);
        return this.update(entity);
    }

    /**
     * Service deletes a HProject
     *
     * @param id id from which HProject object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{id}", notes = "Service for deleting a hproject entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteHProject(
            @ApiParam(value = "The hproject id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service DELETE /hyperiot/hprojects/{}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available HProject
     *
     * @return list of all available HProject
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/all", notes = "Service for finding all hproject entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHProject() {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/");
        return this.findAll();
    }

    /**
     * Service finds all available HProject
     *
     * @return list of all available HProject
     */
    @GET
    @Path("/all/cards")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/all/cards", notes = "Service for finding all hproject entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @Deprecated
    @JsonView(HProjectJSONView.Cards.class)
    public Response cardsView() {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/");
        return this.findAll();
    }

    @DELETE
    @Path("/{id}/hadoopData")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{id}/hadoopData", notes = "Delete Hadoop data of this project, i.e. data on HDFS and HBase ",
            httpMethod = "DELETE", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Project not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response clearHadoopData(
            @ApiParam(value = "id of the project", required = true) @PathParam("id") long id) throws IOException {
        getLog().debug("In Rest Service DELETE /hyperiot/hprojects/{}/hadoopData", id);
        try {
            this.hProjectHadoopApi.deleteHadoopData(getHyperIoTContext(), id);
            return Response.ok("Data have been deleted successfully!").build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service finds all available HProject
     *
     * @return list of all available HProject
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects", notes = "Service for finding all hproject entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHProjectPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/");
        return this.findAll(delta, page);
    }

    @GET
    @Path("/{id}/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{id}/tree", notes = "Return the project tree in JSON format", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getHProjectTreeView(
            @ApiParam(value = "id of the project", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/{}", id);
        try {
            return Response.ok().entity(entityService.getProjectTreeViewJson(getHyperIoTContext(), id)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Gets the list of a project areas
     *
     * @param id id of the project from which to get the list of areas
     * @return List of project areas
     */
    @GET
    @Path("/{id}/areas")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{id}/areas", notes = "Return the list of project areas", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getHProjectAreaList(
            @ApiParam(value = "id of the project", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/{}", id);
        try {
            return Response.ok().entity(entityService.getAreasList(getHyperIoTContext(), id)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    @GET
    @Path("/{hProjectId}/hpackets/{rowKeyLowerBound}/{rowKeyUpperBound}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{hProjectId}/hpackets/{rowKeyLowerBound}/{rowKeyUpperBound}", notes = "Service for scan HProject data", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response scanHProject(
            @ApiParam(value = "HProject ID from retrieve HPackets in Avro format and events", required = true) @PathParam("hProjectId") long hProjectId,
            @ApiParam(value = "HBase row key lower bound", required = true) @PathParam("rowKeyLowerBound") long rowKeyLowerBound,
            @ApiParam(value = "HBase row key upper bound", required = true) @PathParam("rowKeyUpperBound") long rowKeyUpperBound,
            @ApiParam(value = "Limit, maximum number of records, please use carefully", required = true) @QueryParam("maxResults") Integer maxResults,
            @ApiParam(value = "HPacket list, containing comma separated ID", required = true) @QueryParam("packetIds") String packetIds,
            @ApiParam(value = "HDevice list, containing comma separated ID", required = true) @QueryParam("deviceIds") String deviceIds,
            @ApiParam(value = "Alarm state , contain the state of the alarm", required = true) @QueryParam("alarmState") String alarmState) {
        getLog().debug("In Rest Service GET hyperiot/hprojects/scanHProject/{}/{}/{}?packetIds={}&deviceIds={}&?alarmState={}",
                hProjectId, rowKeyLowerBound, rowKeyUpperBound, packetIds, deviceIds, alarmState);

        final int limit = (maxResults == null) ? 0 : maxResults;
        try {
            if (rowKeyLowerBound > rowKeyUpperBound)
                throw new IllegalArgumentException("startTime must be prior or equal to endTime");
            if (!isIdsListQueryParamValid(packetIds, PACKET_IDS_REGEX)) {
                throw new IllegalArgumentException("wrong packetIds parameter");
            }
            if (!isIdsListQueryParamValid(deviceIds, DEVICE_IDS_REGEX)) {
                throw new IllegalArgumentException("wrong deviceIds parameter");
            }
            List<String> packetIdsList = formatIdsListFromQueryParam(packetIds);
            List<String> deviceIdsList = formatIdsListFromQueryParam(deviceIds);
            StreamingOutput stream = out -> hProjectHBaseApi.scanHProject(getHyperIoTContext(), hProjectId,
                    packetIdsList, deviceIdsList, rowKeyLowerBound, rowKeyUpperBound, limit, alarmState, out);
            return Response.ok(stream).build();
        } catch (IllegalArgumentException e) {
            return handleException(e);
        }
    }

    @GET
    @Path("/{hProjectId}/hpacket/{hPacketId}/attachments/{fieldId}/{rowKeyLowerBound}/{rowKeyUpperBound}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{hProjectId}/hpacket/{hPacketId}/attachments/{fieldId}/{timestamp}", notes = "Service for retrieving HProject attachments", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response scanHProject(
            @ApiParam(value = "HProject ID from retrieve HPackets in Avro format and events", required = true) @PathParam("hProjectId") long hProjectId,
            @ApiParam(value = "HPacket ID", required = true) @PathParam("hPacketId") long hPacketId,
            @ApiParam(value = "Attachment field id", required = true) @PathParam("fieldId") long fieldId,
            @ApiParam(value = "Attachment Timestamp identifier", required = true) @PathParam("rowKeyLowerBound") long rowKeyLowerBound,
            @ApiParam(value = "Attachment Timestamp identifier", required = true) @PathParam("rowKeyUpperBound") long rowKeyUpperBound) {

        getLog().debug("In Rest Service GET hyperiot/hprojects/{}/hpacket/{}/attachments/{}/{}/{}",
                hProjectId, hPacketId, fieldId, rowKeyLowerBound, rowKeyUpperBound);
        try {
            byte[] attachment = this.hProjectHBaseApi.getHPacketAttachment(getHyperIoTContext(), hProjectId, hPacketId, fieldId, rowKeyLowerBound, rowKeyUpperBound);
            return Response.ok(attachment).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @POST
    @Path("/{hProjectId}/hpacket/{hPacketId}/export/{hPacketFormat}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{hProjectId}/hpacket/{hPacketId}/export/{hPacketFormat}", notes = "Service for exporting HPacket data", httpMethod = "POST", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response exportHPacketData(
            @ApiParam(value = "HProject ID from retrieve HPacket data", required = true) @PathParam("hProjectId") long hProjectId,
            @ApiParam(value = "HPacket ID related to the packet the user wants to exports", required = true) @PathParam("hPacketId") long hPacketId,
            @ApiParam(value = "HPacket Format ", required = true) @PathParam("hPacketFormat") String hPacketFormatName,
            @ApiParam(value = "Export name", required = true) @QueryParam("exportName") String exportName,
            @ApiParam(value = "HBase row key lower bound", required = true) @QueryParam("rowKeyLowerBound") long rowKeyLowerBound,
            @ApiParam(value = "HBase row key upper bound", required = true) @QueryParam("rowKeyUpperBound") long rowKeyUpperBound,
            @ApiParam(value = "Prettify timestamp", required = false) @QueryParam("prettyTimestamp") Boolean prettifyTimestamp,
            @ApiParam(value = "timestamp pattern", required = false) @QueryParam("timestampPattern") String timestampPattern) {
        getLog().debug("In Rest Service GET /{}/hpacket/{}/export/{}",
                hProjectId, hPacketId, hPacketFormatName);
        if (prettifyTimestamp == null)
            prettifyTimestamp = false;
        try {
            if (rowKeyLowerBound > rowKeyUpperBound)
                throw new IllegalArgumentException("startTime must be prior or equal to endTime");
            if (!isIdsListQueryParamValid(String.valueOf(hPacketId), PACKET_IDS_REGEX)) {
                throw new IllegalArgumentException("wrong packetIds parameter");
            }
            HPacketDataExporter exporter = this.entityService.exportHPacketData(HPacketFormat.valueOf(hPacketFormatName.toUpperCase()), exportName, hProjectId, hPacketId, prettifyTimestamp, timestampPattern, getHyperIoTContext());
            if (rowKeyLowerBound > 0)
                exporter.setFrom(rowKeyLowerBound);

            if (rowKeyUpperBound > 0)
                exporter.setTo(rowKeyUpperBound);
            //default time is from now to avoid possibile exploits
            HPacketDataExportStatus status = exporter.start();
            return Response.ok(status).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GET
    @Path("/export/{exportId}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/export/{exportId}", notes = "Service for getting the status of HPacket export", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response getExportStatus(
            @ApiParam(value = "Export ID ", required = true) @PathParam("exportId") String exportId) {
        getLog().debug("In Rest Service GET /export/{}", exportId);
        try {
            HPacketDataExport dataExport = hPacketDataExportManager.getHPacketDataExport(exportId);
            return Response.ok(this.entityService.exportStatus(exportId, dataExport.gethProjectId(), getHyperIoTContext())).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @POST
    @Path("/export/stop/{exportId}")
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/export/{exportId}/stop", notes = "Service for stopping HPacket export", httpMethod = "POST", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response stopExport(
            @ApiParam(value = "Export ID ", required = true) @PathParam("exportId") String exportId) {
        getLog().debug("In Rest Service GET /export/{}/stop", exportId);
        try {
            HPacketDataExport dataExport = hPacketDataExportManager.getHPacketDataExport(exportId);
            this.entityService.stopExport(exportId, dataExport.gethProjectId(), getHyperIoTContext());
            return Response.ok().build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GET
    @Path("/export/download/{exportId}")
    @Produces({"application/octet-stream"})
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/export/download/{exportId}", notes = "Service for downloading HPacket export", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response downloadHPacketDataExport(
            @ApiParam(value = "Export ID ", required = true) @PathParam("exportId") String exportId) {
        getLog().debug("In Rest Service GET /export/download/{}", exportId);
        try {
            HPacketDataExport dataExport = hPacketDataExportManager.getHPacketDataExport(exportId);
            if (dataExport.isDownloaded())
                throw new HyperIoTRuntimeException("Export already downloaded");
            HPacketDataExportStatus status = HPacketDataExportStatus.fromJsonBytes(this.entityService.exportStatus(exportId, dataExport.gethProjectId(), getHyperIoTContext()).getBytes());
            if (!status.isCompleted())
                throw new HyperIoTRuntimeException("Exporting not completed yet...");
            StreamingOutput streamingOutput = (output -> {
                try (InputStream inputStream = this.entityService.getExportStream(exportId, dataExport.gethProjectId(), getHyperIoTContext())) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                    //setting export as downloaded and eventually remove file
                    this.entityService.finalizeExportDownload(exportId, dataExport.gethProjectId(), getHyperIoTContext());
                } catch (Exception e) {
                    getLog().error(e.getMessage(), e);
                    throw new HyperIoTRuntimeException("Impossible to read exported file...");
                }
            });
            return Response.ok(streamingOutput).header("Content-Disposition", "attachment; filename=\"" + status.getFileName() + "\"").build();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * Service count HPackets from timeline table and HPacket events
     *
     * @param projectId Project ID
     * @param packetIds Packet IDs
     * @param startTime Timeline start time
     * @param endTime   Timeline end time
     * @return Response object
     */
    @GET
    @Path("/timeline/events/count/{projectId}/{startTime}/{endTime}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/timeline/events/count/{projectId}/{startTime}/{endTime}",
            notes = "Service for count data and get it back", httpMethod = "GET", produces = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response timelineEventCount(
            @ApiParam(value = "Project ID", required = true) @PathParam("projectId") long projectId,
            @ApiParam(value = "Scanning start time", required = true) @PathParam("startTime") long startTime,
            @ApiParam(value = "Scanning end time", required = true) @PathParam("endTime") long endTime,
            @ApiParam(value = "HPacket list, containing comma separated ID", required = true) @QueryParam("packetIds") String packetIds,
            @ApiParam(value = "HDevice list, containing comma separated ID", required = true) @QueryParam("deviceIds") String deviceIds
    ) {
        getLog().info("In Rest Service GET hyperiot/hprojects/timeline/events/count/{}/{}/{}?packetIds={}",
                projectId, startTime, endTime, packetIds);
        try {
            if (startTime > endTime)
                throw new IllegalArgumentException("startTime must be prior or equal to endTime");
            if (!isIdsListQueryParamValid(packetIds, PACKET_IDS_REGEX)) {
                throw new IllegalArgumentException("wrong packetIds parameter");
            }
            if (!isIdsListQueryParamValid(deviceIds, DEVICE_IDS_REGEX)) {
                throw new IllegalArgumentException("wrong deviceIds parameter");
            }
            List<String> packetIdsList = formatIdsListFromQueryParam(packetIds);
            List<String> deviceIdsList = formatIdsListFromQueryParam(deviceIds);
            return Response.ok(hProjectHBaseApi.timelineEventCount(this.getHyperIoTContext(), projectId, packetIdsList, deviceIdsList, startTime, endTime)).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service scans and returns data from timeline table and hpacket event table
     *
     * @param tableName Table name
     * @param packetIds Packet IDs
     * @param step      Step, which determines output
     * @param startTime Timeline start time
     * @param endTime   Timeline end time
     * @return Response object
     */
    @GET
    @Path("/timeline/events/{tableName}/{step}/{startTime}/{endTime}/{timezone}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/timeline/events/{tableName}/{step}/{startTime}/{endTime}/{timezone}",
            notes = "Service for scan data and get it back for timeline queries", httpMethod = "GET", produces = "application/json",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response timelineScan(
            @ApiParam(value = "Table name which count hpackets from", required = true) @PathParam("tableName") String tableName,
            @ApiParam(value = "Scanning step", required = true) @PathParam("step") String step,
            @ApiParam(value = "Scanning start time", required = true) @PathParam("startTime") long startTime,
            @ApiParam(value = "Scanning end time", required = true) @PathParam("endTime") long endTime,
            @ApiParam(value = "Timezone Timezone of client which has invoked the method, i.e. Europe/Rome", required = true) @PathParam("timezone") String timezone,
            @ApiParam(value = "HPacket list, containing comma separated ID", required = true) @QueryParam("packetIds") String packetIds,
            @ApiParam(value = "HDevice list, containing comma separated ID", required = true) @QueryParam("deviceIds") String deviceIds) {
        getLog().debug("In Rest Service GET hyperiot/hprojects/timeline/events/{}/{}/{}/{}?packetIds={}",
                tableName, step, startTime, endTime, packetIds);
        try {
            TimelineColumnFamily convertedStep = TimelineColumnFamily.valueOf(step.toUpperCase());
            if (startTime > endTime)
                throw new IllegalArgumentException("startTime must be prior or equal to endTime");
            if (!isIdsListQueryParamValid(packetIds, PACKET_IDS_REGEX)) {
                throw new IllegalArgumentException("wrong packetIds parameter");
            }
            if (!isIdsListQueryParamValid(deviceIds, DEVICE_IDS_REGEX)) {
                throw new IllegalArgumentException("wrong deviceIds parameter");
            }
            List<String> packetIdsList = formatIdsListFromQueryParam(packetIds);
            List<String> deviceIdsList = formatIdsListFromQueryParam(deviceIds); // remove duplicates
            return Response.ok(hProjectHBaseApi.timelineScan(this.getHyperIoTContext(), tableName, packetIdsList, deviceIdsList,
                    convertedStep, startTime, endTime, timezone)).build();
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @GET
    @Path("/{id}/exports")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{id}/exports", notes = "Service for export hproject", httpMethod = "GET", produces = "application/octet-stream", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value =
            {@ApiResponse(code = 200, message = "Successful operation"),
                    @ApiResponse(code = 403, message = "Not authorized"),
                    @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HProjectJSONView.Export.class)
    public Response exportHProject(
            @ApiParam(value = "id from which project object will be retrieve", required = true) @PathParam("id") long id) {
        getLog().debug("In Rest Service GET /hyperiot/hprojects/{}/exports", id);
        try {
            if (id <= 0) {
                throw new IllegalArgumentException("Invalid Request Parameter");
            }
            HyperIoTContext cxt = this.getHyperIoTContext();
            ExportProjectDTO projectToExport = entityService.loadHProjectForExport(cxt, id);
            return Response
                    .ok(serializeHProjectToFile(projectToExport))
                    .header("Content-Disposition",
                            String.format("attachment; filename =\"%s_%d.json\"", "Export_HProject", id))
                    .build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    @POST
    @Path("/imports")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/imports", notes = "Service for import a new HProject from a jsonFile",
            httpMethod = "POST", produces = "application/json", consumes = "multipart/form-data",
            authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HProjectJSONView.Export.class)
    public Response uploadFile(
            @Multipart(value = "file") InputStream uploadedFile) {
        getLog().debug("In Rest Service POST /hyperiot/hprojects/imports");
        try {
            if (uploadedFile.available() <= 0) {
                throw new HyperIoTRuntimeException("Invalid request parameter , the file is empty");
            }
            ExportProjectDTO dtoProject = deserializeProjectForImport(uploadedFile);
            if (dtoProject == null) {
                throw new HyperIoTRuntimeException("Invalid request parameter , the dtoProject is null");
            }
            if (dtoProject.getProject() == null) {
                throw new HyperIoTRuntimeException("Invalid request parameter , the dtoProject hasn't associate a project");
            }
            ImportLogReport logReport = entityService.importHProject(dtoProject, dtoProject.getProject(), this.getHyperIoTContext());
            return Response
                    .ok(logReport)
                    .build();
        } catch (Throwable exc) {
            return getResponseWhenImportFail(exc);
        }
    }

    /**
     * Service updates userOwner of HProject entity.
     *
     * @param projectId The id of the HProject to associate the new owner with
     * @param ownerId   the id of the new owner of the HProject.
     * @return the HProject updated
     */
    @PUT
    @Path("/{projectId}/owner/{ownerId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hprojects/{projectId}/owner/{ownerId}", notes = "Service for updating  hproject's owner entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHProjectOwner(
            @ApiParam(value = "id from which hproject object will retrieve", required = true) @PathParam("projectId") long projectId,
            @ApiParam(value = "id of the new owner of the hproject", required = true) @PathParam("ownerId") long ownerId) {
        getLog().debug("In Rest Service PUT /hyperiot/hprojects/{}/owner/{} ", projectId, ownerId);
        try {
            HProject project = entityService.updateHProjectOwner(this.getHyperIoTContext(), projectId, ownerId);
            return Response
                    .ok(project)
                    .build();
        } catch (Throwable e) {
            return this.handleException(e);
        }
    }

    private boolean isIdsListQueryParamValid(String idsList, String regex) {
        if (idsList == null || idsList.isEmpty())
            return true;
        return Pattern.matches(regex, idsList);
    }

    private List<String> formatIdsListFromQueryParam(String idsList) {
        if (idsList == null || idsList.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new HashSet<>(Arrays.asList(idsList.trim().split(","))));
    }

    private ExportProjectDTO deserializeProjectForImport(InputStream uploadedFile) throws IOException {
        getLog().debug("deserializeProjectForImport ");
        ObjectMapper mapper = new ObjectMapper();
        //Mixin is added because we need to override entity's serialization logic during import's operation.
        return mapper.
                setSerializationInclusion(JsonInclude.Include.NON_NULL).
                addMixIn(HyperIoTAbstractEntity.class, HProjectExportInheritedFieldMixin.class).
                addMixIn(HProjectAlgorithm.class, HProjectExportHProjectAlgorithmMixin.class).
                addMixIn(HPacketField.class, HProjectExportHPacketFieldMixin.class).
                addMixIn(AreaDevice.class, HProjectExportAreaDeviceMixin.class).
                addMixIn(Area.class, HProjectExportAreaMixin.class).
                readerWithView(HProjectJSONView.Export.class).
                readValue(uploadedFile, ExportProjectDTO.class);
    }

    private Response getResponseWhenImportFail(Throwable exc) {
        ImportLogReport importFailureReport = new ImportLogReport();
        importFailureReport.setImportResult(ImportReportStatus.FAILED);
        HyperIoTBaseError errorInformation = (HyperIoTBaseError) handleException(exc).getEntity();
        if (!(exc instanceof HyperIoTValidationException)) {
            if (errorInformation.getErrorMessages() != null && !errorInformation.getErrorMessages().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                Iterator<String> it = errorInformation.getErrorMessages().iterator();
                while (it.hasNext()) {
                    String currMessage = it.next();
                    sb.append(currMessage);
                    if (it.hasNext()) {
                        sb.append(" , ");
                    }
                }
                importFailureReport.addLogMessage(ImportLogLevel.ERROR, String.format("Error : %s", sb));
            }
            getLog().debug("No error message avaiable for exception");
        }
        if (errorInformation.getValidationErrors() != null && !errorInformation.getValidationErrors().isEmpty()) {
            for (HyperIoTValidationError validationError : errorInformation.getValidationErrors()) {
                importFailureReport.addLogMessage(ImportLogLevel.ERROR,
                        String.format("Error cause : %s ,Error message : %s Error field : %s , Error value : %s",
                                "VALIDATION ERROR",
                                validationError.getMessage(),
                                validationError.getField(),
                                validationError.getInvalidValue()
                        ));
            }
        }
        return Response.status(errorInformation.getStatusCode()).
                entity(importFailureReport).
                build();
    }

    private StreamingOutput serializeHProjectToFile(ExportProjectDTO project) {
        return output -> {
            Writer writer = new BufferedWriter(new OutputStreamWriter(output));
            ObjectMapper mapper = new ObjectMapper();
            //Mixin is added because we need to override entity's serialization logic during export's operation.
            String objectJsonRepresentation = mapper.
                    setSerializationInclusion(JsonInclude.Include.NON_NULL).
                    addMixIn(HyperIoTAbstractEntity.class, HProjectExportInheritedFieldMixin.class).
                    addMixIn(HProjectAlgorithm.class, HProjectExportHProjectAlgorithmMixin.class).
                    addMixIn(HPacketField.class, HProjectExportHPacketFieldMixin.class).
                    addMixIn(AreaDevice.class, HProjectExportAreaDeviceMixin.class).
                    addMixIn(Area.class, HProjectExportAreaMixin.class).
                    writerWithView(HProjectJSONView.Export.class).
                    withDefaultPrettyPrinter().
                    writeValueAsString(project);
            writer.write(objectJsonRepresentation);
            writer.flush();
        };
    }
}

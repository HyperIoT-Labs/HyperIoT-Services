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

package  it.acsoftware.hyperiot.kit.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.kit.api.KitApi;
import it.acsoftware.hyperiot.kit.model.Kit;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.Collection;


/**
 * 
 * @author Aristide Cittadino Kit rest service class. Registered with DOSGi CXF
 * 
 */
@SwaggerDefinition(basePath = "/kits", info = @Info(description = "HyperIoT Kit API", version = "2.0.0", title = "hyperiot Kit", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")),securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
		@ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(tags = "Kit", value = "/kits", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HyperIoTRestService.class, immediate = true)
@Path("/kits")
public class KitRestApi extends HyperIoTBaseEntityRestApi<Kit> implements HyperIoTRestService {
	private KitApi entityService ;

	/**
	 * Simple service for checking module status
	 * 
	 * @return HyperIoT Role Module work!
	 */
	@GET
	@Path("/module/status")
	@ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
	public Response checkModuleWorking() {
		getLog().debug("In Rest Service GET /hyperiot/kit/module/status");
		return Response.ok("Kit Module works!").build();
	}

	/**
	 * @Return the current entityService
	 */
	@Override
	protected HyperIoTBaseEntityApi<Kit> getEntityService() {
		getLog().debug("invoking getEntityService, returning: {}" , this.entityService);
		return entityService;
	}

	/**
	 * 
	 * @param entityService: Injecting entityService 
	 */
	@Reference(service = KitApi.class)
	protected void setEntityService(KitApi entityService) {
		getLog().debug("invoking setEntityService, setting: {}" , this.entityService);
		this.entityService = entityService;
	}

	/**
	 * 
	 * @param entityService: Unsetting current entityService
	 */
	protected void unsetEntityService(KitApi entityService) {
		getLog().debug("invoking unsetEntityService, setting: {}" , entityService);
		this.entityService = entityService;
	}

	/**
	 * Service finds an existing Kit
	 * 
	 * @param id id from which Kit object will retrieved
	 * @return  Kit if found
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits/{id}", notes = "Service for finding kit", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findKit(@PathParam("id") long id) {
		getLog().debug("In Rest Service GET /hyperiot/kits/{}" , id);
		return this.find(id);
	}

	/**
	 * Service saves a new Kit
	 * 
	 * @param entity Kit object to store in database
	 * @return the Kit saved
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits", notes = "Service for adding a new kit entity", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response saveKit(
		@ApiParam(value = "Kit entity which must be saved ", required = true) Kit entity) {
		getLog().debug("In Rest Service POST /hyperiot/kits \n Body: {}" , entity);
		return this.save(entity);
	}

	/**
	 * Service updates a Kit
	 * 
	 * @param entity Kit object to update in database
	 * @return the Kit updated
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits", notes = "Service for updating a kit entity", httpMethod = "PUT", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Invalid ID supplied") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response updateKit(
		@ApiParam(value = "Kit entity which must be updated ", required = true)Kit entity) {
		getLog().debug("In Rest Service PUT /hyperiot/kits \n Body: {}" , entity);
		return this.update(entity);
	}

	/**
	 * Service deletes a Kit
	 * 
	 * @param id id from which Kit object will deleted
	 * @return 200 OK if it has been deleted
	 */
	@DELETE
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits/{id}", notes = "Service for deleting a kit entity", httpMethod = "DELETE", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response deleteKit(
		@ApiParam(value = "The kit id which must be deleted", required = true) @PathParam("id") long id) {
		getLog().debug("In Rest Service DELETE /hyperiot/kits/{}" , id);
		return this.remove(id);
	}

	/**
	 * Service finds all available kit
	 * 
	 * @return list of all available kit
	 */
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits/all", notes = "Service for finding all kit entities", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllKit() {
		getLog().debug("In Rest Service GET /hyperiot/kits/");
		return this.findAll();
	}

	/**
	 * Service finds all available kit
	 * 
	 * @return list of all available kit
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits", notes = "Service for finding all kit entities", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllKitPaginated(@QueryParam("delta") Integer delta,@QueryParam("page") Integer page) {
		getLog().debug("In Rest Service GET /hyperiot/kits/");
		return this.findAll(delta,page);
	}

	/**
	 * Service find Kit's tags
	 *
	 * @param kitId from which tags will retrieved
	 * @return Kit's tag
	 */
	@GET
	@Path("/{kitId}/tags")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits/{kitId}/tags", notes = "Service for find Kit's tags ", httpMethod = "GET", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findKitTags(
			@PathParam("kitId") long  kitId) {
		getLog().debug("In Rest Service GET /hyperiot/kits/{}/tags" ,kitId );
		try {
			Collection<AssetTag> buildingTags = entityService.getKitTags(kitId,this.getHyperIoTContext());
			return Response.ok(buildingTags).build();
		}catch (Throwable exc){
			return this.handleException(exc);
		}
	}

	/**
	 * Service add a tag to Kit
	 *
	 * @param kitId The id of the Kit to associate the tag
	 * @return the tag saved.
	 */
	@POST
	@Path("/{kitId}/tags")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits/{kitId}/tags", notes = "Service for add a tag on a Kit", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response addTagToKit(
			@PathParam("kitId") long  kitId,
			@ApiParam(value = "Tag entity which must be saved", required = true) AssetTag tag) {
		getLog().debug("In Rest Service POST /hyperiot/kits/{}/tags" ,kitId );
		try {
			AssetTag buildingTag =entityService.addTagToKit(kitId,tag,this.getHyperIoTContext());
			return Response.ok(buildingTag).build();
		}catch (Throwable exc){
			return this.handleException(exc);
		}
	}


	/**
	 * Service deletes a tag from kit's tag list.
	 *
	 * @param  kitId the id of the kit to which tag is related .
	 * @param  tagId the id of the tag that will be delete
	 * @return 200 OK if it has been deleted
	 */
	@DELETE
	@Path("/{kitId}/tags/{tagId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits/{kitId}/tags/{tagId}", notes = "Service for delete a tag from a Kit", httpMethod = "DELETE", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response deleteTagFromKitTagsList(
			@PathParam("kitId") long kitId,
			@PathParam("tagId") long tagId) {
		getLog().debug("In Rest Service DELETE /hyperiot/kits/{}/tags/{}",kitId,tagId);
		try{
			entityService.deleteTagFromKit(kitId,tagId,getHyperIoTContext());
			return Response.ok().build();
		}catch (Throwable exc){
			return this.handleException(exc);
		}
	}

	/**
	 * Service add an HDevice to a HProject.
	 * The HDevice will be configure through HDeviceTemplate.
	 * @param projectId The id of the HProject on which the HDevice will be install.
	 * @param kitId The id of the kit from which HDeviceTemplate will be retrieved.
	 * @param deviceTemplateId The id of the HDeviceTemplate from which the HDevice will be configure.
	 * @param deviceName the deviceName of the HDevice that will be install.
	 * @return the saved HDevice.
	 */
	@POST
	@Path("/{kitId}/devicetemplate/{deviceTemplateId}/project/{projectId}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/kits/{kitId}/devicetemplate/{deviceTemplateId}/project/{projectId}", notes = "Service for add a new HDevice on a HProject", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response installHDeviceTemplateOnProject(
			@PathParam("kitId") long kitId,
			@PathParam("deviceTemplateId") long deviceTemplateId,
			@PathParam("projectId") long  projectId,
			@QueryParam("deviceName") String deviceName) {
		getLog().debug("In Rest Service POST /hyperiot/kits/{}/devicetemplate/{}/project/{}",kitId, deviceTemplateId, projectId);
		try {
			HDevice device = entityService.installHDeviceTemplateOnProject(this.getHyperIoTContext(), projectId, kitId, deviceName, deviceTemplateId);
			StreamingOutput jsonResponse = ((outputStream) -> {
				ObjectMapper mapper = new ObjectMapper();
				mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
				try(JsonGenerator jsonGenerator = mapper.getFactory().createGenerator(outputStream)) {
					jsonGenerator.writeObject(mapper.readValue(mapper.writerWithView(HyperIoTJSONView.Public.class).writeValueAsString(device), HDevice.class));
					jsonGenerator.flush();
				}});
			return Response
					.ok(jsonResponse)
					.build();
		}catch (Throwable exc){
			return this.handleException(exc);
		}
	}
	
}

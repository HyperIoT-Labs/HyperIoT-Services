package  it.acsoftware.hyperiot.services.service.rest;

import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import  it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi ;
import  it.acsoftware.hyperiot.base.api.HyperIoTBaseApi ;
import it.acsoftware.hyperiot.services.api.ServicesApi;


/**
 * 
 * @author Aristide Cittadino Services rest service class. Registered with DOSGi CXF
 * 
 */
@SwaggerDefinition(basePath = "/servicess", info = @Info(description = "HyperIoT Services API", version = "2.0.0", title = "hyperiot Services", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")),securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
		@ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/servicess", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = ServicesRestApi.class, property = { 
	    "service.exported.interfaces=it.acsoftware.hyperiot.services.service.rest.ServicesRestApi",
		"service.exported.configs=org.apache.cxf.rs","org.apache.cxf.rs.address=/servicess",
		"service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
		"service.exported.intents=swagger","service.exported.intents=exceptionmapper"
		 }, immediate = true)
@Path("")
public class ServicesRestApi extends  HyperIoTBaseRestApi  {
	private ServicesApi  service;

	/**
	 * Simple service for checking module status
	 * 
	 * @return HyperIoT Role Module work!
	 */
	@GET
	@Path("/module/status")
	@ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
	public Response checkModuleWorking() {
		getLog().debug("In Rest Service GET /hyperiot/services/module/status");
		return Response.ok("Services Module works!").build();
	}

	
	/**
	 * @Return the current service class
	 */
	protected HyperIoTBaseApi getService() {
		getLog().log(Level.FINEST, "invoking getService, returning: {}" , this.service);
		return service;
	}

	/**
	 * 
	 * @param entityService: Injecting service class 
	 */
	@Reference(service = ServicesApi.class)
	protected void setService(ServicesApi service) {
		getLog().debug("invoking setService, setting: {}" , service);
		this.service = service;
	}

	
}

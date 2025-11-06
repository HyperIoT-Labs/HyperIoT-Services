package it.acsoftware.hyperiot.services.service.rest;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.HyperIoTBaseApi;
import it.acsoftware.hyperiot.base.api.HyperIoTRestService;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.services.api.ServicesApi;
import it.acsoftware.hyperiot.services.util.HyperIoTServicesUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Aristide Cittadino Services rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/services", info = @Info(description = "HyperIoT Services API", version = "2.0.0", title = "hyperiot Services", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(tags = "Services", value = "/services", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HyperIoTRestService.class, immediate = true)
@Path("/services")
public class ServicesRestApi extends HyperIoTBaseRestApi implements HyperIoTRestService {
    private ServicesApi service;

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

    @GET
    @Path("/version")
    @ApiOperation(value = "/version", notes = "Returns current HyperIoT Services version", httpMethod = "GET")
    public Response getHyperIoTServicesVersions() {
        getLog().debug("In Rest Service GET /hyperiot/services/versions");
        Map<String, String> versionMap = new HashMap<>();
        versionMap.put("version", HyperIoTServicesUtil.getHyperIoTServicesVersion());
        return Response.ok(versionMap).build();
    }


    /**
     * @Return the current service class
     */
    protected HyperIoTBaseApi getService() {
        getLog().debug("invoking getService, returning: {}", this.service);
        return service;
    }

    /**
     * @param service: Injecting service class
     */
    @Reference(service = ServicesApi.class)
    protected void setService(ServicesApi service) {
        getLog().debug("invoking setService, setting: {}", service);
        this.service = service;
    }


}
